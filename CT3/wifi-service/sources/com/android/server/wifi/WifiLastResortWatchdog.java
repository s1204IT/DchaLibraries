package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import android.util.Pair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WifiLastResortWatchdog {
    public static final String BSSID_ANY = "any";
    private static final boolean DBG = true;
    public static final int FAILURE_CODE_ASSOCIATION = 1;
    public static final int FAILURE_CODE_AUTHENTICATION = 2;
    public static final int FAILURE_CODE_DHCP = 3;
    public static final int FAILURE_THRESHOLD = 7;
    public static final int MAX_BSSID_AGE = 10;
    private static final String TAG = "WifiLastResortWatchdog";
    private static final boolean VDBG = false;
    private WifiMetrics mWifiMetrics;
    private Map<String, AvailableNetworkFailureCount> mRecentAvailableNetworks = new HashMap();
    private Map<String, Pair<AvailableNetworkFailureCount, Integer>> mSsidFailureCount = new HashMap();
    private boolean mWifiIsConnected = false;
    private boolean mWatchdogAllowedToTrigger = true;

    WifiLastResortWatchdog(WifiMetrics wifiMetrics) {
        this.mWifiMetrics = wifiMetrics;
    }

    public void updateAvailableNetworks(List<Pair<ScanDetail, WifiConfiguration>> availableNetworks) {
        Pair<AvailableNetworkFailureCount, Integer> ssidFailsAndApCount;
        if (availableNetworks != null) {
            for (Pair<ScanDetail, WifiConfiguration> pair : availableNetworks) {
                ScanDetail scanDetail = (ScanDetail) pair.first;
                WifiConfiguration config = (WifiConfiguration) pair.second;
                ScanResult scanResult = scanDetail.getScanResult();
                if (scanResult != null) {
                    String bssid = scanResult.BSSID;
                    String ssid = "\"" + scanDetail.getSSID() + "\"";
                    AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(bssid);
                    if (availableNetworkFailureCount == null) {
                        availableNetworkFailureCount = new AvailableNetworkFailureCount(config);
                        availableNetworkFailureCount.ssid = ssid;
                        Pair<AvailableNetworkFailureCount, Integer> ssidFailsAndApCount2 = this.mSsidFailureCount.get(ssid);
                        if (ssidFailsAndApCount2 == null) {
                            ssidFailsAndApCount = Pair.create(new AvailableNetworkFailureCount(config), 1);
                            setWatchdogTriggerEnabled(true);
                        } else {
                            Integer numberOfAps = (Integer) ssidFailsAndApCount2.second;
                            ssidFailsAndApCount = Pair.create((AvailableNetworkFailureCount) ssidFailsAndApCount2.first, Integer.valueOf(numberOfAps.intValue() + 1));
                        }
                        this.mSsidFailureCount.put(ssid, ssidFailsAndApCount);
                    }
                    if (config != null) {
                        availableNetworkFailureCount.config = config;
                    }
                    availableNetworkFailureCount.age = -1;
                    this.mRecentAvailableNetworks.put(bssid, availableNetworkFailureCount);
                }
            }
        }
        Iterator<Map.Entry<String, AvailableNetworkFailureCount>> it = this.mRecentAvailableNetworks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, AvailableNetworkFailureCount> entry = it.next();
            if (entry.getValue().age < 9) {
                entry.getValue().age++;
            } else {
                String ssid2 = entry.getValue().ssid;
                Pair<AvailableNetworkFailureCount, Integer> ssidFails = this.mSsidFailureCount.get(ssid2);
                if (ssidFails != null) {
                    Integer apCount = Integer.valueOf(((Integer) ssidFails.second).intValue() - 1);
                    if (apCount.intValue() > 0) {
                        this.mSsidFailureCount.put(ssid2, Pair.create((AvailableNetworkFailureCount) ssidFails.first, apCount));
                    } else {
                        this.mSsidFailureCount.remove(ssid2);
                    }
                } else {
                    Log.d(TAG, "updateAvailableNetworks: SSID to AP count mismatch for " + ssid2);
                }
                it.remove();
            }
        }
    }

    public boolean noteConnectionFailureAndTriggerIfNeeded(String ssid, String bssid, int reason) {
        updateFailureCountForNetwork(ssid, bssid, reason);
        boolean isRestartNeeded = checkTriggerCondition();
        if (isRestartNeeded) {
            setWatchdogTriggerEnabled(false);
            restartWifiStack();
            incrementWifiMetricsTriggerCounts();
            clearAllFailureCounts();
        }
        return isRestartNeeded;
    }

    public void connectedStateTransition(boolean isEntering) {
        this.mWifiIsConnected = isEntering;
        if (!isEntering) {
            return;
        }
        clearAllFailureCounts();
        setWatchdogTriggerEnabled(true);
    }

    private void updateFailureCountForNetwork(String ssid, String bssid, int reason) {
        if (BSSID_ANY.equals(bssid)) {
            incrementSsidFailureCount(ssid, reason);
        } else {
            incrementBssidFailureCount(ssid, bssid, reason);
        }
    }

    private void incrementSsidFailureCount(String ssid, int reason) {
        Pair<AvailableNetworkFailureCount, Integer> ssidFails = this.mSsidFailureCount.get(ssid);
        if (ssidFails == null) {
            Log.v(TAG, "updateFailureCountForNetwork: No networks for ssid = " + ssid);
        } else {
            AvailableNetworkFailureCount failureCount = (AvailableNetworkFailureCount) ssidFails.first;
            failureCount.incrementFailureCount(reason);
        }
    }

    private void incrementBssidFailureCount(String ssid, String bssid, int reason) {
        AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(bssid);
        if (availableNetworkFailureCount == null) {
            Log.d(TAG, "updateFailureCountForNetwork: Unable to find Network [" + ssid + ", " + bssid + "]");
        } else {
            if (!availableNetworkFailureCount.ssid.equals(ssid)) {
                Log.d(TAG, "updateFailureCountForNetwork: Failed connection attempt has wrong ssid. Failed [" + ssid + ", " + bssid + "], buffered [" + availableNetworkFailureCount.ssid + ", " + bssid + "]");
                return;
            }
            if (availableNetworkFailureCount.config == null) {
            }
            availableNetworkFailureCount.incrementFailureCount(reason);
            incrementSsidFailureCount(ssid, reason);
        }
    }

    private boolean checkTriggerCondition() {
        if (this.mWifiIsConnected || !this.mWatchdogAllowedToTrigger) {
            return false;
        }
        boolean atleastOneNetworkHasEverConnected = false;
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            if (entry.getValue().config != null && entry.getValue().config.getNetworkSelectionStatus().getHasEverConnected()) {
                atleastOneNetworkHasEverConnected = true;
            }
            if (!isOverFailureThreshold(entry.getKey())) {
                return false;
            }
        }
        return atleastOneNetworkHasEverConnected;
    }

    private void restartWifiStack() {
        Log.i(TAG, "Triggered.");
        Log.d(TAG, toString());
    }

    private void incrementWifiMetricsTriggerCounts() {
        this.mWifiMetrics.incrementNumLastResortWatchdogTriggers();
        this.mWifiMetrics.addCountToNumLastResortWatchdogAvailableNetworksTotal(this.mSsidFailureCount.size());
        int badAuth = 0;
        int badAssoc = 0;
        int badDhcp = 0;
        for (Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry : this.mSsidFailureCount.entrySet()) {
            badAuth += ((AvailableNetworkFailureCount) entry.getValue().first).authenticationFailure >= 7 ? 1 : 0;
            badAssoc += ((AvailableNetworkFailureCount) entry.getValue().first).associationRejection >= 7 ? 1 : 0;
            badDhcp += ((AvailableNetworkFailureCount) entry.getValue().first).dhcpFailure >= 7 ? 1 : 0;
        }
        if (badAuth > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAuthenticationNetworksTotal(badAuth);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAuthentication();
        }
        if (badAssoc > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAssociationNetworksTotal(badAssoc);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAssociation();
        }
        if (badDhcp <= 0) {
            return;
        }
        this.mWifiMetrics.addCountToNumLastResortWatchdogBadDhcpNetworksTotal(badDhcp);
        this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadDhcp();
    }

    private void clearAllFailureCounts() {
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            entry.getValue();
            entry.getValue().resetCounts();
        }
        Iterator entry$iterator = this.mSsidFailureCount.entrySet().iterator();
        while (entry$iterator.hasNext()) {
            AvailableNetworkFailureCount failureCount = (AvailableNetworkFailureCount) ((Map.Entry) entry$iterator.next()).getValue().first;
            failureCount.resetCounts();
        }
    }

    Map<String, AvailableNetworkFailureCount> getRecentAvailableNetworks() {
        return this.mRecentAvailableNetworks;
    }

    private void setWatchdogTriggerEnabled(boolean enable) {
        this.mWatchdogAllowedToTrigger = enable;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mWatchdogAllowedToTrigger: ").append(this.mWatchdogAllowedToTrigger);
        sb.append("\nmWifiIsConnected: ").append(this.mWifiIsConnected);
        sb.append("\nmRecentAvailableNetworks: ").append(this.mRecentAvailableNetworks.size());
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            sb.append("\n ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        sb.append("\nmSsidFailureCount:");
        for (Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry2 : this.mSsidFailureCount.entrySet()) {
            AvailableNetworkFailureCount failureCount = (AvailableNetworkFailureCount) entry2.getValue().first;
            Integer apCount = (Integer) entry2.getValue().second;
            sb.append("\n").append(entry2.getKey()).append(": ").append(apCount).append(", ").append(failureCount.toString());
        }
        return sb.toString();
    }

    public boolean isOverFailureThreshold(String bssid) {
        return getFailureCount(bssid, 1) >= 7 || getFailureCount(bssid, 2) >= 7 || getFailureCount(bssid, 3) >= 7;
    }

    public int getFailureCount(String bssid, int reason) {
        AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(bssid);
        if (availableNetworkFailureCount == null) {
            return 0;
        }
        String ssid = availableNetworkFailureCount.ssid;
        Pair<AvailableNetworkFailureCount, Integer> ssidFails = this.mSsidFailureCount.get(ssid);
        if (ssidFails == null) {
            Log.d(TAG, "getFailureCount: Could not find SSID count for " + ssid);
            return 0;
        }
        AvailableNetworkFailureCount failCount = (AvailableNetworkFailureCount) ssidFails.first;
        switch (reason) {
        }
        return 0;
    }

    public static class AvailableNetworkFailureCount {
        public WifiConfiguration config;
        public String ssid = "";
        public int associationRejection = 0;
        public int authenticationFailure = 0;
        public int dhcpFailure = 0;
        public int age = 0;

        AvailableNetworkFailureCount(WifiConfiguration configParam) {
            this.config = configParam;
        }

        public void incrementFailureCount(int reason) {
            switch (reason) {
                case 1:
                    this.associationRejection++;
                    break;
                case 2:
                    this.authenticationFailure++;
                    break;
                case 3:
                    this.dhcpFailure++;
                    break;
            }
        }

        void resetCounts() {
            this.associationRejection = 0;
            this.authenticationFailure = 0;
            this.dhcpFailure = 0;
        }

        public String toString() {
            return this.ssid + ", HasEverConnected: " + (this.config != null ? Boolean.valueOf(this.config.getNetworkSelectionStatus().getHasEverConnected()) : "null_config") + ", Failures: {Assoc: " + this.associationRejection + ", Auth: " + this.authenticationFailure + ", Dhcp: " + this.dhcpFailure + "}, Age: " + this.age;
        }
    }
}
