package com.android.managedprovisioning.task.wifi;

import android.net.IpConfiguration;
import android.net.ProxyInfo;
import android.net.wifi.WifiConfiguration;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.model.WifiInfo;
import com.android.managedprovisioning.preprovisioning.EncryptionController;

public class WifiConfigurationProvider {

    @VisibleForTesting
    static final String NONE = "NONE";

    @VisibleForTesting
    static final String WEP = "WEP";

    @VisibleForTesting
    static final String WPA = "WPA";

    public WifiConfiguration generateWifiConfiguration(WifiInfo wifiInfo) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = wifiInfo.ssid;
        wifiConfiguration.status = 2;
        wifiConfiguration.hiddenSSID = wifiInfo.hidden;
        byte b = 1;
        wifiConfiguration.userApproved = 1;
        String str = wifiInfo.securityType != null ? wifiInfo.securityType : NONE;
        int iHashCode = str.hashCode();
        if (iHashCode != 85826) {
            b = (iHashCode == 86152 && str.equals(WPA)) ? (byte) 0 : (byte) -1;
        } else if (!str.equals(WEP)) {
        }
        switch (b) {
            case 0:
                updateForWPAConfiguration(wifiConfiguration, wifiInfo.password);
                break;
            case EncryptionController.NOTIFICATION_ID:
                updateForWEPConfiguration(wifiConfiguration, wifiInfo.password);
                break;
            default:
                wifiConfiguration.allowedKeyManagement.set(0);
                wifiConfiguration.allowedAuthAlgorithms.set(0);
                break;
        }
        updateForProxy(wifiConfiguration, wifiInfo.proxyHost, wifiInfo.proxyPort, wifiInfo.proxyBypassHosts, wifiInfo.pacUrl);
        return wifiConfiguration;
    }

    private void updateForWPAConfiguration(WifiConfiguration wifiConfiguration, String str) {
        wifiConfiguration.allowedKeyManagement.set(1);
        wifiConfiguration.allowedAuthAlgorithms.set(0);
        wifiConfiguration.allowedProtocols.set(0);
        wifiConfiguration.allowedProtocols.set(1);
        wifiConfiguration.allowedPairwiseCiphers.set(1);
        wifiConfiguration.allowedPairwiseCiphers.set(2);
        wifiConfiguration.allowedGroupCiphers.set(2);
        wifiConfiguration.allowedGroupCiphers.set(3);
        if (!TextUtils.isEmpty(str)) {
            wifiConfiguration.preSharedKey = "\"" + str + "\"";
        }
    }

    private void updateForWEPConfiguration(WifiConfiguration wifiConfiguration, String str) {
        wifiConfiguration.allowedKeyManagement.set(0);
        wifiConfiguration.allowedAuthAlgorithms.set(0);
        wifiConfiguration.allowedAuthAlgorithms.set(1);
        wifiConfiguration.allowedGroupCiphers.set(0);
        wifiConfiguration.allowedGroupCiphers.set(1);
        wifiConfiguration.allowedGroupCiphers.set(2);
        wifiConfiguration.allowedGroupCiphers.set(3);
        int length = str.length();
        if ((length == 10 || length == 26 || length == 58) && str.matches("[0-9A-Fa-f]*")) {
            wifiConfiguration.wepKeys[0] = str;
        } else {
            wifiConfiguration.wepKeys[0] = '\"' + str + '\"';
        }
        wifiConfiguration.wepTxKeyIndex = 0;
    }

    private void updateForProxy(WifiConfiguration wifiConfiguration, String str, int i, String str2, String str3) {
        if (TextUtils.isEmpty(str) && TextUtils.isEmpty(str3)) {
            return;
        }
        if (!TextUtils.isEmpty(str)) {
            wifiConfiguration.setProxy(IpConfiguration.ProxySettings.STATIC, new ProxyInfo(str, i, str2));
        } else {
            wifiConfiguration.setProxy(IpConfiguration.ProxySettings.PAC, new ProxyInfo(str3));
        }
    }
}
