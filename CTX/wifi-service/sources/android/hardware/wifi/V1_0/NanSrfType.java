package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanSrfType {
    public static final int BLOOM_FILTER = 0;
    public static final int PARTIAL_MAC_ADDR = 1;

    public static final String toString(int i) {
        if (i == 0) {
            return "BLOOM_FILTER";
        }
        if (i == 1) {
            return "PARTIAL_MAC_ADDR";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("BLOOM_FILTER");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("PARTIAL_MAC_ADDR");
        } else {
            i2 = 0;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
