package com.mediatek.settings.ext;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.view.View;

/* loaded from: classes.dex */
public class DefaultWifiTetherSettingsExt implements IWifiTetherSettingsExt {
    private static final String TAG = "DefaultWifiTetherSettingsExt";
    private Context mContext;

    public DefaultWifiTetherSettingsExt(Context context) {
        this.mContext = context;
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void customizePreference(Object obj) {
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void addPreferenceController(Context context, Object obj, Object obj2) {
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void onPrefChangeNotify(String str, Object obj) {
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void customizeView(Context context, View view, WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void updateConfig(WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void setApChannel(int i, boolean z) {
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void addAllowedDeviceListPreference(Object obj) {
    }

    @Override // com.mediatek.settings.ext.IWifiTetherSettingsExt
    public void launchAllowedDeviceActivity(Object obj) {
    }
}
