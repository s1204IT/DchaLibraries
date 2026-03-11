package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import jp.co.benesse.dcha.util.Logger;

class AccessPoint extends Preference {
    static final int[] STATE_NONE = new int[0];
    String bssid;
    private WifiConfiguration mConfig;
    private WifiInfo mInfo;
    int mRssi;
    private NetworkInfo.DetailedState mState;
    int networkId;
    PskType pskType;
    int security;
    String securityMsg;
    String ssid;
    boolean wpsAvailable;

    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    static int getSecurity(WifiConfiguration config) {
        Logger.d("AccessPoint", "getSecurity 0001");
        if (config.allowedKeyManagement.get(1)) {
            Logger.d("AccessPoint", "getSecurity 0002");
            return 2;
        }
        if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            Logger.d("AccessPoint", "getSecurity 0003");
            return 3;
        }
        Logger.d("AccessPoint", "getSecurity 0004");
        return config.wepKeys[0] == null ? 0 : 1;
    }

    private static int getSecurity(ScanResult result) {
        Logger.d("AccessPoint", "getSecurity 0005");
        if (result.capabilities.contains("WEP")) {
            Logger.d("AccessPoint", "getSecurity 0006");
            return 1;
        }
        if (result.capabilities.contains("PSK")) {
            Logger.d("AccessPoint", "getSecurity 0007");
            return 2;
        }
        if (result.capabilities.contains("EAP")) {
            Logger.d("AccessPoint", "getSecurity 0008");
            return 3;
        }
        Logger.d("AccessPoint", "getSecurity 0009");
        return 0;
    }

    public String getSecurityString(boolean concise) {
        Logger.d("AccessPoint", "getSecurityString 0001");
        Context context = getContext();
        switch (this.security) {
            case 0:
                Logger.d("AccessPoint", "getSecurityString 0009");
                break;
            case 1:
                Logger.d("AccessPoint", "getSecurityString 0008");
                return concise ? context.getString(R.string.wifi_security_short_wep) : context.getString(R.string.wifi_security_wep);
            case 2:
                Logger.d("AccessPoint", "getSecurityString 0002");
                switch (this.pskType) {
                    case WPA:
                        Logger.d("AccessPoint", "getSecurityString 0003");
                        return concise ? context.getString(R.string.wifi_security_short_wpa) : context.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        Logger.d("AccessPoint", "getSecurityString 0004");
                        return concise ? context.getString(R.string.wifi_security_short_wpa2) : context.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        Logger.d("AccessPoint", "getSecurityString 0005");
                        return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) : context.getString(R.string.wifi_security_wpa_wpa2);
                    case UNKNOWN:
                        Logger.d("AccessPoint", "getSecurityString 0006");
                        break;
                }
                Logger.d("AccessPoint", "getSecurityString 0007");
                return concise ? context.getString(R.string.wifi_security_short_psk_generic) : context.getString(R.string.wifi_security_psk_generic);
        }
        Logger.d("AccessPoint", "getSecurityString 0010");
        return concise ? "" : context.getString(R.string.wifi_security_none);
    }

    private static PskType getPskType(ScanResult result) {
        Logger.d("AccessPoint", "getPskType 0001");
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            Logger.d("AccessPoint", "getPskType 0002");
            return PskType.WPA_WPA2;
        }
        if (wpa2) {
            Logger.d("AccessPoint", "getPskType 0003");
            return PskType.WPA2;
        }
        if (wpa) {
            Logger.d("AccessPoint", "getPskType 0004");
            return PskType.WPA;
        }
        Logger.d("AccessPoint", "getPskType 0005");
        Logger.w("AccessPoint", "Received abnormal flag string: " + result.capabilities);
        return PskType.UNKNOWN;
    }

    AccessPoint(Context context, WifiConfiguration config) {
        super(context);
        this.wpsAvailable = false;
        this.pskType = PskType.UNKNOWN;
        Logger.d("AccessPoint", "AccessPoint 0001");
        loadConfig(config);
        refresh();
        Logger.d("AccessPoint", "AccessPoint 0002");
    }

    AccessPoint(Context context, ScanResult result) {
        super(context);
        this.wpsAvailable = false;
        this.pskType = PskType.UNKNOWN;
        Logger.d("AccessPoint", "AccessPoint 0003");
        loadResult(result);
        refresh();
        Logger.d("AccessPoint", "AccessPoint 0004");
    }

    private void loadConfig(WifiConfiguration config) {
        Logger.d("AccessPoint", "loadConfig 0001");
        this.ssid = config.SSID == null ? "" : removeDoubleQuotes(config.SSID);
        this.bssid = config.BSSID;
        this.security = getSecurity(config);
        this.networkId = config.networkId;
        this.mRssi = Integer.MAX_VALUE;
        this.mConfig = config;
        Logger.d("AccessPoint", "loadConfig 0002");
    }

    private void loadResult(ScanResult result) {
        Logger.d("AccessPoint", "loadResult 0001");
        this.ssid = result.SSID;
        this.bssid = result.BSSID;
        this.security = getSecurity(result);
        this.wpsAvailable = this.security != 3 && result.capabilities.contains("WPS");
        if (this.security == 2) {
            Logger.d("AccessPoint", "loadResult 0002");
            this.pskType = getPskType(result);
        }
        this.networkId = -1;
        this.mRssi = result.level;
        Logger.d("AccessPoint", "loadResult 0003");
    }

    @Override
    public int compareTo(Preference preference) {
        Logger.d("AccessPoint", "compareTo 0001");
        if (!(preference instanceof AccessPoint)) {
            Logger.d("AccessPoint", "compareTo 0002");
            return 1;
        }
        AccessPoint other = (AccessPoint) preference;
        if (this.mInfo != null && other.mInfo == null) {
            Logger.d("AccessPoint", "compareTo 0003");
            return -1;
        }
        if (this.mInfo == null && other.mInfo != null) {
            Logger.d("AccessPoint", "compareTo 0004");
            return 1;
        }
        if (this.mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "compareTo 0005");
            return -1;
        }
        if (this.mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "compareTo 0006");
            return 1;
        }
        if (this.networkId != -1 && other.networkId == -1) {
            Logger.d("AccessPoint", "compareTo 0007");
            return -1;
        }
        if (this.networkId == -1 && other.networkId != -1) {
            Logger.d("AccessPoint", "compareTo 0008");
            return 1;
        }
        int difference = WifiManager.compareSignalLevel(other.mRssi, this.mRssi);
        if (difference != 0) {
            Logger.d("AccessPoint", "compareTo 0009");
            return difference;
        }
        Logger.d("AccessPoint", "compareTo 0010");
        return this.ssid.compareToIgnoreCase(other.ssid);
    }

    public boolean equals(Object other) {
        Logger.d("AccessPoint", "equals 0001");
        if (!(other instanceof AccessPoint)) {
            Logger.d("AccessPoint", "equals 0002");
            return false;
        }
        Logger.d("AccessPoint", "equals 0003");
        return compareTo((Preference) other) == 0;
    }

    public int hashCode() {
        Logger.d("AccessPoint", "hashCode 0001");
        int result = 0;
        if (this.mInfo != null) {
            Logger.d("AccessPoint", "hashCode 0002");
            result = 0 + (this.mInfo.hashCode() * 13);
        }
        int result2 = result + (this.mRssi * 19) + (this.networkId * 23) + (this.ssid.hashCode() * 29);
        Logger.d("AccessPoint", "hashCode 0003");
        return result2;
    }

    boolean update(ScanResult result) {
        Logger.d("AccessPoint", "update 0001");
        if (this.ssid.equals(result.SSID) && this.security == getSecurity(result)) {
            Logger.d("AccessPoint", "update 0002");
            if (WifiManager.compareSignalLevel(result.level, this.mRssi) > 0) {
                Logger.d("AccessPoint", "update 0003");
                int oldLevel = getLevel();
                this.mRssi = result.level;
                if (getLevel() != oldLevel) {
                    Logger.d("AccessPoint", "update 0004");
                    notifyChanged();
                }
            }
            if (this.security == 2) {
                Logger.d("AccessPoint", "update 0005");
                this.pskType = getPskType(result);
            }
            refresh();
            Logger.d("AccessPoint", "update 0006");
            return true;
        }
        Logger.d("AccessPoint", "update 0007");
        return false;
    }

    void update(WifiInfo info, NetworkInfo.DetailedState state) {
        Logger.d("AccessPoint", "update 0008");
        boolean reorder = false;
        if (info != null && this.networkId != -1 && this.networkId == info.getNetworkId()) {
            Logger.d("AccessPoint", "update 0009");
            reorder = this.mInfo == null;
            this.mInfo = info;
            this.mState = state;
            refresh();
        } else if (this.mInfo != null) {
            Logger.d("AccessPoint", "update 0010");
            reorder = true;
            this.mInfo = null;
            this.mState = null;
            refresh();
        }
        if (reorder) {
            Logger.d("AccessPoint", "update 0011");
            notifyHierarchyChanged();
        }
        Logger.d("AccessPoint", "update 0012");
    }

    int getLevel() {
        Logger.d("AccessPoint", "getLevel 0001");
        if (this.mRssi == Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "getLevel 0002");
            return -1;
        }
        Logger.d("AccessPoint", "getLevel 0003");
        return WifiManager.calculateSignalLevel(this.mRssi, 4);
    }

    WifiConfiguration getConfig() {
        Logger.d("AccessPoint", "getConfig 0001");
        return this.mConfig;
    }

    WifiInfo getInfo() {
        Logger.d("AccessPoint", "getInfo 0001");
        return this.mInfo;
    }

    NetworkInfo.DetailedState getState() {
        Logger.d("AccessPoint", "getState 0001");
        return this.mState;
    }

    static String removeDoubleQuotes(String string) {
        Logger.d("AccessPoint", "removeDoubleQuotes 0001");
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            Logger.d("AccessPoint", "removeDoubleQuotes 0002");
            return string.substring(1, length - 1);
        }
        Logger.d("AccessPoint", "removeDoubleQuotes 0003");
        return string;
    }

    static String convertToQuotedString(String string) {
        Logger.d("AccessPoint", "convertToQuotedString 0001");
        return "\"" + string + "\"";
    }

    boolean isActive() {
        Logger.d("AccessPoint", "isActive 0001");
        return (this.mState == null || (this.networkId == -1 && this.mState == NetworkInfo.DetailedState.DISCONNECTED)) ? false : true;
    }

    private void refresh() {
        String securityStrFormat;
        Logger.d("AccessPoint", "refresh 0001");
        setTitle(this.ssid);
        Context context = getContext();
        if (this.mState != null) {
            Logger.d("AccessPoint", "refresh 0002");
            setSummary(Summary.get(context, this.mState));
        } else if (this.mRssi == Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "refresh 0003");
            setSummary(context.getString(R.string.wifi_not_in_range));
        } else if (this.mConfig != null && this.mConfig.status == 1) {
            Logger.d("AccessPoint", "refresh 0004");
            switch (this.mConfig.disableReason) {
                case 0:
                    Logger.d("AccessPoint", "refresh 0008");
                    setSummary(context.getString(R.string.wifi_disabled_generic));
                    break;
                case 2:
                    Logger.d("AccessPoint", "refresh 0006");
                case 1:
                    Logger.d("AccessPoint", "refresh 0007");
                    setSummary(context.getString(R.string.wifi_disabled_network_failure));
                    break;
                case 3:
                    Logger.d("AccessPoint", "refresh 0005");
                    setSummary(context.getString(R.string.wifi_disabled_password_failure));
                    break;
            }
        } else {
            Logger.d("AccessPoint", "refresh 0009");
            StringBuilder summary = new StringBuilder();
            if (this.mConfig != null) {
                Logger.d("AccessPoint", "refresh 0010");
                summary.append(context.getString(R.string.wifi_remembered));
            }
            if (this.security != 0) {
                Logger.d("AccessPoint", "refresh 0011");
                if (summary.length() == 0) {
                    Logger.d("AccessPoint", "refresh 0012");
                    securityStrFormat = context.getString(R.string.wifi_secured_first_item);
                } else {
                    Logger.d("AccessPoint", "refresh 0013");
                    securityStrFormat = context.getString(R.string.wifi_secured_second_item);
                }
                summary.append(String.format(securityStrFormat, getSecurityString(true)));
            }
            if (this.mConfig == null && this.wpsAvailable) {
                Logger.d("AccessPoint", "refresh 0014");
                if (summary.length() == 0) {
                    Logger.d("AccessPoint", "refresh 0015");
                    summary.append(context.getString(R.string.wifi_wps_available_first_item));
                } else {
                    Logger.d("AccessPoint", "refresh 0016");
                    summary.append(context.getString(R.string.wifi_wps_available_second_item));
                }
            }
            setSummary(summary.toString());
        }
        if (getSummary() != null) {
            Logger.d("AccessPoint", "refresh 0017");
            this.securityMsg = getSummary().toString();
        }
        Logger.d("AccessPoint", "refresh 0018");
    }
}
