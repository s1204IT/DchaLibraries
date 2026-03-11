package com.mediatek.settings.ext;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.widget.ImageView;

public interface ISettingsMiscExt {
    void addCustomizedItem(Object obj, Boolean bool);

    void customizeDashboardTile(Object obj, ImageView imageView);

    String customizeMacAddressString(String str, String str2);

    String customizeSimDisplayString(String str, int i);

    String getNetworktypeString(String str, int i);

    void initCustomizedLocationSettings(PreferenceScreen preferenceScreen, int i);

    boolean isWifiOnlyModeSet();

    void setFactoryResetTitle(Object obj);

    void setTimeoutPrefTitle(Preference preference);

    void updateCustomizedLocationSettings();
}
