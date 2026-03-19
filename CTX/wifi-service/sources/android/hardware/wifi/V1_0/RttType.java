package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class RttType {
    public static final int ONE_SIDED = 1;
    public static final int TWO_SIDED = 2;

    public static final String toString(int i) {
        if (i == 1) {
            return "ONE_SIDED";
        }
        if (i == 2) {
            return "TWO_SIDED";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("ONE_SIDED");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("TWO_SIDED");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
