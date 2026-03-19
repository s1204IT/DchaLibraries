package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.net.ProxyInfo;
import android.os.BatteryStats;
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
    public static final String ACTION_BLE_STATE_CHANGED = "android.bluetooth.adapter.action.BLE_STATE_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED";
    public static final String ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";
    public static final String ACTION_LOCAL_NAME_CHANGED = "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED";
    public static final String ACTION_REQUEST_BLE_SCAN_ALWAYS_AVAILABLE = "android.bluetooth.adapter.action.REQUEST_BLE_SCAN_ALWAYS_AVAILABLE";
    public static final String ACTION_REQUEST_DISCOVERABLE = "android.bluetooth.adapter.action.REQUEST_DISCOVERABLE";
    public static final String ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";
    public static final String ACTION_SCAN_MODE_CHANGED = "android.bluetooth.adapter.action.SCAN_MODE_CHANGED";
    public static final String ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
    private static final int ADDRESS_LENGTH = 17;
    public static final String BLUETOOTH_MANAGER_SERVICE = "bluetooth_manager";
    private static final boolean DBG = true;
    public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    public static final int ERROR = Integer.MIN_VALUE;
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
    private static final boolean VDBG = true;
    private static BluetoothAdapter sAdapter;
    private static BluetoothLeAdvertiser sBluetoothLeAdvertiser;
    private static BluetoothLeScanner sBluetoothLeScanner;
    private final Map<LeScanCallback, ScanCallback> mLeScanClients;
    private final IBluetoothManager mManagerService;
    private IBluetooth mService;
    private final IBinder mToken;
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();
    private final Object mLock = new Object();
    private final IBluetoothManagerCallback mManagerCallback = new IBluetoothManagerCallback.Stub() {
        @Override
        public void onBluetoothServiceUp(IBluetooth bluetoothService) {
            Log.d(BluetoothAdapter.TAG, "onBluetoothServiceUp: " + bluetoothService);
            BluetoothAdapter.this.mServiceLock.writeLock().lock();
            BluetoothAdapter.this.mService = bluetoothService;
            BluetoothAdapter.this.mServiceLock.writeLock().unlock();
            synchronized (BluetoothAdapter.this.mProxyServiceStateCallbacks) {
                for (IBluetoothManagerCallback cb : BluetoothAdapter.this.mProxyServiceStateCallbacks) {
                    if (cb != null) {
                        try {
                            cb.onBluetoothServiceUp(bluetoothService);
                        } catch (Exception e) {
                            Log.e(BluetoothAdapter.TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                        }
                    } else {
                        Log.d(BluetoothAdapter.TAG, "onBluetoothServiceUp: cb is null!!!");
                    }
                }
            }
        }

        @Override
        public void onBluetoothServiceDown() {
            Log.d(BluetoothAdapter.TAG, "onBluetoothServiceDown: " + BluetoothAdapter.this.mService);
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
                    for (IBluetoothManagerCallback cb : BluetoothAdapter.this.mProxyServiceStateCallbacks) {
                        if (cb != null) {
                            try {
                                cb.onBluetoothServiceDown();
                            } catch (Exception e) {
                                Log.e(BluetoothAdapter.TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                            }
                        } else {
                            Log.d(BluetoothAdapter.TAG, "onBluetoothServiceDown: cb is null!!!");
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
            Log.i(BluetoothAdapter.TAG, "on QBrEdrDown: ");
        }
    };
    private final ArrayList<IBluetoothManagerCallback> mProxyServiceStateCallbacks = new ArrayList<>();

    public interface BluetoothStateChangeCallback {
        void onBluetoothStateChange(boolean z);
    }

    public interface LeScanCallback {
        void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bArr);
    }

    public static synchronized BluetoothAdapter getDefaultAdapter() {
        if (sAdapter == null) {
            IBinder b = ServiceManager.getService(BLUETOOTH_MANAGER_SERVICE);
            if (b != null) {
                IBluetoothManager managerService = IBluetoothManager.Stub.asInterface(b);
                sAdapter = new BluetoothAdapter(managerService);
            } else {
                Log.e(TAG, "Bluetooth binder is null");
            }
        }
        return sAdapter;
    }

    BluetoothAdapter(IBluetoothManager managerService) {
        try {
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.writeLock().unlock();
        }
        if (managerService == null) {
            throw new IllegalArgumentException("bluetooth manager service is null");
        }
        this.mServiceLock.writeLock().lock();
        this.mService = managerService.registerAdapter(this.mManagerCallback);
        this.mManagerService = managerService;
        this.mLeScanClients = new HashMap();
        this.mToken = new Binder();
    }

    public BluetoothDevice getRemoteDevice(String address) {
        return new BluetoothDevice(address);
    }

    public BluetoothDevice getRemoteDevice(byte[] address) {
        if (address == null || address.length != 6) {
            throw new IllegalArgumentException("Bluetooth address must have 6 bytes");
        }
        return new BluetoothDevice(String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", Byte.valueOf(address[0]), Byte.valueOf(address[1]), Byte.valueOf(address[2]), Byte.valueOf(address[3]), Byte.valueOf(address[4]), Byte.valueOf(address[5])));
    }

    public BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
        if (!getLeAccess()) {
            return null;
        }
        if (!isMultipleAdvertisementSupported() && !isPeripheralModeSupported()) {
            Log.e(TAG, "Bluetooth LE advertising not supported");
            return null;
        }
        synchronized (this.mLock) {
            if (sBluetoothLeAdvertiser == null) {
                sBluetoothLeAdvertiser = new BluetoothLeAdvertiser(this.mManagerService);
            }
        }
        return sBluetoothLeAdvertiser;
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
        Log.d(TAG, "isEnabled");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.isEnabled();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        this.mServiceLock.readLock().unlock();
    }

    public boolean isLeEnabled() {
        int state = getLeState();
        if (state == 12) {
            Log.d(TAG, "STATE_ON");
            return true;
        }
        if (state == 15) {
            Log.d(TAG, "STATE_BLE_ON");
            return true;
        }
        Log.d(TAG, "STATE_OFF");
        return false;
    }

    private void notifyUserAction(boolean enable) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null) {
                Log.e(TAG, "mService is null");
                return;
            }
            if (enable) {
                this.mService.onLeServiceUp();
            } else {
                this.mService.onBrEdrDown();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean disableBLE() {
        if (!isBleScanAlwaysAvailable()) {
            return false;
        }
        int state = getLeState();
        if (state == 12) {
            Log.d(TAG, "STATE_ON: shouldn't disable");
            try {
                this.mManagerService.updateBleAppCount(this.mToken, false);
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
            return true;
        }
        if (state == 15) {
            Log.d(TAG, "STATE_BLE_ON");
            int bleAppCnt = 0;
            try {
                bleAppCnt = this.mManagerService.updateBleAppCount(this.mToken, false);
            } catch (RemoteException e2) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e2);
            }
            if (bleAppCnt == 0) {
                notifyUserAction(false);
            }
            return true;
        }
        Log.d(TAG, "STATE_OFF: Already disabled");
        return false;
    }

    public boolean enableBLE() {
        if (!isBleScanAlwaysAvailable()) {
            return false;
        }
        if (isLeEnabled()) {
            Log.d(TAG, "enableBLE(): BT is already enabled..!");
            try {
                this.mManagerService.updateBleAppCount(this.mToken, true);
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
            return true;
        }
        try {
            Log.d(TAG, "Calling enableBLE");
            this.mManagerService.updateBleAppCount(this.mToken, true);
            return this.mManagerService.enable();
        } catch (RemoteException e2) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e2);
            return false;
        }
    }

    public int getState() {
        int state = 10;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                state = this.mService.getState();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        if (state == 15 || state == 14 || state == 16) {
            Log.d(TAG, "Consider internal state as OFF");
            state = 10;
        }
        Log.d(TAG, ProxyInfo.LOCAL_EXCL_LIST + hashCode() + ": getState(). Returning " + state);
        return state;
    }

    public int getLeState() {
        int state = 10;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                state = this.mService.getState();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        Log.d(TAG, "getLeState() returning " + state);
        return state;
    }

    boolean getLeAccess() {
        return getLeState() == 12 || getLeState() == 15;
    }

    public boolean enable() {
        if (isEnabled()) {
            Log.d(TAG, "enable(): BT is already enabled..!");
            return true;
        }
        try {
            Log.d(TAG, "enable");
            return this.mManagerService.enable();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean disable() {
        try {
            Log.d(TAG, "disable");
            return this.mManagerService.disable(true);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean disable(boolean persist) {
        try {
            Log.d(TAG, "disable: persist = " + persist);
            return this.mManagerService.disable(persist);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public String getAddress() {
        Log.d(TAG, "getAddress");
        try {
            return this.mManagerService.getAddress();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return null;
        }
    }

    public String getName() {
        Log.d(TAG, "getName");
        try {
            return this.mManagerService.getName();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return null;
        }
    }

    public boolean configHciSnoopLog(boolean enable) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.configHciSnoopLog(enable);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        this.mServiceLock.readLock().unlock();
    }

    public boolean factoryReset() {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.factoryReset();
            }
            SystemProperties.set("persist.bluetooth.factoryreset", "true");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public ParcelUuid[] getUuids() {
        Log.d(TAG, "getUuids");
        if (getState() != 12) {
            return null;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.getUuids();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return null;
    }

    public boolean setName(String name) {
        Log.d(TAG, "setName: name = " + name);
        if (getState() != 12) {
            return false;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.setName(name);
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return false;
    }

    public int getScanMode() {
        Log.d(TAG, "getScanMode");
        if (getState() != 12) {
            return 20;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.getScanMode();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return 20;
    }

    public boolean setScanMode(int mode, int duration) {
        Log.d(TAG, "setScanMode: mode = " + mode + ", duration = " + duration);
        try {
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        if (getState() != 12) {
            return false;
        }
        this.mServiceLock.readLock().lock();
        if (this.mService != null) {
            return this.mService.setScanMode(mode, duration);
        }
        return false;
    }

    public boolean setScanMode(int mode) {
        Log.d(TAG, "setScanMode: mode = " + mode + ", getDiscoverableTimeout() = " + getDiscoverableTimeout());
        if (getState() != 12) {
            return false;
        }
        return setScanMode(mode, getDiscoverableTimeout());
    }

    public int getDiscoverableTimeout() {
        Log.d(TAG, "getDiscoverableTimeout");
        if (getState() != 12) {
            return -1;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.getDiscoverableTimeout();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return -1;
    }

    public void setDiscoverableTimeout(int timeout) {
        Log.d(TAG, "setDiscoverableTimeout: timeout = " + timeout);
        try {
            if (getState() != 12) {
                return;
            }
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                this.mService.setDiscoverableTimeout(timeout);
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean startDiscovery() {
        Log.d(TAG, "startDiscovery");
        if (getState() != 12) {
            return false;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.startDiscovery();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return false;
    }

    public boolean cancelDiscovery() {
        Log.d(TAG, "cancelDiscovery");
        if (getState() != 12) {
            return false;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.cancelDiscovery();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return false;
    }

    public boolean isDiscovering() {
        Log.d(TAG, "isDiscovering");
        if (getState() != 12) {
            return false;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.isDiscovering();
            }
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return false;
    }

    public boolean isMultipleAdvertisementSupported() {
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
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return false;
    }

    public boolean isBleScanAlwaysAvailable() {
        try {
            return this.mManagerService.isBleScanAlwaysAvailable();
        } catch (RemoteException e) {
            Log.e(TAG, "remote expection when calling isBleScanAlwaysAvailable", e);
            return false;
        }
    }

    public boolean isPeripheralModeSupported() {
        if (getState() != 12) {
            return false;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.isPeripheralModeSupported();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get peripheral mode capability: ", e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return false;
    }

    public boolean isOffloadedFilteringSupported() {
        try {
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get isOffloadedFilteringSupported, error: ", e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        if (!getLeAccess()) {
            return false;
        }
        this.mServiceLock.readLock().lock();
        if (this.mService != null) {
            return this.mService.isOffloadedFilteringSupported();
        }
        return false;
    }

    public boolean isOffloadedScanBatchingSupported() {
        try {
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get isOffloadedScanBatchingSupported, error: ", e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        if (!getLeAccess()) {
            return false;
        }
        this.mServiceLock.readLock().lock();
        if (this.mService != null) {
            return this.mService.isOffloadedScanBatchingSupported();
        }
        return false;
    }

    public boolean isHardwareTrackingFiltersAvailable() {
        if (!getLeAccess()) {
            return false;
        }
        try {
            IBluetoothGatt iGatt = this.mManagerService.getBluetoothGatt();
            if (iGatt == null) {
                return false;
            }
            return iGatt.numHwTrackFiltersAvailable() != 0;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    @Deprecated
    public BluetoothActivityEnergyInfo getControllerActivityEnergyInfo(int updateType) {
        SynchronousResultReceiver receiver = new SynchronousResultReceiver();
        requestControllerActivityEnergyInfo(receiver);
        try {
            SynchronousResultReceiver.Result result = receiver.awaitResult(1000L);
            if (result.bundle != null) {
                return (BluetoothActivityEnergyInfo) result.bundle.getParcelable(BatteryStats.RESULT_RECEIVER_CONTROLLER_KEY);
            }
        } catch (TimeoutException e) {
            Log.e(TAG, "getControllerActivityEnergyInfo timed out");
        }
        return null;
    }

    public void requestControllerActivityEnergyInfo(ResultReceiver result) {
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null) {
                    this.mService.requestActivityInfo(result);
                    result = null;
                }
                this.mServiceLock.readLock().unlock();
                if (result == null) {
                    return;
                }
                result.send(0, null);
            } catch (RemoteException e) {
                Log.e(TAG, "getControllerActivityEnergyInfoCallback: " + e);
                this.mServiceLock.readLock().unlock();
                if (result == null) {
                    return;
                }
                result.send(0, null);
            }
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
            if (result != null) {
                result.send(0, null);
            }
            throw th;
        }
    }

    public Set<BluetoothDevice> getBondedDevices() {
        try {
            if (getState() != 12) {
                return toDeviceSet(new BluetoothDevice[0]);
            }
            this.mServiceLock.readLock().lock();
            return this.mService != null ? toDeviceSet(this.mService.getBondedDevices()) : toDeviceSet(new BluetoothDevice[0]);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return null;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getConnectionState() {
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
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return 0;
    }

    public int getProfileConnectionState(int profile) {
        if (getState() != 12) {
            return 0;
        }
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null) {
                return this.mService.getProfileConnectionState(profile);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getProfileConnectionState:", e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
        return 0;
    }

    public BluetoothServerSocket listenUsingRfcommOn(int channel) throws IOException {
        return listenUsingRfcommOn(channel, false, false);
    }

    public BluetoothServerSocket listenUsingRfcommOn(int channel, boolean mitm, boolean min16DigitPin) throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(1, true, true, channel, mitm, min16DigitPin);
        int errno = socket.mSocket.bindListen();
        if (channel == -2) {
            socket.setChannel(socket.mSocket.getPort());
        }
        if (errno != 0) {
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    public BluetoothServerSocket listenUsingRfcommWithServiceRecord(String name, UUID uuid) throws IOException {
        return createNewRfcommSocketAndRecord(name, uuid, true, true);
    }

    public BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String name, UUID uuid) throws IOException {
        return createNewRfcommSocketAndRecord(name, uuid, false, false);
    }

    public BluetoothServerSocket listenUsingEncryptedRfcommWithServiceRecord(String name, UUID uuid) throws IOException {
        return createNewRfcommSocketAndRecord(name, uuid, false, true);
    }

    private BluetoothServerSocket createNewRfcommSocketAndRecord(String name, UUID uuid, boolean auth, boolean encrypt) throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(1, auth, encrypt, new ParcelUuid(uuid));
        socket.setServiceName(name);
        int errno = socket.mSocket.bindListen();
        if (errno != 0) {
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    public BluetoothServerSocket listenUsingInsecureRfcommOn(int port) throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(1, false, false, port);
        int errno = socket.mSocket.bindListen();
        if (port == -2) {
            socket.setChannel(socket.mSocket.getPort());
        }
        if (errno != 0) {
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    public BluetoothServerSocket listenUsingEncryptedRfcommOn(int port) throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(1, false, true, port);
        int errno = socket.mSocket.bindListen();
        if (port == -2) {
            socket.setChannel(socket.mSocket.getPort());
        }
        if (errno < 0) {
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    public static BluetoothServerSocket listenUsingScoOn() throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(2, false, false, -1);
        int errno = socket.mSocket.bindListen();
        if (errno < 0) {
        }
        return socket;
    }

    public BluetoothServerSocket listenUsingL2capOn(int port, boolean mitm, boolean min16DigitPin) throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(3, true, true, port, mitm, min16DigitPin);
        int errno = socket.mSocket.bindListen();
        if (port == -2) {
            socket.setChannel(socket.mSocket.getPort());
        }
        if (errno != 0) {
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    public BluetoothServerSocket listenUsingL2capOn(int port) throws IOException {
        return listenUsingL2capOn(port, false, false);
    }

    public Pair<byte[], byte[]> readOutOfBandData() {
        return getState() != 12 ? null : null;
    }

    public boolean getProfileProxy(Context context, BluetoothProfile.ServiceListener listener, int profile) {
        if (context == null || listener == null) {
            return false;
        }
        if (profile == 1) {
            new BluetoothHeadset(context, listener);
            return true;
        }
        if (profile == 2) {
            new BluetoothA2dp(context, listener);
            return true;
        }
        if (profile == 11) {
            new BluetoothA2dpSink(context, listener);
            return true;
        }
        if (profile == 12) {
            new BluetoothAvrcpController(context, listener);
            return true;
        }
        if (profile == 4) {
            new BluetoothInputDevice(context, listener);
            return true;
        }
        if (profile == 5) {
            new BluetoothPan(context, listener);
            return true;
        }
        if (profile == 3) {
            new BluetoothHealth(context, listener);
            return true;
        }
        if (profile == 9) {
            new BluetoothMap(context, listener);
            return true;
        }
        if (profile == 16) {
            new BluetoothHeadsetClient(context, listener);
            return true;
        }
        if (profile == 10) {
            new BluetoothSap(context, listener);
            return true;
        }
        if (profile == 17) {
            new BluetoothPbapClient(context, listener);
            return true;
        }
        return false;
    }

    public void closeProfileProxy(int profile, BluetoothProfile proxy) {
        if (proxy == null) {
        }
        switch (profile) {
            case 1:
                BluetoothHeadset headset = (BluetoothHeadset) proxy;
                headset.close();
                break;
            case 2:
                BluetoothA2dp a2dp = (BluetoothA2dp) proxy;
                a2dp.close();
                break;
            case 3:
                BluetoothHealth health = (BluetoothHealth) proxy;
                health.close();
                break;
            case 4:
                BluetoothInputDevice iDev = (BluetoothInputDevice) proxy;
                iDev.close();
                break;
            case 5:
                BluetoothPan pan = (BluetoothPan) proxy;
                pan.close();
                break;
            case 7:
                BluetoothGatt gatt = (BluetoothGatt) proxy;
                gatt.close();
                break;
            case 8:
                BluetoothGattServer gattServer = (BluetoothGattServer) proxy;
                gattServer.close();
                break;
            case 9:
                BluetoothMap map = (BluetoothMap) proxy;
                map.close();
                break;
            case 10:
                BluetoothSap sap = (BluetoothSap) proxy;
                sap.close();
                break;
            case 11:
                BluetoothA2dpSink a2dpSink = (BluetoothA2dpSink) proxy;
                a2dpSink.close();
                break;
            case 12:
                BluetoothAvrcpController avrcp = (BluetoothAvrcpController) proxy;
                avrcp.close();
                break;
            case 16:
                BluetoothHeadsetClient headsetClient = (BluetoothHeadsetClient) proxy;
                headsetClient.close();
                break;
            case 17:
                BluetoothPbapClient pbapClient = (BluetoothPbapClient) proxy;
                pbapClient.close();
                break;
        }
    }

    public boolean enableNoAutoConnect() {
        if (isEnabled()) {
            Log.d(TAG, "enableNoAutoConnect(): BT is already enabled..!");
            return true;
        }
        try {
            Log.d(TAG, "enableNoAutoConnect");
            return this.mManagerService.enableNoAutoConnect();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean changeApplicationBluetoothState(boolean on, BluetoothStateChangeCallback callback) {
        return callback == null ? false : false;
    }

    public class StateChangeCallbackWrapper extends IBluetoothStateChangeCallback.Stub {
        private BluetoothStateChangeCallback mCallback;

        StateChangeCallbackWrapper(BluetoothStateChangeCallback callback) {
            this.mCallback = callback;
        }

        @Override
        public void onBluetoothStateChange(boolean on) {
            this.mCallback.onBluetoothStateChange(on);
        }
    }

    private Set<BluetoothDevice> toDeviceSet(BluetoothDevice[] devices) {
        Set<BluetoothDevice> deviceSet = new HashSet<>(Arrays.asList(devices));
        return Collections.unmodifiableSet(deviceSet);
    }

    protected void finalize() throws Throwable {
        try {
            this.mManagerService.unregisterAdapter(this.mManagerCallback);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        } finally {
            super.finalize();
        }
    }

    public static boolean checkBluetoothAddress(String address) {
        if (address == null || address.length() != 17) {
            return false;
        }
        for (int i = 0; i < 17; i++) {
            char c = address.charAt(i);
            switch (i % 3) {
                case 0:
                case 1:
                    if ((c < '0' || c > '9') && (c < 'A' || c > 'F')) {
                        return false;
                    }
                    break;
                    break;
                case 2:
                    if (c != ':') {
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

    IBluetooth getBluetoothService(IBluetoothManagerCallback cb) {
        synchronized (this.mProxyServiceStateCallbacks) {
            if (cb == null) {
                Log.w(TAG, "getBluetoothService() called with no BluetoothManagerCallback");
            } else if (!this.mProxyServiceStateCallbacks.contains(cb)) {
                this.mProxyServiceStateCallbacks.add(cb);
            }
        }
        return this.mService;
    }

    void removeServiceStateCallback(IBluetoothManagerCallback cb) {
        synchronized (this.mProxyServiceStateCallbacks) {
            this.mProxyServiceStateCallbacks.remove(cb);
        }
    }

    @Deprecated
    public boolean startLeScan(LeScanCallback callback) {
        return startLeScan(null, callback);
    }

    @Deprecated
    public boolean startLeScan(final UUID[] serviceUuids, final LeScanCallback callback) {
        Log.d(TAG, "startLeScan(): " + Arrays.toString(serviceUuids));
        if (callback == null) {
            Log.e(TAG, "startLeScan: null callback");
            return false;
        }
        BluetoothLeScanner scanner = getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "startLeScan: cannot get BluetoothLeScanner");
            return false;
        }
        synchronized (this.mLeScanClients) {
            if (this.mLeScanClients.containsKey(callback)) {
                Log.e(TAG, "LE Scan has already started");
                return false;
            }
            try {
                IBluetoothGatt iGatt = this.mManagerService.getBluetoothGatt();
                if (iGatt == null) {
                    return false;
                }
                ScanCallback scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        if (callbackType != 1) {
                            Log.e(BluetoothAdapter.TAG, "LE Scan has already started");
                            return;
                        }
                        ScanRecord scanRecord = result.getScanRecord();
                        if (scanRecord == null) {
                            return;
                        }
                        if (serviceUuids != null) {
                            List<ParcelUuid> uuids = new ArrayList<>();
                            for (UUID uuid : serviceUuids) {
                                uuids.add(new ParcelUuid(uuid));
                            }
                            List<ParcelUuid> scanServiceUuids = scanRecord.getServiceUuids();
                            if (scanServiceUuids == null || !scanServiceUuids.containsAll(uuids)) {
                                Log.d(BluetoothAdapter.TAG, "uuids does not match");
                                return;
                            }
                        }
                        callback.onLeScan(result.getDevice(), result.getRssi(), scanRecord.getBytes());
                    }
                };
                ScanSettings settings = new ScanSettings.Builder().setCallbackType(1).setScanMode(2).build();
                List<ScanFilter> filters = new ArrayList<>();
                if (serviceUuids != null && serviceUuids.length > 0) {
                    ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUuids[0])).build();
                    filters.add(filter);
                }
                scanner.startScan(filters, settings, scanCallback);
                this.mLeScanClients.put(callback, scanCallback);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                return false;
            }
        }
    }

    @Deprecated
    public void stopLeScan(LeScanCallback callback) {
        Log.d(TAG, "stopLeScan()");
        BluetoothLeScanner scanner = getBluetoothLeScanner();
        if (scanner == null) {
            return;
        }
        synchronized (this.mLeScanClients) {
            ScanCallback scanCallback = this.mLeScanClients.remove(callback);
            if (scanCallback == null) {
                Log.d(TAG, "scan not started yet");
            } else {
                scanner.stopScan(scanCallback);
            }
        }
    }
}
