package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.widget.ImageView;

public interface ISettingsMiscExt {
    void addCustomizedItem(Object obj, Boolean bool);

    void addPreferenceController(Object obj, Object obj2);

    Object createPreferenceController(Context context, Object obj);

    void customizeAGPRS(PreferenceScreen preferenceScreen);

    void customizeDashboardTile(Object obj, ImageView imageView);

    String customizeMacAddressString(String str, String str2);

    String customizeSimDisplayString(String str, int i);

    boolean doUpdateTilesList(Activity activity, boolean z, boolean z2);

    void doWosFactoryReset();

    String getNetworktypeString(String str, int i);

    void initCustomizedLocationSettings(PreferenceScreen preferenceScreen, int i);

    boolean isWifiOnlyModeSet();

    void setFactoryResetTitle(Object obj);

    void setTimeoutPrefTitle(Preference preference);

    void updateCustomizedLocationSettings();
}
