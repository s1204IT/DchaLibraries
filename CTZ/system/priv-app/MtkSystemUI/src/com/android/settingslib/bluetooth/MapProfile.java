package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMap;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.List;

/* loaded from: classes.dex */
public class MapProfile implements LocalBluetoothProfile {
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothMap mService;
    private static boolean V = true;
    static final ParcelUuid[] UUIDS = {BluetoothUuid.MAP, BluetoothUuid.MNS, BluetoothUuid.MAS};

    private final class MapServiceListener implements BluetoothProfile.ServiceListener {
        private MapServiceListener() {
        }

        /* synthetic */ MapServiceListener(MapProfile mapProfile, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (MapProfile.V) {
                Log.d("MapProfile", "Bluetooth service connected");
            }
            MapProfile.this.mService = (BluetoothMap) bluetoothProfile;
            List connectedDevices = MapProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice cachedBluetoothDeviceFindDevice = MapProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (cachedBluetoothDeviceFindDevice == null) {
                    Log.w("MapProfile", "MapProfile found new device: " + bluetoothDevice);
                    cachedBluetoothDeviceFindDevice = MapProfile.this.mDeviceManager.addDevice(MapProfile.this.mLocalAdapter, MapProfile.this.mProfileManager, bluetoothDevice);
                }
                cachedBluetoothDeviceFindDevice.onProfileStateChanged(MapProfile.this, 2);
                cachedBluetoothDeviceFindDevice.refresh();
            }
            MapProfile.this.mProfileManager.callServiceConnectedListeners();
            MapProfile.this.mIsProfileReady = true;
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceDisconnected(int i) {
            if (MapProfile.V) {
                Log.d("MapProfile", "Bluetooth service disconnected");
            }
            MapProfile.this.mProfileManager.callServiceDisconnectedListeners();
            MapProfile.this.mIsProfileReady = false;
        }
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getProfileId() {
        return 9;
    }

    MapProfile(Context context, LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        this.mLocalAdapter.getProfileProxy(context, new MapServiceListener(), 9);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isConnectable() {
        return true;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isAutoConnectable() {
        return true;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (V) {
            Log.d("MapProfile", "connect() - should not get called");
            return false;
        }
        return false;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        List connectedDevices = this.mService.getConnectedDevices();
        if (connectedDevices.isEmpty() || !((BluetoothDevice) connectedDevices.get(0)).equals(bluetoothDevice)) {
            return false;
        }
        if (this.mService.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return 0;
        }
        List connectedDevices = this.mService.getConnectedDevices();
        if (V) {
            Log.d("MapProfile", "getConnectionStatus: status is: " + this.mService.getConnectionState(bluetoothDevice));
        }
        if (connectedDevices.isEmpty() || !((BluetoothDevice) connectedDevices.get(0)).equals(bluetoothDevice)) {
            return 0;
        }
        return this.mService.getConnectionState(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return this.mService != null && this.mService.getPriority(bluetoothDevice) > 0;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
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

    public String toString() {
        return "MAP";
    }

    protected void finalize() {
        if (V) {
            Log.d("MapProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(9, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("MapProfile", "Error cleaning up MAP proxy", th);
            }
        }
    }
}
