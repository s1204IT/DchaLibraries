package java.lang;

import sun.misc.DoubleConsts;
import sun.misc.FloatingDecimal;
import sun.util.locale.LanguageTag;

public final class Double extends Number implements Comparable<Double> {
    public static final int BYTES = 8;
    public static final int MAX_EXPONENT = 1023;
    public static final double MAX_VALUE = Double.MAX_VALUE;
    public static final int MIN_EXPONENT = -1022;
    public static final double MIN_NORMAL = Double.MIN_NORMAL;
    public static final double MIN_VALUE = Double.MIN_VALUE;
    public static final double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;
    public static final double NaN = Double.NaN;
    public static final double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;
    public static final int SIZE = 64;
    public static final Class<Double> TYPE = Class.getPrimitiveClass("double");
    private static final long serialVersionUID = -9172774392245257468L;
    private final double value;

    public static native long doubleToRawLongBits(double d);

    public static native double longBitsToDouble(long j);

    public static String toString(double d) {
        return FloatingDecimal.toJavaFormatString(d);
    }

    public static String toHexString(double d) {
        String strReplaceFirst;
        int exponent;
        if (!isFinite(d)) {
            return toString(d);
        }
        StringBuilder sb = new StringBuilder(24);
        if (Math.copySign(1.0d, d) == -1.0d) {
            sb.append(LanguageTag.SEP);
        }
        sb.append("0x");
        double dAbs = Math.abs(d);
        if (dAbs == 0.0d) {
            sb.append("0.0p0");
        } else {
            boolean z = dAbs < Double.MIN_NORMAL;
            long jDoubleToLongBits = (doubleToLongBits(dAbs) & DoubleConsts.SIGNIF_BIT_MASK) | 1152921504606846976L;
            sb.append(z ? "0." : "1.");
            String strSubstring = Long.toHexString(jDoubleToLongBits).substring(3, 16);
            if (strSubstring.equals("0000000000000")) {
                strReplaceFirst = "0";
            } else {
                strReplaceFirst = strSubstring.replaceFirst("0{1,12}$", "");
            }
            sb.append(strReplaceFirst);
            sb.append('p');
            if (z) {
                exponent = -1022;
            } else {
                exponent = Math.getExponent(dAbs);
            }
            sb.append(exponent);
        }
        return sb.toString();
    }

    public static Double valueOf(String str) throws NumberFormatException {
        return new Double(parseDouble(str));
    }

    public static Double valueOf(double d) {
        return new Double(d);
    }

    public static double parseDouble(String str) throws NumberFormatException {
        return FloatingDecimal.parseDouble(str);
    }

    public static boolean isNaN(double d) {
        return d != d;
    }

    public static boolean isInfinite(double d) {
        return d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY;
    }

    public static boolean isFinite(double d) {
        return Math.abs(d) <= Double.MAX_VALUE;
    }

    public Double(double d) {
        this.value = d;
    }

    public Double(String str) throws NumberFormatException {
        this.value = parseDouble(str);
    }

    public boolean isNaN() {
        return isNaN(this.value);
    }

    public boolean isInfinite() {
        return isInfinite(this.value);
    }

    public String toString() {
        return toString(this.value);
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    @Override
    public short shortValue() {
        return (short) this.value;
    }

    @Override
    public int intValue() {
        return (int) this.value;
    }

    @Override
    public long longValue() {
        return (long) this.value;
    }

    @Override
    public float floatValue() {
        return (float) this.value;
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public int hashCode() {
        return hashCode(this.value);
    }

    public static int hashCode(double d) {
        long jDoubleToLongBits = doubleToLongBits(d);
        return (int) (jDoubleToLongBits ^ (jDoubleToLongBits >>> 32));
    }

    public boolean equals(Object obj) {
        return (obj instanceof Double) && doubleToLongBits(((Double) obj).value) == doubleToLongBits(this.value);
    }

    public static long doubleToLongBits(double d) {
        long jDoubleToRawLongBits = doubleToRawLongBits(d);
        if ((jDoubleToRawLongBits & DoubleConsts.EXP_BIT_MASK) == DoubleConsts.EXP_BIT_MASK && (DoubleConsts.SIGNIF_BIT_MASK & jDoubleToRawLongBits) != 0) {
            return 9221120237041090560L;
        }
        return jDoubleToRawLongBits;
    }

    @Override
    public int compareTo(Double d) {
        return compare(this.value, d.value);
    }

    public static int compare(double d, double d2) {
        if (d < d2) {
            return -1;
        }
        if (d > d2) {
            return 1;
        }
        long jDoubleToLongBits = doubleToLongBits(d);
        long jDoubleToLongBits2 = doubleToLongBits(d2);
        if (jDoubleToLongBits == jDoubleToLongBits2) {
            return 0;
        }
        return jDoubleToLongBits < jDoubleToLongBits2 ? -1 : 1;
    }

    public static double sum(double d, double d2) {
        return d + d2;
    }

    public static double max(double d, double d2) {
        return Math.max(d, d2);
    }

    public static double min(double d, double d2) {
        return Math.min(d, d2);
    }
}
