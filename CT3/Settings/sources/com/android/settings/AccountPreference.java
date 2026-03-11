package com.android.settings;

import android.accounts.Account;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.widget.ImageView;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;

public class AccountPreference extends Preference {
    private Account mAccount;
    private ArrayList<String> mAuthorities;
    private boolean mShowTypeIcon;
    private int mStatus;
    private ImageView mSyncStatusIcon;

    public AccountPreference(Context context, Account account, Drawable icon, ArrayList<String> authorities, boolean showTypeIcon) {
        super(context);
        this.mAccount = account;
        this.mAuthorities = authorities;
        this.mShowTypeIcon = showTypeIcon;
        if (showTypeIcon) {
            setIcon(icon);
        } else {
            setIcon(getSyncStatusIcon(1));
        }
        setTitle(this.mAccount.name);
        setSummary("");
        setPersistent(false);
        setSyncStatus(1, false);
    }

    public Account getAccount() {
        return this.mAccount;
    }

    public ArrayList<String> getAuthorities() {
        return this.mAuthorities;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (this.mShowTypeIcon) {
            return;
        }
        this.mSyncStatusIcon = (ImageView) view.findViewById(android.R.id.icon);
        this.mSyncStatusIcon.setImageResource(getSyncStatusIcon(this.mStatus));
        this.mSyncStatusIcon.setContentDescription(getSyncContentDescription(this.mStatus));
    }

    public void setSyncStatus(int status, boolean updateSummary) {
        this.mStatus = status;
        if (!this.mShowTypeIcon && this.mSyncStatusIcon != null) {
            this.mSyncStatusIcon.setImageResource(getSyncStatusIcon(status));
            this.mSyncStatusIcon.setContentDescription(getSyncContentDescription(this.mStatus));
        }
        if (!updateSummary) {
            return;
        }
        setSummary(getSyncStatusMessage(status));
    }

    private int getSyncStatusMessage(int status) {
        switch (status) {
            case DefaultWfcSettingsExt.RESUME:
                return R.string.sync_enabled;
            case DefaultWfcSettingsExt.PAUSE:
                return R.string.sync_disabled;
            case DefaultWfcSettingsExt.CREATE:
                return R.string.sync_error;
            case DefaultWfcSettingsExt.DESTROY:
                return R.string.sync_in_progress;
            default:
                Log.e("AccountPreference", "Unknown sync status: " + status);
                return R.string.sync_error;
        }
    }

    private int getSyncStatusIcon(int status) {
        switch (status) {
            case DefaultWfcSettingsExt.RESUME:
            case DefaultWfcSettingsExt.DESTROY:
                return R.drawable.ic_settings_sync;
            case DefaultWfcSettingsExt.PAUSE:
                return R.drawable.ic_sync_grey_holo;
            case DefaultWfcSettingsExt.CREATE:
                return R.drawable.ic_sync_red_holo;
            default:
                Log.e("AccountPreference", "Unknown sync status: " + status);
                return R.drawable.ic_sync_red_holo;
        }
    }

    private String getSyncContentDescription(int status) {
        switch (status) {
            case DefaultWfcSettingsExt.RESUME:
                return getContext().getString(R.string.accessibility_sync_enabled);
            case DefaultWfcSettingsExt.PAUSE:
                return getContext().getString(R.string.accessibility_sync_disabled);
            case DefaultWfcSettingsExt.CREATE:
                return getContext().getString(R.string.accessibility_sync_error);
            case DefaultWfcSettingsExt.DESTROY:
                return getContext().getString(R.string.accessibility_sync_in_progress);
            default:
                Log.e("AccountPreference", "Unknown sync status: " + status);
                return getContext().getString(R.string.accessibility_sync_error);
        }
    }
}
