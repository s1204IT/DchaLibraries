package com.android.settings.dashboard.conditional;

import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.R;
import com.android.settings.Settings;
import java.util.List;

/* loaded from: classes.dex */
public class WorkModeCondition extends Condition {
    private UserManager mUm;
    private UserHandle mUserHandle;

    public WorkModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mUm = (UserManager) this.mManager.getContext().getSystemService("user");
    }

    private void updateUserHandle() {
        List profiles = this.mUm.getProfiles(UserHandle.myUserId());
        int size = profiles.size();
        this.mUserHandle = null;
        for (int i = 0; i < size; i++) {
            UserInfo userInfo = (UserInfo) profiles.get(i);
            if (userInfo.isManagedProfile()) {
                this.mUserHandle = userInfo.getUserHandle();
                return;
            }
        }
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void refreshState() {
        updateUserHandle();
        setActive(this.mUserHandle != null && this.mUm.isQuietModeEnabled(this.mUserHandle));
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_signal_workmode_enable);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_work_title);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_work_summary);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_on)};
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPrimaryClick() {
        this.mManager.getContext().startActivity(new Intent(this.mManager.getContext(), (Class<?>) Settings.AccountDashboardActivity.class).addFlags(268435456));
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onActionClick(int i) {
        if (i == 0) {
            if (this.mUserHandle != null) {
                this.mUm.requestQuietModeEnabled(false, this.mUserHandle);
            }
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + i);
        }
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public int getMetricsConstant() {
        return 383;
    }
}
