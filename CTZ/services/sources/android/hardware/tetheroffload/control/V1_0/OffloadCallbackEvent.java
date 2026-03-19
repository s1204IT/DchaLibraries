package android.hardware.tetheroffload.control.V1_0;

import java.util.ArrayList;

public final class OffloadCallbackEvent {
    public static final int OFFLOAD_STARTED = 1;
    public static final int OFFLOAD_STOPPED_ERROR = 2;
    public static final int OFFLOAD_STOPPED_LIMIT_REACHED = 5;
    public static final int OFFLOAD_STOPPED_UNSUPPORTED = 3;
    public static final int OFFLOAD_SUPPORT_AVAILABLE = 4;

    public static final String toString(int i) {
        if (i == 1) {
            return "OFFLOAD_STARTED";
        }
        if (i == 2) {
            return "OFFLOAD_STOPPED_ERROR";
        }
        if (i == 3) {
            return "OFFLOAD_STOPPED_UNSUPPORTED";
        }
        if (i == 4) {
            return "OFFLOAD_SUPPORT_AVAILABLE";
        }
        if (i == 5) {
            return "OFFLOAD_STOPPED_LIMIT_REACHED";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("OFFLOAD_STARTED");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("OFFLOAD_STOPPED_ERROR");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("OFFLOAD_STOPPED_UNSUPPORTED");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("OFFLOAD_SUPPORT_AVAILABLE");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("OFFLOAD_STOPPED_LIMIT_REACHED");
            i2 |= 5;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
