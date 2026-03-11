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
import jp.co.benesse.dcha.util.Logger;

public class AccessPoint implements Cloneable, Comparable<AccessPoint> {
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

    public AccessPoint(Context context, Bundle bundle) {
        this.mScanResultCache = new LruCache<>(32);
        this.networkId = -1;
        this.pskType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWpsAvailable = false;
        Logger.d("AccessPoint", "AccessPoint 0001");
        this.mContext = context;
        if (bundle.containsKey("key_config")) {
            this.mConfig = (WifiConfiguration) bundle.getParcelable("key_config");
        }
        if (this.mConfig != null) {
            Logger.d("AccessPoint", "AccessPoint 0002");
            loadConfig(this.mConfig);
        }
        if (bundle.containsKey("key_ssid")) {
            Logger.d("AccessPoint", "AccessPoint 0003");
            this.ssid = bundle.getString("key_ssid");
        }
        if (bundle.containsKey("key_security")) {
            Logger.d("AccessPoint", "AccessPoint 0004");
            this.security = bundle.getInt("key_security");
        }
        if (bundle.containsKey("key_psktype")) {
            Logger.d("AccessPoint", "AccessPoint 0005");
            this.pskType = bundle.getInt("key_psktype");
        }
        this.mInfo = (WifiInfo) bundle.getParcelable("key_wifiinfo");
        if (bundle.containsKey("key_networkinfo")) {
            Logger.d("AccessPoint", "AccessPoint 0006");
            this.mNetworkInfo = (NetworkInfo) bundle.getParcelable("key_networkinfo");
        }
        if (bundle.containsKey("key_scanresultcache")) {
            Logger.d("AccessPoint", "AccessPoint 0007");
            ArrayList<ScanResult> parcelableArrayList = bundle.getParcelableArrayList("key_scanresultcache");
            this.mScanResultCache.evictAll();
            for (ScanResult scanResult : parcelableArrayList) {
                Logger.d("AccessPoint", "AccessPoint 0008");
                this.mScanResultCache.put(scanResult.BSSID, scanResult);
            }
        }
        update(this.mConfig, this.mInfo, this.mNetworkInfo);
        Logger.d("AccessPoint", "AccessPoint 0009");
    }

    AccessPoint(Context context, ScanResult scanResult) {
        this.mScanResultCache = new LruCache<>(32);
        this.networkId = -1;
        this.pskType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWpsAvailable = false;
        Logger.d("AccessPoint", "AccessPoint 0010");
        this.mContext = context;
        initWithScanResult(scanResult);
        Logger.d("AccessPoint", "AccessPoint 0011");
    }

    AccessPoint(Context context, WifiConfiguration wifiConfiguration) {
        this.mScanResultCache = new LruCache<>(32);
        this.networkId = -1;
        this.pskType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWpsAvailable = false;
        Logger.d("AccessPoint", "AccessPoint 0012");
        this.mContext = context;
        loadConfig(wifiConfiguration);
        Logger.d("AccessPoint", "AccessPoint 0013");
    }

    public Object clone() {
        AccessPoint accessPoint;
        try {
            Logger.d("AccessPoint", "clone 0001");
            accessPoint = (AccessPoint) super.clone();
        } catch (CloneNotSupportedException e) {
            Logger.d("AccessPoint", "clone 0002");
            Logger.e("AccessPoint", "CloneNotSupportedException happens in clone()");
            accessPoint = null;
        }
        Logger.d("AccessPoint", "clone 0003");
        return accessPoint;
    }

