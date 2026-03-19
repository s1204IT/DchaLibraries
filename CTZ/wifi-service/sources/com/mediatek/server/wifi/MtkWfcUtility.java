package com.mediatek.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.WorkSource;
import android.util.Log;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiInjector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MtkWfcUtility {
    private static final String TAG = "MtkWfcUtility";
    private static final WorkSource WIFI_WORK_SOURCE = new WorkSource(1010);
    private static Set<Integer> mSavedNetworkChannelSet = new HashSet();

    public static void init(Context context) {
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Log.v(MtkWfcUtility.TAG, "onReceive: WFC_REQUEST_PARTIAL_SCAN");
                MtkWfcUtility.startPartialScanForSavedChannel();
            }
        }, new IntentFilter("com.mediatek.intent.action.WFC_REQUEST_PARTIAL_SCAN"));
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getIntExtra("scan_enabled", 1) == 1) {
                    Log.v(MtkWfcUtility.TAG, "Clear Saved Network Channel due to Wi-Fi disabled");
                    MtkWfcUtility.clearSavedNetworkChannel();
                }
            }
        }, new IntentFilter("wifi_scan_available"));
    }

    public static void updateSavedNetworkChannel(List<ScanDetail> list) {
        WifiConfigManager wifiConfigManager = WifiInjector.getInstance().getWifiConfigManager();
        for (ScanDetail scanDetail : list) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (wifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail) != null) {
                mSavedNetworkChannelSet.add(Integer.valueOf(scanResult.frequency));
            }
        }
    }

    public static void clearSavedNetworkChannel() {
        mSavedNetworkChannelSet.clear();
    }

    public static Set<Integer> startPartialScanForSavedChannel() {
        WifiConfigManager wifiConfigManager = WifiInjector.getInstance().getWifiConfigManager();
        WifiScanner.ScanSettings scanSettings = new WifiScanner.ScanSettings();
        scanSettings.reportEvents = 3;
        scanSettings.type = 2;
        List<WifiScanner.ScanSettings.HiddenNetwork> listRetrieveHiddenNetworkList = wifiConfigManager.retrieveHiddenNetworkList();
        scanSettings.hiddenNetworks = (WifiScanner.ScanSettings.HiddenNetwork[]) listRetrieveHiddenNetworkList.toArray(new WifiScanner.ScanSettings.HiddenNetwork[listRetrieveHiddenNetworkList.size()]);
        Set<Integer> set = mSavedNetworkChannelSet;
        scanSettings.channels = new WifiScanner.ChannelSpec[set.size()];
        if (set != null && set.size() != 0) {
            scanSettings.channels = new WifiScanner.ChannelSpec[set.size()];
            Iterator<Integer> it = set.iterator();
            int i = 0;
            while (it.hasNext()) {
                scanSettings.channels[i] = new WifiScanner.ChannelSpec(it.next().intValue());
                i++;
            }
            scanSettings.band = 0;
        } else {
            scanSettings.band = 7;
        }
        WifiInjector.getInstance().getWifiScanner().startScan(scanSettings, new WifiScanner.ScanListener() {
            public void onSuccess() {
            }

            public void onFailure(int i2, String str) {
            }

            public void onResults(WifiScanner.ScanData[] scanDataArr) {
            }

            public void onFullResult(ScanResult scanResult) {
            }

            public void onPeriodChanged(int i2) {
            }
        }, WIFI_WORK_SOURCE);
        if (scanSettings.band == 0) {
            Log.v(TAG, "Start partial scan for channels " + set);
        } else {
            Log.v(TAG, "Start full scan since no saved channel available");
        }
        return set;
    }
}
