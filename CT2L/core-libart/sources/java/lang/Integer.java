package java.lang;

@FindBugsSuppressWarnings({"DM_NUMBER_CTOR"})
public final class Integer extends Number implements Comparable<Integer> {
    public static final int MAX_VALUE = Integer.MAX_VALUE;
    public static final int MIN_VALUE = Integer.MIN_VALUE;
    public static final int SIZE = 32;
    private static final long serialVersionUID = 1360826667806852920L;
    private final int value;
    private static final byte[] NTZ_TABLE = {32, 0, 1, 12, 2, 6, -1, 13, 3, -1, 7, -1, -1, -1, -1, 14, 10, 4, -1, -1, 8, -1, -1, Character.MATH_SYMBOL, -1, -1, -1, -1, -1, Character.START_PUNCTUATION, Character.MODIFIER_SYMBOL, 15, 31, 11, 5, -1, -1, -1, -1, -1, 9, -1, -1, Character.OTHER_PUNCTUATION, -1, -1, Character.DASH_PUNCTUATION, Character.CURRENCY_SYMBOL, Character.FINAL_QUOTE_PUNCTUATION, -1, -1, -1, -1, Character.CONNECTOR_PUNCTUATION, -1, Character.SURROGATE, Character.INITIAL_QUOTE_PUNCTUATION, -1, Character.END_PUNCTUATION, 18, Character.OTHER_SYMBOL, Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE, 16, -1};
    public static final Class<Integer> TYPE = int[].class.getComponentType();
    private static final Integer[] SMALL_VALUES = new Integer[256];

    static {
        for (int i = -128; i < 128; i++) {
            SMALL_VALUES[i + 128] = new Integer(i);
        }
    }

    public Integer(int value) {
        this.value = value;
    }

