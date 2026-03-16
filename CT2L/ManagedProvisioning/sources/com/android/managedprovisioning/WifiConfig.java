package com.android.managedprovisioning;

import android.net.IpConfiguration;
import android.net.ProxyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

public class WifiConfig {
    private final WifiManager mWifiManager;

    enum SecurityType {
        NONE,
        WPA,
        WEP
    }

    public WifiConfig(WifiManager manager) {
        this.mWifiManager = manager;
    }

    public int addNetwork(String ssid, boolean hidden, String type, String password, String proxyHost, int proxyPort, String proxyBypassHosts, String pacUrl) {
        SecurityType securityType;
        if (!this.mWifiManager.isWifiEnabled()) {
            this.mWifiManager.setWifiEnabled(true);
        }
        WifiConfiguration wifiConf = new WifiConfiguration();
        if (type == null || TextUtils.isEmpty(type)) {
            securityType = SecurityType.NONE;
        } else {
            securityType = (SecurityType) Enum.valueOf(SecurityType.class, type.toUpperCase());
        }
        if (securityType.equals(SecurityType.NONE) && !TextUtils.isEmpty(password)) {
            securityType = SecurityType.WPA;
        }
        wifiConf.SSID = ssid;
        wifiConf.status = 2;
        wifiConf.hiddenSSID = hidden;
        switch (securityType) {
            case NONE:
                wifiConf.allowedKeyManagement.set(0);
                wifiConf.allowedAuthAlgorithms.set(0);
                break;
            case WPA:
                updateForWPAConfiguration(wifiConf, password);
                break;
            case WEP:
                updateForWEPConfiguration(wifiConf, password);
                break;
        }
        updateForProxy(wifiConf, proxyHost, proxyPort, proxyBypassHosts, pacUrl);
        int netId = this.mWifiManager.addNetwork(wifiConf);
        if (netId != -1) {
            this.mWifiManager.enableNetwork(netId, true);
            this.mWifiManager.saveConfiguration();
        }
        return netId;
    }

    protected void updateForWPAConfiguration(WifiConfiguration wifiConf, String wifiPassword) {
        wifiConf.allowedKeyManagement.set(1);
        wifiConf.allowedAuthAlgorithms.set(0);
        wifiConf.allowedProtocols.set(0);
        wifiConf.allowedProtocols.set(1);
        wifiConf.allowedPairwiseCiphers.set(1);
        wifiConf.allowedPairwiseCiphers.set(2);
        wifiConf.allowedGroupCiphers.set(2);
        wifiConf.allowedGroupCiphers.set(3);
        if (!TextUtils.isEmpty(wifiPassword)) {
            wifiConf.preSharedKey = "\"" + wifiPassword + "\"";
        }
    }

    protected void updateForWEPConfiguration(WifiConfiguration wifiConf, String password) {
        wifiConf.allowedKeyManagement.set(0);
        wifiConf.allowedAuthAlgorithms.set(0);
        wifiConf.allowedAuthAlgorithms.set(1);
        wifiConf.allowedGroupCiphers.set(0);
        wifiConf.allowedGroupCiphers.set(1);
        wifiConf.allowedGroupCiphers.set(2);
        wifiConf.allowedGroupCiphers.set(3);
        int length = password.length();
        if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
            wifiConf.wepKeys[0] = password;
        } else {
            wifiConf.wepKeys[0] = '\"' + password + '\"';
        }
        wifiConf.wepTxKeyIndex = 0;
    }

    private void updateForProxy(WifiConfiguration wifiConf, String proxyHost, int proxyPort, String proxyBypassHosts, String pacUrl) {
        if (!TextUtils.isEmpty(proxyHost) || !TextUtils.isEmpty(pacUrl)) {
            if (!TextUtils.isEmpty(proxyHost)) {
                ProxyInfo proxy = new ProxyInfo(proxyHost, proxyPort, proxyBypassHosts);
                wifiConf.setProxy(IpConfiguration.ProxySettings.STATIC, proxy);
            } else {
                ProxyInfo proxy2 = new ProxyInfo(pacUrl);
                wifiConf.setProxy(IpConfiguration.ProxySettings.PAC, proxy2);
            }
        }
    }
}
