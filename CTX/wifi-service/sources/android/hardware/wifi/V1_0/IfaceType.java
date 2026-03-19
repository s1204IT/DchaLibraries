package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class IfaceType {
    public static final int AP = 1;
    public static final int NAN = 3;
    public static final int P2P = 2;
    public static final int STA = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "STA";
        }
        if (i == 1) {
            return "AP";
        }
        if (i == 2) {
            return "P2P";
        }
        if (i == 3) {
            return "NAN";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("STA");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("AP");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("P2P");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("NAN");
            i2 |= 3;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
