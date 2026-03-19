package android.hardware.wifi.hostapd.V1_0;

import java.util.ArrayList;

public final class HostapdStatusCode {
    public static final int FAILURE_ARGS_INVALID = 2;
    public static final int FAILURE_IFACE_EXISTS = 4;
    public static final int FAILURE_IFACE_UNKNOWN = 3;
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
            return "FAILURE_IFACE_UNKNOWN";
        }
        if (i == 4) {
            return "FAILURE_IFACE_EXISTS";
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
            arrayList.add("FAILURE_IFACE_UNKNOWN");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("FAILURE_IFACE_EXISTS");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
