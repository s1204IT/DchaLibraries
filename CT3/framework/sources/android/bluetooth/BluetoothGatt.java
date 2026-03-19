package android.bluetooth;

import android.content.Context;
import android.net.ProxyInfo;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BluetoothGatt implements BluetoothProfile {
    static final int AUTHENTICATION_MITM = 2;
    static final int AUTHENTICATION_NONE = 0;
    static final int AUTHENTICATION_NO_MITM = 1;
    public static final int CONNECTION_PRIORITY_BALANCED = 0;
    public static final int CONNECTION_PRIORITY_HIGH = 1;
    public static final int CONNECTION_PRIORITY_LOW_POWER = 2;
    private static final int CONN_STATE_CLOSED = 4;
    private static final int CONN_STATE_CONNECTED = 2;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final int CONN_STATE_IDLE = 0;
    private static final boolean DBG = true;
    public static final int GATT_CONNECTION_CONGESTED = 143;
    public static final int GATT_FAILURE = 257;
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 5;
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 15;
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 13;
    public static final int GATT_INVALID_OFFSET = 7;
    public static final int GATT_READ_NOT_PERMITTED = 2;
    public static final int GATT_REQUEST_NOT_SUPPORTED = 6;
    public static final int GATT_SUCCESS = 0;
    public static final int GATT_WRITE_NOT_PERMITTED = 3;
    private static final String TAG = "BluetoothGatt";
    private static final boolean VDBG = true;
    private boolean mAutoConnect;
    private BluetoothGattCallback mCallback;
    private int mClientIf;
    private final Context mContext;
    private BluetoothDevice mDevice;
    private IBluetoothGatt mService;
    private int mTransport;
    private boolean mAuthRetry = false;
    private final Object mStateLock = new Object();
    private Boolean mDeviceBusy = false;
    private final IBluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallbackWrapper() {
        @Override
        public void onClientRegistered(int status, int clientIf) {
            Log.d(BluetoothGatt.TAG, "onClientRegistered() - status=" + status + " clientIf=" + clientIf);
            synchronized (BluetoothGatt.this.mStateLock) {
                if (BluetoothGatt.this.mConnState != 1) {
                    Log.e(BluetoothGatt.TAG, "Bad connection state: " + BluetoothGatt.this.mConnState);
                }
            }
            BluetoothGatt.this.mClientIf = clientIf;
            if (status != 0) {
                BluetoothGatt.this.mCallback.onConnectionStateChange(BluetoothGatt.this, 257, 0);
                synchronized (BluetoothGatt.this.mStateLock) {
                    BluetoothGatt.this.mConnState = 0;
                }
            } else {
                try {
                    BluetoothGatt.this.mService.clientConnect(BluetoothGatt.this.mClientIf, BluetoothGatt.this.mDevice.getAddress(), BluetoothGatt.this.mAutoConnect ? false : true, BluetoothGatt.this.mTransport);
                } catch (RemoteException e) {
                    Log.e(BluetoothGatt.TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                }
            }
        }

        @Override
        public void onClientConnectionState(int status, int clientIf, boolean connected, String address) {
            Log.d(BluetoothGatt.TAG, "onClientConnectionState() - status=" + status + " clientIf=" + clientIf + " device=" + address);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onClientConnectionState() - !address.equals(mDevice.getAddress())");
                return;
            }
            int profileState = connected ? 2 : 0;
            try {
                BluetoothGatt.this.mCallback.onConnectionStateChange(BluetoothGatt.this, status, profileState);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
            synchronized (BluetoothGatt.this.mStateLock) {
                if (connected) {
                    BluetoothGatt.this.mConnState = 2;
                } else {
                    BluetoothGatt.this.mConnState = 0;
                }
            }
            synchronized (BluetoothGatt.this.mDeviceBusy) {
                BluetoothGatt.this.mDeviceBusy = false;
            }
        }

        @Override
        public void onSearchComplete(String address, List<BluetoothGattService> services, int status) {
            Log.d(BluetoothGatt.TAG, "onSearchComplete() = Device=" + address + " Status=" + status);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onSearchComplete() - !address.equals(mDevice.getAddress())");
                return;
            }
            for (BluetoothGattService s : services) {
                s.setDevice(BluetoothGatt.this.mDevice);
            }
            BluetoothGatt.this.mServices.addAll(services);
            for (BluetoothGattService fixedService : BluetoothGatt.this.mServices) {
                ArrayList<BluetoothGattService> includedServices = new ArrayList<>(fixedService.getIncludedServices());
                fixedService.getIncludedServices().clear();
                for (BluetoothGattService brokenRef : includedServices) {
                    BluetoothGattService includedService = BluetoothGatt.this.getService(BluetoothGatt.this.mDevice, brokenRef.getUuid(), brokenRef.getInstanceId(), brokenRef.getType());
                    if (includedService != null) {
                        fixedService.addIncludedService(includedService);
                    } else {
                        Log.e(BluetoothGatt.TAG, "Broken GATT database: can't find included service.");
                    }
                }
            }
            try {
                BluetoothGatt.this.mCallback.onServicesDiscovered(BluetoothGatt.this, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onCharacteristicRead(String address, int status, int handle, byte[] value) {
            Log.d(BluetoothGatt.TAG, "onCharacteristicRead() - Device=" + address + " handle=" + handle + " Status=" + status);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onCharacteristicRead() - !address.equals(mDevice.getAddress())");
                return;
            }
            synchronized (BluetoothGatt.this.mDeviceBusy) {
                BluetoothGatt.this.mDeviceBusy = false;
            }
            if ((status == 5 || status == 15) && !BluetoothGatt.this.mAuthRetry) {
                try {
                    Log.d(BluetoothGatt.TAG, "onCharacteristicRead() - retry");
                    BluetoothGatt.this.mAuthRetry = true;
                    BluetoothGatt.this.mService.readCharacteristic(BluetoothGatt.this.mClientIf, address, handle, 2);
                    return;
                } catch (RemoteException e) {
                    Log.e(BluetoothGatt.TAG, "Exception happen when retry", e);
                }
            }
            BluetoothGatt.this.mAuthRetry = false;
            BluetoothGattCharacteristic characteristic = BluetoothGatt.this.getCharacteristicById(BluetoothGatt.this.mDevice, handle);
            if (characteristic == null) {
                Log.w(BluetoothGatt.TAG, "onCharacteristicRead() failed to find characteristic!");
                return;
            }
            if (status == 0) {
                characteristic.setValue(value);
            }
            try {
                BluetoothGatt.this.mCallback.onCharacteristicRead(BluetoothGatt.this, characteristic, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onCharacteristicWrite(String address, int status, int handle) {
            Log.d(BluetoothGatt.TAG, "onCharacteristicWrite() - Device=" + address + " handle=" + handle + " Status=" + status);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onCharacteristicWrite() - !address.equals(mDevice.getAddress())");
                return;
            }
            synchronized (BluetoothGatt.this.mDeviceBusy) {
                BluetoothGatt.this.mDeviceBusy = false;
            }
            BluetoothGattCharacteristic characteristic = BluetoothGatt.this.getCharacteristicById(BluetoothGatt.this.mDevice, handle);
            if (characteristic == null) {
                Log.w(BluetoothGatt.TAG, "onCharacteristicWrite() failed to find characteristic!");
                return;
            }
            if ((status == 5 || status == 15) && !BluetoothGatt.this.mAuthRetry) {
                try {
                    Log.d(BluetoothGatt.TAG, "onCharacteristicWrite() - retry");
                    BluetoothGatt.this.mAuthRetry = true;
                    BluetoothGatt.this.mService.writeCharacteristic(BluetoothGatt.this.mClientIf, address, handle, characteristic.getWriteType(), 2, characteristic.getValue());
                    return;
                } catch (RemoteException e) {
                    Log.e(BluetoothGatt.TAG, "Exception happen when retry", e);
                }
            }
            BluetoothGatt.this.mAuthRetry = false;
            try {
                BluetoothGatt.this.mCallback.onCharacteristicWrite(BluetoothGatt.this, characteristic, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onNotify(String address, int handle, byte[] value) {
            Log.d(BluetoothGatt.TAG, "onNotify() - Device=" + address + " handle=" + handle);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onNotify() - !address.equals(mDevice.getAddress())");
                return;
            }
            BluetoothGattCharacteristic characteristic = BluetoothGatt.this.getCharacteristicById(BluetoothGatt.this.mDevice, handle);
            if (characteristic == null) {
                Log.w(BluetoothGatt.TAG, "onNotify() failed to find characteristic!");
                return;
            }
            characteristic.setValue(value);
            try {
                BluetoothGatt.this.mCallback.onCharacteristicChanged(BluetoothGatt.this, characteristic);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onDescriptorRead(String address, int status, int handle, byte[] value) {
            Log.d(BluetoothGatt.TAG, "onDescriptorRead() - Device=" + address + " handle=" + handle);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onDescriptorRead() - !address.equals(mDevice.getAddress())");
                return;
            }
            synchronized (BluetoothGatt.this.mDeviceBusy) {
                BluetoothGatt.this.mDeviceBusy = false;
            }
            BluetoothGattDescriptor descriptor = BluetoothGatt.this.getDescriptorById(BluetoothGatt.this.mDevice, handle);
            if (descriptor == null) {
                Log.w(BluetoothGatt.TAG, "onDescriptorRead() failed to find descriptor!");
                return;
            }
            if (status == 0) {
                descriptor.setValue(value);
            }
            if ((status == 5 || status == 15) && !BluetoothGatt.this.mAuthRetry) {
                try {
                    Log.d(BluetoothGatt.TAG, "onDescritporRead() - retry");
                    BluetoothGatt.this.mAuthRetry = true;
                    BluetoothGatt.this.mService.readDescriptor(BluetoothGatt.this.mClientIf, address, handle, 2);
                    return;
                } catch (RemoteException e) {
                    Log.e(BluetoothGatt.TAG, "Exception happen when retry", e);
                }
            }
            BluetoothGatt.this.mAuthRetry = true;
            try {
                BluetoothGatt.this.mCallback.onDescriptorRead(BluetoothGatt.this, descriptor, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onDescriptorWrite(String address, int status, int handle) {
            Log.d(BluetoothGatt.TAG, "onDescriptorWrite() - Device=" + address + " handle=" + handle);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onDescriptorWrite() - !address.equals(mDevice.getAddress())");
                return;
            }
            synchronized (BluetoothGatt.this.mDeviceBusy) {
                BluetoothGatt.this.mDeviceBusy = false;
            }
            BluetoothGattDescriptor descriptor = BluetoothGatt.this.getDescriptorById(BluetoothGatt.this.mDevice, handle);
            if (descriptor == null) {
                return;
            }
            if ((status == 5 || status == 15) && !BluetoothGatt.this.mAuthRetry) {
                try {
                    Log.d(BluetoothGatt.TAG, "onDescriptorWrite() - retry");
                    BluetoothGatt.this.mAuthRetry = true;
                    BluetoothGatt.this.mService.writeDescriptor(BluetoothGatt.this.mClientIf, address, handle, 2, 2, descriptor.getValue());
                    return;
                } catch (RemoteException e) {
                    Log.e(BluetoothGatt.TAG, "Exception happen when retry", e);
                }
            }
            BluetoothGatt.this.mAuthRetry = false;
            try {
                BluetoothGatt.this.mCallback.onDescriptorWrite(BluetoothGatt.this, descriptor, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onExecuteWrite(String address, int status) {
            Log.d(BluetoothGatt.TAG, "onExecuteWrite() - Device=" + address + " status=" + status);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onExecuteWrite() - !address.equals(mDevice.getAddress())");
                return;
            }
            synchronized (BluetoothGatt.this.mDeviceBusy) {
                BluetoothGatt.this.mDeviceBusy = false;
            }
            try {
                BluetoothGatt.this.mCallback.onReliableWriteCompleted(BluetoothGatt.this, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onReadRemoteRssi(String address, int rssi, int status) {
            Log.d(BluetoothGatt.TAG, "onReadRemoteRssi() - Device=" + address + " rssi=" + rssi + " status=" + status);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                Log.w(BluetoothGatt.TAG, "onReadRemoteRssi() - !address.equals(mDevice.getAddress())");
                return;
            }
            try {
                BluetoothGatt.this.mCallback.onReadRemoteRssi(BluetoothGatt.this, rssi, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onConfigureMTU(String address, int mtu, int status) {
            Log.d(BluetoothGatt.TAG, "onConfigureMTU() - Device=" + address + " mtu=" + mtu + " status=" + status);
            if (!address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                return;
            }
            try {
                BluetoothGatt.this.mCallback.onMtuChanged(BluetoothGatt.this, mtu, status);
            } catch (Exception ex) {
                Log.w(BluetoothGatt.TAG, "Unhandled exception in callback", ex);
            }
        }
    };
    private List<BluetoothGattService> mServices = new ArrayList();
    private int mConnState = 0;

    BluetoothGatt(Context context, IBluetoothGatt iGatt, BluetoothDevice device, int transport) {
        this.mContext = context;
        this.mService = iGatt;
        this.mDevice = device;
        this.mTransport = transport;
    }

    public void close() {
        Log.d(TAG, "close()");
        unregisterApp();
        this.mConnState = 4;
    }

    BluetoothGattService getService(BluetoothDevice device, UUID uuid, int instanceId, int type) {
        for (BluetoothGattService svc : this.mServices) {
            if (svc.getDevice().equals(device) && svc.getType() == type && svc.getInstanceId() == instanceId && svc.getUuid().equals(uuid)) {
                return svc;
            }
        }
        return null;
    }

    BluetoothGattCharacteristic getCharacteristicById(BluetoothDevice device, int instanceId) {
        for (BluetoothGattService svc : this.mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                if (charac.getInstanceId() == instanceId) {
                    return charac;
                }
            }
        }
        return null;
    }

    BluetoothGattDescriptor getDescriptorById(BluetoothDevice device, int instanceId) {
        for (BluetoothGattService svc : this.mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                for (BluetoothGattDescriptor desc : charac.getDescriptors()) {
                    if (desc.getInstanceId() == instanceId) {
                        return desc;
                    }
                }
            }
        }
        return null;
    }

    private boolean registerApp(BluetoothGattCallback callback) {
        Log.d(TAG, "registerApp()");
        if (this.mService == null) {
            return false;
        }
        this.mCallback = callback;
        UUID uuid = UUID.randomUUID();
        Log.d(TAG, "registerApp() - UUID=" + uuid);
        try {
            this.mService.registerClient(new ParcelUuid(uuid), this.mBluetoothGattCallback);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    private void unregisterApp() {
        Log.d(TAG, "unregisterApp() - mClientIf=" + this.mClientIf);
        if (this.mService == null || this.mClientIf == 0) {
            return;
        }
        try {
            this.mCallback = null;
            this.mService.unregisterClient(this.mClientIf);
            this.mClientIf = 0;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
    }

    boolean connect(Boolean autoConnect, BluetoothGattCallback callback) {
        Log.d(TAG, "connect() - device: " + this.mDevice.getAddress() + ", auto: " + autoConnect);
        synchronized (this.mStateLock) {
            if (this.mConnState != 0) {
                throw new IllegalStateException("Not idle");
            }
            this.mConnState = 1;
        }
        this.mAutoConnect = autoConnect.booleanValue();
        if (registerApp(callback)) {
            return true;
        }
        synchronized (this.mStateLock) {
            this.mConnState = 0;
        }
        Log.e(TAG, "Failed to register callback");
        return false;
    }

    public void disconnect() {
        Log.d(TAG, "cancelOpen() - device: " + this.mDevice.getAddress());
        if (this.mService == null || this.mClientIf == 0) {
            return;
        }
        try {
            this.mService.clientDisconnect(this.mClientIf, this.mDevice.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
    }

    public boolean connect() {
        try {
            this.mService.clientConnect(this.mClientIf, this.mDevice.getAddress(), false, this.mTransport);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public boolean discoverServices() {
        Log.d(TAG, "discoverServices() - device: " + this.mDevice.getAddress());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        this.mServices.clear();
        try {
            this.mService.discoverServices(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public List<BluetoothGattService> getServices() {
        List<BluetoothGattService> result = new ArrayList<>();
        for (BluetoothGattService service : this.mServices) {
            if (service.getDevice().equals(this.mDevice)) {
                result.add(service);
            }
        }
        return result;
    }

    public BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService service : this.mServices) {
            if (service.getDevice().equals(this.mDevice) && service.getUuid().equals(uuid)) {
                return service;
            }
        }
        return null;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        BluetoothGattService service;
        BluetoothDevice device;
        if ((characteristic.getProperties() & 2) == 0) {
            return false;
        }
        Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        if (this.mService == null || this.mClientIf == 0 || (service = characteristic.getService()) == null || (device = service.getDevice()) == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                Log.d(TAG, "readCharacteristic: mDeviceBusy = true, and return false");
                return false;
            }
            this.mDeviceBusy = true;
            try {
                this.mService.readCharacteristic(this.mClientIf, device.getAddress(), characteristic.getInstanceId(), 0);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                this.mDeviceBusy = false;
                return false;
            }
        }
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        BluetoothGattService service;
        BluetoothDevice device;
        if ((characteristic.getProperties() & 8) == 0 && (characteristic.getProperties() & 4) == 0) {
            return false;
        }
        Log.d(TAG, "writeCharacteristic() - uuid: " + characteristic.getUuid());
        if (this.mService == null || this.mClientIf == 0 || characteristic.getValue() == null || (service = characteristic.getService()) == null || (device = service.getDevice()) == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                Log.d(TAG, "writeCharacteristic: mDeviceBusy = true, and return false");
                return false;
            }
            this.mDeviceBusy = true;
            try {
                this.mService.writeCharacteristic(this.mClientIf, device.getAddress(), characteristic.getInstanceId(), characteristic.getWriteType(), 0, characteristic.getValue());
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                this.mDeviceBusy = false;
                return false;
            }
        }
    }

    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        BluetoothGattCharacteristic characteristic;
        BluetoothGattService service;
        BluetoothDevice device;
        Log.d(TAG, "readDescriptor() - uuid: " + descriptor.getUuid());
        if (this.mService == null || this.mClientIf == 0 || (characteristic = descriptor.getCharacteristic()) == null || (service = characteristic.getService()) == null || (device = service.getDevice()) == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                Log.d(TAG, "readDescriptor: mDeviceBusy = true, and return false");
                return false;
            }
            this.mDeviceBusy = true;
            try {
                this.mService.readDescriptor(this.mClientIf, device.getAddress(), descriptor.getInstanceId(), 0);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                this.mDeviceBusy = false;
                return false;
            }
        }
    }

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        BluetoothGattCharacteristic characteristic;
        BluetoothGattService service;
        BluetoothDevice device;
        Log.d(TAG, "writeDescriptor() - uuid: " + descriptor.getUuid());
        if (this.mService == null || this.mClientIf == 0 || descriptor.getValue() == null || (characteristic = descriptor.getCharacteristic()) == null || (service = characteristic.getService()) == null || (device = service.getDevice()) == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                Log.d(TAG, "writeDescriptor: mDeviceBusy = true, and return false");
                return false;
            }
            this.mDeviceBusy = true;
            try {
                this.mService.writeDescriptor(this.mClientIf, device.getAddress(), descriptor.getInstanceId(), 2, 0, descriptor.getValue());
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                this.mDeviceBusy = false;
                return false;
            }
        }
    }

    public boolean beginReliableWrite() {
        Log.d(TAG, "beginReliableWrite() - device: " + this.mDevice.getAddress());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.beginReliableWrite(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean executeReliableWrite() {
        Log.d(TAG, "executeReliableWrite() - device: " + this.mDevice.getAddress());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                Log.d(TAG, "executeReliableWrite: mDeviceBusy = true, and return false");
                return false;
            }
            this.mDeviceBusy = true;
            try {
                this.mService.endReliableWrite(this.mClientIf, this.mDevice.getAddress(), true);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
                this.mDeviceBusy = false;
                return false;
            }
        }
    }

    public void abortReliableWrite() {
        Log.d(TAG, "abortReliableWrite() - device: " + this.mDevice.getAddress());
        if (this.mService == null || this.mClientIf == 0) {
            return;
        }
        try {
            this.mService.endReliableWrite(this.mClientIf, this.mDevice.getAddress(), false);
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
    }

    public void abortReliableWrite(BluetoothDevice mDevice) {
        abortReliableWrite();
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        BluetoothGattService service;
        BluetoothDevice device;
        Log.d(TAG, "setCharacteristicNotification() - uuid: " + characteristic.getUuid() + " enable: " + enable);
        if (this.mService == null || this.mClientIf == 0 || (service = characteristic.getService()) == null || (device = service.getDevice()) == null) {
            return false;
        }
        try {
            this.mService.registerForNotification(this.mClientIf, device.getAddress(), characteristic.getInstanceId(), enable);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean refresh() {
        Log.d(TAG, "refresh() - device: " + this.mDevice.getAddress());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.refreshDevice(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean readRemoteRssi() {
        Log.d(TAG, "readRssi() - device: " + this.mDevice.getAddress());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.readRemoteRssi(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean requestMtu(int mtu) {
        Log.d(TAG, "configureMTU() - device: " + this.mDevice.getAddress() + " mtu: " + mtu);
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.configureMTU(this.mClientIf, this.mDevice.getAddress(), mtu);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean requestConnectionPriority(int connectionPriority) {
        if (connectionPriority < 0 || connectionPriority > 2) {
            throw new IllegalArgumentException("connectionPriority not within valid range");
        }
        Log.d(TAG, "requestConnectionPriority() - params: " + connectionPriority);
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.connectionParameterUpdate(this.mClientIf, this.mDevice.getAddress(), connectionPriority);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    @Override
    public int getConnectionState(BluetoothDevice device) {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectedDevices instead.");
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        throw new UnsupportedOperationException("Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}
