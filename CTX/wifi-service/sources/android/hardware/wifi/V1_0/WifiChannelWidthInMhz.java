package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiChannelWidthInMhz {
    public static final int WIDTH_10 = 6;
    public static final int WIDTH_160 = 3;
    public static final int WIDTH_20 = 0;
    public static final int WIDTH_40 = 1;
    public static final int WIDTH_5 = 5;
    public static final int WIDTH_80 = 2;
    public static final int WIDTH_80P80 = 4;
    public static final int WIDTH_INVALID = -1;

    public static final String toString(int i) {
        if (i == 0) {
            return "WIDTH_20";
        }
        if (i == 1) {
            return "WIDTH_40";
        }
        if (i == 2) {
            return "WIDTH_80";
        }
        if (i == 3) {
            return "WIDTH_160";
        }
        if (i == 4) {
            return "WIDTH_80P80";
        }
        if (i == 5) {
            return "WIDTH_5";
        }
        if (i == 6) {
            return "WIDTH_10";
        }
        if (i == -1) {
            return "WIDTH_INVALID";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("WIDTH_20");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("WIDTH_40");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("WIDTH_80");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("WIDTH_160");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("WIDTH_80P80");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("WIDTH_5");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("WIDTH_10");
            i2 |= 6;
        }
        if ((i & (-1)) == -1) {
            arrayList.add("WIDTH_INVALID");
            i2 |= -1;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
