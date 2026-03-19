package com.android.bluetooth.gatt;

class CallbackInfo {
    public String address;
    public int handle;
    public int status;

    CallbackInfo(String str, int i, int i2) {
        this.address = str;
        this.status = i;
        this.handle = i2;
    }

    CallbackInfo(String str, int i) {
        this.address = str;
        this.status = i;
    }
}
