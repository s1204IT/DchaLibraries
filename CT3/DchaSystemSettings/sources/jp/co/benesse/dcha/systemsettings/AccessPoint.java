package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.IWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.LruCache;
import java.util.ArrayList;
import java.util.Map;
import jp.co.benesse.dcha.util.Logger;

public class AccessPoint implements Comparable<AccessPoint>, Cloneable {
    private String bssid;
    private AccessPointListener mAccessPointListener;
    private WifiConfiguration mConfig;
    private final Context mContext;
    private WifiInfo mInfo;
    private NetworkInfo mNetworkInfo;
    private int mRssi;
    public LruCache<String, ScanResult> mScanResultCache;
    boolean mWpsAvailable;
    private int networkId;
    private int pskType;
    private int security;
    private String ssid;

    public interface AccessPointListener {
        void onAccessPointChanged(AccessPoint accessPoint);

        void onLevelChanged(AccessPoint accessPoint);
    }

    public AccessPoint(Context context, Bundle savedState) {
        this.mScanResultCache = new LruCache<>(32);
        this.networkId = -1;
        this.pskType = 0;
        this.mRssi = Integer.MAX_VALUE;
        this.mWpsAvailable = false;
        Logger.d("AccessPoint", "AccessPoint 0001");
        this.mContext = context;
        this.mConfig = (WifiConfiguration) savedState.getParcelable("key_config");
        if (this.mConfig != null) {
            Logger.d("AccessPoint", "AccessPoint 0002");
            loadConfig(this.mConfig);
        }
        if (savedState.containsKey("key_ssid")) {
            Logger.d("AccessPoint", "AccessPoint 0003");
            this.ssid = savedState.getString("key_ssid");
        }
        if (savedState.containsKey("key_security")) {
            Logger.d("AccessPoint", "AccessPoint 0004");
            this.security = savedState.getInt("key_security");
        }
        if (savedState.containsKey("key_psktype")) {
            Logger.d("AccessPoint", "AccessPoint 0005");
            this.pskType = savedState.getInt("key_psktype");
        }
        this.mInfo = (WifiInfo) savedState.getParcelable("key_wifiinfo");
        if (savedState.containsKey("key_networkinfo")) {
            Logger.d("AccessPoint", "AccessPoint 0006");
            this.mNetworkInfo = (NetworkInfo) savedState.getParcelable("key_networkinfo");
        }
        if (savedState.containsKey("key_scanresultcache")) {
            Logger.d("AccessPoint", "AccessPoint 0007");
            ArrayList<ScanResult> scanResultArrayList = savedState.getParcelableArrayList("key_scanresultcache");
            this.mScanResultCache.evictAll();
            for (ScanResult result : scanResultArrayList) {
                Logger.d("AccessPoint", "AccessPoint 0008");
                this.mScanResultCache.put(result.BSSID, result);
            }
        }
        update(this.mConfig, this.mInfo, this.mNetworkInfo);
        this.mRssi = getRssi();
        Logger.d("AccessPoint", "AccessPoint 0009");
    }

    AccessPoint(Context context, ScanResult result) {
        this.mScanResultCache = new LruCache<>(32);
        this.networkId = -1;
        this.pskType = 0;
        this.mRssi = Integer.MAX_VALUE;
        this.mWpsAvailable = false;
        Logger.d("AccessPoint", "AccessPoint 0010");
        this.mContext = context;
        initWithScanResult(result);
        Logger.d("AccessPoint", "AccessPoint 0011");
    }

    AccessPoint(Context context, WifiConfiguration config) {
        this.mScanResultCache = new LruCache<>(32);
        this.networkId = -1;
        this.pskType = 0;
        this.mRssi = Integer.MAX_VALUE;
        this.mWpsAvailable = false;
        Logger.d("AccessPoint", "AccessPoint 0012");
        this.mContext = context;
        loadConfig(config);
        Logger.d("AccessPoint", "AccessPoint 0013");
    }

    public Object clone() {
        AccessPoint object = null;
        try {
            Logger.d("AccessPoint", "clone 0001");
            object = (AccessPoint) super.clone();
        } catch (CloneNotSupportedException e) {
            Logger.d("AccessPoint", "clone 0002");
            Logger.e("AccessPoint", "CloneNotSupportedException happens in clone()");
        }
        Logger.d("AccessPoint", "clone 0003");
        return object;
    }

