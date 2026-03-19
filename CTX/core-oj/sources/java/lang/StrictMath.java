package java.lang;

import java.util.Random;
import sun.misc.DoubleConsts;

public final class StrictMath {
    static final boolean $assertionsDisabled = false;
    public static final double E = 2.718281828459045d;
    public static final double PI = 3.141592653589793d;

    public static native double IEEEremainder(double d, double d2);

    public static native double acos(double d);

    public static native double asin(double d);

    public static native double atan(double d);

    public static native double atan2(double d, double d2);

    public static native double cbrt(double d);

    public static native double cos(double d);

    public static native double cosh(double d);

    public static native double exp(double d);

    public static native double expm1(double d);

    public static native double hypot(double d, double d2);

    public static native double log(double d);

    public static native double log10(double d);

    public static native double log1p(double d);

    public static native double pow(double d, double d2);

    public static native double sin(double d);

    public static native double sinh(double d);

    public static native double sqrt(double d);

    public static native double tan(double d);

    public static native double tanh(double d);

    private StrictMath() {
    }

    public static double toRadians(double d) {
        return (d / 180.0d) * 3.141592653589793d;
    }

    public static double toDegrees(double d) {
        return (d * 180.0d) / 3.141592653589793d;
    }

    public static double ceil(double d) {
        return floorOrCeil(d, -0.0d, 1.0d, 1.0d);
    }

    public static double floor(double d) {
        return floorOrCeil(d, -1.0d, 0.0d, -1.0d);
    }

    private static double floorOrCeil(double d, double d2, double d3, double d4) {
        int exponent = Math.getExponent(d);
        if (exponent < 0) {
            return d == 0.0d ? d : d < 0.0d ? d2 : d3;
        }
        if (exponent >= 52) {
            return d;
        }
        long jDoubleToRawLongBits = Double.doubleToRawLongBits(d);
        long j = DoubleConsts.SIGNIF_BIT_MASK >> exponent;
        if ((j & jDoubleToRawLongBits) == 0) {
            return d;
        }
        double dLongBitsToDouble = Double.longBitsToDouble(jDoubleToRawLongBits & (~j));
        if (d * d4 > 0.0d) {
            return dLongBitsToDouble + d4;
        }
        return dLongBitsToDouble;
    }

    public static double rint(double d) {
        double dCopySign = Math.copySign(1.0d, d);
        double dAbs = Math.abs(d);
        if (dAbs < 4.503599627370496E15d) {
            dAbs = (dAbs + 4.503599627370496E15d) - 4.503599627370496E15d;
        }
        return dCopySign * dAbs;
    }

    public static int round(float f) {
        return Math.round(f);
    }

    public static long round(double d) {
        return Math.round(d);
    }

    private static final class RandomNumberGeneratorHolder {
        static final Random randomNumberGenerator = new Random();

        private RandomNumberGeneratorHolder() {
        }
    }

    public static double random() {
        return RandomNumberGeneratorHolder.randomNumberGenerator.nextDouble();
    }

    public static int addExact(int i, int i2) {
        return Math.addExact(i, i2);
    }

    public static long addExact(long j, long j2) {
        return Math.addExact(j, j2);
    }

    public static int subtractExact(int i, int i2) {
        return Math.subtractExact(i, i2);
    }

    public static long subtractExact(long j, long j2) {
        return Math.subtractExact(j, j2);
    }

    public static int multiplyExact(int i, int i2) {
        return Math.multiplyExact(i, i2);
    }

    public static long multiplyExact(long j, long j2) {
        return Math.multiplyExact(j, j2);
    }

    public static int toIntExact(long j) {
        return Math.toIntExact(j);
    }

    public static int floorDiv(int i, int i2) {
        return Math.floorDiv(i, i2);
    }

    public static long floorDiv(long j, long j2) {
        return Math.floorDiv(j, j2);
    }

    public static int floorMod(int i, int i2) {
        return Math.floorMod(i, i2);
    }

    public static long floorMod(long j, long j2) {
        return Math.floorMod(j, j2);
    }

    public static int abs(int i) {
        return Math.abs(i);
    }

    public static long abs(long j) {
        return Math.abs(j);
    }

    public static float abs(float f) {
        return Math.abs(f);
    }

    public static double abs(double d) {
        return Math.abs(d);
    }

    public static int max(int i, int i2) {
        return Math.max(i, i2);
    }

    public static long max(long j, long j2) {
        return Math.max(j, j2);
    }

    public static float max(float f, float f2) {
        return Math.max(f, f2);
    }

    public static double max(double d, double d2) {
        return Math.max(d, d2);
    }

    public static int min(int i, int i2) {
        return Math.min(i, i2);
    }

    public static long min(long j, long j2) {
        return Math.min(j, j2);
    }

    public static float min(float f, float f2) {
        return Math.min(f, f2);
    }

    public static double min(double d, double d2) {
        return Math.min(d, d2);
    }

    public static double ulp(double d) {
        return Math.ulp(d);
    }

    public static float ulp(float f) {
        return Math.ulp(f);
    }

    public static double signum(double d) {
        return Math.signum(d);
    }

    public static float signum(float f) {
        return Math.signum(f);
    }

    public static double copySign(double d, double d2) {
        if (Double.isNaN(d2)) {
            d2 = 1.0d;
        }
        return Math.copySign(d, d2);
    }

    public static float copySign(float f, float f2) {
        if (Float.isNaN(f2)) {
            f2 = 1.0f;
        }
        return Math.copySign(f, f2);
    }

    public static int getExponent(float f) {
        return Math.getExponent(f);
    }

    public static int getExponent(double d) {
        return Math.getExponent(d);
    }

    public static double nextAfter(double d, double d2) {
        return Math.nextAfter(d, d2);
    }

    public static float nextAfter(float f, double d) {
        return Math.nextAfter(f, d);
    }

    public static double nextUp(double d) {
        return Math.nextUp(d);
    }

    public static float nextUp(float f) {
        return Math.nextUp(f);
    }

    public static double nextDown(double d) {
        return Math.nextDown(d);
    }

    public static float nextDown(float f) {
        return Math.nextDown(f);
    }

    public static double scalb(double d, int i) {
        return Math.scalb(d, i);
    }

    public static float scalb(float f, int i) {
        return Math.scalb(f, i);
    }
}
