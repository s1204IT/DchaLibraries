package com.android.settings.accounts;

import android.R;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.widget.ImageView;

public class AccountPreference extends Preference {
    private boolean mShowTypeIcon;
    private int mStatus;
    private ImageView mSyncStatusIcon;

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (!this.mShowTypeIcon) {
            this.mSyncStatusIcon = (ImageView) preferenceViewHolder.findViewById(R.id.icon);
            this.mSyncStatusIcon.setImageResource(getSyncStatusIcon(this.mStatus));
            this.mSyncStatusIcon.setContentDescription(getSyncContentDescription(this.mStatus));
        }
    }

    private int getSyncStatusIcon(int i) {
        switch (i) {
            case 0:
            case 3:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                Log.e("AccountPreference", "Unknown sync status: " + i);
                break;
        }
        return com.android.settings.R.drawable.ic_sync_red_holo;
    }

    private String getSyncContentDescription(int i) {
        switch (i) {
            case 0:
                return getContext().getString(com.android.settings.R.string.accessibility_sync_enabled);
            case 1:
                return getContext().getString(com.android.settings.R.string.accessibility_sync_disabled);
            case 2:
                return getContext().getString(com.android.settings.R.string.accessibility_sync_error);
            case 3:
                return getContext().getString(com.android.settings.R.string.accessibility_sync_in_progress);
            default:
                Log.e("AccountPreference", "Unknown sync status: " + i);
                return getContext().getString(com.android.settings.R.string.accessibility_sync_error);
        }
    }
}
