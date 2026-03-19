package com.android.server.wifi;

import android.net.wifi.SupplicantState;
import android.net.wifi.WifiSsid;

public class StateChangeResult {
    String BSSID;
    int networkId;
    SupplicantState state;
    WifiSsid wifiSsid;

    StateChangeResult(int i, WifiSsid wifiSsid, String str, SupplicantState supplicantState) {
        this.state = supplicantState;
        this.wifiSsid = wifiSsid;
        this.BSSID = str;
        this.networkId = i;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(" SSID: ");
        stringBuffer.append(this.wifiSsid.toString());
        stringBuffer.append(" BSSID: ");
        stringBuffer.append(this.BSSID);
        stringBuffer.append(" nid: ");
        stringBuffer.append(this.networkId);
        stringBuffer.append(" state: ");
        stringBuffer.append(this.state);
        return stringBuffer.toString();
    }
}
