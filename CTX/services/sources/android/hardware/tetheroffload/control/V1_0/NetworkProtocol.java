package android.hardware.tetheroffload.control.V1_0;

import java.util.ArrayList;

public final class NetworkProtocol {
    public static final int TCP = 6;
    public static final int UDP = 17;

    public static final String toString(int i) {
        if (i == 6) {
            return "TCP";
        }
        if (i == 17) {
            return "UDP";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 6;
        if ((i & 6) == 6) {
            arrayList.add("TCP");
        } else {
            i2 = 0;
        }
        if ((i & 17) == 17) {
            arrayList.add("UDP");
            i2 |= 17;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
