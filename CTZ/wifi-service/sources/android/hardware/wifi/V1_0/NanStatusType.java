package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanStatusType {
    public static final int ALREADY_ENABLED = 10;
    public static final int FOLLOWUP_TX_QUEUE_FULL = 11;
    public static final int INTERNAL_FAILURE = 1;
    public static final int INVALID_ARGS = 5;
    public static final int INVALID_NDP_ID = 7;
    public static final int INVALID_PEER_ID = 6;
    public static final int INVALID_SESSION_ID = 3;
    public static final int NAN_NOT_ALLOWED = 8;
    public static final int NO_OTA_ACK = 9;
    public static final int NO_RESOURCES_AVAILABLE = 4;
    public static final int PROTOCOL_FAILURE = 2;
    public static final int SUCCESS = 0;
    public static final int UNSUPPORTED_CONCURRENCY_NAN_DISABLED = 12;

    public static final String toString(int i) {
        if (i == 0) {
            return "SUCCESS";
        }
        if (i == 1) {
            return "INTERNAL_FAILURE";
        }
        if (i == 2) {
            return "PROTOCOL_FAILURE";
        }
        if (i == 3) {
            return "INVALID_SESSION_ID";
        }
        if (i == 4) {
            return "NO_RESOURCES_AVAILABLE";
        }
        if (i == 5) {
            return "INVALID_ARGS";
        }
        if (i == 6) {
            return "INVALID_PEER_ID";
        }
        if (i == 7) {
            return "INVALID_NDP_ID";
        }
        if (i == 8) {
            return "NAN_NOT_ALLOWED";
        }
        if (i == 9) {
            return "NO_OTA_ACK";
        }
        if (i == 10) {
            return "ALREADY_ENABLED";
        }
        if (i == 11) {
            return "FOLLOWUP_TX_QUEUE_FULL";
        }
        if (i == 12) {
            return "UNSUPPORTED_CONCURRENCY_NAN_DISABLED";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("SUCCESS");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("INTERNAL_FAILURE");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("PROTOCOL_FAILURE");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("INVALID_SESSION_ID");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("NO_RESOURCES_AVAILABLE");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("INVALID_ARGS");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("INVALID_PEER_ID");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("INVALID_NDP_ID");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("NAN_NOT_ALLOWED");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("NO_OTA_ACK");
            i2 |= 9;
        }
        if ((i & 10) == 10) {
            arrayList.add("ALREADY_ENABLED");
            i2 |= 10;
        }
        if ((i & 11) == 11) {
            arrayList.add("FOLLOWUP_TX_QUEUE_FULL");
            i2 |= 11;
        }
        if ((i & 12) == 12) {
            arrayList.add("UNSUPPORTED_CONCURRENCY_NAN_DISABLED");
            i2 |= 12;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
