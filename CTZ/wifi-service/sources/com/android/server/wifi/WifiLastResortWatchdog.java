package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WifiLastResortWatchdog {
    public static final String BSSID_ANY = "any";
    public static final String BUGREPORT_TITLE = "Wifi watchdog triggered";
    public static final int FAILURE_CODE_ASSOCIATION = 1;
    public static final int FAILURE_CODE_AUTHENTICATION = 2;
    public static final int FAILURE_CODE_DHCP = 3;
    public static final int FAILURE_THRESHOLD = 7;
    public static final int MAX_BSSID_AGE = 10;
    public static final double PROB_TAKE_BUGREPORT_DEFAULT = 0.08d;
    private static final String TAG = "WifiLastResortWatchdog";
    private Clock mClock;
    private SelfRecovery mSelfRecovery;
    private long mTimeLastTrigger;
    private WifiMetrics mWifiMetrics;
    private WifiStateMachine mWifiStateMachine;
    private Looper mWifiStateMachineLooper;
    private boolean mVerboseLoggingEnabled = false;
    private Map<String, AvailableNetworkFailureCount> mRecentAvailableNetworks = new HashMap();
    private Map<String, Pair<AvailableNetworkFailureCount, Integer>> mSsidFailureCount = new HashMap();
    private boolean mWifiIsConnected = false;
    private boolean mWatchdogAllowedToTrigger = true;
    private double mBugReportProbability = 0.08d;
    private boolean mWatchdogFixedWifi = true;

    WifiLastResortWatchdog(SelfRecovery selfRecovery, Clock clock, WifiMetrics wifiMetrics, WifiStateMachine wifiStateMachine, Looper looper) {
        this.mSelfRecovery = selfRecovery;
        this.mClock = clock;
        this.mWifiMetrics = wifiMetrics;
        this.mWifiStateMachine = wifiStateMachine;
        this.mWifiStateMachineLooper = looper;
    }

    public void updateAvailableNetworks(List<Pair<ScanDetail, WifiConfiguration>> list) {
        Pair<AvailableNetworkFailureCount, Integer> pairCreate;
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "updateAvailableNetworks: size = " + list.size());
        }
        if (list != null) {
            for (Pair<ScanDetail, WifiConfiguration> pair : list) {
                ScanDetail scanDetail = (ScanDetail) pair.first;
                WifiConfiguration wifiConfiguration = (WifiConfiguration) pair.second;
                ScanResult scanResult = scanDetail.getScanResult();
                if (scanResult != null) {
                    String str = scanResult.BSSID;
                    String str2 = "\"" + scanDetail.getSSID() + "\"";
                    if (this.mVerboseLoggingEnabled) {
                        Log.v(TAG, " " + str + ": " + scanDetail.getSSID());
                    }
                    AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(str);
                    if (availableNetworkFailureCount == null) {
                        availableNetworkFailureCount = new AvailableNetworkFailureCount(wifiConfiguration);
                        availableNetworkFailureCount.ssid = str2;
                        Pair<AvailableNetworkFailureCount, Integer> pair2 = this.mSsidFailureCount.get(str2);
                        if (pair2 == null) {
                            pairCreate = Pair.create(new AvailableNetworkFailureCount(wifiConfiguration), 1);
                            setWatchdogTriggerEnabled(true);
                        } else {
                            pairCreate = Pair.create((AvailableNetworkFailureCount) pair2.first, Integer.valueOf(((Integer) pair2.second).intValue() + 1));
                        }
                        this.mSsidFailureCount.put(str2, pairCreate);
                    }
                    if (wifiConfiguration != null) {
                        availableNetworkFailureCount.config = wifiConfiguration;
                    }
                    availableNetworkFailureCount.age = -1;
                    this.mRecentAvailableNetworks.put(str, availableNetworkFailureCount);
                }
            }
        }
        Iterator<Map.Entry<String, AvailableNetworkFailureCount>> it = this.mRecentAvailableNetworks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, AvailableNetworkFailureCount> next = it.next();
            if (next.getValue().age < 9) {
                next.getValue().age++;
            } else {
                String str3 = next.getValue().ssid;
                Pair<AvailableNetworkFailureCount, Integer> pair3 = this.mSsidFailureCount.get(str3);
                if (pair3 != null) {
                    Integer numValueOf = Integer.valueOf(((Integer) pair3.second).intValue() - 1);
                    if (numValueOf.intValue() > 0) {
                        this.mSsidFailureCount.put(str3, Pair.create((AvailableNetworkFailureCount) pair3.first, numValueOf));
                    } else {
                        this.mSsidFailureCount.remove(str3);
                    }
                } else {
                    Log.d(TAG, "updateAvailableNetworks: SSID to AP count mismatch for " + str3);
                }
                it.remove();
            }
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, toString());
        }
    }

    public boolean noteConnectionFailureAndTriggerIfNeeded(String str, String str2, int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "noteConnectionFailureAndTriggerIfNeeded: [" + str + ", " + str2 + ", " + i + "]");
        }
        updateFailureCountForNetwork(str, str2, i);
        if (!this.mWatchdogAllowedToTrigger) {
            this.mWifiMetrics.incrementWatchdogTotalConnectionFailureCountAfterTrigger();
            this.mWatchdogFixedWifi = false;
        }
        boolean zCheckTriggerCondition = checkTriggerCondition();
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "isRestartNeeded = " + zCheckTriggerCondition);
        }
        if (zCheckTriggerCondition) {
            setWatchdogTriggerEnabled(false);
            this.mWatchdogFixedWifi = true;
            Log.e(TAG, "Watchdog triggering recovery");
            this.mTimeLastTrigger = this.mClock.getElapsedSinceBootMillis();
            this.mSelfRecovery.trigger(0);
            incrementWifiMetricsTriggerCounts();
            clearAllFailureCounts();
        }
        return zCheckTriggerCondition;
    }

    public void connectedStateTransition(boolean z) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "connectedStateTransition: isEntering = " + z);
        }
        this.mWifiIsConnected = z;
        if (!z) {
            return;
        }
        if (!this.mWatchdogAllowedToTrigger && this.mWatchdogFixedWifi && checkIfAtleastOneNetworkHasEverConnected()) {
            takeBugReportWithCurrentProbability("Wifi fixed after restart");
            this.mWifiMetrics.incrementNumLastResortWatchdogSuccesses();
            this.mWifiMetrics.setWatchdogSuccessTimeDurationMs(this.mClock.getElapsedSinceBootMillis() - this.mTimeLastTrigger);
        }
        clearAllFailureCounts();
        setWatchdogTriggerEnabled(true);
    }

    private void takeBugReportWithCurrentProbability(final String str) {
        if (this.mBugReportProbability <= Math.random()) {
            return;
        }
        new Handler(this.mWifiStateMachineLooper).post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mWifiStateMachine.takeBugReport(WifiLastResortWatchdog.BUGREPORT_TITLE, str);
            }
        });
    }

    private void updateFailureCountForNetwork(String str, String str2, int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "updateFailureCountForNetwork: [" + str + ", " + str2 + ", " + i + "]");
        }
        if ("any".equals(str2)) {
            incrementSsidFailureCount(str, i);
        } else {
            incrementBssidFailureCount(str, str2, i);
        }
    }

    private void incrementSsidFailureCount(String str, int i) {
        Pair<AvailableNetworkFailureCount, Integer> pair = this.mSsidFailureCount.get(str);
        if (pair == null) {
            Log.d(TAG, "updateFailureCountForNetwork: No networks for ssid = " + str);
            return;
        }
        ((AvailableNetworkFailureCount) pair.first).incrementFailureCount(i);
    }

    private void incrementBssidFailureCount(String str, String str2, int i) {
        AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(str2);
        if (availableNetworkFailureCount == null) {
            Log.d(TAG, "updateFailureCountForNetwork: Unable to find Network [" + str + ", " + str2 + "]");
            return;
        }
        if (!availableNetworkFailureCount.ssid.equals(str)) {
            Log.d(TAG, "updateFailureCountForNetwork: Failed connection attempt has wrong ssid. Failed [" + str + ", " + str2 + "], buffered [" + availableNetworkFailureCount.ssid + ", " + str2 + "]");
            return;
        }
        if (availableNetworkFailureCount.config == null && this.mVerboseLoggingEnabled) {
            Log.v(TAG, "updateFailureCountForNetwork: network has no config [" + str + ", " + str2 + "]");
        }
        availableNetworkFailureCount.incrementFailureCount(i);
        incrementSsidFailureCount(str, i);
    }

    private boolean checkTriggerCondition() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "checkTriggerCondition.");
        }
        if (this.mWifiIsConnected || !this.mWatchdogAllowedToTrigger) {
            return false;
        }
        Iterator<Map.Entry<String, AvailableNetworkFailureCount>> it = this.mRecentAvailableNetworks.entrySet().iterator();
        while (it.hasNext()) {
            if (!isOverFailureThreshold(it.next().getKey())) {
                return false;
            }
        }
        boolean zCheckIfAtleastOneNetworkHasEverConnected = checkIfAtleastOneNetworkHasEverConnected();
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "checkTriggerCondition: return = " + zCheckIfAtleastOneNetworkHasEverConnected);
        }
        return checkIfAtleastOneNetworkHasEverConnected();
    }

    private boolean checkIfAtleastOneNetworkHasEverConnected() {
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            if (entry.getValue().config != null && entry.getValue().config.getNetworkSelectionStatus().getHasEverConnected()) {
                return true;
            }
        }
        return false;
    }

    private void incrementWifiMetricsTriggerCounts() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "incrementWifiMetricsTriggerCounts.");
        }
        this.mWifiMetrics.incrementNumLastResortWatchdogTriggers();
        this.mWifiMetrics.addCountToNumLastResortWatchdogAvailableNetworksTotal(this.mSsidFailureCount.size());
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        for (Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry : this.mSsidFailureCount.entrySet()) {
            int i4 = 1;
            i += ((AvailableNetworkFailureCount) entry.getValue().first).authenticationFailure >= 7 ? 1 : 0;
            i2 += ((AvailableNetworkFailureCount) entry.getValue().first).associationRejection >= 7 ? 1 : 0;
            if (((AvailableNetworkFailureCount) entry.getValue().first).dhcpFailure < 7) {
                i4 = 0;
            }
            i3 += i4;
        }
        if (i > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAuthenticationNetworksTotal(i);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAuthentication();
        }
        if (i2 > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAssociationNetworksTotal(i2);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAssociation();
        }
        if (i3 > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadDhcpNetworksTotal(i3);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadDhcp();
        }
    }

    public void clearAllFailureCounts() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "clearAllFailureCounts.");
        }
        Iterator<Map.Entry<String, AvailableNetworkFailureCount>> it = this.mRecentAvailableNetworks.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().resetCounts();
        }
        Iterator<Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>>> it2 = this.mSsidFailureCount.entrySet().iterator();
        while (it2.hasNext()) {
            ((AvailableNetworkFailureCount) it2.next().getValue().first).resetCounts();
        }
    }

    Map<String, AvailableNetworkFailureCount> getRecentAvailableNetworks() {
        return this.mRecentAvailableNetworks;
    }

    private void setWatchdogTriggerEnabled(boolean z) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "setWatchdogTriggerEnabled: enable = " + z);
        }
        this.mWatchdogAllowedToTrigger = z;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mWatchdogAllowedToTrigger: ");
        sb.append(this.mWatchdogAllowedToTrigger);
        sb.append("\nmWifiIsConnected: ");
        sb.append(this.mWifiIsConnected);
        sb.append("\nmRecentAvailableNetworks: ");
        sb.append(this.mRecentAvailableNetworks.size());
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            sb.append("\n ");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(", Age: ");
            sb.append(entry.getValue().age);
        }
        sb.append("\nmSsidFailureCount:");
        for (Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry2 : this.mSsidFailureCount.entrySet()) {
            AvailableNetworkFailureCount availableNetworkFailureCount = (AvailableNetworkFailureCount) entry2.getValue().first;
            Integer num = (Integer) entry2.getValue().second;
            sb.append("\n");
            sb.append(entry2.getKey());
            sb.append(": ");
            sb.append(num);
            sb.append(",");
            sb.append(availableNetworkFailureCount.toString());
        }
        return sb.toString();
    }

    public boolean isOverFailureThreshold(String str) {
        if (getFailureCount(str, 1) >= 7 || getFailureCount(str, 2) >= 7 || getFailureCount(str, 3) >= 7) {
            return true;
        }
        return false;
    }

    public int getFailureCount(String str, int i) {
        AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(str);
        if (availableNetworkFailureCount == null) {
            return 0;
        }
        String str2 = availableNetworkFailureCount.ssid;
        Pair<AvailableNetworkFailureCount, Integer> pair = this.mSsidFailureCount.get(str2);
        if (pair == null) {
            Log.d(TAG, "getFailureCount: Could not find SSID count for " + str2);
            return 0;
        }
        AvailableNetworkFailureCount availableNetworkFailureCount2 = (AvailableNetworkFailureCount) pair.first;
        switch (i) {
        }
        return 0;
    }

    protected void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    @VisibleForTesting
    protected void setBugReportProbability(double d) {
        this.mBugReportProbability = d;
    }

    public static class AvailableNetworkFailureCount {
        public WifiConfiguration config;
        public String ssid = "";
        public int associationRejection = 0;
        public int authenticationFailure = 0;
        public int dhcpFailure = 0;
        public int age = 0;

        AvailableNetworkFailureCount(WifiConfiguration wifiConfiguration) {
            this.config = wifiConfiguration;
        }

        public void incrementFailureCount(int i) {
            switch (i) {
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
            StringBuilder sb = new StringBuilder();
            sb.append(this.ssid);
            sb.append(" HasEverConnected: ");
            sb.append(this.config != null ? Boolean.valueOf(this.config.getNetworkSelectionStatus().getHasEverConnected()) : "null_config");
            sb.append(", Failures: {Assoc: ");
            sb.append(this.associationRejection);
            sb.append(", Auth: ");
            sb.append(this.authenticationFailure);
            sb.append(", Dhcp: ");
            sb.append(this.dhcpFailure);
            sb.append("}");
            return sb.toString();
        }
    }
}
