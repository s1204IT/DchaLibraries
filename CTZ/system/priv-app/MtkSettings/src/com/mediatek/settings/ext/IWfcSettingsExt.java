package com.mediatek.settings.ext;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;

/* loaded from: classes.dex */
public interface IWfcSettingsExt {
    void addOtherCustomPreference();

    void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen);

    String getWfcSummary(Context context, int i);

    void initPlugin(PreferenceFragment preferenceFragment);

    boolean isWifiCallingProvisioned(Context context, int i);

    void onWfcSettingsEvent(int i);

    void onWirelessSettingsEvent(int i);

    boolean showWfcTetheringAlertDialog(Context context);

    void updateWfcModePreference(PreferenceScreen preferenceScreen, ListPreference listPreference, boolean z, int i);
}
