package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiDebugTxPacketFate {
    public static final int ACKED = 0;
    public static final int DRV_DROP_INVALID = 7;
    public static final int DRV_DROP_NOBUFS = 8;
    public static final int DRV_DROP_OTHER = 9;
    public static final int DRV_QUEUED = 6;
    public static final int FW_DROP_INVALID = 3;
    public static final int FW_DROP_NOBUFS = 4;
    public static final int FW_DROP_OTHER = 5;
    public static final int FW_QUEUED = 2;
    public static final int SENT = 1;

    public static final String toString(int i) {
        if (i == 0) {
            return "ACKED";
        }
        if (i == 1) {
            return "SENT";
        }
        if (i == 2) {
            return "FW_QUEUED";
        }
        if (i == 3) {
            return "FW_DROP_INVALID";
        }
        if (i == 4) {
            return "FW_DROP_NOBUFS";
        }
        if (i == 5) {
            return "FW_DROP_OTHER";
        }
        if (i == 6) {
            return "DRV_QUEUED";
        }
        if (i == 7) {
            return "DRV_DROP_INVALID";
        }
        if (i == 8) {
            return "DRV_DROP_NOBUFS";
        }
        if (i == 9) {
            return "DRV_DROP_OTHER";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("ACKED");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("SENT");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("FW_QUEUED");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("FW_DROP_INVALID");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("FW_DROP_NOBUFS");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("FW_DROP_OTHER");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("DRV_QUEUED");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("DRV_DROP_INVALID");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("DRV_DROP_NOBUFS");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("DRV_DROP_OTHER");
            i2 |= 9;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
