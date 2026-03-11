package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class MapClientProfile implements LocalBluetoothProfile {
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothMapClient mService;
    private static boolean V = false;
    static final ParcelUuid[] UUIDS = {BluetoothUuid.MAP, BluetoothUuid.MNS, BluetoothUuid.MAS};

    private final class MapClientServiceListener implements BluetoothProfile.ServiceListener {
        private MapClientServiceListener() {
        }

        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (MapClientProfile.V) {
                Log.d("MapClientProfile", "Bluetooth service connected");
            }
            MapClientProfile.this.mService = (BluetoothMapClient) bluetoothProfile;
            List connectedDevices = MapClientProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice cachedBluetoothDeviceFindDevice = MapClientProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (cachedBluetoothDeviceFindDevice == null) {
                    Log.w("MapClientProfile", "MapProfile found new device: " + bluetoothDevice);
                    cachedBluetoothDeviceFindDevice = MapClientProfile.this.mDeviceManager.addDevice(MapClientProfile.this.mLocalAdapter, MapClientProfile.this.mProfileManager, bluetoothDevice);
                }
                cachedBluetoothDeviceFindDevice.onProfileStateChanged(MapClientProfile.this, 2);
                cachedBluetoothDeviceFindDevice.refresh();
            }
            MapClientProfile.this.mProfileManager.callServiceConnectedListeners();
            MapClientProfile.this.mIsProfileReady = true;
        }

        @Override
        public void onServiceDisconnected(int i) {
            if (MapClientProfile.V) {
                Log.d("MapClientProfile", "Bluetooth service disconnected");
            }
            MapClientProfile.this.mProfileManager.callServiceDisconnectedListeners();
            MapClientProfile.this.mIsProfileReady = false;
        }
    }

    @Override
    public int getProfileId() {
        return 18;
    }

    MapClientProfile(Context context, LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        this.mLocalAdapter.getProfileProxy(context, new MapClientServiceListener(), 18);
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isAutoConnectable() {
        return true;
    }

    @Override
    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> connectedDevices = getConnectedDevices();
        if (connectedDevices != null && connectedDevices.contains(bluetoothDevice)) {
            Log.d("MapClientProfile", "Ignoring Connect");
            return true;
        }
        return this.mService.connect(bluetoothDevice);
    }

    @Override
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        if (this.mService.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    @Override
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return 0;
        }
        return this.mService.getConnectionState(bluetoothDevice);
    }

    @Override
    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return this.mService != null && this.mService.getPriority(bluetoothDevice) > 0;
    }

    @Override
    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        if (this.mService == null) {
            return;
        }
        if (z) {
            if (this.mService.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
                return;
            }
            return;
        }
        this.mService.setPriority(bluetoothDevice, 0);
    }

    public List<BluetoothDevice> getConnectedDevices() {
        return this.mService == null ? new ArrayList(0) : this.mService.getDevicesMatchingConnectionStates(new int[]{2, 1, 3});
    }

    public String toString() {
        return "MAP Client";
    }

    protected void finalize() {
        if (V) {
            Log.d("MapClientProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(18, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("MapClientProfile", "Error cleaning up MAP Client proxy", th);
            }
        }
    }
}
