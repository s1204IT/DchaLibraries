package com.android.server.wifi.scanner;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Looper;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiNative;
import java.util.Comparator;

public abstract class WifiScannerImpl {
    public static final WifiScannerImplFactory DEFAULT_FACTORY = new WifiScannerImplFactory() {
        @Override
        public WifiScannerImpl create(Context context, Looper looper, Clock clock) {
            WifiNative wifiNative = WifiNative.getWlanNativeInterface();
            if (wifiNative.getScanCapabilities(new WifiNative.ScanCapabilities())) {
                Log.d("WifiScannerImpl", "DEFAULT_FACTORY use HalWifiScannerImpl");
                return new HalWifiScannerImpl(context, wifiNative, looper, clock);
            }
            Log.d("WifiScannerImpl", "DEFAULT_FACTORY use SupplicantWifiScannerImpl");
            return new SupplicantWifiScannerImpl(context, wifiNative, looper, clock);
        }
    };
    protected static final Comparator<ScanResult> SCAN_RESULT_SORT_COMPARATOR = new Comparator<ScanResult>() {
        @Override
        public int compare(ScanResult r1, ScanResult r2) {
            return r2.level - r1.level;
        }
    };

    public interface WifiScannerImplFactory {
        WifiScannerImpl create(Context context, Looper looper, Clock clock);
    }

    public abstract void cleanup();

    public abstract ChannelHelper getChannelHelper();

    public abstract WifiScanner.ScanData[] getLatestBatchedScanResults(boolean z);

    public abstract WifiScanner.ScanData getLatestSingleScanResults();

    public abstract boolean getScanCapabilities(WifiNative.ScanCapabilities scanCapabilities);

    public abstract boolean isHwPnoSupported(boolean z);

    public abstract void pauseBatchedScan();

    public abstract void resetHotlist();

    public abstract boolean resetHwPnoList();

    public abstract void restartBatchedScan();

    public abstract boolean setHotlist(WifiScanner.HotlistSettings hotlistSettings, WifiNative.HotlistEventHandler hotlistEventHandler);

    public abstract boolean setHwPnoList(WifiNative.PnoSettings pnoSettings, WifiNative.PnoEventHandler pnoEventHandler);

    public abstract boolean shouldScheduleBackgroundScanForHwPno();

    public abstract boolean startBatchedScan(WifiNative.ScanSettings scanSettings, WifiNative.ScanEventHandler scanEventHandler);

    public abstract boolean startSingleScan(WifiNative.ScanSettings scanSettings, WifiNative.ScanEventHandler scanEventHandler);

    public abstract void stopBatchedScan();

    public abstract boolean trackSignificantWifiChange(WifiScanner.WifiChangeSettings wifiChangeSettings, WifiNative.SignificantWifiChangeEventHandler significantWifiChangeEventHandler);

    public abstract void untrackSignificantWifiChange();
}
