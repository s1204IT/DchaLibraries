package com.mediatek.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.util.Pair;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiNetworkSelector;
import com.mediatek.server.wifi.MtkWifiService;
import java.util.List;

public class MtkNetworkEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private static final String NAME = "MtkNetworkEvaluator";
    private final Context mContext;
    private final MtkWifiService mService;
    private final WifiConfigManager mWifiConfigManager;

    public MtkNetworkEvaluator(Context context, WifiConfigManager wifiConfigManager, MtkWifiService mtkWifiService) {
        this.mContext = context;
        this.mWifiConfigManager = wifiConfigManager;
        this.mService = mtkWifiService;
    }

    public String getName() {
        return NAME;
    }

    public void update(List<ScanDetail> list) {
    }

    public WifiConfiguration evaluateNetworks(List<ScanDetail> list, WifiConfiguration wifiConfiguration, String str, boolean z, boolean z2, List<Pair<ScanDetail, WifiConfiguration>> list2) {
        if (this.mService.getOpExt().hasNetworkSelection() == 1) {
            MtkWifiService.log("MtkNetworkEvaluator.evaluateNetworks: IMtkWifiServiceExt.OP_01");
            MtkWifiService.AutoConnectManager acm = this.mService.getACM();
            acm.setShowReselectDialog(false);
            if (acm.getScanForWeakSignal()) {
                acm.showReselectionDialog();
            }
            acm.clearDisconnectNetworkId();
            return null;
        }
        return null;
    }
}
