package com.mediatek.settings.ext;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.view.View;

/* loaded from: classes.dex */
public interface IWifiTetherSettingsExt {
    void addAllowedDeviceListPreference(Object obj);

    void addPreferenceController(Context context, Object obj, Object obj2);

    void customizePreference(Object obj);

    void customizeView(Context context, View view, WifiConfiguration wifiConfiguration);

    void launchAllowedDeviceActivity(Object obj);

    void onPrefChangeNotify(String str, Object obj);

    void setApChannel(int i, boolean z);

    void updateConfig(WifiConfiguration wifiConfiguration);
}
