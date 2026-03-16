package java.lang;

import dalvik.bytecode.Opcodes;
import libcore.math.MathUtils;

final class RealToString {
    private static final ThreadLocal<RealToString> INSTANCE = new ThreadLocal<RealToString>() {
        @Override
        protected RealToString initialValue() {
            return new RealToString();
        }
    };
    private static final double invLogOfTenBaseTwo = Math.log(2.0d) / Math.log(10.0d);
    private int digitCount;
    private final int[] digits;
    private int firstK;

    private native void bigIntDigitGenerator(long j, int i, boolean z, int i2);

    private RealToString() {
        this.digits = new int[64];
    }

    public static RealToString getInstance() {
        return INSTANCE.get();
    }

    private static String resultOrSideEffect(AbstractStringBuilder sb, String s) {
        if (sb != null) {
            sb.append0(s);
            return null;
        }
        return s;
    }

    public String doubleToString(double d) {
        return convertDouble(null, d);
    }

    public void appendDouble(AbstractStringBuilder sb, double d) {
        convertDouble(sb, d);
    }

    private String convertDouble(AbstractStringBuilder sb, double inputNumber) {
        int pow;
        long inputNumberBits = Double.doubleToRawLongBits(inputNumber);
        boolean positive = (Long.MIN_VALUE & inputNumberBits) == 0;
        int e = (int) ((9218868437227405312L & inputNumberBits) >> 52);
        long f = inputNumberBits & 4503599627370495L;
        boolean mantissaIsZero = f == 0;
        String quickResult = null;
        if (e == 2047) {
            quickResult = mantissaIsZero ? positive ? "Infinity" : "-Infinity" : "NaN";
        } else if (e == 0) {
            if (mantissaIsZero) {
                quickResult = positive ? "0.0" : "-0.0";
            } else if (f == 1) {
                quickResult = positive ? "4.9E-324" : "-4.9E-324";
            }
        }
        if (quickResult != null) {
            return resultOrSideEffect(sb, quickResult);
        }
        int numBits = 52;
        if (e == 0) {
            pow = 1 - 1075;
            long ff = f;
            while ((4503599627370496L & ff) == 0) {
                ff <<= 1;
                numBits--;
            }
        } else {
            f |= 4503599627370496L;
            pow = e - 1075;
        }
        this.digitCount = 0;
        this.firstK = 0;
        if ((-59 < pow && pow < 6) || (pow == -59 && !mantissaIsZero)) {
            longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
        } else {
            bigIntDigitGenerator(f, pow, e == 0, numBits);
        }
        AbstractStringBuilder dst = sb != null ? sb : new StringBuilder(26);
        if (inputNumber >= 1.0E7d || inputNumber <= -1.0E7d || (inputNumber > -0.001d && inputNumber < 0.001d)) {
            freeFormatExponential(dst, positive);
        } else {
            freeFormat(dst, positive);
        }
        if (sb != null) {
            return null;
        }
        return dst.toString();
    }

    public String floatToString(float f) {
        return convertFloat(null, f);
    }

    public void appendFloat(AbstractStringBuilder sb, float f) {
        convertFloat(sb, f);
    }

    public String convertFloat(AbstractStringBuilder sb, float inputNumber) {
        int pow;
        int inputNumberBits = Float.floatToRawIntBits(inputNumber);
        boolean positive = (Integer.MIN_VALUE & inputNumberBits) == 0;
        int e = (2139095040 & inputNumberBits) >> 23;
        int f = inputNumberBits & 8388607;
        boolean mantissaIsZero = f == 0;
        String quickResult = null;
        if (e == 255) {
            quickResult = mantissaIsZero ? positive ? "Infinity" : "-Infinity" : "NaN";
        } else if (e == 0 && mantissaIsZero) {
            quickResult = positive ? "0.0" : "-0.0";
        }
        if (quickResult != null) {
            return resultOrSideEffect(sb, quickResult);
        }
        int numBits = 23;
        if (e == 0) {
            pow = 1 - Opcodes.OP_OR_INT;
            if (f < 8) {
                f <<= 2;
                pow -= 2;
            }
            int ff = f;
            while ((8388608 & ff) == 0) {
                ff <<= 1;
                numBits--;
            }
        } else {
            f |= 8388608;
            pow = e - Opcodes.OP_OR_INT;
        }
        this.digitCount = 0;
        this.firstK = 0;
        if ((-59 < pow && pow < 35) || (pow == -59 && !mantissaIsZero)) {
            longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
        } else {
            bigIntDigitGenerator(f, pow, e == 0, numBits);
        }
        AbstractStringBuilder dst = sb != null ? sb : new StringBuilder(26);
        if (inputNumber >= 1.0E7f || inputNumber <= -1.0E7f || (inputNumber > -0.001f && inputNumber < 0.001f)) {
            freeFormatExponential(dst, positive);
        } else {
            freeFormat(dst, positive);
        }
        if (sb != null) {
            return null;
        }
        return dst.toString();
    }

