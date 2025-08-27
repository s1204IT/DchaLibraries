package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;

/* loaded from: classes.dex */
public interface LocalBluetoothProfile {
    boolean connect(BluetoothDevice bluetoothDevice);

    boolean disconnect(BluetoothDevice bluetoothDevice);

    int getConnectionStatus(BluetoothDevice bluetoothDevice);

    int getProfileId();

    boolean isAutoConnectable();

    boolean isConnectable();

    boolean isPreferred(BluetoothDevice bluetoothDevice);

    void setPreferred(BluetoothDevice bluetoothDevice, boolean z);
}
