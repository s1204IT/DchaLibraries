package com.android.server.telecom.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.telecom.Log;
import com.android.server.telecom.BluetoothAdapterProxy;
import com.android.server.telecom.BluetoothHeadsetProxy;
import com.android.server.telecom.TelecomSystem;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class BluetoothDeviceManager {
    private BluetoothHeadsetProxy mBluetoothHeadsetService;
    private BluetoothRouteManager mBluetoothRouteManager;
    private final TelecomSystem.SyncRoot mLock;
    private final BluetoothProfile.ServiceListener mBluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            Log.startSession("BMSL.oSC");
            try {
                synchronized (BluetoothDeviceManager.this.mLock) {
                    try {
                        if (i == 1) {
                            BluetoothDeviceManager.this.mBluetoothHeadsetService = new BluetoothHeadsetProxy((BluetoothHeadset) bluetoothProfile);
                            Log.i(this, "- Got BluetoothHeadset: " + BluetoothDeviceManager.this.mBluetoothHeadsetService, new Object[0]);
                        } else {
                            Log.w(this, "Connected to non-headset bluetooth service. Not changing bluetooth headset.", new Object[0]);
                        }
                    } finally {
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        @Override
        public void onServiceDisconnected(int i) {
            Log.startSession("BMSL.oSD");
            try {
                synchronized (BluetoothDeviceManager.this.mLock) {
                    BluetoothDeviceManager.this.mBluetoothHeadsetService = null;
                    Log.i(BluetoothDeviceManager.this, "Lost BluetoothHeadset service. Removing all tracked devices.", new Object[0]);
                    LinkedList linkedList = new LinkedList(BluetoothDeviceManager.this.mConnectedDevicesByAddress.values());
                    BluetoothDeviceManager.this.mConnectedDevicesByAddress.clear();
                    Iterator it = linkedList.iterator();
                    while (it.hasNext()) {
                        BluetoothDeviceManager.this.mBluetoothRouteManager.onDeviceLost(((BluetoothDevice) it.next()).getAddress());
                    }
                }
            } finally {
                Log.endSession();
            }
        }
    };
    private final LinkedHashMap<String, BluetoothDevice> mConnectedDevicesByAddress = new LinkedHashMap<>();

    public BluetoothDeviceManager(Context context, BluetoothAdapterProxy bluetoothAdapterProxy, TelecomSystem.SyncRoot syncRoot) {
        this.mLock = syncRoot;
        if (bluetoothAdapterProxy != null) {
            bluetoothAdapterProxy.getProfileProxy(context, this.mBluetoothProfileServiceListener, 1);
        }
    }

    public void setBluetoothRouteManager(BluetoothRouteManager bluetoothRouteManager) {
        this.mBluetoothRouteManager = bluetoothRouteManager;
    }

    public int getNumConnectedDevices() {
        return this.mConnectedDevicesByAddress.size();
    }

    public Collection<BluetoothDevice> getConnectedDevices() {
        return this.mConnectedDevicesByAddress.values();
    }

    public BluetoothHeadsetProxy getHeadsetService() {
        return this.mBluetoothHeadsetService;
    }

    public BluetoothDevice getDeviceFromAddress(String str) {
        return this.mConnectedDevicesByAddress.get(str);
    }

    void onDeviceConnected(BluetoothDevice bluetoothDevice) {
        synchronized (this.mLock) {
            if (!this.mConnectedDevicesByAddress.containsKey(bluetoothDevice.getAddress())) {
                this.mConnectedDevicesByAddress.put(bluetoothDevice.getAddress(), bluetoothDevice);
                this.mBluetoothRouteManager.onDeviceAdded(bluetoothDevice.getAddress());
            }
        }
    }

    void onDeviceDisconnected(BluetoothDevice bluetoothDevice) {
        synchronized (this.mLock) {
            if (this.mConnectedDevicesByAddress.containsKey(bluetoothDevice.getAddress())) {
                this.mConnectedDevicesByAddress.remove(bluetoothDevice.getAddress());
                this.mBluetoothRouteManager.onDeviceLost(bluetoothDevice.getAddress());
            }
        }
    }
}
