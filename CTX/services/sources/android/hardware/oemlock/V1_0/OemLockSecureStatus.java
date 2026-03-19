package android.hardware.oemlock.V1_0;

import java.util.ArrayList;

public final class OemLockSecureStatus {
    public static final int FAILED = 1;
    public static final int INVALID_SIGNATURE = 2;
    public static final int OK = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "OK";
        }
        if (i == 1) {
            return "FAILED";
        }
        if (i == 2) {
            return "INVALID_SIGNATURE";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("OK");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("FAILED");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("INVALID_SIGNATURE");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
