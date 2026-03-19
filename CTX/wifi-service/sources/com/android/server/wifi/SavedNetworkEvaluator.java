package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.util.TelephonyUtil;
import com.mediatek.server.wifi.MtkEapSimUtility;
import java.util.Iterator;
import java.util.List;

public class SavedNetworkEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private static final String NAME = "SavedNetworkEvaluator";
    private final int mBand5GHzAward;
    private final Clock mClock;
    private final WifiConnectivityHelper mConnectivityHelper;
    private final int mLastSelectionAward;
    private final LocalLog mLocalLog;
    private final int mRssiScoreOffset;
    private final int mRssiScoreSlope;
    private final int mSameBssidAward;
    private final int mSameNetworkAward;
    private final ScoringParams mScoringParams;
    private final int mSecurityAward;
    private final WifiConfigManager mWifiConfigManager;

    SavedNetworkEvaluator(Context context, ScoringParams scoringParams, WifiConfigManager wifiConfigManager, Clock clock, LocalLog localLog, WifiConnectivityHelper wifiConnectivityHelper) {
        this.mScoringParams = scoringParams;
        this.mWifiConfigManager = wifiConfigManager;
        this.mClock = clock;
        this.mLocalLog = localLog;
        this.mConnectivityHelper = wifiConnectivityHelper;
        this.mRssiScoreSlope = context.getResources().getInteger(R.integer.config_jobSchedulerInactivityIdleThresholdOnStablePower);
        this.mRssiScoreOffset = context.getResources().getInteger(R.integer.config_jobSchedulerInactivityIdleThreshold);
        this.mSameBssidAward = context.getResources().getInteger(R.integer.config_jobSchedulerUserGracePeriod);
        this.mSameNetworkAward = context.getResources().getInteger(R.integer.config_letterboxDefaultPositionForTabletopModeReachability);
        this.mLastSelectionAward = context.getResources().getInteger(R.integer.config_jobSchedulerBackgroundJobsDelay);
        this.mSecurityAward = context.getResources().getInteger(R.integer.config_jumpTapTimeoutMillis);
        this.mBand5GHzAward = context.getResources().getInteger(R.integer.config_hoverTapTimeoutMillis);
    }

    private void localLog(String str) {
        this.mLocalLog.log(str);
        Log.d(NAME, str);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void updateSavedNetworkSelectionStatus() {
        List<WifiConfiguration> savedNetworks = this.mWifiConfigManager.getSavedNetworks();
        if (savedNetworks.size() == 0) {
            localLog("No saved networks.");
            return;
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (WifiConfiguration wifiConfiguration : savedNetworks) {
            if (!wifiConfiguration.isPasspoint()) {
                this.mWifiConfigManager.tryEnableNetwork(wifiConfiguration.networkId);
                this.mWifiConfigManager.clearNetworkCandidateScanResult(wifiConfiguration.networkId);
                WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
                if (!networkSelectionStatus.isNetworkEnabled()) {
                    stringBuffer.append("  ");
                    stringBuffer.append(WifiNetworkSelector.toNetworkString(wifiConfiguration));
                    stringBuffer.append(" ");
                    for (int i = 1; i < 14; i++) {
                        int disableReasonCounter = networkSelectionStatus.getDisableReasonCounter(i);
                        if (disableReasonCounter > 0) {
                            stringBuffer.append("reason=");
                            stringBuffer.append(WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(i));
                            stringBuffer.append(", count=");
                            stringBuffer.append(disableReasonCounter);
                            stringBuffer.append("; ");
                        }
                    }
                    stringBuffer.append("\n");
                }
            }
        }
        if (stringBuffer.length() > 0) {
            localLog("Disabled saved networks:");
            localLog(stringBuffer.toString());
        }
    }

    @Override
    public void update(List<ScanDetail> list) {
        updateSavedNetworkSelectionStatus();
    }

    private int calculateBssidScore(ScanResult scanResult, WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2, String str, StringBuffer stringBuffer) {
        boolean zIs5GHz = scanResult.is5GHz();
        stringBuffer.append("[ ");
        stringBuffer.append(scanResult.SSID);
        stringBuffer.append(" ");
        stringBuffer.append(scanResult.BSSID);
        stringBuffer.append(" RSSI:");
        stringBuffer.append(scanResult.level);
        stringBuffer.append(" ] ");
        int goodRssi = this.mScoringParams.getGoodRssi(scanResult.frequency);
        if (scanResult.level < goodRssi) {
            goodRssi = scanResult.level;
        }
        int i = ((goodRssi + this.mRssiScoreOffset) * this.mRssiScoreSlope) + 0;
        stringBuffer.append(" RSSI score: ");
        stringBuffer.append(i);
        stringBuffer.append(",");
        if (zIs5GHz) {
            i += this.mBand5GHzAward;
            stringBuffer.append(" 5GHz bonus: ");
            stringBuffer.append(this.mBand5GHzAward);
            stringBuffer.append(",");
        }
        int lastSelectedNetwork = this.mWifiConfigManager.getLastSelectedNetwork();
        if (lastSelectedNetwork != -1 && lastSelectedNetwork == wifiConfiguration.networkId) {
            long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis() - this.mWifiConfigManager.getLastSelectedTimeStamp();
            if (elapsedSinceBootMillis > 0) {
                long j = (elapsedSinceBootMillis / 1000) / 60;
                int i2 = this.mLastSelectionAward - ((int) j);
                i += i2 > 0 ? i2 : 0;
                stringBuffer.append(" User selection ");
                stringBuffer.append(j);
                stringBuffer.append(" minutes ago, bonus: ");
                stringBuffer.append(i2);
                stringBuffer.append(",");
            }
        }
        if (wifiConfiguration2 != null && wifiConfiguration.networkId == wifiConfiguration2.networkId) {
            i += this.mSameNetworkAward;
            stringBuffer.append(" Same network bonus: ");
            stringBuffer.append(this.mSameNetworkAward);
            stringBuffer.append(",");
            if (this.mConnectivityHelper.isFirmwareRoamingSupported() && str != null && !str.equals(scanResult.BSSID)) {
                i += this.mSameBssidAward;
                stringBuffer.append(" Equivalent BSSID bonus: ");
                stringBuffer.append(this.mSameBssidAward);
                stringBuffer.append(",");
            }
        }
        if (str != null && str.equals(scanResult.BSSID)) {
            i += this.mSameBssidAward;
            stringBuffer.append(" Same BSSID bonus: ");
            stringBuffer.append(this.mSameBssidAward);
            stringBuffer.append(",");
        }
        if (!WifiConfigurationUtil.isConfigForOpenNetwork(wifiConfiguration)) {
            i += this.mSecurityAward;
            stringBuffer.append(" Secure network bonus: ");
            stringBuffer.append(this.mSecurityAward);
            stringBuffer.append(",");
        }
        stringBuffer.append(" ## Total score: ");
        stringBuffer.append(i);
        stringBuffer.append("\n");
        return i;
    }

    @Override
    public WifiConfiguration evaluateNetworks(List<ScanDetail> list, WifiConfiguration wifiConfiguration, String str, boolean z, boolean z2, List<Pair<ScanDetail, WifiConfiguration>> list2) {
        Iterator<ScanDetail> it;
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<ScanDetail> it2 = list.iterator();
        ScanResult scanResult = null;
        WifiConfiguration configuredNetwork = null;
        int i = Integer.MIN_VALUE;
        while (it2.hasNext()) {
            ScanDetail next = it2.next();
            ScanResult scanResult2 = next.getScanResult();
            WifiConfiguration configuredNetworkForScanDetailAndCache = this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(next);
            if (configuredNetworkForScanDetailAndCache == null || configuredNetworkForScanDetailAndCache.isPasspoint() || configuredNetworkForScanDetailAndCache.isEphemeral()) {
                it = it2;
            } else {
                WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = configuredNetworkForScanDetailAndCache.getNetworkSelectionStatus();
                networkSelectionStatus.setSeenInLastQualifiedNetworkSelection(true);
                if (!networkSelectionStatus.isNetworkEnabled()) {
                    localLog("Network " + WifiNetworkSelector.toNetworkString(configuredNetworkForScanDetailAndCache) + " skip calculateBssidScore process since it is not enabled(" + networkSelectionStatus.getNetworkDisableReasonString() + ")");
                } else if (configuredNetworkForScanDetailAndCache.BSSID != null && !configuredNetworkForScanDetailAndCache.BSSID.equals("any") && !configuredNetworkForScanDetailAndCache.BSSID.equals(scanResult2.BSSID)) {
                    localLog("Network " + WifiNetworkSelector.toNetworkString(configuredNetworkForScanDetailAndCache) + " has specified BSSID " + configuredNetworkForScanDetailAndCache.BSSID + ". Skip " + scanResult2.BSSID);
                } else if (TelephonyUtil.isSimConfig(configuredNetworkForScanDetailAndCache) && !MtkEapSimUtility.isConfigSimCardPresent(configuredNetworkForScanDetailAndCache)) {
                    localLog("Network " + WifiNetworkSelector.toNetworkString(configuredNetworkForScanDetailAndCache) + " is skipped due to sim card absent");
                } else {
                    it = it2;
                    int iCalculateBssidScore = calculateBssidScore(scanResult2, configuredNetworkForScanDetailAndCache, wifiConfiguration, str, stringBuffer);
                    if (iCalculateBssidScore > networkSelectionStatus.getCandidateScore() || (iCalculateBssidScore == networkSelectionStatus.getCandidateScore() && networkSelectionStatus.getCandidate() != null && scanResult2.level > networkSelectionStatus.getCandidate().level)) {
                        this.mWifiConfigManager.setNetworkCandidateScanResult(configuredNetworkForScanDetailAndCache.networkId, scanResult2, iCalculateBssidScore);
                    }
                    if (configuredNetworkForScanDetailAndCache.useExternalScores) {
                        localLog("Network " + WifiNetworkSelector.toNetworkString(configuredNetworkForScanDetailAndCache) + " has external score.");
                    } else {
                        if (list2 != null) {
                            list2.add(Pair.create(next, this.mWifiConfigManager.getConfiguredNetwork(configuredNetworkForScanDetailAndCache.networkId)));
                        }
                        if (iCalculateBssidScore > i || (iCalculateBssidScore == i && scanResult != null && scanResult2.level > scanResult.level)) {
                            this.mWifiConfigManager.setNetworkCandidateScanResult(configuredNetworkForScanDetailAndCache.networkId, scanResult2, iCalculateBssidScore);
                            i = iCalculateBssidScore;
                            configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(configuredNetworkForScanDetailAndCache.networkId);
                            scanResult = scanResult2;
                        }
                    }
                }
                it = it2;
            }
            it2 = it;
        }
        if (stringBuffer.length() > 0) {
            localLog("\n" + stringBuffer.toString());
        }
        if (scanResult == null) {
            localLog("did not see any good candidates.");
        }
        return configuredNetwork;
    }
}
