package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;

final class OppProfile implements LocalBluetoothProfile {
    OppProfile() {
    }

    @Override
    public boolean isConnectable() {
        return false;
    }

    @Override
    public boolean isAutoConnectable() {
        return false;
    }

    @Override
    public boolean connect(BluetoothDevice bluetoothDevice) {
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        return false;
    }

    @Override
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        return 0;
    }

    @Override
    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return false;
    }

    @Override
    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
    }

    @Override
    public int getProfileId() {
        return 20;
    }

    public String toString() {
        return "OPP";
    }
}
