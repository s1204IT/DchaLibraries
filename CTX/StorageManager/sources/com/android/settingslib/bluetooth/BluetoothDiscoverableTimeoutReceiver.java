package com.android.settingslib.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothDiscoverableTimeoutReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals("android.bluetooth.intent.DISCOVERABLE_TIMEOUT")) {
            return;
        }
        LocalBluetoothAdapter localBluetoothAdapter = LocalBluetoothAdapter.getInstance();
        if (localBluetoothAdapter != null && localBluetoothAdapter.getState() == 12) {
            Log.d("BluetoothDiscoverableTimeoutReceiver", "Disable discoverable...");
            localBluetoothAdapter.setScanMode(21);
        } else {
            Log.e("BluetoothDiscoverableTimeoutReceiver", "localBluetoothAdapter is NULL!!");
        }
    }
}
