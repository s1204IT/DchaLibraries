package android.bluetooth;

import android.app.job.JobInfo;
import android.bluetooth.IBluetoothGattServerCallback;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class BluetoothGattServer implements BluetoothProfile {
    private static final int CALLBACK_REG_TIMEOUT = 10000;
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothGattServer";
    private static final boolean VDBG = false;
    private BluetoothGattService mPendingService;
    private IBluetoothGatt mService;
    private int mTransport;
    private Object mServerIfLock = new Object();
    private final IBluetoothGattServerCallback mBluetoothGattServerCallback = new IBluetoothGattServerCallback.Stub() {
        @Override
        public void onServerRegistered(int i, int i2) {
            Log.d(BluetoothGattServer.TAG, "onServerRegistered() - status=" + i + " serverIf=" + i2);
            synchronized (BluetoothGattServer.this.mServerIfLock) {
                if (BluetoothGattServer.this.mCallback != null) {
                    BluetoothGattServer.this.mServerIf = i2;
                    BluetoothGattServer.this.mServerIfLock.notify();
                } else {
                    Log.e(BluetoothGattServer.TAG, "onServerRegistered: mCallback is null");
                }
            }
        }

        @Override
        public void onServerConnectionState(int i, int i2, boolean z, String str) {
            Log.d(BluetoothGattServer.TAG, "onServerConnectionState() - status=" + i + " serverIf=" + i2 + " device=" + str);
            try {
                BluetoothGattServer.this.mCallback.onConnectionStateChange(BluetoothGattServer.this.mAdapter.getRemoteDevice(str), i, z ? 2 : 0);
            } catch (Exception e) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", e);
            }
        }

        @Override
        public void onServiceAdded(int i, BluetoothGattService bluetoothGattService) {
            Log.d(BluetoothGattServer.TAG, "onServiceAdded() - handle=" + bluetoothGattService.getInstanceId() + " uuid=" + bluetoothGattService.getUuid() + " status=" + i);
            if (BluetoothGattServer.this.mPendingService != null) {
                BluetoothGattService bluetoothGattService2 = BluetoothGattServer.this.mPendingService;
                BluetoothGattServer.this.mPendingService = null;
                bluetoothGattService2.setInstanceId(bluetoothGattService.getInstanceId());
                List<BluetoothGattCharacteristic> characteristics = bluetoothGattService2.getCharacteristics();
                List<BluetoothGattCharacteristic> characteristics2 = bluetoothGattService.getCharacteristics();
                for (int i2 = 0; i2 < characteristics2.size(); i2++) {
                    BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(i2);
                    BluetoothGattCharacteristic bluetoothGattCharacteristic2 = characteristics2.get(i2);
                    bluetoothGattCharacteristic.setInstanceId(bluetoothGattCharacteristic2.getInstanceId());
                    List<BluetoothGattDescriptor> descriptors = bluetoothGattCharacteristic.getDescriptors();
                    List<BluetoothGattDescriptor> descriptors2 = bluetoothGattCharacteristic2.getDescriptors();
                    for (int i3 = 0; i3 < descriptors2.size(); i3++) {
                        descriptors.get(i3).setInstanceId(descriptors2.get(i3).getInstanceId());
                    }
                }
                BluetoothGattServer.this.mServices.add(bluetoothGattService2);
                try {
                    BluetoothGattServer.this.mCallback.onServiceAdded(i, bluetoothGattService2);
                } catch (Exception e) {
                    Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", e);
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(String str, int i, int i2, boolean z, int i3) {
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            BluetoothGattCharacteristic characteristicByHandle = BluetoothGattServer.this.getCharacteristicByHandle(i3);
            if (characteristicByHandle != null) {
                try {
                    BluetoothGattServer.this.mCallback.onCharacteristicReadRequest(remoteDevice, i, i2, characteristicByHandle);
                    return;
                } catch (Exception e) {
                    Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", e);
                    return;
                }
            }
            Log.w(BluetoothGattServer.TAG, "onCharacteristicReadRequest() no char for handle " + i3);
        }

        @Override
        public void onDescriptorReadRequest(String str, int i, int i2, boolean z, int i3) {
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            BluetoothGattDescriptor descriptorByHandle = BluetoothGattServer.this.getDescriptorByHandle(i3);
            if (descriptorByHandle != null) {
                try {
                    BluetoothGattServer.this.mCallback.onDescriptorReadRequest(remoteDevice, i, i2, descriptorByHandle);
                    return;
                } catch (Exception e) {
                    Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", e);
                    return;
                }
            }
            Log.w(BluetoothGattServer.TAG, "onDescriptorReadRequest() no desc for handle " + i3);
        }

        @Override
        public void onCharacteristicWriteRequest(String str, int i, int i2, int i3, boolean z, boolean z2, int i4, byte[] bArr) {
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            BluetoothGattCharacteristic characteristicByHandle = BluetoothGattServer.this.getCharacteristicByHandle(i4);
            if (characteristicByHandle != null) {
                try {
                    BluetoothGattServer.this.mCallback.onCharacteristicWriteRequest(remoteDevice, i, characteristicByHandle, z, z2, i2, bArr);
                    return;
                } catch (Exception e) {
                    Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", e);
                    return;
                }
            }
            Log.w(BluetoothGattServer.TAG, "onCharacteristicWriteRequest() no char for handle " + i4);
        }

        @Override
        public void onDescriptorWriteRequest(String str, int i, int i2, int i3, boolean z, boolean z2, int i4, byte[] bArr) {
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            BluetoothGattDescriptor descriptorByHandle = BluetoothGattServer.this.getDescriptorByHandle(i4);
            if (descriptorByHandle != null) {
                try {
                    BluetoothGattServer.this.mCallback.onDescriptorWriteRequest(remoteDevice, i, descriptorByHandle, z, z2, i2, bArr);
                    return;
                } catch (Exception e) {
                    Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", e);
                    return;
                }
            }
            Log.w(BluetoothGattServer.TAG, "onDescriptorWriteRequest() no desc for handle " + i4);
        }

        @Override
        public void onExecuteWrite(String str, int i, boolean z) {
            Log.d(BluetoothGattServer.TAG, "onExecuteWrite() - device=" + str + ", transId=" + i + "execWrite=" + z);
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            if (remoteDevice == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onExecuteWrite(remoteDevice, i, z);
            } catch (Exception e) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception in callback", e);
            }
        }

        @Override
        public void onNotificationSent(String str, int i) {
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            if (remoteDevice == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onNotificationSent(remoteDevice, i);
            } catch (Exception e) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception: " + e);
            }
        }

        @Override
        public void onMtuChanged(String str, int i) {
            Log.d(BluetoothGattServer.TAG, "onMtuChanged() - device=" + str + ", mtu=" + i);
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            if (remoteDevice == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onMtuChanged(remoteDevice, i);
            } catch (Exception e) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception: " + e);
            }
        }

        @Override
        public void onPhyUpdate(String str, int i, int i2, int i3) {
            Log.d(BluetoothGattServer.TAG, "onPhyUpdate() - device=" + str + ", txPHy=" + i + ", rxPHy=" + i2);
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            if (remoteDevice == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onPhyUpdate(remoteDevice, i, i2, i3);
            } catch (Exception e) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception: " + e);
            }
        }

        @Override
        public void onPhyRead(String str, int i, int i2, int i3) {
            Log.d(BluetoothGattServer.TAG, "onPhyUpdate() - device=" + str + ", txPHy=" + i + ", rxPHy=" + i2);
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            if (remoteDevice == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onPhyRead(remoteDevice, i, i2, i3);
            } catch (Exception e) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception: " + e);
            }
        }

        @Override
        public void onConnectionUpdated(String str, int i, int i2, int i3, int i4) {
            Log.d(BluetoothGattServer.TAG, "onConnectionUpdated() - Device=" + str + " interval=" + i + " latency=" + i2 + " timeout=" + i3 + " status=" + i4);
            BluetoothDevice remoteDevice = BluetoothGattServer.this.mAdapter.getRemoteDevice(str);
            if (remoteDevice == null) {
                return;
            }
            try {
                BluetoothGattServer.this.mCallback.onConnectionUpdated(remoteDevice, i, i2, i3, i4);
            } catch (Exception e) {
                Log.w(BluetoothGattServer.TAG, "Unhandled exception: " + e);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothGattServerCallback mCallback = null;
    private int mServerIf = 0;
    private List<BluetoothGattService> mServices = new ArrayList();

    BluetoothGattServer(IBluetoothGatt iBluetoothGatt, int i) {
        this.mService = iBluetoothGatt;
        this.mTransport = i;
    }

    BluetoothGattCharacteristic getCharacteristicByHandle(int i) {
        Iterator<BluetoothGattService> it = this.mServices.iterator();
        while (it.hasNext()) {
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : it.next().getCharacteristics()) {
                if (bluetoothGattCharacteristic.getInstanceId() == i) {
                    return bluetoothGattCharacteristic;
                }
            }
        }
        return null;
    }

    BluetoothGattDescriptor getDescriptorByHandle(int i) {
        Iterator<BluetoothGattService> it = this.mServices.iterator();
        while (it.hasNext()) {
            Iterator<BluetoothGattCharacteristic> it2 = it.next().getCharacteristics().iterator();
            while (it2.hasNext()) {
                for (BluetoothGattDescriptor bluetoothGattDescriptor : it2.next().getDescriptors()) {
                    if (bluetoothGattDescriptor.getInstanceId() == i) {
                        return bluetoothGattDescriptor;
                    }
                }
            }
        }
        return null;
    }

    public void close() {
        Log.d(TAG, "close()");
        unregisterCallback();
    }

    boolean registerCallback(BluetoothGattServerCallback bluetoothGattServerCallback) {
        Log.d(TAG, "registerCallback()");
        if (this.mService == null) {
            Log.e(TAG, "GATT service not available");
            return false;
        }
        UUID uuidRandomUUID = UUID.randomUUID();
        Log.d(TAG, "registerCallback() - UUID=" + uuidRandomUUID);
        synchronized (this.mServerIfLock) {
            if (this.mCallback != null) {
                Log.e(TAG, "App can register callback only once");
                return false;
            }
            this.mCallback = bluetoothGattServerCallback;
            try {
                this.mService.registerServer(new ParcelUuid(uuidRandomUUID), this.mBluetoothGattServerCallback);
                try {
                    this.mServerIfLock.wait(JobInfo.MIN_BACKOFF_MILLIS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "" + e);
                    this.mCallback = null;
                }
                if (this.mServerIf == 0) {
                    this.mCallback = null;
                    return false;
                }
                return true;
            } catch (RemoteException e2) {
                Log.e(TAG, "", e2);
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
            Log.e(TAG, "", e);
        }
    }

    BluetoothGattService getService(UUID uuid, int i, int i2) {
        for (BluetoothGattService bluetoothGattService : this.mServices) {
            if (bluetoothGattService.getType() == i2 && bluetoothGattService.getInstanceId() == i && bluetoothGattService.getUuid().equals(uuid)) {
                return bluetoothGattService;
            }
        }
        return null;
    }

    public boolean connect(BluetoothDevice bluetoothDevice, boolean z) {
        Log.d(TAG, "connect() - device: " + bluetoothDevice.getAddress() + ", auto: " + z);
        if (this.mService == null || this.mServerIf == 0) {
            return false;
        }
        try {
            this.mService.serverConnect(this.mServerIf, bluetoothDevice.getAddress(), !z, this.mTransport);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public void cancelConnection(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "cancelConnection() - device: " + bluetoothDevice.getAddress());
        if (this.mService == null || this.mServerIf == 0) {
            return;
        }
        try {
            this.mService.serverDisconnect(this.mServerIf, bluetoothDevice.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    public void setPreferredPhy(BluetoothDevice bluetoothDevice, int i, int i2, int i3) {
        try {
            this.mService.serverSetPreferredPhy(this.mServerIf, bluetoothDevice.getAddress(), i, i2, i3);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    public void readPhy(BluetoothDevice bluetoothDevice) {
        try {
            this.mService.serverReadPhy(this.mServerIf, bluetoothDevice.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    public boolean sendResponse(BluetoothDevice bluetoothDevice, int i, int i2, int i3, byte[] bArr) {
        if (this.mService == null || this.mServerIf == 0) {
            return false;
        }
        try {
            this.mService.sendResponse(this.mServerIf, bluetoothDevice.getAddress(), i, i2, i3, bArr);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean notifyCharacteristicChanged(BluetoothDevice bluetoothDevice, BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z) {
        if (this.mService == null || this.mServerIf == 0 || bluetoothGattCharacteristic.getService() == null) {
            return false;
        }
        if (bluetoothGattCharacteristic.getValue() == null) {
            throw new IllegalArgumentException("Chracteristic value is empty. Use BluetoothGattCharacteristic#setvalue to update");
        }
        try {
            this.mService.sendNotification(this.mServerIf, bluetoothDevice.getAddress(), bluetoothGattCharacteristic.getInstanceId(), z, bluetoothGattCharacteristic.getValue());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean addService(BluetoothGattService bluetoothGattService) {
        Log.d(TAG, "addService() - service: " + bluetoothGattService.getUuid());
        if (this.mService == null || this.mServerIf == 0) {
            return false;
        }
        this.mPendingService = bluetoothGattService;
        try {
            this.mService.addService(this.mServerIf, bluetoothGattService);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean removeService(BluetoothGattService bluetoothGattService) {
        BluetoothGattService service;
        Log.d(TAG, "removeService() - service: " + bluetoothGattService.getUuid());
        if (this.mService == null || this.mServerIf == 0 || (service = getService(bluetoothGattService.getUuid(), bluetoothGattService.getInstanceId(), bluetoothGattService.getType())) == null) {
            return false;
        }
        try {
            this.mService.removeService(this.mServerIf, bluetoothGattService.getInstanceId());
            this.mServices.remove(service);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
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
            Log.e(TAG, "", e);
        }
    }

    public List<BluetoothGattService> getServices() {
        return this.mServices;
    }

    public BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService bluetoothGattService : this.mServices) {
            if (bluetoothGattService.getUuid().equals(uuid)) {
                return bluetoothGattService;
            }
        }
        return null;
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectedDevices instead.");
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        throw new UnsupportedOperationException("Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}
