package com.android.settings.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import com.android.settings.R;

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
    public int getPreferred(BluetoothDevice device) {
        return 0;
    }

    @Override
    public void setPreferred(BluetoothDevice device, boolean preferred) {
    }

    @Override
    public boolean isProfileReady() {
        return true;
    }

    public String toString() {
        return "OPP";
    }

    @Override
    public int getOrdinal() {
        return 2;
    }

    @Override
    public int getNameResource(BluetoothDevice device) {
        return R.string.bluetooth_profile_opp;
    }

    @Override
    public int getDrawableResource(BluetoothClass btClass) {
        return 0;
    }
}
