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
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.ServiceManager;
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

public final class BluetoothAdapter {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED";
    public static final String ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";
    public static final String ACTION_LOCAL_NAME_CHANGED = "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED";
    public static final String ACTION_REQUEST_DISCOVERABLE = "android.bluetooth.adapter.action.REQUEST_DISCOVERABLE";
    public static final String ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";
    public static final String ACTION_SCAN_MODE_CHANGED = "android.bluetooth.adapter.action.SCAN_MODE_CHANGED";
    public static final String ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
    public static final int ACTIVITY_ENERGY_INFO_CACHED = 0;
    public static final int ACTIVITY_ENERGY_INFO_REFRESHED = 1;
    private static final int ADDRESS_LENGTH = 17;
    public static final String BLUETOOTH_MANAGER_SERVICE = "bluetooth_manager";
    private static final int CONTROLLER_ENERGY_UPDATE_TIMEOUT_MILLIS = 30;
    private static final boolean DBG = true;
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
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_DISCONNECTING = 3;
    public static final int STATE_OFF = 10;
    public static final int STATE_ON = 12;
    public static final int STATE_TURNING_OFF = 13;
    public static final int STATE_TURNING_ON = 11;
    private static final String TAG = "BluetoothAdapter";
    private static final boolean VDBG = false;
    private static BluetoothAdapter sAdapter;
    private static BluetoothLeAdvertiser sBluetoothLeAdvertiser;
    private static BluetoothLeScanner sBluetoothLeScanner;
    private final Map<LeScanCallback, ScanCallback> mLeScanClients;
    private final IBluetoothManager mManagerService;
    private IBluetooth mService;
    private final Object mLock = new Object();
    private final IBluetoothManagerCallback mManagerCallback = new IBluetoothManagerCallback.Stub() {
        @Override
        public void onBluetoothServiceUp(IBluetooth bluetoothService) {
            synchronized (BluetoothAdapter.this.mManagerCallback) {
                BluetoothAdapter.this.mService = bluetoothService;
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
        }

        @Override
        public void onBluetoothServiceDown() {
            synchronized (BluetoothAdapter.this.mManagerCallback) {
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
            }
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
        if (managerService == null) {
            throw new IllegalArgumentException("bluetooth manager service is null");
        }
        try {
            this.mService = managerService.registerAdapter(this.mManagerCallback);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
        this.mManagerService = managerService;
        this.mLeScanClients = new HashMap();
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
        if (getState() != 12) {
            return null;
        }
        if (!isMultipleAdvertisementSupported() && !isPeripheralModeSupported()) {
            Log.e(TAG, "bluetooth le advertising not supported");
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
        if (getState() != 12) {
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
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
        synchronized (this.mManagerCallback) {
            if (this.mService != null) {
                return this.mService.isEnabled();
            }
            return false;
        }
    }

    public int getState() {
        try {
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
        synchronized (this.mManagerCallback) {
            if (this.mService != null) {
                int state = this.mService.getState();
                return state;
            }
            Log.d(TAG, ProxyInfo.LOCAL_EXCL_LIST + hashCode() + ": getState() :  mService = null. Returning STATE_OFF");
            return 10;
        }
    }

    public boolean enable() {
        if (isEnabled()) {
            Log.d(TAG, "enable(): BT is already enabled..!");
            return true;
        }
        try {
            return this.mManagerService.enable();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean disable() {
        try {
            return this.mManagerService.disable(true);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean disable(boolean persist) {
        try {
            return this.mManagerService.disable(persist);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public String getAddress() {
        try {
            return this.mManagerService.getAddress();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return null;
        }
    }

    public String getName() {
        try {
            return this.mManagerService.getName();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return null;
        }
    }

    public boolean configHciSnoopLog(boolean enable) {
        try {
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
        synchronized (this.mManagerCallback) {
            if (this.mService != null) {
                return this.mService.configHciSnoopLog(enable);
            }
            return false;
        }
    }

    public ParcelUuid[] getUuids() {
        ParcelUuid[] uuids = null;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        uuids = this.mService.getUuids();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return uuids;
    }

    public boolean setName(String name) {
        boolean name2 = false;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        name2 = this.mService.setName(name);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return name2;
    }

    public int getScanMode() {
        int scanMode = 20;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        scanMode = this.mService.getScanMode();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return scanMode;
    }

    public boolean setScanMode(int mode, int duration) {
        boolean scanMode = false;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        scanMode = this.mService.setScanMode(mode, duration);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return scanMode;
    }

    public boolean setScanMode(int mode) {
        if (getState() != 12) {
            return false;
        }
        return setScanMode(mode, getDiscoverableTimeout());
    }

    public int getDiscoverableTimeout() {
        int discoverableTimeout = -1;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        discoverableTimeout = this.mService.getDiscoverableTimeout();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return discoverableTimeout;
    }

    public void setDiscoverableTimeout(int timeout) {
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        this.mService.setDiscoverableTimeout(timeout);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
    }

    public boolean startDiscovery() {
        boolean zStartDiscovery = false;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        zStartDiscovery = this.mService.startDiscovery();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return zStartDiscovery;
    }

    public boolean cancelDiscovery() {
        boolean zCancelDiscovery = false;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        zCancelDiscovery = this.mService.cancelDiscovery();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return zCancelDiscovery;
    }

    public boolean isDiscovering() {
        boolean zIsDiscovering = false;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        zIsDiscovering = this.mService.isDiscovering();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        return zIsDiscovering;
    }

    public boolean isMultipleAdvertisementSupported() {
        if (getState() != 12) {
            return false;
        }
        try {
            return this.mService.isMultiAdvertisementSupported();
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get isMultipleAdvertisementSupported, error: ", e);
            return false;
        }
    }

    public boolean isPeripheralModeSupported() {
        if (getState() != 12) {
            return false;
        }
        try {
            return this.mService.isPeripheralModeSupported();
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get peripheral mode capability: ", e);
            return false;
        }
    }

    public boolean isOffloadedFilteringSupported() {
        if (getState() != 12) {
            return false;
        }
        try {
            return this.mService.isOffloadedFilteringSupported();
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get isOffloadedFilteringSupported, error: ", e);
            return false;
        }
    }

    public boolean isOffloadedScanBatchingSupported() {
        if (getState() != 12) {
            return false;
        }
        try {
            return this.mService.isOffloadedScanBatchingSupported();
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get isOffloadedScanBatchingSupported, error: ", e);
            return false;
        }
    }

    public BluetoothActivityEnergyInfo getControllerActivityEnergyInfo(int updateType) {
        if (getState() != 12) {
            return null;
        }
        try {
            if (!this.mService.isActivityAndEnergyReportingSupported()) {
                return null;
            }
            synchronized (this) {
                if (updateType == 1) {
                    this.mService.getActivityEnergyInfoFromController();
                    wait(30L);
                }
                BluetoothActivityEnergyInfo record = this.mService.reportActivityInfo();
                if (record.isValid()) {
                    return record;
                }
                return null;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getControllerActivityEnergyInfoCallback: " + e);
            return null;
        } catch (InterruptedException e2) {
            Log.e(TAG, "getControllerActivityEnergyInfoCallback wait interrupted: " + e2);
            return null;
        }
    }

    public Set<BluetoothDevice> getBondedDevices() {
        Set<BluetoothDevice> deviceSet;
        if (getState() != 12) {
            return toDeviceSet(new BluetoothDevice[0]);
        }
        try {
            synchronized (this.mManagerCallback) {
                deviceSet = this.mService != null ? toDeviceSet(this.mService.getBondedDevices()) : toDeviceSet(new BluetoothDevice[0]);
            }
            return deviceSet;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return null;
        }
    }

    public int getConnectionState() {
        int adapterConnectionState = 0;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        adapterConnectionState = this.mService.getAdapterConnectionState();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getConnectionState:", e);
            }
        }
        return adapterConnectionState;
    }

    public int getProfileConnectionState(int profile) {
        int profileConnectionState = 0;
        if (getState() == 12) {
            try {
                synchronized (this.mManagerCallback) {
                    if (this.mService != null) {
                        profileConnectionState = this.mService.getProfileConnectionState(profile);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getProfileConnectionState:", e);
            }
        }
        return profileConnectionState;
    }

    public BluetoothServerSocket listenUsingRfcommOn(int channel) throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(1, true, true, channel);
        int errno = socket.mSocket.bindListen();
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
        if (errno != 0) {
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    public BluetoothServerSocket listenUsingEncryptedRfcommOn(int port) throws IOException {
        BluetoothServerSocket socket = new BluetoothServerSocket(1, false, true, port);
        int errno = socket.mSocket.bindListen();
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

    public Pair<byte[], byte[]> readOutOfBandData() {
        if (getState() != 12) {
        }
        return null;
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
        if (profile == 10) {
            new BluetoothA2dpSink(context, listener);
            return true;
        }
        if (profile == 11) {
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
        if (profile != 16) {
            return false;
        }
        new BluetoothHeadsetClient(context, listener);
        return true;
    }

    public void closeProfileProxy(int profile, BluetoothProfile proxy) {
        if (proxy != null) {
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
                    BluetoothA2dpSink a2dpSink = (BluetoothA2dpSink) proxy;
                    a2dpSink.close();
                    break;
                case 11:
                    BluetoothAvrcpController avrcp = (BluetoothAvrcpController) proxy;
                    avrcp.close();
                    break;
                case 16:
                    BluetoothHeadsetClient headsetClient = (BluetoothHeadsetClient) proxy;
                    headsetClient.close();
                    break;
            }
        }
    }

    public boolean enableNoAutoConnect() {
        if (isEnabled()) {
            Log.d(TAG, "enableNoAutoConnect(): BT is already enabled..!");
            return true;
        }
        try {
            return this.mManagerService.enableNoAutoConnect();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean changeApplicationBluetoothState(boolean on, BluetoothStateChangeCallback callback) {
        if (callback == null) {
        }
        return false;
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
        Log.d(TAG, "startLeScan(): " + serviceUuids);
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
                        if (scanRecord != null) {
                            if (serviceUuids != null) {
                                List<ParcelUuid> uuids = new ArrayList<>();
                                UUID[] arr$ = serviceUuids;
                                for (UUID uuid : arr$) {
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
        if (scanner != null) {
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
}
