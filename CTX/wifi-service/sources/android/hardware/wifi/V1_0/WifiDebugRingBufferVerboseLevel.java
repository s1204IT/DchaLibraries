package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiDebugRingBufferVerboseLevel {
    public static final int DEFAULT = 1;
    public static final int EXCESSIVE = 3;
    public static final int NONE = 0;
    public static final int VERBOSE = 2;

    public static final String toString(int i) {
        if (i == 0) {
            return "NONE";
        }
        if (i == 1) {
            return "DEFAULT";
        }
        if (i == 2) {
            return "VERBOSE";
        }
        if (i == 3) {
            return "EXCESSIVE";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("NONE");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("DEFAULT");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("VERBOSE");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("EXCESSIVE");
            i2 |= 3;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
