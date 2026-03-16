package android.bluetooth.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCallbackWrapper;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BluetoothLeAdvertiser {
    private static final int FLAGS_FIELD_BYTES = 3;
    private static final int MANUFACTURER_SPECIFIC_DATA_LENGTH = 2;
    private static final int MAX_ADVERTISING_DATA_BYTES = 31;
    private static final int OVERHEAD_BYTES_PER_FIELD = 2;
    private static final int SERVICE_DATA_UUID_LENGTH = 2;
    private static final String TAG = "BluetoothLeAdvertiser";
    private final IBluetoothManager mBluetoothManager;
    private final Map<AdvertiseCallback, AdvertiseCallbackWrapper> mLeAdvertisers = new HashMap();
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public BluetoothLeAdvertiser(IBluetoothManager bluetoothManager) {
        this.mBluetoothManager = bluetoothManager;
    }

    public void startAdvertising(AdvertiseSettings settings, AdvertiseData advertiseData, AdvertiseCallback callback) {
        startAdvertising(settings, advertiseData, null, callback);
    }

    public void startAdvertising(AdvertiseSettings settings, AdvertiseData advertiseData, AdvertiseData scanResponse, AdvertiseCallback callback) {
        synchronized (this.mLeAdvertisers) {
            BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
            if (callback == null) {
                throw new IllegalArgumentException("callback cannot be null");
            }
            if (!this.mBluetoothAdapter.isMultipleAdvertisementSupported() && !this.mBluetoothAdapter.isPeripheralModeSupported()) {
                postStartFailure(callback, 5);
                return;
            }
            boolean isConnectable = settings.isConnectable();
            if (totalBytes(advertiseData, isConnectable) > 31 || totalBytes(scanResponse, false) > 31) {
                postStartFailure(callback, 1);
                return;
            }
            if (this.mLeAdvertisers.containsKey(callback)) {
                postStartFailure(callback, 3);
                return;
            }
            try {
                IBluetoothGatt gatt = this.mBluetoothManager.getBluetoothGatt();
                AdvertiseCallbackWrapper wrapper = new AdvertiseCallbackWrapper(callback, advertiseData, scanResponse, settings, gatt);
                wrapper.startRegisteration();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to get Bluetooth gatt - ", e);
                postStartFailure(callback, 4);
            }
        }
    }

    public void stopAdvertising(AdvertiseCallback callback) {
        synchronized (this.mLeAdvertisers) {
            BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
            if (callback == null) {
                throw new IllegalArgumentException("callback cannot be null");
            }
            AdvertiseCallbackWrapper wrapper = this.mLeAdvertisers.get(callback);
            if (wrapper != null) {
                wrapper.stopAdvertising();
            }
        }
    }

    public void cleanup() {
        this.mLeAdvertisers.clear();
    }

    private int totalBytes(AdvertiseData data, boolean isFlagsIncluded) {
        if (data == null) {
            return 0;
        }
        int size = isFlagsIncluded ? 3 : 0;
        if (data.getServiceUuids() != null) {
            int num16BitUuids = 0;
            int num32BitUuids = 0;
            int num128BitUuids = 0;
            for (ParcelUuid uuid : data.getServiceUuids()) {
                if (BluetoothUuid.is16BitUuid(uuid)) {
                    num16BitUuids++;
                } else if (BluetoothUuid.is32BitUuid(uuid)) {
                    num32BitUuids++;
                } else {
                    num128BitUuids++;
                }
            }
            if (num16BitUuids != 0) {
                size += (num16BitUuids * 2) + 2;
            }
            if (num32BitUuids != 0) {
                size += (num32BitUuids * 4) + 2;
            }
            if (num128BitUuids != 0) {
                size += (num128BitUuids * 16) + 2;
            }
        }
        Iterator<ParcelUuid> it = data.getServiceData().keySet().iterator();
        while (it.hasNext()) {
            size += byteLength(data.getServiceData().get(it.next())) + 4;
        }
        for (int i = 0; i < data.getManufacturerSpecificData().size(); i++) {
            size += byteLength(data.getManufacturerSpecificData().valueAt(i)) + 4;
        }
        if (data.getIncludeTxPowerLevel()) {
            size += 3;
        }
        if (data.getIncludeDeviceName() && this.mBluetoothAdapter.getName() != null) {
            return size + this.mBluetoothAdapter.getName().length() + 2;
        }
        return size;
    }

    private int byteLength(byte[] array) {
        if (array == null) {
            return 0;
        }
        return array.length;
    }

    private class AdvertiseCallbackWrapper extends BluetoothGattCallbackWrapper {
        private static final int LE_CALLBACK_TIMEOUT_MILLIS = 2000;
        private final AdvertiseCallback mAdvertiseCallback;
        private final AdvertiseData mAdvertisement;
        private final IBluetoothGatt mBluetoothGatt;
        private final AdvertiseData mScanResponse;
        private final AdvertiseSettings mSettings;
        private boolean mIsAdvertising = false;
        private int mClientIf = 0;

        public AdvertiseCallbackWrapper(AdvertiseCallback advertiseCallback, AdvertiseData advertiseData, AdvertiseData scanResponse, AdvertiseSettings settings, IBluetoothGatt bluetoothGatt) {
            this.mAdvertiseCallback = advertiseCallback;
            this.mAdvertisement = advertiseData;
            this.mScanResponse = scanResponse;
            this.mSettings = settings;
            this.mBluetoothGatt = bluetoothGatt;
        }

        public void startRegisteration() {
            Exception e;
            synchronized (this) {
                if (this.mClientIf != -1) {
                    try {
                        UUID uuid = UUID.randomUUID();
                        this.mBluetoothGatt.registerClient(new ParcelUuid(uuid), this);
                        wait(2000L);
                    } catch (RemoteException e2) {
                        e = e2;
                        Log.e(BluetoothLeAdvertiser.TAG, "Failed to start registeration", e);
                    } catch (InterruptedException e3) {
                        e = e3;
                        Log.e(BluetoothLeAdvertiser.TAG, "Failed to start registeration", e);
                    }
                    if (this.mClientIf > 0 && this.mIsAdvertising) {
                        BluetoothLeAdvertiser.this.mLeAdvertisers.put(this.mAdvertiseCallback, this);
                    } else if (this.mClientIf <= 0) {
                        BluetoothLeAdvertiser.this.postStartFailure(this.mAdvertiseCallback, 4);
                    } else {
                        try {
                            this.mBluetoothGatt.unregisterClient(this.mClientIf);
                            this.mClientIf = -1;
                        } catch (RemoteException e4) {
                            Log.e(BluetoothLeAdvertiser.TAG, "remote exception when unregistering", e4);
                        }
                    }
                }
            }
        }

        public void stopAdvertising() {
            Exception e;
            synchronized (this) {
                try {
                    this.mBluetoothGatt.stopMultiAdvertising(this.mClientIf);
                    wait(2000L);
                } catch (RemoteException e2) {
                    e = e2;
                    Log.e(BluetoothLeAdvertiser.TAG, "Failed to stop advertising", e);
                } catch (InterruptedException e3) {
                    e = e3;
                    Log.e(BluetoothLeAdvertiser.TAG, "Failed to stop advertising", e);
                }
                if (BluetoothLeAdvertiser.this.mLeAdvertisers.containsKey(this.mAdvertiseCallback)) {
                    BluetoothLeAdvertiser.this.mLeAdvertisers.remove(this.mAdvertiseCallback);
                }
            }
        }

        @Override
        public void onClientRegistered(int status, int clientIf) {
            Log.d(BluetoothLeAdvertiser.TAG, "onClientRegistered() - status=" + status + " clientIf=" + clientIf);
            synchronized (this) {
                if (status == 0) {
                    this.mClientIf = clientIf;
                    try {
                        this.mBluetoothGatt.startMultiAdvertising(this.mClientIf, this.mAdvertisement, this.mScanResponse, this.mSettings);
                        return;
                    } catch (RemoteException e) {
                        Log.e(BluetoothLeAdvertiser.TAG, "failed to start advertising", e);
                    }
                }
                this.mClientIf = -1;
                notifyAll();
            }
        }

        @Override
        public void onMultiAdvertiseCallback(int status, boolean isStart, AdvertiseSettings settings) {
            synchronized (this) {
                if (isStart) {
                    if (status != 0) {
                        BluetoothLeAdvertiser.this.postStartFailure(this.mAdvertiseCallback, status);
                    } else {
                        this.mIsAdvertising = true;
                        BluetoothLeAdvertiser.this.postStartSuccess(this.mAdvertiseCallback, settings);
                    }
                } else {
                    try {
                        this.mBluetoothGatt.unregisterClient(this.mClientIf);
                        this.mClientIf = -1;
                        this.mIsAdvertising = false;
                        BluetoothLeAdvertiser.this.mLeAdvertisers.remove(this.mAdvertiseCallback);
                    } catch (RemoteException e) {
                        Log.e(BluetoothLeAdvertiser.TAG, "remote exception when unregistering", e);
                    }
                }
                notifyAll();
            }
        }
    }

    private void postStartFailure(final AdvertiseCallback callback, final int error) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onStartFailure(error);
            }
        });
    }

    private void postStartSuccess(final AdvertiseCallback callback, final AdvertiseSettings settings) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onStartSuccess(settings);
            }
        });
    }
}
