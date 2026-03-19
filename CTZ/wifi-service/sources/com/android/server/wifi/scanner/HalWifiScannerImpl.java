package com.android.server.wifi.scanner;

import android.content.Context;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class HalWifiScannerImpl extends WifiScannerImpl implements Handler.Callback {
    private static final boolean DBG = false;
    private static final String TAG = "HalWifiScannerImpl";
    private final ChannelHelper mChannelHelper;
    private final String mIfaceName;
    private final WifiNative mWifiNative;
    private final WificondScannerImpl mWificondScannerDelegate;

    public HalWifiScannerImpl(Context context, String str, WifiNative wifiNative, WifiMonitor wifiMonitor, Looper looper, Clock clock) {
        this.mIfaceName = str;
        this.mWifiNative = wifiNative;
        this.mChannelHelper = new WificondChannelHelper(wifiNative);
        this.mWificondScannerDelegate = new WificondScannerImpl(context, this.mIfaceName, wifiNative, wifiMonitor, this.mChannelHelper, looper, clock);
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.w(TAG, "Unknown message received: " + message.what);
        return true;
    }

    @Override
    public void cleanup() {
        this.mWificondScannerDelegate.cleanup();
    }

    @Override
    public boolean getScanCapabilities(WifiNative.ScanCapabilities scanCapabilities) {
        return this.mWifiNative.getBgScanCapabilities(this.mIfaceName, scanCapabilities);
    }

    @Override
    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    @Override
    public boolean startSingleScan(WifiNative.ScanSettings scanSettings, WifiNative.ScanEventHandler scanEventHandler) {
        return this.mWificondScannerDelegate.startSingleScan(scanSettings, scanEventHandler);
    }

    @Override
    public WifiScanner.ScanData getLatestSingleScanResults() {
        return this.mWificondScannerDelegate.getLatestSingleScanResults();
    }

    @Override
    public boolean startBatchedScan(WifiNative.ScanSettings scanSettings, WifiNative.ScanEventHandler scanEventHandler) {
        if (scanSettings == null || scanEventHandler == null) {
            Log.w(TAG, "Invalid arguments for startBatched: settings=" + scanSettings + ",eventHandler=" + scanEventHandler);
            return DBG;
        }
        return this.mWifiNative.startBgScan(this.mIfaceName, scanSettings, scanEventHandler);
    }

    @Override
    public void stopBatchedScan() {
        this.mWifiNative.stopBgScan(this.mIfaceName);
    }

    @Override
    public void pauseBatchedScan() {
        this.mWifiNative.pauseBgScan(this.mIfaceName);
    }

    @Override
    public void restartBatchedScan() {
        this.mWifiNative.restartBgScan(this.mIfaceName);
    }

    @Override
    public WifiScanner.ScanData[] getLatestBatchedScanResults(boolean z) {
        return this.mWifiNative.getBgScanResults(this.mIfaceName);
    }

    @Override
    public boolean setHwPnoList(WifiNative.PnoSettings pnoSettings, WifiNative.PnoEventHandler pnoEventHandler) {
        return this.mWificondScannerDelegate.setHwPnoList(pnoSettings, pnoEventHandler);
    }

    @Override
    public boolean resetHwPnoList() {
        return this.mWificondScannerDelegate.resetHwPnoList();
    }

    @Override
    public boolean isHwPnoSupported(boolean z) {
        return this.mWificondScannerDelegate.isHwPnoSupported(z);
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mWificondScannerDelegate.dump(fileDescriptor, printWriter, strArr);
    }
}
