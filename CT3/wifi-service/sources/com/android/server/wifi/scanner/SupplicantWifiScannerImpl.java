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
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SupplicantWifiScannerImpl extends WifiScannerImpl implements Handler.Callback {
    private static final String ACTION_SCAN_PERIOD = "com.android.server.util.SupplicantWifiScannerImpl.action.SCAN_PERIOD";
    public static final String BACKGROUND_PERIOD_ALARM_TAG = "SupplicantWifiScannerImpl Background Scan Period";
    private static final boolean DBG = true;
    private static final int MAX_APS_PER_SCAN = 32;
    public static final int MAX_HIDDEN_NETWORK_IDS_PER_SCAN = 16;
    private static final int MAX_SCAN_BUCKETS = 16;
    private static final int SCAN_BUFFER_CAPACITY = 10;
    private static final long SCAN_TIMEOUT_MS = 15000;
    private static final String TAG = "SupplicantWifiScannerImpl";
    public static final String TIMEOUT_ALARM_TAG = "SupplicantWifiScannerImpl Scan Timeout";
    private final AlarmManager mAlarmManager;
    private ScanBuffer mBackgroundScanBuffer;
    private WifiNative.ScanEventHandler mBackgroundScanEventHandler;
    private boolean mBackgroundScanPaused;
    private boolean mBackgroundScanPeriodPending;
    private WifiNative.ScanSettings mBackgroundScanSettings;
    private final ChannelHelper mChannelHelper;
    private final Clock mClock;
    private final Context mContext;
    private final Handler mEventHandler;
    private ChangeBuffer mHotlistChangeBuffer;
    private WifiNative.HotlistEventHandler mHotlistHandler;
    private final HwPnoDebouncer mHwPnoDebouncer;
    private final HwPnoDebouncer.Listener mHwPnoDebouncerListener;
    private final boolean mHwPnoScanSupported;
    private LastScanSettings mLastScanSettings;
    private WifiScanner.ScanData mLatestSingleScanResult;
    private int mNextBackgroundScanId;
    private int mNextBackgroundScanPeriod;
    private WifiNative.ScanEventHandler mPendingBackgroundScanEventHandler;
    private WifiNative.ScanSettings mPendingBackgroundScanSettings;
    private WifiNative.ScanEventHandler mPendingSingleScanEventHandler;
    private WifiNative.ScanSettings mPendingSingleScanSettings;
    private WifiNative.PnoEventHandler mPnoEventHandler;
    private WifiNative.PnoSettings mPnoSettings;
    AlarmManager.OnAlarmListener mScanPeriodListener;
    AlarmManager.OnAlarmListener mScanTimeoutListener;
    private Object mSettingsLock;
    private final WifiNative mWifiNative;

    public SupplicantWifiScannerImpl(Context context, WifiNative wifiNative, ChannelHelper channelHelper, Looper looper, Clock clock) {
        this.mSettingsLock = new Object();
        this.mPendingBackgroundScanSettings = null;
        this.mPendingBackgroundScanEventHandler = null;
        this.mPendingSingleScanSettings = null;
        this.mPendingSingleScanEventHandler = null;
        this.mBackgroundScanSettings = null;
        this.mBackgroundScanEventHandler = null;
        this.mNextBackgroundScanPeriod = 0;
        this.mNextBackgroundScanId = 0;
        this.mBackgroundScanPeriodPending = false;
        this.mBackgroundScanPaused = false;
        this.mBackgroundScanBuffer = new ScanBuffer(10);
        this.mLatestSingleScanResult = new WifiScanner.ScanData(0, 0, new ScanResult[0]);
        this.mLastScanSettings = null;
        this.mHotlistHandler = null;
        this.mHotlistChangeBuffer = new ChangeBuffer(null);
        this.mPnoSettings = null;
        this.mHwPnoDebouncerListener = new HwPnoDebouncer.Listener() {
            @Override
            public void onPnoScanFailed() {
                Log.e(SupplicantWifiScannerImpl.TAG, "Pno scan failure received");
                SupplicantWifiScannerImpl.this.reportPnoScanFailure();
            }
        };
        this.mScanPeriodListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                synchronized (SupplicantWifiScannerImpl.this.mSettingsLock) {
                    SupplicantWifiScannerImpl.this.handleScanPeriod();
                }
            }
        };
        this.mScanTimeoutListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                synchronized (SupplicantWifiScannerImpl.this.mSettingsLock) {
                    SupplicantWifiScannerImpl.this.handleScanTimeout();
                }
            }
        };
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mChannelHelper = channelHelper;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mEventHandler = new Handler(looper, this);
        this.mClock = clock;
        this.mHwPnoDebouncer = new HwPnoDebouncer(this.mWifiNative, this.mAlarmManager, this.mEventHandler, this.mClock);
        Log.d(TAG, "SupplicantWifiScannerImpl is created");
        this.mHwPnoScanSupported = this.mContext.getResources().getBoolean(R.^attr-private.backgroundLeft);
        WifiMonitor.getInstance().registerHandler(this.mWifiNative.getInterfaceName(), WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
        WifiMonitor.getInstance().registerHandler(this.mWifiNative.getInterfaceName(), WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
    }

    public SupplicantWifiScannerImpl(Context context, WifiNative wifiNative, Looper looper, Clock clock) {
        this(context, wifiNative, new NoBandChannelHelper(), looper, clock);
    }

    @Override
    public void cleanup() {
        synchronized (this.mSettingsLock) {
            this.mPendingSingleScanSettings = null;
            this.mPendingSingleScanEventHandler = null;
            stopHwPnoScan();
            stopBatchedScan();
            resetHotlist();
            untrackSignificantWifiChange();
            this.mLastScanSettings = null;
        }
    }

    @Override
    public boolean getScanCapabilities(WifiNative.ScanCapabilities capabilities) {
        capabilities.max_scan_cache_size = Integer.MAX_VALUE;
        capabilities.max_scan_buckets = 16;
        capabilities.max_ap_cache_per_scan = 32;
        capabilities.max_rssi_sample_size = 8;
        capabilities.max_scan_reporting_threshold = 10;
        capabilities.max_hotlist_bssids = 0;
        capabilities.max_significant_wifi_change_aps = 0;
        return true;
    }

    @Override
    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    @Override
    public boolean startSingleScan(WifiNative.ScanSettings settings, WifiNative.ScanEventHandler eventHandler) {
        if (eventHandler == null || settings == null) {
            Log.w(TAG, "Invalid arguments for startSingleScan: settings=" + settings + ",eventHandler=" + eventHandler);
            return false;
        }
        if (this.mPendingSingleScanSettings != null || (this.mLastScanSettings != null && this.mLastScanSettings.singleScanActive)) {
            Log.w(TAG, "A single scan is already running");
            return false;
        }
        synchronized (this.mSettingsLock) {
            this.mPendingSingleScanSettings = settings;
            this.mPendingSingleScanEventHandler = eventHandler;
            processPendingScans();
        }
        return true;
    }

    @Override
    public WifiScanner.ScanData getLatestSingleScanResults() {
        return this.mLatestSingleScanResult;
    }

    @Override
    public boolean startBatchedScan(WifiNative.ScanSettings settings, WifiNative.ScanEventHandler eventHandler) {
        if (settings == null || eventHandler == null) {
            Log.w(TAG, "Invalid arguments for startBatched: settings=" + settings + ",eventHandler=" + eventHandler);
            return false;
        }
        if (settings.max_ap_per_scan < 0 || settings.max_ap_per_scan > 32 || settings.num_buckets < 0 || settings.num_buckets > 16 || settings.report_threshold_num_scans < 0 || settings.report_threshold_num_scans > 10 || settings.report_threshold_percent < 0 || settings.report_threshold_percent > 100 || settings.base_period_ms <= 0) {
            return false;
        }
        for (int i = 0; i < settings.num_buckets; i++) {
            WifiNative.BucketSettings bucket = settings.buckets[i];
            if (bucket.period_ms % settings.base_period_ms != 0) {
                return false;
            }
        }
        synchronized (this.mSettingsLock) {
            stopBatchedScan();
            Log.d(TAG, "Starting scan num_buckets=" + settings.num_buckets + ", base_period=" + settings.base_period_ms + " ms");
            this.mPendingBackgroundScanSettings = settings;
            this.mPendingBackgroundScanEventHandler = eventHandler;
            handleScanPeriod();
        }
        return true;
    }

    @Override
    public void stopBatchedScan() {
        synchronized (this.mSettingsLock) {
            Log.d(TAG, "Stopping scan");
            this.mBackgroundScanSettings = null;
            this.mBackgroundScanEventHandler = null;
            this.mPendingBackgroundScanSettings = null;
            this.mPendingBackgroundScanEventHandler = null;
            this.mBackgroundScanPaused = false;
            this.mBackgroundScanPeriodPending = false;
            unscheduleScansLocked();
        }
        processPendingScans();
    }

    @Override
    public void pauseBatchedScan() {
        synchronized (this.mSettingsLock) {
            Log.d(TAG, "Pausing scan");
            if (this.mPendingBackgroundScanSettings == null) {
                this.mPendingBackgroundScanSettings = this.mBackgroundScanSettings;
                this.mPendingBackgroundScanEventHandler = this.mBackgroundScanEventHandler;
            }
            this.mBackgroundScanSettings = null;
            this.mBackgroundScanEventHandler = null;
            this.mBackgroundScanPeriodPending = false;
            this.mBackgroundScanPaused = true;
            unscheduleScansLocked();
            WifiScanner.ScanData[] results = getLatestBatchedScanResults(true);
            if (this.mPendingBackgroundScanEventHandler != null) {
                this.mPendingBackgroundScanEventHandler.onScanPaused(results);
            }
        }
        processPendingScans();
    }

    @Override
    public void restartBatchedScan() {
        synchronized (this.mSettingsLock) {
            Log.d(TAG, "Restarting scan");
            if (this.mPendingBackgroundScanEventHandler != null) {
                this.mPendingBackgroundScanEventHandler.onScanRestarted();
            }
            this.mBackgroundScanPaused = false;
            handleScanPeriod();
        }
    }

    private void unscheduleScansLocked() {
        this.mAlarmManager.cancel(this.mScanPeriodListener);
        if (this.mLastScanSettings == null) {
            return;
        }
        this.mLastScanSettings.backgroundScanActive = false;
    }

    private void handleScanPeriod() {
        synchronized (this.mSettingsLock) {
            this.mBackgroundScanPeriodPending = true;
            processPendingScans();
        }
    }

    private void handleScanTimeout() {
        Log.e(TAG, "Timed out waiting for scan result from supplicant");
        reportScanFailure();
        processPendingScans();
    }

    private void processPendingScans() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings == null || this.mLastScanSettings.hwPnoScanActive) {
                ChannelHelper.ChannelCollection allFreqs = this.mChannelHelper.createChannelCollection();
                Set<Integer> hiddenNetworkIdSet = new HashSet<>();
                final LastScanSettings newScanSettings = new LastScanSettings(this.mClock.elapsedRealtime());
                if (!this.mBackgroundScanPaused) {
                    if (this.mPendingBackgroundScanSettings != null) {
                        this.mBackgroundScanSettings = this.mPendingBackgroundScanSettings;
                        this.mBackgroundScanEventHandler = this.mPendingBackgroundScanEventHandler;
                        this.mNextBackgroundScanPeriod = 0;
                        this.mPendingBackgroundScanSettings = null;
                        this.mPendingBackgroundScanEventHandler = null;
                        this.mBackgroundScanPeriodPending = true;
                    }
                    if (this.mBackgroundScanPeriodPending && this.mBackgroundScanSettings != null) {
                        int reportEvents = 4;
                        for (int bucket_id = 0; bucket_id < this.mBackgroundScanSettings.num_buckets; bucket_id++) {
                            WifiNative.BucketSettings bucket = this.mBackgroundScanSettings.buckets[bucket_id];
                            if (this.mNextBackgroundScanPeriod % (bucket.period_ms / this.mBackgroundScanSettings.base_period_ms) == 0) {
                                if ((bucket.report_events & 1) != 0) {
                                    reportEvents |= 1;
                                }
                                if ((bucket.report_events & 2) != 0) {
                                    reportEvents |= 2;
                                }
                                if ((bucket.report_events & 4) == 0) {
                                    reportEvents &= -5;
                                }
                                allFreqs.addChannels(bucket);
                            }
                        }
                        if (!allFreqs.isEmpty()) {
                            int i = this.mNextBackgroundScanId;
                            this.mNextBackgroundScanId = i + 1;
                            newScanSettings.setBackgroundScan(i, this.mBackgroundScanSettings.max_ap_per_scan, reportEvents, this.mBackgroundScanSettings.report_threshold_num_scans, this.mBackgroundScanSettings.report_threshold_percent);
                        }
                        int[] hiddenNetworkIds = this.mBackgroundScanSettings.hiddenNetworkIds;
                        if (hiddenNetworkIds != null) {
                            int numHiddenNetworkIds = Math.min(hiddenNetworkIds.length, 16);
                            for (int i2 = 0; i2 < numHiddenNetworkIds; i2++) {
                                hiddenNetworkIdSet.add(Integer.valueOf(hiddenNetworkIds[i2]));
                            }
                        }
                        this.mNextBackgroundScanPeriod++;
                        this.mBackgroundScanPeriodPending = false;
                        this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + ((long) this.mBackgroundScanSettings.base_period_ms), BACKGROUND_PERIOD_ALARM_TAG, this.mScanPeriodListener, this.mEventHandler);
                    }
                }
                if (this.mPendingSingleScanSettings != null) {
                    boolean reportFullResults = false;
                    ChannelHelper.ChannelCollection singleScanFreqs = this.mChannelHelper.createChannelCollection();
                    for (int i3 = 0; i3 < this.mPendingSingleScanSettings.num_buckets; i3++) {
                        WifiNative.BucketSettings bucketSettings = this.mPendingSingleScanSettings.buckets[i3];
                        if ((bucketSettings.report_events & 2) != 0) {
                            reportFullResults = true;
                        }
                        singleScanFreqs.addChannels(bucketSettings);
                        allFreqs.addChannels(bucketSettings);
                    }
                    newScanSettings.setSingleScan(reportFullResults, singleScanFreqs, this.mPendingSingleScanEventHandler);
                    int[] hiddenNetworkIds2 = this.mPendingSingleScanSettings.hiddenNetworkIds;
                    if (hiddenNetworkIds2 != null) {
                        int numHiddenNetworkIds2 = Math.min(hiddenNetworkIds2.length, 16);
                        for (int i4 = 0; i4 < numHiddenNetworkIds2; i4++) {
                            hiddenNetworkIdSet.add(Integer.valueOf(hiddenNetworkIds2[i4]));
                        }
                    }
                    this.mPendingSingleScanSettings = null;
                    this.mPendingSingleScanEventHandler = null;
                }
                if ((newScanSettings.backgroundScanActive || newScanSettings.singleScanActive) && !allFreqs.isEmpty()) {
                    pauseHwPnoScan();
                    Set<Integer> freqs = allFreqs.getSupplicantScanFreqs();
                    boolean success = this.mWifiNative.scan(freqs, hiddenNetworkIdSet);
                    if (success) {
                        Log.d(TAG, "Starting wifi scan for freqs=" + freqs + ", background=" + newScanSettings.backgroundScanActive + ", single=" + newScanSettings.singleScanActive);
                        this.mLastScanSettings = newScanSettings;
                        this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + SCAN_TIMEOUT_MS, TIMEOUT_ALARM_TAG, this.mScanTimeoutListener, this.mEventHandler);
                    } else {
                        Log.e(TAG, "Failed to start scan, freqs=" + freqs);
                        this.mEventHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (newScanSettings.singleScanEventHandler == null) {
                                    return;
                                }
                                newScanSettings.singleScanEventHandler.onScanStatus(3);
                            }
                        });
                    }
                } else if (isHwPnoScanRequired()) {
                    newScanSettings.setHwPnoScan(this.mPnoEventHandler);
                    if (startHwPnoScan()) {
                        this.mLastScanSettings = newScanSettings;
                    } else {
                        Log.e(TAG, "Failed to start PNO scan");
                        this.mEventHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (SupplicantWifiScannerImpl.this.mPnoEventHandler != null) {
                                    SupplicantWifiScannerImpl.this.mPnoEventHandler.onPnoScanFailed();
                                }
                                SupplicantWifiScannerImpl.this.mPnoSettings = null;
                                SupplicantWifiScannerImpl.this.mPnoEventHandler = null;
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WifiMonitor.SCAN_RESULTS_EVENT:
                this.mAlarmManager.cancel(this.mScanTimeoutListener);
                pollLatestScanData();
                processPendingScans();
                break;
            case WifiMonitor.SCAN_FAILED_EVENT:
                Log.w(TAG, "Scan failed");
                this.mAlarmManager.cancel(this.mScanTimeoutListener);
                reportScanFailure();
                processPendingScans();
                break;
        }
        return true;
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
            if (this.mLastScanSettings != null && this.mLastScanSettings.hwPnoScanActive) {
                if (this.mLastScanSettings.pnoScanEventHandler != null) {
                    this.mLastScanSettings.pnoScanEventHandler.onPnoScanFailed();
                }
                this.mPnoSettings = null;
                this.mPnoEventHandler = null;
                this.mLastScanSettings = null;
            }
        }
    }

    private void pollLatestScanData() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings == null) {
                return;
            }
            Log.d(TAG, "Polling scan data for scan: " + this.mLastScanSettings.scanId);
            ArrayList<ScanDetail> nativeResults = this.mWifiNative.getScanResults();
            List<ScanResult> singleScanResults = new ArrayList<>();
            List<ScanResult> backgroundScanResults = new ArrayList<>();
            List<ScanResult> hwPnoScanResults = new ArrayList<>();
            for (int i = 0; i < nativeResults.size(); i++) {
                ScanResult result = nativeResults.get(i).getScanResult();
                long timestamp_ms = result.timestamp / 1000;
                if (timestamp_ms > this.mLastScanSettings.startTime) {
                    if (this.mLastScanSettings.backgroundScanActive) {
                        backgroundScanResults.add(result);
                    }
                    if (this.mLastScanSettings.singleScanActive && this.mLastScanSettings.singleScanFreqs.containsChannel(result.frequency)) {
                        singleScanResults.add(result);
                    }
                    if (this.mLastScanSettings.hwPnoScanActive) {
                        hwPnoScanResults.add(result);
                    }
                }
            }
            if (this.mLastScanSettings.backgroundScanActive) {
                if (this.mBackgroundScanEventHandler != null && (this.mLastScanSettings.reportEvents & 2) != 0) {
                    for (ScanResult scanResult : backgroundScanResults) {
                        this.mBackgroundScanEventHandler.onFullScanResult(scanResult, 0);
                    }
                }
                Collections.sort(backgroundScanResults, SCAN_RESULT_SORT_COMPARATOR);
                ScanResult[] scanResultsArray = new ScanResult[Math.min(this.mLastScanSettings.maxAps, backgroundScanResults.size())];
                for (int i2 = 0; i2 < scanResultsArray.length; i2++) {
                    scanResultsArray[i2] = backgroundScanResults.get(i2);
                }
                if ((this.mLastScanSettings.reportEvents & 4) == 0) {
                    this.mBackgroundScanBuffer.add(new WifiScanner.ScanData(this.mLastScanSettings.scanId, 0, scanResultsArray));
                }
                if (this.mBackgroundScanEventHandler != null && ((this.mLastScanSettings.reportEvents & 2) != 0 || (this.mLastScanSettings.reportEvents & 1) != 0 || (this.mLastScanSettings.reportEvents == 0 && (this.mBackgroundScanBuffer.size() >= (this.mBackgroundScanBuffer.capacity() * this.mLastScanSettings.reportPercentThreshold) / 100 || this.mBackgroundScanBuffer.size() >= this.mLastScanSettings.reportNumScansThreshold)))) {
                    this.mBackgroundScanEventHandler.onScanStatus(0);
                }
                if (this.mHotlistHandler != null) {
                    int event = this.mHotlistChangeBuffer.processScan(backgroundScanResults);
                    if ((ChangeBuffer.EVENT_FOUND & event) != 0) {
                        this.mHotlistHandler.onHotlistApFound(this.mHotlistChangeBuffer.getLastResults(ChangeBuffer.EVENT_FOUND));
                    }
                    if ((ChangeBuffer.EVENT_LOST & event) != 0) {
                        this.mHotlistHandler.onHotlistApLost(this.mHotlistChangeBuffer.getLastResults(ChangeBuffer.EVENT_LOST));
                    }
                }
            }
            if (this.mLastScanSettings.singleScanActive && this.mLastScanSettings.singleScanEventHandler != null) {
                if (this.mLastScanSettings.reportSingleScanFullResults) {
                    for (ScanResult scanResult2 : singleScanResults) {
                        this.mLastScanSettings.singleScanEventHandler.onFullScanResult(scanResult2, 0);
                    }
                }
                Collections.sort(singleScanResults, SCAN_RESULT_SORT_COMPARATOR);
                this.mLatestSingleScanResult = new WifiScanner.ScanData(this.mLastScanSettings.scanId, 0, (ScanResult[]) singleScanResults.toArray(new ScanResult[singleScanResults.size()]));
                this.mLastScanSettings.singleScanEventHandler.onScanStatus(0);
            }
            if (this.mLastScanSettings.hwPnoScanActive && this.mLastScanSettings.pnoScanEventHandler != null) {
                ScanResult[] pnoScanResultsArray = new ScanResult[hwPnoScanResults.size()];
                for (int i3 = 0; i3 < pnoScanResultsArray.length; i3++) {
                    pnoScanResultsArray[i3] = hwPnoScanResults.get(i3);
                }
                this.mLastScanSettings.pnoScanEventHandler.onPnoNetworkFound(pnoScanResultsArray);
            }
            this.mLastScanSettings = null;
        }
    }

    @Override
    public WifiScanner.ScanData[] getLatestBatchedScanResults(boolean flush) {
        WifiScanner.ScanData[] results;
        synchronized (this.mSettingsLock) {
            results = this.mBackgroundScanBuffer.get();
            if (flush) {
                this.mBackgroundScanBuffer.clear();
            }
        }
        return results;
    }

    private boolean setNetworkPriorities(WifiNative.PnoNetwork[] networkList) {
        if (networkList != null) {
            Log.i(TAG, "Enable network and Set priorities for PNO.");
            for (WifiNative.PnoNetwork network : networkList) {
                if (!this.mWifiNative.setNetworkVariable(network.networkId, "priority", Integer.toString(network.priority))) {
                    Log.e(TAG, "Set priority failed for: " + network.networkId);
                    return false;
                }
                if (!this.mWifiNative.enableNetworkWithoutConnect(network.networkId)) {
                    Log.e(TAG, "Enable network failed for: " + network.networkId);
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean startHwPnoScan() {
        return this.mHwPnoDebouncer.startPnoScan(this.mHwPnoDebouncerListener);
    }

    private void stopHwPnoScan() {
        this.mHwPnoDebouncer.stopPnoScan();
    }

    private void pauseHwPnoScan() {
        this.mHwPnoDebouncer.forceStopPnoScan();
    }

    private boolean isHwPnoScanRequired(boolean isConnectedPno) {
        return (!isConnectedPno) & this.mHwPnoScanSupported;
    }

    private boolean isHwPnoScanRequired() {
        if (this.mPnoSettings == null) {
            return false;
        }
        return isHwPnoScanRequired(this.mPnoSettings.isConnected);
    }

    @Override
    public boolean setHwPnoList(WifiNative.PnoSettings settings, WifiNative.PnoEventHandler eventHandler) {
        synchronized (this.mSettingsLock) {
            if (this.mPnoSettings != null) {
                Log.w(TAG, "Already running a PNO scan");
                return false;
            }
            this.mPnoEventHandler = eventHandler;
            this.mPnoSettings = settings;
            if (!setNetworkPriorities(settings.networkList)) {
                return false;
            }
            processPendingScans();
            return true;
        }
    }

    @Override
    public boolean resetHwPnoList() {
        synchronized (this.mSettingsLock) {
            if (this.mPnoSettings == null) {
                Log.w(TAG, "No PNO scan running");
                return false;
            }
            this.mPnoEventHandler = null;
            this.mPnoSettings = null;
            stopHwPnoScan();
            return true;
        }
    }

    @Override
    public boolean isHwPnoSupported(boolean isConnectedPno) {
        return isHwPnoScanRequired(isConnectedPno);
    }

    @Override
    public boolean shouldScheduleBackgroundScanForHwPno() {
        return false;
    }

    @Override
    public boolean setHotlist(WifiScanner.HotlistSettings settings, WifiNative.HotlistEventHandler eventHandler) {
        if (settings == null || eventHandler == null) {
            return false;
        }
        synchronized (this.mSettingsLock) {
            this.mHotlistHandler = eventHandler;
            this.mHotlistChangeBuffer.setSettings(settings.bssidInfos, settings.apLostThreshold, 1);
        }
        return true;
    }

    @Override
    public void resetHotlist() {
        synchronized (this.mSettingsLock) {
            this.mHotlistChangeBuffer.clearSettings();
            this.mHotlistHandler = null;
        }
    }

    @Override
    public boolean trackSignificantWifiChange(WifiScanner.WifiChangeSettings settings, WifiNative.SignificantWifiChangeEventHandler handler) {
        return false;
    }

    @Override
    public void untrackSignificantWifiChange() {
    }

    private static class LastScanSettings {
        public int maxAps;
        public WifiNative.PnoEventHandler pnoScanEventHandler;
        public int reportEvents;
        public int reportNumScansThreshold;
        public int reportPercentThreshold;
        public boolean reportSingleScanFullResults;
        public int scanId;
        public WifiNative.ScanEventHandler singleScanEventHandler;
        public ChannelHelper.ChannelCollection singleScanFreqs;
        public long startTime;
        public boolean backgroundScanActive = false;
        public boolean singleScanActive = false;
        public boolean hwPnoScanActive = false;

        public LastScanSettings(long startTime) {
            this.startTime = startTime;
        }

        public void setBackgroundScan(int scanId, int maxAps, int reportEvents, int reportNumScansThreshold, int reportPercentThreshold) {
            this.backgroundScanActive = true;
            this.scanId = scanId;
            this.maxAps = maxAps;
            this.reportEvents = reportEvents;
            this.reportNumScansThreshold = reportNumScansThreshold;
            this.reportPercentThreshold = reportPercentThreshold;
        }

        public void setSingleScan(boolean reportSingleScanFullResults, ChannelHelper.ChannelCollection singleScanFreqs, WifiNative.ScanEventHandler singleScanEventHandler) {
            this.singleScanActive = true;
            this.reportSingleScanFullResults = reportSingleScanFullResults;
            this.singleScanFreqs = singleScanFreqs;
            this.singleScanEventHandler = singleScanEventHandler;
        }

        public void setHwPnoScan(WifiNative.PnoEventHandler pnoScanEventHandler) {
            this.hwPnoScanActive = true;
            this.pnoScanEventHandler = pnoScanEventHandler;
        }
    }

    private static class ScanBuffer {
        private final ArrayDeque<WifiScanner.ScanData> mBuffer;
        private int mCapacity;

        public ScanBuffer(int capacity) {
            this.mCapacity = capacity;
            this.mBuffer = new ArrayDeque<>(this.mCapacity);
        }

        public int size() {
            return this.mBuffer.size();
        }

        public int capacity() {
            return this.mCapacity;
        }

        public boolean isFull() {
            return size() == this.mCapacity;
        }

        public void add(WifiScanner.ScanData scanData) {
            if (isFull()) {
                this.mBuffer.pollFirst();
            }
            this.mBuffer.offerLast(scanData);
        }

        public void clear() {
            this.mBuffer.clear();
        }

        public WifiScanner.ScanData[] get() {
            return (WifiScanner.ScanData[]) this.mBuffer.toArray(new WifiScanner.ScanData[this.mBuffer.size()]);
        }
    }

    private static class ChangeBuffer {
        private int mApLostThreshold;
        private WifiScanner.BssidInfo[] mBssidInfos;
        private boolean mFiredEvents;
        private int[] mLostCount;
        private int mMinEvents;
        private ScanResult[] mMostRecentResult;
        private int[] mPendingEvent;
        public static int EVENT_NONE = 0;
        public static int EVENT_LOST = 1;
        public static int EVENT_FOUND = 2;
        public static int STATE_FOUND = 0;

        ChangeBuffer(ChangeBuffer changeBuffer) {
            this();
        }

        private ChangeBuffer() {
            this.mBssidInfos = null;
            this.mLostCount = null;
            this.mMostRecentResult = null;
            this.mPendingEvent = null;
            this.mFiredEvents = false;
        }

        private static ScanResult findResult(List<ScanResult> results, String bssid) {
            for (int i = 0; i < results.size(); i++) {
                if (bssid.equalsIgnoreCase(results.get(i).BSSID)) {
                    return results.get(i);
                }
            }
            return null;
        }

        public void setSettings(WifiScanner.BssidInfo[] bssidInfos, int apLostThreshold, int minEvents) {
            this.mBssidInfos = bssidInfos;
            if (apLostThreshold <= 0) {
                this.mApLostThreshold = 1;
            } else {
                this.mApLostThreshold = apLostThreshold;
            }
            this.mMinEvents = minEvents;
            if (bssidInfos != null) {
                this.mLostCount = new int[bssidInfos.length];
                Arrays.fill(this.mLostCount, this.mApLostThreshold);
                this.mMostRecentResult = new ScanResult[bssidInfos.length];
                this.mPendingEvent = new int[bssidInfos.length];
                this.mFiredEvents = false;
                return;
            }
            this.mLostCount = null;
            this.mMostRecentResult = null;
            this.mPendingEvent = null;
        }

        public void clearSettings() {
            setSettings(null, 0, 0);
        }

        public ScanResult[] getLastResults(int event) {
            ArrayList<ScanResult> results = new ArrayList<>();
            for (int i = 0; i < this.mLostCount.length; i++) {
                if (this.mPendingEvent[i] == event) {
                    results.add(this.mMostRecentResult[i]);
                }
            }
            return (ScanResult[]) results.toArray(new ScanResult[results.size()]);
        }

        public int processScan(List<ScanResult> scanResults) {
            if (this.mBssidInfos == null) {
                return EVENT_NONE;
            }
            if (this.mFiredEvents) {
                this.mFiredEvents = false;
                for (int i = 0; i < this.mLostCount.length; i++) {
                    this.mPendingEvent[i] = EVENT_NONE;
                }
            }
            int eventCount = 0;
            int eventType = EVENT_NONE;
            for (int i2 = 0; i2 < this.mLostCount.length; i2++) {
                ScanResult result = findResult(scanResults, this.mBssidInfos[i2].bssid);
                int rssi = Integer.MIN_VALUE;
                if (result != null) {
                    this.mMostRecentResult[i2] = result;
                    rssi = result.level;
                }
                if (rssi >= this.mBssidInfos[i2].low) {
                    if (this.mLostCount[i2] >= this.mApLostThreshold) {
                        if (this.mPendingEvent[i2] == EVENT_LOST) {
                            this.mPendingEvent[i2] = EVENT_NONE;
                        } else {
                            this.mPendingEvent[i2] = EVENT_FOUND;
                        }
                    }
                    this.mLostCount[i2] = STATE_FOUND;
                } else if (this.mLostCount[i2] < this.mApLostThreshold) {
                    int[] iArr = this.mLostCount;
                    iArr[i2] = iArr[i2] + 1;
                    if (this.mLostCount[i2] >= this.mApLostThreshold) {
                        if (this.mPendingEvent[i2] == EVENT_FOUND) {
                            this.mPendingEvent[i2] = EVENT_NONE;
                        } else {
                            this.mPendingEvent[i2] = EVENT_LOST;
                        }
                    }
                }
                Log.d(SupplicantWifiScannerImpl.TAG, "ChangeBuffer BSSID: " + this.mBssidInfos[i2].bssid + "=" + this.mLostCount[i2] + ", " + this.mPendingEvent[i2] + ", rssi=" + rssi);
                if (this.mPendingEvent[i2] != EVENT_NONE) {
                    eventCount++;
                    eventType |= this.mPendingEvent[i2];
                }
            }
            Log.d(SupplicantWifiScannerImpl.TAG, "ChangeBuffer events count=" + eventCount + ": " + eventType);
            if (eventCount < this.mMinEvents) {
                return EVENT_NONE;
            }
            this.mFiredEvents = true;
            return eventType;
        }
    }

    public static class HwPnoDebouncer {
        private static final int MINIMUM_PNO_GAP_MS = 5000;
        public static final String PNO_DEBOUNCER_ALARM_TAG = "SupplicantWifiScannerImplPno Monitor";
        private final AlarmManager mAlarmManager;
        private final Clock mClock;
        private final Handler mEventHandler;
        private Listener mListener;
        private final WifiNative mWifiNative;
        private long mLastPnoChangeTimeStamp = -1;
        private boolean mExpectedPnoState = false;
        private boolean mCurrentPnoState = false;
        private boolean mWaitForTimer = false;
        private final AlarmManager.OnAlarmListener mAlarmListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                Log.d(SupplicantWifiScannerImpl.TAG, "PNO timer expired, expected state " + HwPnoDebouncer.this.mExpectedPnoState);
                if (!HwPnoDebouncer.this.updatePnoState(HwPnoDebouncer.this.mExpectedPnoState) && HwPnoDebouncer.this.mListener != null) {
                    HwPnoDebouncer.this.mListener.onPnoScanFailed();
                }
                HwPnoDebouncer.this.mWaitForTimer = false;
            }
        };

        public interface Listener {
            void onPnoScanFailed();
        }

        public HwPnoDebouncer(WifiNative wifiNative, AlarmManager alarmManager, Handler eventHandler, Clock clock) {
            this.mWifiNative = wifiNative;
            this.mAlarmManager = alarmManager;
            this.mEventHandler = eventHandler;
            this.mClock = clock;
        }

        private boolean updatePnoState(boolean enable) {
            if (this.mCurrentPnoState == enable) {
                Log.d(SupplicantWifiScannerImpl.TAG, "PNO state is already " + enable);
                return true;
            }
            this.mLastPnoChangeTimeStamp = this.mClock.elapsedRealtime();
            if (this.mWifiNative.setPnoScan(enable)) {
                Log.d(SupplicantWifiScannerImpl.TAG, "Changed PNO state from " + this.mCurrentPnoState + " to " + enable);
                this.mCurrentPnoState = enable;
                return true;
            }
            Log.e(SupplicantWifiScannerImpl.TAG, "PNO state change to " + enable + " failed");
            return false;
        }

        private boolean setPnoState(boolean enable) {
            this.mExpectedPnoState = enable;
            if (this.mWaitForTimer) {
                return true;
            }
            long timeDifference = this.mClock.elapsedRealtime() - this.mLastPnoChangeTimeStamp;
            if (timeDifference >= 5000) {
                boolean isSuccess = updatePnoState(enable);
                return isSuccess;
            }
            long alarmTimeout = 5000 - timeDifference;
            Log.d(SupplicantWifiScannerImpl.TAG, "Start PNO timer with delay " + alarmTimeout);
            this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + alarmTimeout, PNO_DEBOUNCER_ALARM_TAG, this.mAlarmListener, this.mEventHandler);
            this.mWaitForTimer = true;
            return true;
        }

        public boolean startPnoScan(Listener listener) {
            Log.d(SupplicantWifiScannerImpl.TAG, "Starting PNO scan");
            this.mListener = listener;
            if (setPnoState(true)) {
                return true;
            }
            this.mListener = null;
            return false;
        }

        public void stopPnoScan() {
            Log.d(SupplicantWifiScannerImpl.TAG, "Stopping PNO scan");
            setPnoState(false);
            this.mListener = null;
        }

        public void forceStopPnoScan() {
            if (!this.mCurrentPnoState) {
                return;
            }
            Log.d(SupplicantWifiScannerImpl.TAG, "Force stopping Pno scan");
            if (this.mWaitForTimer) {
                this.mAlarmManager.cancel(this.mAlarmListener);
                this.mWaitForTimer = false;
            }
            updatePnoState(false);
        }
    }
}