    @Override
    public int compareTo(AccessPoint accessPoint) {
        Logger.d("AccessPoint", "compareTo 0001");
        if (isActive() && !accessPoint.isActive()) {
            return -1;
        }
        if (!isActive() && accessPoint.isActive()) {
            return 1;
        }
        if (isReachable() && !accessPoint.isReachable()) {
            return -1;
        }
        if (!isReachable() && accessPoint.isReachable()) {
            return 1;
        }
        if (isSaved() && !accessPoint.isSaved()) {
            return -1;
        }
        if (!isSaved() && accessPoint.isSaved()) {
            return 1;
        }
        int iCalculateSignalLevel = WifiManager.calculateSignalLevel(accessPoint.mRssi, 4) - WifiManager.calculateSignalLevel(this.mRssi, 4);
        if (iCalculateSignalLevel != 0) {
            Logger.d("AccessPoint", "compareTo 0008");
            return iCalculateSignalLevel;
        }
        int iCompareToIgnoreCase = this.ssid.compareToIgnoreCase(accessPoint.ssid);
        if (iCompareToIgnoreCase != 0) {
            Logger.d("AccessPoint", "compareTo 0009");
            return iCompareToIgnoreCase;
        }
        Logger.d("AccessPoint", "compareTo 0010");
        return this.ssid.compareTo(accessPoint.ssid);
    }

    public boolean equals(Object obj) {
        Logger.d("AccessPoint", "equals 0001");
        if (!(obj instanceof AccessPoint)) {
            Logger.d("AccessPoint", "equals 0002");
            return false;
        }
        Logger.d("AccessPoint", "equals 0003");
        return compareTo((AccessPoint) obj) == 0;
    }

    public int hashCode() {
        int iHashCode;
        Logger.d("AccessPoint", "hashCode 0001");
        if (this.mInfo != null) {
            Logger.d("AccessPoint", "hashCode 0002");
            iHashCode = (13 * this.mInfo.hashCode()) + 0;
        } else {
            iHashCode = 0;
        }
        int iHashCode2 = iHashCode + (19 * this.mRssi) + (23 * this.networkId) + (29 * this.ssid.hashCode());
        Logger.d("AccessPoint", "hashCode 0003");
        return iHashCode2;
    }

    public String toString() {
        Logger.d("AccessPoint", "toString 0001");
        StringBuilder sb = new StringBuilder();
        sb.append("AccessPoint(");
        sb.append(this.ssid);
        if (isSaved()) {
            Logger.d("AccessPoint", "toString 0002");
            sb.append(',');
            sb.append("saved");
        }
        if (isActive()) {
            Logger.d("AccessPoint", "toString 0003");
            sb.append(',');
            sb.append("active");
        }
        if (isEphemeral()) {
            Logger.d("AccessPoint", "toString 0004");
            sb.append(',');
            sb.append("ephemeral");
        }
        if (isConnectable()) {
            Logger.d("AccessPoint", "toString 0005");
            sb.append(',');
            sb.append("connectable");
        }
        if (this.security != 0) {
            Logger.d("AccessPoint", "toString 0006");
            sb.append(',');
            sb.append(securityToString(this.security, this.pskType));
        }
        Logger.d("AccessPoint", "toString 0007");
        sb.append(')');
        return sb.toString();
    }

    public boolean matches(ScanResult scanResult) {
        Logger.d("AccessPoint", "matches 0001");
        return this.ssid.equals(scanResult.SSID) && this.security == getSecurity(scanResult);
    }

