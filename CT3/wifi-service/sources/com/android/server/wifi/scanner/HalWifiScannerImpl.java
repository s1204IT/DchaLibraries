package com.android.server.wifi.scanner;

import android.content.Context;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiNative;

public class HalWifiScannerImpl extends WifiScannerImpl implements Handler.Callback {
    private static final boolean DBG = true;
    private static final String TAG = "HalWifiScannerImpl";
    private final ChannelHelper mChannelHelper;
    private final boolean mHalBasedPnoSupported = false;
    private final SupplicantWifiScannerImpl mSupplicantScannerDelegate;
    private final WifiNative mWifiNative;

    public HalWifiScannerImpl(Context context, WifiNative wifiNative, Looper looper, Clock clock) {
        this.mWifiNative = wifiNative;
        this.mChannelHelper = new HalChannelHelper(wifiNative);
        this.mSupplicantScannerDelegate = new SupplicantWifiScannerImpl(context, wifiNative, this.mChannelHelper, looper, clock);
        Log.d(TAG, "HalWifiScannerImpl is created");
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.w(TAG, "Unknown message received: " + msg.what);
        return true;
    }

    @Override
    public void cleanup() {
        this.mSupplicantScannerDelegate.cleanup();
    }

    @Override
    public boolean getScanCapabilities(WifiNative.ScanCapabilities capabilities) {
        return this.mWifiNative.getScanCapabilities(capabilities);
    }

    @Override
    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    @Override
    public boolean startSingleScan(WifiNative.ScanSettings settings, WifiNative.ScanEventHandler eventHandler) {
        return this.mSupplicantScannerDelegate.startSingleScan(settings, eventHandler);
    }

    @Override
    public WifiScanner.ScanData getLatestSingleScanResults() {
        return this.mSupplicantScannerDelegate.getLatestSingleScanResults();
    }

    @Override
    public boolean startBatchedScan(WifiNative.ScanSettings settings, WifiNative.ScanEventHandler eventHandler) {
        if (settings == null || eventHandler == null) {
            Log.w(TAG, "Invalid arguments for startBatched: settings=" + settings + ",eventHandler=" + eventHandler);
            return false;
        }
        return this.mWifiNative.startScan(settings, eventHandler);
    }

    @Override
    public void stopBatchedScan() {
        this.mWifiNative.stopScan();
    }

    @Override
    public void pauseBatchedScan() {
        this.mWifiNative.pauseScan();
    }

    @Override
    public void restartBatchedScan() {
        this.mWifiNative.restartScan();
    }

    @Override
    public WifiScanner.ScanData[] getLatestBatchedScanResults(boolean flush) {
        return this.mWifiNative.getScanResults(flush);
    }

    @Override
    public boolean setHwPnoList(WifiNative.PnoSettings settings, WifiNative.PnoEventHandler eventHandler) {
        if (this.mHalBasedPnoSupported) {
            return this.mWifiNative.setPnoList(settings, eventHandler);
        }
        return this.mSupplicantScannerDelegate.setHwPnoList(settings, eventHandler);
    }

    @Override
    public boolean resetHwPnoList() {
        if (this.mHalBasedPnoSupported) {
            return this.mWifiNative.resetPnoList();
        }
        return this.mSupplicantScannerDelegate.resetHwPnoList();
    }

    @Override
    public boolean isHwPnoSupported(boolean isConnectedPno) {
        if (this.mHalBasedPnoSupported) {
            return true;
        }
        return this.mSupplicantScannerDelegate.isHwPnoSupported(isConnectedPno);
    }

    @Override
    public boolean shouldScheduleBackgroundScanForHwPno() {
        if (this.mHalBasedPnoSupported) {
            return true;
        }
        return this.mSupplicantScannerDelegate.shouldScheduleBackgroundScanForHwPno();
    }

    @Override
    public boolean setHotlist(WifiScanner.HotlistSettings settings, WifiNative.HotlistEventHandler eventHandler) {
        return this.mWifiNative.setHotlist(settings, eventHandler);
    }

    @Override
    public void resetHotlist() {
        this.mWifiNative.resetHotlist();
    }

    @Override
    public boolean trackSignificantWifiChange(WifiScanner.WifiChangeSettings settings, WifiNative.SignificantWifiChangeEventHandler handler) {
        return this.mWifiNative.trackSignificantWifiChange(settings, handler);
    }

    @Override
    public void untrackSignificantWifiChange() {
        this.mWifiNative.untrackSignificantWifiChange();
    }
}
