package com.android.settings.dashboard.conditional;

import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Icon;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.R;
import com.android.settings.Settings;
import java.util.List;

public class WorkModeCondition extends Condition {
    private UserManager mUm;
    private UserHandle mUserHandle;

    public WorkModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mUm = (UserManager) this.mManager.getContext().getSystemService("user");
    }

    private void updateUserHandle() {
        List<UserInfo> profiles = this.mUm.getProfiles(UserHandle.myUserId());
        int profilesCount = profiles.size();
        this.mUserHandle = null;
        for (int i = 0; i < profilesCount; i++) {
            UserInfo userInfo = profiles.get(i);
            if (userInfo.isManagedProfile()) {
                this.mUserHandle = userInfo.getUserHandle();
                return;
            }
        }
    }

    @Override
    public void refreshState() {
        updateUserHandle();
        setActive(this.mUserHandle != null ? this.mUm.isQuietModeEnabled(this.mUserHandle) : false);
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(this.mManager.getContext(), R.drawable.ic_signal_workmode_enable);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_work_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_work_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_on)};
    }

    @Override
    public void onPrimaryClick() {
        this.mManager.getContext().startActivity(new Intent(this.mManager.getContext(), (Class<?>) Settings.AccountSettingsActivity.class));
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            this.mUm.trySetQuietModeDisabled(this.mUserHandle.getIdentifier(), null);
            setActive(false);
            return;
        }
        throw new IllegalArgumentException("Unexpected index " + index);
    }

    @Override
    public int getMetricsConstant() {
        return 383;
    }
}
