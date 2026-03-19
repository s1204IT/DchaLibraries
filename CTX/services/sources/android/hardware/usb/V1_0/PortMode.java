package android.hardware.usb.V1_0;

import java.util.ArrayList;

public final class PortMode {
    public static final int DFP = 2;
    public static final int DRP = 3;
    public static final int NONE = 0;
    public static final int NUM_MODES = 4;
    public static final int UFP = 1;

    public static final String toString(int i) {
        if (i == 0) {
            return "NONE";
        }
        if (i == 1) {
            return "UFP";
        }
        if (i == 2) {
            return "DFP";
        }
        if (i == 3) {
            return "DRP";
        }
        if (i == 4) {
            return "NUM_MODES";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("NONE");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("UFP");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("DFP");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("DRP");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("NUM_MODES");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
