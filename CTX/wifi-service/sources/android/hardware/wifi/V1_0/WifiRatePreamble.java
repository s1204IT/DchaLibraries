package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiRatePreamble {
    public static final int CCK = 1;
    public static final int HT = 2;
    public static final int OFDM = 0;
    public static final int RESERVED = 4;
    public static final int VHT = 3;

    public static final String toString(int i) {
        if (i == 0) {
            return "OFDM";
        }
        if (i == 1) {
            return "CCK";
        }
        if (i == 2) {
            return "HT";
        }
        if (i == 3) {
            return "VHT";
        }
        if (i == 4) {
            return "RESERVED";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("OFDM");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("CCK");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("HT");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("VHT");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("RESERVED");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
