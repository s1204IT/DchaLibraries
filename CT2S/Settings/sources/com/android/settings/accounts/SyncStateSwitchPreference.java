package com.android.settings.accounts;

import android.accounts.Account;
import android.app.ActivityManager;
import android.content.Context;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.widget.AnimatedImageView;

public class SyncStateSwitchPreference extends SwitchPreference {
    private Account mAccount;
    private String mAuthority;
    private boolean mFailed;
    private boolean mIsActive;
    private boolean mIsPending;
    private boolean mOneTimeSyncMode;

    public SyncStateSwitchPreference(Context context, Account account, String authority) {
        super(context, null);
        this.mIsActive = false;
        this.mIsPending = false;
        this.mFailed = false;
        this.mOneTimeSyncMode = false;
        this.mAccount = account;
        this.mAuthority = authority;
        setWidgetLayoutResource(R.layout.preference_widget_sync_toggle);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        AnimatedImageView syncActiveView = (AnimatedImageView) view.findViewById(R.id.sync_active);
        View syncFailedView = view.findViewById(R.id.sync_failed);
        boolean activeVisible = this.mIsActive || this.mIsPending;
        syncActiveView.setVisibility(activeVisible ? 0 : 8);
        syncActiveView.setAnimating(this.mIsActive);
        boolean failedVisible = this.mFailed && !activeVisible;
        syncFailedView.setVisibility(failedVisible ? 0 : 8);
        View switchView = view.findViewById(android.R.id.home_panel);
        if (this.mOneTimeSyncMode) {
            switchView.setVisibility(8);
            TextView summary = (TextView) view.findViewById(android.R.id.summary);
            summary.setText(getContext().getString(R.string.sync_one_time_sync, getSummary()));
            return;
        }
        switchView.setVisibility(0);
    }

    public void setActive(boolean isActive) {
        this.mIsActive = isActive;
        notifyChanged();
    }

    public void setPending(boolean isPending) {
        this.mIsPending = isPending;
        notifyChanged();
    }

    public void setFailed(boolean failed) {
        this.mFailed = failed;
        notifyChanged();
    }

    public void setOneTimeSyncMode(boolean oneTimeSyncMode) {
        this.mOneTimeSyncMode = oneTimeSyncMode;
        notifyChanged();
    }

    public boolean isOneTimeSyncMode() {
        return this.mOneTimeSyncMode;
    }

    @Override
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
}
