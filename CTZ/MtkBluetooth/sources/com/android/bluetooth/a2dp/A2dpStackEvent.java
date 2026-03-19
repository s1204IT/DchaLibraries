package com.android.bluetooth.a2dp;

import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;

public class A2dpStackEvent {
    static final int AUDIO_STATE_REMOTE_SUSPEND = 0;
    static final int AUDIO_STATE_STARTED = 2;
    static final int AUDIO_STATE_STOPPED = 1;
    static final int CONNECTION_STATE_CONNECTED = 2;
    static final int CONNECTION_STATE_CONNECTING = 1;
    static final int CONNECTION_STATE_DISCONNECTED = 0;
    static final int CONNECTION_STATE_DISCONNECTING = 3;
    public static final int EVENT_TYPE_AUDIO_STATE_CHANGED = 2;
    public static final int EVENT_TYPE_CODEC_CONFIG_CHANGED = 3;
    public static final int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    private static final int EVENT_TYPE_NONE = 0;
    public BluetoothCodecStatus codecStatus;
    public BluetoothDevice device;
    public int type;
    public int valueInt = 0;

    A2dpStackEvent(int i) {
        this.type = 0;
        this.type = i;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("A2dpStackEvent {type:" + eventTypeToString(this.type));
        sb.append(", device:" + this.device);
        sb.append(", value1:" + eventTypeValueIntToString(this.type, this.valueInt));
        if (this.codecStatus != null) {
            sb.append(", codecStatus:" + this.codecStatus);
        }
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
                return "EVENT_TYPE_AUDIO_STATE_CHANGED";
            case 3:
                return "EVENT_TYPE_CODEC_CONFIG_CHANGED";
            default:
                return "EVENT_TYPE_UNKNOWN:" + i;
        }
    }

    private static String eventTypeValueIntToString(int i, int i2) {
        switch (i) {
            case 1:
                switch (i2) {
                    case 0:
                        return "DISCONNECTED";
                    case 1:
                        return "CONNECTING";
                    case 2:
                        return "CONNECTED";
                    case 3:
                        return "DISCONNECTING";
                }
            case 2:
                switch (i2) {
                    case 0:
                        return "REMOTE_SUSPEND";
                    case 1:
                        return "STOPPED";
                    case 2:
                        return "STARTED";
                }
        }
        return Integer.toString(i2);
    }
}
