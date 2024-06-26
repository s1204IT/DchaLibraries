package com.android.settingslib.bluetooth;
/* loaded from: classes.dex */
public interface BluetoothCallback {
    void onBluetoothStateChanged(int i);

    void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i);

    void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice);

    void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i);

    void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice);

    void onScanningStateChanged(boolean z);
}