    @Override
    public int compareTo(AccessPoint other) {
        Logger.d("AccessPoint", "compareTo 0001");
        if (isActive() && !other.isActive()) {
            Logger.d("AccessPoint", "compareTo 0002");
            return -1;
        }
        if (!isActive() && other.isActive()) {
            Logger.d("AccessPoint", "compareTo 0003");
            return 1;
        }
        if (this.mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "compareTo 0004");
            return -1;
        }
        if (this.mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "compareTo 0005");
            return 1;
        }
        if (this.networkId != -1 && other.networkId == -1) {
            Logger.d("AccessPoint", "compareTo 0006");
            return -1;
        }
        if (this.networkId == -1 && other.networkId != -1) {
            Logger.d("AccessPoint", "compareTo 0007");
            return 1;
        }
        int difference = WifiManager.calculateSignalLevel(other.mRssi, 4) - WifiManager.calculateSignalLevel(this.mRssi, 4);
        if (difference != 0) {
            Logger.d("AccessPoint", "compareTo 0008");
            return difference;
        }
        Logger.d("AccessPoint", "compareTo 0009");
        return this.ssid.compareToIgnoreCase(other.ssid);
    }

    public boolean equals(Object other) {
        Logger.d("AccessPoint", "equals 0001");
        if (!(other instanceof AccessPoint)) {
            Logger.d("AccessPoint", "equals 0002");
            return false;
        }
        Logger.d("AccessPoint", "equals 0003");
        return compareTo((AccessPoint) other) == 0;
    }

    public int hashCode() {
        Logger.d("AccessPoint", "hashCode 0001");
        int result = 0;
        if (this.mInfo != null) {
            Logger.d("AccessPoint", "hashCode 0002");
            result = (this.mInfo.hashCode() * 13) + 0;
        }
        int result2 = result + (this.mRssi * 19) + (this.networkId * 23) + (this.ssid.hashCode() * 29);
        Logger.d("AccessPoint", "hashCode 0003");
        return result2;
    }

    public String toString() {
        Logger.d("AccessPoint", "toString 0001");
        StringBuilder builder = new StringBuilder().append("AccessPoint(").append(this.ssid);
        if (isSaved()) {
            Logger.d("AccessPoint", "toString 0002");
            builder.append(',').append("saved");
        }
        if (isActive()) {
            Logger.d("AccessPoint", "toString 0003");
            builder.append(',').append("active");
        }
        if (isEphemeral()) {
            Logger.d("AccessPoint", "toString 0004");
            builder.append(',').append("ephemeral");
        }
        if (isConnectable()) {
            Logger.d("AccessPoint", "toString 0005");
            builder.append(',').append("connectable");
        }
        if (this.security != 0) {
            Logger.d("AccessPoint", "toString 0006");
            builder.append(',').append(securityToString(this.security, this.pskType));
        }
        Logger.d("AccessPoint", "toString 0007");
        return builder.append(')').toString();
    }

    public boolean matches(ScanResult result) {
        Logger.d("AccessPoint", "matches 0001");
        return this.ssid.equals(result.SSID) && this.security == getSecurity(result);
    }

    public boolean matches(WifiConfiguration config) {
        Logger.d("AccessPoint", "matches 0002");
        if (config.isPasspoint() && this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "matches 0003");
            return config.FQDN.equals(this.mConfig.providerFriendlyName);
        }
        Logger.d("AccessPoint", "matches 0004");
        if (this.ssid.equals(removeDoubleQuotes(config.SSID)) && this.security == getSecurity(config)) {
            return this.mConfig == null || this.mConfig.shared == config.shared;
        }
        return false;
    }

    public WifiConfiguration getConfig() {
        Logger.d("AccessPoint", "getConfig 0001");
        return this.mConfig;
    }

    public void clearConfig() {
        Logger.d("AccessPoint", "clearConfig 0001");
        this.mConfig = null;
        this.networkId = -1;
        Logger.d("AccessPoint", "clearConfig 0002");
    }

    public WifiInfo getInfo() {
        Logger.d("AccessPoint", "getInfo 0001");
        return this.mInfo;
    }

    public int getLevel() {
        Logger.d("AccessPoint", "getLevel 0001");
        if (this.mRssi == Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "getLevel 0002");
            return -1;
        }
        Logger.d("AccessPoint", "getLevel 0003");
        return WifiManager.calculateSignalLevel(this.mRssi, 4);
    }

    public int getRssi() {
        Logger.d("AccessPoint", "getRssi 0001");
        int rssi = Integer.MIN_VALUE;
        for (ScanResult result : this.mScanResultCache.snapshot().values()) {
            if (result.level > rssi) {
                rssi = result.level;
            }
        }
        Logger.d("AccessPoint", "getRssi 0002");
        return rssi;
    }

    public NetworkInfo getNetworkInfo() {
        Logger.d("AccessPoint", "getNetworkInfo 0001");
        return this.mNetworkInfo;
    }

    public int getSecurity() {
        Logger.d("AccessPoint", "getSecurity 0001");
        return this.security;
    }

    public String getSecurityString(boolean concise) {
        Logger.d("AccessPoint", "getSecurityString 0001");
        Context context = this.mContext;
        if (this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "getSecurityString 0002");
            return concise ? context.getString(R.string.wifi_security_short_eap) : context.getString(R.string.wifi_security_eap);
        }
        switch (this.security) {
            case 0:
                Logger.d("AccessPoint", "getSecurityString 0011");
                break;
            case 1:
                Logger.d("AccessPoint", "getSecurityString 0010");
                return concise ? context.getString(R.string.wifi_security_short_wep) : context.getString(R.string.wifi_security_wep);
            case 2:
                Logger.d("AccessPoint", "getSecurityString 0004");
                switch (this.pskType) {
                    case 0:
                        Logger.d("AccessPoint", "getSecurityString 0008");
                        break;
                    case 1:
                        Logger.d("AccessPoint", "getSecurityString 0005");
                        return concise ? context.getString(R.string.wifi_security_short_wpa) : context.getString(R.string.wifi_security_wpa);
                    case 2:
                        Logger.d("AccessPoint", "getSecurityString 0006");
                        return concise ? context.getString(R.string.wifi_security_short_wpa2) : context.getString(R.string.wifi_security_wpa2);
                    case 3:
                        Logger.d("AccessPoint", "getSecurityString 0007");
                        return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) : context.getString(R.string.wifi_security_wpa_wpa2);
                }
                Logger.d("AccessPoint", "getSecurityString 0009");
                return concise ? context.getString(R.string.wifi_security_short_psk_generic) : context.getString(R.string.wifi_security_psk_generic);
            case 3:
                Logger.d("AccessPoint", "getSecurityString 0003");
                return concise ? context.getString(R.string.wifi_security_short_eap) : context.getString(R.string.wifi_security_eap);
        }
        Logger.d("AccessPoint", "getSecurityString 0012");
        return concise ? "" : context.getString(R.string.wifi_security_none);
    }

    public String getSsidStr() {
        Logger.d("AccessPoint", "getSsidStr 0001");
        return this.ssid;
    }

    public CharSequence getSsid() {
        Logger.d("AccessPoint", "getSsid 0001");
        SpannableString str = new SpannableString(this.ssid);
        str.setSpan(new TtsSpan.VerbatimBuilder(this.ssid).build(), 0, this.ssid.length(), 18);
        Logger.d("AccessPoint", "getSsid 0002");
        return str;
    }

    public NetworkInfo.DetailedState getDetailedState() {
        Logger.d("AccessPoint", "getDetailedState 0001");
        if (this.mNetworkInfo != null) {
            return this.mNetworkInfo.getDetailedState();
        }
        return null;
    }

    public String getSummary() {
        Logger.d("AccessPoint", "getSummary 0001");
        return getSettingsSummary();
    }

    public String getSettingsSummary() {
        String securityStrFormat;
        Logger.d("AccessPoint", "getSettingsSummary 0001");
        StringBuilder summary = new StringBuilder();
        if (isActive() && this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "getSettingsSummary 0002");
            summary.append(getSummary(this.mContext, getDetailedState(), false, this.mConfig.providerFriendlyName));
        } else if (isActive()) {
            Logger.d("AccessPoint", "getSettingsSummary 0003");
            summary.append(getSummary(this.mContext, getDetailedState(), this.mInfo != null ? this.mInfo.isEphemeral() : false));
        } else if (this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "getSettingsSummary 0004");
            String format = this.mContext.getString(R.string.available_via_passpoint);
            summary.append(String.format(format, this.mConfig.providerFriendlyName));
        } else if (this.mConfig != null && this.mConfig.hasNoInternetAccess()) {
            Logger.d("AccessPoint", "getSettingsSummary 0005");
            summary.append(this.mContext.getString(R.string.wifi_no_internet));
        } else if (this.mConfig != null && !this.mConfig.getNetworkSelectionStatus().isNetworkEnabled()) {
            Logger.d("AccessPoint", "getSettingsSummary 0006");
            switch (this.mConfig.getNetworkSelectionStatus().getNetworkSelectionDisableReason()) {
                case 2:
                    Logger.d("AccessPoint", "getSettingsSummary 0010");
                    summary.append(this.mContext.getString(R.string.wifi_disabled_generic));
                    break;
                case 3:
                    Logger.d("AccessPoint", "getSettingsSummary 0007");
                    summary.append(this.mContext.getString(R.string.wifi_disabled_password_failure));
                    break;
                case 4:
                    Logger.d("AccessPoint", "getSettingsSummary 0008");
                case 5:
                    Logger.d("AccessPoint", "getSettingsSummary 0009");
                    summary.append(this.mContext.getString(R.string.wifi_disabled_network_failure));
                    break;
            }
        } else if (this.mRssi == Integer.MAX_VALUE) {
            Logger.d("AccessPoint", "getSettingsSummary 0011");
            summary.append(this.mContext.getString(R.string.wifi_not_in_range));
        } else {
            Logger.d("AccessPoint", "getSettingsSummary 0012");
            if (this.mConfig != null) {
                Logger.d("AccessPoint", "getSettingsSummary 0013");
                summary.append(this.mContext.getString(R.string.wifi_remembered));
            }
            if (this.security != 0) {
                Logger.d("AccessPoint", "getSettingsSummary 0014");
                if (summary.length() == 0) {
                    Logger.d("AccessPoint", "getSettingsSummary 0015");
                    securityStrFormat = this.mContext.getString(R.string.wifi_secured_first_item);
                } else {
                    Logger.d("AccessPoint", "getSettingsSummary 0016");
                    securityStrFormat = this.mContext.getString(R.string.wifi_secured_second_item);
                }
                summary.append(String.format(securityStrFormat, getSecurityString(true)));
            }
            if (this.mConfig == null && this.mWpsAvailable) {
                Logger.d("AccessPoint", "getSettingsSummary 0017");
                if (summary.length() == 0) {
                    summary.append(this.mContext.getString(R.string.wifi_wps_available_first_item));
                    Logger.d("AccessPoint", "getSettingsSummary 0018");
                } else {
                    summary.append(this.mContext.getString(R.string.wifi_wps_available_second_item));
                    Logger.d("AccessPoint", "getSettingsSummary 0019");
                }
            }
        }
        if (WifiTracker.sVerboseLogging > 0) {
            Logger.d("AccessPoint", "getSettingsSummary 0020");
            if (this.mInfo != null && this.mNetworkInfo != null) {
                Logger.d("AccessPoint", "getSettingsSummary 0021");
                summary.append(" f=").append(Integer.toString(this.mInfo.getFrequency()));
            }
            summary.append(" ").append(getVisibilityStatus());
            if (this.mConfig != null && !this.mConfig.getNetworkSelectionStatus().isNetworkEnabled()) {
                Logger.d("AccessPoint", "getSettingsSummary 0022");
                summary.append(" (").append(this.mConfig.getNetworkSelectionStatus().getNetworkStatusString());
                if (this.mConfig.getNetworkSelectionStatus().getDisableTime() > 0) {
                    Logger.d("AccessPoint", "getSettingsSummary 0023");
                    long now = System.currentTimeMillis();
                    long diff = (now - this.mConfig.getNetworkSelectionStatus().getDisableTime()) / 1000;
                    long sec = diff % 60;
                    long min = (diff / 60) % 60;
                    long hour = (min / 60) % 60;
                    summary.append(", ");
                    if (hour > 0) {
                        Logger.d("AccessPoint", "getSettingsSummary 0024");
                        summary.append(Long.toString(hour)).append("h ");
                    }
                    summary.append(Long.toString(min)).append("m ");
                    summary.append(Long.toString(sec)).append("s ");
                }
                summary.append(")");
            }
            if (this.mConfig != null) {
                Logger.d("AccessPoint", "getSettingsSummary 0025");
                WifiConfiguration.NetworkSelectionStatus networkStatus = this.mConfig.getNetworkSelectionStatus();
                for (int index = 0; index < 11; index++) {
                    if (networkStatus.getDisableReasonCounter(index) != 0) {
                        summary.append(" ").append(WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(index)).append("=").append(networkStatus.getDisableReasonCounter(index));
                    }
                }
            }
        }
        Logger.d("AccessPoint", "getSettingsSummary 0026");
        return summary.toString();
    }

    private String getVisibilityStatus() {
        Logger.d("AccessPoint", "getVisibilityStatus 0001");
        StringBuilder visibility = new StringBuilder();
        StringBuilder scans24GHz = null;
        StringBuilder scans5GHz = null;
        String bssid = null;
        if (this.mInfo != null) {
            Logger.d("AccessPoint", "getVisibilityStatus 0002");
            bssid = this.mInfo.getBSSID();
            if (bssid != null) {
                Logger.d("AccessPoint", "getVisibilityStatus 0003");
                visibility.append(" ").append(bssid);
            }
            visibility.append(" rssi=").append(this.mInfo.getRssi());
            visibility.append(" ");
            visibility.append(" score=").append(this.mInfo.score);
            visibility.append(String.format(" tx=%.1f,", Double.valueOf(this.mInfo.txSuccessRate)));
            visibility.append(String.format("%.1f,", Double.valueOf(this.mInfo.txRetriesRate)));
            visibility.append(String.format("%.1f ", Double.valueOf(this.mInfo.txBadRate)));
            visibility.append(String.format("rx=%.1f", Double.valueOf(this.mInfo.rxSuccessRate)));
        }
        int rssi5 = WifiConfiguration.INVALID_RSSI;
        int rssi24 = WifiConfiguration.INVALID_RSSI;
        int num5 = 0;
        int num24 = 0;
        int n24 = 0;
        int n5 = 0;
        Map<String, ScanResult> list = this.mScanResultCache.snapshot();
        for (ScanResult result : list.values()) {
            if (result.frequency >= 4900 && result.frequency <= 5900) {
                num5++;
            } else if (result.frequency >= 2400 && result.frequency <= 2500) {
                num24++;
            }
            if (result.frequency >= 4900 && result.frequency <= 5900) {
                if (result.level > rssi5) {
                    rssi5 = result.level;
                }
                if (n5 < 4) {
                    if (scans5GHz == null) {
                        scans5GHz = new StringBuilder();
                    }
                    scans5GHz.append(" \n{").append(result.BSSID);
                    if (bssid != null && result.BSSID.equals(bssid)) {
                        scans5GHz.append("*");
                    }
                    scans5GHz.append("=").append(result.frequency);
                    scans5GHz.append(",").append(result.level);
                    scans5GHz.append("}");
                    n5++;
                }
            } else if (result.frequency >= 2400 && result.frequency <= 2500) {
                if (result.level > rssi24) {
                    rssi24 = result.level;
                }
                if (n24 < 4) {
                    if (scans24GHz == null) {
                        scans24GHz = new StringBuilder();
                    }
                    scans24GHz.append(" \n{").append(result.BSSID);
                    if (bssid != null && result.BSSID.equals(bssid)) {
                        scans24GHz.append("*");
                    }
                    scans24GHz.append("=").append(result.frequency);
                    scans24GHz.append(",").append(result.level);
                    scans24GHz.append("}");
                    n24++;
                }
            }
        }
        visibility.append(" [");
        if (num24 > 0) {
            visibility.append("(").append(num24).append(")");
            if (n24 <= 4) {
                Logger.d("AccessPoint", "getVisibilityStatus 0004");
                if (scans24GHz != null) {
                    Logger.d("AccessPoint", "getVisibilityStatus 0005");
                    visibility.append(scans24GHz.toString());
                }
            } else {
                Logger.d("AccessPoint", "getVisibilityStatus 0006");
                visibility.append("max=").append(rssi24);
                if (scans24GHz != null) {
                    Logger.d("AccessPoint", "getVisibilityStatus 0007");
                    visibility.append(",").append(scans24GHz.toString());
                }
            }
        }
        visibility.append(";");
        if (num5 > 0) {
            Logger.d("AccessPoint", "getVisibilityStatus 0008");
            visibility.append("(").append(num5).append(")");
            if (n5 <= 4) {
                Logger.d("AccessPoint", "getVisibilityStatus 0009");
                if (scans5GHz != null) {
                    Logger.d("AccessPoint", "getVisibilityStatus 0010");
                    visibility.append(scans5GHz.toString());
                }
            } else {
                Logger.d("AccessPoint", "getVisibilityStatus 0011");
                visibility.append("max=").append(rssi5);
                if (scans5GHz != null) {
                    Logger.d("AccessPoint", "getVisibilityStatus 0012");
                    visibility.append(",").append(scans5GHz.toString());
                }
            }
        }
        visibility.append("]");
        Logger.d("AccessPoint", "getVisibilityStatus 0014");
        return visibility.toString();
    }

    public boolean isActive() {
        Logger.d("AccessPoint", "isActive 0001");
        if (this.mNetworkInfo != null) {
            return (this.networkId == -1 && this.mNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED) ? false : true;
        }
        return false;
    }

    public boolean isConnectable() {
        Logger.d("AccessPoint", "isConnectable 0001");
        return getLevel() != -1 && getDetailedState() == null;
    }

    public boolean isEphemeral() {
        Logger.d("AccessPoint", "isEphemeral 0001");
        if (this.mInfo == null || !this.mInfo.isEphemeral() || this.mNetworkInfo == null) {
            return false;
        }
        return this.mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED;
    }

    public boolean isPasspoint() {
        Logger.d("AccessPoint", "isPasspoint 0001");
        if (this.mConfig != null) {
            return this.mConfig.isPasspoint();
        }
        return false;
    }

    private boolean isInfoForThisAccessPoint(WifiConfiguration config, WifiInfo info) {
        Logger.d("AccessPoint", "isInfoForThisAccessPoint 0001");
        if (!isPasspoint() && this.networkId != -1) {
            Logger.d("AccessPoint", "isInfoForThisAccessPoint 0002");
            return this.networkId == info.getNetworkId();
        }
        if (config != null) {
            Logger.d("AccessPoint", "isInfoForThisAccessPoint 0003");
            return matches(config);
        }
        Logger.d("AccessPoint", "isInfoForThisAccessPoint 0004");
        return this.ssid.equals(removeDoubleQuotes(info.getSSID()));
    }

    public boolean isSaved() {
        Logger.d("AccessPoint", "isSaved 0001");
        return this.networkId != -1;
    }

    void loadConfig(WifiConfiguration config) {
        Logger.d("AccessPoint", "loadConfig 0001");
        if (config.isPasspoint()) {
            Logger.d("AccessPoint", "loadConfig 0002");
            this.ssid = config.providerFriendlyName;
        } else {
            Logger.d("AccessPoint", "loadConfig 0003");
            this.ssid = config.SSID == null ? "" : removeDoubleQuotes(config.SSID);
        }
        this.bssid = config.BSSID;
        this.security = getSecurity(config);
        this.networkId = config.networkId;
        this.mConfig = config;
        Logger.d("AccessPoint", "loadConfig 0004");
    }

    private void initWithScanResult(ScanResult result) {
        Logger.d("AccessPoint", "initWithScanResult 0001");
        this.ssid = result.SSID;
        this.bssid = result.BSSID;
        this.security = getSecurity(result);
        this.mWpsAvailable = this.security != 3 ? result.capabilities.contains("WPS") : false;
        if (this.security == 2) {
            Logger.d("AccessPoint", "initWithScanResult 0002");
            this.pskType = getPskType(result);
        }
        this.mRssi = result.level;
        Logger.d("AccessPoint", "initWithScanResult 0003");
    }

    public void saveWifiState(Bundle savedState) {
        Logger.d("AccessPoint", "saveWifiState 0001");
        if (this.ssid != null) {
            Logger.d("AccessPoint", "saveWifiState 0002");
            savedState.putString("key_ssid", getSsidStr());
        }
        savedState.putInt("key_security", this.security);
        savedState.putInt("key_psktype", this.pskType);
        if (this.mConfig != null) {
            Logger.d("AccessPoint", "saveWifiState 0003");
            savedState.putParcelable("key_config", this.mConfig);
        }
        savedState.putParcelable("key_wifiinfo", this.mInfo);
        savedState.putParcelableArrayList("key_scanresultcache", new ArrayList<>(this.mScanResultCache.snapshot().values()));
        if (this.mNetworkInfo != null) {
            Logger.d("AccessPoint", "saveWifiState 0004");
            savedState.putParcelable("key_networkinfo", this.mNetworkInfo);
        }
        Logger.d("AccessPoint", "saveWifiState 0005");
    }

    public void setListener(AccessPointListener listener) {
        Logger.d("AccessPoint", "setListener 0001");
        this.mAccessPointListener = listener;
        Logger.d("AccessPoint", "setListener 0002");
    }

    boolean update(ScanResult result) {
        Logger.d("AccessPoint", "update 0001");
        if (matches(result)) {
            Logger.d("AccessPoint", "update 0002");
            this.mScanResultCache.get(result.BSSID);
            this.mScanResultCache.put(result.BSSID, result);
            int oldLevel = getLevel();
            int oldRssi = getRssi();
            this.mRssi = (getRssi() + oldRssi) / 2;
            int newLevel = getLevel();
            if (newLevel > 0 && newLevel != oldLevel && this.mAccessPointListener != null) {
                Logger.d("AccessPoint", "update 0003");
                this.mAccessPointListener.onLevelChanged(this);
            }
            if (this.security == 2) {
                Logger.d("AccessPoint", "update 0004");
                this.pskType = getPskType(result);
            }
            if (this.mAccessPointListener != null) {
                Logger.d("AccessPoint", "update 0005");
                this.mAccessPointListener.onAccessPointChanged(this);
            }
            Logger.d("AccessPoint", "update 0006");
            return true;
        }
        Logger.d("AccessPoint", "update 0007");
        return false;
    }

    boolean update(WifiConfiguration config, WifiInfo info, NetworkInfo networkInfo) {
        Logger.d("AccessPoint", "update 0008");
        boolean reorder = false;
        if (info != null && isInfoForThisAccessPoint(config, info)) {
            Logger.d("AccessPoint", "update 0009");
            reorder = this.mInfo == null;
            this.mRssi = info.getRssi();
            this.mInfo = info;
            this.mNetworkInfo = networkInfo;
            if (this.mAccessPointListener != null) {
                Logger.d("AccessPoint", "update 0010");
                this.mAccessPointListener.onAccessPointChanged(this);
            }
        } else if (this.mInfo != null) {
            Logger.d("AccessPoint", "update 0011");
            reorder = true;
            this.mInfo = null;
            this.mNetworkInfo = null;
            if (this.mAccessPointListener != null) {
                Logger.d("AccessPoint", "update 0012");
                this.mAccessPointListener.onAccessPointChanged(this);
            }
        }
        Logger.d("AccessPoint", "update 0013");
        return reorder;
    }

    void update(WifiConfiguration config) {
        Logger.d("AccessPoint", "update 0014");
        this.mConfig = config;
        this.networkId = config.networkId;
        if (this.mAccessPointListener != null) {
            Logger.d("AccessPoint", "update 0015");
            this.mAccessPointListener.onAccessPointChanged(this);
        }
        Logger.d("AccessPoint", "update 0016");
    }

    void setRssi(int rssi) {
        Logger.d("AccessPoint", "setRssi 0001");
        this.mRssi = rssi;
        Logger.d("AccessPoint", "setRssi 0002");
    }

    public static String getSummary(Context context, String ssid, NetworkInfo.DetailedState state, boolean isEphemeral, String passpointProvider) {
        Network currentNetwork;
        Logger.d("AccessPoint", "getSummary 0002");
        if (state == NetworkInfo.DetailedState.CONNECTED && ssid == null) {
            Logger.d("AccessPoint", "getSummary 0003");
            if (!TextUtils.isEmpty(passpointProvider)) {
                Logger.d("AccessPoint", "getSummary 0004");
                String format = context.getString(R.string.connected_via_passpoint);
                return String.format(format, passpointProvider);
            }
            if (isEphemeral) {
                Logger.d("AccessPoint", "getSummary 0005");
                return context.getString(R.string.connected_via_wfa);
            }
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (state == NetworkInfo.DetailedState.CONNECTED) {
            Logger.d("AccessPoint", "getSummary 0006");
            IWifiManager wifiManager = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi"));
            try {
                currentNetwork = wifiManager.getCurrentNetwork();
            } catch (RemoteException e) {
                Logger.d("AccessPoint", "getSummary 0007");
                Logger.d("AccessPoint", "RemoteException", e);
                currentNetwork = null;
            }
            NetworkCapabilities nc = cm.getNetworkCapabilities(currentNetwork);
            if (nc != null && !nc.hasCapability(16)) {
                Logger.d("AccessPoint", "getSummary 0008");
                return context.getString(R.string.wifi_connected_no_internet);
            }
        }
        String[] formats = context.getResources().getStringArray(ssid == null ? R.array.wifi_status : R.array.wifi_status_with_ssid);
        int index = state.ordinal();
        if (index >= formats.length || formats[index].length() == 0) {
            Logger.d("AccessPoint", "getSummary 0009");
            return "";
        }
        Logger.d("AccessPoint", "getSummary 0010");
        return String.format(formats[index], ssid);
    }

    public static String getSummary(Context context, NetworkInfo.DetailedState state, boolean isEphemeral) {
        Logger.d("AccessPoint", "getSummary 0011");
        return getSummary(context, null, state, isEphemeral, null);
    }

    public static String getSummary(Context context, NetworkInfo.DetailedState state, boolean isEphemeral, String passpointProvider) {
        Logger.d("AccessPoint", "getSummary 0012");
        return getSummary(context, null, state, isEphemeral, passpointProvider);
    }

    public static String convertToQuotedString(String string) {
        Logger.d("AccessPoint", "convertToQuotedString 0001");
        return "\"" + string + "\"";
    }

    private static int getPskType(ScanResult result) {
        Logger.d("AccessPoint", "getPskType 0001");
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            Logger.d("AccessPoint", "getPskType 0002");
            return 3;
        }
        if (wpa2) {
            Logger.d("AccessPoint", "getPskType 0003");
            return 2;
        }
        if (wpa) {
            Logger.d("AccessPoint", "getPskType 0004");
            return 1;
        }
        Logger.d("AccessPoint", "getPskType 0005");
        Logger.w("AccessPoint", "Received abnormal flag string: " + result.capabilities);
        return 0;
    }

    private static int getSecurity(ScanResult result) {
        Logger.d("AccessPoint", "getSecurity 0001");
        if (result.capabilities.contains("WEP")) {
            Logger.d("AccessPoint", "getSecurity 0002");
            return 1;
        }
        if (result.capabilities.contains("PSK")) {
            Logger.d("AccessPoint", "getSecurity 0003");
            return 2;
        }
        if (result.capabilities.contains("EAP")) {
            Logger.d("AccessPoint", "getSecurity 0004");
            return 3;
        }
        Logger.d("AccessPoint", "getSecurity 0005");
        return 0;
    }

    static int getSecurity(WifiConfiguration config) {
        Logger.d("AccessPoint", "getSecurity 0006");
        if (config.allowedKeyManagement.get(1)) {
            Logger.d("AccessPoint", "getSecurity 0007");
            return 2;
        }
        if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            Logger.d("AccessPoint", "getSecurity 0008");
            return 3;
        }
        Logger.d("AccessPoint", "getSecurity 0009");
        return config.wepKeys[0] != null ? 1 : 0;
    }

    public static String securityToString(int security, int pskType) {
        Logger.d("AccessPoint", "securityToString 0001");
        if (security == 1) {
            Logger.d("AccessPoint", "securityToString 0002");
            return "WEP";
        }
        if (security == 2) {
            Logger.d("AccessPoint", "securityToString 0003");
            if (pskType == 1) {
                Logger.d("AccessPoint", "securityToString 0004");
                return "WPA";
            }
            if (pskType == 2) {
                Logger.d("AccessPoint", "securityToString 0005");
                return "WPA2";
            }
            if (pskType == 3) {
                Logger.d("AccessPoint", "securityToString 0006");
                return "WPA_WPA2";
            }
            Logger.d("AccessPoint", "securityToString 0007");
            return "PSK";
        }
        if (security == 3) {
            Logger.d("AccessPoint", "securityToString 0008");
            return "EAP";
        }
        Logger.d("AccessPoint", "securityToString 0009");
        return "NONE";
    }

    static String removeDoubleQuotes(String string) {
        Logger.d("AccessPoint", "removeDoubleQuotes 0001");
        if (TextUtils.isEmpty(string)) {
            Logger.d("AccessPoint", "removeDoubleQuotes 0002");
            return "";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            Logger.d("AccessPoint", "removeDoubleQuotes 0003");
            return string.substring(1, length - 1);
        }
        Logger.d("AccessPoint", "removeDoubleQuotes 0004");
        return string;
    }
}
