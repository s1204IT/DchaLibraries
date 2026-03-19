package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class RttBw {
    public static final int BW_10MHZ = 2;
    public static final int BW_160MHZ = 32;
    public static final int BW_20MHZ = 4;
    public static final int BW_40MHZ = 8;
    public static final int BW_5MHZ = 1;
    public static final int BW_80MHZ = 16;

    public static final String toString(int i) {
        if (i == 1) {
            return "BW_5MHZ";
        }
        if (i == 2) {
            return "BW_10MHZ";
        }
        if (i == 4) {
            return "BW_20MHZ";
        }
        if (i == 8) {
            return "BW_40MHZ";
        }
        if (i == 16) {
            return "BW_80MHZ";
        }
        if (i == 32) {
            return "BW_160MHZ";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("BW_5MHZ");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("BW_10MHZ");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("BW_20MHZ");
            i2 |= 4;
        }
        if ((i & 8) == 8) {
            arrayList.add("BW_40MHZ");
            i2 |= 8;
        }
        if ((i & 16) == 16) {
            arrayList.add("BW_80MHZ");
            i2 |= 16;
        }
        if ((i & 32) == 32) {
            arrayList.add("BW_160MHZ");
            i2 |= 32;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
