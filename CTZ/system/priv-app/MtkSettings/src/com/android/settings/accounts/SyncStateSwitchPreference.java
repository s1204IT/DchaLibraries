package com.android.settings.accounts;

import android.accounts.Account;
import android.app.ActivityManager;
import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.widget.AnimatedImageView;

/* loaded from: classes.dex */
public class SyncStateSwitchPreference extends SwitchPreference {
    private Account mAccount;
    private String mAuthority;
    private boolean mFailed;
    private boolean mIsActive;
    private boolean mIsPending;
    private boolean mOneTimeSyncMode;
    private String mPackageName;
    private int mUid;

    public SyncStateSwitchPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0, R.style.SyncSwitchPreference);
        this.mIsActive = false;
        this.mIsPending = false;
        this.mFailed = false;
        this.mOneTimeSyncMode = false;
        this.mAccount = null;
        this.mAuthority = null;
        this.mPackageName = null;
        this.mUid = 0;
    }

    public SyncStateSwitchPreference(Context context, Account account, String str, String str2, int i) {
        super(context, null, 0, R.style.SyncSwitchPreference);
        this.mIsActive = false;
        this.mIsPending = false;
        this.mFailed = false;
        this.mOneTimeSyncMode = false;
        setup(account, str, str2, i);
    }

    public void setup(Account account, String str, String str2, int i) {
        this.mAccount = account;
        this.mAuthority = str;
        this.mPackageName = str2;
        this.mUid = i;
        setVisible(!TextUtils.isEmpty(this.mAuthority));
        notifyChanged();
    }

    @Override // android.support.v14.preference.SwitchPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        AnimatedImageView animatedImageView = (AnimatedImageView) preferenceViewHolder.findViewById(R.id.sync_active);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.sync_failed);
        boolean z = this.mIsActive || this.mIsPending;
        animatedImageView.setVisibility(z ? 0 : 8);
        animatedImageView.setAnimating(this.mIsActive);
        viewFindViewById.setVisibility(this.mFailed && !z ? 0 : 8);
        View viewFindViewById2 = preferenceViewHolder.findViewById(android.R.id.switch_widget);
        if (this.mOneTimeSyncMode) {
            viewFindViewById2.setVisibility(8);
            ((TextView) preferenceViewHolder.findViewById(android.R.id.summary)).setText(getContext().getString(R.string.sync_one_time_sync, getSummary()));
        } else {
            viewFindViewById2.setVisibility(0);
        }
    }

    public void setActive(boolean z) {
        this.mIsActive = z;
        notifyChanged();
    }

    public void setPending(boolean z) {
        this.mIsPending = z;
        notifyChanged();
    }

    public void setFailed(boolean z) {
        this.mFailed = z;
        notifyChanged();
    }

    public void setOneTimeSyncMode(boolean z) {
        this.mOneTimeSyncMode = z;
        notifyChanged();
    }

    public boolean isOneTimeSyncMode() {
        return this.mOneTimeSyncMode;
    }

    @Override // android.support.v7.preference.TwoStatePreference, android.support.v7.preference.Preference
    protected void onClick() {
        if (!this.mOneTimeSyncMode) {
            if (ActivityManager.isUserAMonkey()) {
                Log.d("SyncState", "ignoring monkey's attempt to flip sync state");
            } else {
                super.onClick();
            }
        }
    }

    public Account getAccount() {
        return this.mAccount;
    }

    public String getAuthority() {
        return this.mAuthority;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getUid() {
        return this.mUid;
    }
}
