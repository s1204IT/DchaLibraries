package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class RttPeerType {
    public static final int AP = 1;
    public static final int NAN = 5;
    public static final int P2P_CLIENT = 4;
    public static final int P2P_GO = 3;
    public static final int STA = 2;

    public static final String toString(int i) {
        if (i == 1) {
            return "AP";
        }
        if (i == 2) {
            return "STA";
        }
        if (i == 3) {
            return "P2P_GO";
        }
        if (i == 4) {
            return "P2P_CLIENT";
        }
        if (i == 5) {
            return "NAN";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("AP");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("STA");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("P2P_GO");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("P2P_CLIENT");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("NAN");
            i2 |= 5;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
