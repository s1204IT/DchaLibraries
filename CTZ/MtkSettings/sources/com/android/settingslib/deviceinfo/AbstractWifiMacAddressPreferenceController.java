package com.android.settingslib.deviceinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settingslib.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public abstract class AbstractWifiMacAddressPreferenceController extends AbstractConnectivityPreferenceController {
    private static final String[] CONNECTIVITY_INTENTS = {"android.net.conn.CONNECTIVITY_CHANGE", "android.net.wifi.LINK_CONFIGURATION_CHANGED", "android.net.wifi.STATE_CHANGE"};
    static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private Preference mWifiMacAddress;
    private final WifiManager mWifiManager;

    public AbstractWifiMacAddressPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        this.mWifiManager = (WifiManager) context.getSystemService(WifiManager.class);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WIFI_MAC_ADDRESS;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mWifiMacAddress = preferenceScreen.findPreference(KEY_WIFI_MAC_ADDRESS);
        updateConnectivity();
    }

    @Override
    protected String[] getConnectivityIntents() {
        return CONNECTIVITY_INTENTS;
    }

    @Override
    @SuppressLint({"HardwareIds"})
    protected void updateConnectivity() {
        WifiInfo connectionInfo = this.mWifiManager.getConnectionInfo();
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_connected_mac_randomization_enabled", 0);
        String macAddress = connectionInfo == null ? null : connectionInfo.getMacAddress();
        if (TextUtils.isEmpty(macAddress)) {
            this.mWifiMacAddress.setSummary(R.string.status_unavailable);
        } else if (i == 1 && "02:00:00:00:00:00".equals(macAddress)) {
            this.mWifiMacAddress.setSummary(R.string.wifi_status_mac_randomized);
        } else {
            this.mWifiMacAddress.setSummary(macAddress);
        }
    }
}
