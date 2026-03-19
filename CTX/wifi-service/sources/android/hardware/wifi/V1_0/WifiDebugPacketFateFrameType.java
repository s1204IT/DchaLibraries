package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiDebugPacketFateFrameType {
    public static final int ETHERNET_II = 1;
    public static final int MGMT_80211 = 2;
    public static final int UNKNOWN = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "UNKNOWN";
        }
        if (i == 1) {
            return "ETHERNET_II";
        }
        if (i == 2) {
            return "MGMT_80211";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("UNKNOWN");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("ETHERNET_II");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("MGMT_80211");
            i2 |= 2;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
