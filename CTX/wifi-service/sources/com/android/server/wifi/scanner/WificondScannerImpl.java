package com.android.server.wifi.scanner;

import android.R;
import android.app.AlarmManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;

public class WificondScannerImpl extends WifiScannerImpl implements Handler.Callback {
    private static final boolean DBG = false;
    private static final int MAX_APS_PER_SCAN = 32;
    public static final int MAX_HIDDEN_NETWORK_IDS_PER_SCAN = 16;
    private static final int MAX_SCAN_BUCKETS = 16;
    private static final int SCAN_BUFFER_CAPACITY = 10;
    private static final long SCAN_TIMEOUT_MS = 15000;
    private static final String TAG = "WificondScannerImpl";
    public static final String TIMEOUT_ALARM_TAG = "WificondScannerImpl Scan Timeout";
    private final AlarmManager mAlarmManager;
    private final ChannelHelper mChannelHelper;
    private final Clock mClock;
    private final Context mContext;
    private final Handler mEventHandler;
    private final boolean mHwPnoScanSupported;
    private final String mIfaceName;
    private ArrayList<ScanDetail> mNativePnoScanResults;
    private ArrayList<ScanDetail> mNativeScanResults;

    @GuardedBy("mSettingsLock")
    private AlarmManager.OnAlarmListener mScanTimeoutListener;
    private final WifiMonitor mWifiMonitor;
    private final WifiNative mWifiNative;
    private final Object mSettingsLock = new Object();
    private WifiScanner.ScanData mLatestSingleScanResult = new WifiScanner.ScanData(0, 0, new ScanResult[0]);
    private LastScanSettings mLastScanSettings = null;
    private LastPnoScanSettings mLastPnoScanSettings = null;

