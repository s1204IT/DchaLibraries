package java.lang;

import dalvik.annotation.optimization.CriticalNative;
import java.util.Random;
import sun.misc.DoubleConsts;
import sun.misc.FloatConsts;

public final class Math {
    static final boolean $assertionsDisabled = false;
    public static final double E = 2.718281828459045d;
    public static final double PI = 3.141592653589793d;
    private static long negativeZeroFloatBits = Float.floatToRawIntBits(-0.0f);
    private static long negativeZeroDoubleBits = Double.doubleToRawLongBits(-0.0d);
    static double twoToTheDoubleScaleUp = powerOfTwoD(512);
    static double twoToTheDoubleScaleDown = powerOfTwoD(-512);

    @CriticalNative
    public static native double IEEEremainder(double d, double d2);

    @CriticalNative
    public static native double acos(double d);

    @CriticalNative
    public static native double asin(double d);

    @CriticalNative
    public static native double atan(double d);

    @CriticalNative
    public static native double atan2(double d, double d2);

    @CriticalNative
    public static native double cbrt(double d);

    @CriticalNative
    public static native double ceil(double d);

    @CriticalNative
    public static native double cos(double d);

    @CriticalNative
    public static native double cosh(double d);

    @CriticalNative
    public static native double exp(double d);

    @CriticalNative
    public static native double expm1(double d);

    @CriticalNative
    public static native double floor(double d);

    @CriticalNative
    public static native double hypot(double d, double d2);

    @CriticalNative
    public static native double log(double d);

    @CriticalNative
    public static native double log10(double d);

    @CriticalNative
    public static native double log1p(double d);

    @CriticalNative
    public static native double pow(double d, double d2);

    @CriticalNative
    public static native double rint(double d);

    @CriticalNative
    public static native double sin(double d);

    @CriticalNative
    public static native double sinh(double d);

    @CriticalNative
    public static native double sqrt(double d);

    @CriticalNative
    public static native double tan(double d);

    @CriticalNative
    public static native double tanh(double d);

    private Math() {
    }

    public static double toRadians(double d) {
        return (d / 180.0d) * 3.141592653589793d;
    }

    public static double toDegrees(double d) {
        return (d * 180.0d) / 3.141592653589793d;
    }

    public static int round(float f) {
        int iFloatToRawIntBits = Float.floatToRawIntBits(f);
        int i = 149 - ((2139095040 & iFloatToRawIntBits) >> 23);
        if ((i & (-32)) == 0) {
            int i2 = (8388607 & iFloatToRawIntBits) | 8388608;
            if (iFloatToRawIntBits < 0) {
                i2 = -i2;
            }
            return ((i2 >> i) + 1) >> 1;
        }
        return (int) f;
    }

    public static long round(double d) {
        long jDoubleToRawLongBits = Double.doubleToRawLongBits(d);
        long j = 1074 - ((DoubleConsts.EXP_BIT_MASK & jDoubleToRawLongBits) >> 52);
        if (((-64) & j) == 0) {
            long j2 = (DoubleConsts.SIGNIF_BIT_MASK & jDoubleToRawLongBits) | 4503599627370496L;
            if (jDoubleToRawLongBits < 0) {
                j2 = -j2;
            }
            return ((j2 >> ((int) j)) + 1) >> 1;
        }
        return (long) d;
    }

    private static final class RandomNumberGeneratorHolder {
        static final Random randomNumberGenerator = new Random();

        private RandomNumberGeneratorHolder() {
        }
    }

    public static double random() {
        return RandomNumberGeneratorHolder.randomNumberGenerator.nextDouble();
    }

    public static void setRandomSeedInternal(long j) {
        RandomNumberGeneratorHolder.randomNumberGenerator.setSeed(j);
    }

    public static int randomIntInternal() {
        return RandomNumberGeneratorHolder.randomNumberGenerator.nextInt();
    }

    public static long randomLongInternal() {
        return RandomNumberGeneratorHolder.randomNumberGenerator.nextLong();
    }

