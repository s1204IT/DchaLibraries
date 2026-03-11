package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import com.android.settingslib.R;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class PanProfile implements LocalBluetoothProfile {
    private static boolean V = true;
    private final HashMap<BluetoothDevice, Integer> mDeviceRoleMap = new HashMap<>();
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private BluetoothPan mService;

    private final class PanServiceListener implements BluetoothProfile.ServiceListener {
        private PanServiceListener() {
        }

        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (PanProfile.V) {
                Log.d("PanProfile", "Bluetooth service connected");
            }
            PanProfile.this.mService = (BluetoothPan) bluetoothProfile;
            PanProfile.this.mIsProfileReady = true;
        }

        @Override
        public void onServiceDisconnected(int i) {
            if (PanProfile.V) {
                Log.d("PanProfile", "Bluetooth service disconnected");
            }
            PanProfile.this.mIsProfileReady = false;
        }
    }

    @Override
    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    @Override
    public int getProfileId() {
        return 5;
    }

    PanProfile(Context context, LocalBluetoothAdapter localBluetoothAdapter) {
        this.mLocalAdapter = localBluetoothAdapter;
        this.mLocalAdapter.getProfileProxy(context, new PanServiceListener(), 5);
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isAutoConnectable() {
        return false;
    }

    @Override
    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        List connectedDevices = this.mService.getConnectedDevices();
        if (connectedDevices != null) {
            Iterator it = connectedDevices.iterator();
            while (it.hasNext()) {
                this.mService.disconnect((BluetoothDevice) it.next());
            }
        }
        return this.mService.connect(bluetoothDevice);
    }

    @Override
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
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
        return true;
    }

    @Override
    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
    }

    public String toString() {
        return "PAN";
    }

    @Override
    public int getNameResource(BluetoothDevice bluetoothDevice) {
        if (isLocalRoleNap(bluetoothDevice)) {
            return R.string.bluetooth_profile_pan_nap;
        }
        return R.string.bluetooth_profile_pan;
    }

    @Override
    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R.drawable.ic_bt_network_pan;
    }

    void setLocalRole(BluetoothDevice bluetoothDevice, int i) {
        this.mDeviceRoleMap.put(bluetoothDevice, Integer.valueOf(i));
    }

    boolean isLocalRoleNap(BluetoothDevice bluetoothDevice) {
        return this.mDeviceRoleMap.containsKey(bluetoothDevice) && this.mDeviceRoleMap.get(bluetoothDevice).intValue() == 1;
    }

    protected void finalize() {
        if (V) {
            Log.d("PanProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(5, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("PanProfile", "Error cleaning up PAN proxy", th);
            }
        }
    }
}
