package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanParamSizeLimits {
    public static final int MAX_PASSPHRASE_LENGTH = 63;
    public static final int MIN_PASSPHRASE_LENGTH = 8;

    public static final String toString(int i) {
        if (i == 8) {
            return "MIN_PASSPHRASE_LENGTH";
        }
        if (i == 63) {
            return "MAX_PASSPHRASE_LENGTH";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 8;
        if ((i & 8) == 8) {
            arrayList.add("MIN_PASSPHRASE_LENGTH");
        } else {
            i2 = 0;
        }
        if ((i & 63) == 63) {
            arrayList.add("MAX_PASSPHRASE_LENGTH");
            i2 |= 63;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
