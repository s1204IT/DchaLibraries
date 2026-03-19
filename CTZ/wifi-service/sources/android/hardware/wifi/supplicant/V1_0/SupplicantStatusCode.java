package android.hardware.wifi.supplicant.V1_0;

import java.util.ArrayList;

public final class SupplicantStatusCode {
    public static final int FAILURE_ARGS_INVALID = 2;
    public static final int FAILURE_IFACE_DISABLED = 6;
    public static final int FAILURE_IFACE_EXISTS = 5;
    public static final int FAILURE_IFACE_INVALID = 3;
    public static final int FAILURE_IFACE_NOT_DISCONNECTED = 7;
    public static final int FAILURE_IFACE_UNKNOWN = 4;
    public static final int FAILURE_NETWORK_INVALID = 8;
    public static final int FAILURE_NETWORK_UNKNOWN = 9;
    public static final int FAILURE_UNKNOWN = 1;
    public static final int SUCCESS = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "SUCCESS";
        }
        if (i == 1) {
            return "FAILURE_UNKNOWN";
        }
        if (i == 2) {
            return "FAILURE_ARGS_INVALID";
        }
        if (i == 3) {
            return "FAILURE_IFACE_INVALID";
        }
        if (i == 4) {
            return "FAILURE_IFACE_UNKNOWN";
        }
        if (i == 5) {
            return "FAILURE_IFACE_EXISTS";
        }
        if (i == 6) {
            return "FAILURE_IFACE_DISABLED";
        }
        if (i == 7) {
            return "FAILURE_IFACE_NOT_DISCONNECTED";
        }
        if (i == 8) {
            return "FAILURE_NETWORK_INVALID";
        }
        if (i == 9) {
            return "FAILURE_NETWORK_UNKNOWN";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("SUCCESS");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("FAILURE_UNKNOWN");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("FAILURE_ARGS_INVALID");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("FAILURE_IFACE_INVALID");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("FAILURE_IFACE_UNKNOWN");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("FAILURE_IFACE_EXISTS");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("FAILURE_IFACE_DISABLED");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("FAILURE_IFACE_NOT_DISCONNECTED");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("FAILURE_NETWORK_INVALID");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("FAILURE_NETWORK_UNKNOWN");
            i2 |= 9;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
