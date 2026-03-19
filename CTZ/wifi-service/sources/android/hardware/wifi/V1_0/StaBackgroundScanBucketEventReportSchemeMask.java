package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class StaBackgroundScanBucketEventReportSchemeMask {
    public static final int EACH_SCAN = 1;
    public static final int FULL_RESULTS = 2;
    public static final int NO_BATCH = 4;

    public static final String toString(int i) {
        if (i == 1) {
            return "EACH_SCAN";
        }
        if (i == 2) {
            return "FULL_RESULTS";
        }
        if (i == 4) {
            return "NO_BATCH";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("EACH_SCAN");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("FULL_RESULTS");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("NO_BATCH");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
