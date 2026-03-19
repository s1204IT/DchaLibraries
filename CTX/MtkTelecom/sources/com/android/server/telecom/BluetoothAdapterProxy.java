package com.android.server.telecom;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

public class BluetoothAdapterProxy {
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public boolean getProfileProxy(Context context, BluetoothProfile.ServiceListener serviceListener, int i) {
        if (this.mBluetoothAdapter == null) {
            return false;
        }
        return this.mBluetoothAdapter.getProfileProxy(context, serviceListener, i);
    }
}
