package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class RttPreamble {
    public static final int HT = 2;
    public static final int LEGACY = 1;
    public static final int VHT = 4;

    public static final String toString(int i) {
        if (i == 1) {
            return "LEGACY";
        }
        if (i == 2) {
            return "HT";
        }
        if (i == 4) {
            return "VHT";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("LEGACY");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("HT");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("VHT");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
