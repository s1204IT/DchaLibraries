package android.hardware.broadcastradio.V2_0;

import java.util.ArrayList;

public final class Constants {
    public static final int ANTENNA_DISCONNECTED_TIMEOUT_MS = 100;
    public static final int INVALID_IMAGE = 0;
    public static final int LIST_COMPLETE_TIMEOUT_MS = 300000;

    public static final String toString(int i) {
        if (i == 0) {
            return "INVALID_IMAGE";
        }
        if (i == 100) {
            return "ANTENNA_DISCONNECTED_TIMEOUT_MS";
        }
        if (i == 300000) {
            return "LIST_COMPLETE_TIMEOUT_MS";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("INVALID_IMAGE");
        int i2 = 100;
        if ((i & 100) == 100) {
            arrayList.add("ANTENNA_DISCONNECTED_TIMEOUT_MS");
        } else {
            i2 = 0;
        }
        if ((i & LIST_COMPLETE_TIMEOUT_MS) == 300000) {
            arrayList.add("LIST_COMPLETE_TIMEOUT_MS");
            i2 |= LIST_COMPLETE_TIMEOUT_MS;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
