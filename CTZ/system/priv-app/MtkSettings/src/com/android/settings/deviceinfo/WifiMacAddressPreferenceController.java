package com.android.settings.deviceinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.deviceinfo.AbstractWifiMacAddressPreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;

/* loaded from: classes.dex */
public class WifiMacAddressPreferenceController extends AbstractWifiMacAddressPreferenceController implements PreferenceControllerMixin {
    private ISettingsMiscExt mExt;
    private Preference mWifiMacAddressPreference;
    private final WifiManager mWifiManager;

    public WifiMacAddressPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        this.mWifiManager = (WifiManager) context.getSystemService(WifiManager.class);
        this.mExt = UtilsExt.getMiscPlugin(this.mContext);
    }

    @Override // com.android.settingslib.deviceinfo.AbstractWifiMacAddressPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_wifi_mac_address);
    }

    @Override // com.android.settingslib.deviceinfo.AbstractWifiMacAddressPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mWifiMacAddressPreference = preferenceScreen.findPreference("wifi_mac_address");
        updateConnectivity();
    }

    @Override // com.android.settingslib.deviceinfo.AbstractWifiMacAddressPreferenceController, com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController
    @SuppressLint({"HardwareIds"})
    protected void updateConnectivity() {
        String str;
        WifiInfo connectionInfo = this.mWifiManager.getConnectionInfo();
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_connected_mac_randomization_enabled", 0);
        String macAddress = connectionInfo == null ? null : connectionInfo.getMacAddress();
        if (macAddress == null) {
            str = "deviceinfo macAddress = null";
        } else {
            str = "deviceinfo " + macAddress;
        }
        Log.i("mtk81234", str);
        if (this.mWifiMacAddressPreference == null) {
            return;
        }
        if (TextUtils.isEmpty(macAddress)) {
            this.mWifiMacAddressPreference.setSummary(R.string.status_unavailable);
        } else if (i == 1 && "02:00:00:00:00:00".equals(macAddress)) {
            this.mWifiMacAddressPreference.setSummary(R.string.wifi_status_mac_randomized);
        } else {
            this.mWifiMacAddressPreference.setSummary(this.mExt.customizeMacAddressString(macAddress, this.mContext.getString(R.string.status_unavailable)));
        }
    }
}
