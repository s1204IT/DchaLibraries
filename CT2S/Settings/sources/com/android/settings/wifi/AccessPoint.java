package com.android.settings.wifi;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import java.util.Map;

class AccessPoint extends Preference {
    String bssid;
    private WifiConfiguration mConfig;
    private android.net.wifi.WifiInfo mInfo;
    private NetworkInfo mNetworkInfo;
    private int mRssi;
    ScanResult mScanResult;
    public LruCache<String, ScanResult> mScanResultCache;
    private long mSeen;
    private TextView mSummaryView;
    int networkId;
    PskType pskType;
    int security;
    boolean showSummary;
    String ssid;
    boolean wpsAvailable;
    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    private static final int[] STATE_NONE = new int[0];
    private static int[] wifi_signal_attributes = {R.attr.wifi_signal};

    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(1)) {
            return 2;
        }
        if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            return 3;
        }
        if (config.wapiPsk != null && config.wapiPsk.length() != 0) {
            return 4;
        }
        if (config.wapiRootCert == null || config.wapiUserCert == null || config.wapiRootCert.length() == 0 || config.wapiUserCert.length() == 0) {
            return config.wepKeys[0] == null ? 0 : 1;
        }
        return 5;
    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return 1;
        }
        if (result.capabilities.contains("PSK") && !result.capabilities.contains("WAPI-PSK")) {
            return 2;
        }
        if (result.capabilities.contains("EAP")) {
            return 3;
        }
        if (result.capabilities.contains("WAPI-PSK")) {
            return 4;
        }
        if (result.capabilities.contains("WAPI-CERT")) {
            return 5;
        }
        return 0;
    }

    public String getSecurityString(boolean concise) {
        Context context = getContext();
        switch (this.security) {
            case 1:
                return concise ? context.getString(R.string.wifi_security_short_wep) : context.getString(R.string.wifi_security_wep);
            case 2:
                switch (this.pskType) {
                    case WPA:
                        return concise ? context.getString(R.string.wifi_security_short_wpa) : context.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa2) : context.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) : context.getString(R.string.wifi_security_wpa_wpa2);
                    default:
                        return concise ? context.getString(R.string.wifi_security_short_psk_generic) : context.getString(R.string.wifi_security_psk_generic);
                }
            case 3:
                return concise ? context.getString(R.string.wifi_security_short_eap) : context.getString(R.string.wifi_security_eap);
            case 4:
                return concise ? context.getString(R.string.wifi_security_short_wapi_psk) : context.getString(R.string.wifi_security_wapi_psk);
            case 5:
                return concise ? context.getString(R.string.wifi_security_short_wapi_cert) : context.getString(R.string.wifi_security_wapi_cert);
            default:
                return concise ? "" : context.getString(R.string.wifi_security_none);
        }
    }

    private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        }
        if (wpa2) {
            return PskType.WPA2;
        }
        if (wpa) {
            return PskType.WPA;
        }
        Log.w("Settings.AccessPoint", "Received abnormal flag string: " + result.capabilities);
        return PskType.UNKNOWN;
    }

    AccessPoint(Context context, WifiConfiguration config) {
        super(context);
        this.networkId = -1;
        this.wpsAvailable = false;
        this.showSummary = true;
        this.pskType = PskType.UNKNOWN;
        this.mRssi = Integer.MAX_VALUE;
        this.mSeen = 0L;
        loadConfig(config);
        refresh();
    }

    AccessPoint(Context context, ScanResult result) {
        super(context);
        this.networkId = -1;
        this.wpsAvailable = false;
        this.showSummary = true;
        this.pskType = PskType.UNKNOWN;
        this.mRssi = Integer.MAX_VALUE;
        this.mSeen = 0L;
        loadResult(result);
        refresh();
    }

    AccessPoint(Context context, Bundle savedState) {
        super(context);
        this.networkId = -1;
        this.wpsAvailable = false;
        this.showSummary = true;
        this.pskType = PskType.UNKNOWN;
        this.mRssi = Integer.MAX_VALUE;
        this.mSeen = 0L;
        this.mConfig = (WifiConfiguration) savedState.getParcelable("key_config");
        if (this.mConfig != null) {
            loadConfig(this.mConfig);
        }
        this.mScanResult = (ScanResult) savedState.getParcelable("key_scanresult");
        if (this.mScanResult != null) {
            loadResult(this.mScanResult);
        }
        this.mInfo = (android.net.wifi.WifiInfo) savedState.getParcelable("key_wifiinfo");
        if (savedState.containsKey("key_networkinfo")) {
            this.mNetworkInfo = (NetworkInfo) savedState.getParcelable("key_networkinfo");
        }
        update(this.mInfo, this.mNetworkInfo);
    }

    public void saveWifiState(Bundle savedState) {
        savedState.putParcelable("key_config", this.mConfig);
        savedState.putParcelable("key_scanresult", this.mScanResult);
        savedState.putParcelable("key_wifiinfo", this.mInfo);
        if (this.mNetworkInfo != null) {
            savedState.putParcelable("key_networkinfo", this.mNetworkInfo);
        }
    }

    private void loadConfig(WifiConfiguration config) {
        this.ssid = config.SSID == null ? "" : removeDoubleQuotes(config.SSID);
        this.bssid = config.BSSID;
        this.security = getSecurity(config);
        this.networkId = config.networkId;
        this.mConfig = config;
    }

    private void loadResult(ScanResult result) {
        this.ssid = result.SSID;
        this.bssid = result.BSSID;
        this.security = getSecurity(result);
        this.wpsAvailable = this.security != 3 && result.capabilities.contains("WPS");
        if (this.security == 2) {
            this.pskType = getPskType(result);
        }
        this.mRssi = result.level;
        this.mScanResult = result;
        if (result.seen > this.mSeen) {
            this.mSeen = result.seen;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        updateIcon(getLevel(), getContext());
        this.mSummaryView = (TextView) view.findViewById(android.R.id.summary);
        this.mSummaryView.setVisibility(this.showSummary ? 0 : 8);
        notifyChanged();
    }

    protected void updateIcon(int level, Context context) {
        StateListDrawable sld;
        if (level == -1) {
            setIcon((Drawable) null);
            return;
        }
        Drawable drawable = getIcon();
        if (drawable == null && (sld = (StateListDrawable) context.getTheme().obtainStyledAttributes(wifi_signal_attributes).getDrawable(0)) != null) {
            sld.setState(this.security != 0 ? STATE_SECURED : STATE_NONE);
            drawable = sld.getCurrent();
            setIcon(drawable);
        }
        if (drawable != null) {
            drawable.setLevel(level);
        }
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof AccessPoint)) {
            return 1;
        }
        AccessPoint other = (AccessPoint) preference;
        if (isActive() && !other.isActive()) {
            return -1;
        }
        if (!isActive() && other.isActive()) {
            return 1;
        }
        if (this.mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        if (this.mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) {
            return 1;
        }
        if (this.networkId != -1 && other.networkId == -1) {
            return -1;
        }
        if (this.networkId == -1 && other.networkId != -1) {
            return 1;
        }
        int difference = WifiManager.compareSignalLevel(other.mRssi, this.mRssi);
        return difference == 0 ? this.ssid.compareToIgnoreCase(other.ssid) : difference;
    }

    public boolean equals(Object other) {
        return (other instanceof AccessPoint) && compareTo((Preference) other) == 0;
    }

    public int hashCode() {
        int result = this.mInfo != null ? 0 + (this.mInfo.hashCode() * 13) : 0;
        return result + (this.mRssi * 19) + (this.networkId * 23) + (this.ssid.hashCode() * 29);
    }

    boolean update(ScanResult result) {
        if (result.seen > this.mSeen) {
            this.mSeen = result.seen;
        }
        if (WifiSettings.mVerboseLogging > 0) {
            if (this.mScanResultCache == null) {
                this.mScanResultCache = new LruCache<>(32);
            }
            this.mScanResultCache.put(result.BSSID, result);
        }
        if (!this.ssid.equals(result.SSID) || this.security != getSecurity(result)) {
            return false;
        }
        if (WifiManager.compareSignalLevel(result.level, this.mRssi) > 0) {
            int oldLevel = getLevel();
            this.mRssi = result.level;
            if (getLevel() != oldLevel) {
                notifyChanged();
            }
        }
        if (this.security == 2) {
            this.pskType = getPskType(result);
        }
        this.mScanResult = result;
        refresh();
        return true;
    }

    private boolean isInfoForThisAccessPoint(android.net.wifi.WifiInfo info) {
        if (this.networkId != -1) {
            return this.networkId == info.getNetworkId();
        }
        return this.ssid.equals(removeDoubleQuotes(info.getSSID()));
    }

    void update(android.net.wifi.WifiInfo info, NetworkInfo networkInfo) {
        boolean reorder = false;
        if (info != null && isInfoForThisAccessPoint(info)) {
            reorder = this.mInfo == null;
            this.mRssi = info.getRssi();
            this.mInfo = info;
            this.mNetworkInfo = networkInfo;
            refresh();
        } else if (this.mInfo != null) {
            reorder = true;
            this.mInfo = null;
            this.mNetworkInfo = null;
            refresh();
        }
        if (reorder) {
            notifyHierarchyChanged();
        }
    }

    int getLevel() {
        if (this.mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(this.mRssi, 4);
    }

    WifiConfiguration getConfig() {
        return this.mConfig;
    }

    android.net.wifi.WifiInfo getInfo() {
        return this.mInfo;
    }

    NetworkInfo getNetworkInfo() {
        return this.mNetworkInfo;
    }

    NetworkInfo.DetailedState getState() {
        if (this.mNetworkInfo != null) {
            return this.mNetworkInfo.getDetailedState();
        }
        return null;
    }

    static String removeDoubleQuotes(String string) {
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
        if (this.mSummaryView != null) {
            this.mSummaryView.setVisibility(showSummary ? 0 : 8);
        }
    }

    private String getVisibilityStatus() {
        StringBuilder visibility = new StringBuilder();
        StringBuilder scans24GHz = null;
        StringBuilder scans5GHz = null;
        String bssid = null;
        long now = System.currentTimeMillis();
        if (this.mInfo != null) {
            bssid = this.mInfo.getBSSID();
            if (bssid != null) {
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
        if (this.mScanResultCache != null) {
            int rssi5 = WifiConfiguration.INVALID_RSSI;
            int rssi24 = WifiConfiguration.INVALID_RSSI;
            int num5 = 0;
            int num24 = 0;
            int numBlackListed = 0;
            int n24 = 0;
            int n5 = 0;
            Map<String, ScanResult> list = this.mScanResultCache.snapshot();
            for (ScanResult result : list.values()) {
                if (result.seen != 0) {
                    if (result.autoJoinStatus != 0) {
                        numBlackListed++;
                    }
                    if (result.frequency >= 4900 && result.frequency <= 5900) {
                        num5++;
                    } else if (result.frequency >= 2400 && result.frequency <= 2500) {
                        num24++;
                    }
                    if (now - result.seen <= 20000) {
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
                                if (result.autoJoinStatus != 0) {
                                    scans5GHz.append(",st=").append(result.autoJoinStatus);
                                }
                                if (result.numIpConfigFailures != 0) {
                                    scans5GHz.append(",ipf=").append(result.numIpConfigFailures);
                                }
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
                                if (result.autoJoinStatus != 0) {
                                    scans24GHz.append(",st=").append(result.autoJoinStatus);
                                }
                                if (result.numIpConfigFailures != 0) {
                                    scans24GHz.append(",ipf=").append(result.numIpConfigFailures);
                                }
                                scans24GHz.append("}");
                                n24++;
                            }
                        }
                    }
                }
            }
            visibility.append(" [");
            if (num24 > 0) {
                visibility.append("(").append(num24).append(")");
                if (n24 <= 4) {
                    if (scans24GHz != null) {
                        visibility.append(scans24GHz.toString());
                    }
                } else {
                    visibility.append("max=").append(rssi24);
                    if (scans24GHz != null) {
                        visibility.append(",").append(scans24GHz.toString());
                    }
                }
            }
            visibility.append(";");
            if (num5 > 0) {
                visibility.append("(").append(num5).append(")");
                if (n5 <= 4) {
                    if (scans5GHz != null) {
                        visibility.append(scans5GHz.toString());
                    }
                } else {
                    visibility.append("max=").append(rssi5);
                    if (scans5GHz != null) {
                        visibility.append(",").append(scans5GHz.toString());
                    }
                }
            }
            if (numBlackListed > 0) {
                visibility.append("!").append(numBlackListed);
            }
            visibility.append("]");
        } else if (this.mRssi != Integer.MAX_VALUE) {
            visibility.append(" rssi=");
            visibility.append(this.mRssi);
            if (this.mScanResult != null) {
                visibility.append(", f=");
                visibility.append(this.mScanResult.frequency);
            }
        }
        return visibility.toString();
    }

    boolean isActive() {
        return (this.mNetworkInfo == null || (this.networkId == -1 && this.mNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED)) ? false : true;
    }

    private void refresh() {
        setTitle(this.ssid);
        Context context = getContext();
        updateIcon(getLevel(), context);
        setSummary((CharSequence) null);
        StringBuilder summary = new StringBuilder();
        if (isActive()) {
            summary.append(Summary.get(context, getState(), this.networkId == -1));
        } else if (this.mConfig != null && this.mConfig.hasNoInternetAccess()) {
            summary.append(context.getString(R.string.wifi_no_internet));
        } else if (this.mConfig != null && ((this.mConfig.status == 1 && this.mConfig.disableReason != 0) || this.mConfig.autoJoinStatus >= 128)) {
            if (this.mConfig.autoJoinStatus >= 128) {
                if (this.mConfig.disableReason == 2) {
                    summary.append(context.getString(R.string.wifi_disabled_network_failure));
                } else if (this.mConfig.disableReason == 3) {
                    summary.append(context.getString(R.string.wifi_disabled_password_failure));
                } else {
                    summary.append(context.getString(R.string.wifi_disabled_wifi_failure));
                }
            } else {
                switch (this.mConfig.disableReason) {
                    case 0:
                    case 4:
                        summary.append(context.getString(R.string.wifi_disabled_generic));
                        break;
                    case 1:
                    case 2:
                        summary.append(context.getString(R.string.wifi_disabled_network_failure));
                        break;
                    case 3:
                        summary.append(context.getString(R.string.wifi_disabled_password_failure));
                        break;
                }
            }
        } else if (this.mRssi == Integer.MAX_VALUE) {
            summary.append(context.getString(R.string.wifi_not_in_range));
        } else if (this.mConfig != null) {
            summary.append(context.getString(R.string.wifi_remembered));
        }
        if (WifiSettings.mVerboseLogging > 0) {
            if (this.mInfo != null && this.mNetworkInfo != null) {
                summary.append(" f=" + Integer.toString(this.mInfo.getFrequency()));
            }
            summary.append(" " + getVisibilityStatus());
            if (this.mConfig != null && this.mConfig.autoJoinStatus > 0) {
                summary.append(" (" + this.mConfig.autoJoinStatus);
                if (this.mConfig.blackListTimestamp > 0) {
                    long now = System.currentTimeMillis();
                    long diff = (now - this.mConfig.blackListTimestamp) / 1000;
                    long sec = diff % 60;
                    long min = (diff / 60) % 60;
                    long hour = (min / 60) % 60;
                    summary.append(", ");
                    if (hour > 0) {
                        summary.append(Long.toString(hour) + "h ");
                    }
                    summary.append(Long.toString(min) + "m ");
                    summary.append(Long.toString(sec) + "s ");
                }
                summary.append(")");
            }
            if (this.mConfig != null && this.mConfig.numIpConfigFailures > 0) {
                summary.append(" ipf=").append(this.mConfig.numIpConfigFailures);
            }
            if (this.mConfig != null && this.mConfig.numConnectionFailures > 0) {
                summary.append(" cf=").append(this.mConfig.numConnectionFailures);
            }
            if (this.mConfig != null && this.mConfig.numAuthFailures > 0) {
                summary.append(" authf=").append(this.mConfig.numAuthFailures);
            }
            if (this.mConfig != null && this.mConfig.numNoInternetAccessReports > 0) {
                summary.append(" noInt=").append(this.mConfig.numNoInternetAccessReports);
            }
        }
        if (summary.length() > 0) {
            setSummary(summary.toString());
            setShowSummary(true);
        } else {
            setShowSummary(false);
        }
    }

    protected void generateOpenNetworkConfig() {
        if (this.security != 0) {
            throw new IllegalStateException();
        }
        if (this.mConfig == null) {
            this.mConfig = new WifiConfiguration();
            this.mConfig.SSID = convertToQuotedString(this.ssid);
            this.mConfig.allowedKeyManagement.set(0);
        }
    }
}
