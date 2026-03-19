package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.UserManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConfigurationMap {
    private final UserManager mUserManager;
    private final Map<Integer, WifiConfiguration> mPerID = new HashMap();
    private final Map<Integer, WifiConfiguration> mPerIDForCurrentUser = new HashMap();
    private final Map<ScanResultMatchInfo, WifiConfiguration> mScanResultMatchInfoMapForCurrentUser = new HashMap();
    private int mCurrentUserId = 0;

    ConfigurationMap(UserManager userManager) {
        this.mUserManager = userManager;
    }

    public WifiConfiguration put(WifiConfiguration wifiConfiguration) {
        WifiConfiguration wifiConfigurationPut = this.mPerID.put(Integer.valueOf(wifiConfiguration.networkId), wifiConfiguration);
        if (WifiConfigurationUtil.isVisibleToAnyProfile(wifiConfiguration, this.mUserManager.getProfiles(this.mCurrentUserId))) {
            this.mPerIDForCurrentUser.put(Integer.valueOf(wifiConfiguration.networkId), wifiConfiguration);
            this.mScanResultMatchInfoMapForCurrentUser.put(ScanResultMatchInfo.fromWifiConfiguration(wifiConfiguration), wifiConfiguration);
        }
        return wifiConfigurationPut;
    }

    public WifiConfiguration remove(int i) {
        WifiConfiguration wifiConfigurationRemove = this.mPerID.remove(Integer.valueOf(i));
        if (wifiConfigurationRemove == null) {
            return null;
        }
        this.mPerIDForCurrentUser.remove(Integer.valueOf(i));
        Iterator<Map.Entry<ScanResultMatchInfo, WifiConfiguration>> it = this.mScanResultMatchInfoMapForCurrentUser.entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            if (it.next().getValue().networkId == i) {
                it.remove();
                break;
            }
        }
        return wifiConfigurationRemove;
    }

    public void clear() {
        this.mPerID.clear();
        this.mPerIDForCurrentUser.clear();
        this.mScanResultMatchInfoMapForCurrentUser.clear();
    }

    public void setNewUser(int i) {
        this.mCurrentUserId = i;
    }

    public WifiConfiguration getForAllUsers(int i) {
        return this.mPerID.get(Integer.valueOf(i));
    }

    public WifiConfiguration getForCurrentUser(int i) {
        return this.mPerIDForCurrentUser.get(Integer.valueOf(i));
    }

    public int sizeForAllUsers() {
        return this.mPerID.size();
    }

    public int sizeForCurrentUser() {
        return this.mPerIDForCurrentUser.size();
    }

    public WifiConfiguration getByConfigKeyForCurrentUser(String str) {
        if (str == null) {
            return null;
        }
        for (WifiConfiguration wifiConfiguration : this.mPerIDForCurrentUser.values()) {
            if (wifiConfiguration.configKey().equals(str)) {
                return wifiConfiguration;
            }
        }
        return null;
    }

    public WifiConfiguration getByScanResultForCurrentUser(ScanResult scanResult) {
        return this.mScanResultMatchInfoMapForCurrentUser.get(ScanResultMatchInfo.fromScanResult(scanResult));
    }

    public Collection<WifiConfiguration> valuesForAllUsers() {
        return this.mPerID.values();
    }

    public Collection<WifiConfiguration> valuesForCurrentUser() {
        return this.mPerIDForCurrentUser.values();
    }
}
