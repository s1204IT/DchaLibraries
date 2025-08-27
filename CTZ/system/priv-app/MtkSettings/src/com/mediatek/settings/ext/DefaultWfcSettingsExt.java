package com.mediatek.settings.ext;

import android.R;
import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.ims.ImsManager;

/* loaded from: classes.dex */
public class DefaultWfcSettingsExt implements IWfcSettingsExt {
    public static final int CONFIG_CHANGE = 4;
    public static final int CREATE = 2;
    public static final int DESTROY = 3;
    public static final int PAUSE = 1;
    public static final int RESUME = 0;
    private static final String TAG = "DefaultWfcSettingsExt";

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public void initPlugin(PreferenceFragment preferenceFragment) {
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public String getWfcSummary(Context context, int i) {
        return context.getResources().getString(i);
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public void onWirelessSettingsEvent(int i) {
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public void onWfcSettingsEvent(int i) {
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public void addOtherCustomPreference() {
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public void updateWfcModePreference(PreferenceScreen preferenceScreen, ListPreference listPreference, boolean z, int i) {
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public boolean showWfcTetheringAlertDialog(Context context) {
        return false;
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWfcSettingsExt
    public boolean isWifiCallingProvisioned(Context context, int i) {
        Log.d(TAG, "in default: isWifiCallingProvisioned");
        return true;
    }

    private int getWfcModeSummary(Context context, int i) {
        if (ImsManager.isWfcEnabledByUser(context)) {
            switch (i) {
                case 0:
                    return R.string.org_name;
                case 1:
                    return R.string.orgTypeWork;
                case 2:
                    return R.string.org_unit;
                default:
                    Log.e(TAG, "Unexpected WFC mode value: " + i);
                    break;
            }
        }
        return R.string.permdesc_answerPhoneCalls;
    }
}
