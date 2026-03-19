package android.bluetooth.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PeriodicAdvertisingManager {
    private static final int SKIP_MAX = 499;
    private static final int SKIP_MIN = 0;
    private static final int SYNC_STARTING = -1;
    private static final String TAG = "PeriodicAdvertisingManager";
    private static final int TIMEOUT_MAX = 16384;
    private static final int TIMEOUT_MIN = 10;
    private final IBluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Map<PeriodicAdvertisingCallback, IPeriodicAdvertisingCallback> mCallbackWrappers = new IdentityHashMap();

    public PeriodicAdvertisingManager(IBluetoothManager iBluetoothManager) {
        this.mBluetoothManager = iBluetoothManager;
    }

    public void registerSync(ScanResult scanResult, int i, int i2, PeriodicAdvertisingCallback periodicAdvertisingCallback) {
        registerSync(scanResult, i, i2, periodicAdvertisingCallback, null);
    }

    public void registerSync(ScanResult scanResult, int i, int i2, PeriodicAdvertisingCallback periodicAdvertisingCallback, Handler handler) {
        if (periodicAdvertisingCallback == null) {
            throw new IllegalArgumentException("callback can't be null");
        }
        if (scanResult == null) {
            throw new IllegalArgumentException("scanResult can't be null");
        }
        if (scanResult.getAdvertisingSid() == 255) {
            throw new IllegalArgumentException("scanResult must contain a valid sid");
        }
        if (i < 0 || i > 499) {
            throw new IllegalArgumentException("timeout must be between 10 and 16384");
        }
        if (i2 < 10 || i2 > 16384) {
            throw new IllegalArgumentException("timeout must be between 10 and 16384");
        }
        try {
            IBluetoothGatt bluetoothGatt = this.mBluetoothManager.getBluetoothGatt();
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            IPeriodicAdvertisingCallback iPeriodicAdvertisingCallbackWrap = wrap(periodicAdvertisingCallback, handler);
            this.mCallbackWrappers.put(periodicAdvertisingCallback, iPeriodicAdvertisingCallbackWrap);
            try {
                bluetoothGatt.registerSync(scanResult, i, i2, iPeriodicAdvertisingCallbackWrap);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to register sync - ", e);
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to get Bluetooth gatt - ", e2);
            periodicAdvertisingCallback.onSyncEstablished(0, scanResult.getDevice(), scanResult.getAdvertisingSid(), i, i2, 2);
        }
    }

    public void unregisterSync(PeriodicAdvertisingCallback periodicAdvertisingCallback) {
        if (periodicAdvertisingCallback == null) {
            throw new IllegalArgumentException("callback can't be null");
        }
        try {
            IBluetoothGatt bluetoothGatt = this.mBluetoothManager.getBluetoothGatt();
            IPeriodicAdvertisingCallback iPeriodicAdvertisingCallbackRemove = this.mCallbackWrappers.remove(periodicAdvertisingCallback);
            if (iPeriodicAdvertisingCallbackRemove == null) {
                throw new IllegalArgumentException("callback was not properly registered");
            }
            try {
                bluetoothGatt.unregisterSync(iPeriodicAdvertisingCallbackRemove);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to cancel sync creation - ", e);
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to get Bluetooth gatt - ", e2);
        }
    }

    private IPeriodicAdvertisingCallback wrap(final PeriodicAdvertisingCallback periodicAdvertisingCallback, final Handler handler) {
        return new IPeriodicAdvertisingCallback.Stub() {
            @Override
            public void onSyncEstablished(final int i, final BluetoothDevice bluetoothDevice, final int i2, final int i3, final int i4, final int i5) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        periodicAdvertisingCallback.onSyncEstablished(i, bluetoothDevice, i2, i3, i4, i5);
                        if (i5 != 0) {
                            PeriodicAdvertisingManager.this.mCallbackWrappers.remove(periodicAdvertisingCallback);
                        }
                    }
                });
            }

            @Override
            public void onPeriodicAdvertisingReport(final PeriodicAdvertisingReport periodicAdvertisingReport) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        periodicAdvertisingCallback.onPeriodicAdvertisingReport(periodicAdvertisingReport);
                    }
                });
            }

            @Override
            public void onSyncLost(final int i) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        periodicAdvertisingCallback.onSyncLost(i);
                        PeriodicAdvertisingManager.this.mCallbackWrappers.remove(periodicAdvertisingCallback);
                    }
                });
            }
        };
    }
}
