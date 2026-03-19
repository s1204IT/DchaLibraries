package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiRateNss {
    public static final int NSS_1x1 = 0;
    public static final int NSS_2x2 = 1;
    public static final int NSS_3x3 = 2;
    public static final int NSS_4x4 = 3;

    public static final String toString(int i) {
        if (i == 0) {
            return "NSS_1x1";
        }
        if (i == 1) {
            return "NSS_2x2";
        }
        if (i == 2) {
            return "NSS_3x3";
        }
        if (i == 3) {
            return "NSS_4x4";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("NSS_1x1");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("NSS_2x2");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("NSS_3x3");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("NSS_4x4");
            i2 |= 3;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
