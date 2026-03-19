package com.android.server.wifi;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative;
import java.util.ArrayList;

public class WifiConnectivityHelper {

    @VisibleForTesting
    public static int INVALID_LIST_SIZE = -1;
    private static final String TAG = "WifiConnectivityHelper";
    private boolean mFirmwareRoamingSupported = false;
    private int mMaxNumBlacklistBssid = INVALID_LIST_SIZE;
    private int mMaxNumWhitelistSsid = INVALID_LIST_SIZE;
    private final WifiNative mWifiNative;

    WifiConnectivityHelper(WifiNative wifiNative) {
        this.mWifiNative = wifiNative;
    }

    public boolean getFirmwareRoamingInfo() {
        this.mFirmwareRoamingSupported = false;
        this.mMaxNumBlacklistBssid = INVALID_LIST_SIZE;
        this.mMaxNumWhitelistSsid = INVALID_LIST_SIZE;
        int supportedFeatureSet = this.mWifiNative.getSupportedFeatureSet(this.mWifiNative.getClientInterfaceName());
        Log.d(TAG, "Firmware supported feature set: " + Integer.toHexString(supportedFeatureSet));
        if ((supportedFeatureSet & 8388608) == 0) {
            Log.d(TAG, "Firmware roaming is not supported");
            return true;
        }
        WifiNative.RoamingCapabilities roamingCapabilities = new WifiNative.RoamingCapabilities();
        if (this.mWifiNative.getRoamingCapabilities(this.mWifiNative.getClientInterfaceName(), roamingCapabilities)) {
            if (roamingCapabilities.maxBlacklistSize < 0 || roamingCapabilities.maxWhitelistSize < 0) {
                Log.e(TAG, "Invalid firmware roaming capabilities: max num blacklist bssid=" + roamingCapabilities.maxBlacklistSize + " max num whitelist ssid=" + roamingCapabilities.maxWhitelistSize);
            } else {
                this.mFirmwareRoamingSupported = true;
                this.mMaxNumBlacklistBssid = roamingCapabilities.maxBlacklistSize;
                this.mMaxNumWhitelistSsid = roamingCapabilities.maxWhitelistSize;
                Log.d(TAG, "Firmware roaming supported with capabilities: max num blacklist bssid=" + this.mMaxNumBlacklistBssid + " max num whitelist ssid=" + this.mMaxNumWhitelistSsid);
                return true;
            }
        } else {
            Log.e(TAG, "Failed to get firmware roaming capabilities");
        }
        return false;
    }

    public boolean isFirmwareRoamingSupported() {
        return this.mFirmwareRoamingSupported;
    }

    public int getMaxNumBlacklistBssid() {
        if (this.mFirmwareRoamingSupported) {
            return this.mMaxNumBlacklistBssid;
        }
        Log.e(TAG, "getMaxNumBlacklistBssid: Firmware roaming is not supported");
        return INVALID_LIST_SIZE;
    }

    public int getMaxNumWhitelistSsid() {
        if (this.mFirmwareRoamingSupported) {
            return this.mMaxNumWhitelistSsid;
        }
        Log.e(TAG, "getMaxNumWhitelistSsid: Firmware roaming is not supported");
        return INVALID_LIST_SIZE;
    }

    public boolean setFirmwareRoamingConfiguration(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        if (!this.mFirmwareRoamingSupported) {
            Log.e(TAG, "Firmware roaming is not supported");
            return false;
        }
        if (arrayList == null || arrayList2 == null) {
            Log.e(TAG, "Invalid firmware roaming configuration settings");
            return false;
        }
        int size = arrayList.size();
        int size2 = arrayList2.size();
        if (size > this.mMaxNumBlacklistBssid || size2 > this.mMaxNumWhitelistSsid) {
            Log.e(TAG, "Invalid BSSID blacklist size " + size + " SSID whitelist size " + size2 + ". Max blacklist size: " + this.mMaxNumBlacklistBssid + ", max whitelist size: " + this.mMaxNumWhitelistSsid);
            return false;
        }
        WifiNative.RoamingConfig roamingConfig = new WifiNative.RoamingConfig();
        roamingConfig.blacklistBssids = arrayList;
        roamingConfig.whitelistSsids = arrayList2;
        return this.mWifiNative.configureRoaming(this.mWifiNative.getClientInterfaceName(), roamingConfig);
    }

    public void removeNetworkIfCurrent(int i) {
        this.mWifiNative.removeNetworkIfCurrent(this.mWifiNative.getClientInterfaceName(), i);
    }
}