    public Integer(String string) throws NumberFormatException {
        this(parseInt(string));
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    @Override
    public int compareTo(Integer object) {
        return compare(this.value, object.value);
    }

    public static int compare(int lhs, int rhs) {
        if (lhs < rhs) {
            return -1;
        }
        return lhs == rhs ? 0 : 1;
    }

    private static NumberFormatException invalidInt(String s) {
        throw new NumberFormatException("Invalid int: \"" + s + "\"");
    }

    public static Integer decode(String string) throws NumberFormatException {
        int length = string.length();
        if (length == 0) {
            throw invalidInt(string);
        }
        int i = 0;
        char firstDigit = string.charAt(0);
        boolean negative = firstDigit == '-';
        if (negative || firstDigit == '+') {
            if (length == 1) {
                throw invalidInt(string);
            }
            i = 0 + 1;
            firstDigit = string.charAt(i);
        }
        int base = 10;
        if (firstDigit == '0') {
            i++;
            if (i == length) {
                return 0;
            }
            char firstDigit2 = string.charAt(i);
            if (firstDigit2 == 'x' || firstDigit2 == 'X') {
                i++;
                if (i == length) {
                    throw invalidInt(string);
                }
                base = 16;
            } else {
                base = 8;
            }
        } else if (firstDigit == '#') {
            i++;
            if (i == length) {
                throw invalidInt(string);
            }
            base = 16;
        }
        int result = parse(string, i, base, negative);
        return valueOf(result);
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public boolean equals(Object o) {
        return (o instanceof Integer) && ((Integer) o).value == this.value;
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    public static Integer getInteger(String string) {
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

    public static Integer getInteger(String string, int defaultValue) {
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

    public static Integer getInteger(String string, Integer defaultValue) {
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

    public static int parseInt(String string) throws NumberFormatException {
        return parseInt(string, 10);
    }

    public static int parseInt(String string, int radix) throws NumberFormatException {
        if (radix < 2 || radix > 36) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.isEmpty()) {
            throw invalidInt(string);
        }
        char firstChar = string.charAt(0);
        int firstDigitIndex = (firstChar == '-' || firstChar == '+') ? 1 : 0;
        if (firstDigitIndex == string.length()) {
            throw invalidInt(string);
        }
        return parse(string, firstDigitIndex, radix, firstChar == '-');
    }

    public static int parsePositiveInt(String string) throws NumberFormatException {
        return parsePositiveInt(string, 10);
    }

    public static int parsePositiveInt(String string, int radix) throws NumberFormatException {
        if (radix < 2 || radix > 36) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.length() == 0) {
            throw invalidInt(string);
        }
        return parse(string, 0, radix, false);
    }

    private static int parse(String string, int offset, int radix, boolean negative) throws NumberFormatException {
        int max = Integer.MIN_VALUE / radix;
        int result = 0;
        int length = string.length();
        int offset2 = offset;
        while (offset2 < length) {
            int offset3 = offset2 + 1;
            int digit = Character.digit(string.charAt(offset2), radix);
            if (digit == -1) {
                throw invalidInt(string);
            }
            if (max > result) {
                throw invalidInt(string);
            }
            int next = (result * radix) - digit;
            if (next > result) {
                throw invalidInt(string);
            }
            result = next;
            offset2 = offset3;
        }
        if (!negative && (result = -result) < 0) {
            throw invalidInt(string);
        }
        return result;
    }

    @Override
    public short shortValue() {
        return (short) this.value;
    }

    public static String toBinaryString(int i) {
        return IntegralToString.intToBinaryString(i);
    }

    public static String toHexString(int i) {
        return IntegralToString.intToHexString(i, false, 0);
    }

    public static String toOctalString(int i) {
        return IntegralToString.intToOctalString(i);
    }

    public String toString() {
        return toString(this.value);
    }

    public static String toString(int i) {
        return IntegralToString.intToString(i);
    }

    public static String toString(int i, int radix) {
        return IntegralToString.intToString(i, radix);
    }

    public static Integer valueOf(String string) throws NumberFormatException {
        return valueOf(parseInt(string));
    }

    public static Integer valueOf(String string, int radix) throws NumberFormatException {
        return valueOf(parseInt(string, radix));
    }

    public static int highestOneBit(int i) {
        int i2 = i | (i >> 1);
        int i3 = i2 | (i2 >> 2);
        int i4 = i3 | (i3 >> 4);
        int i5 = i4 | (i4 >> 8);
        int i6 = i5 | (i5 >> 16);
        return i6 - (i6 >>> 1);
    }

    public static int lowestOneBit(int i) {
        return (-i) & i;
    }

    public static int numberOfLeadingZeros(int i) {
        if (i <= 0) {
            return ((i ^ (-1)) >> 26) & 32;
        }
        int n = 1;
        if ((i >> 16) == 0) {
            n = 1 + 16;
            i <<= 16;
        }
        if ((i >> 24) == 0) {
            n += 8;
            i <<= 8;
        }
        if ((i >> 28) == 0) {
            n += 4;
            i <<= 4;
        }
        if ((i >> 30) == 0) {
            n += 2;
            i <<= 2;
        }
        return n - (i >>> 31);
    }

    public static int numberOfTrailingZeros(int i) {
        return NTZ_TABLE[(((-i) & i) * 72416175) >>> 26];
    }

    public static int bitCount(int i) {
        int i2 = i - ((i >> 1) & 1431655765);
        int i3 = (i2 & 858993459) + ((i2 >> 2) & 858993459);
        int i4 = ((i3 >> 4) + i3) & 252645135;
        int i5 = i4 + (i4 >> 8);
        return (i5 + (i5 >> 16)) & 63;
    }

    public static int rotateLeft(int i, int distance) {
        return (i << distance) | (i >>> (-distance));
    }

    public static int rotateRight(int i, int distance) {
        return (i >>> distance) | (i << (-distance));
    }

    public static int reverseBytes(int i) {
        int i2 = ((i >>> 8) & 16711935) | ((16711935 & i) << 8);
        return (i2 >>> 16) | (i2 << 16);
    }

    public static int reverse(int i) {
        int i2 = ((i >>> 1) & 1431655765) | ((1431655765 & i) << 1);
        int i3 = ((i2 >>> 2) & 858993459) | ((i2 & 858993459) << 2);
        int i4 = ((i3 >>> 4) & 252645135) | ((i3 & 252645135) << 4);
        int i5 = ((i4 >>> 8) & 16711935) | ((i4 & 16711935) << 8);
        return (i5 >>> 16) | (i5 << 16);
    }

    public static int signum(int i) {
        return (i >> 31) | ((-i) >>> 31);
    }

    public static Integer valueOf(int i) {
        return (i >= 128 || i < -128) ? new Integer(i) : SMALL_VALUES[i + 128];
    }
}
