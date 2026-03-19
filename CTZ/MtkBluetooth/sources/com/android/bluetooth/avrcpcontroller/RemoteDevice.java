package com.android.bluetooth.avrcpcontroller;

import android.bluetooth.BluetoothDevice;
import com.android.bluetooth.Utils;

class RemoteDevice {
    private static final int FEAT_ABSOLUTE_VOLUME = 2;
    private static final int FEAT_BROWSE = 4;
    private static final int FEAT_METADATA = 1;
    private static final int FEAT_NONE = 0;
    private static final int VOLUME_LABEL_UNDEFINED = -1;
    final BluetoothDevice mBTDevice;
    private int mRemoteFeatures = 0;
    private boolean mAbsVolNotificationRequested = false;
    private int mNotificationLabel = -1;
    private boolean mFirstAbsVolCmdRecvd = false;

    RemoteDevice(BluetoothDevice bluetoothDevice) {
        this.mBTDevice = bluetoothDevice;
    }

    synchronized void setRemoteFeatures(int i) {
        this.mRemoteFeatures = i;
    }

    public synchronized byte[] getBluetoothAddress() {
        return Utils.getByteAddress(this.mBTDevice);
    }

    public synchronized void setNotificationLabel(int i) {
        this.mNotificationLabel = i;
    }

    public synchronized int getNotificationLabel() {
        return this.mNotificationLabel;
    }

    public synchronized void setAbsVolNotificationRequested(boolean z) {
        this.mAbsVolNotificationRequested = z;
    }

    public synchronized boolean getAbsVolNotificationRequested() {
        return this.mAbsVolNotificationRequested;
    }

    public synchronized void setFirstAbsVolCmdRecvd() {
        this.mFirstAbsVolCmdRecvd = true;
    }

    public synchronized boolean getFirstAbsVolCmdRecvd() {
        return this.mFirstAbsVolCmdRecvd;
    }
}
