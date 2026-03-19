package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.net.NetworkKey;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.util.ArrayList;
import java.util.List;

public class ScoredNetworkEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private final ContentObserver mContentObserver;
    private final LocalLog mLocalLog;
    private boolean mNetworkRecommendationsEnabled;
    private final NetworkScoreManager mNetworkScoreManager;
    private WifiNetworkScoreCache mScoreCache;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private static final String TAG = "ScoredNetworkEvaluator";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    ScoredNetworkEvaluator(final Context context, Looper looper, final FrameworkFacade frameworkFacade, NetworkScoreManager networkScoreManager, WifiConfigManager wifiConfigManager, LocalLog localLog, WifiNetworkScoreCache wifiNetworkScoreCache, WifiPermissionsUtil wifiPermissionsUtil) {
        this.mScoreCache = wifiNetworkScoreCache;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mNetworkScoreManager = networkScoreManager;
        this.mWifiConfigManager = wifiConfigManager;
        this.mLocalLog = localLog;
        this.mContentObserver = new ContentObserver(new Handler(looper)) {
            @Override
            public void onChange(boolean z) {
                ScoredNetworkEvaluator.this.mNetworkRecommendationsEnabled = frameworkFacade.getIntegerSetting(context, "network_recommendations_enabled", 0) == 1;
            }
        };
        frameworkFacade.registerContentObserver(context, Settings.Global.getUriFor("network_recommendations_enabled"), false, this.mContentObserver);
        this.mContentObserver.onChange(false);
        this.mLocalLog.log("ScoredNetworkEvaluator constructed. mNetworkRecommendationsEnabled: " + this.mNetworkRecommendationsEnabled);
    }

    @Override
    public void update(List<ScanDetail> list) {
        if (this.mNetworkRecommendationsEnabled) {
            updateNetworkScoreCache(list);
        }
    }

    private void updateNetworkScoreCache(List<ScanDetail> list) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            NetworkKey networkKeyCreateFromScanResult = NetworkKey.createFromScanResult(list.get(i).getScanResult());
            if (networkKeyCreateFromScanResult != null && this.mScoreCache.getScoredNetwork(networkKeyCreateFromScanResult) == null) {
                arrayList.add(networkKeyCreateFromScanResult);
            }
        }
        if (!arrayList.isEmpty() && activeScorerAllowedtoSeeScanResults()) {
            this.mNetworkScoreManager.requestScores((NetworkKey[]) arrayList.toArray(new NetworkKey[arrayList.size()]));
        }
    }

    private boolean activeScorerAllowedtoSeeScanResults() {
        NetworkScorerAppData activeScorer = this.mNetworkScoreManager.getActiveScorer();
        String activeScorerPackage = this.mNetworkScoreManager.getActiveScorerPackage();
        if (activeScorer == null || activeScorerPackage == null) {
            return false;
        }
        try {
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(activeScorerPackage, activeScorer.packageUid);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    @Override
    public WifiConfiguration evaluateNetworks(List<ScanDetail> list, WifiConfiguration wifiConfiguration, String str, boolean z, boolean z2, List<Pair<ScanDetail, WifiConfiguration>> list2) {
        if (!this.mNetworkRecommendationsEnabled) {
            this.mLocalLog.log("Skipping evaluateNetworks; Network recommendations disabled.");
            return null;
        }
        ScoreTracker scoreTracker = new ScoreTracker();
        for (int i = 0; i < list.size(); i++) {
            ScanDetail scanDetail = list.get(i);
            ScanResult scanResult = scanDetail.getScanResult();
            if (scanResult != null) {
                if (this.mWifiConfigManager.wasEphemeralNetworkDeleted(ScanResultUtil.createQuotedSSID(scanResult.SSID))) {
                    debugLog("Ignoring disabled ephemeral SSID: " + scanResult.SSID);
                } else {
                    WifiConfiguration configuredNetworkForScanDetailAndCache = this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail);
                    boolean z3 = true;
                    boolean z4 = configuredNetworkForScanDetailAndCache == null || configuredNetworkForScanDetailAndCache.ephemeral;
                    if (z2 || !z4) {
                        if (configuredNetworkForScanDetailAndCache == null) {
                            if (ScanResultUtil.isScanResultForOpenNetwork(scanResult)) {
                                scoreTracker.trackUntrustedCandidate(scanResult);
                            }
                        } else if (configuredNetworkForScanDetailAndCache.ephemeral || configuredNetworkForScanDetailAndCache.useExternalScores) {
                            if (configuredNetworkForScanDetailAndCache.getNetworkSelectionStatus().isNetworkEnabled()) {
                                if (wifiConfiguration == null || wifiConfiguration.networkId != configuredNetworkForScanDetailAndCache.networkId || !TextUtils.equals(str, scanResult.BSSID)) {
                                    z3 = false;
                                }
                                if (configuredNetworkForScanDetailAndCache.ephemeral) {
                                    scoreTracker.trackUntrustedCandidate(scanResult, configuredNetworkForScanDetailAndCache, z3);
                                } else {
                                    scoreTracker.trackExternallyScoredCandidate(scanResult, configuredNetworkForScanDetailAndCache, z3);
                                }
                                if (list2 != null) {
                                    list2.add(Pair.create(scanDetail, configuredNetworkForScanDetailAndCache));
                                }
                            } else {
                                debugLog("Ignoring disabled SSID: " + configuredNetworkForScanDetailAndCache.SSID);
                            }
                        }
                    }
                }
            }
        }
        return scoreTracker.getCandidateConfiguration();
    }

    class ScoreTracker {
        private static final int EXTERNAL_SCORED_NONE = 0;
        private static final int EXTERNAL_SCORED_SAVED_NETWORK = 1;
        private static final int EXTERNAL_SCORED_UNTRUSTED_NETWORK = 2;
        private WifiConfiguration mEphemeralConfig;
        private WifiConfiguration mSavedConfig;
        private ScanResult mScanResultCandidate;
        private int mBestCandidateType = 0;
        private int mHighScore = -128;

        ScoreTracker() {
        }

        private Integer getNetworkScore(ScanResult scanResult, boolean z) {
            if (ScoredNetworkEvaluator.this.mScoreCache.isScoredNetwork(scanResult)) {
                int networkScore = ScoredNetworkEvaluator.this.mScoreCache.getNetworkScore(scanResult, z);
                if (ScoredNetworkEvaluator.DEBUG) {
                    ScoredNetworkEvaluator.this.mLocalLog.log(WifiNetworkSelector.toScanId(scanResult) + " has score: " + networkScore + " isCurrentNetwork network: " + z);
                }
                return Integer.valueOf(networkScore);
            }
            return null;
        }

        void trackUntrustedCandidate(ScanResult scanResult) {
            Integer networkScore = getNetworkScore(scanResult, false);
            if (networkScore != null && networkScore.intValue() > this.mHighScore) {
                this.mHighScore = networkScore.intValue();
                this.mScanResultCandidate = scanResult;
                this.mBestCandidateType = 2;
                ScoredNetworkEvaluator.this.debugLog(WifiNetworkSelector.toScanId(scanResult) + " becomes the new untrusted candidate.");
            }
        }

        void trackUntrustedCandidate(ScanResult scanResult, WifiConfiguration wifiConfiguration, boolean z) {
            Integer networkScore = getNetworkScore(scanResult, z);
            if (networkScore != null && networkScore.intValue() > this.mHighScore) {
                this.mHighScore = networkScore.intValue();
                this.mScanResultCandidate = scanResult;
                this.mBestCandidateType = 2;
                this.mEphemeralConfig = wifiConfiguration;
                ScoredNetworkEvaluator.this.mWifiConfigManager.setNetworkCandidateScanResult(wifiConfiguration.networkId, scanResult, 0);
                ScoredNetworkEvaluator.this.debugLog(WifiNetworkSelector.toScanId(scanResult) + " becomes the new untrusted candidate.");
            }
        }

        void trackExternallyScoredCandidate(ScanResult scanResult, WifiConfiguration wifiConfiguration, boolean z) {
            Integer networkScore = getNetworkScore(scanResult, z);
            if (networkScore != null) {
                if (networkScore.intValue() > this.mHighScore || (this.mBestCandidateType == 2 && networkScore.intValue() == this.mHighScore)) {
                    this.mHighScore = networkScore.intValue();
                    this.mSavedConfig = wifiConfiguration;
                    this.mScanResultCandidate = scanResult;
                    this.mBestCandidateType = 1;
                    ScoredNetworkEvaluator.this.mWifiConfigManager.setNetworkCandidateScanResult(wifiConfiguration.networkId, scanResult, 0);
                    ScoredNetworkEvaluator.this.debugLog(WifiNetworkSelector.toScanId(scanResult) + " becomes the new externally scored saved network candidate.");
                }
            }
        }

        WifiConfiguration getCandidateConfiguration() {
            int networkId;
            switch (this.mBestCandidateType) {
                case 1:
                    networkId = this.mSavedConfig.networkId;
                    ScoredNetworkEvaluator.this.mLocalLog.log(String.format("new saved network candidate %s network ID:%d", WifiNetworkSelector.toScanId(this.mScanResultCandidate), Integer.valueOf(networkId)));
                    break;
                case 2:
                    if (this.mEphemeralConfig != null) {
                        networkId = this.mEphemeralConfig.networkId;
                        ScoredNetworkEvaluator.this.mLocalLog.log(String.format("existing ephemeral candidate %s network ID:%d, meteredHint=%b", WifiNetworkSelector.toScanId(this.mScanResultCandidate), Integer.valueOf(networkId), Boolean.valueOf(this.mEphemeralConfig.meteredHint)));
                    } else {
                        this.mEphemeralConfig = ScanResultUtil.createNetworkFromScanResult(this.mScanResultCandidate);
                        this.mEphemeralConfig.ephemeral = true;
                        this.mEphemeralConfig.meteredHint = ScoredNetworkEvaluator.this.mScoreCache.getMeteredHint(this.mScanResultCandidate);
                        NetworkUpdateResult networkUpdateResultAddOrUpdateNetwork = ScoredNetworkEvaluator.this.mWifiConfigManager.addOrUpdateNetwork(this.mEphemeralConfig, 1010);
                        if (!networkUpdateResultAddOrUpdateNetwork.isSuccess()) {
                            ScoredNetworkEvaluator.this.mLocalLog.log("Failed to add ephemeral network");
                        } else if (!ScoredNetworkEvaluator.this.mWifiConfigManager.updateNetworkSelectionStatus(networkUpdateResultAddOrUpdateNetwork.getNetworkId(), 0)) {
                            ScoredNetworkEvaluator.this.mLocalLog.log("Failed to make ephemeral network selectable");
                        } else {
                            networkId = networkUpdateResultAddOrUpdateNetwork.getNetworkId();
                            ScoredNetworkEvaluator.this.mWifiConfigManager.setNetworkCandidateScanResult(networkId, this.mScanResultCandidate, 0);
                            ScoredNetworkEvaluator.this.mLocalLog.log(String.format("new ephemeral candidate %s network ID:%d, meteredHint=%b", WifiNetworkSelector.toScanId(this.mScanResultCandidate), Integer.valueOf(networkId), Boolean.valueOf(this.mEphemeralConfig.meteredHint)));
                        }
                        networkId = -1;
                    }
                    break;
                default:
                    ScoredNetworkEvaluator.this.mLocalLog.log("ScoredNetworkEvaluator did not see any good candidates.");
                    networkId = -1;
                    break;
            }
            return ScoredNetworkEvaluator.this.mWifiConfigManager.getConfiguredNetwork(networkId);
        }
    }

    private void debugLog(String str) {
        if (DEBUG) {
            this.mLocalLog.log(str);
        }
    }

    @Override
    public String getName() {
        return TAG;
    }
}
