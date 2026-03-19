package sun.misc;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.util.locale.LanguageTag;

public class FloatingDecimal {
    static final int BIG_DECIMAL_EXPONENT = 324;
    static final long EXP_ONE = 4607182418800017408L;
    static final int EXP_SHIFT = 52;
    static final long FRACT_HOB = 4503599627370496L;
    static final int INT_DECIMAL_DIGITS = 9;
    static final int MAX_DECIMAL_DIGITS = 15;
    static final int MAX_DECIMAL_EXPONENT = 308;
    static final int MAX_NDIGITS = 1100;
    static final int MAX_SMALL_BIN_EXP = 62;
    static final int MIN_DECIMAL_EXPONENT = -324;
    static final int MIN_SMALL_BIN_EXP = -21;
    static final int SINGLE_EXP_SHIFT = 23;
    static final int SINGLE_FRACT_HOB = 8388608;
    static final int SINGLE_MAX_DECIMAL_DIGITS = 7;
    static final int SINGLE_MAX_DECIMAL_EXPONENT = 38;
    static final int SINGLE_MAX_NDIGITS = 200;
    static final int SINGLE_MIN_DECIMAL_EXPONENT = -45;
    private static final String INFINITY_REP = "Infinity";
    private static final int INFINITY_LENGTH = INFINITY_REP.length();
    private static final String NAN_REP = "NaN";
    private static final int NAN_LENGTH = NAN_REP.length();
    static final boolean $assertionsDisabled = false;
    private static final BinaryToASCIIConverter B2AC_POSITIVE_INFINITY = new ExceptionalBinaryToASCIIBuffer(INFINITY_REP, $assertionsDisabled);
    private static final BinaryToASCIIConverter B2AC_NEGATIVE_INFINITY = new ExceptionalBinaryToASCIIBuffer("-Infinity", true);
    private static final BinaryToASCIIConverter B2AC_NOT_A_NUMBER = new ExceptionalBinaryToASCIIBuffer(NAN_REP, $assertionsDisabled);
    private static final BinaryToASCIIConverter B2AC_POSITIVE_ZERO = new BinaryToASCIIBuffer($assertionsDisabled, new char[]{'0'});
    private static final BinaryToASCIIConverter B2AC_NEGATIVE_ZERO = new BinaryToASCIIBuffer(true, new char[]{'0'});
    private static final ThreadLocal<BinaryToASCIIBuffer> threadLocalBinaryToASCIIBuffer = new ThreadLocal<BinaryToASCIIBuffer>() {
        @Override
        protected BinaryToASCIIBuffer initialValue() {
            return new BinaryToASCIIBuffer();
        }
    };
    static final ASCIIToBinaryConverter A2BC_POSITIVE_INFINITY = new PreparedASCIIToBinaryBuffer(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    static final ASCIIToBinaryConverter A2BC_NEGATIVE_INFINITY = new PreparedASCIIToBinaryBuffer(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    static final ASCIIToBinaryConverter A2BC_NOT_A_NUMBER = new PreparedASCIIToBinaryBuffer(Double.NaN, Float.NaN);
    static final ASCIIToBinaryConverter A2BC_POSITIVE_ZERO = new PreparedASCIIToBinaryBuffer(0.0d, 0.0f);
    static final ASCIIToBinaryConverter A2BC_NEGATIVE_ZERO = new PreparedASCIIToBinaryBuffer(-0.0d, -0.0f);

    interface ASCIIToBinaryConverter {
        double doubleValue();

        float floatValue();
    }

    public interface BinaryToASCIIConverter {
        void appendTo(Appendable appendable);

        boolean decimalDigitsExact();

        boolean digitsRoundedUp();

        int getDecimalExponent();

        int getDigits(char[] cArr);

        boolean isExceptional();

        boolean isNegative();

        String toJavaFormatString();
    }

    public static String toJavaFormatString(double d) {
        return getBinaryToASCIIConverter(d).toJavaFormatString();
    }

    public static String toJavaFormatString(float f) {
        return getBinaryToASCIIConverter(f).toJavaFormatString();
    }

    public static void appendTo(double d, Appendable appendable) {
        getBinaryToASCIIConverter(d).appendTo(appendable);
    }

    public static void appendTo(float f, Appendable appendable) {
        getBinaryToASCIIConverter(f).appendTo(appendable);
    }

    public static double parseDouble(String str) throws NumberFormatException {
        return readJavaFormatString(str).doubleValue();
    }

    public static float parseFloat(String str) throws NumberFormatException {
        return readJavaFormatString(str).floatValue();
    }

    private static class ExceptionalBinaryToASCIIBuffer implements BinaryToASCIIConverter {
        static final boolean $assertionsDisabled = false;
        private final String image;
        private boolean isNegative;

        public ExceptionalBinaryToASCIIBuffer(String str, boolean z) {
            this.image = str;
            this.isNegative = z;
        }

        @Override
        public String toJavaFormatString() {
            return this.image;
        }

        @Override
        public void appendTo(Appendable appendable) {
            if (appendable instanceof StringBuilder) {
                ((StringBuilder) appendable).append(this.image);
            } else if (appendable instanceof StringBuffer) {
                ((StringBuffer) appendable).append(this.image);
            }
        }

        @Override
        public int getDecimalExponent() {
            throw new IllegalArgumentException("Exceptional value does not have an exponent");
        }

        @Override
        public int getDigits(char[] cArr) {
            throw new IllegalArgumentException("Exceptional value does not have digits");
        }

        @Override
        public boolean isNegative() {
            return this.isNegative;
        }

        @Override
        public boolean isExceptional() {
            return true;
        }

        @Override
        public boolean digitsRoundedUp() {
            throw new IllegalArgumentException("Exceptional value is not rounded");
        }

        @Override
        public boolean decimalDigitsExact() {
            throw new IllegalArgumentException("Exceptional value is not exact");
        }
    }

    static class BinaryToASCIIBuffer implements BinaryToASCIIConverter {
        static final boolean $assertionsDisabled = false;
        private final char[] buffer;
        private int decExponent;
        private boolean decimalDigitsRoundedUp;
        private final char[] digits;
        private boolean exactDecimalConversion;
        private int firstDigitIndex;
        private boolean isNegative;
        private int nDigits;
        private static int[] insignificantDigitsNumber = {0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 18, 19};
        private static final int[] N_5_BITS = {0, 3, 5, 7, 10, 12, 14, 17, 19, 21, 24, 26, 28, 31, 33, 35, 38, 40, 42, 45, 47, 49, FloatingDecimal.EXP_SHIFT, 54, 56, 59, 61};

        BinaryToASCIIBuffer() {
            this.buffer = new char[26];
            this.exactDecimalConversion = FloatingDecimal.$assertionsDisabled;
            this.decimalDigitsRoundedUp = FloatingDecimal.$assertionsDisabled;
            this.digits = new char[20];
        }

        BinaryToASCIIBuffer(boolean z, char[] cArr) {
            this.buffer = new char[26];
            this.exactDecimalConversion = FloatingDecimal.$assertionsDisabled;
            this.decimalDigitsRoundedUp = FloatingDecimal.$assertionsDisabled;
            this.isNegative = z;
            this.decExponent = 0;
            this.digits = cArr;
            this.firstDigitIndex = 0;
            this.nDigits = cArr.length;
        }

        @Override
        public String toJavaFormatString() {
            return new String(this.buffer, 0, getChars(this.buffer));
        }

        @Override
        public void appendTo(Appendable appendable) {
            int chars = getChars(this.buffer);
            if (appendable instanceof StringBuilder) {
                ((StringBuilder) appendable).append(this.buffer, 0, chars);
            } else if (appendable instanceof StringBuffer) {
                ((StringBuffer) appendable).append(this.buffer, 0, chars);
            }
        }

        @Override
        public int getDecimalExponent() {
            return this.decExponent;
        }

        @Override
        public int getDigits(char[] cArr) {
            System.arraycopy((Object) this.digits, this.firstDigitIndex, (Object) cArr, 0, this.nDigits);
            return this.nDigits;
        }

        @Override
        public boolean isNegative() {
            return this.isNegative;
        }

        @Override
        public boolean isExceptional() {
            return FloatingDecimal.$assertionsDisabled;
        }

        @Override
        public boolean digitsRoundedUp() {
            return this.decimalDigitsRoundedUp;
        }

        @Override
        public boolean decimalDigitsExact() {
            return this.exactDecimalConversion;
        }

        private void setSign(boolean z) {
            this.isNegative = z;
        }

        private void developLongDigits(int i, long j, int i2) {
            if (i2 != 0) {
                long j2 = FDBigInteger.LONG_5_POW[i2] << i2;
                long j3 = j % j2;
                j /= j2;
                i += i2;
                if (j3 >= (j2 >> 1)) {
                    j++;
                }
            }
            int length = this.digits.length - 1;
            if (j <= 2147483647L) {
                int i3 = (int) j;
                int i4 = i3 % 10;
                int i5 = i3 / 10;
                while (i4 == 0) {
                    i++;
                    i4 = i5 % 10;
                    i5 /= 10;
                }
                while (i5 != 0) {
                    this.digits[length] = (char) (i4 + 48);
                    i++;
                    i4 = i5 % 10;
                    i5 /= 10;
                    length--;
                }
                this.digits[length] = (char) (i4 + 48);
            } else {
                int i6 = (int) (j % 10);
                long j4 = j / 10;
                while (i6 == 0) {
                    i++;
                    i6 = (int) (j4 % 10);
                    j4 /= 10;
                }
                while (j4 != 0) {
                    this.digits[length] = (char) (i6 + 48);
                    i++;
                    i6 = (int) (j4 % 10);
                    j4 /= 10;
                    length--;
                }
                this.digits[length] = (char) (i6 + 48);
            }
            this.decExponent = i + 1;
            this.firstDigitIndex = length;
            this.nDigits = this.digits.length - length;
        }

        private void dtoa(int i, long j, int i2, boolean z) {
            int i3;
            boolean z2;
            boolean z3;
            long jCmp;
            int i4;
            long j2;
            int iInsignificantDigitsForPow2;
            long j3;
            int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(j);
            int i5 = 53 - iNumberOfTrailingZeros;
            this.decimalDigitsRoundedUp = FloatingDecimal.$assertionsDisabled;
            this.exactDecimalConversion = FloatingDecimal.$assertionsDisabled;
            int iMax = Math.max(0, (i5 - i) - 1);
            if (i <= FloatingDecimal.MAX_SMALL_BIN_EXP && i >= FloatingDecimal.MIN_SMALL_BIN_EXP && iMax < FDBigInteger.LONG_5_POW.length && N_5_BITS[iMax] + i5 < 64 && iMax == 0) {
                if (i > i2) {
                    iInsignificantDigitsForPow2 = insignificantDigitsForPow2((i - i2) - 1);
                } else {
                    iInsignificantDigitsForPow2 = 0;
                }
                if (i >= FloatingDecimal.EXP_SHIFT) {
                    j3 = j << (i - FloatingDecimal.EXP_SHIFT);
                } else {
                    j3 = j >>> (FloatingDecimal.EXP_SHIFT - i);
                }
                developLongDigits(0, j3, iInsignificantDigitsForPow2);
                return;
            }
            int iEstimateDecExp = estimateDecExp(j, i);
            int iMax2 = Math.max(0, -iEstimateDecExp);
            int i6 = iMax2 + iMax + i;
            int iMax3 = Math.max(0, iEstimateDecExp);
            int i7 = iMax + iMax3;
            int i8 = i6 - i2;
            long j4 = j >>> iNumberOfTrailingZeros;
            int i9 = i6 - (i5 - 1);
            int iMin = Math.min(i9, i7);
            int i10 = i9 - iMin;
            int i11 = i7 - iMin;
            int i12 = i8 - iMin;
            if (i5 == 1) {
                i12--;
            }
            if (i12 < 0) {
                i10 -= i12;
                i11 -= i12;
                i12 = 0;
            }
            int i13 = i5 + i10 + (iMax2 < N_5_BITS.length ? N_5_BITS[iMax2] : iMax2 * 3);
            int i14 = iMax3 + 1;
            int i15 = i11 + 1 + (i14 < N_5_BITS.length ? N_5_BITS[i14] : i14 * 3);
            if (i13 < 64 && i15 < 64) {
                if (i13 < 32 && i15 < 32) {
                    int i16 = (((int) j4) * FDBigInteger.SMALL_5_POW[iMax2]) << i10;
                    int i17 = FDBigInteger.SMALL_5_POW[iMax3] << i11;
                    int i18 = FDBigInteger.SMALL_5_POW[iMax2] << i12;
                    int i19 = i17 * 10;
                    int i20 = i16 / i17;
                    int i21 = (i16 % i17) * 10;
                    int i22 = i18 * 10;
                    z2 = i21 < i22;
                    z3 = i21 + i22 > i19;
                    if (i20 != 0 || z3) {
                        this.digits[0] = (char) (48 + i20);
                        i3 = 1;
                    } else {
                        iEstimateDecExp--;
                        i3 = 0;
                    }
                    if (!z || iEstimateDecExp < -3 || iEstimateDecExp >= 8) {
                        z2 = false;
                        z3 = false;
                    }
                    while (!z2 && !z3) {
                        int i23 = i21 / i17;
                        i21 = (i21 % i17) * 10;
                        i22 *= 10;
                        if (i22 > 0) {
                            z2 = i21 < i22;
                            z3 = i21 + i22 > i19;
                        } else {
                            z2 = true;
                            z3 = true;
                        }
                        this.digits[i3] = (char) (48 + i23);
                        i3++;
                    }
                    jCmp = (i21 << 1) - i19;
                    this.exactDecimalConversion = i21 == 0;
                } else {
                    long j5 = (j4 * FDBigInteger.LONG_5_POW[iMax2]) << i10;
                    long j6 = FDBigInteger.LONG_5_POW[iMax3] << i11;
                    long j7 = j6 * 10;
                    int i24 = (int) (j5 / j6);
                    long j8 = (j5 % j6) * 10;
                    long j9 = (FDBigInteger.LONG_5_POW[iMax2] << i12) * 10;
                    boolean z4 = j8 < j9 ? true : FloatingDecimal.$assertionsDisabled;
                    boolean z5 = j8 + j9 > j7 ? true : FloatingDecimal.$assertionsDisabled;
                    if (i24 == 0 && !z5) {
                        iEstimateDecExp--;
                        i4 = 0;
                    } else {
                        this.digits[0] = (char) (48 + i24);
                        i4 = 1;
                    }
                    if (!z || iEstimateDecExp < -3 || iEstimateDecExp >= 8) {
                        j2 = j9;
                        z2 = FloatingDecimal.$assertionsDisabled;
                        z3 = FloatingDecimal.$assertionsDisabled;
                    } else {
                        z2 = z4;
                        z3 = z5;
                        j2 = j9;
                    }
                    while (!z2 && !z3) {
                        int i25 = (int) (j8 / j6);
                        j8 = (j8 % j6) * 10;
                        j2 *= 10;
                        if (j2 > 0) {
                            z2 = j8 < j2 ? true : FloatingDecimal.$assertionsDisabled;
                            z3 = j8 + j2 > j7 ? true : FloatingDecimal.$assertionsDisabled;
                        } else {
                            z2 = true;
                            z3 = true;
                        }
                        this.digits[i4] = (char) (48 + i25);
                        i4++;
                    }
                    long j10 = (j8 << 1) - j7;
                    this.exactDecimalConversion = j8 == 0 ? true : FloatingDecimal.$assertionsDisabled;
                    i3 = i4;
                    jCmp = j10;
                }
            } else {
                FDBigInteger fDBigIntegerValueOfPow52 = FDBigInteger.valueOfPow52(iMax3, i11);
                int normalizationBias = fDBigIntegerValueOfPow52.getNormalizationBias();
                FDBigInteger fDBigIntegerLeftShift = fDBigIntegerValueOfPow52.leftShift(normalizationBias);
                FDBigInteger fDBigIntegerValueOfMulPow52 = FDBigInteger.valueOfMulPow52(j4, iMax2, i10 + normalizationBias);
                FDBigInteger fDBigIntegerValueOfPow522 = FDBigInteger.valueOfPow52(iMax2 + 1, i12 + normalizationBias + 1);
                FDBigInteger fDBigIntegerValueOfPow523 = FDBigInteger.valueOfPow52(i14, i11 + normalizationBias + 1);
                int iQuoRemIteration = fDBigIntegerValueOfMulPow52.quoRemIteration(fDBigIntegerLeftShift);
                boolean z6 = fDBigIntegerValueOfMulPow52.cmp(fDBigIntegerValueOfPow522) < 0 ? true : FloatingDecimal.$assertionsDisabled;
                boolean z7 = fDBigIntegerValueOfPow523.addAndCmp(fDBigIntegerValueOfMulPow52, fDBigIntegerValueOfPow522) <= 0 ? true : FloatingDecimal.$assertionsDisabled;
                if (iQuoRemIteration == 0 && !z7) {
                    iEstimateDecExp--;
                    i3 = 0;
                } else {
                    this.digits[0] = (char) (48 + iQuoRemIteration);
                    i3 = 1;
                }
                if (!z || iEstimateDecExp < -3 || iEstimateDecExp >= 8) {
                    z2 = FloatingDecimal.$assertionsDisabled;
                    z3 = FloatingDecimal.$assertionsDisabled;
                } else {
                    z3 = z7;
                    z2 = z6;
                }
                while (!z2 && !z3) {
                    int iQuoRemIteration2 = fDBigIntegerValueOfMulPow52.quoRemIteration(fDBigIntegerLeftShift);
                    fDBigIntegerValueOfPow522 = fDBigIntegerValueOfPow522.multBy10();
                    z2 = fDBigIntegerValueOfMulPow52.cmp(fDBigIntegerValueOfPow522) < 0 ? true : FloatingDecimal.$assertionsDisabled;
                    z3 = fDBigIntegerValueOfPow523.addAndCmp(fDBigIntegerValueOfMulPow52, fDBigIntegerValueOfPow522) <= 0 ? true : FloatingDecimal.$assertionsDisabled;
                    this.digits[i3] = (char) (48 + iQuoRemIteration2);
                    i3++;
                }
                if (z3 && z2) {
                    fDBigIntegerValueOfMulPow52 = fDBigIntegerValueOfMulPow52.leftShift(1);
                    jCmp = fDBigIntegerValueOfMulPow52.cmp(fDBigIntegerValueOfPow523);
                } else {
                    jCmp = 0;
                }
                this.exactDecimalConversion = fDBigIntegerValueOfMulPow52.cmp(FDBigInteger.ZERO) == 0 ? true : FloatingDecimal.$assertionsDisabled;
            }
            this.decExponent = iEstimateDecExp + 1;
            this.firstDigitIndex = 0;
            this.nDigits = i3;
            if (z3) {
                if (!z2) {
                    roundup();
                    return;
                }
                if (jCmp == 0) {
                    if ((this.digits[(this.firstDigitIndex + this.nDigits) - 1] & 1) != 0) {
                        roundup();
                    }
                } else if (jCmp > 0) {
                    roundup();
                }
            }
        }

        private void roundup() {
            int i = (this.firstDigitIndex + this.nDigits) - 1;
            char c = this.digits[i];
            if (c == '9') {
                while (c == '9' && i > this.firstDigitIndex) {
                    this.digits[i] = '0';
                    i--;
                    c = this.digits[i];
                }
                if (c == '9') {
                    this.decExponent++;
                    this.digits[this.firstDigitIndex] = '1';
                    return;
                }
            }
            this.digits[i] = (char) (c + 1);
            this.decimalDigitsRoundedUp = true;
        }

        static int estimateDecExp(long j, int i) {
            double dLongBitsToDouble = ((Double.longBitsToDouble((j & DoubleConsts.SIGNIF_BIT_MASK) | FloatingDecimal.EXP_ONE) - 1.5d) * 0.289529654d) + 0.176091259d + (((double) i) * 0.301029995663981d);
            long jDoubleToRawLongBits = Double.doubleToRawLongBits(dLongBitsToDouble);
            int i2 = ((int) ((DoubleConsts.EXP_BIT_MASK & jDoubleToRawLongBits) >> 52)) - 1023;
            boolean z = (Long.MIN_VALUE & jDoubleToRawLongBits) != 0;
            if (i2 >= 0 && i2 < FloatingDecimal.EXP_SHIFT) {
                long j2 = DoubleConsts.SIGNIF_BIT_MASK >> i2;
                int i3 = (int) (((DoubleConsts.SIGNIF_BIT_MASK & jDoubleToRawLongBits) | FloatingDecimal.FRACT_HOB) >> (FloatingDecimal.EXP_SHIFT - i2));
                return z ? (j2 & jDoubleToRawLongBits) == 0 ? -i3 : (-i3) - 1 : i3;
            }
            if (i2 < 0) {
                return ((Long.MAX_VALUE & jDoubleToRawLongBits) != 0 && z) ? -1 : 0;
            }
            return (int) dLongBitsToDouble;
        }

        private static int insignificantDigits(int i) {
            int i2 = 0;
            while (true) {
                long j = i;
                if (j >= 10) {
                    i = (int) (j / 10);
                    i2++;
                } else {
                    return i2;
                }
            }
        }

        private static int insignificantDigitsForPow2(int i) {
            if (i > 1 && i < insignificantDigitsNumber.length) {
                return insignificantDigitsNumber[i];
            }
            return 0;
        }

        private int getChars(char[] cArr) {
            int i;
            int i2;
            int i3;
            int i4;
            int i5 = 0;
            if (this.isNegative) {
                cArr[0] = '-';
                i5 = 1;
            }
            if (this.decExponent > 0 && this.decExponent < 8) {
                int iMin = Math.min(this.nDigits, this.decExponent);
                System.arraycopy((Object) this.digits, this.firstDigitIndex, (Object) cArr, i5, iMin);
                int i6 = i5 + iMin;
                if (iMin < this.decExponent) {
                    int i7 = (this.decExponent - iMin) + i6;
                    Arrays.fill(cArr, i6, i7, '0');
                    int i8 = i7 + 1;
                    cArr[i7] = '.';
                    int i9 = i8 + 1;
                    cArr[i8] = '0';
                    return i9;
                }
                int i10 = i6 + 1;
                cArr[i6] = '.';
                if (iMin < this.nDigits) {
                    int i11 = this.nDigits - iMin;
                    System.arraycopy((Object) this.digits, this.firstDigitIndex + iMin, (Object) cArr, i10, i11);
                    return i10 + i11;
                }
                int i12 = i10 + 1;
                cArr[i10] = '0';
                return i12;
            }
            if (this.decExponent <= 0 && this.decExponent > -3) {
                int i13 = i5 + 1;
                cArr[i5] = '0';
                int i14 = i13 + 1;
                cArr[i13] = '.';
                if (this.decExponent != 0) {
                    Arrays.fill(cArr, i14, i14 - this.decExponent, '0');
                    i14 -= this.decExponent;
                }
                System.arraycopy((Object) this.digits, this.firstDigitIndex, (Object) cArr, i14, this.nDigits);
                return i14 + this.nDigits;
            }
            int i15 = i5 + 1;
            cArr[i5] = this.digits[this.firstDigitIndex];
            int i16 = i15 + 1;
            cArr[i15] = '.';
            if (this.nDigits > 1) {
                System.arraycopy((Object) this.digits, this.firstDigitIndex + 1, (Object) cArr, i16, this.nDigits - 1);
                i = i16 + (this.nDigits - 1);
            } else {
                i = i16 + 1;
                cArr[i16] = '0';
            }
            int i17 = i + 1;
            cArr[i] = 'E';
            if (this.decExponent <= 0) {
                i3 = i17 + 1;
                cArr[i17] = '-';
                i2 = (-this.decExponent) + 1;
            } else {
                i2 = this.decExponent - 1;
                i3 = i17;
            }
            if (i2 <= 9) {
                i4 = i3 + 1;
                cArr[i3] = (char) (i2 + 48);
            } else {
                if (i2 <= 99) {
                    int i18 = i3 + 1;
                    cArr[i3] = (char) ((i2 / 10) + 48);
                    int i19 = i18 + 1;
                    cArr[i18] = (char) ((i2 % 10) + 48);
                    return i19;
                }
                int i20 = i3 + 1;
                cArr[i3] = (char) ((i2 / 100) + 48);
                int i21 = i2 % 100;
                int i22 = i20 + 1;
                cArr[i20] = (char) ((i21 / 10) + 48);
                i4 = i22 + 1;
                cArr[i22] = (char) ((i21 % 10) + 48);
            }
            return i4;
        }
    }

    private static BinaryToASCIIBuffer getBinaryToASCIIBuffer() {
        return threadLocalBinaryToASCIIBuffer.get();
    }

    static class PreparedASCIIToBinaryBuffer implements ASCIIToBinaryConverter {
        private final double doubleVal;
        private final float floatVal;

        public PreparedASCIIToBinaryBuffer(double d, float f) {
            this.doubleVal = d;
            this.floatVal = f;
        }

        @Override
        public double doubleValue() {
            return this.doubleVal;
        }

        @Override
        public float floatValue() {
            return this.floatVal;
        }
    }

    static class ASCIIToBinaryBuffer implements ASCIIToBinaryConverter {
        static final boolean $assertionsDisabled = false;
        int decExponent;
        char[] digits;
        boolean isNegative;
        int nDigits;
        private static final double[] SMALL_10_POW = {1.0d, 10.0d, 100.0d, 1000.0d, 10000.0d, 100000.0d, 1000000.0d, 1.0E7d, 1.0E8d, 1.0E9d, 1.0E10d, 1.0E11d, 1.0E12d, 1.0E13d, 1.0E14d, 1.0E15d, 1.0E16d, 1.0E17d, 1.0E18d, 1.0E19d, 1.0E20d, 1.0E21d, 1.0E22d};
        private static final float[] SINGLE_SMALL_10_POW = {1.0f, 10.0f, 100.0f, 1000.0f, 10000.0f, 100000.0f, 1000000.0f, 1.0E7f, 1.0E8f, 1.0E9f, 1.0E10f};
        private static final double[] BIG_10_POW = {1.0E16d, 1.0E32d, 1.0E64d, 1.0E128d, 1.0E256d};
        private static final double[] TINY_10_POW = {1.0E-16d, 1.0E-32d, 1.0E-64d, 1.0E-128d, 1.0E-256d};
        private static final int MAX_SMALL_TEN = SMALL_10_POW.length - 1;
        private static final int SINGLE_MAX_SMALL_TEN = SINGLE_SMALL_10_POW.length - 1;

        ASCIIToBinaryBuffer(boolean z, int i, char[] cArr, int i2) {
            this.isNegative = z;
            this.decExponent = i;
            this.digits = cArr;
            this.nDigits = i2;
        }

        @Override
        public double doubleValue() {
            long j;
            int i;
            int i2;
            int i3;
            int i4;
            FDBigInteger fDBigIntegerRightInplaceSub;
            boolean z;
            int iMin = Math.min(this.nDigits, 16);
            int i5 = this.digits[0] - '0';
            int iMin2 = Math.min(iMin, 9);
            int i6 = 1;
            int i7 = i5;
            for (int i8 = 1; i8 < iMin2; i8++) {
                i7 = ((i7 * 10) + this.digits[i8]) - 48;
            }
            long j2 = i7;
            while (iMin2 < iMin) {
                j2 = (j2 * 10) + ((long) (this.digits[iMin2] - '0'));
                iMin2++;
            }
            double d = j2;
            int i9 = this.decExponent - iMin;
            if (this.nDigits <= 15) {
                if (i9 == 0 || d == 0.0d) {
                    return this.isNegative ? -d : d;
                }
                if (i9 >= 0) {
                    if (i9 <= MAX_SMALL_TEN) {
                        double d2 = d * SMALL_10_POW[i9];
                        return this.isNegative ? -d2 : d2;
                    }
                    int i10 = 15 - iMin;
                    if (i9 <= MAX_SMALL_TEN + i10) {
                        double d3 = d * SMALL_10_POW[i10] * SMALL_10_POW[i9 - i10];
                        return this.isNegative ? -d3 : d3;
                    }
                } else if (i9 >= (-MAX_SMALL_TEN)) {
                    double d4 = d / SMALL_10_POW[-i9];
                    return this.isNegative ? -d4 : d4;
                }
            }
            if (i9 > 0) {
                if (this.decExponent > 309) {
                    return this.isNegative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                }
                int i11 = i9 & 15;
                if (i11 != 0) {
                    d *= SMALL_10_POW[i11];
                }
                int i12 = i9 >> 4;
                if (i12 != 0) {
                    int i13 = 0;
                    while (i12 > 1) {
                        if ((i12 & 1) != 0) {
                            d *= BIG_10_POW[i13];
                        }
                        i13++;
                        i12 >>= 1;
                    }
                    double d5 = d * BIG_10_POW[i13];
                    if (Double.isInfinite(d5)) {
                        if (Double.isInfinite((d / 2.0d) * BIG_10_POW[i13])) {
                            return this.isNegative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                        }
                        d = Double.MAX_VALUE;
                    } else {
                        d = d5;
                    }
                }
            } else if (i9 < 0) {
                int i14 = -i9;
                if (this.decExponent < -325) {
                    return this.isNegative ? -0.0d : 0.0d;
                }
                int i15 = i14 & 15;
                if (i15 != 0) {
                    d /= SMALL_10_POW[i15];
                }
                int i16 = i14 >> 4;
                if (i16 != 0) {
                    int i17 = 0;
                    while (i16 > 1) {
                        if ((i16 & 1) != 0) {
                            d *= TINY_10_POW[i17];
                        }
                        i17++;
                        i16 >>= 1;
                    }
                    double d6 = TINY_10_POW[i17] * d;
                    if (d6 == 0.0d) {
                        if (d * 2.0d * TINY_10_POW[i17] == 0.0d) {
                            return this.isNegative ? -0.0d : 0.0d;
                        }
                        d = Double.MIN_VALUE;
                    } else {
                        d = d6;
                    }
                }
            }
            if (this.nDigits > FloatingDecimal.MAX_NDIGITS) {
                this.nDigits = 1101;
                this.digits[FloatingDecimal.MAX_NDIGITS] = '1';
            }
            FDBigInteger fDBigInteger = new FDBigInteger(j2, this.digits, iMin, this.nDigits);
            int i18 = this.decExponent - this.nDigits;
            long jDoubleToRawLongBits = Double.doubleToRawLongBits(d);
            int iMax = Math.max(0, -i18);
            int iMax2 = Math.max(0, i18);
            FDBigInteger fDBigIntegerMultByPow52 = fDBigInteger.multByPow52(iMax2, 0);
            fDBigIntegerMultByPow52.makeImmutable();
            FDBigInteger fDBigIntegerLeftShift = null;
            int i19 = 0;
            while (true) {
                int i20 = (int) (jDoubleToRawLongBits >>> 52);
                long j3 = DoubleConsts.SIGNIF_BIT_MASK & jDoubleToRawLongBits;
                if (i20 > 0) {
                    j = j3 | FloatingDecimal.FRACT_HOB;
                } else {
                    int iNumberOfLeadingZeros = Long.numberOfLeadingZeros(j3) - 11;
                    j = j3 << iNumberOfLeadingZeros;
                    i20 = 1 - iNumberOfLeadingZeros;
                }
                int i21 = i20 - 1023;
                int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(j);
                long j4 = j >>> iNumberOfTrailingZeros;
                int i22 = (i21 - 52) + iNumberOfTrailingZeros;
                int i23 = 53 - iNumberOfTrailingZeros;
                if (i22 >= 0) {
                    i2 = iMax + i22;
                    i = iMax2;
                } else {
                    i = iMax2 - i22;
                    i2 = iMax;
                }
                if (i21 <= -1023) {
                    i3 = i21 + iNumberOfTrailingZeros + 1023;
                } else {
                    i3 = i6 + iNumberOfTrailingZeros;
                }
                int i24 = i2;
                int i25 = i24 + i3;
                int i26 = i + i3;
                int iMin3 = Math.min(i25, Math.min(i26, i24));
                int i27 = i25 - iMin3;
                int i28 = i26 - iMin3;
                int i29 = i24 - iMin3;
                FDBigInteger fDBigIntegerValueOfMulPow52 = FDBigInteger.valueOfMulPow52(j4, iMax, i27);
                if (fDBigIntegerLeftShift == null || i19 != i28) {
                    fDBigIntegerLeftShift = fDBigIntegerMultByPow52.leftShift(i28);
                    i19 = i28;
                }
                int iCmp = fDBigIntegerValueOfMulPow52.cmp(fDBigIntegerLeftShift);
                if (iCmp > 0) {
                    fDBigIntegerRightInplaceSub = fDBigIntegerValueOfMulPow52.leftInplaceSub(fDBigIntegerLeftShift);
                    i4 = 1;
                    if (i23 != 1 || i22 <= -1022) {
                        z = true;
                    } else {
                        int i30 = i29 - 1;
                        if (i30 >= 0) {
                            i29 = i30;
                            z = true;
                        } else {
                            fDBigIntegerRightInplaceSub = fDBigIntegerRightInplaceSub.leftShift(1);
                            z = true;
                            i29 = 0;
                        }
                    }
                } else {
                    i4 = 1;
                    if (iCmp >= 0) {
                        break;
                    }
                    fDBigIntegerRightInplaceSub = fDBigIntegerLeftShift.rightInplaceSub(fDBigIntegerValueOfMulPow52);
                    z = FloatingDecimal.$assertionsDisabled;
                }
                int iCmpPow52 = fDBigIntegerRightInplaceSub.cmpPow52(iMax, i29);
                if (iCmpPow52 < 0) {
                    break;
                }
                if (iCmpPow52 != 0) {
                    jDoubleToRawLongBits += z ? -1L : 1L;
                    if (jDoubleToRawLongBits == 0 || jDoubleToRawLongBits == DoubleConsts.EXP_BIT_MASK) {
                        break;
                    }
                    i6 = i4;
                } else if ((jDoubleToRawLongBits & 1) != 0) {
                    jDoubleToRawLongBits += z ? -1L : 1L;
                }
            }
            if (this.isNegative) {
                jDoubleToRawLongBits |= Long.MIN_VALUE;
            }
            return Double.longBitsToDouble(jDoubleToRawLongBits);
        }

        @Override
        public float floatValue() {
            int i;
            int i2;
            int i3;
            int i4;
            FDBigInteger fDBigIntegerRightInplaceSub;
            boolean z;
            int i5 = 8;
            int iMin = Math.min(this.nDigits, 8);
            int i6 = 1;
            int i7 = this.digits[0] - '0';
            for (int i8 = 1; i8 < iMin; i8++) {
                i7 = ((i7 * 10) + this.digits[i8]) - 48;
            }
            float f = i7;
            int i9 = this.decExponent - iMin;
            if (this.nDigits > 7) {
                if (this.decExponent >= this.nDigits && this.nDigits + this.decExponent <= 15) {
                    long j = i7;
                    while (iMin < this.nDigits) {
                        j = (j * 10) + ((long) (this.digits[iMin] - '0'));
                        iMin++;
                    }
                    float f2 = (float) (j * SMALL_10_POW[this.decExponent - this.nDigits]);
                    return this.isNegative ? -f2 : f2;
                }
            } else {
                if (i9 == 0 || f == 0.0f) {
                    return this.isNegative ? -f : f;
                }
                if (i9 >= 0) {
                    if (i9 <= SINGLE_MAX_SMALL_TEN) {
                        float f3 = f * SINGLE_SMALL_10_POW[i9];
                        return this.isNegative ? -f3 : f3;
                    }
                    int i10 = 7 - iMin;
                    if (i9 <= SINGLE_MAX_SMALL_TEN + i10) {
                        float f4 = f * SINGLE_SMALL_10_POW[i10] * SINGLE_SMALL_10_POW[i9 - i10];
                        return this.isNegative ? -f4 : f4;
                    }
                } else if (i9 >= (-SINGLE_MAX_SMALL_TEN)) {
                    float f5 = f / SINGLE_SMALL_10_POW[-i9];
                    return this.isNegative ? -f5 : f5;
                }
            }
            double d = f;
            if (i9 > 0) {
                if (this.decExponent > 39) {
                    return this.isNegative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
                }
                int i11 = i9 & 15;
                if (i11 != 0) {
                    d *= SMALL_10_POW[i11];
                }
                int i12 = i9 >> 4;
                if (i12 != 0) {
                    int i13 = 0;
                    while (i12 > 0) {
                        if ((i12 & 1) != 0) {
                            d *= BIG_10_POW[i13];
                        }
                        i13++;
                        i12 >>= 1;
                    }
                }
            } else if (i9 < 0) {
                int i14 = -i9;
                if (this.decExponent < -46) {
                    return this.isNegative ? -0.0f : 0.0f;
                }
                int i15 = i14 & 15;
                if (i15 != 0) {
                    d /= SMALL_10_POW[i15];
                }
                int i16 = i14 >> 4;
                if (i16 != 0) {
                    int i17 = 0;
                    while (i16 > 0) {
                        if ((i16 & 1) != 0) {
                            d *= TINY_10_POW[i17];
                        }
                        i17++;
                        i16 >>= 1;
                    }
                }
            }
            float fMax = Math.max(Float.MIN_VALUE, Math.min(Float.MAX_VALUE, (float) d));
            if (this.nDigits > 200) {
                this.nDigits = HttpURLConnection.HTTP_CREATED;
                this.digits[200] = '1';
            }
            FDBigInteger fDBigInteger = new FDBigInteger(i7, this.digits, iMin, this.nDigits);
            int i18 = this.decExponent - this.nDigits;
            int iFloatToRawIntBits = Float.floatToRawIntBits(fMax);
            int iMax = Math.max(0, -i18);
            int iMax2 = Math.max(0, i18);
            FDBigInteger fDBigIntegerMultByPow52 = fDBigInteger.multByPow52(iMax2, 0);
            fDBigIntegerMultByPow52.makeImmutable();
            FDBigInteger fDBigIntegerLeftShift = null;
            int i19 = 0;
            while (true) {
                int i20 = iFloatToRawIntBits >>> FloatingDecimal.SINGLE_EXP_SHIFT;
                int i21 = 8388607 & iFloatToRawIntBits;
                if (i20 > 0) {
                    i = i21 | FloatingDecimal.SINGLE_FRACT_HOB;
                } else {
                    int iNumberOfLeadingZeros = Integer.numberOfLeadingZeros(i21) - i5;
                    i = i21 << iNumberOfLeadingZeros;
                    i20 = 1 - iNumberOfLeadingZeros;
                }
                int i22 = i20 - 127;
                int iNumberOfTrailingZeros = Integer.numberOfTrailingZeros(i);
                int i23 = i >>> iNumberOfTrailingZeros;
                int i24 = (i22 - 23) + iNumberOfTrailingZeros;
                int i25 = 24 - iNumberOfTrailingZeros;
                if (i24 >= 0) {
                    i3 = iMax + i24;
                    i2 = iMax2;
                } else {
                    i2 = iMax2 - i24;
                    i3 = iMax;
                }
                if (i22 <= -127) {
                    i4 = i22 + iNumberOfTrailingZeros + 127;
                } else {
                    i4 = i6 + iNumberOfTrailingZeros;
                }
                int i26 = i3 + i4;
                int i27 = i2 + i4;
                int iMin2 = Math.min(i26, Math.min(i27, i3));
                int i28 = i27 - iMin2;
                int i29 = i3 - iMin2;
                FDBigInteger fDBigIntegerValueOfMulPow52 = FDBigInteger.valueOfMulPow52(i23, iMax, i26 - iMin2);
                if (fDBigIntegerLeftShift == null || i19 != i28) {
                    fDBigIntegerLeftShift = fDBigIntegerMultByPow52.leftShift(i28);
                    i19 = i28;
                }
                int iCmp = fDBigIntegerValueOfMulPow52.cmp(fDBigIntegerLeftShift);
                if (iCmp > 0) {
                    fDBigIntegerRightInplaceSub = fDBigIntegerValueOfMulPow52.leftInplaceSub(fDBigIntegerLeftShift);
                    i6 = 1;
                    if (i25 != 1 || i24 <= -126) {
                        z = true;
                    } else {
                        int i30 = i29 - 1;
                        if (i30 >= 0) {
                            i29 = i30;
                            z = true;
                        } else {
                            fDBigIntegerRightInplaceSub = fDBigIntegerRightInplaceSub.leftShift(1);
                            z = true;
                            i29 = 0;
                        }
                    }
                } else {
                    i6 = 1;
                    if (iCmp >= 0) {
                        break;
                    }
                    fDBigIntegerRightInplaceSub = fDBigIntegerLeftShift.rightInplaceSub(fDBigIntegerValueOfMulPow52);
                    z = FloatingDecimal.$assertionsDisabled;
                }
                int iCmpPow52 = fDBigIntegerRightInplaceSub.cmpPow52(iMax, i29);
                if (iCmpPow52 < 0) {
                    break;
                }
                if (iCmpPow52 == 0) {
                    if ((iFloatToRawIntBits & 1) != 0) {
                        iFloatToRawIntBits += z ? -1 : i6;
                    }
                } else {
                    iFloatToRawIntBits += z ? -1 : i6;
                    if (iFloatToRawIntBits == 0 || iFloatToRawIntBits == 2139095040) {
                        break;
                    }
                    i5 = 8;
                }
            }
            if (this.isNegative) {
                iFloatToRawIntBits |= Integer.MIN_VALUE;
            }
            return Float.intBitsToFloat(iFloatToRawIntBits);
        }
    }

    public static BinaryToASCIIConverter getBinaryToASCIIConverter(double d) {
        return getBinaryToASCIIConverter(d, true);
    }

    static BinaryToASCIIConverter getBinaryToASCIIConverter(double d, boolean z) {
        boolean z2;
        int i;
        long j;
        long jDoubleToRawLongBits = Double.doubleToRawLongBits(d);
        if ((Long.MIN_VALUE & jDoubleToRawLongBits) == 0) {
            z2 = $assertionsDisabled;
        } else {
            z2 = true;
        }
        long j2 = DoubleConsts.SIGNIF_BIT_MASK & jDoubleToRawLongBits;
        int i2 = (int) ((jDoubleToRawLongBits & DoubleConsts.EXP_BIT_MASK) >> 52);
        if (i2 == 2047) {
            if (j2 == 0) {
                return z2 ? B2AC_NEGATIVE_INFINITY : B2AC_POSITIVE_INFINITY;
            }
            return B2AC_NOT_A_NUMBER;
        }
        if (i2 != 0) {
            i = 53;
            j = FRACT_HOB | j2;
        } else {
            if (j2 == 0) {
                return z2 ? B2AC_NEGATIVE_ZERO : B2AC_POSITIVE_ZERO;
            }
            int iNumberOfLeadingZeros = Long.numberOfLeadingZeros(j2);
            int i3 = iNumberOfLeadingZeros - 11;
            i = 64 - iNumberOfLeadingZeros;
            i2 = 1 - i3;
            j = j2 << i3;
        }
        int i4 = i2 - 1023;
        BinaryToASCIIBuffer binaryToASCIIBuffer = getBinaryToASCIIBuffer();
        binaryToASCIIBuffer.setSign(z2);
        binaryToASCIIBuffer.dtoa(i4, j, i, z);
        return binaryToASCIIBuffer;
    }

    private static BinaryToASCIIConverter getBinaryToASCIIConverter(float f) {
        boolean z;
        int i;
        int i2;
        int iFloatToRawIntBits = Float.floatToRawIntBits(f);
        if ((Integer.MIN_VALUE & iFloatToRawIntBits) == 0) {
            z = $assertionsDisabled;
        } else {
            z = true;
        }
        int i3 = 8388607 & iFloatToRawIntBits;
        int i4 = (iFloatToRawIntBits & FloatConsts.EXP_BIT_MASK) >> SINGLE_EXP_SHIFT;
        if (i4 == 255) {
            if (i3 == 0) {
                return z ? B2AC_NEGATIVE_INFINITY : B2AC_POSITIVE_INFINITY;
            }
            return B2AC_NOT_A_NUMBER;
        }
        if (i4 == 0) {
            if (i3 == 0) {
                return z ? B2AC_NEGATIVE_ZERO : B2AC_POSITIVE_ZERO;
            }
            int iNumberOfLeadingZeros = Integer.numberOfLeadingZeros(i3);
            int i5 = iNumberOfLeadingZeros - 8;
            i = i3 << i5;
            i2 = 32 - iNumberOfLeadingZeros;
            i4 = 1 - i5;
        } else {
            i = i3 | SINGLE_FRACT_HOB;
            i2 = 24;
        }
        int i6 = i4 - 127;
        BinaryToASCIIBuffer binaryToASCIIBuffer = getBinaryToASCIIBuffer();
        binaryToASCIIBuffer.setSign(z);
        binaryToASCIIBuffer.dtoa(i6, ((long) i) << 29, i2, true);
        return binaryToASCIIBuffer;
    }

    static ASCIIToBinaryConverter readJavaFormatString(String str) throws NumberFormatException {
        String strTrim;
        int length;
        boolean z;
        boolean z2;
        int i;
        boolean z3;
        char cCharAt;
        int i2;
        int i3;
        char cCharAt2;
        int i4;
        int i5;
        boolean z4;
        int i6;
        int i7;
        int i8;
        int i9;
        char cCharAt3;
        try {
            strTrim = str.trim();
            try {
                length = strTrim.length();
            } catch (StringIndexOutOfBoundsException e) {
            }
        } catch (StringIndexOutOfBoundsException e2) {
            strTrim = str;
        }
        if (length == 0) {
            throw new NumberFormatException("empty String");
        }
        char cCharAt4 = strTrim.charAt(0);
        if (cCharAt4 == '+') {
            z = false;
        } else if (cCharAt4 == '-') {
            z = true;
        } else {
            i = 0;
            z2 = false;
            z3 = false;
            cCharAt = strTrim.charAt(i);
            if (cCharAt != 'N') {
                if (length - i == NAN_LENGTH && strTrim.indexOf(NAN_REP, i) == i) {
                    return A2BC_NOT_A_NUMBER;
                }
            } else if (cCharAt == 'I') {
                if (length - i == INFINITY_LENGTH && strTrim.indexOf(INFINITY_REP, i) == i) {
                    return z2 ? A2BC_NEGATIVE_INFINITY : A2BC_POSITIVE_INFINITY;
                }
            } else {
                if (cCharAt == '0' && length > (i9 = i + 1) && ((cCharAt3 = strTrim.charAt(i9)) == 'x' || cCharAt3 == 'X')) {
                    return parseHexString(strTrim);
                }
                char[] cArr = new char[length];
                int i10 = 0;
                boolean z5 = false;
                int i11 = 0;
                while (i < length) {
                    char cCharAt5 = strTrim.charAt(i);
                    if (cCharAt5 == '0') {
                        i10++;
                    } else {
                        if (cCharAt5 != '.') {
                            break;
                        }
                        if (z5) {
                            throw new NumberFormatException("multiple points");
                        }
                        if (z3) {
                            i8 = i - 1;
                        } else {
                            i8 = i;
                        }
                        i11 = i8;
                        z5 = true;
                    }
                    i++;
                }
                boolean z6 = z5;
                int i12 = i11;
                int i13 = 0;
                int i14 = 0;
                while (i < length) {
                    char cCharAt6 = strTrim.charAt(i);
                    if (cCharAt6 >= '1' && cCharAt6 <= '9') {
                        cArr[i13] = cCharAt6;
                        i13++;
                        i14 = 0;
                    } else if (cCharAt6 == '0') {
                        cArr[i13] = cCharAt6;
                        i14++;
                        i13++;
                    } else {
                        if (cCharAt6 != '.') {
                            break;
                        }
                        if (z6) {
                            throw new NumberFormatException("multiple points");
                        }
                        if (z3) {
                            i7 = i - 1;
                        } else {
                            i7 = i;
                        }
                        i12 = i7;
                        z6 = true;
                    }
                    i++;
                }
                int i15 = i13 - i14;
                boolean z7 = i15 == 0 ? true : $assertionsDisabled;
                if (!z7 || i10 != 0) {
                    if (z6) {
                        i2 = i12 - i10;
                    } else {
                        i2 = i15 + i14;
                    }
                    if (i < length && ((cCharAt2 = strTrim.charAt(i)) == 'e' || cCharAt2 == 'E')) {
                        int i16 = i + 1;
                        char cCharAt7 = strTrim.charAt(i16);
                        if (cCharAt7 == '+') {
                            i4 = 1;
                        } else if (cCharAt7 == '-') {
                            i4 = -1;
                        } else {
                            i4 = 1;
                            i5 = i16;
                            int i17 = 0;
                            z4 = $assertionsDisabled;
                            while (i5 < length) {
                                if (i17 >= 214748364) {
                                    z4 = true;
                                }
                                int i18 = i5 + 1;
                                char cCharAt8 = strTrim.charAt(i5);
                                if (cCharAt8 >= '0' && cCharAt8 <= '9') {
                                    i17 = (i17 * 10) + (cCharAt8 - '0');
                                    i5 = i18;
                                } else {
                                    i3 = i18 - 1;
                                    break;
                                }
                            }
                            i3 = i5;
                            i6 = BIG_DECIMAL_EXPONENT + i15 + i14;
                            if (!z4 || i17 > i6) {
                                i2 = i4 * i6;
                            } else {
                                i2 += i4 * i17;
                            }
                            if (i3 == i16) {
                            }
                        }
                        i16++;
                        i5 = i16;
                        int i172 = 0;
                        z4 = $assertionsDisabled;
                        while (i5 < length) {
                        }
                        i3 = i5;
                        i6 = BIG_DECIMAL_EXPONENT + i15 + i14;
                        if (!z4) {
                            i2 = i4 * i6;
                            if (i3 == i16) {
                            }
                        }
                    } else {
                        i3 = i;
                    }
                    int i19 = i2;
                    if (i3 < length && (i3 != length - 1 || (strTrim.charAt(i3) != 'f' && strTrim.charAt(i3) != 'F' && strTrim.charAt(i3) != 'd' && strTrim.charAt(i3) != 'D'))) {
                    }
                    if (z7) {
                        return z2 ? A2BC_NEGATIVE_ZERO : A2BC_POSITIVE_ZERO;
                    }
                    return new ASCIIToBinaryBuffer(z2, i19, cArr, i15);
                }
            }
            throw new NumberFormatException("For input string: \"" + strTrim + "\"");
        }
        z2 = z;
        i = 1;
        z3 = true;
        cCharAt = strTrim.charAt(i);
        if (cCharAt != 'N') {
        }
        throw new NumberFormatException("For input string: \"" + strTrim + "\"");
    }

    private static class HexFloatPattern {
        private static final Pattern VALUE = Pattern.compile("([-+])?0[xX](((\\p{XDigit}+)\\.?)|((\\p{XDigit}*)\\.(\\p{XDigit}+)))[pP]([-+])?(\\p{Digit}+)[fFdD]?");

        private HexFloatPattern() {
        }
    }

    static ASCIIToBinaryConverter parseHexString(String str) {
        int length;
        int length2;
        String string;
        int i;
        long hexDigit;
        int i2;
        boolean z;
        boolean z2;
        long j;
        double dLongBitsToDouble;
        Matcher matcher = HexFloatPattern.VALUE.matcher(str);
        if (!matcher.matches()) {
            throw new NumberFormatException("For input string: \"" + str + "\"");
        }
        boolean z3 = true;
        String strGroup = matcher.group(1);
        boolean z4 = strGroup != null && strGroup.equals(LanguageTag.SEP);
        String strGroup2 = matcher.group(4);
        if (strGroup2 != null) {
            string = stripLeadingZeros(strGroup2);
            length = string.length();
            length2 = 0;
        } else {
            String strStripLeadingZeros = stripLeadingZeros(matcher.group(6));
            length = strStripLeadingZeros.length();
            String strGroup3 = matcher.group(7);
            length2 = strGroup3.length();
            StringBuilder sb = new StringBuilder();
            if (strStripLeadingZeros == null) {
                strStripLeadingZeros = "";
            }
            sb.append(strStripLeadingZeros);
            sb.append(strGroup3);
            string = sb.toString();
        }
        String strStripLeadingZeros2 = stripLeadingZeros(string);
        int length3 = strStripLeadingZeros2.length();
        if (length < 1) {
            i = (-4) * ((length2 - length3) + 1);
        } else {
            i = 4 * (length - 1);
        }
        if (length3 == 0) {
            return z4 ? A2BC_NEGATIVE_ZERO : A2BC_POSITIVE_ZERO;
        }
        String strGroup4 = matcher.group(8);
        boolean z5 = strGroup4 == null || strGroup4.equals("+");
        try {
            long j2 = ((z5 ? 1L : -1L) * ((long) Integer.parseInt(matcher.group(9)))) + ((long) i);
            long hexDigit2 = getHexDigit(strStripLeadingZeros2, 0);
            if (hexDigit2 == 1) {
                hexDigit = 0 | (hexDigit2 << 52);
                i2 = 48;
            } else if (hexDigit2 <= 3) {
                hexDigit = 0 | (hexDigit2 << 51);
                i2 = 47;
                j2++;
            } else if (hexDigit2 <= 7) {
                hexDigit = 0 | (hexDigit2 << 50);
                i2 = 46;
                j2 += 2;
            } else if (hexDigit2 <= 15) {
                hexDigit = 0 | (hexDigit2 << 49);
                i2 = 45;
                j2 += 3;
            } else {
                throw new AssertionError((Object) "Result from digit conversion too large!");
            }
            int i3 = i2;
            int i4 = 1;
            while (i4 < length3 && i3 >= 0) {
                hexDigit |= ((long) getHexDigit(strStripLeadingZeros2, i4)) << i3;
                i3 -= 4;
                i4++;
            }
            if (i4 < length3) {
                long hexDigit3 = getHexDigit(strStripLeadingZeros2, i4);
                switch (i3) {
                    case -4:
                        z = (hexDigit3 & 8) != 0 ? true : $assertionsDisabled;
                        z2 = (hexDigit3 & 7) != 0 ? true : $assertionsDisabled;
                        break;
                    case -3:
                        hexDigit |= (hexDigit3 & 8) >> 3;
                        z = (hexDigit3 & 4) != 0 ? true : $assertionsDisabled;
                        z2 = (hexDigit3 & 3) != 0 ? true : $assertionsDisabled;
                        break;
                    case -2:
                        hexDigit |= (hexDigit3 & 12) >> 2;
                        z = (hexDigit3 & 2) != 0 ? true : $assertionsDisabled;
                        z2 = (hexDigit3 & 1) != 0 ? true : $assertionsDisabled;
                        break;
                    case -1:
                        hexDigit |= (hexDigit3 & 14) >> 1;
                        z = (hexDigit3 & 1) != 0 ? true : $assertionsDisabled;
                        z2 = $assertionsDisabled;
                        break;
                    default:
                        throw new AssertionError((Object) "Unexpected shift distance remainder.");
                }
                for (int i5 = i4 + 1; i5 < length3 && !z2; i5++) {
                    z2 = (z2 || ((long) getHexDigit(strStripLeadingZeros2, i5)) != 0) ? true : $assertionsDisabled;
                }
            } else {
                z = $assertionsDisabled;
                z2 = $assertionsDisabled;
            }
            int i6 = z4 ? Integer.MIN_VALUE : 0;
            if (j2 >= -126) {
                if (j2 > 127) {
                    i6 |= FloatConsts.EXP_BIT_MASK;
                } else {
                    boolean z6 = ((268435455 & hexDigit) != 0 || z || z2) ? true : $assertionsDisabled;
                    int i7 = (int) (hexDigit >>> 28);
                    if ((i7 & 3) != 1 || z6) {
                        i7++;
                    }
                    i6 |= ((((int) j2) + 126) << SINGLE_EXP_SHIFT) + (i7 >> 1);
                }
            } else if (j2 >= -150) {
                int i8 = (int) ((-98) - j2);
                boolean z7 = ((((1 << i8) - 1) & hexDigit) != 0 || z || z2) ? true : $assertionsDisabled;
                int i9 = (int) (hexDigit >>> i8);
                if ((i9 & 3) != 1 || z7) {
                    i9++;
                }
                i6 |= i9 >> 1;
            }
            float fIntBitsToFloat = Float.intBitsToFloat(i6);
            if (j2 > 1023) {
                return z4 ? A2BC_NEGATIVE_INFINITY : A2BC_POSITIVE_INFINITY;
            }
            if (j2 <= 1023 && j2 >= -1022) {
                j = (((j2 + 1023) << 52) & DoubleConsts.EXP_BIT_MASK) | (DoubleConsts.SIGNIF_BIT_MASK & hexDigit);
            } else {
                if (j2 < -1075) {
                    return z4 ? A2BC_NEGATIVE_ZERO : A2BC_POSITIVE_ZERO;
                }
                boolean z8 = (z2 || z) ? true : $assertionsDisabled;
                int i10 = 53 - ((((int) j2) + 1074) + 1);
                int i11 = i10 - 1;
                boolean z9 = ((1 << i11) & hexDigit) != 0 ? true : $assertionsDisabled;
                if (i10 > 1) {
                    z8 = (z8 || ((~((-1) << i11)) & hexDigit) != 0) ? true : $assertionsDisabled;
                }
                j = 0 | (DoubleConsts.SIGNIF_BIT_MASK & (hexDigit >> i10));
                boolean z10 = z9;
                z2 = z8;
                z = z10;
            }
            if ((j & 1) != 0) {
                z3 = $assertionsDisabled;
            }
            if ((z3 && z && z2) || (!z3 && z)) {
                j++;
            }
            if (z4) {
                dLongBitsToDouble = Double.longBitsToDouble(Long.MIN_VALUE | j);
            } else {
                dLongBitsToDouble = Double.longBitsToDouble(j);
            }
            return new PreparedASCIIToBinaryBuffer(dLongBitsToDouble, fIntBitsToFloat);
        } catch (NumberFormatException e) {
            return z4 ? z5 ? A2BC_NEGATIVE_INFINITY : A2BC_NEGATIVE_ZERO : z5 ? A2BC_POSITIVE_INFINITY : A2BC_POSITIVE_ZERO;
        }
    }

    static String stripLeadingZeros(String str) {
        if (!str.isEmpty() && str.charAt(0) == '0') {
            for (int i = 1; i < str.length(); i++) {
                if (str.charAt(i) != '0') {
                    return str.substring(i);
                }
            }
            return "";
        }
        return str;
    }

    static int getHexDigit(String str, int i) {
        int iDigit = Character.digit(str.charAt(i), 16);
        if (iDigit <= -1 || iDigit >= 16) {
            throw new AssertionError((Object) ("Unexpected failure of digit conversion of " + str.charAt(i)));
        }
        return iDigit;
    }
}
