package android.hardware.broadcastradio.V2_0;

import java.util.ArrayList;

public final class ProgramInfoFlags {
    public static final int LIVE = 1;
    public static final int MUTED = 2;
    public static final int STEREO = 32;
    public static final int TRAFFIC_ANNOUNCEMENT = 8;
    public static final int TRAFFIC_PROGRAM = 4;
    public static final int TUNED = 16;

    public static final String toString(int i) {
        if (i == 1) {
            return "LIVE";
        }
        if (i == 2) {
            return "MUTED";
        }
        if (i == 4) {
            return "TRAFFIC_PROGRAM";
        }
        if (i == 8) {
            return "TRAFFIC_ANNOUNCEMENT";
        }
        if (i == 16) {
            return "TUNED";
        }
        if (i == 32) {
            return "STEREO";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("LIVE");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("MUTED");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("TRAFFIC_PROGRAM");
            i2 |= 4;
        }
        if ((i & 8) == 8) {
            arrayList.add("TRAFFIC_ANNOUNCEMENT");
            i2 |= 8;
        }
        if ((i & 16) == 16) {
            arrayList.add("TUNED");
            i2 |= 16;
        }
        if ((i & 32) == 32) {
            arrayList.add("STEREO");
            i2 |= 32;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
