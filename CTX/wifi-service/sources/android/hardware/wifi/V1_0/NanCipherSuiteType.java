package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanCipherSuiteType {
    public static final int NONE = 0;
    public static final int SHARED_KEY_128_MASK = 1;
    public static final int SHARED_KEY_256_MASK = 2;

    public static final String toString(int i) {
        if (i == 0) {
            return "NONE";
        }
        if (i == 1) {
            return "SHARED_KEY_128_MASK";
        }
        if (i == 2) {
            return "SHARED_KEY_256_MASK";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("NONE");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("SHARED_KEY_128_MASK");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("SHARED_KEY_256_MASK");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
