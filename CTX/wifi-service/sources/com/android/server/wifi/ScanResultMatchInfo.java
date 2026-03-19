package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.android.server.wifi.util.ScanResultUtil;
import com.mediatek.server.wifi.MtkWapi;
import java.util.Objects;

public class ScanResultMatchInfo {
    public static final int NETWORK_TYPE_EAP = 3;
    public static final int NETWORK_TYPE_OPEN = 0;
    public static final int NETWORK_TYPE_PSK = 2;
    public static final int NETWORK_TYPE_WAPI = 4;
    public static final int NETWORK_TYPE_WEP = 1;
    public String networkSsid;
    public int networkType;

    public static ScanResultMatchInfo fromWifiConfiguration(WifiConfiguration wifiConfiguration) {
        ScanResultMatchInfo scanResultMatchInfo = new ScanResultMatchInfo();
        scanResultMatchInfo.networkSsid = wifiConfiguration.SSID;
        if (WifiConfigurationUtil.isConfigForPskNetwork(wifiConfiguration)) {
            scanResultMatchInfo.networkType = 2;
        } else if (WifiConfigurationUtil.isConfigForEapNetwork(wifiConfiguration)) {
            scanResultMatchInfo.networkType = 3;
        } else if (WifiConfigurationUtil.isConfigForWepNetwork(wifiConfiguration)) {
            scanResultMatchInfo.networkType = 1;
        } else if (WifiConfigurationUtil.isConfigForOpenNetwork(wifiConfiguration)) {
            scanResultMatchInfo.networkType = 0;
        } else if (MtkWapi.isConfigForWapiNetwork(wifiConfiguration)) {
            scanResultMatchInfo.networkType = 4;
        } else {
            throw new IllegalArgumentException("Invalid WifiConfiguration: " + wifiConfiguration);
        }
        return scanResultMatchInfo;
    }

    public static ScanResultMatchInfo fromScanResult(ScanResult scanResult) {
        ScanResultMatchInfo scanResultMatchInfo = new ScanResultMatchInfo();
        scanResultMatchInfo.networkSsid = ScanResultUtil.createQuotedSSID(scanResult.SSID);
        if (MtkWapi.isScanResultForWapiNetwork(scanResult)) {
            scanResultMatchInfo.networkType = 4;
        } else if (ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
            scanResultMatchInfo.networkType = 2;
        } else if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
            scanResultMatchInfo.networkType = 3;
        } else if (ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
            scanResultMatchInfo.networkType = 1;
        } else if (ScanResultUtil.isScanResultForOpenNetwork(scanResult)) {
            scanResultMatchInfo.networkType = 0;
        } else {
            throw new IllegalArgumentException("Invalid ScanResult: " + scanResult);
        }
        return scanResultMatchInfo;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ScanResultMatchInfo)) {
            return false;
        }
        ScanResultMatchInfo scanResultMatchInfo = (ScanResultMatchInfo) obj;
        return Objects.equals(this.networkSsid, scanResultMatchInfo.networkSsid) && this.networkType == scanResultMatchInfo.networkType;
    }

    public int hashCode() {
        return Objects.hash(this.networkSsid, Integer.valueOf(this.networkType));
    }

    public String toString() {
        return "ScanResultMatchInfo: " + this.networkSsid + ", type: " + this.networkType;
    }
}
