package android.hardware.secure_element.V1_0;

import java.util.ArrayList;

public final class SecureElementStatus {
    public static final byte CHANNEL_NOT_AVAILABLE = 2;
    public static final byte FAILED = 1;
    public static final byte IOERROR = 5;
    public static final byte NO_SUCH_ELEMENT_ERROR = 3;
    public static final byte SUCCESS = 0;
    public static final byte UNSUPPORTED_OPERATION = 4;

    public static final String toString(byte b) {
        if (b == 0) {
            return "SUCCESS";
        }
        if (b == 1) {
            return "FAILED";
        }
        if (b == 2) {
            return "CHANNEL_NOT_AVAILABLE";
        }
        if (b == 3) {
            return "NO_SUCH_ELEMENT_ERROR";
        }
        if (b == 4) {
            return "UNSUPPORTED_OPERATION";
        }
        if (b == 5) {
            return "IOERROR";
        }
        return "0x" + Integer.toHexString(Byte.toUnsignedInt(b));
    }

    public static final String dumpBitfield(byte b) {
        byte b2;
        ArrayList arrayList = new ArrayList();
        arrayList.add("SUCCESS");
        if ((b & 1) == 1) {
            arrayList.add("FAILED");
            b2 = (byte) 1;
        } else {
            b2 = 0;
        }
        if ((b & 2) == 2) {
            arrayList.add("CHANNEL_NOT_AVAILABLE");
            b2 = (byte) (b2 | 2);
        }
        if ((b & 3) == 3) {
            arrayList.add("NO_SUCH_ELEMENT_ERROR");
            b2 = (byte) (b2 | 3);
        }
        if ((b & 4) == 4) {
            arrayList.add("UNSUPPORTED_OPERATION");
            b2 = (byte) (b2 | 4);
        }
        if ((b & 5) == 5) {
            arrayList.add("IOERROR");
            b2 = (byte) (b2 | 5);
        }
        if (b != b2) {
            arrayList.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte) (b & (~b2)))));
        }
        return String.join(" | ", arrayList);
    }
}
