package android.util;

import android.hardware.contexthub.V1_0.HostEndPoint;
import sun.misc.FloatingDecimal;

public final class Half extends Number implements Comparable<Half> {
    public static final short EPSILON = 5120;
    private static final int FP16_COMBINED = 32767;
    private static final int FP16_EXPONENT_BIAS = 15;
    private static final int FP16_EXPONENT_MASK = 31;
    private static final int FP16_EXPONENT_MAX = 31744;
    private static final int FP16_EXPONENT_SHIFT = 10;
    private static final int FP16_SIGNIFICAND_MASK = 1023;
    private static final int FP16_SIGN_MASK = 32768;
    private static final int FP16_SIGN_SHIFT = 15;
    private static final int FP32_EXPONENT_BIAS = 127;
    private static final int FP32_EXPONENT_MASK = 255;
    private static final int FP32_EXPONENT_SHIFT = 23;
    private static final int FP32_SIGNIFICAND_MASK = 8388607;
    private static final int FP32_SIGN_SHIFT = 31;
    public static final short LOWEST_VALUE = -1025;
    public static final int MAX_EXPONENT = 15;
    public static final short MAX_VALUE = 31743;
    public static final int MIN_EXPONENT = -14;
    public static final short MIN_NORMAL = 1024;
    public static final short MIN_VALUE = 1;
    public static final short NEGATIVE_INFINITY = -1024;
    public static final short NEGATIVE_ZERO = Short.MIN_VALUE;
    public static final short NaN = 32256;
    public static final short POSITIVE_INFINITY = 31744;
    public static final short POSITIVE_ZERO = 0;
    public static final int SIZE = 16;
    private final short mValue;
    private static final int FP32_DENORMAL_MAGIC = 1056964608;
    private static final float FP32_DENORMAL_FLOAT = Float.intBitsToFloat(FP32_DENORMAL_MAGIC);

    public Half(short s) {
        this.mValue = s;
    }

    public Half(float f) {
        this.mValue = toHalf(f);
    }

    public Half(double d) {
        this.mValue = toHalf((float) d);
    }

    public Half(String str) throws NumberFormatException {
        this.mValue = toHalf(Float.parseFloat(str));
    }

    public short halfValue() {
        return this.mValue;
    }

    @Override
    public byte byteValue() {
        return (byte) toFloat(this.mValue);
    }

    @Override
    public short shortValue() {
        return (short) toFloat(this.mValue);
    }

    @Override
    public int intValue() {
        return (int) toFloat(this.mValue);
    }

    @Override
    public long longValue() {
        return (long) toFloat(this.mValue);
    }

    @Override
    public float floatValue() {
        return toFloat(this.mValue);
    }

    @Override
    public double doubleValue() {
        return toFloat(this.mValue);
    }

    public boolean isNaN() {
        return isNaN(this.mValue);
    }

    public boolean equals(Object obj) {
        return (obj instanceof Half) && halfToIntBits(((Half) obj).mValue) == halfToIntBits(this.mValue);
    }

    public int hashCode() {
        return hashCode(this.mValue);
    }

    public String toString() {
        return toString(this.mValue);
    }

    @Override
    public int compareTo(Half half) {
        return compare(this.mValue, half.mValue);
    }

    public static int hashCode(short s) {
        return halfToIntBits(s);
    }

    public static int compare(short s, short s2) {
        if (less(s, s2)) {
            return -1;
        }
        if (greater(s, s2)) {
            return 1;
        }
        if ((s & Short.MAX_VALUE) > FP16_EXPONENT_MAX) {
            s = 32256;
        }
        if ((s2 & Short.MAX_VALUE) > FP16_EXPONENT_MAX) {
            s2 = 32256;
        }
        if (s == s2) {
            return 0;
        }
        return s < s2 ? -1 : 1;
    }

    public static short halfToShortBits(short s) {
        return (s & Short.MAX_VALUE) > FP16_EXPONENT_MAX ? NaN : s;
    }

    public static int halfToIntBits(short s) {
        if ((s & Short.MAX_VALUE) > FP16_EXPONENT_MAX) {
            return 32256;
        }
        return s & HostEndPoint.BROADCAST;
    }

    public static int halfToRawIntBits(short s) {
        return s & HostEndPoint.BROADCAST;
    }

    public static short intBitsToHalf(int i) {
        return (short) (i & 65535);
    }

    public static short copySign(short s, short s2) {
        return (short) ((s & Short.MAX_VALUE) | (s2 & NEGATIVE_ZERO));
    }

    public static short abs(short s) {
        return (short) (s & Short.MAX_VALUE);
    }

    public static short round(short s) {
        int i = s & HostEndPoint.BROADCAST;
        int i2 = i & FP16_COMBINED;
        if (i2 < 15360) {
            i = (i & 32768) | ((i2 < 14336 ? 0 : 65535) & 15360);
        } else if (i2 < 25600) {
            int i3 = 25 - (i2 >> 10);
            i = (i + (1 << (i3 - 1))) & (~((1 << i3) - 1));
        }
        return (short) i;
    }

