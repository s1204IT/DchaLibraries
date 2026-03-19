package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanBandIndex {
    public static final int NAN_BAND_24GHZ = 0;
    public static final int NAN_BAND_5GHZ = 1;

    public static final String toString(int i) {
        if (i == 0) {
            return "NAN_BAND_24GHZ";
        }
        if (i == 1) {
            return "NAN_BAND_5GHZ";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("NAN_BAND_24GHZ");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("NAN_BAND_5GHZ");
        } else {
            i2 = 0;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
