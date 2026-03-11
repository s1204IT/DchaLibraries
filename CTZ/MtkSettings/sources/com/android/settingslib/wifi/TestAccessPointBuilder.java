package com.android.settingslib.wifi;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Parcelable;
import java.util.ArrayList;

public class TestAccessPointBuilder {
    private static final int MAX_RSSI = -55;
    private static final int MIN_RSSI = -100;
    Context mContext;
    private ArrayList<ScanResult> mScanResults;
    private ArrayList<TimestampedScoredNetwork> mScoredNetworkCache;
    private WifiConfiguration mWifiConfig;
    private WifiInfo mWifiInfo;
    private String mBssid = null;
    private int mSpeed = 0;
    private int mRssi = AccessPoint.UNREACHABLE_RSSI;
    private int mNetworkId = -1;
    private String ssid = "TestSsid";
    private NetworkInfo mNetworkInfo = null;
    private String mFqdn = null;
    private String mProviderFriendlyName = null;
    private int mSecurity = 0;
    private boolean mIsCarrierAp = false;
    private String mCarrierName = null;

    public TestAccessPointBuilder(Context context) {
        this.mContext = context;
    }

    public AccessPoint build() {
        Bundle bundle = new Bundle();
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.networkId = this.mNetworkId;
        wifiConfiguration.BSSID = this.mBssid;
        bundle.putString("key_ssid", this.ssid);
        bundle.putParcelable("key_config", wifiConfiguration);
        bundle.putParcelable("key_networkinfo", this.mNetworkInfo);
        bundle.putParcelable("key_wifiinfo", this.mWifiInfo);
        if (this.mFqdn != null) {
            bundle.putString("key_fqdn", this.mFqdn);
        }
        if (this.mProviderFriendlyName != null) {
            bundle.putString("key_provider_friendly_name", this.mProviderFriendlyName);
        }
        if (this.mScanResults != null) {
            bundle.putParcelableArray("key_scanresults", (Parcelable[]) this.mScanResults.toArray(new Parcelable[this.mScanResults.size()]));
        }
        if (this.mScoredNetworkCache != null) {
            bundle.putParcelableArrayList("key_scorednetworkcache", this.mScoredNetworkCache);
        }
        bundle.putInt("key_security", this.mSecurity);
        bundle.putInt("key_speed", this.mSpeed);
        bundle.putBoolean("key_is_carrier_ap", this.mIsCarrierAp);
        if (this.mCarrierName != null) {
            bundle.putString("key_carrier_name", this.mCarrierName);
        }
        AccessPoint accessPoint = new AccessPoint(this.mContext, bundle);
        accessPoint.setRssi(this.mRssi);
        return accessPoint;
    }

    public TestAccessPointBuilder setActive(boolean z) {
        if (z) {
            this.mNetworkInfo = new NetworkInfo(8, 8, "TestNetwork", "TestNetwork");
        } else {
            this.mNetworkInfo = null;
        }
        return this;
    }

    public TestAccessPointBuilder setLevel(int i) {
        if (i == 0) {
            this.mRssi = MIN_RSSI;
        } else if (i >= 5) {
            this.mRssi = MAX_RSSI;
        } else {
            this.mRssi = (int) (((i * 45.0f) / 4.0f) - 100.0f);
        }
        return this;
    }

    public TestAccessPointBuilder setNetworkInfo(NetworkInfo networkInfo) {
        this.mNetworkInfo = networkInfo;
        return this;
    }

    public TestAccessPointBuilder setRssi(int i) {
        this.mRssi = i;
        return this;
    }

    public TestAccessPointBuilder setSpeed(int i) {
        this.mSpeed = i;
        return this;
    }

    public TestAccessPointBuilder setReachable(boolean z) {
        if (z) {
            if (this.mRssi == Integer.MIN_VALUE) {
                this.mRssi = MIN_RSSI;
            }
        } else {
            this.mRssi = AccessPoint.UNREACHABLE_RSSI;
        }
        return this;
    }

    public TestAccessPointBuilder setSaved(boolean z) {
        if (z) {
            this.mNetworkId = 1;
        } else {
            this.mNetworkId = -1;
        }
        return this;
    }

    public TestAccessPointBuilder setSecurity(int i) {
        this.mSecurity = i;
        return this;
    }

    public TestAccessPointBuilder setSsid(String str) {
        this.ssid = str;
        return this;
    }

    public TestAccessPointBuilder setFqdn(String str) {
        this.mFqdn = str;
        return this;
    }

    public TestAccessPointBuilder setProviderFriendlyName(String str) {
        this.mProviderFriendlyName = str;
        return this;
    }

    public TestAccessPointBuilder setWifiInfo(WifiInfo wifiInfo) {
        this.mWifiInfo = wifiInfo;
        return this;
    }

    public TestAccessPointBuilder setNetworkId(int i) {
        this.mNetworkId = i;
        return this;
    }

    public TestAccessPointBuilder setBssid(String str) {
        this.mBssid = str;
        return this;
    }

    public TestAccessPointBuilder setScanResults(ArrayList<ScanResult> arrayList) {
        this.mScanResults = arrayList;
        return this;
    }

    public TestAccessPointBuilder setIsCarrierAp(boolean z) {
        this.mIsCarrierAp = z;
        return this;
    }

    public TestAccessPointBuilder setCarrierName(String str) {
        this.mCarrierName = str;
        return this;
    }

    public TestAccessPointBuilder setScoredNetworkCache(ArrayList<TimestampedScoredNetwork> arrayList) {
        this.mScoredNetworkCache = arrayList;
        return this;
    }
}
