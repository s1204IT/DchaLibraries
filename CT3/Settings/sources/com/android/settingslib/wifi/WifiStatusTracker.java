package com.android.settingslib.wifi;

import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.util.List;

public class WifiStatusTracker {
    public boolean connected;
    public boolean enabled;
    public int level;
    private final WifiManager mWifiManager;
    public int rssi;
    public String ssid;

    public WifiStatusTracker(WifiManager wifiManager) {
        this.mWifiManager = wifiManager;
    }

    public void handleBroadcast(Intent intent) {
        WifiInfo info;
        String action = intent.getAction();
        if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
            this.enabled = intent.getIntExtra("wifi_state", 4) == 3;
            return;
        }
        if (action.equals("android.net.wifi.STATE_CHANGE")) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            this.connected = networkInfo != null ? networkInfo.isConnected() : false;
            if (this.connected) {
                if (intent.getParcelableExtra("wifiInfo") != null) {
                    info = (WifiInfo) intent.getParcelableExtra("wifiInfo");
                } else {
                    info = this.mWifiManager.getConnectionInfo();
                }
                if (info != null) {
                    this.ssid = getSsid(info);
                    return;
                } else {
                    this.ssid = null;
                    return;
                }
            }
            if (this.connected) {
                return;
            }
            this.ssid = null;
            return;
        }
        if (!action.equals("android.net.wifi.RSSI_CHANGED")) {
            return;
        }
        this.rssi = intent.getIntExtra("newRssi", -200);
        this.level = WifiManager.calculateSignalLevel(this.rssi, 5);
    }

    private String getSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        List<WifiConfiguration> networks = this.mWifiManager.getConfiguredNetworks();
        int length = networks.size();
        for (int i = 0; i < length; i++) {
            if (networks.get(i).networkId == info.getNetworkId()) {
                return networks.get(i).SSID;
            }
        }
        return null;
    }
}
