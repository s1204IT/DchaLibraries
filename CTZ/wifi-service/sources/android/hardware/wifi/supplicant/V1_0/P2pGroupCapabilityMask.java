package android.hardware.wifi.supplicant.V1_0;

import java.util.ArrayList;

public final class P2pGroupCapabilityMask {
    public static final int CROSS_CONN = 16;
    public static final int GROUP_FORMATION = 64;
    public static final int GROUP_LIMIT = 4;
    public static final int GROUP_OWNER = 1;
    public static final int INTRA_BSS_DIST = 8;
    public static final int PERSISTENT_GROUP = 2;
    public static final int PERSISTENT_RECONN = 32;

    public static final String toString(int i) {
        if (i == 1) {
            return "GROUP_OWNER";
        }
        if (i == 2) {
            return "PERSISTENT_GROUP";
        }
        if (i == 4) {
            return "GROUP_LIMIT";
        }
        if (i == 8) {
            return "INTRA_BSS_DIST";
        }
        if (i == 16) {
            return "CROSS_CONN";
        }
        if (i == 32) {
            return "PERSISTENT_RECONN";
        }
        if (i == 64) {
            return "GROUP_FORMATION";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("GROUP_OWNER");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("PERSISTENT_GROUP");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("GROUP_LIMIT");
            i2 |= 4;
        }
        if ((i & 8) == 8) {
            arrayList.add("INTRA_BSS_DIST");
            i2 |= 8;
        }
        if ((i & 16) == 16) {
            arrayList.add("CROSS_CONN");
            i2 |= 16;
        }
        if ((i & 32) == 32) {
            arrayList.add("PERSISTENT_RECONN");
            i2 |= 32;
        }
        if ((i & 64) == 64) {
            arrayList.add("GROUP_FORMATION");
            i2 |= 64;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
