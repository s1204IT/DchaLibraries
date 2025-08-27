package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

/* loaded from: classes.dex */
public interface LocalBluetoothProfile {
    boolean connect(BluetoothDevice bluetoothDevice);

    boolean disconnect(BluetoothDevice bluetoothDevice);

    int getConnectionStatus(BluetoothDevice bluetoothDevice);

    int getDrawableResource(BluetoothClass bluetoothClass);

    int getNameResource(BluetoothDevice bluetoothDevice);

    int getProfileId();

    boolean isAutoConnectable();

    boolean isConnectable();

    boolean isPreferred(BluetoothDevice bluetoothDevice);

    boolean isProfileReady();

    void setPreferred(BluetoothDevice bluetoothDevice, boolean z);
}
