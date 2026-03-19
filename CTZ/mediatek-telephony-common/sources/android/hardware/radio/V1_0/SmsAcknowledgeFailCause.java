package android.hardware.radio.V1_0;

import java.util.ArrayList;

public final class SmsAcknowledgeFailCause {
    public static final int MEMORY_CAPACITY_EXCEEDED = 211;
    public static final int UNSPECIFIED_ERROR = 255;

    public static final String toString(int i) {
        if (i == 211) {
            return "MEMORY_CAPACITY_EXCEEDED";
        }
        if (i == 255) {
            return "UNSPECIFIED_ERROR";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = i & MEMORY_CAPACITY_EXCEEDED;
        int i3 = MEMORY_CAPACITY_EXCEEDED;
        if (i2 == 211) {
            arrayList.add("MEMORY_CAPACITY_EXCEEDED");
        } else {
            i3 = 0;
        }
        if ((i & 255) == 255) {
            arrayList.add("UNSPECIFIED_ERROR");
            i3 |= 255;
        }
        if (i != i3) {
            arrayList.add("0x" + Integer.toHexString(i & (~i3)));
        }
        return String.join(" | ", arrayList);
    }
}