    public WificondScannerImpl(Context context, String str, WifiNative wifiNative, WifiMonitor wifiMonitor, ChannelHelper channelHelper, Looper looper, Clock clock) {
        this.mContext = context;
        this.mIfaceName = str;
        this.mWifiNative = wifiNative;
        this.mWifiMonitor = wifiMonitor;
        this.mChannelHelper = channelHelper;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mEventHandler = new Handler(looper, this);
        this.mClock = clock;
        this.mHwPnoScanSupported = this.mContext.getResources().getBoolean(R.^attr-private.preferenceFragmentListStyle);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.PNO_SCAN_RESULTS_EVENT, this.mEventHandler);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
    }

    @Override
    public void cleanup() {
        synchronized (this.mSettingsLock) {
            stopHwPnoScan();
            this.mLastScanSettings = null;
            this.mLastPnoScanSettings = null;
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.PNO_SCAN_RESULTS_EVENT, this.mEventHandler);
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
        }
    }

    @Override
    public boolean getScanCapabilities(WifiNative.ScanCapabilities scanCapabilities) {
        scanCapabilities.max_scan_cache_size = ScoringParams.Values.MAX_EXPID;
        scanCapabilities.max_scan_buckets = 16;
        scanCapabilities.max_ap_cache_per_scan = 32;
        scanCapabilities.max_rssi_sample_size = 8;
        scanCapabilities.max_scan_reporting_threshold = 10;
        return true;
    }

    @Override
    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    @Override
    public boolean startSingleScan(WifiNative.ScanSettings scanSettings, WifiNative.ScanEventHandler scanEventHandler) {
        boolean zScan = DBG;
        if (scanEventHandler == null || scanSettings == null) {
            Log.w(TAG, "Invalid arguments for startSingleScan: settings=" + scanSettings + ",eventHandler=" + scanEventHandler);
            return DBG;
        }
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null) {
                Log.w(TAG, "A single scan is already running");
                return DBG;
            }
            ChannelHelper.ChannelCollection channelCollectionCreateChannelCollection = this.mChannelHelper.createChannelCollection();
            boolean z = false;
            for (int i = 0; i < scanSettings.num_buckets; i++) {
                WifiNative.BucketSettings bucketSettings = scanSettings.buckets[i];
                if ((bucketSettings.report_events & 2) != 0) {
                    z = true;
                }
                channelCollectionCreateChannelCollection.addChannels(bucketSettings);
            }
            ArrayList arrayList = new ArrayList();
            if (scanSettings.hiddenNetworks != null) {
                int iMin = Math.min(scanSettings.hiddenNetworks.length, 16);
                for (int i2 = 0; i2 < iMin; i2++) {
                    arrayList.add(scanSettings.hiddenNetworks[i2].ssid);
                }
            }
            this.mLastScanSettings = new LastScanSettings(this.mClock.getElapsedSinceBootMillis(), z, channelCollectionCreateChannelCollection, scanEventHandler);
            if (!channelCollectionCreateChannelCollection.isEmpty()) {
                Set<Integer> scanFreqs = channelCollectionCreateChannelCollection.getScanFreqs();
                zScan = this.mWifiNative.scan(this.mIfaceName, scanSettings.scanType, scanFreqs, arrayList);
                if (!zScan) {
                    Log.e(TAG, "Failed to start scan, freqs=" + scanFreqs);
                }
            } else {
                Log.e(TAG, "Failed to start scan because there is no available channel to scan");
            }
            if (zScan) {
                this.mScanTimeoutListener = new AlarmManager.OnAlarmListener() {
                    @Override
                    public void onAlarm() {
                        WificondScannerImpl.this.handleScanTimeout();
                    }
                };
                this.mAlarmManager.set(2, SCAN_TIMEOUT_MS + this.mClock.getElapsedSinceBootMillis(), TIMEOUT_ALARM_TAG, this.mScanTimeoutListener, this.mEventHandler);
            } else {
                this.mEventHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        WificondScannerImpl.this.reportScanFailure();
                    }
                });
            }
            return true;
        }
    }

    @Override
    public WifiScanner.ScanData getLatestSingleScanResults() {
        return this.mLatestSingleScanResult;
    }

    @Override
    public boolean startBatchedScan(WifiNative.ScanSettings scanSettings, WifiNative.ScanEventHandler scanEventHandler) {
        Log.w(TAG, "startBatchedScan() is not supported");
        return DBG;
    }

    @Override
    public void stopBatchedScan() {
        Log.w(TAG, "stopBatchedScan() is not supported");
    }

    @Override
    public void pauseBatchedScan() {
        Log.w(TAG, "pauseBatchedScan() is not supported");
    }

    @Override
    public void restartBatchedScan() {
        Log.w(TAG, "restartBatchedScan() is not supported");
    }

    private void handleScanTimeout() {
        synchronized (this.mSettingsLock) {
            Log.e(TAG, "Timed out waiting for scan result from wificond");
            reportScanFailure();
            this.mScanTimeoutListener = null;
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        int i = message.what;
        if (i != 147461) {
            switch (i) {
                case WifiMonitor.SCAN_FAILED_EVENT:
                    Log.w(TAG, "Scan failed");
                    cancelScanTimeout();
                    reportScanFailure();
                    break;
                case WifiMonitor.PNO_SCAN_RESULTS_EVENT:
                    pollLatestScanDataForPno();
                    break;
            }
            return true;
        }
        cancelScanTimeout();
        pollLatestScanData();
        return true;
    }

    private void cancelScanTimeout() {
        synchronized (this.mSettingsLock) {
            if (this.mScanTimeoutListener != null) {
                this.mAlarmManager.cancel(this.mScanTimeoutListener);
                this.mScanTimeoutListener = null;
            }
        }
    }

    private void reportScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null) {
                if (this.mLastScanSettings.singleScanEventHandler != null) {
                    this.mLastScanSettings.singleScanEventHandler.onScanStatus(3);
                }
                this.mLastScanSettings = null;
            }
        }
    }

    private void reportPnoScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings != null) {
                if (this.mLastPnoScanSettings.pnoScanEventHandler != null) {
                    this.mLastPnoScanSettings.pnoScanEventHandler.onPnoScanFailed();
                }
                this.mLastPnoScanSettings = null;
            }
        }
    }

    private void pollLatestScanDataForPno() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings == null) {
                return;
            }
            this.mNativePnoScanResults = this.mWifiNative.getPnoScanResults(this.mIfaceName);
            ArrayList arrayList = new ArrayList();
            int i = 0;
            for (int i2 = 0; i2 < this.mNativePnoScanResults.size(); i2++) {
                ScanResult scanResult = this.mNativePnoScanResults.get(i2).getScanResult();
                long j = scanResult.timestamp / 1000;
                if (j > this.mLastPnoScanSettings.startTime) {
                    arrayList.add(scanResult);
                } else {
                    i++;
                    Log.d(TAG, "pollLatestScanDataForPno filtered by timestamp: " + j + " BSSID: " + scanResult.BSSID + " freq: " + scanResult.frequency);
                }
            }
            if (i != 0) {
                Log.d(TAG, "Filtering out " + i + " pno scan results.");
            }
            if (this.mLastPnoScanSettings.pnoScanEventHandler != null) {
                this.mLastPnoScanSettings.pnoScanEventHandler.onPnoNetworkFound((ScanResult[]) arrayList.toArray(new ScanResult[arrayList.size()]));
            }
        }
    }

    private static boolean isAllChannelsScanned(ChannelHelper.ChannelCollection channelCollection) {
        if (channelCollection.containsBand(1) && channelCollection.containsBand(2)) {
            return true;
        }
        return DBG;
    }

    private void pollLatestScanData() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings == null) {
                return;
            }
            this.mNativeScanResults = this.mWifiNative.getScanResults(this.mIfaceName);
            ArrayList arrayList = new ArrayList();
            int i = 0;
            for (int i2 = 0; i2 < this.mNativeScanResults.size(); i2++) {
                ScanResult scanResult = this.mNativeScanResults.get(i2).getScanResult();
                long j = scanResult.timestamp / 1000;
                if (j > this.mLastScanSettings.startTime) {
                    if (this.mLastScanSettings.singleScanFreqs.containsChannel(scanResult.frequency)) {
                        arrayList.add(scanResult);
                    } else {
                        Log.d(TAG, "pollLatestScanData filtered by freq: " + scanResult.frequency + " BSSID: " + scanResult.BSSID);
                    }
                } else {
                    i++;
                    Log.d(TAG, "pollLatestScanData filtered by timestamp: " + j + " BSSID: " + scanResult.BSSID + " freq: " + scanResult.frequency);
                }
            }
            if (i != 0) {
                Log.d(TAG, "Filtering out " + i + " scan results.");
            }
            if (this.mLastScanSettings.singleScanEventHandler != null) {
                if (this.mLastScanSettings.reportSingleScanFullResults) {
                    Iterator it = arrayList.iterator();
                    while (it.hasNext()) {
                        this.mLastScanSettings.singleScanEventHandler.onFullScanResult((ScanResult) it.next(), 0);
                    }
                }
                Collections.sort(arrayList, SCAN_RESULT_SORT_COMPARATOR);
                this.mLatestSingleScanResult = new WifiScanner.ScanData(0, 0, 0, isAllChannelsScanned(this.mLastScanSettings.singleScanFreqs), (ScanResult[]) arrayList.toArray(new ScanResult[arrayList.size()]));
                this.mLastScanSettings.singleScanEventHandler.onScanStatus(0);
            }
            this.mLastScanSettings = null;
        }
    }

    @Override
    public WifiScanner.ScanData[] getLatestBatchedScanResults(boolean z) {
        return null;
    }

    private boolean startHwPnoScan(WifiNative.PnoSettings pnoSettings) {
        return this.mWifiNative.startPnoScan(this.mIfaceName, pnoSettings);
    }

    private void stopHwPnoScan() {
        this.mWifiNative.stopPnoScan(this.mIfaceName);
    }

    private boolean isHwPnoScanRequired(boolean z) {
        if (z || !this.mHwPnoScanSupported) {
            return DBG;
        }
        return true;
    }

    @Override
    public boolean setHwPnoList(WifiNative.PnoSettings pnoSettings, WifiNative.PnoEventHandler pnoEventHandler) {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings != null) {
                Log.w(TAG, "Already running a PNO scan");
                return DBG;
            }
            if (!isHwPnoScanRequired(pnoSettings.isConnected)) {
                return DBG;
            }
            if (startHwPnoScan(pnoSettings)) {
                this.mLastPnoScanSettings = new LastPnoScanSettings(this.mClock.getElapsedSinceBootMillis(), pnoSettings.networkList, pnoEventHandler);
            } else {
                Log.e(TAG, "Failed to start PNO scan");
                reportPnoScanFailure();
            }
            return true;
        }
    }

    @Override
    public boolean resetHwPnoList() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings == null) {
                Log.w(TAG, "No PNO scan running");
                return DBG;
            }
            this.mLastPnoScanSettings = null;
            stopHwPnoScan();
            return true;
        }
    }

    @Override
    public boolean isHwPnoSupported(boolean z) {
        return isHwPnoScanRequired(z);
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mSettingsLock) {
            long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
            printWriter.println("Latest native scan results:");
            if (this.mNativeScanResults != null) {
                ScanResultUtil.dumpScanResults(printWriter, (List) this.mNativeScanResults.stream().map(new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return ((ScanDetail) obj).getScanResult();
                    }
                }).collect(Collectors.toList()), elapsedSinceBootMillis);
            }
            printWriter.println("Latest native pno scan results:");
            if (this.mNativePnoScanResults != null) {
                ScanResultUtil.dumpScanResults(printWriter, (List) this.mNativePnoScanResults.stream().map(new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return ((ScanDetail) obj).getScanResult();
                    }
                }).collect(Collectors.toList()), elapsedSinceBootMillis);
            }
        }
    }

    private static class LastScanSettings {
        public boolean reportSingleScanFullResults;
        public WifiNative.ScanEventHandler singleScanEventHandler;
        public ChannelHelper.ChannelCollection singleScanFreqs;
        public long startTime;

        LastScanSettings(long j, boolean z, ChannelHelper.ChannelCollection channelCollection, WifiNative.ScanEventHandler scanEventHandler) {
            this.startTime = j;
            this.reportSingleScanFullResults = z;
            this.singleScanFreqs = channelCollection;
            this.singleScanEventHandler = scanEventHandler;
        }
    }

    private static class LastPnoScanSettings {
        public WifiNative.PnoNetwork[] pnoNetworkList;
        public WifiNative.PnoEventHandler pnoScanEventHandler;
        public long startTime;

        LastPnoScanSettings(long j, WifiNative.PnoNetwork[] pnoNetworkArr, WifiNative.PnoEventHandler pnoEventHandler) {
            this.startTime = j;
            this.pnoNetworkList = pnoNetworkArr;
            this.pnoScanEventHandler = pnoEventHandler;
        }
    }
}
