package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;

public class LocalBluetoothAdapter {
    private static LocalBluetoothAdapter sInstance;
    private final BluetoothAdapter mAdapter;
    private int mState = Integer.MIN_VALUE;

    private LocalBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
        this.mAdapter = bluetoothAdapter;
    }

    static synchronized LocalBluetoothAdapter getInstance() {
        BluetoothAdapter defaultAdapter;
        if (sInstance == null && (defaultAdapter = BluetoothAdapter.getDefaultAdapter()) != null) {
            sInstance = new LocalBluetoothAdapter(defaultAdapter);
        }
        return sInstance;
    }

    public int getState() {
        return this.mAdapter.getState();
    }

    public void setScanMode(int i) {
        this.mAdapter.setScanMode(i);
    }
}
