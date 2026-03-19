package java.lang;

import java.util.Locale;

public final class Byte extends Number implements Comparable<Byte> {
    public static final int BYTES = 1;
    public static final byte MAX_VALUE = 127;
    public static final byte MIN_VALUE = -128;
    public static final int SIZE = 8;
    private static final long serialVersionUID = -7183698231559129828L;
    private final byte value;
    public static final Class<Byte> TYPE = Class.getPrimitiveClass("byte");
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z'};
    private static final char[] UPPER_CASE_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    public static String toString(byte b) {
        return Integer.toString(b, 10);
    }

    private static class ByteCache {
        static final Byte[] cache = new Byte[256];

        private ByteCache() {
        }

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new Byte((byte) (i - 128));
            }
        }
    }

    public static Byte valueOf(byte b) {
        return ByteCache.cache[b + 128];
    }

    public static byte parseByte(String str, int i) throws NumberFormatException {
        int i2 = Integer.parseInt(str, i);
        if (i2 < -128 || i2 > 127) {
            throw new NumberFormatException("Value out of range. Value:\"" + str + "\" Radix:" + i);
        }
        return (byte) i2;
    }

    public static byte parseByte(String str) throws NumberFormatException {
        return parseByte(str, 10);
    }

    public static Byte valueOf(String str, int i) throws NumberFormatException {
        return valueOf(parseByte(str, i));
    }

    public static Byte valueOf(String str) throws NumberFormatException {
        return valueOf(str, 10);
    }

    public static Byte decode(String str) throws NumberFormatException {
        int iIntValue = Integer.decode(str).intValue();
        if (iIntValue < -128 || iIntValue > 127) {
            throw new NumberFormatException("Value " + iIntValue + " out of range from input " + str);
        }
        return valueOf((byte) iIntValue);
    }

    public Byte(byte b) {
        this.value = b;
    }

    public Byte(String str) throws NumberFormatException {
        this.value = parseByte(str, 10);
    }

    @Override
    public byte byteValue() {
        return this.value;
    }

    @Override
    public short shortValue() {
        return this.value;
    }

    @Override
    public int intValue() {
        return this.value;
    }

    @Override
    public long longValue() {
        return this.value;
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public String toString() {
        return Integer.toString(this.value);
    }

    public int hashCode() {
        return hashCode(this.value);
    }

    public static int hashCode(byte b) {
        return b;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Byte) && this.value == ((Byte) obj).byteValue();
    }

    @Override
    public int compareTo(Byte b) {
        return compare(this.value, b.value);
    }

    public static int compare(byte b, byte b2) {
        return b - b2;
    }

    public static int toUnsignedInt(byte b) {
        return b & Character.DIRECTIONALITY_UNDEFINED;
    }

    public static long toUnsignedLong(byte b) {
        return ((long) b) & 255;
    }

    public static String toHexString(byte b, boolean z) {
        char[] cArr = z ? UPPER_CASE_DIGITS : DIGITS;
        return new String(0, 2, new char[]{cArr[(b >> 4) & 15], cArr[b & 15]});
    }
}
