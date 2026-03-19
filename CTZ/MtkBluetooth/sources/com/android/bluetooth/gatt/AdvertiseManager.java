package com.android.bluetooth.gatt;

import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.btservice.AdapterService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AdvertiseManager {
    private static final String TAG = "BtGatt.AdvertiseManager";
    private final AdapterService mAdapterService;
    Map<IBinder, AdvertiserInfo> mAdvertisers = Collections.synchronizedMap(new HashMap());
    private Handler mHandler;
    private final GattService mService;
    private static final boolean DBG = GattServiceConfig.DBG;
    static int sTempRegistrationId = -1;

    private static native void classInitNative();

    private native void cleanupNative();

    private native void enableAdvertisingSetNative(int i, boolean z, int i2, int i3);

    private native void getOwnAddressNative(int i);

    private native void initializeNative();

    private native void setAdvertisingDataNative(int i, byte[] bArr);

    private native void setAdvertisingParametersNative(int i, AdvertisingSetParameters advertisingSetParameters);

    private native void setPeriodicAdvertisingDataNative(int i, byte[] bArr);

    private native void setPeriodicAdvertisingEnableNative(int i, boolean z);

    private native void setPeriodicAdvertisingParametersNative(int i, PeriodicAdvertisingParameters periodicAdvertisingParameters);

    private native void setScanResponseDataNative(int i, byte[] bArr);

    private native void startAdvertisingSetNative(AdvertisingSetParameters advertisingSetParameters, byte[] bArr, byte[] bArr2, PeriodicAdvertisingParameters periodicAdvertisingParameters, byte[] bArr3, int i, int i2, int i3);

    private native void stopAdvertisingSetNative(int i);

    static {
        classInitNative();
    }

    AdvertiseManager(GattService gattService, AdapterService adapterService) {
        if (DBG) {
            Log.d(TAG, "advertise manager created");
        }
        this.mService = gattService;
        this.mAdapterService = adapterService;
    }

    void start() {
        initializeNative();
        HandlerThread handlerThread = new HandlerThread("BluetoothAdvertiseManager");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
    }

    void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }
        cleanupNative();
        this.mAdvertisers.clear();
        sTempRegistrationId = -1;
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
            Looper looper = this.mHandler.getLooper();
            if (looper != null) {
                looper.quit();
            }
            this.mHandler = null;
        }
    }

    class AdvertiserInfo {
        public IAdvertisingSetCallback callback;
        public AdvertisingSetDeathRecipient deathRecipient;
        public Integer id;

        AdvertiserInfo(Integer num, AdvertisingSetDeathRecipient advertisingSetDeathRecipient, IAdvertisingSetCallback iAdvertisingSetCallback) {
            this.id = num;
            this.deathRecipient = advertisingSetDeathRecipient;
            this.callback = iAdvertisingSetCallback;
        }
    }

    IBinder toBinder(IAdvertisingSetCallback iAdvertisingSetCallback) {
        return iAdvertisingSetCallback.asBinder();
    }

    class AdvertisingSetDeathRecipient implements IBinder.DeathRecipient {
        public IAdvertisingSetCallback callback;

        AdvertisingSetDeathRecipient(IAdvertisingSetCallback iAdvertisingSetCallback) {
            this.callback = iAdvertisingSetCallback;
        }

        @Override
        public void binderDied() {
            if (AdvertiseManager.DBG) {
                Log.d(AdvertiseManager.TAG, "Binder is dead - unregistering advertising set");
            }
            AdvertiseManager.this.stopAdvertisingSet(this.callback);
        }
    }

    Map.Entry<IBinder, AdvertiserInfo> findAdvertiser(int i) {
        for (Map.Entry<IBinder, AdvertiserInfo> entry : this.mAdvertisers.entrySet()) {
            if (entry.getValue().id.intValue() == i) {
                return entry;
            }
        }
        return null;
    }

    void onAdvertisingSetStarted(int i, int i2, int i3, int i4) throws Exception {
        if (DBG) {
            Log.d(TAG, "onAdvertisingSetStarted() - regId=" + i + ", advertiserId=" + i2 + ", status=" + i4);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onAdvertisingSetStarted() - no callback found for regId " + i);
            stopAdvertisingSetNative(i2);
            return;
        }
        IAdvertisingSetCallback iAdvertisingSetCallback = entryFindAdvertiser.getValue().callback;
        if (i4 == 0) {
            entryFindAdvertiser.setValue(new AdvertiserInfo(Integer.valueOf(i2), entryFindAdvertiser.getValue().deathRecipient, iAdvertisingSetCallback));
        } else {
            IBinder key = entryFindAdvertiser.getKey();
            key.unlinkToDeath(entryFindAdvertiser.getValue().deathRecipient, 0);
            this.mAdvertisers.remove(key);
        }
        iAdvertisingSetCallback.onAdvertisingSetStarted(i2, i3, i4);
    }

    void onAdvertisingEnabled(int i, boolean z, int i2) throws Exception {
        if (DBG) {
            Log.d(TAG, "onAdvertisingSetEnabled() - advertiserId=" + i + ", enable=" + z + ", status=" + i2);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onAdvertisingSetEnable() - no callback found for advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onAdvertisingEnabled(i, z, i2);
    }

    void startAdvertisingSet(AdvertisingSetParameters advertisingSetParameters, AdvertiseData advertiseData, AdvertiseData advertiseData2, PeriodicAdvertisingParameters periodicAdvertisingParameters, AdvertiseData advertiseData3, int i, int i2, IAdvertisingSetCallback iAdvertisingSetCallback) {
        AdvertisingSetDeathRecipient advertisingSetDeathRecipient = new AdvertisingSetDeathRecipient(iAdvertisingSetCallback);
        IBinder binder = toBinder(iAdvertisingSetCallback);
        try {
            binder.linkToDeath(advertisingSetDeathRecipient, 0);
            String name = AdapterService.getAdapterService().getName();
            byte[] bArrAdvertiseDataToBytes = AdvertiseHelper.advertiseDataToBytes(advertiseData, name);
            byte[] bArrAdvertiseDataToBytes2 = AdvertiseHelper.advertiseDataToBytes(advertiseData2, name);
            byte[] bArrAdvertiseDataToBytes3 = AdvertiseHelper.advertiseDataToBytes(advertiseData3, name);
            int i3 = sTempRegistrationId - 1;
            sTempRegistrationId = i3;
            this.mAdvertisers.put(binder, new AdvertiserInfo(Integer.valueOf(i3), advertisingSetDeathRecipient, iAdvertisingSetCallback));
            if (DBG) {
                Log.d(TAG, "startAdvertisingSet() - reg_id=" + i3 + ", callback: " + binder);
            }
            startAdvertisingSetNative(advertisingSetParameters, bArrAdvertiseDataToBytes, bArrAdvertiseDataToBytes2, periodicAdvertisingParameters, bArrAdvertiseDataToBytes3, i, i2, i3);
        } catch (RemoteException e) {
            throw new IllegalArgumentException("Can't link to advertiser's death");
        }
    }

    void onOwnAddressRead(int i, int i2, String str) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onOwnAddressRead() advertiserId=" + i);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.w(TAG, "onOwnAddressRead() - bad advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onOwnAddressRead(i, i2, str);
    }

    void getOwnAddress(int i) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "getOwnAddress() - bad advertiserId " + i);
            return;
        }
        getOwnAddressNative(i);
    }

    void stopAdvertisingSet(IAdvertisingSetCallback iAdvertisingSetCallback) {
        IBinder binder = toBinder(iAdvertisingSetCallback);
        if (DBG) {
            Log.d(TAG, "stopAdvertisingSet() " + binder);
        }
        AdvertiserInfo advertiserInfoRemove = this.mAdvertisers.remove(binder);
        if (advertiserInfoRemove == null) {
            Log.e(TAG, "stopAdvertisingSet() - no client found for callback");
            return;
        }
        Integer num = advertiserInfoRemove.id;
        binder.unlinkToDeath(advertiserInfoRemove.deathRecipient, 0);
        if (num.intValue() < 0) {
            Log.i(TAG, "stopAdvertisingSet() - advertiser not finished registration yet");
            return;
        }
        stopAdvertisingSetNative(num.intValue());
        try {
            iAdvertisingSetCallback.onAdvertisingSetStopped(num.intValue());
        } catch (RemoteException e) {
            Log.i(TAG, "error sending onAdvertisingSetStopped callback", e);
        }
    }

    void enableAdvertisingSet(int i, boolean z, int i2, int i3) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "enableAdvertisingSet() - bad advertiserId " + i);
            return;
        }
        enableAdvertisingSetNative(i, z, i2, i3);
    }

    void setAdvertisingData(int i, AdvertiseData advertiseData) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "setAdvertisingData() - bad advertiserId " + i);
            return;
        }
        setAdvertisingDataNative(i, AdvertiseHelper.advertiseDataToBytes(advertiseData, AdapterService.getAdapterService().getName()));
    }

    void setScanResponseData(int i, AdvertiseData advertiseData) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "setScanResponseData() - bad advertiserId " + i);
            return;
        }
        setScanResponseDataNative(i, AdvertiseHelper.advertiseDataToBytes(advertiseData, AdapterService.getAdapterService().getName()));
    }

    void setAdvertisingParameters(int i, AdvertisingSetParameters advertisingSetParameters) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "setAdvertisingParameters() - bad advertiserId " + i);
            return;
        }
        setAdvertisingParametersNative(i, advertisingSetParameters);
    }

    void setPeriodicAdvertisingParameters(int i, PeriodicAdvertisingParameters periodicAdvertisingParameters) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "setPeriodicAdvertisingParameters() - bad advertiserId " + i);
            return;
        }
        setPeriodicAdvertisingParametersNative(i, periodicAdvertisingParameters);
    }

    void setPeriodicAdvertisingData(int i, AdvertiseData advertiseData) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "setPeriodicAdvertisingData() - bad advertiserId " + i);
            return;
        }
        setPeriodicAdvertisingDataNative(i, AdvertiseHelper.advertiseDataToBytes(advertiseData, AdapterService.getAdapterService().getName()));
    }

    void setPeriodicAdvertisingEnable(int i, boolean z) {
        if (findAdvertiser(i) == null) {
            Log.w(TAG, "setPeriodicAdvertisingEnable() - bad advertiserId " + i);
            return;
        }
        setPeriodicAdvertisingEnableNative(i, z);
    }

    void onAdvertisingDataSet(int i, int i2) throws Exception {
        if (DBG) {
            Log.d(TAG, "onAdvertisingDataSet() advertiserId=" + i + ", status=" + i2);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onAdvertisingDataSet() - bad advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onAdvertisingDataSet(i, i2);
    }

    void onScanResponseDataSet(int i, int i2) throws Exception {
        if (DBG) {
            Log.d(TAG, "onScanResponseDataSet() advertiserId=" + i + ", status=" + i2);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onScanResponseDataSet() - bad advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onScanResponseDataSet(i, i2);
    }

    void onAdvertisingParametersUpdated(int i, int i2, int i3) throws Exception {
        if (DBG) {
            Log.d(TAG, "onAdvertisingParametersUpdated() advertiserId=" + i + ", txPower=" + i2 + ", status=" + i3);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onAdvertisingParametersUpdated() - bad advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onAdvertisingParametersUpdated(i, i2, i3);
    }

    void onPeriodicAdvertisingParametersUpdated(int i, int i2) throws Exception {
        if (DBG) {
            Log.d(TAG, "onPeriodicAdvertisingParametersUpdated() advertiserId=" + i + ", status=" + i2);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onPeriodicAdvertisingParametersUpdated() - bad advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onPeriodicAdvertisingParametersUpdated(i, i2);
    }

    void onPeriodicAdvertisingDataSet(int i, int i2) throws Exception {
        if (DBG) {
            Log.d(TAG, "onPeriodicAdvertisingDataSet() advertiserId=" + i + ", status=" + i2);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onPeriodicAdvertisingDataSet() - bad advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onPeriodicAdvertisingDataSet(i, i2);
    }

    void onPeriodicAdvertisingEnabled(int i, boolean z, int i2) throws Exception {
        if (DBG) {
            Log.d(TAG, "onPeriodicAdvertisingEnabled() advertiserId=" + i + ", status=" + i2);
        }
        Map.Entry<IBinder, AdvertiserInfo> entryFindAdvertiser = findAdvertiser(i);
        if (entryFindAdvertiser == null) {
            Log.i(TAG, "onAdvertisingSetEnable() - bad advertiserId " + i);
            return;
        }
        entryFindAdvertiser.getValue().callback.onPeriodicAdvertisingEnabled(i, z, i2);
    }
}