    public static short ceil(short s) {
        int i = s & HostEndPoint.BROADCAST;
        int i2 = i & FP16_COMBINED;
        if (i2 < 15360) {
            i = ((-((~(i >> 15)) & (i2 == 0 ? 0 : 1))) & 15360) | (32768 & i);
        } else if (i2 < 25600) {
            int i3 = (1 << (25 - (i2 >> 10))) - 1;
            i = (i + (((i >> 15) - 1) & i3)) & (~i3);
        }
        return (short) i;
    }

    public static short floor(short s) {
        int i = s & HostEndPoint.BROADCAST;
        int i2 = i & FP16_COMBINED;
        if (i2 < 15360) {
            i = (15360 & (i <= 32768 ? 0 : 65535)) | (i & 32768);
        } else if (i2 < 25600) {
            int i3 = (1 << (25 - (i2 >> 10))) - 1;
            i = (i + ((-(i >> 15)) & i3)) & (~i3);
        }
        return (short) i;
    }

    public static short trunc(short s) {
        int i = s & HostEndPoint.BROADCAST;
        int i2 = i & FP16_COMBINED;
        if (i2 < 15360) {
            i &= 32768;
        } else if (i2 < 25600) {
            i &= ~((1 << (25 - (i2 >> 10))) - 1);
        }
        return (short) i;
    }

    public static short min(short s, short s2) {
        int i;
        int i2 = s & Short.MAX_VALUE;
        if (i2 > FP16_EXPONENT_MAX || (i = s2 & Short.MAX_VALUE) > FP16_EXPONENT_MAX) {
            return NaN;
        }
        if (i2 == 0 && i == 0) {
            return (s & NEGATIVE_ZERO) != 0 ? s : s2;
        }
        return ((s & NEGATIVE_ZERO) != 0 ? 32768 - (s & HostEndPoint.BROADCAST) : s & HostEndPoint.BROADCAST) < ((s2 & NEGATIVE_ZERO) != 0 ? 32768 - (65535 & s2) : s2 & HostEndPoint.BROADCAST) ? s : s2;
    }

    public static short max(short s, short s2) {
        int i;
        int i2 = s & Short.MAX_VALUE;
        if (i2 > FP16_EXPONENT_MAX || (i = s2 & Short.MAX_VALUE) > FP16_EXPONENT_MAX) {
            return NaN;
        }
        if (i2 == 0 && i == 0) {
            return (s & NEGATIVE_ZERO) != 0 ? s2 : s;
        }
        return ((s & NEGATIVE_ZERO) != 0 ? 32768 - (s & HostEndPoint.BROADCAST) : s & HostEndPoint.BROADCAST) > ((s2 & NEGATIVE_ZERO) != 0 ? 32768 - (65535 & s2) : s2 & HostEndPoint.BROADCAST) ? s : s2;
    }

    public static boolean less(short s, short s2) {
        if ((s & Short.MAX_VALUE) <= FP16_EXPONENT_MAX && (s2 & Short.MAX_VALUE) <= FP16_EXPONENT_MAX) {
            return ((s & NEGATIVE_ZERO) != 0 ? 32768 - (s & HostEndPoint.BROADCAST) : s & HostEndPoint.BROADCAST) < ((s2 & NEGATIVE_ZERO) != 0 ? 32768 - (s2 & HostEndPoint.BROADCAST) : s2 & HostEndPoint.BROADCAST);
        }
        return false;
    }

    public static boolean lessEquals(short s, short s2) {
        if ((s & Short.MAX_VALUE) <= FP16_EXPONENT_MAX && (s2 & Short.MAX_VALUE) <= FP16_EXPONENT_MAX) {
            return ((s & NEGATIVE_ZERO) != 0 ? 32768 - (s & HostEndPoint.BROADCAST) : s & HostEndPoint.BROADCAST) <= ((s2 & NEGATIVE_ZERO) != 0 ? 32768 - (s2 & HostEndPoint.BROADCAST) : s2 & HostEndPoint.BROADCAST);
        }
        return false;
    }

    public static boolean greater(short s, short s2) {
        if ((s & Short.MAX_VALUE) <= FP16_EXPONENT_MAX && (s2 & Short.MAX_VALUE) <= FP16_EXPONENT_MAX) {
            return ((s & NEGATIVE_ZERO) != 0 ? 32768 - (s & HostEndPoint.BROADCAST) : s & HostEndPoint.BROADCAST) > ((s2 & NEGATIVE_ZERO) != 0 ? 32768 - (s2 & HostEndPoint.BROADCAST) : s2 & HostEndPoint.BROADCAST);
        }
        return false;
    }

