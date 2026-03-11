package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

final class HfpClientProfile implements LocalBluetoothProfile {
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothHeadsetClient mService;
    private static boolean V = false;
    static final ParcelUuid[] SRC_UUIDS = {BluetoothUuid.HSP_AG, BluetoothUuid.Handsfree_AG};

    private final class HfpClientServiceListener implements BluetoothProfile.ServiceListener {
        HfpClientServiceListener(HfpClientProfile this$0, HfpClientServiceListener hfpClientServiceListener) {
            this();
        }

        private HfpClientServiceListener() {
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (HfpClientProfile.V) {
                Log.d("HfpClientProfile", "Bluetooth service connected");
            }
            HfpClientProfile.this.mService = (BluetoothHeadsetClient) proxy;
            List<BluetoothDevice> deviceList = HfpClientProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = HfpClientProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("HfpClientProfile", "HfpClient profile found new device: " + nextDevice);
                    device = HfpClientProfile.this.mDeviceManager.addDevice(HfpClientProfile.this.mLocalAdapter, HfpClientProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(HfpClientProfile.this, 2);
                device.refresh();
            }
            HfpClientProfile.this.mIsProfileReady = true;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (HfpClientProfile.V) {
                Log.d("HfpClientProfile", "Bluetooth service disconnected");
            }
            HfpClientProfile.this.mIsProfileReady = false;
        }
    }

    HfpClientProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new HfpClientServiceListener(this, null), 16);
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isAutoConnectable() {
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        return this.mService == null ? new ArrayList(0) : this.mService.getDevicesMatchingConnectionStates(new int[]{2, 1, 3});
    }

    @Override
    public boolean connect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> srcs = getConnectedDevices();
        if (srcs != null) {
            for (BluetoothDevice src : srcs) {
                if (src.equals(device)) {
                    Log.d("HfpClientProfile", "Ignoring Connect");
                    return true;
                }
            }
            for (BluetoothDevice src2 : srcs) {
                this.mService.disconnect(src2);
            }
        }
        return this.mService.connect(device);
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        if (this.mService.getPriority(device) > 100) {
            this.mService.setPriority(device, 100);
        }
        return this.mService.disconnect(device);
    }

    @Override
    public int getConnectionStatus(BluetoothDevice device) {
        if (this.mService == null) {
            return 0;
        }
        return this.mService.getConnectionState(device);
    }

    @Override
    public boolean isPreferred(BluetoothDevice device) {
        return this.mService != null && this.mService.getPriority(device) > 0;
    }

    @Override
    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (this.mService == null) {
            return;
        }
        if (preferred) {
            if (this.mService.getPriority(device) >= 100) {
                return;
            }
            this.mService.setPriority(device, 100);
            return;
        }
        this.mService.setPriority(device, 0);
    }

    public String toString() {
        return "HEADSET_CLIENT";
    }

    protected void finalize() {
        if (V) {
            Log.d("HfpClientProfile", "finalize()");
        }
        if (this.mService == null) {
            return;
        }
        try {
            BluetoothAdapter.getDefaultAdapter().closeProfileProxy(16, this.mService);
            this.mService = null;
        } catch (Throwable t) {
            Log.w("HfpClientProfile", "Error cleaning up HfpClient proxy", t);
        }
    }
}
