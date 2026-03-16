package java.lang;

public final class Float extends Number implements Comparable<Float> {
    static final int EXPONENT_BIAS = 127;
    static final int EXPONENT_BITS = 9;
    static final int EXPONENT_MASK = 2139095040;
    static final int MANTISSA_BITS = 23;
    static final int MANTISSA_MASK = 8388607;
    public static final int MAX_EXPONENT = 127;
    public static final float MAX_VALUE = Float.MAX_VALUE;
    public static final int MIN_EXPONENT = -126;
    public static final float MIN_NORMAL = Float.MIN_NORMAL;
    public static final float MIN_VALUE = Float.MIN_VALUE;
    public static final float NEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
    static final int NON_MANTISSA_BITS = 9;
    public static final float NaN = Float.NaN;
    public static final float POSITIVE_INFINITY = Float.POSITIVE_INFINITY;
    static final int SIGN_MASK = Integer.MIN_VALUE;
    public static final int SIZE = 32;
    public static final Class<Float> TYPE = float[].class.getComponentType();
    private static final long serialVersionUID = -2671257302660747028L;
    private final float value;

    public static native int floatToRawIntBits(float f);

    public static native float intBitsToFloat(int i);

    public Float(float value) {
        this.value = value;
    }

    public Float(double value) {
        this.value = (float) value;
    }

    public Float(String string) throws NumberFormatException {
        this(parseFloat(string));
    }

    @Override
    public int compareTo(Float object) {
        return compare(this.value, object.value);
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public boolean equals(Object object) {
        return (object instanceof Float) && floatToIntBits(this.value) == floatToIntBits(((Float) object).value);
    }

    public static int floatToIntBits(float value) {
        if (value != value) {
            return 2143289344;
        }
        return floatToRawIntBits(value);
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    public int hashCode() {
        return floatToIntBits(this.value);
    }

    @Override
    public int intValue() {
        return (int) this.value;
    }

    public boolean isInfinite() {
        return isInfinite(this.value);
    }

    public static boolean isInfinite(float f) {
        return f == Float.POSITIVE_INFINITY || f == Float.NEGATIVE_INFINITY;
    }

    public boolean isNaN() {
        return isNaN(this.value);
    }

    public static boolean isNaN(float f) {
        return f != f;
    }

    @Override
    public long longValue() {
        return (long) this.value;
    }

    public static float parseFloat(String string) throws NumberFormatException {
        return StringToReal.parseFloat(string);
    }

    @Override
    public short shortValue() {
        return (short) this.value;
    }

    public String toString() {
        return toString(this.value);
    }

    public static String toString(float f) {
        return RealToString.getInstance().floatToString(f);
    }

    public static Float valueOf(String string) throws NumberFormatException {
        return valueOf(parseFloat(string));
    }

    public static int compare(float float1, float float2) {
        if (float1 > float2) {
            return 1;
        }
        if (float2 > float1) {
            return -1;
        }
        if (float1 == float2 && 0.0f != float1) {
            return 0;
        }
        if (isNaN(float1)) {
            return isNaN(float2) ? 0 : 1;
        }
        if (isNaN(float2)) {
            return -1;
        }
        int f1 = floatToRawIntBits(float1);
        int f2 = floatToRawIntBits(float2);
        return (f1 >> 31) - (f2 >> 31);
    }

    public static Float valueOf(float f) {
        return new Float(f);
    }

    public static String toHexString(float f) {
        if (f != f) {
            return "NaN";
        }
        if (f == Float.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (f == Float.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        int bitValue = floatToIntBits(f);
        boolean negative = (Integer.MIN_VALUE & bitValue) != 0;
        int exponent = (EXPONENT_MASK & bitValue) >>> 23;
        int significand = (MANTISSA_MASK & bitValue) << 1;
        if (exponent == 0 && significand == 0) {
            return negative ? "-0x0.0p0" : "0x0.0p0";
        }
        StringBuilder hexString = new StringBuilder(10);
        if (negative) {
            hexString.append("-0x");
        } else {
            hexString.append("0x");
        }
        if (exponent == 0) {
            hexString.append("0.");
            int fractionDigits = 6;
            while (significand != 0 && (significand & 15) == 0) {
                significand >>>= 4;
                fractionDigits--;
            }
            String hexSignificand = Integer.toHexString(significand);
            if (significand != 0 && fractionDigits > hexSignificand.length()) {
                int digitDiff = fractionDigits - hexSignificand.length();
                while (true) {
                    int digitDiff2 = digitDiff;
                    digitDiff = digitDiff2 - 1;
                    if (digitDiff2 == 0) {
                        break;
                    }
                    hexString.append('0');
                }
            }
            hexString.append(hexSignificand);
            hexString.append("p-126");
        } else {
            hexString.append("1.");
            int fractionDigits2 = 6;
            while (significand != 0 && (significand & 15) == 0) {
                significand >>>= 4;
                fractionDigits2--;
            }
            String hexSignificand2 = Integer.toHexString(significand);
            if (significand != 0 && fractionDigits2 > hexSignificand2.length()) {
                int digitDiff3 = fractionDigits2 - hexSignificand2.length();
                while (true) {
                    int digitDiff4 = digitDiff3;
                    digitDiff3 = digitDiff4 - 1;
                    if (digitDiff4 == 0) {
                        break;
                    }
                    hexString.append('0');
                }
            }
            hexString.append(hexSignificand2);
            hexString.append('p');
            hexString.append(exponent - 127);
        }
        return hexString.toString();
    }
}
