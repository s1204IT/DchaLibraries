package com.android.server.wifi;

import android.net.wifi.WifiConfiguration;

public class SoftApModeConfiguration {
    final WifiConfiguration mConfig;
    final int mTargetMode;

    SoftApModeConfiguration(int i, WifiConfiguration wifiConfiguration) {
        this.mTargetMode = i;
        this.mConfig = wifiConfiguration;
    }

    public int getTargetMode() {
        return this.mTargetMode;
    }

    public WifiConfiguration getWifiConfiguration() {
        return this.mConfig;
    }
}
