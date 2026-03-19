package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanRangingIndication {
    public static final int CONTINUOUS_INDICATION_MASK = 1;
    public static final int EGRESS_MET_MASK = 4;
    public static final int INGRESS_MET_MASK = 2;

    public static final String toString(int i) {
        if (i == 1) {
            return "CONTINUOUS_INDICATION_MASK";
        }
        if (i == 2) {
            return "INGRESS_MET_MASK";
        }
        if (i == 4) {
            return "EGRESS_MET_MASK";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("CONTINUOUS_INDICATION_MASK");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("INGRESS_MET_MASK");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("EGRESS_MET_MASK");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
