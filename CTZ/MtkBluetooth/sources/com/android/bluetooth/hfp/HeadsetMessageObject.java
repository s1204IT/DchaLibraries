package com.android.bluetooth.hfp;

public abstract class HeadsetMessageObject {
    public abstract void buildString(StringBuilder sb);

    public String toString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb);
        return sb.toString();
    }
}
