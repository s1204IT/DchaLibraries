package android.bluetooth.le;

import android.app.ActivityThread;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCallbackWrapper;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BluetoothLeScanner {
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothLeScanner";
    private static final boolean VDBG = false;
    private final IBluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Map<ScanCallback, BleScanCallbackWrapper> mLeScanClients = new HashMap();

    public BluetoothLeScanner(IBluetoothManager bluetoothManager) {
        this.mBluetoothManager = bluetoothManager;
    }

    public void startScan(ScanCallback callback) {
        startScan(null, new ScanSettings.Builder().build(), callback);
    }

    public void startScan(List<ScanFilter> filters, ScanSettings settings, ScanCallback callback) {
        startScan(filters, settings, null, callback, null);
    }

    public void startScanFromSource(WorkSource workSource, ScanCallback callback) {
        startScanFromSource(null, new ScanSettings.Builder().build(), workSource, callback);
    }

    public void startScanFromSource(List<ScanFilter> filters, ScanSettings settings, WorkSource workSource, ScanCallback callback) {
        startScan(filters, settings, workSource, callback, null);
    }

    private void startScan(List<ScanFilter> filters, ScanSettings settings, WorkSource workSource, ScanCallback callback, List<List<ResultStorageDescriptor>> resultStorages) {
        IBluetoothGatt bluetoothGatt;
        Log.d(TAG, "startScan");
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }
        if (settings == null) {
            throw new IllegalArgumentException("settings is null");
        }
        synchronized (this.mLeScanClients) {
            if (this.mLeScanClients.containsKey(callback)) {
                postCallbackError(callback, 1);
                return;
            }
            try {
                bluetoothGatt = this.mBluetoothManager.getBluetoothGatt();
            } catch (RemoteException e) {
                bluetoothGatt = null;
            }
            if (bluetoothGatt == null) {
                postCallbackError(callback, 3);
                return;
            }
            if (!isSettingsConfigAllowedForScan(settings)) {
                postCallbackError(callback, 4);
                return;
            }
            if (!isHardwareResourcesAvailableForScan(settings)) {
                postCallbackError(callback, 5);
            } else if (!isSettingsAndFilterComboAllowed(settings, filters)) {
                postCallbackError(callback, 4);
            } else {
                BleScanCallbackWrapper wrapper = new BleScanCallbackWrapper(bluetoothGatt, filters, settings, workSource, callback, resultStorages);
                wrapper.startRegisteration();
            }
        }
    }

    public void stopScan(ScanCallback callback) {
        Log.d(TAG, "stopScan");
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        synchronized (this.mLeScanClients) {
            Log.i(TAG, "startRegisteration: mLeScanClients=" + this.mLeScanClients + " ,callback=" + callback);
            BleScanCallbackWrapper wrapper = this.mLeScanClients.remove(callback);
            if (wrapper == null) {
                Log.d(TAG, "could not find callback wrapper");
            } else {
                wrapper.stopLeScan();
            }
        }
    }

    public void flushPendingScanResults(ScanCallback callback) {
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null!");
        }
        synchronized (this.mLeScanClients) {
            BleScanCallbackWrapper wrapper = this.mLeScanClients.get(callback);
            if (wrapper == null) {
                return;
            }
            wrapper.flushPendingBatchResults();
        }
    }

    public void startTruncatedScan(List<TruncatedFilter> truncatedFilters, ScanSettings settings, ScanCallback callback) {
        int filterSize = truncatedFilters.size();
        List<ScanFilter> scanFilters = new ArrayList<>(filterSize);
        List<List<ResultStorageDescriptor>> scanStorages = new ArrayList<>(filterSize);
        for (TruncatedFilter filter : truncatedFilters) {
            scanFilters.add(filter.getFilter());
            scanStorages.add(filter.getStorageDescriptors());
        }
        startScan(scanFilters, settings, null, callback, scanStorages);
    }

    public void cleanup() {
        Log.d(TAG, "cleanup");
        this.mLeScanClients.clear();
    }

    private class BleScanCallbackWrapper extends BluetoothGattCallbackWrapper {
        private static final int REGISTRATION_CALLBACK_TIMEOUT_MILLIS = 2000;
        private IBluetoothGatt mBluetoothGatt;
        private int mClientIf = 0;
        private final List<ScanFilter> mFilters;
        private List<List<ResultStorageDescriptor>> mResultStorages;
        private final ScanCallback mScanCallback;
        private ScanSettings mSettings;
        private final WorkSource mWorkSource;

        public BleScanCallbackWrapper(IBluetoothGatt bluetoothGatt, List<ScanFilter> filters, ScanSettings settings, WorkSource workSource, ScanCallback scanCallback, List<List<ResultStorageDescriptor>> resultStorages) {
            this.mBluetoothGatt = bluetoothGatt;
            this.mFilters = filters;
            this.mSettings = settings;
            this.mWorkSource = workSource;
            this.mScanCallback = scanCallback;
            this.mResultStorages = resultStorages;
        }

        public void startRegisteration() {
            synchronized (this) {
                if (this.mClientIf == -1) {
                    return;
                }
                try {
                    UUID uuid = UUID.randomUUID();
                    this.mBluetoothGatt.registerClient(new ParcelUuid(uuid), this);
                    wait(2000L);
                } catch (RemoteException | InterruptedException e) {
                    Log.e(BluetoothLeScanner.TAG, "application registeration exception", e);
                    BluetoothLeScanner.this.postCallbackError(this.mScanCallback, 3);
                }
                if (this.mClientIf > 0) {
                    BluetoothLeScanner.this.mLeScanClients.put(this.mScanCallback, this);
                    Log.i(BluetoothLeScanner.TAG, "startRegisteration: mLeScanClients=" + BluetoothLeScanner.this.mLeScanClients);
                } else {
                    if (this.mClientIf == 0) {
                        this.mClientIf = -1;
                    }
                    BluetoothLeScanner.this.postCallbackError(this.mScanCallback, 2);
                }
            }
        }

        public void stopLeScan() {
            synchronized (this) {
                if (this.mClientIf <= 0) {
                    Log.e(BluetoothLeScanner.TAG, "Error state, mLeHandle: " + this.mClientIf);
                    return;
                }
                try {
                    this.mBluetoothGatt.stopScan(this.mClientIf, false);
                    this.mBluetoothGatt.unregisterClient(this.mClientIf);
                } catch (RemoteException e) {
                    Log.e(BluetoothLeScanner.TAG, "Failed to stop scan and unregister", e);
                }
                this.mClientIf = -1;
            }
        }

        void flushPendingBatchResults() {
            synchronized (this) {
                if (this.mClientIf <= 0) {
                    Log.e(BluetoothLeScanner.TAG, "Error state, mLeHandle: " + this.mClientIf);
                    return;
                }
                try {
                    this.mBluetoothGatt.flushPendingBatchResults(this.mClientIf, false);
                } catch (RemoteException e) {
                    Log.e(BluetoothLeScanner.TAG, "Failed to get pending scan results", e);
                }
            }
        }

        @Override
        public void onClientRegistered(int status, int clientIf) {
            Log.d(BluetoothLeScanner.TAG, "onClientRegistered() - status=" + status + " clientIf=" + clientIf);
            synchronized (this) {
                Log.d(BluetoothLeScanner.TAG, "mClientIf=" + this.mClientIf);
                if (status == 0) {
                    try {
                        if (this.mClientIf == -1) {
                            this.mBluetoothGatt.unregisterClient(clientIf);
                        } else {
                            this.mClientIf = clientIf;
                            this.mBluetoothGatt.startScan(this.mClientIf, false, this.mSettings, this.mFilters, this.mWorkSource, this.mResultStorages, ActivityThread.currentOpPackageName());
                        }
                    } catch (RemoteException e) {
                        Log.e(BluetoothLeScanner.TAG, "fail to start le scan: " + e);
                        this.mClientIf = -1;
                    }
                } else {
                    this.mClientIf = -1;
                }
                notifyAll();
            }
        }

        @Override
        public void onScanResult(final ScanResult scanResult) {
            synchronized (this) {
                if (this.mClientIf <= 0) {
                    return;
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        BleScanCallbackWrapper.this.mScanCallback.onScanResult(1, scanResult);
                    }
                });
            }
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    BleScanCallbackWrapper.this.mScanCallback.onBatchScanResults(results);
                }
            });
        }

        @Override
        public void onFoundOrLost(final boolean onFound, final ScanResult scanResult) {
            synchronized (this) {
                if (this.mClientIf <= 0) {
                    return;
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onFound) {
                            BleScanCallbackWrapper.this.mScanCallback.onScanResult(2, scanResult);
                        } else {
                            BleScanCallbackWrapper.this.mScanCallback.onScanResult(4, scanResult);
                        }
                    }
                });
            }
        }

        @Override
        public void onScanManagerErrorCallback(int errorCode) {
            synchronized (this) {
                if (this.mClientIf <= 0) {
                    return;
                }
                BluetoothLeScanner.this.postCallbackError(this.mScanCallback, errorCode);
            }
        }
    }

    private void postCallbackError(final ScanCallback callback, final int errorCode) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onScanFailed(errorCode);
            }
        });
    }

    private boolean isSettingsConfigAllowedForScan(ScanSettings settings) {
        if (this.mBluetoothAdapter.isOffloadedFilteringSupported()) {
            return true;
        }
        int callbackType = settings.getCallbackType();
        return callbackType == 1 && settings.getReportDelayMillis() == 0;
    }

    private boolean isSettingsAndFilterComboAllowed(ScanSettings settings, List<ScanFilter> filterList) {
        int callbackType = settings.getCallbackType();
        if ((callbackType & 6) != 0) {
            if (filterList == null) {
                return false;
            }
            for (ScanFilter filter : filterList) {
                if (filter.isAllFieldsEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean isHardwareResourcesAvailableForScan(ScanSettings settings) {
        int callbackType = settings.getCallbackType();
        if ((callbackType & 2) != 0 || (callbackType & 4) != 0) {
            if (this.mBluetoothAdapter.isOffloadedFilteringSupported()) {
                return this.mBluetoothAdapter.isHardwareTrackingFiltersAvailable();
            }
            return false;
        }
        return true;
    }
}
