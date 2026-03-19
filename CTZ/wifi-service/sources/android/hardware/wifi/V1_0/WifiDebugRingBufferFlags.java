package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiDebugRingBufferFlags {
    public static final int HAS_ASCII_ENTRIES = 2;
    public static final int HAS_BINARY_ENTRIES = 1;
    public static final int HAS_PER_PACKET_ENTRIES = 4;

    public static final String toString(int i) {
        if (i == 1) {
            return "HAS_BINARY_ENTRIES";
        }
        if (i == 2) {
            return "HAS_ASCII_ENTRIES";
        }
        if (i == 4) {
            return "HAS_PER_PACKET_ENTRIES";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("HAS_BINARY_ENTRIES");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("HAS_ASCII_ENTRIES");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("HAS_PER_PACKET_ENTRIES");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
