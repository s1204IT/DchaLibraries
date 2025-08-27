package com.android.settingslib.wrapper;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;

/* loaded from: classes.dex */
public class BluetoothA2dpWrapper {
    private BluetoothA2dp mService;

    public BluetoothA2dpWrapper(BluetoothA2dp bluetoothA2dp) {
        this.mService = bluetoothA2dp;
    }

    public BluetoothCodecStatus getCodecStatus(BluetoothDevice bluetoothDevice) {
        return this.mService.getCodecStatus(bluetoothDevice);
    }

    public int supportsOptionalCodecs(BluetoothDevice bluetoothDevice) {
        return this.mService.supportsOptionalCodecs(bluetoothDevice);
    }

    public int getOptionalCodecsEnabled(BluetoothDevice bluetoothDevice) {
        return this.mService.getOptionalCodecsEnabled(bluetoothDevice);
    }

    public void setOptionalCodecsEnabled(BluetoothDevice bluetoothDevice, int i) {
        this.mService.setOptionalCodecsEnabled(bluetoothDevice, i);
    }
}
