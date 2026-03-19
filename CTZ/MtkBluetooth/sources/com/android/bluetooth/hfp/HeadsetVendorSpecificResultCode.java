package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;

class HeadsetVendorSpecificResultCode extends HeadsetMessageObject {
    String mArg;
    String mCommand;
    BluetoothDevice mDevice;

    HeadsetVendorSpecificResultCode(BluetoothDevice bluetoothDevice, String str, String str2) {
        this.mDevice = bluetoothDevice;
        this.mCommand = str;
        this.mArg = str2;
    }

    @Override
    public void buildString(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append(getClass().getSimpleName());
        sb.append("[device=");
        sb.append(this.mDevice);
        sb.append(", command=");
        sb.append(this.mCommand);
        sb.append(", arg=");
        sb.append(this.mArg);
        sb.append("]");
    }
}
