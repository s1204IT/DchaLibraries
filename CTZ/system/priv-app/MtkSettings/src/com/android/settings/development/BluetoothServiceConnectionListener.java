package com.android.settings.development;

import android.bluetooth.BluetoothA2dp;

/* loaded from: classes.dex */
public interface BluetoothServiceConnectionListener {
    void onBluetoothCodecUpdated();

    void onBluetoothServiceConnected(BluetoothA2dp bluetoothA2dp);

    void onBluetoothServiceDisconnected();
}
