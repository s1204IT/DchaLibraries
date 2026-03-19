package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class RttMotionPattern {
    public static final int EXPECTED = 1;
    public static final int NOT_EXPECTED = 0;
    public static final int UNKNOWN = 2;

    public static final String toString(int i) {
        if (i == 0) {
            return "NOT_EXPECTED";
        }
        if (i == 1) {
            return "EXPECTED";
        }
        if (i == 2) {
            return "UNKNOWN";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("NOT_EXPECTED");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("EXPECTED");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("UNKNOWN");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
