package android.hardware.health.V1_0;

import java.util.ArrayList;

public final class BatteryStatus {
    public static final int CHARGING = 2;
    public static final int DISCHARGING = 3;
    public static final int FULL = 5;
    public static final int NOT_CHARGING = 4;
    public static final int UNKNOWN = 1;

    public static final String toString(int i) {
        if (i == 1) {
            return "UNKNOWN";
        }
        if (i == 2) {
            return "CHARGING";
        }
        if (i == 3) {
            return "DISCHARGING";
        }
        if (i == 4) {
            return "NOT_CHARGING";
        }
        if (i == 5) {
            return "FULL";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("UNKNOWN");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("CHARGING");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("DISCHARGING");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("NOT_CHARGING");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("FULL");
            i2 |= 5;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
