package java.lang;

@FindBugsSuppressWarnings({"DM_NUMBER_CTOR"})
public final class Long extends Number implements Comparable<Long> {
    public static final long MAX_VALUE = Long.MAX_VALUE;
    public static final long MIN_VALUE = Long.MIN_VALUE;
    public static final int SIZE = 64;
    private static final long serialVersionUID = 4290774380558885855L;
    private final long value;
    public static final Class<Long> TYPE = long[].class.getComponentType();
    private static final Long[] SMALL_VALUES = new Long[256];

    static {
        for (int i = -128; i < 128; i++) {
            SMALL_VALUES[i + 128] = new Long(i);
        }
    }

    public Long(long value) {
        this.value = value;
    }

    public Long(String string) throws NumberFormatException {
        this(parseLong(string));
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    @Override
    public int compareTo(Long object) {
        return compare(this.value, object.value);
    }

    public static int compare(long lhs, long rhs) {
        if (lhs < rhs) {
            return -1;
        }
        return lhs == rhs ? 0 : 1;
    }

    private static NumberFormatException invalidLong(String s) {
        throw new NumberFormatException("Invalid long: \"" + s + "\"");
    }

    public static Long decode(String string) throws NumberFormatException {
        int length = string.length();
        if (length == 0) {
            throw invalidLong(string);
        }
        int i = 0;
        char firstDigit = string.charAt(0);
        boolean negative = firstDigit == '-';
        if (negative || firstDigit == '+') {
            if (length == 1) {
                throw invalidLong(string);
            }
            i = 0 + 1;
            firstDigit = string.charAt(i);
        }
        int base = 10;
        if (firstDigit == '0') {
            i++;
            if (i == length) {
                return 0L;
            }
            char firstDigit2 = string.charAt(i);
            if (firstDigit2 == 'x' || firstDigit2 == 'X') {
                if (i == length) {
                    throw invalidLong(string);
                }
                i++;
                base = 16;
            } else {
                base = 8;
            }
        } else if (firstDigit == '#') {
            if (i == length) {
                throw invalidLong(string);
            }
            i++;
            base = 16;
        }
        long result = parse(string, i, base, negative);
        return valueOf(result);
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public boolean equals(Object o) {
        return (o instanceof Long) && ((Long) o).value == this.value;
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    public static Long getLong(String string) {
        String prop;
        if (string == null || string.length() == 0 || (prop = System.getProperty(string)) == null) {
            return null;
        }
        try {
            return decode(prop);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long getLong(String string, long defaultValue) {
        if (string == null || string.length() == 0) {
            return valueOf(defaultValue);
        }
        String prop = System.getProperty(string);
        if (prop == null) {
            return valueOf(defaultValue);
        }
        try {
            return decode(prop);
        } catch (NumberFormatException e) {
            return valueOf(defaultValue);
        }
    }

    public static Long getLong(String string, Long defaultValue) {
        String prop;
        if (string != null && string.length() != 0 && (prop = System.getProperty(string)) != null) {
            try {
                return decode(prop);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int hashCode() {
        return (int) (this.value ^ (this.value >>> 32));
    }

    @Override
    public int intValue() {
        return (int) this.value;
    }

    @Override
    public long longValue() {
        return this.value;
    }

    public static long parseLong(String string) throws NumberFormatException {
        return parseLong(string, 10);
    }

    public static long parseLong(String string, int radix) throws NumberFormatException {
        if (radix < 2 || radix > 36) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.isEmpty()) {
            throw invalidLong(string);
        }
        char firstChar = string.charAt(0);
        int firstDigitIndex = (firstChar == '-' || firstChar == '+') ? 1 : 0;
        if (firstDigitIndex == string.length()) {
            throw invalidLong(string);
        }
        return parse(string, firstDigitIndex, radix, firstChar == '-');
    }

    private static long parse(String string, int offset, int radix, boolean negative) {
        long max = Long.MIN_VALUE / ((long) radix);
        long result = 0;
        int length = string.length();
        int offset2 = offset;
        while (offset2 < length) {
            int offset3 = offset2 + 1;
            int digit = Character.digit(string.charAt(offset2), radix);
            if (digit == -1) {
                throw invalidLong(string);
            }
            if (max > result) {
                throw invalidLong(string);
            }
            long next = (((long) radix) * result) - ((long) digit);
            if (next > result) {
                throw invalidLong(string);
            }
            result = next;
            offset2 = offset3;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidLong(string);
            }
        }
        return result;
    }

    public static long parsePositiveLong(String string) throws NumberFormatException {
        return parsePositiveLong(string, 10);
    }

    public static long parsePositiveLong(String string, int radix) throws NumberFormatException {
        if (radix < 2 || radix > 36) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.length() == 0) {
            throw invalidLong(string);
        }
        return parse(string, 0, radix, false);
    }

    @Override
    public short shortValue() {
        return (short) this.value;
    }

    public static String toBinaryString(long v) {
        return IntegralToString.longToBinaryString(v);
    }

    public static String toHexString(long v) {
        return IntegralToString.longToHexString(v);
    }

    public static String toOctalString(long v) {
        return IntegralToString.longToOctalString(v);
    }

    public String toString() {
        return toString(this.value);
    }

    public static String toString(long n) {
        return IntegralToString.longToString(n);
    }

    public static String toString(long v, int radix) {
        return IntegralToString.longToString(v, radix);
    }

    public static Long valueOf(String string) throws NumberFormatException {
        return valueOf(parseLong(string));
    }

    public static Long valueOf(String string, int radix) throws NumberFormatException {
        return valueOf(parseLong(string, radix));
    }

    public static long highestOneBit(long v) {
        long v2 = v | (v >> 1);
        long v3 = v2 | (v2 >> 2);
        long v4 = v3 | (v3 >> 4);
        long v5 = v4 | (v4 >> 8);
        long v6 = v5 | (v5 >> 16);
        long v7 = v6 | (v6 >> 32);
        return v7 - (v7 >>> 1);
    }

    public static long lowestOneBit(long v) {
        return (-v) & v;
    }

    public static int numberOfLeadingZeros(long v) {
        if (v < 0) {
            return 0;
        }
        if (v == 0) {
            return 64;
        }
        int n = 1;
        int i = (int) (v >>> 32);
        if (i == 0) {
            n = 1 + 32;
            i = (int) v;
        }
        if ((i >>> 16) == 0) {
            n += 16;
            i <<= 16;
        }
        if ((i >>> 24) == 0) {
            n += 8;
            i <<= 8;
        }
        if ((i >>> 28) == 0) {
            n += 4;
            i <<= 4;
        }
        if ((i >>> 30) == 0) {
            n += 2;
            i <<= 2;
        }
        return n - (i >>> 31);
    }

    public static int numberOfTrailingZeros(long v) {
        int low = (int) v;
        return low != 0 ? Integer.numberOfTrailingZeros(low) : Integer.numberOfTrailingZeros((int) (v >>> 32)) + 32;
    }

    public static int bitCount(long v) {
        long v2 = v - ((v >>> 1) & 6148914691236517205L);
        long v3 = (v2 & 3689348814741910323L) + ((v2 >>> 2) & 3689348814741910323L);
        int i = ((int) (v3 >>> 32)) + ((int) v3);
        int i2 = (i & 252645135) + ((i >>> 4) & 252645135);
        int i3 = i2 + (i2 >>> 8);
        return (i3 + (i3 >>> 16)) & 127;
    }

    public static long rotateLeft(long v, int distance) {
        return (v << distance) | (v >>> (-distance));
    }

    public static long rotateRight(long v, int distance) {
        return (v >>> distance) | (v << (-distance));
    }

    public static long reverseBytes(long v) {
        long v2 = ((v >>> 8) & 71777214294589695L) | ((71777214294589695L & v) << 8);
        long v3 = ((v2 >>> 16) & 281470681808895L) | ((v2 & 281470681808895L) << 16);
        return (v3 >>> 32) | (v3 << 32);
    }

    public static long reverse(long v) {
        long v2 = ((v >>> 1) & 6148914691236517205L) | ((6148914691236517205L & v) << 1);
        long v3 = ((v2 >>> 2) & 3689348814741910323L) | ((3689348814741910323L & v2) << 2);
        long v4 = ((v3 >>> 4) & 1085102592571150095L) | ((1085102592571150095L & v3) << 4);
        long v5 = ((v4 >>> 8) & 71777214294589695L) | ((71777214294589695L & v4) << 8);
        long v6 = ((v5 >>> 16) & 281470681808895L) | ((281470681808895L & v5) << 16);
        return (v6 >>> 32) | (v6 << 32);
    }

    public static int signum(long v) {
        if (v < 0) {
            return -1;
        }
        return v == 0 ? 0 : 1;
    }

    public static Long valueOf(long v) {
        return (v >= 128 || v < -128) ? new Long(v) : SMALL_VALUES[((int) v) + 128];
    }
}
