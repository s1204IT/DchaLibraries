package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidHost;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import com.android.settingslib.R;
import java.util.List;

/* loaded from: classes.dex */
public class HidProfile implements LocalBluetoothProfile {
    private static boolean V = true;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothHidHost mService;

    private final class HidHostServiceListener implements BluetoothProfile.ServiceListener {
        private HidHostServiceListener() {
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (HidProfile.V) {
                Log.d("HidProfile", "Bluetooth service connected");
            }
            HidProfile.this.mService = (BluetoothHidHost) bluetoothProfile;
            List connectedDevices = HidProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice cachedBluetoothDeviceFindDevice = HidProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (cachedBluetoothDeviceFindDevice == null) {
                    Log.w("HidProfile", "HidProfile found new device: " + bluetoothDevice);
                    cachedBluetoothDeviceFindDevice = HidProfile.this.mDeviceManager.addDevice(HidProfile.this.mLocalAdapter, HidProfile.this.mProfileManager, bluetoothDevice);
                }
                cachedBluetoothDeviceFindDevice.onProfileStateChanged(HidProfile.this, 2);
                cachedBluetoothDeviceFindDevice.refresh();
            }
            HidProfile.this.mIsProfileReady = true;
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceDisconnected(int i) {
            if (HidProfile.V) {
                Log.d("HidProfile", "Bluetooth service disconnected");
            }
            HidProfile.this.mIsProfileReady = false;
        }
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getProfileId() {
        return 4;
    }

    HidProfile(Context context, LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        localBluetoothAdapter.getProfileProxy(context, new HidHostServiceListener(), 4);
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
        if (this.mService == null) {
            return false;
        }
        return this.mService.connect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return 0;
        }
        List connectedDevices = this.mService.getConnectedDevices();
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
        return "HID";
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getNameResource(BluetoothDevice bluetoothDevice) {
        return R.string.bluetooth_profile_hid;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getDrawableResource(BluetoothClass bluetoothClass) {
        if (bluetoothClass == null) {
            return R.drawable.ic_lockscreen_ime;
        }
        return getHidClassDrawable(bluetoothClass);
    }

    public static int getHidClassDrawable(BluetoothClass bluetoothClass) {
        int deviceClass = bluetoothClass.getDeviceClass();
        if (deviceClass != 1344) {
            if (deviceClass == 1408) {
                return R.drawable.ic_bt_pointing_hid;
            }
            if (deviceClass != 1472) {
                return R.drawable.ic_bt_misc_hid;
            }
        }
        return R.drawable.ic_lockscreen_ime;
    }

    protected void finalize() {
        if (V) {
            Log.d("HidProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(4, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("HidProfile", "Error cleaning up HID proxy", th);
            }
        }
    }
}
