package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class StaScanLimits {
    public static final int MAX_AP_CACHE_PER_SCAN = 32;
    public static final int MAX_BUCKETS = 16;
    public static final int MAX_CHANNELS = 16;

    public static final String toString(int i) {
        if (i == 16) {
            return "MAX_CHANNELS";
        }
        if (i == 16) {
            return "MAX_BUCKETS";
        }
        if (i == 32) {
            return "MAX_AP_CACHE_PER_SCAN";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        int i2;
        ArrayList arrayList = new ArrayList();
        int i3 = i & 16;
        if (i3 == 16) {
            arrayList.add("MAX_CHANNELS");
            i2 = 16;
        } else {
            i2 = 0;
        }
        if (i3 == 16) {
            arrayList.add("MAX_BUCKETS");
            i2 |= 16;
        }
        if ((i & 32) == 32) {
            arrayList.add("MAX_AP_CACHE_PER_SCAN");
            i2 |= 32;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
