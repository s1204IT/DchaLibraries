package com.mediatek.settings.ext;

import android.R;
import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.ims.ImsManager;

public class DefaultWfcSettingsExt implements IWfcSettingsExt {
    public static final int CONFIG_CHANGE = 4;
    public static final int CREATE = 2;
    public static final int DESTROY = 3;
    public static final int PAUSE = 1;
    public static final int RESUME = 0;
    private static final String TAG = "DefaultWfcSettingsExt";

    @Override
    public void initPlugin(PreferenceFragment pf) {
    }

    @Override
    public String getWfcSummary(Context context, int defaultSummaryResId) {
        return context.getResources().getString(defaultSummaryResId);
    }

    @Override
    public void onWirelessSettingsEvent(int event) {
    }

    @Override
    public void onWfcSettingsEvent(int event) {
    }

    @Override
    public void addOtherCustomPreference() {
    }

    @Override
    public void updateWfcModePreference(PreferenceScreen root, ListPreference wfcModePref, boolean wfcEnabled, int wfcMode) {
    }

    @Override
    public boolean showWfcTetheringAlertDialog(Context context) {
        return false;
    }

    @Override
    public void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen) {
    }

    @Override
    public boolean isWifiCallingProvisioned(Context context, int phoneId) {
        Log.d(TAG, "in default: isWifiCallingProvisioned");
        return true;
    }

    private int getWfcModeSummary(Context context, int wfcMode) {
        if (!ImsManager.isWfcEnabledByUser(context)) {
            return R.string.accessibility_autoclick_type_settings_panel_title;
        }
        switch (wfcMode) {
            case RESUME:
                break;
            case PAUSE:
                break;
            case CREATE:
                break;
            default:
                Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
                break;
        }
        return R.string.accessibility_autoclick_type_settings_panel_title;
    }
}
