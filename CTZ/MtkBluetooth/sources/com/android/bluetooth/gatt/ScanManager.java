package com.android.bluetooth.gatt;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.gatt.ScanFilterQueue;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScanManager {
    private static final String ACTION_REFRESH_BATCHED_SCAN = "com.android.bluetooth.gatt.REFRESH_BATCHED_SCAN";
    private static final boolean DBG = GattServiceConfig.DBG;
    private static final int FOREGROUND_IMPORTANCE_CUTOFF = 125;
    private static final int MSG_FLUSH_BATCH_RESULTS = 2;
    private static final int MSG_IMPORTANCE_CHANGE = 6;
    private static final int MSG_RESUME_SCANS = 5;
    private static final int MSG_SCAN_TIMEOUT = 3;
    private static final int MSG_START_BLE_SCAN = 0;
    private static final int MSG_STOP_BLE_SCAN = 1;
    private static final int MSG_SUSPEND_SCANS = 4;
    private static final int OPERATION_TIME_OUT_MILLIS = 500;
    static final int SCAN_RESULT_TYPE_BOTH = 3;
    static final int SCAN_RESULT_TYPE_FULL = 2;
    static final int SCAN_RESULT_TYPE_TRUNCATED = 1;
    private static final String TAG = "BtGatt.ScanManager";
    private ActivityManager mActivityManager;
    private BroadcastReceiver mBatchAlarmReceiver;
    private boolean mBatchAlarmReceiverRegistered;
    private BatchScanParams mBatchScanParms;
    private DisplayManager mDm;
    private volatile ClientHandler mHandler;
    private CountDownLatch mLatch;
    private LocationManager mLocationManager;
    private GattService mService;
    private int mLastConfiguredScanSetting = Integer.MIN_VALUE;
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int i) {
        }

        @Override
        public void onDisplayRemoved(int i) {
        }

        @Override
        public void onDisplayChanged(int i) {
            if (!ScanManager.this.isScreenOn() || !ScanManager.this.mLocationManager.isLocationEnabled()) {
                ScanManager.this.sendMessage(4, null);
            } else {
                ScanManager.this.sendMessage(5, null);
            }
        }
    };
    private ActivityManager.OnUidImportanceListener mUidImportanceListener = new ActivityManager.OnUidImportanceListener() {
        public void onUidImportance(int i, int i2) {
            if (ScanManager.this.mService.mScannerMap.getAppScanStatsByUid(i) != null) {
                Message message = new Message();
                message.what = 6;
                message.obj = ScanManager.this.new UidImportance(i, i2);
                ScanManager.this.mHandler.sendMessage(message);
            }
        }
    };
    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.location.MODE_CHANGED".equals(intent.getAction())) {
                if (!ScanManager.this.mLocationManager.isLocationEnabled() || !ScanManager.this.isScreenOn()) {
                    ScanManager.this.sendMessage(4, null);
                } else {
                    ScanManager.this.sendMessage(5, null);
                }
            }
        }
    };
    private Set<ScanClient> mRegularScanClients = Collections.newSetFromMap(new ConcurrentHashMap());
    private Set<ScanClient> mBatchClients = Collections.newSetFromMap(new ConcurrentHashMap());
    private Set<ScanClient> mSuspendedScanClients = Collections.newSetFromMap(new ConcurrentHashMap());
    private ScanNative mScanNative = new ScanNative();
    private Integer mCurUsedTrackableAdvertisements = 0;

    private class UidImportance {
        public int importance;
        public int uid;

        UidImportance(int i, int i2) {
            this.uid = i;
            this.importance = i2;
        }
    }

    ScanManager(GattService gattService) {
        this.mService = gattService;
        this.mDm = (DisplayManager) this.mService.getSystemService("display");
        this.mActivityManager = (ActivityManager) this.mService.getSystemService("activity");
        this.mLocationManager = (LocationManager) this.mService.getSystemService("location");
    }

    void start() {
        HandlerThread handlerThread = new HandlerThread("BluetoothScanManager");
        handlerThread.start();
        this.mHandler = new ClientHandler(handlerThread.getLooper());
        if (this.mDm != null) {
            this.mDm.registerDisplayListener(this.mDisplayListener, null);
        }
        if (this.mActivityManager != null) {
            this.mActivityManager.addOnUidImportanceListener(this.mUidImportanceListener, FOREGROUND_IMPORTANCE_CUTOFF);
        }
        this.mService.registerReceiver(this.mLocationReceiver, new IntentFilter("android.location.MODE_CHANGED"));
    }

    void cleanup() {
        this.mRegularScanClients.clear();
        this.mBatchClients.clear();
        this.mSuspendedScanClients.clear();
        this.mScanNative.cleanup();
        if (this.mActivityManager != null) {
            try {
                this.mActivityManager.removeOnUidImportanceListener(this.mUidImportanceListener);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "exception when invoking removeOnUidImportanceListener", e);
            }
        }
        if (this.mDm != null) {
            this.mDm.unregisterDisplayListener(this.mDisplayListener);
        }
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
            Looper looper = this.mHandler.getLooper();
            if (looper != null) {
                looper.quitSafely();
            }
            this.mHandler = null;
        }
        try {
            this.mService.unregisterReceiver(this.mLocationReceiver);
        } catch (IllegalArgumentException e2) {
            Log.w(TAG, "exception when invoking unregisterReceiver(mLocationReceiver)", e2);
        }
    }

    void registerScanner(UUID uuid) {
        this.mScanNative.registerScannerNative(uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
    }

    void unregisterScanner(int i) {
        this.mScanNative.unregisterScannerNative(i);
    }

    Set<ScanClient> getRegularScanQueue() {
        return this.mRegularScanClients;
    }

    Set<ScanClient> getBatchScanQueue() {
        return this.mBatchClients;
    }

    Set<ScanClient> getFullBatchScanQueue() {
        HashSet hashSet = new HashSet();
        for (ScanClient scanClient : this.mBatchClients) {
            if (scanClient.settings.getScanResultType() == 0) {
                hashSet.add(scanClient);
            }
        }
        return hashSet;
    }

    void startScan(ScanClient scanClient) {
        sendMessage(0, scanClient);
    }

    void stopScan(ScanClient scanClient) {
        sendMessage(1, scanClient);
    }

    void flushBatchScanResults(ScanClient scanClient) {
        sendMessage(2, scanClient);
    }

    void callbackDone(int i, int i2) {
        if (DBG) {
            Log.d(TAG, "callback done for scannerId - " + i + " status - " + i2);
        }
        if (i2 == 0) {
            this.mLatch.countDown();
        }
    }

    private void sendMessage(int i, ScanClient scanClient) {
        ClientHandler clientHandler = this.mHandler;
        if (clientHandler == null) {
            Log.d(TAG, "sendMessage: mHandler is null.");
            return;
        }
        Message message = new Message();
        message.what = i;
        message.obj = scanClient;
        clientHandler.sendMessage(message);
    }

    private boolean isFilteringSupported() {
        return BluetoothAdapter.getDefaultAdapter().isOffloadedFilteringSupported();
    }

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleStartScan((ScanClient) message.obj);
                    break;
                case 1:
                    handleStopScan((ScanClient) message.obj);
                    break;
                case 2:
                    handleFlushBatchResults((ScanClient) message.obj);
                    break;
                case 3:
                    ScanManager.this.mScanNative.regularScanTimeout((ScanClient) message.obj);
                    break;
                case 4:
                    handleSuspendScans();
                    break;
                case 5:
                    handleResumeScans();
                    break;
                case 6:
                    ScanManager.this.handleImportanceChange((UidImportance) message.obj);
                    break;
                default:
                    Log.e(ScanManager.TAG, "received an unkown message : " + message.what);
                    break;
            }
        }

        void handleStartScan(ScanClient scanClient) {
            Utils.enforceAdminPermission(ScanManager.this.mService);
            boolean z = (scanClient.filters == null || scanClient.filters.isEmpty()) ? false : true;
            if (ScanManager.DBG) {
                Log.d(ScanManager.TAG, "handling starting scan");
            }
            if (isScanSupported(scanClient)) {
                if (ScanManager.this.mRegularScanClients.contains(scanClient) || ScanManager.this.mBatchClients.contains(scanClient)) {
                    Log.e(ScanManager.TAG, "Scan already started");
                    return;
                }
                if (!ScanManager.this.mScanNative.isOpportunisticScanClient(scanClient) && !ScanManager.this.isScreenOn() && !z) {
                    Log.w(ScanManager.TAG, "Cannot start unfiltered scan in screen-off. This scan will be resumed later: " + scanClient.scannerId);
                    ScanManager.this.mSuspendedScanClients.add(scanClient);
                    if (scanClient.stats != null) {
                        scanClient.stats.recordScanSuspend(scanClient.scannerId);
                        return;
                    }
                    return;
                }
                if (ScanManager.this.mLocationManager.isLocationEnabled() || z || scanClient.legacyForegroundApp) {
                    if (ScanManager.this.mService.mScannerMap.getById(scanClient.scannerId) == null) {
                        return;
                    }
                    if (isBatchClient(scanClient)) {
                        ScanManager.this.mBatchClients.add(scanClient);
                        ScanManager.this.mScanNative.startBatchScan(scanClient);
                        return;
                    }
                    ScanManager.this.mRegularScanClients.add(scanClient);
                    ScanManager.this.mScanNative.startRegularScan(scanClient);
                    if (!ScanManager.this.mScanNative.isOpportunisticScanClient(scanClient)) {
                        ScanManager.this.mScanNative.configureRegularScanParams();
                        if (!ScanManager.this.mScanNative.isExemptFromScanDowngrade(scanClient)) {
                            Message messageObtainMessage = obtainMessage(3);
                            messageObtainMessage.obj = scanClient;
                            sendMessageDelayed(messageObtainMessage, 1800000L);
                            return;
                        }
                        return;
                    }
                    return;
                }
                Log.i(ScanManager.TAG, "Cannot start unfiltered scan in location-off. This scan will be resumed when location is on: " + scanClient.scannerId);
                ScanManager.this.mSuspendedScanClients.add(scanClient);
                if (scanClient.stats != null) {
                    scanClient.stats.recordScanSuspend(scanClient.scannerId);
                    return;
                }
                return;
            }
            Log.e(ScanManager.TAG, "Scan settings not supported");
        }

        void handleStopScan(ScanClient scanClient) {
            Utils.enforceAdminPermission(ScanManager.this.mService);
            if (scanClient != null) {
                if (ScanManager.this.mSuspendedScanClients.contains(scanClient)) {
                    ScanManager.this.mSuspendedScanClients.remove(scanClient);
                }
                if (ScanManager.this.mRegularScanClients.contains(scanClient)) {
                    ScanClient regularScanClient = ScanManager.this.mScanNative.getRegularScanClient(scanClient.scannerId);
                    ScanManager.this.mScanNative.stopRegularScan(regularScanClient);
                    if (ScanManager.this.mScanNative.numRegularScanClients() == 0) {
                        removeMessages(3);
                    }
                    if (!ScanManager.this.mScanNative.isOpportunisticScanClient(regularScanClient)) {
                        ScanManager.this.mScanNative.configureRegularScanParams();
                    }
                } else if (ScanManager.this.mBatchClients.contains(scanClient)) {
                    ScanManager.this.mScanNative.stopBatchScan(ScanManager.this.mScanNative.getBatchScanClient(scanClient.scannerId));
                }
                if (scanClient.appDied) {
                    if (ScanManager.DBG) {
                        Log.d(ScanManager.TAG, "app died, unregister scanner - " + scanClient.scannerId);
                    }
                    ScanManager.this.mService.unregisterScanner(scanClient.scannerId);
                }
            }
        }

        void handleFlushBatchResults(ScanClient scanClient) {
            Utils.enforceAdminPermission(ScanManager.this.mService);
            if (ScanManager.this.mBatchClients.contains(scanClient)) {
                ScanManager.this.mScanNative.flushBatchResults(scanClient.scannerId);
            }
        }

        private boolean isBatchClient(ScanClient scanClient) {
            if (scanClient == null || scanClient.settings == null) {
                return false;
            }
            ScanSettings scanSettings = scanClient.settings;
            return scanSettings.getCallbackType() == 1 && scanSettings.getReportDelayMillis() != 0;
        }

        private boolean isScanSupported(ScanClient scanClient) {
            if (scanClient == null || scanClient.settings == null) {
                return true;
            }
            ScanSettings scanSettings = scanClient.settings;
            if (ScanManager.this.isFilteringSupported()) {
                return true;
            }
            return scanSettings.getCallbackType() == 1 && scanSettings.getReportDelayMillis() == 0;
        }

        void handleSuspendScans() {
            for (ScanClient scanClient : ScanManager.this.mRegularScanClients) {
                if (!ScanManager.this.mScanNative.isOpportunisticScanClient(scanClient) && (scanClient.filters == null || scanClient.filters.isEmpty())) {
                    if (!scanClient.legacyForegroundApp) {
                        if (scanClient.stats != null) {
                            scanClient.stats.recordScanSuspend(scanClient.scannerId);
                        }
                        handleStopScan(scanClient);
                        ScanManager.this.mSuspendedScanClients.add(scanClient);
                    }
                }
            }
        }

        void handleResumeScans() {
            for (ScanClient scanClient : ScanManager.this.mSuspendedScanClients) {
                if (scanClient.stats != null) {
                    scanClient.stats.recordScanResume(scanClient.scannerId);
                }
                handleStartScan(scanClient);
            }
            ScanManager.this.mSuspendedScanClients.clear();
        }
    }

    class BatchScanParams {
        public int scanMode = -1;
        public int fullScanscannerId = -1;
        public int truncatedScanscannerId = -1;

        BatchScanParams() {
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            BatchScanParams batchScanParams = (BatchScanParams) obj;
            if (this.scanMode == batchScanParams.scanMode && this.fullScanscannerId == batchScanParams.fullScanscannerId && this.truncatedScanscannerId == batchScanParams.truncatedScanscannerId) {
                return true;
            }
            return false;
        }
    }

    public int getCurrentUsedTrackingAdvertisement() {
        return this.mCurUsedTrackableAdvertisements.intValue();
    }

    private class ScanNative {
        private static final int ALL_PASS_FILTER_INDEX_BATCH_SCAN = 2;
        private static final int ALL_PASS_FILTER_INDEX_REGULAR_SCAN = 1;
        private static final int ALL_PASS_FILTER_SELECTION = 0;
        private static final int DELIVERY_MODE_BATCH = 2;
        private static final int DELIVERY_MODE_IMMEDIATE = 0;
        private static final int DELIVERY_MODE_ON_FOUND_LOST = 1;
        private static final int DISCARD_OLDEST_WHEN_BUFFER_FULL = 0;
        private static final int FILTER_LOGIC_TYPE = 1;
        private static final int LIST_LOGIC_TYPE = 17895697;
        private static final int MATCH_MODE_AGGRESSIVE_TIMEOUT_FACTOR = 1;
        private static final int MATCH_MODE_STICKY_TIMEOUT_FACTOR = 3;
        private static final int ONFOUND_SIGHTINGS_AGGRESSIVE = 1;
        private static final int ONFOUND_SIGHTINGS_STICKY = 4;
        private static final int ONLOST_FACTOR = 2;
        private static final int ONLOST_ONFOUND_BASE_TIMEOUT_MS = 500;
        private static final int SCAN_MODE_BALANCED_INTERVAL_MS = 4096;
        private static final int SCAN_MODE_BALANCED_WINDOW_MS = 1024;
        private static final int SCAN_MODE_BATCH_BALANCED_INTERVAL_MS = 15000;
        private static final int SCAN_MODE_BATCH_BALANCED_WINDOW_MS = 1500;
        private static final int SCAN_MODE_BATCH_LOW_LATENCY_INTERVAL_MS = 5000;
        private static final int SCAN_MODE_BATCH_LOW_LATENCY_WINDOW_MS = 1500;
        private static final int SCAN_MODE_BATCH_LOW_POWER_INTERVAL_MS = 150000;
        private static final int SCAN_MODE_BATCH_LOW_POWER_WINDOW_MS = 1500;
        private static final int SCAN_MODE_LOW_LATENCY_INTERVAL_MS = 4096;
        private static final int SCAN_MODE_LOW_LATENCY_WINDOW_MS = 4096;
        private static final int SCAN_MODE_LOW_POWER_INTERVAL_MS = 5120;
        private static final int SCAN_MODE_LOW_POWER_WINDOW_MS = 512;
        private AlarmManager mAlarmManager;
        private PendingIntent mBatchScanIntervalIntent;
        private final Set<Integer> mAllPassRegularClients = new HashSet();
        private final Set<Integer> mAllPassBatchClients = new HashSet();
        private final Deque<Integer> mFilterIndexStack = new ArrayDeque();
        private final Map<Integer, Deque<Integer>> mClientFilterIndexMap = new HashMap();

        private native void gattClientConfigBatchScanStorageNative(int i, int i2, int i3, int i4);

        private native void gattClientReadScanReportsNative(int i, int i2);

        private native void gattClientScanFilterAddNative(int i, ScanFilterQueue.Entry[] entryArr, int i2);

        private native void gattClientScanFilterClearNative(int i, int i2);

        private native void gattClientScanFilterEnableNative(int i, boolean z);

        private native void gattClientScanFilterParamAddNative(FilterParams filterParams);

        private native void gattClientScanFilterParamClearAllNative(int i);

        private native void gattClientScanFilterParamDeleteNative(int i, int i2);

        private native void gattClientScanNative(boolean z);

        private native void gattClientStartBatchScanNative(int i, int i2, int i3, int i4, int i5, int i6);

        private native void gattClientStopBatchScanNative(int i);

        private native void gattSetScanParametersNative(int i, int i2, int i3);

        private native void registerScannerNative(long j, long j2);

        private native void unregisterScannerNative(int i);

        ScanNative() {
            this.mAlarmManager = (AlarmManager) ScanManager.this.mService.getSystemService(NotificationCompat.CATEGORY_ALARM);
            this.mBatchScanIntervalIntent = PendingIntent.getBroadcast(ScanManager.this.mService, 0, new Intent(ScanManager.ACTION_REFRESH_BATCHED_SCAN, (Uri) null), 0);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ScanManager.ACTION_REFRESH_BATCHED_SCAN);
            ScanManager.this.mBatchAlarmReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(ScanManager.TAG, "awakened up at time " + SystemClock.elapsedRealtime());
                    if (intent.getAction().equals(ScanManager.ACTION_REFRESH_BATCHED_SCAN) && !ScanManager.this.mBatchClients.isEmpty() && ScanManager.this.mBatchClients.iterator().hasNext()) {
                        ScanManager.this.flushBatchScanResults((ScanClient) ScanManager.this.mBatchClients.iterator().next());
                    }
                }
            };
            ScanManager.this.mService.registerReceiver(ScanManager.this.mBatchAlarmReceiver, intentFilter);
            ScanManager.this.mBatchAlarmReceiverRegistered = true;
        }

        private void resetCountDownLatch() {
            ScanManager.this.mLatch = new CountDownLatch(1);
        }

        private boolean waitForCallback() {
            try {
                return ScanManager.this.mLatch.await(500L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }

        void configureRegularScanParams() {
            int scanMode;
            if (ScanManager.DBG) {
                Log.d(ScanManager.TAG, "configureRegularScanParams() - queue=" + ScanManager.this.mRegularScanClients.size());
            }
            ScanClient aggressiveClient = getAggressiveClient(ScanManager.this.mRegularScanClients);
            if (aggressiveClient != null) {
                scanMode = aggressiveClient.settings.getScanMode();
            } else {
                scanMode = Integer.MIN_VALUE;
            }
            if (ScanManager.DBG) {
                Log.d(ScanManager.TAG, "configureRegularScanParams() - ScanSetting Scan mode=" + scanMode + " mLastConfiguredScanSetting=" + ScanManager.this.mLastConfiguredScanSetting);
            }
            if (scanMode == Integer.MIN_VALUE || scanMode == -1) {
                ScanManager.this.mLastConfiguredScanSetting = scanMode;
                if (ScanManager.DBG) {
                    Log.d(ScanManager.TAG, "configureRegularScanParams() - queue emtpy, scan stopped");
                    return;
                }
                return;
            }
            if (scanMode != ScanManager.this.mLastConfiguredScanSetting) {
                int scanWindowMillis = getScanWindowMillis(aggressiveClient.settings);
                int scanIntervalMillis = getScanIntervalMillis(aggressiveClient.settings);
                int iMillsToUnit = Utils.millsToUnit(scanWindowMillis);
                int iMillsToUnit2 = Utils.millsToUnit(scanIntervalMillis);
                gattClientScanNative(false);
                if (ScanManager.DBG) {
                    Log.d(ScanManager.TAG, "configureRegularScanParams - scanInterval = " + iMillsToUnit2 + "configureRegularScanParams - scanWindow = " + iMillsToUnit);
                }
                gattSetScanParametersNative(aggressiveClient.scannerId, iMillsToUnit2, iMillsToUnit);
                gattClientScanNative(true);
                ScanManager.this.mLastConfiguredScanSetting = scanMode;
            }
        }

        ScanClient getAggressiveClient(Set<ScanClient> set) {
            ScanClient scanClient = null;
            int scanMode = Integer.MIN_VALUE;
            for (ScanClient scanClient2 : set) {
                if (scanClient2.settings.getScanMode() > scanMode) {
                    scanMode = scanClient2.settings.getScanMode();
                    scanClient = scanClient2;
                }
            }
            return scanClient;
        }

        void startRegularScan(ScanClient scanClient) {
            if (ScanManager.this.isFilteringSupported() && this.mFilterIndexStack.isEmpty() && this.mClientFilterIndexMap.isEmpty()) {
                initFilterIndexStack();
            }
            if (ScanManager.this.isFilteringSupported()) {
                configureScanFilters(scanClient);
            }
            if (numRegularScanClients() == 1) {
                gattClientScanNative(true);
            }
        }

        private int numRegularScanClients() {
            Iterator it = ScanManager.this.mRegularScanClients.iterator();
            int i = 0;
            while (it.hasNext()) {
                if (((ScanClient) it.next()).settings.getScanMode() != -1) {
                    i++;
                }
            }
            return i;
        }

        void startBatchScan(ScanClient scanClient) {
            if (this.mFilterIndexStack.isEmpty() && ScanManager.this.isFilteringSupported()) {
                initFilterIndexStack();
            }
            configureScanFilters(scanClient);
            if (!isOpportunisticScanClient(scanClient)) {
                resetBatchScan(scanClient);
            }
        }

        private boolean isExemptFromScanDowngrade(ScanClient scanClient) {
            return isOpportunisticScanClient(scanClient) || isFirstMatchScanClient(scanClient) || !shouldUseAllPassFilter(scanClient);
        }

        private boolean isOpportunisticScanClient(ScanClient scanClient) {
            return scanClient.settings.getScanMode() == -1;
        }

        private boolean isFirstMatchScanClient(ScanClient scanClient) {
            return (scanClient.settings.getCallbackType() & 2) != 0;
        }

        private void resetBatchScan(ScanClient scanClient) {
            int i = scanClient.scannerId;
            BatchScanParams batchScanParams = getBatchScanParams();
            if (ScanManager.this.mBatchScanParms != null && !ScanManager.this.mBatchScanParms.equals(batchScanParams)) {
                if (ScanManager.DBG) {
                    Log.d(ScanManager.TAG, "stopping BLe Batch");
                }
                resetCountDownLatch();
                gattClientStopBatchScanNative(i);
                waitForCallback();
                flushBatchResults(i);
            }
            if (batchScanParams != null && !batchScanParams.equals(ScanManager.this.mBatchScanParms)) {
                if (ScanManager.DBG) {
                    Log.d(ScanManager.TAG, "Starting BLE batch scan");
                }
                int resultType = getResultType(batchScanParams);
                int fullScanStoragePercent = getFullScanStoragePercent(resultType);
                resetCountDownLatch();
                if (ScanManager.DBG) {
                    Log.d(ScanManager.TAG, "configuring batch scan storage, appIf " + scanClient.scannerId);
                }
                gattClientConfigBatchScanStorageNative(scanClient.scannerId, fullScanStoragePercent, 100 - fullScanStoragePercent, 95);
                waitForCallback();
                resetCountDownLatch();
                gattClientStartBatchScanNative(i, resultType, Utils.millsToUnit(getBatchScanIntervalMillis(batchScanParams.scanMode)), Utils.millsToUnit(getBatchScanWindowMillis(batchScanParams.scanMode)), 0, 0);
                waitForCallback();
            }
            ScanManager.this.mBatchScanParms = batchScanParams;
            setBatchAlarm();
        }

        private int getFullScanStoragePercent(int i) {
            switch (i) {
            }
            return 50;
        }

        private BatchScanParams getBatchScanParams() {
            if (ScanManager.this.mBatchClients.isEmpty()) {
                return null;
            }
            BatchScanParams batchScanParams = ScanManager.this.new BatchScanParams();
            for (ScanClient scanClient : ScanManager.this.mBatchClients) {
                batchScanParams.scanMode = Math.max(batchScanParams.scanMode, scanClient.settings.getScanMode());
                if (scanClient.settings.getScanResultType() == 0) {
                    batchScanParams.fullScanscannerId = scanClient.scannerId;
                } else {
                    batchScanParams.truncatedScanscannerId = scanClient.scannerId;
                }
            }
            return batchScanParams;
        }

        private int getBatchScanWindowMillis(int i) {
            switch (i) {
            }
            return NetworkConstants.ETHER_MTU;
        }

        private int getBatchScanIntervalMillis(int i) {
            switch (i) {
            }
            return SCAN_MODE_BATCH_LOW_POWER_INTERVAL_MS;
        }

        private void setBatchAlarm() {
            this.mAlarmManager.cancel(this.mBatchScanIntervalIntent);
            if (ScanManager.this.mBatchClients.isEmpty()) {
                return;
            }
            long batchTriggerIntervalMillis = getBatchTriggerIntervalMillis();
            long jElapsedRealtime = SystemClock.elapsedRealtime() + batchTriggerIntervalMillis;
            this.mAlarmManager.setWindow(2, jElapsedRealtime, batchTriggerIntervalMillis / 10, this.mBatchScanIntervalIntent);
        }

        void stopRegularScan(ScanClient scanClient) {
            if (scanClient == null) {
                return;
            }
            if (getDeliveryMode(scanClient) == 1) {
                for (ScanFilter scanFilter : scanClient.filters) {
                    int numOfTrackingAdvertisements = getNumOfTrackingAdvertisements(scanClient.settings);
                    if (!manageAllocationOfTrackingAdvertisement(numOfTrackingAdvertisements, false)) {
                        Log.e(ScanManager.TAG, "Error freeing for onfound/onlost filter resources " + numOfTrackingAdvertisements);
                        try {
                            ScanManager.this.mService.onScanManagerErrorCallback(scanClient.scannerId, 3);
                        } catch (RemoteException e) {
                            Log.e(ScanManager.TAG, "failed on onScanManagerCallback at freeing", e);
                        }
                    }
                }
            }
            ScanManager.this.mRegularScanClients.remove(scanClient);
            if (numRegularScanClients() == 0) {
                if (ScanManager.DBG) {
                    Log.d(ScanManager.TAG, "stop scan");
                }
                gattClientScanNative(false);
            }
            removeScanFilters(scanClient.scannerId);
        }

        void regularScanTimeout(ScanClient scanClient) {
            if (!isExemptFromScanDowngrade(scanClient) && scanClient.stats.isScanningTooLong()) {
                Log.w(ScanManager.TAG, "Moving scan client to opportunistic (scannerId " + scanClient.scannerId + ")");
                setOpportunisticScanClient(scanClient);
                removeScanFilters(scanClient.scannerId);
                scanClient.stats.setScanTimeout(scanClient.scannerId);
            }
            configureRegularScanParams();
            if (numRegularScanClients() == 0) {
                if (ScanManager.DBG) {
                    Log.d(ScanManager.TAG, "stop scan");
                }
                gattClientScanNative(false);
            }
        }

        void setOpportunisticScanClient(ScanClient scanClient) {
            ScanSettings.Builder builder = new ScanSettings.Builder();
            ScanSettings scanSettings = scanClient.settings;
            builder.setScanMode(-1);
            builder.setCallbackType(scanSettings.getCallbackType());
            builder.setScanResultType(scanSettings.getScanResultType());
            builder.setReportDelay(scanSettings.getReportDelayMillis());
            builder.setNumOfMatches(scanSettings.getNumOfMatches());
            scanClient.settings = builder.build();
        }

        ScanClient getRegularScanClient(int i) {
            for (ScanClient scanClient : ScanManager.this.mRegularScanClients) {
                if (scanClient.scannerId == i) {
                    return scanClient;
                }
            }
            return null;
        }

        void stopBatchScan(ScanClient scanClient) {
            ScanManager.this.mBatchClients.remove(scanClient);
            removeScanFilters(scanClient.scannerId);
            if (!isOpportunisticScanClient(scanClient)) {
                resetBatchScan(scanClient);
            }
        }

        void flushBatchResults(int i) {
            if (ScanManager.DBG) {
                Log.d(ScanManager.TAG, "flushPendingBatchResults - scannerId = " + i);
            }
            if (ScanManager.this.mBatchScanParms.fullScanscannerId != -1) {
                resetCountDownLatch();
                gattClientReadScanReportsNative(ScanManager.this.mBatchScanParms.fullScanscannerId, 2);
                waitForCallback();
            }
            if (ScanManager.this.mBatchScanParms.truncatedScanscannerId != -1) {
                resetCountDownLatch();
                gattClientReadScanReportsNative(ScanManager.this.mBatchScanParms.truncatedScanscannerId, 1);
                waitForCallback();
            }
            setBatchAlarm();
        }

        void cleanup() {
            this.mAlarmManager.cancel(this.mBatchScanIntervalIntent);
            if (ScanManager.this.mBatchAlarmReceiverRegistered) {
                ScanManager.this.mService.unregisterReceiver(ScanManager.this.mBatchAlarmReceiver);
            }
            ScanManager.this.mBatchAlarmReceiverRegistered = false;
        }

        private long getBatchTriggerIntervalMillis() {
            long jMin = Long.MAX_VALUE;
            for (ScanClient scanClient : ScanManager.this.mBatchClients) {
                if (scanClient.settings != null && scanClient.settings.getReportDelayMillis() > 0) {
                    jMin = Math.min(jMin, scanClient.settings.getReportDelayMillis());
                }
            }
            return jMin;
        }

        private void configureScanFilters(ScanClient scanClient) {
            int i = scanClient.scannerId;
            int deliveryMode = getDeliveryMode(scanClient);
            if (isOpportunisticScanClient(scanClient) || !shouldAddAllPassFilterToController(scanClient, deliveryMode)) {
                return;
            }
            resetCountDownLatch();
            gattClientScanFilterEnableNative(i, true);
            waitForCallback();
            if (shouldUseAllPassFilter(scanClient)) {
                int i2 = deliveryMode == 2 ? 2 : 1;
                resetCountDownLatch();
                configureFilterParamter(i, scanClient, 0, i2, 0);
                waitForCallback();
                return;
            }
            ArrayDeque arrayDeque = new ArrayDeque();
            int numOfTrackingAdvertisements = 0;
            for (ScanFilter scanFilter : scanClient.filters) {
                ScanFilterQueue scanFilterQueue = new ScanFilterQueue();
                scanFilterQueue.addScanFilter(scanFilter);
                int featureSelection = scanFilterQueue.getFeatureSelection();
                int iIntValue = this.mFilterIndexStack.pop().intValue();
                resetCountDownLatch();
                gattClientScanFilterAddNative(i, scanFilterQueue.toArray(), iIntValue);
                waitForCallback();
                resetCountDownLatch();
                if (deliveryMode == 1) {
                    numOfTrackingAdvertisements = getNumOfTrackingAdvertisements(scanClient.settings);
                    if (!manageAllocationOfTrackingAdvertisement(numOfTrackingAdvertisements, true)) {
                        Log.e(ScanManager.TAG, "No hardware resources for onfound/onlost filter " + numOfTrackingAdvertisements);
                        try {
                            ScanManager.this.mService.onScanManagerErrorCallback(i, 3);
                        } catch (RemoteException e) {
                            Log.e(ScanManager.TAG, "failed on onScanManagerCallback", e);
                        }
                    }
                }
                int i3 = numOfTrackingAdvertisements;
                configureFilterParamter(i, scanClient, featureSelection, iIntValue, i3);
                waitForCallback();
                arrayDeque.add(Integer.valueOf(iIntValue));
                numOfTrackingAdvertisements = i3;
            }
            this.mClientFilterIndexMap.put(Integer.valueOf(i), arrayDeque);
        }

        private boolean shouldAddAllPassFilterToController(ScanClient scanClient, int i) {
            if (!shouldUseAllPassFilter(scanClient)) {
                return true;
            }
            if (i == 2) {
                this.mAllPassBatchClients.add(Integer.valueOf(scanClient.scannerId));
                return this.mAllPassBatchClients.size() == 1;
            }
            this.mAllPassRegularClients.add(Integer.valueOf(scanClient.scannerId));
            return this.mAllPassRegularClients.size() == 1;
        }

        private void removeScanFilters(int i) {
            Deque<Integer> dequeRemove = this.mClientFilterIndexMap.remove(Integer.valueOf(i));
            if (dequeRemove != null) {
                this.mFilterIndexStack.addAll(dequeRemove);
                for (Integer num : dequeRemove) {
                    resetCountDownLatch();
                    gattClientScanFilterParamDeleteNative(i, num.intValue());
                    waitForCallback();
                }
            }
            removeFilterIfExisits(this.mAllPassRegularClients, i, 1);
            removeFilterIfExisits(this.mAllPassBatchClients, i, 2);
            Log.d(ScanManager.TAG, "we need also disable APCF!");
            gattClientScanFilterEnableNative(i, false);
        }

        private void removeFilterIfExisits(Set<Integer> set, int i, int i2) {
            if (!set.contains(Integer.valueOf(i))) {
                return;
            }
            set.remove(Integer.valueOf(i));
            if (set.isEmpty()) {
                resetCountDownLatch();
                gattClientScanFilterParamDeleteNative(i, i2);
                waitForCallback();
            }
        }

        private ScanClient getBatchScanClient(int i) {
            for (ScanClient scanClient : ScanManager.this.mBatchClients) {
                if (scanClient.scannerId == i) {
                    return scanClient;
                }
            }
            return null;
        }

        private int getResultType(BatchScanParams batchScanParams) {
            if (batchScanParams.fullScanscannerId != -1 && batchScanParams.truncatedScanscannerId != -1) {
                return 3;
            }
            if (batchScanParams.truncatedScanscannerId != -1) {
                return 1;
            }
            return batchScanParams.fullScanscannerId != -1 ? 2 : -1;
        }

        private boolean shouldUseAllPassFilter(ScanClient scanClient) {
            return scanClient == null || scanClient.filters == null || scanClient.filters.isEmpty() || scanClient.filters.size() > this.mFilterIndexStack.size();
        }

        private void initFilterIndexStack() {
            int numOfOffloadedScanFilterSupported = AdapterService.getAdapterService().getNumOfOffloadedScanFilterSupported();
            for (int i = 3; i < numOfOffloadedScanFilterSupported; i++) {
                this.mFilterIndexStack.add(Integer.valueOf(i));
            }
        }

        private void configureFilterParamter(int i, ScanClient scanClient, int i2, int i3, int i4) {
            int i5;
            int deliveryMode = getDeliveryMode(scanClient);
            ScanSettings scanSettings = scanClient.settings;
            int onFoundOnLostTimeoutMillis = getOnFoundOnLostTimeoutMillis(scanSettings, true);
            getOnFoundOnLostTimeoutMillis(scanSettings, false);
            int onFoundOnLostSightings = getOnFoundOnLostSightings(scanSettings);
            if (ScanManager.DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("configureFilterParamter ");
                sb.append(onFoundOnLostTimeoutMillis);
                sb.append(" ");
                sb.append(10000);
                sb.append(" ");
                sb.append(onFoundOnLostSightings);
                sb.append(" ");
                i5 = i4;
                sb.append(i5);
                Log.d(ScanManager.TAG, sb.toString());
            } else {
                i5 = i4;
            }
            gattClientScanFilterParamAddNative(new FilterParams(i, i3, i2, LIST_LOGIC_TYPE, 1, -128, -128, deliveryMode, onFoundOnLostTimeoutMillis, 10000, onFoundOnLostSightings, i5));
        }

        private int getDeliveryMode(ScanClient scanClient) {
            ScanSettings scanSettings;
            if (scanClient == null || (scanSettings = scanClient.settings) == null) {
                return 0;
            }
            if ((scanSettings.getCallbackType() & 2) == 0 && (scanSettings.getCallbackType() & 4) == 0) {
                return scanSettings.getReportDelayMillis() == 0 ? 0 : 2;
            }
            return 1;
        }

        private int getScanWindowMillis(ScanSettings scanSettings) {
            ContentResolver contentResolver = ScanManager.this.mService.getContentResolver();
            if (scanSettings == null) {
                return Settings.Global.getInt(contentResolver, "ble_scan_low_power_window_ms", 512);
            }
            switch (scanSettings.getScanMode()) {
            }
            return Settings.Global.getInt(contentResolver, "ble_scan_low_power_window_ms", 512);
        }

        private int getScanIntervalMillis(ScanSettings scanSettings) {
            ContentResolver contentResolver = ScanManager.this.mService.getContentResolver();
            if (scanSettings == null) {
                return Settings.Global.getInt(contentResolver, "ble_scan_low_power_interval_ms", SCAN_MODE_LOW_POWER_INTERVAL_MS);
            }
            switch (scanSettings.getScanMode()) {
            }
            return Settings.Global.getInt(contentResolver, "ble_scan_low_power_interval_ms", SCAN_MODE_LOW_POWER_INTERVAL_MS);
        }

        private int getOnFoundOnLostTimeoutMillis(ScanSettings scanSettings, boolean z) {
            int i = scanSettings.getMatchMode() != 1 ? 3 : 1;
            if (!z) {
                i *= 2;
            }
            return ONLOST_ONFOUND_BASE_TIMEOUT_MS * i;
        }

        private int getOnFoundOnLostSightings(ScanSettings scanSettings) {
            if (scanSettings == null || scanSettings.getMatchMode() == 1) {
                return 1;
            }
            return 4;
        }

        private int getNumOfTrackingAdvertisements(ScanSettings scanSettings) {
            if (scanSettings == null) {
                return 0;
            }
            int totalNumOfTrackableAdvertisements = AdapterService.getAdapterService().getTotalNumOfTrackableAdvertisements();
            switch (scanSettings.getNumOfMatches()) {
                case 1:
                    break;
                case 2:
                    return 2;
                case 3:
                    return totalNumOfTrackableAdvertisements / 2;
                default:
                    if (ScanManager.DBG) {
                        Log.d(ScanManager.TAG, "Invalid setting for getNumOfMatches() " + scanSettings.getNumOfMatches());
                    }
                    break;
            }
            return 1;
        }

        private boolean manageAllocationOfTrackingAdvertisement(int i, boolean z) {
            int totalNumOfTrackableAdvertisements = AdapterService.getAdapterService().getTotalNumOfTrackableAdvertisements();
            synchronized (ScanManager.this.mCurUsedTrackableAdvertisements) {
                int iIntValue = totalNumOfTrackableAdvertisements - ScanManager.this.mCurUsedTrackableAdvertisements.intValue();
                if (!z) {
                    if (i > ScanManager.this.mCurUsedTrackableAdvertisements.intValue()) {
                        return false;
                    }
                    ScanManager.this.mCurUsedTrackableAdvertisements = Integer.valueOf(ScanManager.this.mCurUsedTrackableAdvertisements.intValue() - i);
                    return true;
                }
                if (iIntValue < i) {
                    return false;
                }
                ScanManager.this.mCurUsedTrackableAdvertisements = Integer.valueOf(ScanManager.this.mCurUsedTrackableAdvertisements.intValue() + i);
                return true;
            }
        }
    }

    private boolean isScreenOn() {
        Display[] displays = this.mDm.getDisplays();
        if (displays == null) {
            return false;
        }
        for (Display display : displays) {
            if (display.getState() == 2) {
                return true;
            }
        }
        return false;
    }

    private void handleImportanceChange(UidImportance uidImportance) {
        if (uidImportance == null) {
            return;
        }
        int i = uidImportance.uid;
        boolean z = false;
        if (uidImportance.importance <= FOREGROUND_IMPORTANCE_CUTOFF) {
            for (ScanClient scanClient : this.mRegularScanClients) {
                if (scanClient.appUid == i && scanClient.passiveSettings != null) {
                    scanClient.settings = scanClient.passiveSettings;
                    scanClient.passiveSettings = null;
                    z = true;
                }
            }
        } else {
            int i2 = Settings.Global.getInt(this.mService.getContentResolver(), "ble_scan_background_mode", 0);
            for (ScanClient scanClient2 : this.mRegularScanClients) {
                if (scanClient2.appUid == i && !this.mScanNative.isOpportunisticScanClient(scanClient2)) {
                    scanClient2.passiveSettings = scanClient2.settings;
                    ScanSettings.Builder builder = new ScanSettings.Builder();
                    ScanSettings scanSettings = scanClient2.settings;
                    builder.setScanMode(i2);
                    builder.setCallbackType(scanSettings.getCallbackType());
                    builder.setScanResultType(scanSettings.getScanResultType());
                    builder.setReportDelay(scanSettings.getReportDelayMillis());
                    builder.setNumOfMatches(scanSettings.getNumOfMatches());
                    scanClient2.settings = builder.build();
                    z = true;
                }
            }
        }
        if (z) {
            this.mScanNative.configureRegularScanParams();
        }
    }
}
