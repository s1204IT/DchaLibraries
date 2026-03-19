package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiStatusCode {
    public static final int ERROR_BUSY = 8;
    public static final int ERROR_INVALID_ARGS = 7;
    public static final int ERROR_NOT_AVAILABLE = 5;
    public static final int ERROR_NOT_STARTED = 6;
    public static final int ERROR_NOT_SUPPORTED = 4;
    public static final int ERROR_UNKNOWN = 9;
    public static final int ERROR_WIFI_CHIP_INVALID = 1;
    public static final int ERROR_WIFI_IFACE_INVALID = 2;
    public static final int ERROR_WIFI_RTT_CONTROLLER_INVALID = 3;
    public static final int SUCCESS = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "SUCCESS";
        }
        if (i == 1) {
            return "ERROR_WIFI_CHIP_INVALID";
        }
        if (i == 2) {
            return "ERROR_WIFI_IFACE_INVALID";
        }
        if (i == 3) {
            return "ERROR_WIFI_RTT_CONTROLLER_INVALID";
        }
        if (i == 4) {
            return "ERROR_NOT_SUPPORTED";
        }
        if (i == 5) {
            return "ERROR_NOT_AVAILABLE";
        }
        if (i == 6) {
            return "ERROR_NOT_STARTED";
        }
        if (i == 7) {
            return "ERROR_INVALID_ARGS";
        }
        if (i == 8) {
            return "ERROR_BUSY";
        }
        if (i == 9) {
            return "ERROR_UNKNOWN";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("SUCCESS");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("ERROR_WIFI_CHIP_INVALID");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("ERROR_WIFI_IFACE_INVALID");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("ERROR_WIFI_RTT_CONTROLLER_INVALID");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("ERROR_NOT_SUPPORTED");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("ERROR_NOT_AVAILABLE");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("ERROR_NOT_STARTED");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("ERROR_INVALID_ARGS");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("ERROR_BUSY");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("ERROR_UNKNOWN");
            i2 |= 9;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
