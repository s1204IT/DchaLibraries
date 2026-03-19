package com.android.bluetooth.hearingaid;

import android.bluetooth.BluetoothDevice;

public class HearingAidStackEvent {
    static final int CONNECTION_STATE_CONNECTED = 2;
    static final int CONNECTION_STATE_CONNECTING = 1;
    static final int CONNECTION_STATE_DISCONNECTED = 0;
    static final int CONNECTION_STATE_DISCONNECTING = 3;
    public static final int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    public static final int EVENT_TYPE_DEVICE_AVAILABLE = 2;
    private static final int EVENT_TYPE_NONE = 0;
    public BluetoothDevice device;
    public int type;
    public int valueInt1;
    public long valueLong2;

    HearingAidStackEvent(int i) {
        this.type = i;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HearingAidStackEvent {type:" + eventTypeToString(this.type));
        sb.append(", device:" + this.device);
        sb.append(", value1:" + this.valueInt1);
        sb.append(", value2:" + this.valueLong2);
        sb.append("}");
        return sb.toString();
    }

    private static String eventTypeToString(int i) {
        switch (i) {
            case 0:
                return "EVENT_TYPE_NONE";
            case 1:
                return "EVENT_TYPE_CONNECTION_STATE_CHANGED";
            case 2:
                return "EVENT_TYPE_DEVICE_AVAILABLE";
            default:
                return "EVENT_TYPE_UNKNOWN:" + i;
        }
    }
}
