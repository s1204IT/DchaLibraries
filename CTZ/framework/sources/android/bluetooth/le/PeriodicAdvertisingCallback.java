package android.bluetooth.le;

import android.bluetooth.BluetoothDevice;

public abstract class PeriodicAdvertisingCallback {
    public static final int SYNC_NO_RESOURCES = 2;
    public static final int SYNC_NO_RESPONSE = 1;
    public static final int SYNC_SUCCESS = 0;

    public void onSyncEstablished(int i, BluetoothDevice bluetoothDevice, int i2, int i3, int i4, int i5) {
    }

    public void onPeriodicAdvertisingReport(PeriodicAdvertisingReport periodicAdvertisingReport) {
    }

    public void onSyncLost(int i) {
    }
}
