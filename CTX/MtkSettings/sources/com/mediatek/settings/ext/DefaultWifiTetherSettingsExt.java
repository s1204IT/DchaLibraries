package com.mediatek.settings.ext;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.view.View;

public class DefaultWifiTetherSettingsExt implements IWifiTetherSettingsExt {
    private static final String TAG = "DefaultWifiTetherSettingsExt";
    private Context mContext;

    public DefaultWifiTetherSettingsExt(Context context) {
        this.mContext = context;
    }

    @Override
    public void customizePreference(Object obj) {
    }

    @Override
    public void addPreferenceController(Context context, Object obj, Object obj2) {
    }

    @Override
    public void onPrefChangeNotify(String str, Object obj) {
    }

    @Override
    public void customizeView(Context context, View view, WifiConfiguration wifiConfiguration) {
    }

    @Override
    public void updateConfig(WifiConfiguration wifiConfiguration) {
    }

    @Override
    public void setApChannel(int i, boolean z) {
    }

    @Override
    public void addAllowedDeviceListPreference(Object obj) {
    }

    @Override
    public void launchAllowedDeviceActivity(Object obj) {
    }
}
