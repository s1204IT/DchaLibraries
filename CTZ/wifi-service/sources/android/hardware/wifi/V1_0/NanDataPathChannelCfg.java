package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanDataPathChannelCfg {
    public static final int CHANNEL_NOT_REQUESTED = 0;
    public static final int FORCE_CHANNEL_SETUP = 2;
    public static final int REQUEST_CHANNEL_SETUP = 1;

    public static final String toString(int i) {
        if (i == 0) {
            return "CHANNEL_NOT_REQUESTED";
        }
        if (i == 1) {
            return "REQUEST_CHANNEL_SETUP";
        }
        if (i == 2) {
            return "FORCE_CHANNEL_SETUP";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("CHANNEL_NOT_REQUESTED");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("REQUEST_CHANNEL_SETUP");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("FORCE_CHANNEL_SETUP");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
