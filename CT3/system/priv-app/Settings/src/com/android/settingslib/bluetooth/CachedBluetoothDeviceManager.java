package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/* loaded from: classes.dex */
public class CachedBluetoothDeviceManager {
    private final LocalBluetoothManager mBtManager;
    private final List<CachedBluetoothDevice> mCachedDevices = new ArrayList();
    private Context mContext;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CachedBluetoothDeviceManager(Context context, LocalBluetoothManager localBtManager) {
        this.mContext = context;
        this.mBtManager = localBtManager;
    }

    public synchronized Collection<CachedBluetoothDevice> getCachedDevicesCopy() {
        return new ArrayList(this.mCachedDevices);
    }

    public static boolean onDeviceDisappeared(CachedBluetoothDevice cachedDevice) {
        cachedDevice.setVisible(false);
        return cachedDevice.getBondState() == 10;
    }

    public void onDeviceNameUpdated(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) {
            return;
        }
        cachedDevice.refreshName();
    }

    public CachedBluetoothDevice findDevice(BluetoothDevice device) {
        for (CachedBluetoothDevice cachedDevice : this.mCachedDevices) {
            if (cachedDevice.getDevice().equals(device)) {
                return cachedDevice;
            }
        }
        return null;
    }

    public CachedBluetoothDevice addDevice(LocalBluetoothAdapter adapter, LocalBluetoothProfileManager profileManager, BluetoothDevice device) {
        CachedBluetoothDevice newDevice = new CachedBluetoothDevice(this.mContext, adapter, profileManager, device);
        synchronized (this.mCachedDevices) {
            this.mCachedDevices.add(newDevice);
            this.mBtManager.getEventManager().dispatchDeviceAdded(newDevice);
        }
        return newDevice;
    }

    public String getName(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            return cachedDevice.getName();
        }
        String name = device.getAliasName();
        if (name != null) {
            return name;
        }
        return device.getAddress();
    }

    public synchronized void clearNonBondedDevices() {
        for (int i = this.mCachedDevices.size() - 1; i >= 0; i--) {
            CachedBluetoothDevice cachedDevice = this.mCachedDevices.get(i);
            if (cachedDevice.getBondState() != 12) {
                this.mCachedDevices.remove(i);
                Log.d("CachedBluetoothDeviceManager", "Clear NonBondedDevices : " + cachedDevice.getBondState() + "     and device name is : " + cachedDevice.getName());
            }
        }
    }

    public synchronized void onScanningStateChanged(boolean started) {
        if (started) {
            for (int i = this.mCachedDevices.size() - 1; i >= 0; i--) {
                CachedBluetoothDevice cachedDevice = this.mCachedDevices.get(i);
                cachedDevice.setVisible(false);
            }
        }
    }

    public synchronized void onBtClassChanged(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.refreshBtClass();
        }
    }

    public synchronized void onUuidChanged(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.onUuidChanged();
        }
    }

    public synchronized void onBluetoothStateChanged(int bluetoothState) {
        if (bluetoothState == 13) {
            for (int i = this.mCachedDevices.size() - 1; i >= 0; i--) {
                CachedBluetoothDevice cachedDevice = this.mCachedDevices.get(i);
                if (cachedDevice.getBondState() != 12) {
                    cachedDevice.setVisible(false);
                    Log.d("CachedBluetoothDeviceManager", "Remove device for bond state : " + cachedDevice.getBondState() + "     and device name is : " + cachedDevice.getName());
                    this.mCachedDevices.remove(i);
                } else {
                    cachedDevice.clearProfileConnectionState();
                }
            }
        }
    }
}
