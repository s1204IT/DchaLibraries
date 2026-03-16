package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;

class HeadsetVendorSpecificResultCode {
    String mArg;
    String mCommand;
    BluetoothDevice mDevice;

    public HeadsetVendorSpecificResultCode(BluetoothDevice device, String command, String arg) {
        this.mDevice = device;
        this.mCommand = command;
        this.mArg = arg;
    }
}
