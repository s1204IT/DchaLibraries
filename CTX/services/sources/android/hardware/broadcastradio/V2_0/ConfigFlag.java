package android.hardware.broadcastradio.V2_0;

import java.util.ArrayList;

public final class ConfigFlag {
    public static final int DAB_DAB_LINKING = 6;
    public static final int DAB_DAB_SOFT_LINKING = 8;
    public static final int DAB_FM_LINKING = 7;
    public static final int DAB_FM_SOFT_LINKING = 9;
    public static final int FORCE_ANALOG = 2;
    public static final int FORCE_DIGITAL = 3;
    public static final int FORCE_MONO = 1;
    public static final int RDS_AF = 4;
    public static final int RDS_REG = 5;

    public static final String toString(int i) {
        if (i == 1) {
            return "FORCE_MONO";
        }
        if (i == 2) {
            return "FORCE_ANALOG";
        }
        if (i == 3) {
            return "FORCE_DIGITAL";
        }
        if (i == 4) {
            return "RDS_AF";
        }
        if (i == 5) {
            return "RDS_REG";
        }
        if (i == 6) {
            return "DAB_DAB_LINKING";
        }
        if (i == 7) {
            return "DAB_FM_LINKING";
        }
        if (i == 8) {
            return "DAB_DAB_SOFT_LINKING";
        }
        if (i == 9) {
            return "DAB_FM_SOFT_LINKING";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("FORCE_MONO");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("FORCE_ANALOG");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("FORCE_DIGITAL");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("RDS_AF");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("RDS_REG");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("DAB_DAB_LINKING");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("DAB_FM_LINKING");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("DAB_DAB_SOFT_LINKING");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("DAB_FM_SOFT_LINKING");
            i2 |= 9;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
