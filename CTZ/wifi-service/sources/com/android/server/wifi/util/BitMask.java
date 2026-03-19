package com.android.server.wifi.util;

public class BitMask {
    public int value;

    public BitMask(int i) {
        this.value = i;
    }

    public boolean testAndClear(int i) {
        boolean z = (this.value & i) != 0;
        this.value = (~i) & this.value;
        return z;
    }
}
