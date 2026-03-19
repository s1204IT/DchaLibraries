package vendor.mediatek.hardware.radio.V3_0;

import java.util.ArrayList;

public final class DsbpState {
    public static final int DSBP_ENHANCEMENT_END = 0;
    public static final int DSBP_ENHANCEMENT_START = 1;

    public static final String toString(int i) {
        if (i == 0) {
            return "DSBP_ENHANCEMENT_END";
        }
        if (i == 1) {
            return "DSBP_ENHANCEMENT_START";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("DSBP_ENHANCEMENT_END");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("DSBP_ENHANCEMENT_START");
        } else {
            i2 = 0;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
