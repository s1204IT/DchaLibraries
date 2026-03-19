package com.android.server.wifi.hotspot2;

import android.net.wifi.WifiConfiguration;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Pair;
import com.android.server.wifi.NetworkUpdateResult;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.ArrayList;
import java.util.List;

public class PasspointNetworkEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private static final String NAME = "PasspointNetworkEvaluator";
    private final LocalLog mLocalLog;
    private final PasspointManager mPasspointManager;
    private final WifiConfigManager mWifiConfigManager;

    private class PasspointNetworkCandidate {
        PasspointMatch mMatchStatus;
        PasspointProvider mProvider;
        ScanDetail mScanDetail;

        PasspointNetworkCandidate(PasspointProvider passpointProvider, PasspointMatch passpointMatch, ScanDetail scanDetail) {
            this.mProvider = passpointProvider;
            this.mMatchStatus = passpointMatch;
            this.mScanDetail = scanDetail;
        }
    }

    public PasspointNetworkEvaluator(PasspointManager passpointManager, WifiConfigManager wifiConfigManager, LocalLog localLog) {
        this.mPasspointManager = passpointManager;
        this.mWifiConfigManager = wifiConfigManager;
        this.mLocalLog = localLog;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void update(List<ScanDetail> list) {
    }

    @Override
    public WifiConfiguration evaluateNetworks(List<ScanDetail> list, WifiConfiguration wifiConfiguration, String str, boolean z, boolean z2, List<Pair<ScanDetail, WifiConfiguration>> list2) {
        Pair<PasspointProvider, PasspointMatch> pairMatchProvider;
        this.mPasspointManager.sweepCache();
        ArrayList arrayList = new ArrayList();
        for (ScanDetail scanDetail : list) {
            if (scanDetail.getNetworkDetail().isInterworking() && (pairMatchProvider = this.mPasspointManager.matchProvider(scanDetail.getScanResult())) != null && (!((PasspointProvider) pairMatchProvider.first).isSimCredential() || this.mWifiConfigManager.isSimPresent())) {
                arrayList.add(new PasspointNetworkCandidate((PasspointProvider) pairMatchProvider.first, (PasspointMatch) pairMatchProvider.second, scanDetail));
            }
        }
        if (arrayList.isEmpty()) {
            localLog("No suitable Passpoint network found");
            return null;
        }
        PasspointNetworkCandidate passpointNetworkCandidateFindBestNetwork = findBestNetwork(arrayList, wifiConfiguration != null ? wifiConfiguration.SSID : null);
        if (wifiConfiguration != null && TextUtils.equals(wifiConfiguration.SSID, ScanResultUtil.createQuotedSSID(passpointNetworkCandidateFindBestNetwork.mScanDetail.getSSID()))) {
            localLog("Staying with current Passpoint network " + wifiConfiguration.SSID);
            this.mWifiConfigManager.setNetworkCandidateScanResult(wifiConfiguration.networkId, passpointNetworkCandidateFindBestNetwork.mScanDetail.getScanResult(), 0);
            this.mWifiConfigManager.updateScanDetailForNetwork(wifiConfiguration.networkId, passpointNetworkCandidateFindBestNetwork.mScanDetail);
            list2.add(Pair.create(passpointNetworkCandidateFindBestNetwork.mScanDetail, wifiConfiguration));
            return wifiConfiguration;
        }
        WifiConfiguration wifiConfigurationCreateWifiConfigForProvider = createWifiConfigForProvider(passpointNetworkCandidateFindBestNetwork);
        if (wifiConfigurationCreateWifiConfigForProvider != null) {
            list2.add(Pair.create(passpointNetworkCandidateFindBestNetwork.mScanDetail, wifiConfigurationCreateWifiConfigForProvider));
            localLog("Passpoint network to connect to: " + wifiConfigurationCreateWifiConfigForProvider.SSID);
        }
        return wifiConfigurationCreateWifiConfigForProvider;
    }

    private WifiConfiguration createWifiConfigForProvider(PasspointNetworkCandidate passpointNetworkCandidate) {
        WifiConfiguration wifiConfig = passpointNetworkCandidate.mProvider.getWifiConfig();
        wifiConfig.SSID = ScanResultUtil.createQuotedSSID(passpointNetworkCandidate.mScanDetail.getSSID());
        if (passpointNetworkCandidate.mMatchStatus == PasspointMatch.HomeProvider) {
            wifiConfig.isHomeProviderNetwork = true;
        }
        NetworkUpdateResult networkUpdateResultAddOrUpdateNetwork = this.mWifiConfigManager.addOrUpdateNetwork(wifiConfig, 1010);
        if (networkUpdateResultAddOrUpdateNetwork.isSuccess()) {
            this.mWifiConfigManager.enableNetwork(networkUpdateResultAddOrUpdateNetwork.getNetworkId(), false, 1010);
            this.mWifiConfigManager.setNetworkCandidateScanResult(networkUpdateResultAddOrUpdateNetwork.getNetworkId(), passpointNetworkCandidate.mScanDetail.getScanResult(), 0);
            this.mWifiConfigManager.updateScanDetailForNetwork(networkUpdateResultAddOrUpdateNetwork.getNetworkId(), passpointNetworkCandidate.mScanDetail);
            return this.mWifiConfigManager.getConfiguredNetwork(networkUpdateResultAddOrUpdateNetwork.getNetworkId());
        }
        localLog("Failed to add passpoint network");
        return null;
    }

    private PasspointNetworkCandidate findBestNetwork(List<PasspointNetworkCandidate> list, String str) {
        PasspointNetworkCandidate passpointNetworkCandidate = null;
        int i = Integer.MIN_VALUE;
        for (PasspointNetworkCandidate passpointNetworkCandidate2 : list) {
            ScanDetail scanDetail = passpointNetworkCandidate2.mScanDetail;
            int iCalculateScore = PasspointNetworkScore.calculateScore(passpointNetworkCandidate2.mMatchStatus == PasspointMatch.HomeProvider, scanDetail, this.mPasspointManager.getANQPElements(scanDetail.getScanResult()), TextUtils.equals(str, ScanResultUtil.createQuotedSSID(scanDetail.getSSID())));
            if (iCalculateScore > i) {
                passpointNetworkCandidate = passpointNetworkCandidate2;
                i = iCalculateScore;
            }
        }
        localLog("Best Passpoint network " + passpointNetworkCandidate.mScanDetail.getSSID() + " provided by " + passpointNetworkCandidate.mProvider.getConfig().getHomeSp().getFqdn());
        return passpointNetworkCandidate;
    }

    private void localLog(String str) {
        this.mLocalLog.log(str);
    }
}
