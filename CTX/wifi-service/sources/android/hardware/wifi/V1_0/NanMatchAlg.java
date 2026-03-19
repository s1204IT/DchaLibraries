package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanMatchAlg {
    public static final int MATCH_CONTINUOUS = 1;
    public static final int MATCH_NEVER = 2;
    public static final int MATCH_ONCE = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "MATCH_ONCE";
        }
        if (i == 1) {
            return "MATCH_CONTINUOUS";
        }
        if (i == 2) {
            return "MATCH_NEVER";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("MATCH_ONCE");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("MATCH_CONTINUOUS");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("MATCH_NEVER");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
