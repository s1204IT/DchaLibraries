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
    public boolean connect(BluetoothDevice device) {
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        return false;
    }

    @Override
    public int getConnectionStatus(BluetoothDevice device) {
        return 0;
    }

    @Override
    public boolean isPreferred(BluetoothDevice device) {
        return false;
    }

    @Override
    public void setPreferred(BluetoothDevice device, boolean preferred) {
    }

    public String toString() {
        return "OPP";
    }
}
