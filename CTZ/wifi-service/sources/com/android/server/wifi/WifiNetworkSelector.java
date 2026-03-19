package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class WifiNetworkSelector {
    public static final int EVALUATOR_MIN_PRIORITY = 6;
    private static final long INVALID_TIME_STAMP = Long.MIN_VALUE;
    public static final int MAX_NUM_EVALUATORS = 6;

    @VisibleForTesting
    public static final int MINIMUM_NETWORK_SELECTION_INTERVAL_MS = 10000;
    private static final String TAG = "WifiNetworkSelector";
    private final Clock mClock;
    private final boolean mEnableAutoJoinWhenAssociated;
    private final LocalLog mLocalLog;
    private final ScoringParams mScoringParams;
    private final int mStayOnNetworkMinimumRxRate;
    private final int mStayOnNetworkMinimumTxRate;
    private final WifiConfigManager mWifiConfigManager;
    private long mLastNetworkSelectionTimeStamp = INVALID_TIME_STAMP;
    private volatile List<Pair<ScanDetail, WifiConfiguration>> mConnectableNetworks = new ArrayList();
    private List<ScanDetail> mFilteredNetworks = new ArrayList();
    private final NetworkEvaluator[] mEvaluators = new NetworkEvaluator[6];

    public interface NetworkEvaluator {
        WifiConfiguration evaluateNetworks(List<ScanDetail> list, WifiConfiguration wifiConfiguration, String str, boolean z, boolean z2, List<Pair<ScanDetail, WifiConfiguration>> list2);

        String getName();

        void update(List<ScanDetail> list);
    }

    private void localLog(String str) {
        this.mLocalLog.log(str);
        Log.d(TAG, str);
    }

    private boolean isCurrentNetworkSufficient(WifiInfo wifiInfo, List<ScanDetail> list) {
        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
        if (configuredNetwork == null) {
            localLog("No current connected network.");
            return false;
        }
        localLog("Current connected network: " + configuredNetwork.SSID + " , ID: " + configuredNetwork.networkId);
        int rssi = wifiInfo.getRssi();
        boolean z = rssi > this.mScoringParams.getSufficientRssi(wifiInfo.getFrequency());
        boolean z2 = wifiInfo.txSuccessRate > ((double) this.mStayOnNetworkMinimumTxRate) || wifiInfo.rxSuccessRate > ((double) this.mStayOnNetworkMinimumRxRate);
        if (z && z2) {
            localLog("Stay on current network because of good RSSI and ongoing traffic");
            return true;
        }
        if (configuredNetwork.ephemeral) {
            localLog("Current network is an ephemeral one.");
            return false;
        }
        if (WifiConfigurationUtil.isConfigForOpenNetwork(configuredNetwork)) {
            localLog("Current network is a open one.");
            return false;
        }
        if (wifiInfo.is24GHz() && is5GHzNetworkAvailable(list)) {
            localLog("Current network is 2.4GHz. 5GHz networks available.");
            return false;
        }
        if (!z) {
            localLog("Current network RSSI[" + rssi + "]-acceptable but not qualified.");
            return false;
        }
        if (configuredNetwork.numNoInternetAccessReports <= 0 || configuredNetwork.noInternetAccessExpected) {
            return true;
        }
        localLog("Current network has [" + configuredNetwork.numNoInternetAccessReports + "] no-internet access reports.");
        return false;
    }

    private boolean is5GHzNetworkAvailable(List<ScanDetail> list) {
        Iterator<ScanDetail> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().getScanResult().is5GHz()) {
                return true;
            }
        }
        return false;
    }

    private boolean isNetworkSelectionNeeded(List<ScanDetail> list, WifiInfo wifiInfo, boolean z, boolean z2) {
        if (list.size() == 0) {
            localLog("Empty connectivity scan results. Skip network selection.");
            return false;
        }
        if (z) {
            if (!this.mEnableAutoJoinWhenAssociated) {
                localLog("Switching networks in connected state is not allowed. Skip network selection.");
                return false;
            }
            if (this.mLastNetworkSelectionTimeStamp != INVALID_TIME_STAMP) {
                long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis() - this.mLastNetworkSelectionTimeStamp;
                if (elapsedSinceBootMillis < 10000) {
                    localLog("Too short since last network selection: " + elapsedSinceBootMillis + " ms. Skip network selection.");
                    return false;
                }
            }
            if (isCurrentNetworkSufficient(wifiInfo, list)) {
                localLog("Current connected network already sufficient. Skip network selection.");
                return false;
            }
            localLog("Current connected network is not sufficient.");
            return true;
        }
        if (z2) {
            return true;
        }
        localLog("WifiStateMachine is in neither CONNECTED nor DISCONNECTED state. Skip network selection.");
        return false;
    }

    public static String toScanId(ScanResult scanResult) {
        return scanResult == null ? "NULL" : String.format("%s:%s", scanResult.SSID, scanResult.BSSID);
    }

    public static String toNetworkString(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null) {
            return null;
        }
        return wifiConfiguration.SSID + ":" + wifiConfiguration.networkId;
    }

    public boolean isSignalTooWeak(ScanResult scanResult) {
        return scanResult.level < this.mScoringParams.getEntryRssi(scanResult.frequency);
    }

    private List<ScanDetail> filterScanResults(List<ScanDetail> list, HashSet<String> hashSet, boolean z, String str) {
        new ArrayList();
        ArrayList arrayList = new ArrayList();
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer stringBuffer2 = new StringBuffer();
        StringBuffer stringBuffer3 = new StringBuffer();
        boolean z2 = false;
        for (ScanDetail scanDetail : list) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (TextUtils.isEmpty(scanResult.SSID)) {
                stringBuffer.append(scanResult.BSSID);
                stringBuffer.append(" / ");
            } else {
                if (scanResult.BSSID.equals(str)) {
                    z2 = true;
                }
                String scanId = toScanId(scanResult);
                if (hashSet.contains(scanResult.BSSID)) {
                    stringBuffer2.append(scanId);
                    stringBuffer2.append(" / ");
                } else if (isSignalTooWeak(scanResult)) {
                    stringBuffer3.append(scanId);
                    stringBuffer3.append("(");
                    stringBuffer3.append(scanResult.is24GHz() ? "2.4GHz" : "5GHz");
                    stringBuffer3.append(")");
                    stringBuffer3.append(scanResult.level);
                    stringBuffer3.append(" / ");
                } else {
                    arrayList.add(scanDetail);
                }
            }
        }
        if (z && !z2) {
            localLog("Current connected BSSID " + str + " is not in the scan results. Skip network selection.");
            arrayList.clear();
            return arrayList;
        }
        if (stringBuffer.length() != 0) {
            localLog("Networks filtered out due to invalid SSID: " + ((Object) stringBuffer));
        }
        if (stringBuffer2.length() != 0) {
            localLog("Networks filtered out due to blacklist: " + ((Object) stringBuffer2));
        }
        if (stringBuffer3.length() != 0) {
            localLog("Networks filtered out due to low signal strength: " + ((Object) stringBuffer3));
        }
        return arrayList;
    }

    public List<ScanDetail> getFilteredScanDetailsForOpenUnsavedNetworks() {
        ArrayList arrayList = new ArrayList();
        for (ScanDetail scanDetail : this.mFilteredNetworks) {
            if (ScanResultUtil.isScanResultForOpenNetwork(scanDetail.getScanResult()) && this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail) == null) {
                arrayList.add(scanDetail);
            }
        }
        return arrayList;
    }

    public List<ScanDetail> getFilteredScanDetailsForCarrierUnsavedNetworks(CarrierNetworkConfig carrierNetworkConfig) {
        ArrayList arrayList = new ArrayList();
        for (ScanDetail scanDetail : this.mFilteredNetworks) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (ScanResultUtil.isScanResultForEapNetwork(scanResult) && carrierNetworkConfig.isCarrierNetwork(scanResult.SSID) && this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail) == null) {
                arrayList.add(scanDetail);
            }
        }
        return arrayList;
    }

    public List<Pair<ScanDetail, WifiConfiguration>> getConnectableScanDetails() {
        return this.mConnectableNetworks;
    }

    public boolean setUserConnectChoice(int i) {
        localLog("userSelectNetwork: network ID=" + i);
        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(i);
        boolean z = false;
        if (configuredNetwork == null || configuredNetwork.SSID == null) {
            localLog("userSelectNetwork: Invalid configuration with nid=" + i);
            return false;
        }
        if (!configuredNetwork.getNetworkSelectionStatus().isNetworkEnabled()) {
            this.mWifiConfigManager.updateNetworkSelectionStatus(i, 0);
        }
        String strConfigKey = configuredNetwork.configKey();
        long wallClockMillis = this.mClock.getWallClockMillis();
        for (WifiConfiguration wifiConfiguration : this.mWifiConfigManager.getSavedNetworks()) {
            WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
            if (wifiConfiguration.networkId == configuredNetwork.networkId) {
                if (networkSelectionStatus.getConnectChoice() != null) {
                    localLog("Remove user selection preference of " + networkSelectionStatus.getConnectChoice() + " Set Time: " + networkSelectionStatus.getConnectChoiceTimestamp() + " from " + wifiConfiguration.SSID + " : " + wifiConfiguration.networkId);
                    this.mWifiConfigManager.clearNetworkConnectChoice(wifiConfiguration.networkId);
                    z = true;
                }
            } else if (networkSelectionStatus.getSeenInLastQualifiedNetworkSelection() && (networkSelectionStatus.getConnectChoice() == null || !networkSelectionStatus.getConnectChoice().equals(strConfigKey))) {
                localLog("Add key: " + strConfigKey + " Set Time: " + wallClockMillis + " to " + toNetworkString(wifiConfiguration));
                this.mWifiConfigManager.setNetworkConnectChoice(wifiConfiguration.networkId, strConfigKey, wallClockMillis);
                z = true;
            }
        }
        return z;
    }

    private WifiConfiguration overrideCandidateWithUserConnectChoice(WifiConfiguration wifiConfiguration) {
        WifiConfiguration wifiConfiguration2 = wifiConfiguration;
        ScanResult candidate = wifiConfiguration.getNetworkSelectionStatus().getCandidate();
        WifiConfiguration wifiConfiguration3 = wifiConfiguration2;
        while (true) {
            if (wifiConfiguration3.getNetworkSelectionStatus().getConnectChoice() != null) {
                String connectChoice = wifiConfiguration3.getNetworkSelectionStatus().getConnectChoice();
                WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(connectChoice);
                if (configuredNetwork != null) {
                    WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = configuredNetwork.getNetworkSelectionStatus();
                    if (networkSelectionStatus.getCandidate() != null && networkSelectionStatus.isNetworkEnabled()) {
                        candidate = networkSelectionStatus.getCandidate();
                        wifiConfiguration2 = configuredNetwork;
                    }
                    wifiConfiguration3 = configuredNetwork;
                } else {
                    localLog("Connect choice: " + connectChoice + " has no corresponding saved config.");
                    break;
                }
            } else {
                break;
            }
        }
        if (wifiConfiguration2 != wifiConfiguration) {
            localLog("After user selection adjustment, the final candidate is:" + toNetworkString(wifiConfiguration2) + " : " + candidate.BSSID);
        }
        return wifiConfiguration2;
    }

    public WifiConfiguration selectNetwork(List<ScanDetail> list, HashSet<String> hashSet, WifiInfo wifiInfo, boolean z, boolean z2, boolean z3) {
        this.mFilteredNetworks.clear();
        this.mConnectableNetworks.clear();
        WifiConfiguration wifiConfigurationEvaluateNetworks = null;
        if (list.size() == 0) {
            localLog("Empty connectivity scan result");
            return null;
        }
        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
        String bssid = wifiInfo.getBSSID();
        if (!isNetworkSelectionNeeded(list, wifiInfo, z, z2)) {
            return null;
        }
        for (NetworkEvaluator networkEvaluator : this.mEvaluators) {
            if (networkEvaluator != null) {
                networkEvaluator.update(list);
            }
        }
        this.mFilteredNetworks = filterScanResults(list, hashSet, z, bssid);
        if (this.mFilteredNetworks.size() == 0) {
            return null;
        }
        NetworkEvaluator[] networkEvaluatorArr = this.mEvaluators;
        int length = networkEvaluatorArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            NetworkEvaluator networkEvaluator2 = networkEvaluatorArr[i];
            if (networkEvaluator2 != null) {
                localLog("About to run " + networkEvaluator2.getName() + " :");
                wifiConfigurationEvaluateNetworks = networkEvaluator2.evaluateNetworks(new ArrayList(this.mFilteredNetworks), configuredNetwork, bssid, z, z3, this.mConnectableNetworks);
                if (wifiConfigurationEvaluateNetworks != null) {
                    localLog(networkEvaluator2.getName() + " selects " + toNetworkString(wifiConfigurationEvaluateNetworks) + " : " + wifiConfigurationEvaluateNetworks.getNetworkSelectionStatus().getCandidate().BSSID);
                    break;
                }
            }
            i++;
        }
        if (wifiConfigurationEvaluateNetworks != null) {
            WifiConfiguration wifiConfigurationOverrideCandidateWithUserConnectChoice = overrideCandidateWithUserConnectChoice(wifiConfigurationEvaluateNetworks);
            this.mLastNetworkSelectionTimeStamp = this.mClock.getElapsedSinceBootMillis();
            return wifiConfigurationOverrideCandidateWithUserConnectChoice;
        }
        return wifiConfigurationEvaluateNetworks;
    }

    public boolean registerNetworkEvaluator(NetworkEvaluator networkEvaluator, int i) {
        if (i < 0 || i >= 6) {
            localLog("Invalid network evaluator priority: " + i);
            return false;
        }
        if (this.mEvaluators[i] != null) {
            localLog("Priority " + i + " is already registered by " + this.mEvaluators[i].getName());
            return false;
        }
        this.mEvaluators[i] = networkEvaluator;
        return true;
    }

    WifiNetworkSelector(Context context, ScoringParams scoringParams, WifiConfigManager wifiConfigManager, Clock clock, LocalLog localLog) {
        this.mWifiConfigManager = wifiConfigManager;
        this.mClock = clock;
        this.mScoringParams = scoringParams;
        this.mLocalLog = localLog;
        this.mEnableAutoJoinWhenAssociated = context.getResources().getBoolean(R.^attr-private.productId);
        this.mStayOnNetworkMinimumTxRate = context.getResources().getInteger(R.integer.config_lightSensorWarmupTime);
        this.mStayOnNetworkMinimumRxRate = context.getResources().getInteger(R.integer.config_lidOpenRotation);
    }
}
