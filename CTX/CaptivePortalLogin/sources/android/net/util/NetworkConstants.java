package android.net.util;

public final class NetworkConstants {
    public static final byte FF = asByte(255);
    public static final byte[] ETHER_ADDR_BROADCAST = {FF, FF, FF, FF, FF, FF};

    public static byte asByte(int i) {
        return (byte) i;
    }

    public static String asString(int i) {
        return Integer.toString(i);
    }

    public static int asUint(byte b) {
        return b & 255;
    }

    public static int asUint(short s) {
        return s & 65535;
    }
}
