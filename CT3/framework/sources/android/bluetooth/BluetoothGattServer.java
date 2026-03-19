package android.bluetooth;

import android.bluetooth.IBluetoothGattServerCallback;
import android.content.Context;
import android.net.ProxyInfo;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BluetoothGattServer implements BluetoothProfile {
    private static final int CALLBACK_REG_TIMEOUT = 10000;
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothGattServer";
    private static final boolean VDBG = true;
    private final Context mContext;
    private IBluetoothGatt mService;
    private int mTransport;
    private Object mServerIfLock = new Object();
    private final IBluetoothGattServerCallback mBluetoothGattServerCallback = new IBluetoothGattServerCallback.Stub() {
        @Override
        public void onServerRegistered(int status, int serverIf) {
            Log.d(BluetoothGattServer.TAG, "onServerRegistered() - status=" + status + " serverIf=" + serverIf);
            synchronized (BluetoothGattServer.this.mServerIfLock) {
                if (BluetoothGattServer.this.mCallback != null) {
                    BluetoothGattServer.this.mServerIf = serverIf;
                    BluetoothGattServer.this.mServerIfLock.notify();
                } else {
                    Log.e(BluetoothGattServer.TAG, "onServerRegistered: mCallback is null");
                }
            }
        }

        @Override
        public void onScanResult(String address, int rssi, byte[] advData) {
            Log.d(BluetoothGattServer.TAG, "onScanResult() - Device=" + address + " RSSI=" + rssi);
        }

        @Override
        public void onServerConnectionState(int status, int serverIf, boolean connected, String address) {
            Log.d(BluetoothGattServer.TAG, "onServerConnectionState() - status=" + status + " serverIf=" + serverIf + " device=" + address);
            try {
                BluetoothGattServer.this.mCallback.onConnectionStateChange(BluetoothGattServer.this.mAdapter.getRemoteDevice(address), status, connected ? 2 : 0);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onServiceAdded(int status, int srvcType, int srvcInstId, ParcelUuid srvcId) {
            UUID uuid = srvcId == null ? null : srvcId.getUuid();
            Log.d(BluetoothGattServer.TAG, "onServiceAdded() - service=" + uuid + " status=" + status + " srvcInstId=" + srvcInstId + " srvcType=" + srvcType);
            BluetoothGattService service = BluetoothGattServer.this.getService(uuid, srvcInstId, srvcType);
            if (service == null) {
                Log.w(BluetoothGattServer.TAG, "onServiceAdded() - service == null");
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onServiceAdded(status, service);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onCharacteristicReadRequest(String address, int transId, int offset, boolean isLong, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId) {
            UUID srvcUuid = srvcId.getUuid();
            UUID charUuid = charId.getUuid();
            Log.d(BluetoothGattServer.TAG, "onCharacteristicReadRequest() - service=" + srvcUuid + ", characteristic=" + charUuid + ", transId=" + transId + ", offset=" + offset + ", charInstId=" + charInstId);
            BluetoothDevice device = BluetoothGattServer.this.mAdapter.getRemoteDevice(address);
            BluetoothGattService service = BluetoothGattServer.this.getService(srvcUuid, srvcInstId, srvcType);
            if (service == null) {
                Log.w(BluetoothGattServer.TAG, "onCharacteristicReadRequest() - service == null");
                return;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
            if (characteristic == null) {
                Log.w(BluetoothGattServer.TAG, "onCharacteristicReadRequest() - characteristic == null");
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onCharacteristicReadRequest(device, transId, offset, characteristic);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onDescriptorReadRequest(String address, int transId, int offset, boolean isLong, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId, ParcelUuid descrId) {
            UUID srvcUuid = srvcId.getUuid();
            UUID charUuid = charId.getUuid();
            UUID descrUuid = descrId.getUuid();
            Log.d(BluetoothGattServer.TAG, "onCharacteristicReadRequest() - service=" + srvcUuid + ", characteristic=" + charUuid + ", descriptor=" + descrUuid + ", transId=" + transId + ", offset=" + offset + ", isLong=" + isLong + ", srvcType=" + srvcType + ", srvcInstId=" + srvcInstId + ", charInstId=" + charInstId);
            BluetoothDevice device = BluetoothGattServer.this.mAdapter.getRemoteDevice(address);
            BluetoothGattService service = BluetoothGattServer.this.getService(srvcUuid, srvcInstId, srvcType);
            if (service == null) {
                Log.w(BluetoothGattServer.TAG, "onDescriptorReadRequest() - service == null");
                return;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
            if (characteristic == null) {
                Log.w(BluetoothGattServer.TAG, "onDescriptorReadRequest() - characteristic == null");
                return;
            }
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descrUuid);
            if (descriptor == null) {
                Log.w(BluetoothGattServer.TAG, "onDescriptorReadRequest() - descriptor == null");
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onDescriptorReadRequest(device, transId, offset, descriptor);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(String address, int transId, int offset, int length, boolean isPrep, boolean needRsp, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId, byte[] value) {
            UUID srvcUuid = srvcId.getUuid();
            UUID charUuid = charId.getUuid();
            Log.d(BluetoothGattServer.TAG, "onCharacteristicWriteRequest() - service=" + srvcUuid + ", characteristic=" + charUuid + ", transId=" + transId + ", srvcType=" + srvcType + ", srvcInstId=" + srvcInstId + ", charInstId=" + charInstId);
            BluetoothDevice device = BluetoothGattServer.this.mAdapter.getRemoteDevice(address);
            BluetoothGattService service = BluetoothGattServer.this.getService(srvcUuid, srvcInstId, srvcType);
            if (service == null) {
                Log.w(BluetoothGattServer.TAG, "onCharacteristicWriteRequest() - service == null");
                return;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
            if (characteristic == null) {
                Log.w(BluetoothGattServer.TAG, "onCharacteristicWriteRequest() - characteristic == null");
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onCharacteristicWriteRequest(device, transId, characteristic, isPrep, needRsp, offset, value);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onDescriptorWriteRequest(String address, int transId, int offset, int length, boolean isPrep, boolean needRsp, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId, ParcelUuid descrId, byte[] value) {
            UUID srvcUuid = srvcId.getUuid();
            UUID charUuid = charId.getUuid();
            UUID descrUuid = descrId.getUuid();
            Log.d(BluetoothGattServer.TAG, "onDescriptorWriteRequest() - service=" + srvcUuid + ", characteristic=" + charUuid + ", descriptor=" + descrUuid + ", transId=" + transId + ", offset=" + offset + ", length=" + length + ", isPrep=" + isPrep + ", needRsp=" + needRsp + ", srvcType=" + srvcType + ", srvcInstId=" + srvcInstId + ", charInstId=" + charInstId);
            BluetoothDevice device = BluetoothGattServer.this.mAdapter.getRemoteDevice(address);
            BluetoothGattService service = BluetoothGattServer.this.getService(srvcUuid, srvcInstId, srvcType);
            if (service == null) {
                Log.w(BluetoothGattServer.TAG, "onDescriptorWriteRequest() - service == null");
                return;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
            if (characteristic == null) {
                Log.w(BluetoothGattServer.TAG, "onDescriptorWriteRequest() - characteristic == null");
                return;
            }
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descrUuid);
            if (descriptor == null) {
                Log.w(BluetoothGattServer.TAG, "onDescriptorWriteRequest() - descriptor == null");
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onDescriptorWriteRequest(device, transId, descriptor, isPrep, needRsp, offset, value);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onExecuteWrite(String address, int transId, boolean execWrite) {
            Log.d(BluetoothGattServer.TAG, "onExecuteWrite() - device=" + address + ", transId=" + transId + "execWrite=" + execWrite);
            BluetoothDevice device = BluetoothGattServer.this.mAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(BluetoothGattServer.TAG, "onExecuteWrite() - device == null");
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onExecuteWrite(device, transId, execWrite);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", ex);
            }
        }

        @Override
        public void onNotificationSent(String address, int status) {
            Log.d(BluetoothGattServer.TAG, "onNotificationSent() - device=" + address + ", status=" + status);
            BluetoothDevice device = BluetoothGattServer.this.mAdapter.getRemoteDevice(address);
            if (device == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onNotificationSent(device, status);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception: " + ex);
            }
        }

        @Override
        public void onMtuChanged(String address, int mtu) {
            Log.d(BluetoothGattServer.TAG, "onMtuChanged() - device=" + address + ", mtu=" + mtu);
            BluetoothDevice device = BluetoothGattServer.this.mAdapter.getRemoteDevice(address);
            if (device == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onMtuChanged(device, mtu);
            } catch (Exception ex) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception: " + ex);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothGattServerCallback mCallback = null;
    private int mServerIf = 0;
    private List<BluetoothGattService> mServices = new ArrayList();

    BluetoothGattServer(Context context, IBluetoothGatt iGatt, int transport) {
        this.mContext = context;
        this.mService = iGatt;
        this.mTransport = transport;
    }

    public void close() {
        Log.d(TAG, "close()");
        unregisterCallback();
    }

    boolean registerCallback(BluetoothGattServerCallback callback) {
        Log.d(TAG, "registerCallback()");
        if (this.mService == null) {
            Log.e(TAG, "GATT service not available");
            return false;
        }
        UUID uuid = UUID.randomUUID();
        Log.d(TAG, "registerCallback() - UUID=" + uuid);
        synchronized (this.mServerIfLock) {
            if (this.mCallback != null) {
                Log.e(TAG, "App can register callback only once");
                return false;
            }
            this.mCallback = callback;
            try {
                this.mService.registerServer(new ParcelUuid(uuid), this.mBluetoothGattServerCallback);
                try {
                    this.mServerIfLock.wait(10000L);
                } catch (InterruptedException e) {
                    Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST + e);
                    this.mCallback = null;
                }
                if (this.mServerIf == 0) {
                    this.mCallback = null;
                    return false;
                }
                return true;
            } catch (RemoteException e2) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e2);
                this.mCallback = null;
                return false;
            }
        }
    }

    private void unregisterCallback() {
        Log.d(TAG, "unregisterCallback() - mServerIf=" + this.mServerIf);
        if (this.mService == null || this.mServerIf == 0) {
            return;
        }
        try {
            this.mCallback = null;
            this.mService.unregisterServer(this.mServerIf);
            this.mServerIf = 0;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
    }

    BluetoothGattService getService(UUID uuid, int instanceId, int type) {
        for (BluetoothGattService svc : this.mServices) {
            if (svc.getType() == type && svc.getInstanceId() == instanceId && svc.getUuid().equals(uuid)) {
                return svc;
            }
        }
        return null;
    }

    public boolean connect(BluetoothDevice device, boolean autoConnect) {
        Log.d(TAG, "connect() - device: " + device.getAddress() + ", auto: " + autoConnect);
        if (this.mService == null || this.mServerIf == 0) {
            return false;
        }
        try {
            this.mService.serverConnect(this.mServerIf, device.getAddress(), !autoConnect, this.mTransport);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public void cancelConnection(BluetoothDevice device) {
        Log.d(TAG, "cancelConnection() - device: " + device.getAddress());
        if (this.mService == null || this.mServerIf == 0) {
            return;
        }
        try {
            this.mService.serverDisconnect(this.mServerIf, device.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
    }

    public boolean sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
        Log.d(TAG, "sendResponse() - device: " + device.getAddress());
        if (this.mService == null || this.mServerIf == 0) {
            return false;
        }
        try {
            this.mService.sendResponse(this.mServerIf, device.getAddress(), requestId, status, offset, value);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean notifyCharacteristicChanged(BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean confirm) {
        BluetoothGattService service;
        Log.d(TAG, "notifyCharacteristicChanged() - device: " + device.getAddress());
        if (this.mService == null || this.mServerIf == 0 || (service = characteristic.getService()) == null) {
            return false;
        }
        if (characteristic.getValue() == null) {
            throw new IllegalArgumentException("Chracteristic value is empty. Use BluetoothGattCharacteristic#setvalue to update");
        }
        try {
            Log.d(TAG, "notifyCharacteristicChanged() - emit request");
            this.mService.sendNotification(this.mServerIf, device.getAddress(), service.getType(), service.getInstanceId(), new ParcelUuid(service.getUuid()), characteristic.getInstanceId(), new ParcelUuid(characteristic.getUuid()), confirm, characteristic.getValue());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean addService(BluetoothGattService service) {
        Log.d(TAG, "addService() - service: " + service.getUuid());
        if (this.mService == null || this.mServerIf == 0) {
            return false;
        }
        this.mServices.add(service);
        try {
            this.mService.beginServiceDeclaration(this.mServerIf, service.getType(), service.getInstanceId(), service.getHandles(), new ParcelUuid(service.getUuid()), service.isAdvertisePreferred());
            List<BluetoothGattService> includedServices = service.getIncludedServices();
            for (BluetoothGattService includedService : includedServices) {
                this.mService.addIncludedService(this.mServerIf, includedService.getType(), includedService.getInstanceId(), new ParcelUuid(includedService.getUuid()));
            }
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                int permission = ((characteristic.getKeySize() - 7) << 12) + characteristic.getPermissions();
                this.mService.addCharacteristic(this.mServerIf, new ParcelUuid(characteristic.getUuid()), characteristic.getProperties(), permission);
                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    int permission2 = ((characteristic.getKeySize() - 7) << 12) + descriptor.getPermissions();
                    this.mService.addDescriptor(this.mServerIf, new ParcelUuid(descriptor.getUuid()), permission2);
                }
            }
            this.mService.endServiceDeclaration(this.mServerIf);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public boolean removeService(BluetoothGattService service) {
        BluetoothGattService intService;
        Log.d(TAG, "removeService() - service: " + service.getUuid());
        if (this.mService == null || this.mServerIf == 0 || (intService = getService(service.getUuid(), service.getInstanceId(), service.getType())) == null) {
            return false;
        }
        try {
            this.mService.removeService(this.mServerIf, service.getType(), service.getInstanceId(), new ParcelUuid(service.getUuid()));
            this.mServices.remove(intService);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            return false;
        }
    }

    public void clearServices() {
        Log.d(TAG, "clearServices()");
        if (this.mService == null || this.mServerIf == 0) {
            return;
        }
        try {
            this.mService.clearServices(this.mServerIf);
            this.mServices.clear();
        } catch (RemoteException e) {
            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
        }
    }

    public List<BluetoothGattService> getServices() {
        return this.mServices;
    }

    public BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService service : this.mServices) {
            if (service.getUuid().equals(uuid)) {
                return service;
            }
        }
        return null;
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
