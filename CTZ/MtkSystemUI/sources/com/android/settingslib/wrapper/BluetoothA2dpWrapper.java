package com.android.settingslib.wrapper;

import android.bluetooth.BluetoothA2dp;

public class BluetoothA2dpWrapper {
    private BluetoothA2dp mService;

    public BluetoothA2dpWrapper(BluetoothA2dp bluetoothA2dp) {
        this.mService = bluetoothA2dp;
    }
}
