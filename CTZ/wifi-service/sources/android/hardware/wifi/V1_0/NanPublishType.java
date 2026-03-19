package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanPublishType {
    public static final int SOLICITED = 1;
    public static final int UNSOLICITED = 0;
    public static final int UNSOLICITED_SOLICITED = 2;

    public static final String toString(int i) {
        if (i == 0) {
            return "UNSOLICITED";
        }
        if (i == 1) {
            return "SOLICITED";
        }
        if (i == 2) {
            return "UNSOLICITED_SOLICITED";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("UNSOLICITED");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("SOLICITED");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("UNSOLICITED_SOLICITED");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
