package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class HearingAidProfile implements LocalBluetoothProfile {
    private static boolean V = true;
    private Context mContext;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothHearingAid mService;

    private final class HearingAidServiceListener implements BluetoothProfile.ServiceListener {
        private HearingAidServiceListener() {
        }

        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (HearingAidProfile.V) {
                Log.d("HearingAidProfile", "Bluetooth service connected");
            }
            HearingAidProfile.this.mService = (BluetoothHearingAid) bluetoothProfile;
            List<BluetoothDevice> connectedDevices = HearingAidProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDeviceRemove = connectedDevices.remove(0);
                CachedBluetoothDevice cachedBluetoothDeviceFindDevice = HearingAidProfile.this.mDeviceManager.findDevice(bluetoothDeviceRemove);
                if (cachedBluetoothDeviceFindDevice == null) {
                    if (HearingAidProfile.V) {
                        Log.d("HearingAidProfile", "HearingAidProfile found new device: " + bluetoothDeviceRemove);
                    }
                    cachedBluetoothDeviceFindDevice = HearingAidProfile.this.mDeviceManager.addDevice(HearingAidProfile.this.mLocalAdapter, HearingAidProfile.this.mProfileManager, bluetoothDeviceRemove);
                }
                cachedBluetoothDeviceFindDevice.onProfileStateChanged(HearingAidProfile.this, 2);
                cachedBluetoothDeviceFindDevice.refresh();
            }
            HearingAidProfile.this.mDeviceManager.updateHearingAidsDevices(HearingAidProfile.this.mProfileManager);
            HearingAidProfile.this.mIsProfileReady = true;
        }

        @Override
        public void onServiceDisconnected(int i) {
            if (HearingAidProfile.V) {
                Log.d("HearingAidProfile", "Bluetooth service disconnected");
            }
            HearingAidProfile.this.mIsProfileReady = false;
        }
    }

    @Override
    public int getProfileId() {
        return 21;
    }

    HearingAidProfile(Context context, LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mContext = context;
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        this.mLocalAdapter.getProfileProxy(context, new HearingAidServiceListener(), 21);
    }

    @Override
    public boolean isConnectable() {
        return false;
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

    public List<BluetoothDevice> getActiveDevices() {
        return this.mService == null ? new ArrayList() : this.mService.getActiveDevices();
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

    public long getHiSyncId(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return 0L;
        }
        return this.mService.getHiSyncId(bluetoothDevice);
    }

    public String toString() {
        return "HearingAid";
    }

    protected void finalize() {
        if (V) {
            Log.d("HearingAidProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(21, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("HearingAidProfile", "Error cleaning up Hearing Aid proxy", th);
            }
        }
    }
}
