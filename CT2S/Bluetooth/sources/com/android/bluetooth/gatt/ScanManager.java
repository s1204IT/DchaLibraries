package com.android.bluetooth.gatt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.gatt.ScanFilterQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScanManager {
    private static final String ACTION_REFRESH_BATCHED_SCAN = "com.android.bluetooth.gatt.REFRESH_BATCHED_SCAN";
    private static final boolean DBG = true;
    private static final int MSG_FLUSH_BATCH_RESULTS = 2;
    private static final int MSG_START_BLE_SCAN = 0;
    private static final int MSG_STOP_BLE_SCAN = 1;
    private static final int OPERATION_TIME_OUT_MILLIS = 500;
    static final int SCAN_RESULT_TYPE_BOTH = 3;
    static final int SCAN_RESULT_TYPE_FULL = 2;
    static final int SCAN_RESULT_TYPE_TRUNCATED = 1;
    private static final String TAG = "BtGatt.ScanManager";
    private BroadcastReceiver mBatchAlarmReceiver;
    private boolean mBatchAlarmReceiverRegistered;
    private BatchScanParams mBatchScanParms;
    private ClientHandler mHandler;
    private CountDownLatch mLatch;
    private GattService mService;
    private int mLastConfiguredScanSetting = Integer.MIN_VALUE;
    private Set<ScanClient> mRegularScanClients = new HashSet();
    private Set<ScanClient> mBatchClients = new HashSet();
    private ScanNative mScanNative = new ScanNative();

    ScanManager(GattService service) {
        this.mService = service;
    }

    void start() {
        HandlerThread thread = new HandlerThread("BluetoothScanManager");
        thread.start();
        this.mHandler = new ClientHandler(thread.getLooper());
    }

    void cleanup() {
        this.mRegularScanClients.clear();
        this.mBatchClients.clear();
        this.mScanNative.cleanup();
    }

    Set<ScanClient> getRegularScanQueue() {
        return this.mRegularScanClients;
    }

    Set<ScanClient> getBatchScanQueue() {
        return this.mBatchClients;
    }

    Set<ScanClient> getFullBatchScanQueue() {
        Set<ScanClient> fullBatchClients = new HashSet<>();
        for (ScanClient client : this.mBatchClients) {
            if (client.settings.getScanResultType() == 0) {
                fullBatchClients.add(client);
            }
        }
        return fullBatchClients;
    }

    void startScan(ScanClient client) {
        sendMessage(0, client);
    }

    void stopScan(ScanClient client) {
        sendMessage(1, client);
    }

    void flushBatchScanResults(ScanClient client) {
        sendMessage(2, client);
    }

    void callbackDone(int clientIf, int status) {
        logd("callback done for clientIf - " + clientIf + " status - " + status);
        if (status == 0) {
            this.mLatch.countDown();
        }
    }

    private void sendMessage(int what, ScanClient client) {
        Message message = new Message();
        message.what = what;
        message.obj = client;
        this.mHandler.sendMessage(message);
    }

    private boolean isFilteringSupported() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter.isOffloadedFilteringSupported();
    }

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ScanClient client = (ScanClient) msg.obj;
            switch (msg.what) {
                case 0:
                    handleStartScan(client);
                    break;
                case 1:
                    handleStopScan(client);
                    break;
                case 2:
                    handleFlushBatchResults(client);
                    break;
                default:
                    Log.e(ScanManager.TAG, "received an unkown message : " + msg.what);
                    break;
            }
        }

        void handleStartScan(ScanClient client) {
            Utils.enforceAdminPermission(ScanManager.this.mService);
            ScanManager.this.logd("handling starting scan");
            if (isScanSupported(client)) {
                if (ScanManager.this.mRegularScanClients.contains(client) || ScanManager.this.mBatchClients.contains(client)) {
                    Log.e(ScanManager.TAG, "Scan already started");
                    return;
                }
                if (isBatchClient(client)) {
                    ScanManager.this.mBatchClients.add(client);
                    ScanManager.this.mScanNative.startBatchScan(client);
                    return;
                } else {
                    ScanManager.this.mRegularScanClients.add(client);
                    ScanManager.this.mScanNative.startRegularScan(client);
                    ScanManager.this.mScanNative.configureRegularScanParams();
                    return;
                }
            }
            Log.e(ScanManager.TAG, "Scan settings not supported");
        }

        void handleStopScan(ScanClient client) {
            Utils.enforceAdminPermission(ScanManager.this.mService);
            if (client != null) {
                if (ScanManager.this.mRegularScanClients.contains(client)) {
                    ScanManager.this.mScanNative.stopRegularScan(client);
                    ScanManager.this.mScanNative.configureRegularScanParams();
                } else {
                    ScanManager.this.mScanNative.stopBatchScan(client);
                }
                if (client.appDied) {
                    ScanManager.this.logd("app died, unregister client - " + client.clientIf);
                    ScanManager.this.mService.unregisterClient(client.clientIf);
                }
            }
        }

        void handleFlushBatchResults(ScanClient client) {
            Utils.enforceAdminPermission(ScanManager.this.mService);
            if (ScanManager.this.mBatchClients.contains(client)) {
                ScanManager.this.mScanNative.flushBatchResults(client.clientIf);
            }
        }

        private boolean isBatchClient(ScanClient client) {
            if (client == null || client.settings == null) {
                return false;
            }
            ScanSettings settings = client.settings;
            return settings.getCallbackType() == 1 && settings.getReportDelayMillis() != 0;
        }

        private boolean isScanSupported(ScanClient client) {
            if (client == null || client.settings == null) {
                return true;
            }
            ScanSettings settings = client.settings;
            if (ScanManager.this.isFilteringSupported()) {
                return true;
            }
            return settings.getCallbackType() == 1 && settings.getReportDelayMillis() == 0;
        }
    }

    class BatchScanParams {
        int scanMode = -1;
        int fullScanClientIf = -1;
        int truncatedScanClientIf = -1;

        BatchScanParams() {
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            BatchScanParams other = (BatchScanParams) obj;
            return this.scanMode == other.scanMode && this.fullScanClientIf == other.fullScanClientIf && this.truncatedScanClientIf == other.truncatedScanClientIf;
        }
    }

    private class ScanNative {
        private static final int ALL_PASS_FILTER_INDEX_BATCH_SCAN = 2;
        private static final int ALL_PASS_FILTER_INDEX_REGULAR_SCAN = 1;
        private static final int ALL_PASS_FILTER_SELECTION = 0;
        private static final int DEFAULT_ONLOST_ONFOUND_TIMEOUT_MILLIS = 1000;
        private static final int DELIVERY_MODE_BATCH = 2;
        private static final int DELIVERY_MODE_IMMEDIATE = 0;
        private static final int DELIVERY_MODE_ON_FOUND_LOST = 1;
        private static final int DISCARD_OLDEST_WHEN_BUFFER_FULL = 0;
        private static final int FILTER_LOGIC_TYPE = 1;
        private static final int LIST_LOGIC_TYPE = 17895697;
        private static final int ONFOUND_SIGHTINGS = 2;
        private static final int SCAN_MODE_BALANCED_INTERVAL_MS = 5000;
        private static final int SCAN_MODE_BALANCED_WINDOW_MS = 2000;
        private static final int SCAN_MODE_BATCH_BALANCED_INTERVAL_MS = 15000;
        private static final int SCAN_MODE_BATCH_BALANCED_WINDOW_MS = 1500;
        private static final int SCAN_MODE_BATCH_LOW_LATENCY_INTERVAL_MS = 5000;
        private static final int SCAN_MODE_BATCH_LOW_LATENCY_WINDOW_MS = 1500;
        private static final int SCAN_MODE_BATCH_LOW_POWER_INTERVAL_MS = 150000;
        private static final int SCAN_MODE_BATCH_LOW_POWER_WINDOW_MS = 1500;
        private static final int SCAN_MODE_LOW_LATENCY_INTERVAL_MS = 5000;
        private static final int SCAN_MODE_LOW_LATENCY_WINDOW_MS = 5000;
        private static final int SCAN_MODE_LOW_POWER_INTERVAL_MS = 5000;
        private static final int SCAN_MODE_LOW_POWER_WINDOW_MS = 500;
        private AlarmManager mAlarmManager;
        private PendingIntent mBatchScanIntervalIntent;
        private final Set<Integer> mAllPassRegularClients = new HashSet();
        private final Set<Integer> mAllPassBatchClients = new HashSet();
        private final Deque<Integer> mFilterIndexStack = new ArrayDeque();
        private final Map<Integer, Deque<Integer>> mClientFilterIndexMap = new HashMap();

        private native void gattClientConfigBatchScanStorageNative(int i, int i2, int i3, int i4);

        private native void gattClientReadScanReportsNative(int i, int i2);

        private native void gattClientScanFilterAddNative(int i, int i2, int i3, int i4, int i5, long j, long j2, long j3, long j4, String str, String str2, byte b, byte[] bArr, byte[] bArr2);

        private native void gattClientScanFilterClearNative(int i, int i2);

        private native void gattClientScanFilterDeleteNative(int i, int i2, int i3, int i4, int i5, long j, long j2, long j3, long j4, String str, String str2, byte b, byte[] bArr, byte[] bArr2);

        private native void gattClientScanFilterEnableNative(int i, boolean z);

        private native void gattClientScanFilterParamAddNative(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11);

        private native void gattClientScanFilterParamClearAllNative(int i);

        private native void gattClientScanFilterParamDeleteNative(int i, int i2);

        private native void gattClientScanNative(boolean z);

        private native void gattClientStartBatchScanNative(int i, int i2, int i3, int i4, int i5, int i6);

        private native void gattClientStopBatchScanNative(int i);

        private native void gattSetScanParametersNative(int i, int i2);

        ScanNative() {
            this.mAlarmManager = (AlarmManager) ScanManager.this.mService.getSystemService("alarm");
            Intent batchIntent = new Intent(ScanManager.ACTION_REFRESH_BATCHED_SCAN, (Uri) null);
            this.mBatchScanIntervalIntent = PendingIntent.getBroadcast(ScanManager.this.mService, 0, batchIntent, 0);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ScanManager.ACTION_REFRESH_BATCHED_SCAN);
            ScanManager.this.mBatchAlarmReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(ScanManager.TAG, "awakened up at time " + SystemClock.elapsedRealtime());
                    String action = intent.getAction();
                    if (action.equals(ScanManager.ACTION_REFRESH_BATCHED_SCAN) && !ScanManager.this.mBatchClients.isEmpty()) {
                        ScanManager.this.flushBatchScanResults((ScanClient) ScanManager.this.mBatchClients.iterator().next());
                    }
                }
            };
            ScanManager.this.mService.registerReceiver(ScanManager.this.mBatchAlarmReceiver, filter);
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
            int scanWindow;
            int scanInterval;
            ScanManager.this.logd("configureRegularScanParams() - queue=" + ScanManager.this.mRegularScanClients.size());
            int curScanSetting = Integer.MIN_VALUE;
            ScanClient client = getAggressiveClient(ScanManager.this.mRegularScanClients);
            if (client != null) {
                curScanSetting = client.settings.getScanMode();
            }
            ScanManager.this.logd("configureRegularScanParams() - ScanSetting Scan mode=" + curScanSetting + " mLastConfiguredScanSetting=" + ScanManager.this.mLastConfiguredScanSetting);
            if (curScanSetting != Integer.MIN_VALUE) {
                if (curScanSetting != ScanManager.this.mLastConfiguredScanSetting) {
                    switch (curScanSetting) {
                        case 0:
                            scanWindow = SCAN_MODE_LOW_POWER_WINDOW_MS;
                            scanInterval = 5000;
                            break;
                        case 1:
                            scanWindow = SCAN_MODE_BALANCED_WINDOW_MS;
                            scanInterval = 5000;
                            break;
                        case 2:
                            scanWindow = 5000;
                            scanInterval = 5000;
                            break;
                        default:
                            Log.e(ScanManager.TAG, "Invalid value for curScanSetting " + curScanSetting);
                            scanWindow = SCAN_MODE_LOW_POWER_WINDOW_MS;
                            scanInterval = 5000;
                            break;
                    }
                    int scanWindow2 = Utils.millsToUnit(scanWindow);
                    int scanInterval2 = Utils.millsToUnit(scanInterval);
                    gattClientScanNative(false);
                    gattSetScanParametersNative(scanInterval2, scanWindow2);
                    gattClientScanNative(true);
                    ScanManager.this.mLastConfiguredScanSetting = curScanSetting;
                    return;
                }
                return;
            }
            ScanManager.this.mLastConfiguredScanSetting = curScanSetting;
            ScanManager.this.logd("configureRegularScanParams() - queue emtpy, scan stopped");
        }

        ScanClient getAggressiveClient(Set<ScanClient> cList) {
            ScanClient result = null;
            int curScanSetting = Integer.MIN_VALUE;
            for (ScanClient client : cList) {
                if (client.settings.getScanMode() > curScanSetting) {
                    result = client;
                    curScanSetting = client.settings.getScanMode();
                }
            }
            return result;
        }

        void startRegularScan(ScanClient client) {
            if (ScanManager.this.isFilteringSupported() && this.mFilterIndexStack.isEmpty() && this.mClientFilterIndexMap.isEmpty()) {
                initFilterIndexStack();
            }
            if (ScanManager.this.isFilteringSupported()) {
                configureScanFilters(client);
            }
            if (ScanManager.this.mRegularScanClients.size() == 1) {
                gattClientScanNative(true);
            }
        }

        void startBatchScan(ScanClient client) {
            if (this.mFilterIndexStack.isEmpty() && ScanManager.this.isFilteringSupported()) {
                initFilterIndexStack();
            }
            configureScanFilters(client);
            resetBatchScan(client);
        }

        private void resetBatchScan(ScanClient client) {
            int clientIf = client.clientIf;
            BatchScanParams batchScanParams = getBatchScanParams();
            if (ScanManager.this.mBatchScanParms != null && !ScanManager.this.mBatchScanParms.equals(batchScanParams)) {
                ScanManager.this.logd("stopping BLe Batch");
                resetCountDownLatch();
                gattClientStopBatchScanNative(clientIf);
                waitForCallback();
                flushBatchResults(clientIf);
            }
            if (batchScanParams != null && !batchScanParams.equals(ScanManager.this.mBatchScanParms)) {
                ScanManager.this.logd("Starting BLE batch scan");
                int resultType = getResultType(batchScanParams);
                int fullScanPercent = getFullScanStoragePercent(resultType);
                resetCountDownLatch();
                ScanManager.this.logd("configuring batch scan storage, appIf " + client.clientIf);
                gattClientConfigBatchScanStorageNative(client.clientIf, fullScanPercent, 100 - fullScanPercent, 95);
                waitForCallback();
                resetCountDownLatch();
                int scanInterval = Utils.millsToUnit(getBatchScanIntervalMillis(batchScanParams.scanMode));
                int scanWindow = Utils.millsToUnit(getBatchScanWindowMillis(batchScanParams.scanMode));
                gattClientStartBatchScanNative(clientIf, resultType, scanInterval, scanWindow, 0, 0);
                waitForCallback();
            }
            ScanManager.this.mBatchScanParms = batchScanParams;
            setBatchAlarm();
        }

        private int getFullScanStoragePercent(int resultType) {
            switch (resultType) {
                case 1:
                    return 0;
                case 2:
                    return 100;
                case 3:
                default:
                    return 50;
            }
        }

        private BatchScanParams getBatchScanParams() {
            if (ScanManager.this.mBatchClients.isEmpty()) {
                return null;
            }
            BatchScanParams params = ScanManager.this.new BatchScanParams();
            for (ScanClient client : ScanManager.this.mBatchClients) {
                params.scanMode = Math.max(params.scanMode, client.settings.getScanMode());
                if (client.settings.getScanResultType() == 0) {
                    params.fullScanClientIf = client.clientIf;
                } else {
                    params.truncatedScanClientIf = client.clientIf;
                }
            }
            return params;
        }

        private int getBatchScanWindowMillis(int scanMode) {
            switch (scanMode) {
            }
            return 1500;
        }

        private int getBatchScanIntervalMillis(int scanMode) {
            switch (scanMode) {
                case 0:
                default:
                    return SCAN_MODE_BATCH_LOW_POWER_INTERVAL_MS;
                case 1:
                    return SCAN_MODE_BATCH_BALANCED_INTERVAL_MS;
                case 2:
                    return 5000;
            }
        }

        private void setBatchAlarm() {
            this.mAlarmManager.cancel(this.mBatchScanIntervalIntent);
            if (!ScanManager.this.mBatchClients.isEmpty()) {
                long batchTriggerIntervalMillis = getBatchTriggerIntervalMillis();
                long windowLengthMillis = batchTriggerIntervalMillis / 10;
                long windowStartMillis = SystemClock.elapsedRealtime() + batchTriggerIntervalMillis;
                this.mAlarmManager.setWindow(2, windowStartMillis, windowLengthMillis, this.mBatchScanIntervalIntent);
            }
        }

        void stopRegularScan(ScanClient client) {
            removeScanFilters(client.clientIf);
            ScanManager.this.mRegularScanClients.remove(client);
            if (ScanManager.this.mRegularScanClients.isEmpty()) {
                ScanManager.this.logd("stop scan");
                gattClientScanNative(false);
            }
        }

        void stopBatchScan(ScanClient client) {
            ScanManager.this.mBatchClients.remove(client);
            removeScanFilters(client.clientIf);
            resetBatchScan(client);
        }

        void flushBatchResults(int clientIf) {
            ScanManager.this.logd("flushPendingBatchResults - clientIf = " + clientIf);
            if (ScanManager.this.mBatchScanParms.fullScanClientIf != -1) {
                resetCountDownLatch();
                gattClientReadScanReportsNative(ScanManager.this.mBatchScanParms.fullScanClientIf, 2);
                waitForCallback();
            }
            if (ScanManager.this.mBatchScanParms.truncatedScanClientIf != -1) {
                resetCountDownLatch();
                gattClientReadScanReportsNative(ScanManager.this.mBatchScanParms.truncatedScanClientIf, 1);
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
            long intervalMillis = Long.MAX_VALUE;
            for (ScanClient client : ScanManager.this.mBatchClients) {
                if (client.settings != null && client.settings.getReportDelayMillis() > 0) {
                    intervalMillis = Math.min(intervalMillis, client.settings.getReportDelayMillis());
                }
            }
            return intervalMillis;
        }

        private void configureScanFilters(ScanClient client) {
            int clientIf = client.clientIf;
            int deliveryMode = getDeliveryMode(client);
            if (shouldAddAllPassFilterToController(client, deliveryMode)) {
                resetCountDownLatch();
                gattClientScanFilterEnableNative(clientIf, true);
                waitForCallback();
                if (shouldUseAllPassFilter(client)) {
                    int filterIndex = deliveryMode != 2 ? 1 : 2;
                    resetCountDownLatch();
                    configureFilterParamter(clientIf, client, 0, filterIndex);
                    waitForCallback();
                    return;
                }
                Deque<Integer> clientFilterIndices = new ArrayDeque<>();
                for (ScanFilter filter : client.filters) {
                    ScanFilterQueue queue = new ScanFilterQueue();
                    queue.addScanFilter(filter);
                    int featureSelection = queue.getFeatureSelection();
                    int filterIndex2 = this.mFilterIndexStack.pop().intValue();
                    while (!queue.isEmpty()) {
                        resetCountDownLatch();
                        addFilterToController(clientIf, queue.pop(), filterIndex2);
                        waitForCallback();
                    }
                    resetCountDownLatch();
                    configureFilterParamter(clientIf, client, featureSelection, filterIndex2);
                    waitForCallback();
                    clientFilterIndices.add(Integer.valueOf(filterIndex2));
                }
                this.mClientFilterIndexMap.put(Integer.valueOf(clientIf), clientFilterIndices);
            }
        }

        private boolean shouldAddAllPassFilterToController(ScanClient client, int deliveryMode) {
            if (!shouldUseAllPassFilter(client)) {
                return true;
            }
            if (deliveryMode == 2) {
                this.mAllPassBatchClients.add(Integer.valueOf(client.clientIf));
                return this.mAllPassBatchClients.size() == 1;
            }
            this.mAllPassRegularClients.add(Integer.valueOf(client.clientIf));
            return this.mAllPassRegularClients.size() == 1;
        }

        private void removeScanFilters(int clientIf) {
            Deque<Integer> filterIndices = this.mClientFilterIndexMap.remove(Integer.valueOf(clientIf));
            if (filterIndices != null) {
                this.mFilterIndexStack.addAll(filterIndices);
                for (Integer filterIndex : filterIndices) {
                    resetCountDownLatch();
                    gattClientScanFilterParamDeleteNative(clientIf, filterIndex.intValue());
                    waitForCallback();
                }
            }
            removeFilterIfExisits(this.mAllPassRegularClients, clientIf, 1);
            removeFilterIfExisits(this.mAllPassBatchClients, clientIf, 2);
        }

        private void removeFilterIfExisits(Set<Integer> clients, int clientIf, int filterIndex) {
            if (clients.contains(Integer.valueOf(clientIf))) {
                clients.remove(Integer.valueOf(clientIf));
                if (clients.isEmpty()) {
                    resetCountDownLatch();
                    gattClientScanFilterParamDeleteNative(clientIf, filterIndex);
                    waitForCallback();
                }
            }
        }

        private ScanClient getBatchScanClient(int clientIf) {
            for (ScanClient client : ScanManager.this.mBatchClients) {
                if (client.clientIf == clientIf) {
                    return client;
                }
            }
            return null;
        }

        private int getResultType(BatchScanParams params) {
            if (params.fullScanClientIf != -1 && params.truncatedScanClientIf != -1) {
                return 3;
            }
            if (params.truncatedScanClientIf != -1) {
                return 1;
            }
            return params.fullScanClientIf != -1 ? 2 : -1;
        }

        private boolean shouldUseAllPassFilter(ScanClient client) {
            return client == null || client.filters == null || client.filters.isEmpty() || client.filters.size() > this.mFilterIndexStack.size();
        }

        private void addFilterToController(int clientIf, ScanFilterQueue.Entry entry, int filterIndex) {
            ScanManager.this.logd("addFilterToController: " + ((int) entry.type));
            switch (entry.type) {
                case 0:
                    ScanManager.this.logd("add address " + entry.address);
                    gattClientScanFilterAddNative(clientIf, entry.type, filterIndex, 0, 0, 0L, 0L, 0L, 0L, "", entry.address, (byte) 0, new byte[0], new byte[0]);
                    break;
                case 2:
                case 3:
                    gattClientScanFilterAddNative(clientIf, entry.type, filterIndex, 0, 0, entry.uuid.getLeastSignificantBits(), entry.uuid.getMostSignificantBits(), entry.uuid_mask.getLeastSignificantBits(), entry.uuid_mask.getMostSignificantBits(), "", "", (byte) 0, new byte[0], new byte[0]);
                    break;
                case 4:
                    ScanManager.this.logd("adding filters: " + entry.name);
                    gattClientScanFilterAddNative(clientIf, entry.type, filterIndex, 0, 0, 0L, 0L, 0L, 0L, entry.name, "", (byte) 0, new byte[0], new byte[0]);
                    break;
                case 5:
                    int len = entry.data.length;
                    if (entry.data_mask.length == len) {
                        gattClientScanFilterAddNative(clientIf, entry.type, filterIndex, entry.company, entry.company_mask, 0L, 0L, 0L, 0L, "", "", (byte) 0, entry.data, entry.data_mask);
                    }
                    break;
                case 6:
                    gattClientScanFilterAddNative(clientIf, entry.type, filterIndex, 0, 0, 0L, 0L, 0L, 0L, "", "", (byte) 0, entry.data, entry.data_mask);
                    break;
            }
        }

        private void initFilterIndexStack() {
            int maxFiltersSupported = AdapterService.getAdapterService().getNumOfOffloadedScanFilterSupported();
            for (int i = 3; i < maxFiltersSupported; i++) {
                this.mFilterIndexStack.add(Integer.valueOf(i));
            }
        }

        private void configureFilterParamter(int clientIf, ScanClient client, int featureSelection, int filterIndex) {
            int deliveryMode = getDeliveryMode(client);
            int timeout = getOnfoundLostTimeout(client);
            gattClientScanFilterParamAddNative(clientIf, filterIndex, featureSelection, LIST_LOGIC_TYPE, 1, -128, -128, deliveryMode, timeout, timeout, 2);
        }

        private int getDeliveryMode(ScanClient client) {
            ScanSettings settings;
            if (client == null || (settings = client.settings) == null) {
                return 0;
            }
            if ((settings.getCallbackType() & 2) == 0 && (settings.getCallbackType() & 4) == 0) {
                return settings.getReportDelayMillis() != 0 ? 2 : 0;
            }
            return 1;
        }

        private int getOnfoundLostTimeout(ScanClient client) {
            ScanSettings settings;
            if (client == null || (settings = client.settings) == null) {
                return 1000;
            }
            return (int) settings.getReportDelayMillis();
        }
    }

    private void logd(String s) {
        Log.d(TAG, s);
    }
}