    public static int addExact(int i, int i2) {
        int i3 = i + i2;
        if (((i ^ i3) & (i2 ^ i3)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return i3;
    }

    public static long addExact(long j, long j2) {
        long j3 = j + j2;
        if (((j ^ j3) & (j2 ^ j3)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return j3;
    }

    public static int subtractExact(int i, int i2) {
        int i3 = i - i2;
        if (((i ^ i3) & (i2 ^ i)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return i3;
    }

    public static long subtractExact(long j, long j2) {
        long j3 = j - j2;
        if (((j ^ j3) & (j2 ^ j)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return j3;
    }

    public static int multiplyExact(int i, int i2) {
        long j = ((long) i) * ((long) i2);
        int i3 = (int) j;
        if (i3 != j) {
            throw new ArithmeticException("integer overflow");
        }
        return i3;
    }

    public static long multiplyExact(long j, long j2) {
        long j3 = j * j2;
        if (((abs(j) | abs(j2)) >>> 31) != 0 && ((j2 != 0 && j3 / j2 != j) || (j == Long.MIN_VALUE && j2 == -1))) {
            throw new ArithmeticException("long overflow");
        }
        return j3;
    }

    public static int incrementExact(int i) {
        if (i == Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return i + 1;
    }

    public static long incrementExact(long j) {
        if (j == Long.MAX_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return j + 1;
    }

    public static int decrementExact(int i) {
        if (i == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return i - 1;
    }

    public static long decrementExact(long j) {
        if (j == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return j - 1;
    }

    public static int negateExact(int i) {
        if (i == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return -i;
    }

    public static long negateExact(long j) {
        if (j == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -j;
    }

    public static int toIntExact(long j) {
        int i = (int) j;
        if (i != j) {
            throw new ArithmeticException("integer overflow");
        }
        return i;
    }

    public static int floorDiv(int i, int i2) {
        int i3 = i / i2;
        if ((i ^ i2) < 0 && i2 * i3 != i) {
            return i3 - 1;
        }
        return i3;
    }

    public static long floorDiv(long j, long j2) {
        long j3 = j / j2;
        if ((j ^ j2) < 0 && j2 * j3 != j) {
            return j3 - 1;
        }
        return j3;
    }

    public static int floorMod(int i, int i2) {
        return i - (floorDiv(i, i2) * i2);
    }

    public static long floorMod(long j, long j2) {
        return j - (floorDiv(j, j2) * j2);
    }

    public static int abs(int i) {
        return i < 0 ? -i : i;
    }

    public static long abs(long j) {
        return j < 0 ? -j : j;
    }

    public static float abs(float f) {
        return Float.intBitsToFloat(Float.floatToRawIntBits(f) & Integer.MAX_VALUE);
    }

    public static double abs(double d) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(d) & Long.MAX_VALUE);
    }

    public static int max(int i, int i2) {
        return i >= i2 ? i : i2;
    }

    public static long max(long j, long j2) {
        return j >= j2 ? j : j2;
    }

    public static float max(float f, float f2) {
        if (f != f) {
            return f;
        }
        if (f == 0.0f && f2 == 0.0f && Float.floatToRawIntBits(f) == negativeZeroFloatBits) {
            return f2;
        }
        return f >= f2 ? f : f2;
    }

    public static double max(double d, double d2) {
        if (d != d) {
            return d;
        }
        if (d == 0.0d && d2 == 0.0d && Double.doubleToRawLongBits(d) == negativeZeroDoubleBits) {
            return d2;
        }
        return d >= d2 ? d : d2;
    }

    public static int min(int i, int i2) {
        return i <= i2 ? i : i2;
    }

    public static long min(long j, long j2) {
        return j <= j2 ? j : j2;
    }

    public static float min(float f, float f2) {
        if (f != f) {
            return f;
        }
        if (f == 0.0f && f2 == 0.0f && Float.floatToRawIntBits(f2) == negativeZeroFloatBits) {
            return f2;
        }
        return f <= f2 ? f : f2;
    }

    public static double min(double d, double d2) {
        if (d != d) {
            return d;
        }
        if (d == 0.0d && d2 == 0.0d && Double.doubleToRawLongBits(d2) == negativeZeroDoubleBits) {
            return d2;
        }
        return d <= d2 ? d : d2;
    }

    public static double ulp(double d) {
        int exponent = getExponent(d);
        if (exponent == -1023) {
            return Double.MIN_VALUE;
        }
        if (exponent == 1024) {
            return abs(d);
        }
        int i = exponent - 52;
        if (i >= -1022) {
            return powerOfTwoD(i);
        }
        return Double.longBitsToDouble(1 << (i + 1074));
    }

    public static float ulp(float f) {
        int exponent = getExponent(f);
        if (exponent == -127) {
            return Float.MIN_VALUE;
        }
        if (exponent == 128) {
            return abs(f);
        }
        int i = exponent - 23;
        if (i >= -126) {
            return powerOfTwoF(i);
        }
        return Float.intBitsToFloat(1 << (i + 149));
    }

    public static double signum(double d) {
        return (d == 0.0d || Double.isNaN(d)) ? d : copySign(1.0d, d);
    }

    public static float signum(float f) {
        return (f == 0.0f || Float.isNaN(f)) ? f : copySign(1.0f, f);
    }

    public static double copySign(double d, double d2) {
        return Double.longBitsToDouble((Double.doubleToRawLongBits(d) & Long.MAX_VALUE) | (Double.doubleToRawLongBits(d2) & Long.MIN_VALUE));
    }

    public static float copySign(float f, float f2) {
        return Float.intBitsToFloat((Float.floatToRawIntBits(f) & Integer.MAX_VALUE) | (Float.floatToRawIntBits(f2) & Integer.MIN_VALUE));
    }

    public static int getExponent(float f) {
        return ((Float.floatToRawIntBits(f) & FloatConsts.EXP_BIT_MASK) >> 23) - 127;
    }

    public static int getExponent(double d) {
        return (int) (((Double.doubleToRawLongBits(d) & DoubleConsts.EXP_BIT_MASK) >> 52) - 1023);
    }

    public static double nextAfter(double d, double d2) {
        long j;
        if (Double.isNaN(d) || Double.isNaN(d2)) {
            return d + d2;
        }
        if (d == d2) {
            return d2;
        }
        long jDoubleToRawLongBits = Double.doubleToRawLongBits(0.0d + d);
        if (d2 > d) {
            j = jDoubleToRawLongBits + (jDoubleToRawLongBits < 0 ? -1L : 1L);
        } else if (jDoubleToRawLongBits > 0) {
            j = jDoubleToRawLongBits - 1;
        } else if (jDoubleToRawLongBits < 0) {
            j = jDoubleToRawLongBits + 1;
        } else {
            j = -9223372036854775807L;
        }
        return Double.longBitsToDouble(j);
    }

    public static float nextAfter(float f, double d) {
        int i;
        if (Float.isNaN(f) || Double.isNaN(d)) {
            return f + ((float) d);
        }
        double d2 = f;
        if (d2 == d) {
            return (float) d;
        }
        int iFloatToRawIntBits = Float.floatToRawIntBits(f + 0.0f);
        if (d > d2) {
            i = iFloatToRawIntBits + (iFloatToRawIntBits >= 0 ? 1 : -1);
        } else if (iFloatToRawIntBits > 0) {
            i = iFloatToRawIntBits - 1;
        } else if (iFloatToRawIntBits < 0) {
            i = iFloatToRawIntBits + 1;
        } else {
            i = -2147483647;
        }
        return Float.intBitsToFloat(i);
    }

    public static double nextUp(double d) {
        if (Double.isNaN(d) || d == Double.POSITIVE_INFINITY) {
            return d;
        }
        double d2 = d + 0.0d;
        return Double.longBitsToDouble(Double.doubleToRawLongBits(d2) + (d2 >= 0.0d ? 1L : -1L));
    }

    public static float nextUp(float f) {
        if (Float.isNaN(f) || f == Float.POSITIVE_INFINITY) {
            return f;
        }
        float f2 = f + 0.0f;
        return Float.intBitsToFloat(Float.floatToRawIntBits(f2) + (f2 >= 0.0f ? 1 : -1));
    }

    public static double nextDown(double d) {
        if (Double.isNaN(d) || d == Double.NEGATIVE_INFINITY) {
            return d;
        }
        if (d == 0.0d) {
            return -4.9E-324d;
        }
        return Double.longBitsToDouble(Double.doubleToRawLongBits(d) + (d > 0.0d ? -1L : 1L));
    }

    public static float nextDown(float f) {
        if (Float.isNaN(f) || f == Float.NEGATIVE_INFINITY) {
            return f;
        }
        if (f == 0.0f) {
            return -1.4E-45f;
        }
        return Float.intBitsToFloat(Float.floatToRawIntBits(f) + (f > 0.0f ? -1 : 1));
    }

    public static double scalb(double d, int i) {
        int iMin;
        int i2;
        double d2;
        if (i < 0) {
            iMin = max(i, -2099);
            i2 = -512;
            d2 = twoToTheDoubleScaleDown;
        } else {
            iMin = min(i, 2099);
            i2 = 512;
            d2 = twoToTheDoubleScaleUp;
        }
        int i3 = (iMin >> 8) >>> 23;
        int i4 = ((iMin + i3) & 511) - i3;
        double dPowerOfTwoD = d * powerOfTwoD(i4);
        for (int i5 = iMin - i4; i5 != 0; i5 -= i2) {
            dPowerOfTwoD *= d2;
        }
        return dPowerOfTwoD;
    }

    public static float scalb(float f, int i) {
        return (float) (((double) f) * powerOfTwoD(max(min(i, 278), -278)));
    }

    static double powerOfTwoD(int i) {
        return Double.longBitsToDouble(((((long) i) + 1023) << 52) & DoubleConsts.EXP_BIT_MASK);
    }

    static float powerOfTwoF(int i) {
        return Float.intBitsToFloat(((i + 127) << 23) & FloatConsts.EXP_BIT_MASK);
    }
}
