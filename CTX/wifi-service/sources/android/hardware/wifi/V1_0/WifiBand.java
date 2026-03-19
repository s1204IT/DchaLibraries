package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiBand {
    public static final int BAND_24GHZ = 1;
    public static final int BAND_24GHZ_5GHZ = 3;
    public static final int BAND_24GHZ_5GHZ_WITH_DFS = 7;
    public static final int BAND_5GHZ = 2;
    public static final int BAND_5GHZ_DFS = 4;
    public static final int BAND_5GHZ_WITH_DFS = 6;
    public static final int BAND_UNSPECIFIED = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "BAND_UNSPECIFIED";
        }
        if (i == 1) {
            return "BAND_24GHZ";
        }
        if (i == 2) {
            return "BAND_5GHZ";
        }
        if (i == 4) {
            return "BAND_5GHZ_DFS";
        }
        if (i == 6) {
            return "BAND_5GHZ_WITH_DFS";
        }
        if (i == 3) {
            return "BAND_24GHZ_5GHZ";
        }
        if (i == 7) {
            return "BAND_24GHZ_5GHZ_WITH_DFS";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("BAND_UNSPECIFIED");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("BAND_24GHZ");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("BAND_5GHZ");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("BAND_5GHZ_DFS");
            i2 |= 4;
        }
        if ((i & 6) == 6) {
            arrayList.add("BAND_5GHZ_WITH_DFS");
            i2 |= 6;
        }
        if ((i & 3) == 3) {
            arrayList.add("BAND_24GHZ_5GHZ");
            i2 |= 3;
        }
        if ((i & 7) == 7) {
            arrayList.add("BAND_24GHZ_5GHZ_WITH_DFS");
            i2 |= 7;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