    public boolean matches(WifiConfiguration wifiConfiguration) {
        Logger.d("AccessPoint", "matches 0002");
        if (wifiConfiguration.isPasspoint() && this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "matches 0003");
            return wifiConfiguration.FQDN.equals(this.mConfig.providerFriendlyName);
        }
        Logger.d("AccessPoint", "matches 0004");
        return this.ssid.equals(removeDoubleQuotes(wifiConfiguration.SSID)) && this.security == getSecurity(wifiConfiguration) && (this.mConfig == null || this.mConfig.shared == wifiConfiguration.shared);
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
        return WifiManager.calculateSignalLevel(this.mRssi, 4);
    }

    private void updateRssi() {
        if (isActive()) {
            return;
        }
        int i = Integer.MIN_VALUE;
        for (ScanResult scanResult : this.mScanResultCache.snapshot().values()) {
            if (scanResult.level > i) {
                i = scanResult.level;
            }
        }
        if (i != Integer.MIN_VALUE && this.mRssi != Integer.MIN_VALUE) {
            this.mRssi = (this.mRssi + i) / 2;
        } else {
            this.mRssi = i;
        }
    }

    public NetworkInfo getNetworkInfo() {
        Logger.d("AccessPoint", "getNetworkInfo 0001");
        return this.mNetworkInfo;
    }

    public int getSecurity() {
        Logger.d("AccessPoint", "getSecurity 0001");
        return this.security;
    }

    public String getSecurityString(boolean z) {
        Logger.d("AccessPoint", "getSecurityString 0001");
        Context context = this.mContext;
        if (this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "getSecurityString 0002");
            return z ? context.getString(R.string.wifi_security_short_eap) : context.getString(R.string.wifi_security_eap);
        }
        switch (this.security) {
            case 0:
                Logger.d("AccessPoint", "getSecurityString 0011");
                break;
            case 1:
                Logger.d("AccessPoint", "getSecurityString 0010");
                return z ? context.getString(R.string.wifi_security_short_wep) : context.getString(R.string.wifi_security_wep);
            case 2:
                Logger.d("AccessPoint", "getSecurityString 0004");
                switch (this.pskType) {
                    case 0:
                        Logger.d("AccessPoint", "getSecurityString 0008");
                        break;
                    case 1:
                        Logger.d("AccessPoint", "getSecurityString 0005");
                        return z ? context.getString(R.string.wifi_security_short_wpa) : context.getString(R.string.wifi_security_wpa);
                    case 2:
                        Logger.d("AccessPoint", "getSecurityString 0006");
                        return z ? context.getString(R.string.wifi_security_short_wpa2) : context.getString(R.string.wifi_security_wpa2);
                    case 3:
                        Logger.d("AccessPoint", "getSecurityString 0007");
                        return z ? context.getString(R.string.wifi_security_short_wpa_wpa2) : context.getString(R.string.wifi_security_wpa_wpa2);
                }
                Logger.d("AccessPoint", "getSecurityString 0009");
                return z ? context.getString(R.string.wifi_security_short_psk_generic) : context.getString(R.string.wifi_security_psk_generic);
            case 3:
                Logger.d("AccessPoint", "getSecurityString 0003");
                return z ? context.getString(R.string.wifi_security_short_eap) : context.getString(R.string.wifi_security_eap);
        }
        Logger.d("AccessPoint", "getSecurityString 0012");
        return z ? "" : context.getString(R.string.wifi_security_none);
    }

    public String getSsidStr() {
        Logger.d("AccessPoint", "getSsidStr 0001");
        return this.ssid;
    }

    public CharSequence getSsid() {
        Logger.d("AccessPoint", "getSsid 0001");
        SpannableString spannableString = new SpannableString(this.ssid);
        spannableString.setSpan(new TtsSpan.VerbatimBuilder(this.ssid).build(), 0, this.ssid.length(), 18);
        Logger.d("AccessPoint", "getSsid 0002");
        return spannableString;
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
        String string;
        Logger.d("AccessPoint", "getSettingsSummary 0001");
        StringBuilder sb = new StringBuilder();
        if (isActive() && this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "getSettingsSummary 0002");
            sb.append(getSummary(this.mContext, getDetailedState(), false, this.mConfig.providerFriendlyName));
        } else if (isActive()) {
            Logger.d("AccessPoint", "getSettingsSummary 0003");
            sb.append(getSummary(this.mContext, getDetailedState(), this.mInfo != null && this.mInfo.isEphemeral()));
        } else if (this.mConfig != null && this.mConfig.isPasspoint()) {
            Logger.d("AccessPoint", "getSettingsSummary 0004");
            sb.append(String.format(this.mContext.getString(R.string.available_via_passpoint), this.mConfig.providerFriendlyName));
        } else if (this.mConfig != null && this.mConfig.hasNoInternetAccess()) {
            Logger.d("AccessPoint", "getSettingsSummary 0005");
            sb.append(this.mContext.getString(R.string.wifi_no_internet));
        } else if (this.mConfig != null && !this.mConfig.getNetworkSelectionStatus().isNetworkEnabled()) {
            Logger.d("AccessPoint", "getSettingsSummary 0006");
            switch (this.mConfig.getNetworkSelectionStatus().getNetworkSelectionDisableReason()) {
                case 2:
                    Logger.d("AccessPoint", "getSettingsSummary 0010");
                    sb.append(this.mContext.getString(R.string.wifi_disabled_generic));
                    break;
                case 3:
                    Logger.d("AccessPoint", "getSettingsSummary 0007");
                    sb.append(this.mContext.getString(R.string.wifi_disabled_password_failure));
                    break;
                case 4:
                    Logger.d("AccessPoint", "getSettingsSummary 0008");
                case 5:
                    Logger.d("AccessPoint", "getSettingsSummary 0009");
                    sb.append(this.mContext.getString(R.string.wifi_disabled_network_failure));
                    break;
            }
        } else if (!isReachable()) {
            Logger.d("AccessPoint", "getSettingsSummary 0011");
            sb.append(this.mContext.getString(R.string.wifi_not_in_range));
        } else {
            Logger.d("AccessPoint", "getSettingsSummary 0012");
            if (this.mConfig != null) {
                Logger.d("AccessPoint", "getSettingsSummary 0013");
                sb.append(this.mContext.getString(R.string.wifi_remembered));
            }
            if (this.security != 0) {
                Logger.d("AccessPoint", "getSettingsSummary 0014");
                if (sb.length() == 0) {
                    Logger.d("AccessPoint", "getSettingsSummary 0015");
                    string = this.mContext.getString(R.string.wifi_secured_first_item);
                } else {
                    Logger.d("AccessPoint", "getSettingsSummary 0016");
                    string = this.mContext.getString(R.string.wifi_secured_second_item);
                }
                sb.append(String.format(string, getSecurityString(true)));
            }
            if (this.mConfig == null && this.mWpsAvailable) {
                Logger.d("AccessPoint", "getSettingsSummary 0017");
                if (sb.length() == 0) {
                    sb.append(this.mContext.getString(R.string.wifi_wps_available_first_item));
                    Logger.d("AccessPoint", "getSettingsSummary 0018");
                } else {
                    sb.append(this.mContext.getString(R.string.wifi_wps_available_second_item));
                    Logger.d("AccessPoint", "getSettingsSummary 0019");
                }
            }
        }
        Logger.d("AccessPoint", "getSettingsSummary 0020");
        return sb.toString();
    }

    public boolean isActive() {
        Logger.d("AccessPoint", "isActive 0001");
        return (this.mNetworkInfo == null || (this.networkId == -1 && this.mNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED)) ? false : true;
    }

    public boolean isConnectable() {
        Logger.d("AccessPoint", "isConnectable 0001");
        return getLevel() != -1 && getDetailedState() == null;
    }

    public boolean isEphemeral() {
        Logger.d("AccessPoint", "isEphemeral 0001");
        return (this.mInfo == null || !this.mInfo.isEphemeral() || this.mNetworkInfo == null || this.mNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED) ? false : true;
    }

    public boolean isPasspoint() {
        Logger.d("AccessPoint", "isPasspoint 0001");
        return this.mConfig != null && this.mConfig.isPasspoint();
    }

    private boolean isInfoForThisAccessPoint(WifiConfiguration wifiConfiguration, WifiInfo wifiInfo) {
        Logger.d("AccessPoint", "isInfoForThisAccessPoint 0001");
        if (!isPasspoint() && this.networkId != -1) {
            Logger.d("AccessPoint", "isInfoForThisAccessPoint 0002");
            return this.networkId == wifiInfo.getNetworkId();
        }
        if (wifiConfiguration != null) {
            Logger.d("AccessPoint", "isInfoForThisAccessPoint 0003");
            return matches(wifiConfiguration);
        }
        Logger.d("AccessPoint", "isInfoForThisAccessPoint 0004");
        return this.ssid.equals(removeDoubleQuotes(wifiInfo.getSSID()));
    }

    public boolean isSaved() {
        Logger.d("AccessPoint", "isSaved 0001");
        return this.networkId != -1;
    }

    void loadConfig(WifiConfiguration wifiConfiguration) {
        Logger.d("AccessPoint", "loadConfig 0001");
        if (wifiConfiguration.isPasspoint()) {
            Logger.d("AccessPoint", "loadConfig 0002");
            this.ssid = wifiConfiguration.providerFriendlyName;
        } else {
            Logger.d("AccessPoint", "loadConfig 0003");
            this.ssid = wifiConfiguration.SSID == null ? "" : removeDoubleQuotes(wifiConfiguration.SSID);
        }
        this.bssid = wifiConfiguration.BSSID;
        this.security = getSecurity(wifiConfiguration);
        this.networkId = wifiConfiguration.networkId;
        this.mConfig = wifiConfiguration;
        Logger.d("AccessPoint", "loadConfig 0004");
    }

    private void initWithScanResult(ScanResult scanResult) {
        Logger.d("AccessPoint", "initWithScanResult 0001");
        this.ssid = scanResult.SSID;
        this.bssid = scanResult.BSSID;
        this.security = getSecurity(scanResult);
        this.mWpsAvailable = this.security != 3 && scanResult.capabilities.contains("WPS");
        if (this.security == 2) {
            Logger.d("AccessPoint", "initWithScanResult 0002");
            this.pskType = getPskType(scanResult);
        }
        this.mRssi = scanResult.level;
        Logger.d("AccessPoint", "initWithScanResult 0003");
    }

    public void saveWifiState(Bundle bundle) {
        Logger.d("AccessPoint", "saveWifiState 0001");
        if (this.ssid != null) {
            Logger.d("AccessPoint", "saveWifiState 0002");
            bundle.putString("key_ssid", getSsidStr());
        }
        bundle.putInt("key_security", this.security);
        bundle.putInt("key_psktype", this.pskType);
        if (this.mConfig != null) {
            Logger.d("AccessPoint", "saveWifiState 0003");
            bundle.putParcelable("key_config", this.mConfig);
        }
        bundle.putParcelable("key_wifiinfo", this.mInfo);
        bundle.putParcelableArrayList("key_scanresultcache", new ArrayList<>(this.mScanResultCache.snapshot().values()));
        if (this.mNetworkInfo != null) {
            Logger.d("AccessPoint", "saveWifiState 0004");
            bundle.putParcelable("key_networkinfo", this.mNetworkInfo);
        }
        Logger.d("AccessPoint", "saveWifiState 0005");
    }

    public void setListener(AccessPointListener accessPointListener) {
        Logger.d("AccessPoint", "setListener 0001");
        this.mAccessPointListener = accessPointListener;
        Logger.d("AccessPoint", "setListener 0002");
    }

    boolean update(ScanResult scanResult) {
        Logger.d("AccessPoint", "update 0001");
        if (matches(scanResult)) {
            Logger.d("AccessPoint", "update 0002");
            this.mScanResultCache.get(scanResult.BSSID);
            this.mScanResultCache.put(scanResult.BSSID, scanResult);
            int level = getLevel();
            updateRssi();
            int level2 = getLevel();
            if (level2 > 0 && level2 != level && this.mAccessPointListener != null) {
                Logger.d("AccessPoint", "update 0003");
                this.mAccessPointListener.onLevelChanged(this);
            }
            if (this.security == 2) {
                Logger.d("AccessPoint", "update 0004");
                this.pskType = getPskType(scanResult);
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

    boolean update(WifiConfiguration wifiConfiguration, WifiInfo wifiInfo, NetworkInfo networkInfo) {
        boolean z;
        Logger.d("AccessPoint", "update 0008");
        int level = getLevel();
        if (wifiInfo != null && isInfoForThisAccessPoint(wifiConfiguration, wifiInfo)) {
            Logger.d("AccessPoint", "update 0009");
            z = this.mInfo == null;
            if (this.mConfig != wifiConfiguration) {
                Logger.d("AccessPoint", "update 0010");
                update(wifiConfiguration);
            }
            if (this.mRssi != wifiInfo.getRssi() && wifiInfo.getRssi() != -127) {
                Logger.d("AccessPoint", "update 0011");
                this.mRssi = wifiInfo.getRssi();
            } else {
                if (this.mNetworkInfo != null && networkInfo != null && this.mNetworkInfo.getDetailedState() != networkInfo.getDetailedState()) {
                    Logger.d("AccessPoint", "update 0012");
                }
                this.mInfo = wifiInfo;
                this.mNetworkInfo = networkInfo;
            }
            z = true;
            this.mInfo = wifiInfo;
            this.mNetworkInfo = networkInfo;
        } else if (this.mInfo != null) {
            Logger.d("AccessPoint", "update 0013");
            this.mInfo = null;
            this.mNetworkInfo = null;
            z = true;
        } else {
            z = false;
        }
        if (z && this.mAccessPointListener != null) {
            Logger.d("AccessPoint", "update 0014");
            this.mAccessPointListener.onAccessPointChanged(this);
            if (level != getLevel()) {
                this.mAccessPointListener.onLevelChanged(this);
            }
        }
        Logger.d("AccessPoint", "update 0015");
        return z;
    }

    void update(WifiConfiguration wifiConfiguration) {
        Logger.d("AccessPoint", "update 0016");
        this.mConfig = wifiConfiguration;
        this.networkId = wifiConfiguration != null ? wifiConfiguration.networkId : -1;
        if (this.mAccessPointListener != null) {
            Logger.d("AccessPoint", "update 0017");
            this.mAccessPointListener.onAccessPointChanged(this);
        }
        Logger.d("AccessPoint", "update 0018");
    }

    void setRssi(int i) {
        Logger.d("AccessPoint", "setRssi 0001");
        this.mRssi = i;
        Logger.d("AccessPoint", "setRssi 0002");
    }

    public boolean isReachable() {
        return this.mRssi != Integer.MIN_VALUE;
    }

    public static String getSummary(Context context, String str, NetworkInfo.DetailedState detailedState, boolean z, String str2) {
        Network currentNetwork;
        Logger.d("AccessPoint", "getSummary 0002");
        if (detailedState == NetworkInfo.DetailedState.CONNECTED && str == null) {
            Logger.d("AccessPoint", "getSummary 0003");
            if (!TextUtils.isEmpty(str2)) {
                Logger.d("AccessPoint", "getSummary 0004");
                return String.format(context.getString(R.string.connected_via_passpoint), str2);
            }
            if (z) {
                Logger.d("AccessPoint", "getSummary 0005");
                return context.getString(R.string.connected_via_wfa);
            }
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
            Logger.d("AccessPoint", "getSummary 0006");
            try {
                currentNetwork = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi")).getCurrentNetwork();
            } catch (RemoteException e) {
                Logger.d("AccessPoint", "getSummary 0007");
                Logger.d("AccessPoint", "RemoteException", e);
                currentNetwork = null;
            }
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork);
            if (networkCapabilities != null && !networkCapabilities.hasCapability(16)) {
                Logger.d("AccessPoint", "getSummary 0008");
                return context.getString(R.string.wifi_connected_no_internet);
            }
        }
        String[] stringArray = context.getResources().getStringArray(str == null ? R.array.wifi_status : R.array.wifi_status_with_ssid);
        int iOrdinal = detailedState.ordinal();
        if (iOrdinal >= stringArray.length || stringArray[iOrdinal].length() == 0) {
            Logger.d("AccessPoint", "getSummary 0009");
            return "";
        }
        Logger.d("AccessPoint", "getSummary 0010");
        return String.format(stringArray[iOrdinal], str);
    }

    public static String getSummary(Context context, NetworkInfo.DetailedState detailedState, boolean z) {
        Logger.d("AccessPoint", "getSummary 0011");
        return getSummary(context, null, detailedState, z, null);
    }

    public static String getSummary(Context context, NetworkInfo.DetailedState detailedState, boolean z, String str) {
        Logger.d("AccessPoint", "getSummary 0012");
        return getSummary(context, null, detailedState, z, str);
    }

    public static String convertToQuotedString(String str) {
        Logger.d("AccessPoint", "convertToQuotedString 0001");
        return "\"" + str + "\"";
    }

    private static int getPskType(ScanResult scanResult) {
        Logger.d("AccessPoint", "getPskType 0001");
        boolean zContains = scanResult.capabilities.contains("WPA-PSK");
        boolean zContains2 = scanResult.capabilities.contains("WPA2-PSK");
        if (zContains2 && zContains) {
            Logger.d("AccessPoint", "getPskType 0002");
            return 3;
        }
        if (zContains2) {
            Logger.d("AccessPoint", "getPskType 0003");
            return 2;
        }
        if (zContains) {
            Logger.d("AccessPoint", "getPskType 0004");
            return 1;
        }
        Logger.d("AccessPoint", "getPskType 0005");
        Logger.w("AccessPoint", "Received abnormal flag string: " + scanResult.capabilities);
        return 0;
    }

    private static int getSecurity(ScanResult scanResult) {
        Logger.d("AccessPoint", "getSecurity 0001");
        if (scanResult.capabilities.contains("WEP")) {
            Logger.d("AccessPoint", "getSecurity 0002");
            return 1;
        }
        if (scanResult.capabilities.contains("PSK")) {
            Logger.d("AccessPoint", "getSecurity 0003");
            return 2;
        }
        if (scanResult.capabilities.contains("EAP")) {
            Logger.d("AccessPoint", "getSecurity 0004");
            return 3;
        }
        Logger.d("AccessPoint", "getSecurity 0005");
        return 0;
    }

    static int getSecurity(WifiConfiguration wifiConfiguration) {
        Logger.d("AccessPoint", "getSecurity 0006");
        if (wifiConfiguration.allowedKeyManagement.get(1)) {
            Logger.d("AccessPoint", "getSecurity 0007");
            return 2;
        }
        if (wifiConfiguration.allowedKeyManagement.get(2) || wifiConfiguration.allowedKeyManagement.get(3)) {
            Logger.d("AccessPoint", "getSecurity 0008");
            return 3;
        }
        Logger.d("AccessPoint", "getSecurity 0009");
        return wifiConfiguration.wepKeys[0] != null ? 1 : 0;
    }

    public static String securityToString(int i, int i2) {
        Logger.d("AccessPoint", "securityToString 0001");
        if (i == 1) {
            Logger.d("AccessPoint", "securityToString 0002");
            return "WEP";
        }
        if (i == 2) {
            Logger.d("AccessPoint", "securityToString 0003");
            if (i2 == 1) {
                Logger.d("AccessPoint", "securityToString 0004");
                return "WPA";
            }
            if (i2 == 2) {
                Logger.d("AccessPoint", "securityToString 0005");
                return "WPA2";
            }
            if (i2 == 3) {
                Logger.d("AccessPoint", "securityToString 0006");
                return "WPA_WPA2";
            }
            Logger.d("AccessPoint", "securityToString 0007");
            return "PSK";
        }
        if (i == 3) {
            Logger.d("AccessPoint", "securityToString 0008");
            return "EAP";
        }
        Logger.d("AccessPoint", "securityToString 0009");
        return "NONE";
    }

    static String removeDoubleQuotes(String str) {
        Logger.d("AccessPoint", "removeDoubleQuotes 0001");
        if (TextUtils.isEmpty(str)) {
            Logger.d("AccessPoint", "removeDoubleQuotes 0002");
            return "";
        }
        int length = str.length();
        if (length > 1 && str.charAt(0) == '\"') {
            int i = length - 1;
            if (str.charAt(i) == '\"') {
                Logger.d("AccessPoint", "removeDoubleQuotes 0003");
                return str.substring(1, i);
            }
        }
        Logger.d("AccessPoint", "removeDoubleQuotes 0004");
        return str;
    }
}
