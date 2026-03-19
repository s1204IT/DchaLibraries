package android.hardware.broadcastradio.V2_0;

import java.util.ArrayList;

public final class Rds {
    public static final byte RBDS = 2;
    public static final byte RDS = 1;

    public static final String toString(byte b) {
        if (b == 1) {
            return "RDS";
        }
        if (b == 2) {
            return "RBDS";
        }
        return "0x" + Integer.toHexString(Byte.toUnsignedInt(b));
    }

    public static final String dumpBitfield(byte b) {
        byte b2;
        ArrayList arrayList = new ArrayList();
        if ((b & 1) == 1) {
            arrayList.add("RDS");
            b2 = (byte) 1;
        } else {
            b2 = 0;
        }
        if ((b & 2) == 2) {
            arrayList.add("RBDS");
            b2 = (byte) (b2 | 2);
        }
        if (b != b2) {
            arrayList.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte) (b & (~b2)))));
        }
        return String.join(" | ", arrayList);
    }
}
