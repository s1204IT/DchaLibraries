package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.List;

public final class HeadsetProfile implements LocalBluetoothProfile {
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothHeadset mService;
    private static boolean V = true;
    static final ParcelUuid[] UUIDS = {BluetoothUuid.HSP, BluetoothUuid.Handsfree};

    private final class HeadsetServiceListener implements BluetoothProfile.ServiceListener {
        HeadsetServiceListener(HeadsetProfile this$0, HeadsetServiceListener headsetServiceListener) {
            this();
        }

        private HeadsetServiceListener() {
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (HeadsetProfile.V) {
                Log.d("HeadsetProfile", "Bluetooth service connected");
            }
            HeadsetProfile.this.mService = (BluetoothHeadset) proxy;
            List<BluetoothDevice> deviceList = HeadsetProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = HeadsetProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("HeadsetProfile", "HeadsetProfile found new device: " + nextDevice);
                    device = HeadsetProfile.this.mDeviceManager.addDevice(HeadsetProfile.this.mLocalAdapter, HeadsetProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(HeadsetProfile.this, 2);
                device.refresh();
            }
            HeadsetProfile.this.mProfileManager.callServiceConnectedListeners();
            HeadsetProfile.this.mIsProfileReady = true;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (HeadsetProfile.V) {
                Log.d("HeadsetProfile", "Bluetooth service disconnected");
            }
            HeadsetProfile.this.mProfileManager.callServiceDisconnectedListeners();
            HeadsetProfile.this.mIsProfileReady = false;
        }
    }

    HeadsetProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new HeadsetServiceListener(this, null), 1);
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
    public boolean connect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> sinks = this.mService.getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                Log.d("HeadsetProfile", "Not disconnecting device = " + sink);
            }
        }
        return this.mService.connect(device);
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> deviceList = this.mService.getConnectedDevices();
        if (!deviceList.isEmpty()) {
            for (BluetoothDevice dev : deviceList) {
                if (dev.equals(device)) {
                    if (V) {
                        Log.d("HeadsetProfile", "Downgrade priority as useris disconnecting the headset");
                    }
                    if (this.mService.getPriority(device) > 100) {
                        this.mService.setPriority(device, 100);
                    }
                    return this.mService.disconnect(device);
                }
            }
        }
        return false;
    }

    @Override
    public int getConnectionStatus(BluetoothDevice device) {
        if (this.mService == null) {
            return 0;
        }
        List<BluetoothDevice> deviceList = this.mService.getConnectedDevices();
        if (!deviceList.isEmpty()) {
            for (BluetoothDevice dev : deviceList) {
                if (dev.equals(device)) {
                    return this.mService.getConnectionState(device);
                }
            }
        }
        return 0;
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
        return "HEADSET";
    }

    protected void finalize() {
        if (V) {
            Log.d("HeadsetProfile", "finalize()");
        }
        if (this.mService == null) {
            return;
        }
        try {
            BluetoothAdapter.getDefaultAdapter().closeProfileProxy(1, this.mService);
            this.mService = null;
        } catch (Throwable t) {
            Log.w("HeadsetProfile", "Error cleaning up HID proxy", t);
        }
    }
}