    public static boolean greaterEquals(short s, short s2) {
        if ((s & Short.MAX_VALUE) <= FP16_EXPONENT_MAX && (s2 & Short.MAX_VALUE) <= FP16_EXPONENT_MAX) {
            return ((s & NEGATIVE_ZERO) != 0 ? 32768 - (s & HostEndPoint.BROADCAST) : s & HostEndPoint.BROADCAST) >= ((s2 & NEGATIVE_ZERO) != 0 ? 32768 - (s2 & HostEndPoint.BROADCAST) : s2 & HostEndPoint.BROADCAST);
        }
        return false;
    }

    public static boolean equals(short s, short s2) {
        if ((s & Short.MAX_VALUE) <= FP16_EXPONENT_MAX && (s2 & Short.MAX_VALUE) <= FP16_EXPONENT_MAX) {
            return s == s2 || ((s | s2) & FP16_COMBINED) == 0;
        }
        return false;
    }

    public static int getSign(short s) {
        return (s & NEGATIVE_ZERO) == 0 ? 1 : -1;
    }

    public static int getExponent(short s) {
        return ((s >>> 10) & 31) - 15;
    }

    public static int getSignificand(short s) {
        return s & 1023;
    }

    public static boolean isInfinite(short s) {
        return (s & Short.MAX_VALUE) == FP16_EXPONENT_MAX;
    }

    public static boolean isNaN(short s) {
        return (s & Short.MAX_VALUE) > FP16_EXPONENT_MAX;
    }

    public static boolean isNormalized(short s) {
        int i = s & FP16_EXPONENT_MAX;
        return (i == 0 || i == FP16_EXPONENT_MAX) ? false : true;
    }

    public static float toFloat(short s) {
        int i;
        int i2 = s & HostEndPoint.BROADCAST;
        int i3 = 32768 & i2;
        int i4 = (i2 >>> 10) & 31;
        int i5 = i2 & 1023;
        int i6 = 0;
        if (i4 == 0) {
            if (i5 != 0) {
                float fIntBitsToFloat = Float.intBitsToFloat(FP32_DENORMAL_MAGIC + i5) - FP32_DENORMAL_FLOAT;
                return i3 == 0 ? fIntBitsToFloat : -fIntBitsToFloat;
            }
            i = 0;
        } else {
            i6 = i5 << 13;
            if (i4 == 31) {
                i = 255;
            } else {
                i = (i4 - 15) + 127;
            }
        }
        return Float.intBitsToFloat((i << 23) | (i3 << 16) | i6);
    }

    public static short toHalf(float f) {
        int i;
        int iFloatToRawIntBits = Float.floatToRawIntBits(f);
        int i2 = iFloatToRawIntBits >>> 31;
        int i3 = (iFloatToRawIntBits >>> 23) & 255;
        int i4 = iFloatToRawIntBits & FP32_SIGNIFICAND_MASK;
        if (i3 == 255) {
            i = i4 != 0 ? 512 : 0;
            i = 31;
        } else {
            int i5 = (i3 - 127) + 15;
            if (i5 >= 31) {
                i = 49;
                i = 0;
            } else if (i5 > 0) {
                int i6 = i4 >> 13;
                if ((i4 & 4096) != 0) {
                    return (short) ((((i5 << 10) | i6) + 1) | (i2 << 15));
                }
                i = i6;
                i = i5;
            } else if (i5 < -10) {
                i = 0;
            } else {
                int i7 = (i4 | 8388608) >> (1 - i5);
                if ((i7 & 4096) != 0) {
                    i7 += 8192;
                }
                i = i7 >> 13;
            }
        }
        return (short) (i | (i2 << 15) | (i << 10));
    }

    public static Half valueOf(short s) {
        return new Half(s);
    }

    public static Half valueOf(float f) {
        return new Half(f);
    }

    public static Half valueOf(String str) {
        return new Half(str);
    }

    public static short parseHalf(String str) throws NumberFormatException {
        return toHalf(FloatingDecimal.parseFloat(str));
    }

    public static String toString(short s) {
        return Float.toString(toFloat(s));
    }

    public static String toHexString(short s) {
        StringBuilder sb = new StringBuilder();
        int i = s & HostEndPoint.BROADCAST;
        int i2 = i >>> 15;
        int i3 = (i >>> 10) & 31;
        int i4 = i & 1023;
        if (i3 == 31) {
            if (i4 == 0) {
                if (i2 != 0) {
                    sb.append('-');
                }
                sb.append("Infinity");
            } else {
                sb.append("NaN");
            }
        } else {
            if (i2 == 1) {
                sb.append('-');
            }
            if (i3 == 0) {
                if (i4 == 0) {
                    sb.append("0x0.0p0");
                } else {
                    sb.append("0x0.");
                    sb.append(Integer.toHexString(i4).replaceFirst("0{2,}$", ""));
                    sb.append("p-14");
                }
            } else {
                sb.append("0x1.");
                sb.append(Integer.toHexString(i4).replaceFirst("0{2,}$", ""));
                sb.append('p');
                sb.append(Integer.toString(i3 - 15));
            }
        }
        return sb.toString();
    }
}
