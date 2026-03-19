package android.hardware.biometrics.fingerprint.V2_1;

import java.util.ArrayList;

public final class FingerprintError {
    public static final int ERROR_CANCELED = 5;
    public static final int ERROR_HW_UNAVAILABLE = 1;
    public static final int ERROR_LOCKOUT = 7;
    public static final int ERROR_NO_ERROR = 0;
    public static final int ERROR_NO_SPACE = 4;
    public static final int ERROR_TIMEOUT = 3;
    public static final int ERROR_UNABLE_TO_PROCESS = 2;
    public static final int ERROR_UNABLE_TO_REMOVE = 6;
    public static final int ERROR_VENDOR = 8;

    public static final String toString(int i) {
        if (i == 0) {
            return "ERROR_NO_ERROR";
        }
        if (i == 1) {
            return "ERROR_HW_UNAVAILABLE";
        }
        if (i == 2) {
            return "ERROR_UNABLE_TO_PROCESS";
        }
        if (i == 3) {
            return "ERROR_TIMEOUT";
        }
        if (i == 4) {
            return "ERROR_NO_SPACE";
        }
        if (i == 5) {
            return "ERROR_CANCELED";
        }
        if (i == 6) {
            return "ERROR_UNABLE_TO_REMOVE";
        }
        if (i == 7) {
            return "ERROR_LOCKOUT";
        }
        if (i == 8) {
            return "ERROR_VENDOR";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("ERROR_NO_ERROR");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("ERROR_HW_UNAVAILABLE");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("ERROR_UNABLE_TO_PROCESS");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("ERROR_TIMEOUT");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("ERROR_NO_SPACE");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("ERROR_CANCELED");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("ERROR_UNABLE_TO_REMOVE");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("ERROR_LOCKOUT");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("ERROR_VENDOR");
            i2 |= 8;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
