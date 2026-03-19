package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import java.util.Objects;

public class HeadsetStackEvent extends HeadsetMessageObject {
    public static final int EVENT_TYPE_ANSWER_CALL = 4;
    public static final int EVENT_TYPE_AT_CHLD = 10;
    public static final int EVENT_TYPE_AT_CIND = 12;
    public static final int EVENT_TYPE_AT_CLCC = 14;
    public static final int EVENT_TYPE_AT_COPS = 13;
    public static final int EVENT_TYPE_AUDIO_STATE_CHANGED = 2;
    public static final int EVENT_TYPE_BIA = 20;
    public static final int EVENT_TYPE_BIEV = 19;
    public static final int EVENT_TYPE_BIND = 18;
    public static final int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    public static final int EVENT_TYPE_DIAL_CALL = 7;
    public static final int EVENT_TYPE_HANGUP_CALL = 5;
    public static final int EVENT_TYPE_KEY_PRESSED = 16;
    public static final int EVENT_TYPE_NOICE_REDUCTION = 9;
    public static final int EVENT_TYPE_NONE = 0;
    public static final int EVENT_TYPE_SEND_DTMF = 8;
    public static final int EVENT_TYPE_SUBSCRIBER_NUMBER_REQUEST = 11;
    public static final int EVENT_TYPE_UNKNOWN_AT = 15;
    public static final int EVENT_TYPE_VOLUME_CHANGED = 6;
    public static final int EVENT_TYPE_VR_STATE_CHANGED = 3;
    public static final int EVENT_TYPE_WBS = 17;
    public final BluetoothDevice device;
    public final int type;
    public final int valueInt;
    public final int valueInt2;
    public final HeadsetMessageObject valueObject;
    public final String valueString;

    public HeadsetStackEvent(int i, BluetoothDevice bluetoothDevice) {
        this(i, 0, 0, null, null, bluetoothDevice);
    }

    public HeadsetStackEvent(int i, int i2, BluetoothDevice bluetoothDevice) {
        this(i, i2, 0, null, null, bluetoothDevice);
    }

    public HeadsetStackEvent(int i, int i2, int i3, BluetoothDevice bluetoothDevice) {
        this(i, i2, i3, null, null, bluetoothDevice);
    }

    public HeadsetStackEvent(int i, String str, BluetoothDevice bluetoothDevice) {
        this(i, 0, 0, str, null, bluetoothDevice);
    }

    public HeadsetStackEvent(int i, HeadsetMessageObject headsetMessageObject, BluetoothDevice bluetoothDevice) {
        this(i, 0, 0, null, headsetMessageObject, bluetoothDevice);
    }

    public HeadsetStackEvent(int i, int i2, int i3, String str, HeadsetMessageObject headsetMessageObject, BluetoothDevice bluetoothDevice) {
        this.type = i;
        this.valueInt = i2;
        this.valueInt2 = i3;
        this.valueString = str;
        this.valueObject = headsetMessageObject;
        this.device = (BluetoothDevice) Objects.requireNonNull(bluetoothDevice);
    }

    public String getTypeString() {
        switch (this.type) {
            case 0:
                return "EVENT_TYPE_NONE";
            case 1:
                return "EVENT_TYPE_CONNECTION_STATE_CHANGED";
            case 2:
                return "EVENT_TYPE_AUDIO_STATE_CHANGED";
            case 3:
                return "EVENT_TYPE_VR_STATE_CHANGED";
            case 4:
                return "EVENT_TYPE_ANSWER_CALL";
            case 5:
                return "EVENT_TYPE_HANGUP_CALL";
            case 6:
                return "EVENT_TYPE_VOLUME_CHANGED";
            case 7:
                return "EVENT_TYPE_DIAL_CALL";
            case 8:
                return "EVENT_TYPE_SEND_DTMF";
            case 9:
                return "EVENT_TYPE_NOICE_REDUCTION";
            case 10:
                return "EVENT_TYPE_AT_CHLD";
            case 11:
                return "EVENT_TYPE_SUBSCRIBER_NUMBER_REQUEST";
            case 12:
                return "EVENT_TYPE_AT_CIND";
            case 13:
                return "EVENT_TYPE_AT_COPS";
            case 14:
                return "EVENT_TYPE_AT_CLCC";
            case 15:
                return "EVENT_TYPE_UNKNOWN_AT";
            case 16:
                return "EVENT_TYPE_KEY_PRESSED";
            case 17:
                return "EVENT_TYPE_WBS";
            case 18:
                return "EVENT_TYPE_BIND";
            case 19:
                return "EVENT_TYPE_BIEV";
            case 20:
                return "EVENT_TYPE_BIA";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public void buildString(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append(getTypeString());
        sb.append("[");
        sb.append(this.type);
        sb.append("]");
        sb.append(", valInt=");
        sb.append(this.valueInt);
        sb.append(", valInt2=");
        sb.append(this.valueInt2);
        sb.append(", valString=");
        sb.append(this.valueString);
        sb.append(", valObject=");
        sb.append(this.valueObject);
        sb.append(", device=");
        sb.append(this.device);
    }
}
