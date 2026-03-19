package com.android.bluetooth.gatt;

import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.bluetooth.le.PeriodicAdvertisingReport;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.btservice.AdapterService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class PeriodicScanManager {
    private static final String TAG = "BtGatt.SyncManager";
    private final AdapterService mAdapterService;
    Map<IBinder, SyncInfo> mSyncs = Collections.synchronizedMap(new HashMap());
    private static final boolean DBG = GattServiceConfig.DBG;
    static int sTempRegistrationId = -1;

    private static native void classInitNative();

    private native void cleanupNative();

    private native void initializeNative();

    private native void startSyncNative(int i, String str, int i2, int i3, int i4);

    private native void stopSyncNative(int i);

    static {
        classInitNative();
    }

    PeriodicScanManager(AdapterService adapterService) {
        if (DBG) {
            Log.d(TAG, "advertise manager created");
        }
        this.mAdapterService = adapterService;
    }

    void start() {
        initializeNative();
    }

    void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }
        cleanupNative();
        this.mSyncs.clear();
        sTempRegistrationId = -1;
    }

    class SyncInfo {
        public IPeriodicAdvertisingCallback callback;
        public SyncDeathRecipient deathRecipient;
        public Integer id;

        SyncInfo(Integer num, SyncDeathRecipient syncDeathRecipient, IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
            this.id = num;
            this.deathRecipient = syncDeathRecipient;
            this.callback = iPeriodicAdvertisingCallback;
        }
    }

    IBinder toBinder(IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
        return iPeriodicAdvertisingCallback.asBinder();
    }

    class SyncDeathRecipient implements IBinder.DeathRecipient {
        public IPeriodicAdvertisingCallback callback;

        SyncDeathRecipient(IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
            this.callback = iPeriodicAdvertisingCallback;
        }

        @Override
        public void binderDied() {
            if (PeriodicScanManager.DBG) {
                Log.d(PeriodicScanManager.TAG, "Binder is dead - unregistering advertising set");
            }
            PeriodicScanManager.this.stopSync(this.callback);
        }
    }

    Map.Entry<IBinder, SyncInfo> findSync(int i) {
        for (Map.Entry<IBinder, SyncInfo> entry : this.mSyncs.entrySet()) {
            if (entry.getValue().id.intValue() == i) {
                return entry;
            }
        }
        return null;
    }

    void onSyncStarted(int i, int i2, int i3, int i4, String str, int i5, int i6, int i7) throws Exception {
        if (DBG) {
            Log.d(TAG, "onSyncStarted() - regId=" + i + ", syncHandle=" + i2 + ", status=" + i7);
        }
        Map.Entry<IBinder, SyncInfo> entryFindSync = findSync(i);
        if (entryFindSync == null) {
            Log.i(TAG, "onSyncStarted() - no callback found for regId " + i);
            stopSyncNative(i2);
            return;
        }
        IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback = entryFindSync.getValue().callback;
        if (i7 == 0) {
            entryFindSync.setValue(new SyncInfo(Integer.valueOf(i2), entryFindSync.getValue().deathRecipient, iPeriodicAdvertisingCallback));
            return;
        }
        IBinder key = entryFindSync.getKey();
        key.unlinkToDeath(entryFindSync.getValue().deathRecipient, 0);
        this.mSyncs.remove(key);
    }

    void onSyncReport(int i, int i2, int i3, int i4, byte[] bArr) throws Exception {
        if (DBG) {
            Log.d(TAG, "onSyncReport() - syncHandle=" + i);
        }
        Map.Entry<IBinder, SyncInfo> entryFindSync = findSync(i);
        if (entryFindSync == null) {
            Log.i(TAG, "onSyncReport() - no callback found for syncHandle " + i);
            return;
        }
        entryFindSync.getValue().callback.onPeriodicAdvertisingReport(new PeriodicAdvertisingReport(i, i2, i3, i4, ScanRecord.parseFromBytes(bArr)));
    }

    void onSyncLost(int i) throws Exception {
        if (DBG) {
            Log.d(TAG, "onSyncLost() - syncHandle=" + i);
        }
        Map.Entry<IBinder, SyncInfo> entryFindSync = findSync(i);
        if (entryFindSync == null) {
            Log.i(TAG, "onSyncLost() - no callback found for syncHandle " + i);
            return;
        }
        IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback = entryFindSync.getValue().callback;
        this.mSyncs.remove(entryFindSync);
        iPeriodicAdvertisingCallback.onSyncLost(i);
    }

    void startSync(ScanResult scanResult, int i, int i2, IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
        SyncDeathRecipient syncDeathRecipient = new SyncDeathRecipient(iPeriodicAdvertisingCallback);
        IBinder binder = toBinder(iPeriodicAdvertisingCallback);
        try {
            binder.linkToDeath(syncDeathRecipient, 0);
            String address = scanResult.getDevice().getAddress();
            int advertisingSid = scanResult.getAdvertisingSid();
            int i3 = sTempRegistrationId - 1;
            sTempRegistrationId = i3;
            this.mSyncs.put(binder, new SyncInfo(Integer.valueOf(i3), syncDeathRecipient, iPeriodicAdvertisingCallback));
            if (DBG) {
                Log.d(TAG, "startSync() - reg_id=" + i3 + ", callback: " + binder);
            }
            startSyncNative(advertisingSid, address, i, i2, i3);
        } catch (RemoteException e) {
            throw new IllegalArgumentException("Can't link to periodic scanner death");
        }
    }

    void stopSync(IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
        IBinder binder = toBinder(iPeriodicAdvertisingCallback);
        if (DBG) {
            Log.d(TAG, "stopSync() " + binder);
        }
        SyncInfo syncInfoRemove = this.mSyncs.remove(binder);
        if (syncInfoRemove == null) {
            Log.e(TAG, "stopSync() - no client found for callback");
            return;
        }
        Integer num = syncInfoRemove.id;
        binder.unlinkToDeath(syncInfoRemove.deathRecipient, 0);
        if (num.intValue() < 0) {
            Log.i(TAG, "stopSync() - not finished registration yet");
        } else {
            stopSyncNative(num.intValue());
        }
    }
}
