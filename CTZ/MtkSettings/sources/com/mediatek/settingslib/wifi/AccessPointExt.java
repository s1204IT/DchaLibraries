package com.mediatek.settingslib.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.android.settingslib.R;

public class AccessPointExt {
    public AccessPointExt(Context context) {
    }

    public static int getSecurity(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.allowedKeyManagement.get(8)) {
            return 4;
        }
        if (wifiConfiguration.allowedKeyManagement.get(9)) {
            return 5;
        }
        return -1;
    }

    public static int getSecurity(ScanResult scanResult) {
        if (scanResult.capabilities.contains("WAPI-PSK")) {
            return 4;
        }
        if (scanResult.capabilities.contains("WAPI-CERT")) {
            return 5;
        }
        return -1;
    }

    public String getSecurityString(int i, Context context) {
        switch (i) {
            case 4:
                return context.getString(R.string.wifi_security_wapi_psk);
            case 5:
                return context.getString(R.string.wifi_security_wapi_certificate);
            default:
                return null;
        }
    }
}
