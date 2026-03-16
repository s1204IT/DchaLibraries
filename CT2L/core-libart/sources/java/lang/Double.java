package java.lang;

public final class Double extends Number implements Comparable<Double> {
    static final int EXPONENT_BIAS = 1023;
    static final int EXPONENT_BITS = 12;
    static final long EXPONENT_MASK = 9218868437227405312L;
    static final int MANTISSA_BITS = 52;
    static final long MANTISSA_MASK = 4503599627370495L;
    public static final int MAX_EXPONENT = 1023;
    public static final double MAX_VALUE = Double.MAX_VALUE;
    public static final int MIN_EXPONENT = -1022;
    public static final double MIN_NORMAL = Double.MIN_NORMAL;
    public static final double MIN_VALUE = Double.MIN_VALUE;
    public static final double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;
    static final int NON_MANTISSA_BITS = 12;
    public static final double NaN = Double.NaN;
    public static final double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;
    static final long SIGN_MASK = Long.MIN_VALUE;
    public static final int SIZE = 64;
    public static final Class<Double> TYPE = double[].class.getComponentType();
    private static final long serialVersionUID = -9172774392245257468L;
    private final double value;

    public static native long doubleToRawLongBits(double d);

    public static native double longBitsToDouble(long j);

    public Double(double value) {
        this.value = value;
    }

    public Double(String string) throws NumberFormatException {
        this(parseDouble(string));
    }

    @Override
    public int compareTo(Double object) {
        return compare(this.value, object.value);
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    public static long doubleToLongBits(double value) {
        if (value != value) {
            return 9221120237041090560L;
        }
        return doubleToRawLongBits(value);
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public boolean equals(Object object) {
        return (object instanceof Double) && doubleToLongBits(this.value) == doubleToLongBits(((Double) object).value);
    }

    @Override
    public float floatValue() {
        return (float) this.value;
    }

    public int hashCode() {
        long v = doubleToLongBits(this.value);
        return (int) ((v >>> 32) ^ v);
    }

    @Override
    public int intValue() {
        return (int) this.value;
    }

    public boolean isInfinite() {
        return isInfinite(this.value);
    }

    public static boolean isInfinite(double d) {
        return d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY;
    }

    public boolean isNaN() {
        return isNaN(this.value);
    }

    public static boolean isNaN(double d) {
        return d != d;
    }

    @Override
    public long longValue() {
        return (long) this.value;
    }

    public static double parseDouble(String string) throws NumberFormatException {
        return StringToReal.parseDouble(string);
    }

    @Override
    public short shortValue() {
        return (short) this.value;
    }

    public String toString() {
        return toString(this.value);
    }

    public static String toString(double d) {
        return RealToString.getInstance().doubleToString(d);
    }

    public static Double valueOf(String string) throws NumberFormatException {
        return valueOf(parseDouble(string));
    }

    public static int compare(double double1, double double2) {
        if (double1 > double2) {
            return 1;
        }
        if (double2 > double1) {
            return -1;
        }
        if (double1 == double2 && 0.0d != double1) {
            return 0;
        }
        if (isNaN(double1)) {
            return isNaN(double2) ? 0 : 1;
        }
        if (isNaN(double2)) {
            return -1;
        }
        long d1 = doubleToRawLongBits(double1);
        long d2 = doubleToRawLongBits(double2);
        return (int) ((d1 >> 63) - (d2 >> 63));
    }

    public static Double valueOf(double d) {
        return new Double(d);
    }

    public static String toHexString(double d) {
        if (d != d) {
            return "NaN";
        }
        if (d == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        long bitValue = doubleToLongBits(d);
        boolean negative = (Long.MIN_VALUE & bitValue) != 0;
        long exponent = (EXPONENT_MASK & bitValue) >>> 52;
        long significand = bitValue & MANTISSA_MASK;
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
            int fractionDigits = 13;
            while (significand != 0 && (15 & significand) == 0) {
                significand >>>= 4;
                fractionDigits--;
            }
            String hexSignificand = Long.toHexString(significand);
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
            hexString.append("p-1022");
        } else {
            hexString.append("1.");
            int fractionDigits2 = 13;
            while (significand != 0 && (15 & significand) == 0) {
                significand >>>= 4;
                fractionDigits2--;
            }
            String hexSignificand2 = Long.toHexString(significand);
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
            hexString.append(Long.toString(exponent - 1023));
        }
        return hexString.toString();
    }
}
