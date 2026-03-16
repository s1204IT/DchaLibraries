package java.lang;

import java.util.Random;

public final class Math {
    public static final double E = 2.718281828459045d;
    private static final Random INSTANCE = new Random();
    public static final double PI = 3.141592653589793d;

    public static native double IEEEremainder(double d, double d2);

    public static native double acos(double d);

    public static native double asin(double d);

    public static native double atan(double d);

    public static native double atan2(double d, double d2);

    public static native double cbrt(double d);

    public static native double ceil(double d);

    public static native double cos(double d);

    public static native double cosh(double d);

    public static native double exp(double d);

    public static native double expm1(double d);

    public static native double floor(double d);

    public static native double hypot(double d, double d2);

    public static native double log(double d);

    public static native double log10(double d);

    public static native double log1p(double d);

    private static native double nextafter(double d, double d2);

    public static native double pow(double d, double d2);

    public static native double rint(double d);

    public static native double sin(double d);

    public static native double sinh(double d);

    public static native double sqrt(double d);

    public static native double tan(double d);

    public static native double tanh(double d);

    private Math() {
    }

    public static double abs(double d) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(d) & Long.MAX_VALUE);
    }

    public static float abs(float f) {
        return Float.intBitsToFloat(Float.floatToRawIntBits(f) & Integer.MAX_VALUE);
    }

    public static int abs(int i) {
        return i >= 0 ? i : -i;
    }

    public static long abs(long l) {
        return l >= 0 ? l : -l;
    }

    public static double max(double d1, double d2) {
        if (d1 > d2) {
            return d1;
        }
        if (d1 < d2) {
            return d2;
        }
        if (d1 != d2) {
            return Double.NaN;
        }
        if (Double.doubleToRawLongBits(d1) == 0) {
            return 0.0d;
        }
        return d2;
    }

    public static float max(float f1, float f2) {
        if (f1 > f2) {
            return f1;
        }
        if (f1 < f2) {
            return f2;
        }
        if (f1 != f2) {
            return Float.NaN;
        }
        if (Float.floatToRawIntBits(f1) == 0) {
            return 0.0f;
        }
        return f2;
    }

    public static int max(int i1, int i2) {
        return i1 > i2 ? i1 : i2;
    }

    public static long max(long l1, long l2) {
        return l1 > l2 ? l1 : l2;
    }

    public static double min(double d1, double d2) {
        if (d1 > d2) {
            return d2;
        }
        if (d1 < d2) {
            return d1;
        }
        if (d1 != d2) {
            return Double.NaN;
        }
        if (Double.doubleToRawLongBits(d1) == Long.MIN_VALUE) {
            return -0.0d;
        }
        return d2;
    }

    public static float min(float f1, float f2) {
        if (f1 > f2) {
            return f2;
        }
        if (f1 < f2) {
            return f1;
        }
        if (f1 != f2) {
            return Float.NaN;
        }
        if (Float.floatToRawIntBits(f1) == Integer.MIN_VALUE) {
            return -0.0f;
        }
        return f2;
    }

    public static int min(int i1, int i2) {
        return i1 < i2 ? i1 : i2;
    }

    public static long min(long l1, long l2) {
        return l1 < l2 ? l1 : l2;
    }

    public static long round(double d) {
        if (d != d) {
            return 0L;
        }
        return (long) floor(0.5d + d);
    }

    public static int round(float f) {
        if (f != f) {
            return 0;
        }
        return (int) floor(0.5f + f);
    }

    public static double signum(double d) {
        if (Double.isNaN(d)) {
            return Double.NaN;
        }
        if (d > 0.0d) {
            return 1.0d;
        }
        if (d >= 0.0d) {
            return d;
        }
        return -1.0d;
    }

    public static float signum(float f) {
        if (Float.isNaN(f)) {
            return Float.NaN;
        }
        if (f > 0.0f) {
            return 1.0f;
        }
        if (f >= 0.0f) {
            return f;
        }
        return -1.0f;
    }

    public static double random() {
        return INSTANCE.nextDouble();
    }

    public static void setRandomSeedInternal(long seed) {
        INSTANCE.setSeed(seed);
    }

    public static int randomIntInternal() {
        return INSTANCE.nextInt();
    }

    public static double toRadians(double angdeg) {
        return (angdeg / 180.0d) * 3.141592653589793d;
    }

    public static double toDegrees(double angrad) {
        return (180.0d * angrad) / 3.141592653589793d;
    }

    public static double ulp(double d) {
        if (Double.isInfinite(d)) {
            return Double.POSITIVE_INFINITY;
        }
        if (d == Double.MAX_VALUE || d == -1.7976931348623157E308d) {
            return pow(2.0d, 971.0d);
        }
        double d2 = abs(d);
        return nextafter(d2, Double.MAX_VALUE) - d2;
    }

    public static float ulp(float f) {
        int hx;
        if (Float.isNaN(f)) {
            return Float.NaN;
        }
        if (Float.isInfinite(f)) {
            return Float.POSITIVE_INFINITY;
        }
        if (f == Float.MAX_VALUE || f == -3.4028235E38f) {
            return (float) pow(2.0d, 104.0d);
        }
        float f2 = abs(f);
        int hx2 = Float.floatToRawIntBits(f2);
        int hy = Float.floatToRawIntBits(Float.MAX_VALUE);
        if ((Integer.MAX_VALUE & hx2) == 0) {
            return Float.intBitsToFloat((Integer.MIN_VALUE & hy) | 1);
        }
        if ((hx2 > hy) ^ (hx2 > 0)) {
            hx = hx2 + 1;
        } else {
            hx = hx2 - 1;
        }
        return Float.intBitsToFloat(hx) - f2;
    }

    public static double copySign(double magnitude, double sign) {
        long magnitudeBits = Double.doubleToRawLongBits(magnitude);
        long signBits = Double.doubleToRawLongBits(sign);
        return Double.longBitsToDouble((Long.MAX_VALUE & magnitudeBits) | (Long.MIN_VALUE & signBits));
    }

    public static float copySign(float magnitude, float sign) {
        int magnitudeBits = Float.floatToRawIntBits(magnitude);
        int signBits = Float.floatToRawIntBits(sign);
        return Float.intBitsToFloat((Integer.MAX_VALUE & magnitudeBits) | (Integer.MIN_VALUE & signBits));
    }

    public static int getExponent(float f) {
        int bits = Float.floatToRawIntBits(f);
        return ((2139095040 & bits) >> 23) - 127;
    }

    public static int getExponent(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return ((int) ((9218868437227405312L & bits) >> 52)) - 1023;
    }

    public static double nextAfter(double start, double direction) {
        return (start == 0.0d && direction == 0.0d) ? direction : nextafter(start, direction);
    }

    public static float nextAfter(float start, double direction) {
        if (Float.isNaN(start) || Double.isNaN(direction)) {
            return Float.NaN;
        }
        if (start == 0.0f && direction == 0.0d) {
            return (float) direction;
        }
        if ((start == Float.MIN_VALUE && direction < start) || (start == -1.4E-45f && direction > start)) {
            return start <= 0.0f ? -0.0f : 0.0f;
        }
        if (Float.isInfinite(start) && direction != start) {
            return start > 0.0f ? Float.MAX_VALUE : -3.4028235E38f;
        }
        if ((start == Float.MAX_VALUE && direction > start) || (start == -3.4028235E38f && direction < start)) {
            return start > 0.0f ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }
        if (direction > start) {
            if (start > 0.0f) {
                return Float.intBitsToFloat(Float.floatToIntBits(start) + 1);
            }
            if (start < 0.0f) {
                return Float.intBitsToFloat(Float.floatToIntBits(start) - 1);
            }
            return Float.MIN_VALUE;
        }
        if (direction >= start) {
            return (float) direction;
        }
        if (start > 0.0f) {
            return Float.intBitsToFloat(Float.floatToIntBits(start) - 1);
        }
        if (start < 0.0f) {
            return Float.intBitsToFloat(Float.floatToIntBits(start) + 1);
        }
        return -1.4E-45f;
    }

    public static double nextUp(double d) {
        if (Double.isNaN(d)) {
            return Double.NaN;
        }
        if (d == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }
        if (d == 0.0d) {
            return Double.MIN_VALUE;
        }
        if (d > 0.0d) {
            return Double.longBitsToDouble(Double.doubleToLongBits(d) + 1);
        }
        return Double.longBitsToDouble(Double.doubleToLongBits(d) - 1);
    }

    public static float nextUp(float f) {
        if (Float.isNaN(f)) {
            return Float.NaN;
        }
        if (f == Float.POSITIVE_INFINITY) {
            return Float.POSITIVE_INFINITY;
        }
        if (f == 0.0f) {
            return Float.MIN_VALUE;
        }
        if (f > 0.0f) {
            return Float.intBitsToFloat(Float.floatToIntBits(f) + 1);
        }
        return Float.intBitsToFloat(Float.floatToIntBits(f) - 1);
    }

    public static double scalb(double d, int scaleFactor) {
        long result;
        if (Double.isNaN(d) || Double.isInfinite(d) || d == 0.0d) {
            return d;
        }
        long bits = Double.doubleToLongBits(d);
        long sign = bits & Long.MIN_VALUE;
        long factor = (((9218868437227405312L & bits) >> 52) - 1023) + ((long) scaleFactor);
        int subNormalFactor = Long.numberOfLeadingZeros(Long.MAX_VALUE & bits) - 12;
        if (subNormalFactor < 0) {
            subNormalFactor = 0;
        } else {
            factor -= (long) subNormalFactor;
        }
        if (factor > 1023) {
            return d > 0.0d ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        if (factor <= -1023) {
            long digits = 1023 + factor + ((long) subNormalFactor);
            if (abs(d) < Double.MIN_NORMAL) {
                result = shiftLongBits(4503599627370495L & bits, digits);
            } else {
                result = shiftLongBits((4503599627370495L & bits) | 4503599627370496L, digits - 1);
            }
        } else if (abs(d) >= Double.MIN_NORMAL) {
            result = ((1023 + factor) << 52) | (4503599627370495L & bits);
        } else {
            result = ((1023 + factor) << 52) | ((bits << (subNormalFactor + 1)) & 4503599627370495L);
        }
        return Double.longBitsToDouble(result | sign);
    }

    public static float scalb(float d, int scaleFactor) {
        int result;
        if (Float.isNaN(d) || Float.isInfinite(d) || d == 0.0f) {
            return d;
        }
        int bits = Float.floatToIntBits(d);
        int sign = bits & Integer.MIN_VALUE;
        int factor = (((2139095040 & bits) >> 23) - 127) + scaleFactor;
        int subNormalFactor = Integer.numberOfLeadingZeros(Integer.MAX_VALUE & bits) - 9;
        if (subNormalFactor < 0) {
            subNormalFactor = 0;
        } else {
            factor -= subNormalFactor;
        }
        if (factor > 127) {
            return d > 0.0f ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }
        if (factor <= -127) {
            int digits = factor + 127 + subNormalFactor;
            if (abs(d) < Float.MIN_NORMAL) {
                result = shiftIntBits(bits & 8388607, digits);
            } else {
                result = shiftIntBits((bits & 8388607) | 8388608, digits - 1);
            }
        } else if (abs(d) >= Float.MIN_NORMAL) {
            result = ((factor + 127) << 23) | (bits & 8388607);
        } else {
            result = ((factor + 127) << 23) | ((bits << (subNormalFactor + 1)) & 8388607);
        }
        return Float.intBitsToFloat(result | sign);
    }

    private static int shiftIntBits(int bits, int digits) {
        if (digits > 0) {
            return bits << digits;
        }
        int absDigits = -digits;
        if (Integer.numberOfLeadingZeros(Integer.MAX_VALUE & bits) > 32 - absDigits) {
            return 0;
        }
        int ret = bits >> absDigits;
        boolean halfBit = ((bits >> (absDigits + (-1))) & 1) == 1;
        if (halfBit) {
            if (Integer.numberOfTrailingZeros(bits) < absDigits - 1) {
                ret++;
            }
            if (Integer.numberOfTrailingZeros(bits) == absDigits - 1 && (ret & 1) == 1) {
                return ret + 1;
            }
            return ret;
        }
        return ret;
    }

    private static long shiftLongBits(long bits, long digits) {
        if (digits > 0) {
            return bits << ((int) digits);
        }
        long absDigits = -digits;
        if (Long.numberOfLeadingZeros(Long.MAX_VALUE & bits) > 64 - absDigits) {
            return 0L;
        }
        long ret = bits >> ((int) absDigits);
        boolean halfBit = ((bits >> ((int) (absDigits - 1))) & 1) == 1;
        if (!halfBit) {
            return ret;
        }
        if (Long.numberOfTrailingZeros(bits) < absDigits - 1) {
            ret++;
        }
        if (Long.numberOfTrailingZeros(bits) == absDigits - 1 && (ret & 1) == 1) {
            return ret + 1;
        }
        return ret;
    }
}
