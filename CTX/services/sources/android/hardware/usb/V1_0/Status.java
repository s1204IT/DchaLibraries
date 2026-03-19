package android.hardware.usb.V1_0;

import java.util.ArrayList;

public final class Status {
    public static final int ERROR = 1;
    public static final int INVALID_ARGUMENT = 2;
    public static final int SUCCESS = 0;
    public static final int UNRECOGNIZED_ROLE = 3;

    public static final String toString(int i) {
        if (i == 0) {
            return "SUCCESS";
        }
        if (i == 1) {
            return "ERROR";
        }
        if (i == 2) {
            return "INVALID_ARGUMENT";
        }
        if (i == 3) {
            return "UNRECOGNIZED_ROLE";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("SUCCESS");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("ERROR");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("INVALID_ARGUMENT");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("UNRECOGNIZED_ROLE");
            i2 |= 3;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