    private void freeFormatExponential(AbstractStringBuilder sb, boolean positive) {
        if (!positive) {
            sb.append0('-');
        }
        int digitIndex = 0 + 1;
        sb.append0((char) (this.digits[0] + 48));
        sb.append0('.');
        int k = this.firstK;
        while (true) {
            int digitIndex2 = digitIndex;
            k--;
            if (digitIndex2 >= this.digitCount) {
                break;
            }
            digitIndex = digitIndex2 + 1;
            sb.append0((char) (this.digits[digitIndex2] + 48));
        }
        if (k == k - 1) {
            sb.append0('0');
        }
        sb.append0('E');
        IntegralToString.appendInt(sb, k);
    }

    private void freeFormat(AbstractStringBuilder sb, boolean positive) {
        int digitIndex;
        if (!positive) {
            sb.append0('-');
        }
        int k = this.firstK;
        if (k < 0) {
            sb.append0('0');
            sb.append0('.');
            for (int i = k + 1; i < 0; i++) {
                sb.append0('0');
            }
        }
        int digitIndex2 = 0 + 1;
        int U = this.digits[0];
        while (true) {
            if (U != -1) {
                sb.append0((char) (U + 48));
            } else if (k >= -1) {
                sb.append0('0');
            }
            if (k == 0) {
                sb.append0('.');
            }
            k--;
            if (digitIndex2 < this.digitCount) {
                digitIndex = digitIndex2 + 1;
                U = this.digits[digitIndex2];
            } else {
                U = -1;
                digitIndex = digitIndex2;
            }
            if (U == -1 && k < -1) {
                return;
            } else {
                digitIndex2 = digitIndex;
            }
        }
    }

    private void longDigitGenerator(long f, int e, boolean isDenormalized, boolean mantissaIsZero, int p) {
        long M;
        long R;
        long S;
        int U;
        boolean low;
        boolean high;
        if (e >= 0) {
            M = 1 << e;
            if (!mantissaIsZero) {
                R = f << (e + 1);
                S = 2;
            } else {
                R = f << (e + 2);
                S = 4;
            }
        } else {
            M = 1;
            if (isDenormalized || !mantissaIsZero) {
                R = f << 1;
                S = 1 << (1 - e);
            } else {
                R = f << 2;
                S = 1 << (2 - e);
            }
        }
        int k = (int) Math.ceil((((double) ((e + p) - 1)) * invLogOfTenBaseTwo) - 1.0E-10d);
        if (k > 0) {
            S *= MathUtils.LONG_POWERS_OF_TEN[k];
        } else if (k < 0) {
            long scale = MathUtils.LONG_POWERS_OF_TEN[-k];
            R *= scale;
            M = M == 1 ? scale : M * scale;
        }
        if (R + M > S) {
            this.firstK = k;
        } else {
            this.firstK = k - 1;
            R *= 10;
            M *= 10;
        }
        while (true) {
            U = 0;
            for (int i = 3; i >= 0; i--) {
                long remainder = R - (S << i);
                if (remainder >= 0) {
                    R = remainder;
                    U += 1 << i;
                }
            }
            low = R < M;
            high = R + M > S;
            if (low || high) {
                break;
            }
            R *= 10;
            M *= 10;
            int[] iArr = this.digits;
            int i2 = this.digitCount;
            this.digitCount = i2 + 1;
            iArr[i2] = U;
        }
        if (low && !high) {
            int[] iArr2 = this.digits;
            int i3 = this.digitCount;
            this.digitCount = i3 + 1;
            iArr2[i3] = U;
            return;
        }
        if (high && !low) {
            int[] iArr3 = this.digits;
            int i4 = this.digitCount;
            this.digitCount = i4 + 1;
            iArr3[i4] = U + 1;
            return;
        }
        if ((R << 1) < S) {
            int[] iArr4 = this.digits;
            int i5 = this.digitCount;
            this.digitCount = i5 + 1;
            iArr4[i5] = U;
            return;
        }
        int[] iArr5 = this.digits;
        int i6 = this.digitCount;
        this.digitCount = i6 + 1;
        iArr5[i6] = U + 1;
    }
}
