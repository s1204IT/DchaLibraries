package android.bluetooth;

import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.PeriodicAdvertisingManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SynchronousResultReceiver;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Pair;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BluetoothAdapter {
    public static final String ACTION_BLE_ACL_CONNECTED = "android.bluetooth.adapter.action.BLE_ACL_CONNECTED";
    public static final String ACTION_BLE_ACL_DISCONNECTED = "android.bluetooth.adapter.action.BLE_ACL_DISCONNECTED";

    @SystemApi
    public static final String ACTION_BLE_STATE_CHANGED = "android.bluetooth.adapter.action.BLE_STATE_CHANGED";
    public static final String ACTION_BLUETOOTH_ADDRESS_CHANGED = "android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED";
    public static final String ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";
    public static final String ACTION_LOCAL_NAME_CHANGED = "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED";

    @SystemApi
    public static final String ACTION_REQUEST_BLE_SCAN_ALWAYS_AVAILABLE = "android.bluetooth.adapter.action.REQUEST_BLE_SCAN_ALWAYS_AVAILABLE";
    public static final String ACTION_REQUEST_DISABLE = "android.bluetooth.adapter.action.REQUEST_DISABLE";
    public static final String ACTION_REQUEST_DISCOVERABLE = "android.bluetooth.adapter.action.REQUEST_DISCOVERABLE";
    public static final String ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";
    public static final String ACTION_SCAN_MODE_CHANGED = "android.bluetooth.adapter.action.SCAN_MODE_CHANGED";
    public static final String ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
    private static final int ADDRESS_LENGTH = 17;
    public static final String BLUETOOTH_MANAGER_SERVICE = "bluetooth_manager";
    public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    public static final int ERROR = Integer.MIN_VALUE;
    public static final String EXTRA_BLUETOOTH_ADDRESS = "android.bluetooth.adapter.extra.BLUETOOTH_ADDRESS";
    public static final String EXTRA_CONNECTION_STATE = "android.bluetooth.adapter.extra.CONNECTION_STATE";
    public static final String EXTRA_DISCOVERABLE_DURATION = "android.bluetooth.adapter.extra.DISCOVERABLE_DURATION";
    public static final String EXTRA_LOCAL_NAME = "android.bluetooth.adapter.extra.LOCAL_NAME";
    public static final String EXTRA_PREVIOUS_CONNECTION_STATE = "android.bluetooth.adapter.extra.PREVIOUS_CONNECTION_STATE";
    public static final String EXTRA_PREVIOUS_SCAN_MODE = "android.bluetooth.adapter.extra.PREVIOUS_SCAN_MODE";
    public static final String EXTRA_PREVIOUS_STATE = "android.bluetooth.adapter.extra.PREVIOUS_STATE";
    public static final String EXTRA_SCAN_MODE = "android.bluetooth.adapter.extra.SCAN_MODE";
    public static final String EXTRA_STATE = "android.bluetooth.adapter.extra.STATE";
    public static final int SCAN_MODE_CONNECTABLE = 21;
    public static final int SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23;
    public static final int SCAN_MODE_NONE = 20;
    public static final int SOCKET_CHANNEL_AUTO_STATIC_NO_SDP = -2;
    public static final int STATE_BLE_ON = 15;
    public static final int STATE_BLE_TURNING_OFF = 16;
    public static final int STATE_BLE_TURNING_ON = 14;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_DISCONNECTING = 3;
    public static final int STATE_OFF = 10;
    public static final int STATE_ON = 12;
    public static final int STATE_TURNING_OFF = 13;
    public static final int STATE_TURNING_ON = 11;
    private static final String TAG = "BluetoothAdapter";
    private static BluetoothAdapter sAdapter;
    private static BluetoothLeAdvertiser sBluetoothLeAdvertiser;
    private static BluetoothLeScanner sBluetoothLeScanner;
    private static PeriodicAdvertisingManager sPeriodicAdvertisingManager;
    private final Map<LeScanCallback, ScanCallback> mLeScanClients;
    private final IBluetoothManager mManagerService;
    private IBluetooth mService;
    private final IBinder mToken;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final boolean VDBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    public static final UUID LE_PSM_CHARACTERISTIC_UUID = UUID.fromString("2d410339-82b6-42aa-b34e-e2e01df8cc1a");
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();
    private final Object mLock = new Object();
    private final IBluetoothManagerCallback mManagerCallback = new IBluetoothManagerCallback.Stub() {
        @Override
        public void onBluetoothServiceUp(IBluetooth iBluetooth) {
            if (BluetoothAdapter.DBG) {
                Log.d(BluetoothAdapter.TAG, "onBluetoothServiceUp: " + iBluetooth);
            }
            BluetoothAdapter.this.mServiceLock.writeLock().lock();
            BluetoothAdapter.this.mService = iBluetooth;
            BluetoothAdapter.this.mServiceLock.writeLock().unlock();
            synchronized (BluetoothAdapter.this.mProxyServiceStateCallbacks) {
                for (IBluetoothManagerCallback iBluetoothManagerCallback : BluetoothAdapter.this.mProxyServiceStateCallbacks) {
                    if (iBluetoothManagerCallback != null) {
                        try {
                            iBluetoothManagerCallback.onBluetoothServiceUp(iBluetooth);
                        } catch (Exception e) {
                            Log.e(BluetoothAdapter.TAG, "", e);
                        }
                    } else {
                        Log.d(BluetoothAdapter.TAG, "onBluetoothServiceUp: cb is null!");
                    }
                }
            }
        }

        @Override
        public void onBluetoothServiceDown() {
            if (BluetoothAdapter.DBG) {
                Log.d(BluetoothAdapter.TAG, "onBluetoothServiceDown: " + BluetoothAdapter.this.mService);
            }
            try {
                BluetoothAdapter.this.mServiceLock.writeLock().lock();
                BluetoothAdapter.this.mService = null;
                if (BluetoothAdapter.this.mLeScanClients != null) {
                    BluetoothAdapter.this.mLeScanClients.clear();
                }
                if (BluetoothAdapter.sBluetoothLeAdvertiser != null) {
                    BluetoothAdapter.sBluetoothLeAdvertiser.cleanup();
                }
                if (BluetoothAdapter.sBluetoothLeScanner != null) {
                    BluetoothAdapter.sBluetoothLeScanner.cleanup();
                }
                BluetoothAdapter.this.mServiceLock.writeLock().unlock();
                synchronized (BluetoothAdapter.this.mProxyServiceStateCallbacks) {
                    for (IBluetoothManagerCallback iBluetoothManagerCallback : BluetoothAdapter.this.mProxyServiceStateCallbacks) {
                        if (iBluetoothManagerCallback != null) {
                            try {
                                iBluetoothManagerCallback.onBluetoothServiceDown();
                            } catch (Exception e) {
                                Log.e(BluetoothAdapter.TAG, "", e);
                            }
                        } else {
                            Log.d(BluetoothAdapter.TAG, "onBluetoothServiceDown: cb is null!");
                        }
                    }
                }
            } catch (Throwable th) {
                BluetoothAdapter.this.mServiceLock.writeLock().unlock();
                throw th;
            }
        }

        @Override
        public void onBrEdrDown() {
            if (BluetoothAdapter.VDBG) {
                Log.i(BluetoothAdapter.TAG, "onBrEdrDown: " + BluetoothAdapter.this.mService);
            }
        }
    };
    private final ArrayList<IBluetoothManagerCallback> mProxyServiceStateCallbacks = new ArrayList<>();

    @Retention(RetentionPolicy.SOURCE)
    public @interface AdapterState {
    }

    public interface BluetoothStateChangeCallback {
        void onBluetoothStateChange(boolean z);
    }

    public interface LeScanCallback {
        void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bArr);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanMode {
    }

    public static String nameForState(int i) {
        switch (i) {
            case 10:
                return "OFF";
            case 11:
                return "TURNING_ON";
            case 12:
                return "ON";
            case 13:
                return "TURNING_OFF";
            case 14:
                return "BLE_TURNING_ON";
            case 15:
                return "BLE_ON";
            case 16:
                return "BLE_TURNING_OFF";
            default:
                return "?!?!? (" + i + ")";
        }
    }

    public static synchronized BluetoothAdapter getDefaultAdapter() {
        if (sAdapter == null) {
            IBinder service = ServiceManager.getService(BLUETOOTH_MANAGER_SERVICE);
            if (service != null) {
                sAdapter = new BluetoothAdapter(IBluetoothManager.Stub.asInterface(service));
            } else {
                Log.e(TAG, "Bluetooth binder is null");
            }
        }
        return sAdapter;
    }

    BluetoothAdapter(IBluetoothManager iBluetoothManager) {
        try {
            if (iBluetoothManager == null) {
                throw new IllegalArgumentException("bluetooth manager service is null");
            }
            try {
                this.mServiceLock.writeLock().lock();
                this.mService = iBluetoothManager.registerAdapter(this.mManagerCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            this.mServiceLock.writeLock().unlock();
            this.mManagerService = iBluetoothManager;
            this.mLeScanClients = new HashMap();
            this.mToken = new Binder();
        } catch (Throwable th) {
            this.mServiceLock.writeLock().unlock();
            throw th;
        }
    }

    public BluetoothDevice getRemoteDevice(String str) {
        return new BluetoothDevice(str);
    }

    public BluetoothDevice getRemoteDevice(byte[] bArr) {
        if (bArr == null || bArr.length != 6) {
            throw new IllegalArgumentException("Bluetooth address must have 6 bytes");
        }
        return new BluetoothDevice(String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", Byte.valueOf(bArr[0]), Byte.valueOf(bArr[1]), Byte.valueOf(bArr[2]), Byte.valueOf(bArr[3]), Byte.valueOf(bArr[4]), Byte.valueOf(bArr[5])));
    }

    public BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
        if (!getLeAccess()) {
            return null;
        }
        synchronized (this.mLock) {
            if (sBluetoothLeAdvertiser == null) {
                sBluetoothLeAdvertiser = new BluetoothLeAdvertiser(this.mManagerService);
            }
        }
        return sBluetoothLeAdvertiser;
    }

    public PeriodicAdvertisingManager getPeriodicAdvertisingManager() {
        if (!getLeAccess() || !isLePeriodicAdvertisingSupported()) {
            return null;
        }
        synchronized (this.mLock) {
            if (sPeriodicAdvertisingManager == null) {
                sPeriodicAdvertisingManager = new PeriodicAdvertisingManager(this.mManagerService);
            }
        }
        return sPeriodicAdvertisingManager;
    }

    public BluetoothLeScanner getBluetoothLeScanner() {
        if (!getLeAccess()) {
            return null;
        }
        synchronized (this.mLock) {
            if (sBluetoothLeScanner == null) {
                sBluetoothLeScanner = new BluetoothLeScanner(this.mManagerService);
            }
        }
        return sBluetoothLeScanner;
    }

    public boolean isEnabled() {
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isEnabled();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    @SystemApi
    public boolean isLeEnabled() throws RemoteException {
        int leState = getLeState();
        if (DBG) {
            Log.d(TAG, "isLeEnabled(): " + nameForState(leState));
        }
        return leState == 12 || leState == 15;
    }

    @SystemApi
    public boolean disableBLE() throws RemoteException {
        if (!isBleScanAlwaysAvailable()) {
            return false;
        }
        int leState = getLeState();
        if (leState == 12 || leState == 15) {
            String strCurrentPackageName = ActivityThread.currentPackageName();
            if (DBG) {
                Log.d(TAG, "disableBLE(): de-registering " + strCurrentPackageName);
            }
            try {
                this.mManagerService.updateBleAppCount(this.mToken, false, strCurrentPackageName);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return true;
            }
        }
        if (DBG) {
            Log.d(TAG, "disableBLE(): Already disabled");
        }
        return false;
    }

    @SystemApi
    public boolean enableBLE() {
        if (!isBleScanAlwaysAvailable()) {
            return false;
        }
        try {
            String strCurrentPackageName = ActivityThread.currentPackageName();
            this.mManagerService.updateBleAppCount(this.mToken, true, strCurrentPackageName);
            if (isLeEnabled()) {
                if (DBG) {
                    Log.d(TAG, "enableBLE(): Bluetooth already enabled");
                }
                return true;
            }
            if (DBG) {
                Log.d(TAG, "enableBLE(): Calling enable");
            }
            return this.mManagerService.enable(strCurrentPackageName);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public int getState() {
        int state;
        int i = 10;
        try {
            try {
                this.mServiceLock.readLock().lock();
                state = this.mService != null ? this.mService.getState() : 10;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mServiceLock.readLock().unlock();
                state = 10;
            }
            if (state != 15 && state != 14 && state != 16) {
                i = state;
            } else if (VDBG) {
                Log.d(TAG, "Consider " + nameForState(state) + " state as OFF");
            }
            if (VDBG) {
                Log.d(TAG, "" + hashCode() + ": getState(). Returning " + nameForState(i));
            }
            return i;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getLeState() throws RemoteException {
        int state = 10;
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    state = this.mService.getState();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            this.mServiceLock.readLock().unlock();
            if (VDBG) {
                Log.d(TAG, "getLeState() returning " + nameForState(state));
            }
            return state;
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
            throw th;
        }
    }

    boolean getLeAccess() {
        return getLeState() == 12 || getLeState() == 15;
    }

    public boolean enable() {
        if (isEnabled()) {
            if (DBG) {
                Log.d(TAG, "enable(): BT already enabled!");
                return true;
            }
            return true;
        }
        try {
            return this.mManagerService.enable(ActivityThread.currentPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean disable() {
        try {
            return this.mManagerService.disable(ActivityThread.currentPackageName(), true);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean disable(boolean z) {
        try {
            return this.mManagerService.disable(ActivityThread.currentPackageName(), z);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public String getAddress() {
        try {
            return this.mManagerService.getAddress();
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return null;
        }
    }

    public String getName() {
        try {
            return this.mManagerService.getName();
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return null;
        }
    }

    public boolean factoryReset() {
        try {
            try {
                this.mServiceLock.readLock().lock();
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            if (this.mService != null) {
                disable();
                return this.mService.factoryReset();
            }
            SystemProperties.set("persist.bluetooth.factoryreset", "true");
            this.mServiceLock.readLock().unlock();
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public ParcelUuid[] getUuids() {
        try {
            if (getState() != 12) {
                return null;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getUuids();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return null;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean setName(String str) {
        try {
            if (getState() != 12) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.setName(str);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public BluetoothClass getBluetoothClass() {
        try {
            if (getState() != 12) {
                return null;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getBluetoothClass();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return null;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean setBluetoothClass(BluetoothClass bluetoothClass) {
        try {
            if (getState() != 12) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.setBluetoothClass(bluetoothClass);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getScanMode() {
        try {
            if (getState() != 12) {
                return 20;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getScanMode();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return 20;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean setScanMode(int i, int i2) {
        try {
            if (getState() != 12) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.setScanMode(i, i2);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean setScanMode(int i) {
        if (getState() != 12) {
            return false;
        }
        return setScanMode(i, getDiscoverableTimeout());
    }

    public int getDiscoverableTimeout() {
        try {
            if (getState() != 12) {
                return -1;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getDiscoverableTimeout();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return -1;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public void setDiscoverableTimeout(int i) {
        if (getState() != 12) {
            return;
        }
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    this.mService.setDiscoverableTimeout(i);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public long getDiscoveryEndMillis() {
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getDiscoveryEndMillis();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            this.mServiceLock.readLock().unlock();
            return -1L;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean startDiscovery() {
        try {
            if (getState() != 12) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.startDiscovery();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean cancelDiscovery() {
        try {
            if (getState() != 12) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.cancelDiscovery();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isDiscovering() {
        try {
            if (getState() != 12) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isDiscovering();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isMultipleAdvertisementSupported() {
        try {
            if (getState() != 12) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isMultiAdvertisementSupported();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get isMultipleAdvertisementSupported, error: ", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    @SystemApi
    public boolean isBleScanAlwaysAvailable() {
        try {
            return this.mManagerService.isBleScanAlwaysAvailable();
        } catch (RemoteException e) {
            Log.e(TAG, "remote expection when calling isBleScanAlwaysAvailable", e);
            return false;
        }
    }

    public boolean isOffloadedFilteringSupported() {
        try {
            if (!getLeAccess()) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isOffloadedFilteringSupported();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get isOffloadedFilteringSupported, error: ", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isOffloadedScanBatchingSupported() {
        try {
            if (!getLeAccess()) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isOffloadedScanBatchingSupported();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get isOffloadedScanBatchingSupported, error: ", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isLe2MPhySupported() {
        try {
            if (!getLeAccess()) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isLe2MPhySupported();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get isExtendedAdvertisingSupported, error: ", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isLeCodedPhySupported() {
        try {
            if (!getLeAccess()) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isLeCodedPhySupported();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get isLeCodedPhySupported, error: ", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isLeExtendedAdvertisingSupported() {
        try {
            if (!getLeAccess()) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isLeExtendedAdvertisingSupported();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get isLeExtendedAdvertisingSupported, error: ", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isLePeriodicAdvertisingSupported() {
        try {
            if (!getLeAccess()) {
                return false;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.isLePeriodicAdvertisingSupported();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get isLePeriodicAdvertisingSupported, error: ", e);
            }
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getLeMaximumAdvertisingDataLength() {
        try {
            if (!getLeAccess()) {
                return 0;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getLeMaximumAdvertisingDataLength();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get getLeMaximumAdvertisingDataLength, error: ", e);
            }
            return 0;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getMaxConnectedAudioDevices() {
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getMaxConnectedAudioDevices();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failed to get getMaxConnectedAudioDevices, error: ", e);
            }
            this.mServiceLock.readLock().unlock();
            return 1;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isHardwareTrackingFiltersAvailable() {
        if (!getLeAccess()) {
            return false;
        }
        try {
            IBluetoothGatt bluetoothGatt = this.mManagerService.getBluetoothGatt();
            if (bluetoothGatt == null) {
                return false;
            }
            return bluetoothGatt.numHwTrackFiltersAvailable() != 0;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    @Deprecated
    public BluetoothActivityEnergyInfo getControllerActivityEnergyInfo(int i) {
        SynchronousResultReceiver synchronousResultReceiver = new SynchronousResultReceiver();
        requestControllerActivityEnergyInfo(synchronousResultReceiver);
        try {
            SynchronousResultReceiver.Result resultAwaitResult = synchronousResultReceiver.awaitResult(1000L);
            if (resultAwaitResult.bundle != null) {
                return (BluetoothActivityEnergyInfo) resultAwaitResult.bundle.getParcelable("controller_activity");
            }
            return null;
        } catch (TimeoutException e) {
            Log.e(TAG, "getControllerActivityEnergyInfo timed out");
            return null;
        }
    }

    public void requestControllerActivityEnergyInfo(ResultReceiver resultReceiver) {
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    this.mService.requestActivityInfo(resultReceiver);
                    resultReceiver = null;
                }
                this.mServiceLock.readLock().unlock();
                if (resultReceiver == null) {
                    return;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getControllerActivityEnergyInfoCallback: " + e);
                this.mServiceLock.readLock().unlock();
                if (resultReceiver == null) {
                    return;
                }
            }
            resultReceiver.send(0, null);
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
            if (resultReceiver != null) {
                resultReceiver.send(0, null);
            }
            throw th;
        }
    }

    public Set<BluetoothDevice> getBondedDevices() {
        try {
            if (getState() != 12) {
                return toDeviceSet(new BluetoothDevice[0]);
            }
            try {
                this.mServiceLock.readLock().lock();
                return this.mService != null ? toDeviceSet(this.mService.getBondedDevices()) : toDeviceSet(new BluetoothDevice[0]);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mServiceLock.readLock().unlock();
                return null;
            }
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public List<Integer> getSupportedProfiles() {
        ArrayList arrayList = new ArrayList();
        try {
            synchronized (this.mManagerCallback) {
                if (this.mService != null) {
                    long supportedProfiles = this.mService.getSupportedProfiles();
                    for (int i = 0; i <= 22; i++) {
                        if ((((long) (1 << i)) & supportedProfiles) != 0) {
                            arrayList.add(Integer.valueOf(i));
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getSupportedProfiles:", e);
        }
        return arrayList;
    }

    public int getConnectionState() {
        try {
            if (getState() != 12) {
                return 0;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getAdapterConnectionState();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getConnectionState:", e);
            }
            return 0;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getProfileConnectionState(int i) {
        try {
            if (getState() != 12) {
                return 0;
            }
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    return this.mService.getProfileConnectionState(i);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getProfileConnectionState:", e);
            }
            return 0;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public BluetoothServerSocket listenUsingRfcommOn(int i) throws IOException {
        return listenUsingRfcommOn(i, false, false);
    }

    public BluetoothServerSocket listenUsingRfcommOn(int i, boolean z, boolean z2) throws IOException {
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(1, true, true, i, z, z2);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (i == -2) {
            bluetoothServerSocket.setChannel(bluetoothServerSocket.mSocket.getPort());
        }
        if (iBindListen != 0) {
            throw new IOException("Error: " + iBindListen);
        }
        return bluetoothServerSocket;
    }

    public BluetoothServerSocket listenUsingRfcommWithServiceRecord(String str, UUID uuid) throws IOException {
        return createNewRfcommSocketAndRecord(str, uuid, true, true);
    }

    public BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String str, UUID uuid) throws IOException {
        return createNewRfcommSocketAndRecord(str, uuid, false, false);
    }

    public BluetoothServerSocket listenUsingEncryptedRfcommWithServiceRecord(String str, UUID uuid) throws IOException {
        return createNewRfcommSocketAndRecord(str, uuid, false, true);
    }

    private BluetoothServerSocket createNewRfcommSocketAndRecord(String str, UUID uuid, boolean z, boolean z2) throws IOException {
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(1, z, z2, new ParcelUuid(uuid));
        bluetoothServerSocket.setServiceName(str);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (iBindListen != 0) {
            throw new IOException("Error: " + iBindListen);
        }
        return bluetoothServerSocket;
    }

    public BluetoothServerSocket listenUsingInsecureRfcommOn(int i) throws IOException {
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(1, false, false, i);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (i == -2) {
            bluetoothServerSocket.setChannel(bluetoothServerSocket.mSocket.getPort());
        }
        if (iBindListen != 0) {
            throw new IOException("Error: " + iBindListen);
        }
        return bluetoothServerSocket;
    }

    public BluetoothServerSocket listenUsingEncryptedRfcommOn(int i) throws IOException {
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(1, false, true, i);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (i == -2) {
            bluetoothServerSocket.setChannel(bluetoothServerSocket.mSocket.getPort());
        }
        if (iBindListen < 0) {
            throw new IOException("Error: " + iBindListen);
        }
        return bluetoothServerSocket;
    }

    public static BluetoothServerSocket listenUsingScoOn() throws IOException {
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(2, false, false, -1);
        bluetoothServerSocket.mSocket.bindListen();
        return bluetoothServerSocket;
    }

    public BluetoothServerSocket listenUsingL2capOn(int i, boolean z, boolean z2) throws IOException {
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(3, true, true, i, z, z2);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (i == -2) {
            int port = bluetoothServerSocket.mSocket.getPort();
            if (DBG) {
                Log.d(TAG, "listenUsingL2capOn: set assigned channel to " + port);
            }
            bluetoothServerSocket.setChannel(port);
        }
        if (iBindListen != 0) {
            throw new IOException("Error: " + iBindListen);
        }
        return bluetoothServerSocket;
    }

    public BluetoothServerSocket listenUsingL2capOn(int i) throws IOException {
        return listenUsingL2capOn(i, false, false);
    }

    public BluetoothServerSocket listenUsingInsecureL2capOn(int i) throws IOException {
        Log.d(TAG, "listenUsingInsecureL2capOn: port=" + i);
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(3, false, false, i, false, false);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (i == -2) {
            int port = bluetoothServerSocket.mSocket.getPort();
            if (DBG) {
                Log.d(TAG, "listenUsingInsecureL2capOn: set assigned channel to " + port);
            }
            bluetoothServerSocket.setChannel(port);
        }
        if (iBindListen != 0) {
            throw new IOException("Error: " + iBindListen);
        }
        return bluetoothServerSocket;
    }

    public Pair<byte[], byte[]> readOutOfBandData() {
        return null;
    }

    public boolean getProfileProxy(Context context, BluetoothProfile.ServiceListener serviceListener, int i) {
        if (context == null || serviceListener == null) {
            return false;
        }
        if (i == 1) {
            new BluetoothHeadset(context, serviceListener);
            return true;
        }
        if (i == 2) {
            new BluetoothA2dp(context, serviceListener);
            return true;
        }
        if (i == 11) {
            new BluetoothA2dpSink(context, serviceListener);
            return true;
        }
        if (i == 12) {
            new BluetoothAvrcpController(context, serviceListener);
            return true;
        }
        if (i == 4) {
            new BluetoothHidHost(context, serviceListener);
            return true;
        }
        if (i == 5) {
            new BluetoothPan(context, serviceListener);
            return true;
        }
        if (i == 3) {
            new BluetoothHealth(context, serviceListener);
            return true;
        }
        if (i == 9) {
            new BluetoothMap(context, serviceListener);
            return true;
        }
        if (i == 16) {
            new BluetoothHeadsetClient(context, serviceListener);
            return true;
        }
        if (i == 10) {
            new BluetoothSap(context, serviceListener);
            return true;
        }
        if (i == 17) {
            new BluetoothPbapClient(context, serviceListener);
            return true;
        }
        if (i == 18) {
            new BluetoothMapClient(context, serviceListener);
            return true;
        }
        if (i == 19) {
            new BluetoothHidDevice(context, serviceListener);
            return true;
        }
        if (i != 21) {
            return false;
        }
        new BluetoothHearingAid(context, serviceListener);
        return true;
    }

    public void closeProfileProxy(int i, BluetoothProfile bluetoothProfile) {
        if (bluetoothProfile == null) {
        }
        switch (i) {
            case 1:
                ((BluetoothHeadset) bluetoothProfile).close();
                break;
            case 2:
                ((BluetoothA2dp) bluetoothProfile).close();
                break;
            case 3:
                ((BluetoothHealth) bluetoothProfile).close();
                break;
            case 4:
                ((BluetoothHidHost) bluetoothProfile).close();
                break;
            case 5:
                ((BluetoothPan) bluetoothProfile).close();
                break;
            case 7:
                ((BluetoothGatt) bluetoothProfile).close();
                break;
            case 8:
                ((BluetoothGattServer) bluetoothProfile).close();
                break;
            case 9:
                ((BluetoothMap) bluetoothProfile).close();
                break;
            case 10:
                ((BluetoothSap) bluetoothProfile).close();
                break;
            case 11:
                ((BluetoothA2dpSink) bluetoothProfile).close();
                break;
            case 12:
                ((BluetoothAvrcpController) bluetoothProfile).close();
                break;
            case 16:
                ((BluetoothHeadsetClient) bluetoothProfile).close();
                break;
            case 17:
                ((BluetoothPbapClient) bluetoothProfile).close();
                break;
            case 18:
                ((BluetoothMapClient) bluetoothProfile).close();
                break;
            case 19:
                ((BluetoothHidDevice) bluetoothProfile).close();
                break;
            case 21:
                ((BluetoothHearingAid) bluetoothProfile).close();
                break;
        }
    }

    @SystemApi
    public boolean enableNoAutoConnect() {
        if (isEnabled()) {
            if (DBG) {
                Log.d(TAG, "enableNoAutoConnect(): BT already enabled!");
                return true;
            }
            return true;
        }
        try {
            return this.mManagerService.enableNoAutoConnect(ActivityThread.currentPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean changeApplicationBluetoothState(boolean z, BluetoothStateChangeCallback bluetoothStateChangeCallback) {
        return false;
    }

    public class StateChangeCallbackWrapper extends IBluetoothStateChangeCallback.Stub {
        private BluetoothStateChangeCallback mCallback;

        StateChangeCallbackWrapper(BluetoothStateChangeCallback bluetoothStateChangeCallback) {
            this.mCallback = bluetoothStateChangeCallback;
        }

        @Override
        public void onBluetoothStateChange(boolean z) {
            this.mCallback.onBluetoothStateChange(z);
        }
    }

    private Set<BluetoothDevice> toDeviceSet(BluetoothDevice[] bluetoothDeviceArr) {
        return Collections.unmodifiableSet(new HashSet(Arrays.asList(bluetoothDeviceArr)));
    }

    protected void finalize() throws Throwable {
        try {
            try {
                this.mManagerService.unregisterAdapter(this.mManagerCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        } finally {
            super.finalize();
        }
    }

    public static boolean checkBluetoothAddress(String str) {
        if (str == null || str.length() != 17) {
            return false;
        }
        for (int i = 0; i < 17; i++) {
            char cCharAt = str.charAt(i);
            switch (i % 3) {
                case 0:
                case 1:
                    if ((cCharAt < '0' || cCharAt > '9') && (cCharAt < 'A' || cCharAt > 'F')) {
                        return false;
                    }
                    break;
                    break;
                case 2:
                    if (cCharAt != ':') {
                        return false;
                    }
                    break;
                    break;
            }
        }
        return true;
    }

    IBluetoothManager getBluetoothManager() {
        return this.mManagerService;
    }

    IBluetooth getBluetoothService(IBluetoothManagerCallback iBluetoothManagerCallback) {
        synchronized (this.mProxyServiceStateCallbacks) {
            try {
                if (iBluetoothManagerCallback == null) {
                    Log.w(TAG, "getBluetoothService() called with no BluetoothManagerCallback");
                } else if (!this.mProxyServiceStateCallbacks.contains(iBluetoothManagerCallback)) {
                    this.mProxyServiceStateCallbacks.add(iBluetoothManagerCallback);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return this.mService;
    }

    void removeServiceStateCallback(IBluetoothManagerCallback iBluetoothManagerCallback) {
        synchronized (this.mProxyServiceStateCallbacks) {
            this.mProxyServiceStateCallbacks.remove(iBluetoothManagerCallback);
        }
    }

    @Deprecated
    public boolean startLeScan(LeScanCallback leScanCallback) {
        return startLeScan(null, leScanCallback);
    }

    @Deprecated
    public boolean startLeScan(final UUID[] uuidArr, final LeScanCallback leScanCallback) {
        if (DBG) {
            Log.d(TAG, "startLeScan(): " + Arrays.toString(uuidArr));
        }
        if (leScanCallback == null) {
            if (DBG) {
                Log.e(TAG, "startLeScan: null callback");
            }
            return false;
        }
        BluetoothLeScanner bluetoothLeScanner = getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            if (DBG) {
                Log.e(TAG, "startLeScan: cannot get BluetoothLeScanner");
            }
            return false;
        }
        synchronized (this.mLeScanClients) {
            if (this.mLeScanClients.containsKey(leScanCallback)) {
                if (DBG) {
                    Log.e(TAG, "LE Scan has already started");
                }
                return false;
            }
            try {
                if (this.mManagerService.getBluetoothGatt() == null) {
                    return false;
                }
                ScanCallback scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int i, ScanResult scanResult) {
                        if (i != 1) {
                            Log.e(BluetoothAdapter.TAG, "LE Scan has already started");
                            return;
                        }
                        ScanRecord scanRecord = scanResult.getScanRecord();
                        if (scanRecord == null) {
                            return;
                        }
                        if (uuidArr != null) {
                            ArrayList arrayList = new ArrayList();
                            for (UUID uuid : uuidArr) {
                                arrayList.add(new ParcelUuid(uuid));
                            }
                            List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
                            if (serviceUuids == null || !serviceUuids.containsAll(arrayList)) {
                                if (BluetoothAdapter.DBG) {
                                    Log.d(BluetoothAdapter.TAG, "uuids does not match");
                                    return;
                                }
                                return;
                            }
                        }
                        leScanCallback.onLeScan(scanResult.getDevice(), scanResult.getRssi(), scanRecord.getBytes());
                    }
                };
                ScanSettings scanSettingsBuild = new ScanSettings.Builder().setCallbackType(1).setScanMode(2).build();
                ArrayList arrayList = new ArrayList();
                if (uuidArr != null && uuidArr.length > 0) {
                    arrayList.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuidArr[0])).build());
                }
                bluetoothLeScanner.startScan(arrayList, scanSettingsBuild, scanCallback);
                this.mLeScanClients.put(leScanCallback, scanCallback);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }
    }

    @Deprecated
    public void stopLeScan(LeScanCallback leScanCallback) {
        if (DBG) {
            Log.d(TAG, "stopLeScan()");
        }
        BluetoothLeScanner bluetoothLeScanner = getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            return;
        }
        synchronized (this.mLeScanClients) {
            ScanCallback scanCallbackRemove = this.mLeScanClients.remove(leScanCallback);
            if (scanCallbackRemove == null) {
                if (DBG) {
                    Log.d(TAG, "scan not started yet");
                }
            } else {
                bluetoothLeScanner.stopScan(scanCallbackRemove);
            }
        }
    }

    public BluetoothServerSocket listenUsingL2capCoc(int i) throws IOException {
        if (i != 2) {
            throw new IllegalArgumentException("Unsupported transport: " + i);
        }
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(4, true, true, -2, false, false);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (iBindListen != 0) {
            throw new IOException("Error: " + iBindListen);
        }
        int port = bluetoothServerSocket.mSocket.getPort();
        if (port == 0) {
            throw new IOException("Error: Unable to assign PSM value");
        }
        if (DBG) {
            Log.d(TAG, "listenUsingL2capCoc: set assigned PSM to " + port);
        }
        bluetoothServerSocket.setChannel(port);
        return bluetoothServerSocket;
    }

    public BluetoothServerSocket listenUsingInsecureL2capCoc(int i) throws IOException {
        if (i != 2) {
            throw new IllegalArgumentException("Unsupported transport: " + i);
        }
        BluetoothServerSocket bluetoothServerSocket = new BluetoothServerSocket(4, false, false, -2, false, false);
        int iBindListen = bluetoothServerSocket.mSocket.bindListen();
        if (iBindListen != 0) {
            throw new IOException("Error: " + iBindListen);
        }
        int port = bluetoothServerSocket.mSocket.getPort();
        if (port == 0) {
            throw new IOException("Error: Unable to assign PSM value");
        }
        if (DBG) {
            Log.d(TAG, "listenUsingInsecureL2capOn: set assigned PSM to " + port);
        }
        bluetoothServerSocket.setChannel(port);
        return bluetoothServerSocket;
    }
}
