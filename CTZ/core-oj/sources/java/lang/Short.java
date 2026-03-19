package java.lang;

public final class Short extends Number implements Comparable<Short> {
    public static final int BYTES = 2;
    public static final short MAX_VALUE = Short.MAX_VALUE;
    public static final short MIN_VALUE = Short.MIN_VALUE;
    public static final int SIZE = 16;
    public static final Class<Short> TYPE = Class.getPrimitiveClass("short");
    private static final long serialVersionUID = 7515723908773894738L;
    private final short value;

    public static String toString(short s) {
        return Integer.toString(s, 10);
    }

    public static short parseShort(String str, int i) throws NumberFormatException {
        int i2 = Integer.parseInt(str, i);
        if (i2 < -32768 || i2 > 32767) {
            throw new NumberFormatException("Value out of range. Value:\"" + str + "\" Radix:" + i);
        }
        return (short) i2;
    }

    public static short parseShort(String str) throws NumberFormatException {
        return parseShort(str, 10);
    }

    public static Short valueOf(String str, int i) throws NumberFormatException {
        return valueOf(parseShort(str, i));
    }

    public static Short valueOf(String str) throws NumberFormatException {
        return valueOf(str, 10);
    }

    private static class ShortCache {
        static final Short[] cache = new Short[256];

        private ShortCache() {
        }

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new Short((short) (i - 128));
            }
        }
    }

    public static Short valueOf(short s) {
        if (s >= -128 && s <= 127) {
            return ShortCache.cache[s + 128];
        }
        return new Short(s);
    }

    public static Short decode(String str) throws NumberFormatException {
        int iIntValue = Integer.decode(str).intValue();
        if (iIntValue < -32768 || iIntValue > 32767) {
            throw new NumberFormatException("Value " + iIntValue + " out of range from input " + str);
        }
        return valueOf((short) iIntValue);
    }

    public Short(short s) {
        this.value = s;
    }

    public Short(String str) throws NumberFormatException {
        this.value = parseShort(str, 10);
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
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

    public static int hashCode(short s) {
        return s;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Short) && this.value == ((Short) obj).shortValue();
    }

    @Override
    public int compareTo(Short sh) {
        return compare(this.value, sh.value);
    }

    public static int compare(short s, short s2) {
        return s - s2;
    }

    public static short reverseBytes(short s) {
        return (short) ((s << 8) | ((65280 & s) >> 8));
    }

    public static int toUnsignedInt(short s) {
        return s & 65535;
    }

    public static long toUnsignedLong(short s) {
        return ((long) s) & 65535;
    }
}
