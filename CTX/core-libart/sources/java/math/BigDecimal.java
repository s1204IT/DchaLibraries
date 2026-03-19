package java.math;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import libcore.math.MathUtils;

public class BigDecimal extends Number implements Comparable<BigDecimal>, Serializable {
    private static final int BI_SCALED_BY_ZERO_LENGTH = 11;
    private static final BigInteger[] FIVE_POW;
    private static final double LOG10_2 = 0.3010299956639812d;
    public static final BigDecimal ONE;
    public static final int ROUND_CEILING = 2;
    public static final int ROUND_DOWN = 1;
    public static final int ROUND_FLOOR = 3;
    public static final int ROUND_HALF_DOWN = 5;
    public static final int ROUND_HALF_EVEN = 6;
    public static final int ROUND_HALF_UP = 4;
    public static final int ROUND_UNNECESSARY = 7;
    public static final int ROUND_UP = 0;
    public static final BigDecimal TEN;
    private static final BigInteger[] TEN_POW;
    public static final BigDecimal ZERO;
    private static final long serialVersionUID = 6108874887143696463L;
    private transient int bitLength;
    private transient int hashCode;
    private BigInteger intVal;
    private transient int precision;
    private int scale;
    private transient long smallValue;
    private transient String toStringImage;
    private static final long[] LONG_FIVE_POW = {1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625, 1220703125, 6103515625L, 30517578125L, 152587890625L, 762939453125L, 3814697265625L, 19073486328125L, 95367431640625L, 476837158203125L, 2384185791015625L, 11920928955078125L, 59604644775390625L, 298023223876953125L, 1490116119384765625L, 7450580596923828125L};
    private static final int[] LONG_FIVE_POW_BIT_LENGTH = new int[LONG_FIVE_POW.length];
    private static final int[] LONG_POWERS_OF_TEN_BIT_LENGTH = new int[MathUtils.LONG_POWERS_OF_TEN.length];
    private static final BigDecimal[] BI_SCALED_BY_ZERO = new BigDecimal[11];
    private static final BigDecimal[] ZERO_SCALED_BY = new BigDecimal[11];
    private static final char[] CH_ZEROS = new char[100];

    static {
        Arrays.fill(CH_ZEROS, '0');
        for (int i = 0; i < ZERO_SCALED_BY.length; i++) {
            BI_SCALED_BY_ZERO[i] = new BigDecimal(i, 0);
            ZERO_SCALED_BY[i] = new BigDecimal(0, i);
        }
        for (int i2 = 0; i2 < LONG_FIVE_POW_BIT_LENGTH.length; i2++) {
            LONG_FIVE_POW_BIT_LENGTH[i2] = bitLength(LONG_FIVE_POW[i2]);
        }
        for (int i3 = 0; i3 < LONG_POWERS_OF_TEN_BIT_LENGTH.length; i3++) {
            LONG_POWERS_OF_TEN_BIT_LENGTH[i3] = bitLength(MathUtils.LONG_POWERS_OF_TEN[i3]);
        }
        TEN_POW = Multiplication.bigTenPows;
        FIVE_POW = Multiplication.bigFivePows;
        ZERO = new BigDecimal(0, 0);
        ONE = new BigDecimal(1, 0);
        TEN = new BigDecimal(10, 0);
    }

    private BigDecimal(long j, int i) {
        this.toStringImage = null;
        this.hashCode = 0;
        this.precision = 0;
        this.smallValue = j;
        this.scale = i;
        this.bitLength = bitLength(j);
    }

    private BigDecimal(int i, int i2) {
        this.toStringImage = null;
        this.hashCode = 0;
        this.precision = 0;
        this.smallValue = i;
        this.scale = i2;
        this.bitLength = bitLength(i);
    }

    public BigDecimal(char[] cArr, int i, int i2) {
        int i3;
        int i4;
        this.toStringImage = null;
        this.hashCode = 0;
        this.precision = 0;
        int i5 = (i2 - 1) + i;
        if (cArr == null) {
            throw new NullPointerException("in == null");
        }
        if (i5 >= cArr.length || i < 0 || i2 <= 0 || i5 < 0) {
            throw new NumberFormatException("Bad offset/length: offset=" + i + " len=" + i2 + " in.length=" + cArr.length);
        }
        StringBuilder sb = new StringBuilder(i2);
        if (i <= i5 && cArr[i] == '+') {
            i++;
        }
        int i6 = i;
        boolean z = false;
        while (i6 <= i5 && cArr[i6] != '.' && cArr[i6] != 'e' && cArr[i6] != 'E') {
            if (!z && cArr[i6] != '0') {
                z = true;
            }
            i6++;
        }
        int i7 = i6 - i;
        sb.append(cArr, i, i7);
        int i8 = i7 + 0;
        if (i6 <= i5 && cArr[i6] == '.') {
            int i9 = i6 + 1;
            i3 = i9;
            while (i3 <= i5 && cArr[i3] != 'e' && cArr[i3] != 'E') {
                if (!z && cArr[i3] != '0') {
                    z = true;
                }
                i3++;
            }
            this.scale = i3 - i9;
            i8 += this.scale;
            sb.append(cArr, i9, this.scale);
        } else {
            this.scale = 0;
            i3 = i6;
        }
        if (i3 <= i5 && (cArr[i3] == 'e' || cArr[i3] == 'E')) {
            int i10 = i3 + 1;
            if (i10 <= i5 && cArr[i10] == '+' && (i4 = i10 + 1) <= i5 && cArr[i4] != '-') {
                i10 = i4;
            }
            long j = ((long) this.scale) - ((long) Integer.parseInt(String.valueOf(cArr, i10, (i5 + 1) - i10)));
            this.scale = (int) j;
            if (j != this.scale) {
                throw new NumberFormatException("Scale out of range");
            }
        }
        if (i8 < 19) {
            this.smallValue = Long.parseLong(sb.toString());
            this.bitLength = bitLength(this.smallValue);
        } else {
            setUnscaledValue(new BigInteger(sb.toString()));
        }
    }

    public BigDecimal(char[] cArr, int i, int i2, MathContext mathContext) {
        this(cArr, i, i2);
        inplaceRound(mathContext);
    }

    public BigDecimal(char[] cArr) {
        this(cArr, 0, cArr.length);
    }

    public BigDecimal(char[] cArr, MathContext mathContext) {
        this(cArr, 0, cArr.length);
        inplaceRound(mathContext);
    }

    public BigDecimal(String str) {
        this(str.toCharArray(), 0, str.length());
    }

    public BigDecimal(String str, MathContext mathContext) {
        this(str.toCharArray(), 0, str.length());
        inplaceRound(mathContext);
    }

    public BigDecimal(double d) {
        this.toStringImage = null;
        this.hashCode = 0;
        this.precision = 0;
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            throw new NumberFormatException("Infinity or NaN: " + d);
        }
        long jDoubleToLongBits = Double.doubleToLongBits(d);
        this.scale = 1075 - ((int) ((jDoubleToLongBits >> 52) & 2047));
        long j = this.scale == 1075 ? (jDoubleToLongBits & 4503599627370495L) << 1 : (jDoubleToLongBits & 4503599627370495L) | 4503599627370496L;
        if (j == 0) {
            this.scale = 0;
            this.precision = 1;
        }
        if (this.scale > 0) {
            int iMin = Math.min(this.scale, Long.numberOfTrailingZeros(j));
            j >>>= iMin;
            this.scale -= iMin;
        }
        j = (jDoubleToLongBits >> 63) != 0 ? -j : j;
        int iBitLength = bitLength(j);
        if (this.scale < 0) {
            this.bitLength = iBitLength == 0 ? 0 : iBitLength - this.scale;
            if (this.bitLength < 64) {
                this.smallValue = j << (-this.scale);
            } else {
                BigInt bigInt = new BigInt();
                bigInt.putLongInt(j);
                bigInt.shift(-this.scale);
                this.intVal = new BigInteger(bigInt);
            }
            this.scale = 0;
            return;
        }
        if (this.scale > 0) {
            if (this.scale < LONG_FIVE_POW.length && iBitLength + LONG_FIVE_POW_BIT_LENGTH[this.scale] < 64) {
                this.smallValue = j * LONG_FIVE_POW[this.scale];
                this.bitLength = bitLength(this.smallValue);
                return;
            } else {
                setUnscaledValue(Multiplication.multiplyByFivePow(BigInteger.valueOf(j), this.scale));
                return;
            }
        }
        this.smallValue = j;
        this.bitLength = iBitLength;
    }

    public BigDecimal(double d, MathContext mathContext) {
        this(d);
        inplaceRound(mathContext);
    }

    public BigDecimal(BigInteger bigInteger) {
        this(bigInteger, 0);
    }

    public BigDecimal(BigInteger bigInteger, MathContext mathContext) {
        this(bigInteger);
        inplaceRound(mathContext);
    }

    public BigDecimal(BigInteger bigInteger, int i) {
        this.toStringImage = null;
        this.hashCode = 0;
        this.precision = 0;
        if (bigInteger == null) {
            throw new NullPointerException("unscaledVal == null");
        }
        this.scale = i;
        setUnscaledValue(bigInteger);
    }

    public BigDecimal(BigInteger bigInteger, int i, MathContext mathContext) {
        this(bigInteger, i);
        inplaceRound(mathContext);
    }

    public BigDecimal(int i) {
        this(i, 0);
    }

    public BigDecimal(int i, MathContext mathContext) {
        this(i, 0);
        inplaceRound(mathContext);
    }

    public BigDecimal(long j) {
        this(j, 0);
    }

    public BigDecimal(long j, MathContext mathContext) {
        this(j);
        inplaceRound(mathContext);
    }

    public static BigDecimal valueOf(long j, int i) {
        if (i == 0) {
            return valueOf(j);
        }
        if (j == 0 && i >= 0 && i < ZERO_SCALED_BY.length) {
            return ZERO_SCALED_BY[i];
        }
        return new BigDecimal(j, i);
    }

    public static BigDecimal valueOf(long j) {
        if (j >= 0 && j < 11) {
            return BI_SCALED_BY_ZERO[(int) j];
        }
        return new BigDecimal(j, 0);
    }

    public static BigDecimal valueOf(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            throw new NumberFormatException("Infinity or NaN: " + d);
        }
        return new BigDecimal(Double.toString(d));
    }

    public BigDecimal add(BigDecimal bigDecimal) {
        int i = this.scale - bigDecimal.scale;
        if (isZero()) {
            if (i <= 0) {
                return bigDecimal;
            }
            if (bigDecimal.isZero()) {
                return this;
            }
        } else if (bigDecimal.isZero() && i >= 0) {
            return this;
        }
        if (i == 0) {
            if (Math.max(this.bitLength, bigDecimal.bitLength) + 1 < 64) {
                return valueOf(this.smallValue + bigDecimal.smallValue, this.scale);
            }
            return new BigDecimal(getUnscaledValue().add(bigDecimal.getUnscaledValue()), this.scale);
        }
        if (i > 0) {
            return addAndMult10(this, bigDecimal, i);
        }
        return addAndMult10(bigDecimal, this, -i);
    }

    private static BigDecimal addAndMult10(BigDecimal bigDecimal, BigDecimal bigDecimal2, int i) {
        if (i < MathUtils.LONG_POWERS_OF_TEN.length && Math.max(bigDecimal.bitLength, bigDecimal2.bitLength + LONG_POWERS_OF_TEN_BIT_LENGTH[i]) + 1 < 64) {
            return valueOf(bigDecimal.smallValue + (bigDecimal2.smallValue * MathUtils.LONG_POWERS_OF_TEN[i]), bigDecimal.scale);
        }
        BigInt bigInt = Multiplication.multiplyByTenPow(bigDecimal2.getUnscaledValue(), i).getBigInt();
        bigInt.add(bigDecimal.getUnscaledValue().getBigInt());
        return new BigDecimal(new BigInteger(bigInt), bigDecimal.scale);
    }

    public BigDecimal add(BigDecimal bigDecimal, MathContext mathContext) {
        BigDecimal bigDecimal2;
        BigDecimal bigDecimal3;
        BigInteger bigIntegerAdd;
        long j = ((long) this.scale) - ((long) bigDecimal.scale);
        if (bigDecimal.isZero() || isZero() || mathContext.getPrecision() == 0) {
            return add(bigDecimal).round(mathContext);
        }
        if (approxPrecision() < j - 1) {
            bigDecimal3 = this;
            bigDecimal2 = bigDecimal;
        } else {
            if (bigDecimal.approxPrecision() >= (-j) - 1) {
                return add(bigDecimal).round(mathContext);
            }
            bigDecimal2 = this;
            bigDecimal3 = bigDecimal;
        }
        if (mathContext.getPrecision() >= bigDecimal2.approxPrecision()) {
            return add(bigDecimal).round(mathContext);
        }
        int iSignum = bigDecimal2.signum();
        if (iSignum == bigDecimal3.signum()) {
            bigIntegerAdd = Multiplication.multiplyByPositiveInt(bigDecimal2.getUnscaledValue(), 10).add(BigInteger.valueOf(iSignum));
        } else {
            bigIntegerAdd = Multiplication.multiplyByPositiveInt(bigDecimal2.getUnscaledValue().subtract(BigInteger.valueOf(iSignum)), 10).add(BigInteger.valueOf(iSignum * 9));
        }
        return new BigDecimal(bigIntegerAdd, bigDecimal2.scale + 1).round(mathContext);
    }

    public BigDecimal subtract(BigDecimal bigDecimal) {
        int i = this.scale - bigDecimal.scale;
        if (isZero()) {
            if (i <= 0) {
                return bigDecimal.negate();
            }
            if (bigDecimal.isZero()) {
                return this;
            }
        } else if (bigDecimal.isZero() && i >= 0) {
            return this;
        }
        if (i == 0) {
            if (Math.max(this.bitLength, bigDecimal.bitLength) + 1 < 64) {
                return valueOf(this.smallValue - bigDecimal.smallValue, this.scale);
            }
            return new BigDecimal(getUnscaledValue().subtract(bigDecimal.getUnscaledValue()), this.scale);
        }
        if (i > 0) {
            if (i < MathUtils.LONG_POWERS_OF_TEN.length && Math.max(this.bitLength, bigDecimal.bitLength + LONG_POWERS_OF_TEN_BIT_LENGTH[i]) + 1 < 64) {
                return valueOf(this.smallValue - (bigDecimal.smallValue * MathUtils.LONG_POWERS_OF_TEN[i]), this.scale);
            }
            return new BigDecimal(getUnscaledValue().subtract(Multiplication.multiplyByTenPow(bigDecimal.getUnscaledValue(), i)), this.scale);
        }
        int i2 = -i;
        if (i2 < MathUtils.LONG_POWERS_OF_TEN.length && Math.max(this.bitLength + LONG_POWERS_OF_TEN_BIT_LENGTH[i2], bigDecimal.bitLength) + 1 < 64) {
            return valueOf((this.smallValue * MathUtils.LONG_POWERS_OF_TEN[i2]) - bigDecimal.smallValue, bigDecimal.scale);
        }
        return new BigDecimal(Multiplication.multiplyByTenPow(getUnscaledValue(), i2).subtract(bigDecimal.getUnscaledValue()), bigDecimal.scale);
    }

    public BigDecimal subtract(BigDecimal bigDecimal, MathContext mathContext) {
        BigInteger bigIntegerAdd;
        long j = ((long) bigDecimal.scale) - ((long) this.scale);
        if (bigDecimal.isZero() || isZero() || mathContext.getPrecision() == 0) {
            return subtract(bigDecimal).round(mathContext);
        }
        if (bigDecimal.approxPrecision() < j - 1 && mathContext.getPrecision() < approxPrecision()) {
            int iSignum = signum();
            if (iSignum != bigDecimal.signum()) {
                bigIntegerAdd = Multiplication.multiplyByPositiveInt(getUnscaledValue(), 10).add(BigInteger.valueOf(iSignum));
            } else {
                bigIntegerAdd = Multiplication.multiplyByPositiveInt(getUnscaledValue().subtract(BigInteger.valueOf(iSignum)), 10).add(BigInteger.valueOf(iSignum * 9));
            }
            return new BigDecimal(bigIntegerAdd, this.scale + 1).round(mathContext);
        }
        return subtract(bigDecimal).round(mathContext);
    }

    public BigDecimal multiply(BigDecimal bigDecimal) {
        long j = ((long) this.scale) + ((long) bigDecimal.scale);
        if (isZero() || bigDecimal.isZero()) {
            return zeroScaledBy(j);
        }
        if (this.bitLength + bigDecimal.bitLength < 64) {
            long j2 = this.smallValue * bigDecimal.smallValue;
            if (!(j2 == Long.MIN_VALUE && Math.signum((float) this.smallValue) * Math.signum((float) bigDecimal.smallValue) > 0.0f)) {
                return valueOf(j2, safeLongToInt(j));
            }
        }
        return new BigDecimal(getUnscaledValue().multiply(bigDecimal.getUnscaledValue()), safeLongToInt(j));
    }

    public BigDecimal multiply(BigDecimal bigDecimal, MathContext mathContext) {
        BigDecimal bigDecimalMultiply = multiply(bigDecimal);
        bigDecimalMultiply.inplaceRound(mathContext);
        return bigDecimalMultiply;
    }

    public BigDecimal divide(BigDecimal bigDecimal, int i, int i2) {
        return divide(bigDecimal, i, RoundingMode.valueOf(i2));
    }

    public BigDecimal divide(BigDecimal bigDecimal, int i, RoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException("roundingMode == null");
        }
        if (bigDecimal.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        long j = (((long) this.scale) - ((long) bigDecimal.scale)) - ((long) i);
        if (bitLength(j) > 32) {
            throw new ArithmeticException("Unable to perform divisor / dividend scaling: the difference in scale is too big (" + j + ")");
        }
        if (this.bitLength < 64 && bigDecimal.bitLength < 64) {
            if (j == 0) {
                if (this.smallValue != Long.MIN_VALUE || bigDecimal.smallValue != -1) {
                    return dividePrimitiveLongs(this.smallValue, bigDecimal.smallValue, i, roundingMode);
                }
            } else if (j <= 0) {
                long j2 = -j;
                if (j2 < MathUtils.LONG_POWERS_OF_TEN.length) {
                    int i2 = (int) j2;
                    if (this.bitLength + LONG_POWERS_OF_TEN_BIT_LENGTH[i2] < 64) {
                        return dividePrimitiveLongs(this.smallValue * MathUtils.LONG_POWERS_OF_TEN[i2], bigDecimal.smallValue, i, roundingMode);
                    }
                }
            } else if (j < MathUtils.LONG_POWERS_OF_TEN.length) {
                int i3 = (int) j;
                if (bigDecimal.bitLength + LONG_POWERS_OF_TEN_BIT_LENGTH[i3] < 64) {
                    return dividePrimitiveLongs(this.smallValue, MathUtils.LONG_POWERS_OF_TEN[i3] * bigDecimal.smallValue, i, roundingMode);
                }
            }
        }
        BigInteger unscaledValue = getUnscaledValue();
        BigInteger unscaledValue2 = bigDecimal.getUnscaledValue();
        if (j > 0) {
            unscaledValue2 = Multiplication.multiplyByTenPow(unscaledValue2, (int) j);
        } else if (j < 0) {
            unscaledValue = Multiplication.multiplyByTenPow(unscaledValue, (int) (-j));
        }
        return divideBigIntegers(unscaledValue, unscaledValue2, i, roundingMode);
    }

    private static BigDecimal divideBigIntegers(BigInteger bigInteger, BigInteger bigInteger2, int i, RoundingMode roundingMode) {
        int iRoundingBehavior;
        BigInteger[] bigIntegerArrDivideAndRemainder = bigInteger.divideAndRemainder(bigInteger2);
        BigInteger bigInteger3 = bigIntegerArrDivideAndRemainder[0];
        BigInteger bigInteger4 = bigIntegerArrDivideAndRemainder[1];
        if (bigInteger4.signum() == 0) {
            return new BigDecimal(bigInteger3, i);
        }
        int iSignum = bigInteger.signum() * bigInteger2.signum();
        if (bigInteger2.bitLength() < 63) {
            iRoundingBehavior = roundingBehavior(bigInteger3.testBit(0) ? 1 : 0, iSignum * (5 + compareForRounding(bigInteger4.longValue(), bigInteger2.longValue())), roundingMode);
        } else {
            iRoundingBehavior = roundingBehavior(bigInteger3.testBit(0) ? 1 : 0, iSignum * (5 + bigInteger4.abs().shiftLeftOneBit().compareTo(bigInteger2.abs())), roundingMode);
        }
        if (iRoundingBehavior != 0) {
            if (bigInteger3.bitLength() < 63) {
                return valueOf(bigInteger3.longValue() + ((long) iRoundingBehavior), i);
            }
            return new BigDecimal(bigInteger3.add(BigInteger.valueOf(iRoundingBehavior)), i);
        }
        return new BigDecimal(bigInteger3, i);
    }

    private static BigDecimal dividePrimitiveLongs(long j, long j2, int i, RoundingMode roundingMode) {
        long jRoundingBehavior = j / j2;
        long j3 = j % j2;
        int iSignum = Long.signum(j) * Long.signum(j2);
        if (j3 != 0) {
            jRoundingBehavior += (long) roundingBehavior(((int) jRoundingBehavior) & 1, iSignum * (5 + compareForRounding(j3, j2)), roundingMode);
        }
        return valueOf(jRoundingBehavior, i);
    }

    public BigDecimal divide(BigDecimal bigDecimal, int i) {
        return divide(bigDecimal, this.scale, RoundingMode.valueOf(i));
    }

    public BigDecimal divide(BigDecimal bigDecimal, RoundingMode roundingMode) {
        return divide(bigDecimal, this.scale, roundingMode);
    }

    public BigDecimal divide(BigDecimal bigDecimal) {
        BigInteger unscaledValue = getUnscaledValue();
        BigInteger unscaledValue2 = bigDecimal.getUnscaledValue();
        long j = ((long) this.scale) - ((long) bigDecimal.scale);
        int length = FIVE_POW.length - 1;
        if (bigDecimal.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if (unscaledValue.signum() == 0) {
            return zeroScaledBy(j);
        }
        BigInteger bigIntegerGcd = unscaledValue.gcd(unscaledValue2);
        BigInteger bigIntegerDivide = unscaledValue.divide(bigIntegerGcd);
        BigInteger bigIntegerDivide2 = unscaledValue2.divide(bigIntegerGcd);
        int lowestSetBit = bigIntegerDivide2.getLowestSetBit();
        BigInteger bigIntegerShiftRight = bigIntegerDivide2.shiftRight(lowestSetBit);
        int i = 1;
        int i2 = 0;
        while (true) {
            BigInteger[] bigIntegerArrDivideAndRemainder = bigIntegerShiftRight.divideAndRemainder(FIVE_POW[i]);
            if (bigIntegerArrDivideAndRemainder[1].signum() == 0) {
                i2 += i;
                if (i < length) {
                    i++;
                }
                bigIntegerShiftRight = bigIntegerArrDivideAndRemainder[0];
            } else {
                if (i == 1) {
                    break;
                }
                i = 1;
            }
        }
        if (!bigIntegerShiftRight.abs().equals(BigInteger.ONE)) {
            throw new ArithmeticException("Non-terminating decimal expansion; no exact representable decimal result");
        }
        if (bigIntegerShiftRight.signum() < 0) {
            bigIntegerDivide = bigIntegerDivide.negate();
        }
        int iSafeLongToInt = safeLongToInt(j + ((long) Math.max(lowestSetBit, i2)));
        int i3 = lowestSetBit - i2;
        return new BigDecimal(i3 > 0 ? Multiplication.multiplyByFivePow(bigIntegerDivide, i3) : bigIntegerDivide.shiftLeft(-i3), iSafeLongToInt);
    }

    public BigDecimal divide(BigDecimal bigDecimal, MathContext mathContext) {
        long j;
        int i;
        BigInteger bigIntegerAdd;
        long precision = ((((long) mathContext.getPrecision()) + 2) + ((long) bigDecimal.approxPrecision())) - ((long) approxPrecision());
        long j2 = ((long) this.scale) - ((long) bigDecimal.scale);
        int length = TEN_POW.length - 1;
        BigInteger[] bigIntegerArr = {getUnscaledValue()};
        if (mathContext.getPrecision() == 0 || isZero() || bigDecimal.isZero()) {
            return divide(bigDecimal);
        }
        if (precision > 0) {
            bigIntegerArr[0] = getUnscaledValue().multiply(Multiplication.powerOf10(precision));
            j = precision + j2;
        } else {
            j = j2;
        }
        BigInteger[] bigIntegerArrDivideAndRemainder = bigIntegerArr[0].divideAndRemainder(bigDecimal.getUnscaledValue());
        BigInteger bigInteger = bigIntegerArrDivideAndRemainder[0];
        if (bigIntegerArrDivideAndRemainder[1].signum() != 0) {
            bigIntegerAdd = bigInteger.multiply(BigInteger.TEN).add(BigInteger.valueOf(bigIntegerArrDivideAndRemainder[0].signum() * (5 + bigIntegerArrDivideAndRemainder[1].shiftLeftOneBit().compareTo(bigDecimal.getUnscaledValue()))));
            j++;
        } else {
            loop0: do {
                i = 1;
                while (!bigInteger.testBit(0)) {
                    BigInteger[] bigIntegerArrDivideAndRemainder2 = bigInteger.divideAndRemainder(TEN_POW[i]);
                    if (bigIntegerArrDivideAndRemainder2[1].signum() == 0) {
                        long j3 = j - ((long) i);
                        if (j3 >= j2) {
                            if (i < length) {
                                i++;
                            }
                            bigInteger = bigIntegerArrDivideAndRemainder2[0];
                            j = j3;
                        }
                    }
                }
                break loop0;
            } while (i != 1);
            bigIntegerAdd = bigInteger;
        }
        return new BigDecimal(bigIntegerAdd, safeLongToInt(j), mathContext);
    }

    public BigDecimal divideToIntegralValue(BigDecimal bigDecimal) {
        BigInteger bigIntegerDivide;
        long j = ((long) this.scale) - ((long) bigDecimal.scale);
        int length = TEN_POW.length - 1;
        if (bigDecimal.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if (((long) bigDecimal.approxPrecision()) + j > ((long) approxPrecision()) + 1 || isZero()) {
            bigIntegerDivide = BigInteger.ZERO;
        } else if (j == 0) {
            bigIntegerDivide = getUnscaledValue().divide(bigDecimal.getUnscaledValue());
        } else if (j > 0) {
            BigInteger bigIntegerPowerOf10 = Multiplication.powerOf10(j);
            bigIntegerDivide = getUnscaledValue().divide(bigDecimal.getUnscaledValue().multiply(bigIntegerPowerOf10)).multiply(bigIntegerPowerOf10);
        } else {
            bigIntegerDivide = getUnscaledValue().multiply(Multiplication.powerOf10(-j)).divide(bigDecimal.getUnscaledValue());
            long j2 = 0;
            int i = 1;
            while (!bigIntegerDivide.testBit(0)) {
                BigInteger[] bigIntegerArrDivideAndRemainder = bigIntegerDivide.divideAndRemainder(TEN_POW[i]);
                if (bigIntegerArrDivideAndRemainder[1].signum() == 0) {
                    long j3 = j2 - ((long) i);
                    if (j3 >= j) {
                        if (i < length) {
                            i++;
                        }
                        bigIntegerDivide = bigIntegerArrDivideAndRemainder[0];
                        j2 = j3;
                    }
                }
                if (i == 1) {
                    break;
                }
                i = 1;
            }
            j = j2;
        }
        if (bigIntegerDivide.signum() == 0) {
            return zeroScaledBy(j);
        }
        return new BigDecimal(bigIntegerDivide, safeLongToInt(j));
    }

    public BigDecimal divideToIntegralValue(BigDecimal bigDecimal, MathContext mathContext) {
        long jMin;
        BigDecimal bigDecimal2;
        long j;
        int precision = mathContext.getPrecision();
        int iPrecision = precision() - bigDecimal.precision();
        int length = TEN_POW.length - 1;
        long j2 = ((long) this.scale) - ((long) bigDecimal.scale);
        long j3 = iPrecision;
        long j4 = (j3 - j2) + 1;
        BigInteger[] bigIntegerArrDivideAndRemainder = new BigInteger[2];
        if (precision == 0 || isZero() || bigDecimal.isZero()) {
            return divideToIntegralValue(bigDecimal);
        }
        if (j4 <= 0) {
            bigIntegerArrDivideAndRemainder[0] = BigInteger.ZERO;
        } else if (j2 == 0) {
            bigIntegerArrDivideAndRemainder[0] = getUnscaledValue().divide(bigDecimal.getUnscaledValue());
        } else {
            if (j2 <= 0) {
                long jMin2 = Math.min(-j2, Math.max(((long) precision) - j3, 0L));
                bigIntegerArrDivideAndRemainder = getUnscaledValue().multiply(Multiplication.powerOf10(jMin2)).divideAndRemainder(bigDecimal.getUnscaledValue());
                long j5 = jMin2 + j2;
                long j6 = -j5;
                if (bigIntegerArrDivideAndRemainder[1].signum() != 0 && j6 > 0) {
                    long jPrecision = (((long) new BigDecimal(bigIntegerArrDivideAndRemainder[1]).precision()) + j6) - ((long) bigDecimal.precision());
                    if (jPrecision == 0) {
                        bigIntegerArrDivideAndRemainder[1] = bigIntegerArrDivideAndRemainder[1].multiply(Multiplication.powerOf10(j6)).divide(bigDecimal.getUnscaledValue());
                        jPrecision = Math.abs(bigIntegerArrDivideAndRemainder[1].signum());
                    }
                    if (jPrecision > 0) {
                        throw new ArithmeticException("Division impossible");
                    }
                }
                jMin = j5;
            } else {
                bigIntegerArrDivideAndRemainder[0] = getUnscaledValue().divide(bigDecimal.getUnscaledValue().multiply(Multiplication.powerOf10(j2)));
                jMin = Math.min(j2, Math.max((((long) precision) - j4) + 1, 0L));
                bigIntegerArrDivideAndRemainder[0] = bigIntegerArrDivideAndRemainder[0].multiply(Multiplication.powerOf10(jMin));
            }
            int i = 0;
            if (bigIntegerArrDivideAndRemainder[0].signum() != 0) {
                return zeroScaledBy(j2);
            }
            BigInteger bigInteger = bigIntegerArrDivideAndRemainder[0];
            BigDecimal bigDecimal3 = new BigDecimal(bigIntegerArrDivideAndRemainder[0]);
            long jPrecision2 = bigDecimal3.precision();
            int i2 = 1;
            while (true) {
                if (!bigInteger.testBit(i)) {
                    BigInteger[] bigIntegerArrDivideAndRemainder2 = bigInteger.divideAndRemainder(TEN_POW[i2]);
                    if (bigIntegerArrDivideAndRemainder2[1].signum() == 0) {
                        long j7 = i2;
                        long j8 = jPrecision2 - j7;
                        bigDecimal2 = bigDecimal3;
                        j = jPrecision2;
                        if (j8 >= precision || jMin - j7 >= j2) {
                            jMin -= j7;
                            if (i2 < length) {
                                i2++;
                            }
                            bigInteger = bigIntegerArrDivideAndRemainder2[0];
                            i = 0;
                            jPrecision2 = j8;
                            bigDecimal3 = bigDecimal2;
                        }
                    } else {
                        bigDecimal2 = bigDecimal3;
                        j = jPrecision2;
                    }
                    if (i2 == 1) {
                        break;
                    }
                    i2 = 1;
                    i = 0;
                    bigDecimal3 = bigDecimal2;
                    jPrecision2 = j;
                } else {
                    bigDecimal2 = bigDecimal3;
                    j = jPrecision2;
                    break;
                }
            }
            if (j > precision) {
                throw new ArithmeticException("Division impossible");
            }
            BigDecimal bigDecimal4 = bigDecimal2;
            bigDecimal4.scale = safeLongToInt(jMin);
            bigDecimal4.setUnscaledValue(bigInteger);
            return bigDecimal4;
        }
        jMin = j2;
        int i3 = 0;
        if (bigIntegerArrDivideAndRemainder[0].signum() != 0) {
        }
    }

    public BigDecimal remainder(BigDecimal bigDecimal) {
        return divideAndRemainder(bigDecimal)[1];
    }

    public BigDecimal remainder(BigDecimal bigDecimal, MathContext mathContext) {
        return divideAndRemainder(bigDecimal, mathContext)[1];
    }

    public BigDecimal[] divideAndRemainder(BigDecimal bigDecimal) {
        BigDecimal[] bigDecimalArr = new BigDecimal[2];
        bigDecimalArr[0] = divideToIntegralValue(bigDecimal);
        bigDecimalArr[1] = subtract(bigDecimalArr[0].multiply(bigDecimal));
        return bigDecimalArr;
    }

    public BigDecimal[] divideAndRemainder(BigDecimal bigDecimal, MathContext mathContext) {
        BigDecimal[] bigDecimalArr = new BigDecimal[2];
        bigDecimalArr[0] = divideToIntegralValue(bigDecimal, mathContext);
        bigDecimalArr[1] = subtract(bigDecimalArr[0].multiply(bigDecimal));
        return bigDecimalArr;
    }

    public BigDecimal pow(int i) {
        if (i == 0) {
            return ONE;
        }
        if (i < 0 || i > 999999999) {
            throw new ArithmeticException("Invalid operation");
        }
        long j = ((long) this.scale) * ((long) i);
        return isZero() ? zeroScaledBy(j) : new BigDecimal(getUnscaledValue().pow(i), safeLongToInt(j));
    }

    public BigDecimal pow(int i, MathContext mathContext) {
        MathContext mathContext2;
        int iAbs = Math.abs(i);
        int precision = mathContext.getPrecision();
        int iLog10 = ((int) Math.log10(iAbs)) + 1;
        if (i == 0 || (isZero() && i > 0)) {
            return pow(i);
        }
        if (iAbs > 999999999 || ((precision == 0 && i < 0) || (precision > 0 && iLog10 > precision))) {
            throw new ArithmeticException("Invalid operation");
        }
        if (precision > 0) {
            mathContext2 = new MathContext(precision + iLog10 + 1, mathContext.getRoundingMode());
        } else {
            mathContext2 = mathContext;
        }
        BigDecimal bigDecimalRound = round(mathContext2);
        for (int iHighestOneBit = Integer.highestOneBit(iAbs) >> 1; iHighestOneBit > 0; iHighestOneBit >>= 1) {
            bigDecimalRound = bigDecimalRound.multiply(bigDecimalRound, mathContext2);
            if ((iAbs & iHighestOneBit) == iHighestOneBit) {
                bigDecimalRound = bigDecimalRound.multiply(this, mathContext2);
            }
        }
        if (i < 0) {
            bigDecimalRound = ONE.divide(bigDecimalRound, mathContext2);
        }
        bigDecimalRound.inplaceRound(mathContext);
        return bigDecimalRound;
    }

    public BigDecimal abs() {
        return signum() < 0 ? negate() : this;
    }

    public BigDecimal abs(MathContext mathContext) {
        BigDecimal bigDecimalNegate = signum() < 0 ? negate() : new BigDecimal(getUnscaledValue(), this.scale);
        bigDecimalNegate.inplaceRound(mathContext);
        return bigDecimalNegate;
    }

    public BigDecimal negate() {
        if (this.bitLength < 63 || (this.bitLength == 63 && this.smallValue != Long.MIN_VALUE)) {
            return valueOf(-this.smallValue, this.scale);
        }
        return new BigDecimal(getUnscaledValue().negate(), this.scale);
    }

    public BigDecimal negate(MathContext mathContext) {
        BigDecimal bigDecimalNegate = negate();
        bigDecimalNegate.inplaceRound(mathContext);
        return bigDecimalNegate;
    }

    public BigDecimal plus() {
        return this;
    }

    public BigDecimal plus(MathContext mathContext) {
        return round(mathContext);
    }

    public int signum() {
        if (this.bitLength < 64) {
            return Long.signum(this.smallValue);
        }
        return getUnscaledValue().signum();
    }

    private boolean isZero() {
        return this.bitLength == 0 && this.smallValue != -1;
    }

    public int scale() {
        return this.scale;
    }

    public int precision() {
        if (this.precision != 0) {
            return this.precision;
        }
        if (this.bitLength == 0) {
            this.precision = 1;
        } else if (this.bitLength < 64) {
            this.precision = decimalDigitsInLong(this.smallValue);
        } else {
            int i = 1 + ((int) (((double) (this.bitLength - 1)) * LOG10_2));
            if (getUnscaledValue().divide(Multiplication.powerOf10(i)).signum() != 0) {
                i++;
            }
            this.precision = i;
        }
        return this.precision;
    }

    private int decimalDigitsInLong(long j) {
        if (j == Long.MIN_VALUE) {
            return 19;
        }
        int iBinarySearch = Arrays.binarySearch(MathUtils.LONG_POWERS_OF_TEN, Math.abs(j));
        return iBinarySearch < 0 ? (-iBinarySearch) - 1 : iBinarySearch + 1;
    }

    public BigInteger unscaledValue() {
        return getUnscaledValue();
    }

    public BigDecimal round(MathContext mathContext) {
        BigDecimal bigDecimal = new BigDecimal(getUnscaledValue(), this.scale);
        bigDecimal.inplaceRound(mathContext);
        return bigDecimal;
    }

    public BigDecimal setScale(int i, RoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException("roundingMode == null");
        }
        long j = ((long) i) - ((long) this.scale);
        if (j == 0) {
            return this;
        }
        if (j <= 0) {
            if (this.bitLength < 64) {
                long j2 = -j;
                if (j2 < MathUtils.LONG_POWERS_OF_TEN.length) {
                    return dividePrimitiveLongs(this.smallValue, MathUtils.LONG_POWERS_OF_TEN[(int) j2], i, roundingMode);
                }
            }
            return divideBigIntegers(getUnscaledValue(), Multiplication.powerOf10(-j), i, roundingMode);
        }
        if (j < MathUtils.LONG_POWERS_OF_TEN.length) {
            int i2 = (int) j;
            if (this.bitLength + LONG_POWERS_OF_TEN_BIT_LENGTH[i2] < 64) {
                return valueOf(this.smallValue * MathUtils.LONG_POWERS_OF_TEN[i2], i);
            }
        }
        return new BigDecimal(Multiplication.multiplyByTenPow(getUnscaledValue(), (int) j), i);
    }

    public BigDecimal setScale(int i, int i2) {
        return setScale(i, RoundingMode.valueOf(i2));
    }

    public BigDecimal setScale(int i) {
        return setScale(i, RoundingMode.UNNECESSARY);
    }

    public BigDecimal movePointLeft(int i) {
        return movePoint(((long) this.scale) + ((long) i));
    }

    private BigDecimal movePoint(long j) {
        if (isZero()) {
            return zeroScaledBy(Math.max(j, 0L));
        }
        if (j >= 0) {
            if (this.bitLength < 64) {
                return valueOf(this.smallValue, safeLongToInt(j));
            }
            return new BigDecimal(getUnscaledValue(), safeLongToInt(j));
        }
        long j2 = -j;
        if (j2 < MathUtils.LONG_POWERS_OF_TEN.length) {
            int i = (int) j2;
            if (this.bitLength + LONG_POWERS_OF_TEN_BIT_LENGTH[i] < 64) {
                return valueOf(this.smallValue * MathUtils.LONG_POWERS_OF_TEN[i], 0);
            }
        }
        return new BigDecimal(Multiplication.multiplyByTenPow(getUnscaledValue(), safeLongToInt(j2)), 0);
    }

    public BigDecimal movePointRight(int i) {
        return movePoint(((long) this.scale) - ((long) i));
    }

    public BigDecimal scaleByPowerOfTen(int i) {
        long j = ((long) this.scale) - ((long) i);
        if (this.bitLength >= 64) {
            return new BigDecimal(getUnscaledValue(), safeLongToInt(j));
        }
        if (this.smallValue == 0) {
            return zeroScaledBy(j);
        }
        return valueOf(this.smallValue, safeLongToInt(j));
    }

    public BigDecimal stripTrailingZeros() {
        int i;
        int length = TEN_POW.length - 1;
        long j = this.scale;
        if (isZero()) {
            return this;
        }
        BigInteger unscaledValue = getUnscaledValue();
        long j2 = j;
        loop0: do {
            i = 1;
            while (true) {
                if (!unscaledValue.testBit(0)) {
                    BigInteger[] bigIntegerArrDivideAndRemainder = unscaledValue.divideAndRemainder(TEN_POW[i]);
                    if (bigIntegerArrDivideAndRemainder[1].signum() != 0) {
                        break;
                    }
                    j2 -= (long) i;
                    if (i < length) {
                        i++;
                    }
                    unscaledValue = bigIntegerArrDivideAndRemainder[0];
                } else {
                    break loop0;
                }
            }
        } while (i != 1);
        return new BigDecimal(unscaledValue, safeLongToInt(j2));
    }

    @Override
    public int compareTo(BigDecimal bigDecimal) {
        int iSignum = signum();
        int iSignum2 = bigDecimal.signum();
        if (iSignum != iSignum2) {
            return iSignum < iSignum2 ? -1 : 1;
        }
        if (this.scale == bigDecimal.scale && this.bitLength < 64 && bigDecimal.bitLength < 64) {
            if (this.smallValue < bigDecimal.smallValue) {
                return -1;
            }
            return this.smallValue > bigDecimal.smallValue ? 1 : 0;
        }
        long j = ((long) this.scale) - ((long) bigDecimal.scale);
        long jApproxPrecision = approxPrecision() - bigDecimal.approxPrecision();
        if (jApproxPrecision > j + 1) {
            return iSignum;
        }
        if (jApproxPrecision < j - 1) {
            return -iSignum;
        }
        BigInteger unscaledValue = getUnscaledValue();
        BigInteger unscaledValue2 = bigDecimal.getUnscaledValue();
        if (j < 0) {
            unscaledValue = unscaledValue.multiply(Multiplication.powerOf10(-j));
        } else if (j > 0) {
            unscaledValue2 = unscaledValue2.multiply(Multiplication.powerOf10(j));
        }
        return unscaledValue.compareTo(unscaledValue2);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BigDecimal)) {
            return false;
        }
        BigDecimal bigDecimal = (BigDecimal) obj;
        if (bigDecimal.scale == this.scale && bigDecimal.bitLength == this.bitLength) {
            if (this.bitLength < 64) {
                if (bigDecimal.smallValue == this.smallValue) {
                    return true;
                }
            } else if (bigDecimal.intVal.equals(this.intVal)) {
                return true;
            }
        }
        return false;
    }

    public BigDecimal min(BigDecimal bigDecimal) {
        return compareTo(bigDecimal) <= 0 ? this : bigDecimal;
    }

    public BigDecimal max(BigDecimal bigDecimal) {
        return compareTo(bigDecimal) >= 0 ? this : bigDecimal;
    }

    public int hashCode() {
        if (this.hashCode != 0) {
            return this.hashCode;
        }
        if (this.bitLength < 64) {
            this.hashCode = (int) (this.smallValue & (-1));
            this.hashCode = (33 * this.hashCode) + ((int) ((-1) & (this.smallValue >> 32)));
            this.hashCode = (17 * this.hashCode) + this.scale;
            return this.hashCode;
        }
        this.hashCode = (17 * this.intVal.hashCode()) + this.scale;
        return this.hashCode;
    }

    public String toString() {
        if (this.toStringImage != null) {
            return this.toStringImage;
        }
        if (this.bitLength < 32) {
            this.toStringImage = Conversion.toDecimalScaledString(this.smallValue, this.scale);
            return this.toStringImage;
        }
        String string = getUnscaledValue().toString();
        if (this.scale == 0) {
            return string;
        }
        int i = getUnscaledValue().signum() < 0 ? 2 : 1;
        int length = string.length();
        long j = ((-this.scale) + ((long) length)) - ((long) i);
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        if (this.scale <= 0 || j < -6) {
            if (length - i >= 1) {
                sb.insert(i, '.');
                length++;
            }
            sb.insert(length, 'E');
            if (j > 0) {
                length++;
                sb.insert(length, '+');
            }
            sb.insert(length + 1, Long.toString(j));
        } else if (j >= 0) {
            sb.insert(length - this.scale, '.');
        } else {
            sb.insert(i - 1, "0.");
            sb.insert(i + 1, CH_ZEROS, 0, (-((int) j)) - 1);
        }
        this.toStringImage = sb.toString();
        return this.toStringImage;
    }

    public String toEngineeringString() {
        String string = getUnscaledValue().toString();
        if (this.scale == 0) {
            return string;
        }
        int i = getUnscaledValue().signum() < 0 ? 2 : 1;
        int length = string.length();
        long j = ((-this.scale) + ((long) length)) - ((long) i);
        StringBuilder sb = new StringBuilder(string);
        if (this.scale <= 0 || j < -6) {
            int i2 = length - i;
            int i3 = (int) (j % 3);
            if (i3 != 0) {
                if (getUnscaledValue().signum() == 0) {
                    i3 = i3 < 0 ? -i3 : 3 - i3;
                    j += (long) i3;
                } else {
                    if (i3 < 0) {
                        i3 += 3;
                    }
                    j -= (long) i3;
                    i += i3;
                }
                if (i2 < 3) {
                    int i4 = i3 - i2;
                    while (i4 > 0) {
                        sb.insert(length, '0');
                        i4--;
                        length++;
                    }
                }
            }
            if (length - i >= 1) {
                sb.insert(i, '.');
                length++;
            }
            if (j != 0) {
                sb.insert(length, 'E');
                if (j > 0) {
                    length++;
                    sb.insert(length, '+');
                }
                sb.insert(length + 1, Long.toString(j));
            }
        } else if (j >= 0) {
            sb.insert(length - this.scale, '.');
        } else {
            sb.insert(i - 1, "0.");
            sb.insert(i + 1, CH_ZEROS, 0, (-((int) j)) - 1);
        }
        return sb.toString();
    }

    public String toPlainString() {
        String string = getUnscaledValue().toString();
        if (this.scale == 0 || (isZero() && this.scale < 0)) {
            return string;
        }
        int i = signum() < 0 ? 1 : 0;
        int length = this.scale;
        StringBuilder sb = new StringBuilder(string.length() + 1 + Math.abs(this.scale));
        if (i == 1) {
            sb.append('-');
        }
        if (this.scale > 0) {
            int length2 = length - (string.length() - i);
            if (length2 >= 0) {
                sb.append("0.");
                while (length2 > CH_ZEROS.length) {
                    sb.append(CH_ZEROS);
                    length2 -= CH_ZEROS.length;
                }
                sb.append(CH_ZEROS, 0, length2);
                sb.append(string.substring(i));
            } else {
                int i2 = i - length2;
                sb.append(string.substring(i, i2));
                sb.append('.');
                sb.append(string.substring(i2));
            }
        } else {
            sb.append(string.substring(i));
            while (length < (-CH_ZEROS.length)) {
                sb.append(CH_ZEROS);
                length += CH_ZEROS.length;
            }
            sb.append(CH_ZEROS, 0, -length);
        }
        return sb.toString();
    }

    public BigInteger toBigInteger() {
        if (this.scale == 0 || isZero()) {
            return getUnscaledValue();
        }
        if (this.scale < 0) {
            return getUnscaledValue().multiply(Multiplication.powerOf10(-this.scale));
        }
        return getUnscaledValue().divide(Multiplication.powerOf10(this.scale));
    }

    public BigInteger toBigIntegerExact() {
        if (this.scale == 0 || isZero()) {
            return getUnscaledValue();
        }
        if (this.scale < 0) {
            return getUnscaledValue().multiply(Multiplication.powerOf10(-this.scale));
        }
        if (this.scale > approxPrecision() || this.scale > getUnscaledValue().getLowestSetBit()) {
            throw new ArithmeticException("Rounding necessary");
        }
        BigInteger[] bigIntegerArrDivideAndRemainder = getUnscaledValue().divideAndRemainder(Multiplication.powerOf10(this.scale));
        if (bigIntegerArrDivideAndRemainder[1].signum() != 0) {
            throw new ArithmeticException("Rounding necessary");
        }
        return bigIntegerArrDivideAndRemainder[0];
    }

    @Override
    public long longValue() {
        if (this.scale <= -64 || this.scale > approxPrecision()) {
            return 0L;
        }
        return toBigInteger().longValue();
    }

    public long longValueExact() {
        return valueExact(64);
    }

    @Override
    public int intValue() {
        if (this.scale <= -32 || this.scale > approxPrecision()) {
            return 0;
        }
        return toBigInteger().intValue();
    }

    public int intValueExact() {
        return (int) valueExact(32);
    }

    public short shortValueExact() {
        return (short) valueExact(16);
    }

    public byte byteValueExact() {
        return (byte) valueExact(8);
    }

    @Override
    public float floatValue() {
        float fSignum = signum();
        long j = ((long) this.bitLength) - ((long) (((double) this.scale) / LOG10_2));
        if (j < -149 || fSignum == 0.0f) {
            return fSignum * 0.0f;
        }
        if (j > 129) {
            return fSignum * Float.POSITIVE_INFINITY;
        }
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        BigInteger bigIntegerAdd;
        long jLongValue;
        long j;
        long j2;
        int i;
        int iSignum = signum();
        long j3 = ((long) this.bitLength) - ((long) (((double) this.scale) / LOG10_2));
        if (j3 < -1074 || iSignum == 0) {
            return ((double) iSignum) * 0.0d;
        }
        if (j3 > 1025) {
            return ((double) iSignum) * Double.POSITIVE_INFINITY;
        }
        BigInteger bigIntegerAbs = getUnscaledValue().abs();
        int i2 = 1076;
        if (this.scale <= 0) {
            bigIntegerAdd = bigIntegerAbs.multiply(Multiplication.powerOf10(-this.scale));
        } else {
            BigInteger bigIntegerPowerOf10 = Multiplication.powerOf10(this.scale);
            int i3 = 100 - ((int) j3);
            if (i3 > 0) {
                bigIntegerAbs = bigIntegerAbs.shiftLeft(i3);
                i2 = 1076 - i3;
            }
            BigInteger[] bigIntegerArrDivideAndRemainder = bigIntegerAbs.divideAndRemainder(bigIntegerPowerOf10);
            int iCompareTo2 = bigIntegerArrDivideAndRemainder[1].shiftLeftOneBit().compareTo(bigIntegerPowerOf10);
            bigIntegerAdd = bigIntegerArrDivideAndRemainder[0].shiftLeft(2).add(BigInteger.valueOf(((iCompareTo2 * (iCompareTo2 + 3)) / 2) + 1));
            i2 -= 2;
        }
        int lowestSetBit = bigIntegerAdd.getLowestSetBit();
        int iBitLength = bigIntegerAdd.bitLength() - 54;
        if (iBitLength > 0) {
            jLongValue = bigIntegerAdd.shiftRight(iBitLength).longValue();
            if (((jLongValue & 1) == 1 && lowestSetBit < iBitLength) || (jLongValue & 3) == 3) {
                j = jLongValue + 2;
            } else {
                j = jLongValue;
            }
        } else {
            jLongValue = bigIntegerAdd.longValue() << (-iBitLength);
            if ((jLongValue & 3) == 3) {
                j = jLongValue + 2;
            }
        }
        if ((j & 18014398509481984L) == 0) {
            j2 = j >> 1;
            i = i2 + iBitLength;
        } else {
            j2 = j >> 2;
            i = i2 + iBitLength + 1;
        }
        if (i > 2046) {
            return ((double) iSignum) * Double.POSITIVE_INFINITY;
        }
        if (i <= 0) {
            if (i < -53) {
                return ((double) iSignum) * 0.0d;
            }
            long j4 = jLongValue >> 1;
            long j5 = ((-1) >>> (63 + i)) & j4;
            long j6 = j4 >> (-i);
            if ((j6 & 3) == 3 || ((j6 & 1) == 1 && j5 != 0 && lowestSetBit < iBitLength)) {
                j6++;
            }
            j2 = j6 >> 1;
            i = 0;
        }
        return Double.longBitsToDouble((((long) iSignum) & Long.MIN_VALUE) | (((long) i) << 52) | (4503599627370495L & j2));
    }

    public BigDecimal ulp() {
        return valueOf(1L, this.scale);
    }

    private void inplaceRound(MathContext mathContext) {
        int iPrecision;
        int precision = mathContext.getPrecision();
        if (approxPrecision() < precision || precision == 0 || (iPrecision = precision() - precision) <= 0) {
            return;
        }
        if (this.bitLength < 64) {
            smallRound(mathContext, iPrecision);
            return;
        }
        long j = iPrecision;
        BigInteger bigIntegerPowerOf10 = Multiplication.powerOf10(j);
        BigInteger[] bigIntegerArrDivideAndRemainder = getUnscaledValue().divideAndRemainder(bigIntegerPowerOf10);
        long j2 = ((long) this.scale) - j;
        if (bigIntegerArrDivideAndRemainder[1].signum() != 0) {
            int iRoundingBehavior = roundingBehavior(bigIntegerArrDivideAndRemainder[0].testBit(0) ? 1 : 0, bigIntegerArrDivideAndRemainder[1].signum() * (5 + bigIntegerArrDivideAndRemainder[1].abs().shiftLeftOneBit().compareTo(bigIntegerPowerOf10)), mathContext.getRoundingMode());
            if (iRoundingBehavior != 0) {
                bigIntegerArrDivideAndRemainder[0] = bigIntegerArrDivideAndRemainder[0].add(BigInteger.valueOf(iRoundingBehavior));
            }
            if (new BigDecimal(bigIntegerArrDivideAndRemainder[0]).precision() > precision) {
                bigIntegerArrDivideAndRemainder[0] = bigIntegerArrDivideAndRemainder[0].divide(BigInteger.TEN);
                j2--;
            }
        }
        this.scale = safeLongToInt(j2);
        this.precision = precision;
        setUnscaledValue(bigIntegerArrDivideAndRemainder[0]);
    }

    private static int compareAbsoluteValues(long j, long j2) {
        long jAbs = Math.abs(j) - 1;
        long jAbs2 = Math.abs(j2) - 1;
        if (jAbs > jAbs2) {
            return 1;
        }
        return jAbs < jAbs2 ? -1 : 0;
    }

    private static int compareForRounding(long j, long j2) {
        long j3 = j2 / 2;
        if (j == j3 || j == (-j3)) {
            return -(((int) j2) & 1);
        }
        return compareAbsoluteValues(j, j3);
    }

    private void smallRound(MathContext mathContext, int i) {
        long j = MathUtils.LONG_POWERS_OF_TEN[i];
        long j2 = ((long) this.scale) - ((long) i);
        long j3 = this.smallValue;
        long jRoundingBehavior = j3 / j;
        long j4 = j3 % j;
        if (j4 != 0) {
            jRoundingBehavior += (long) roundingBehavior(((int) jRoundingBehavior) & 1, Long.signum(j4) * (5 + compareForRounding(j4, j)), mathContext.getRoundingMode());
            if (Math.log10(Math.abs(jRoundingBehavior)) >= mathContext.getPrecision()) {
                jRoundingBehavior /= 10;
                j2--;
            }
        }
        this.scale = safeLongToInt(j2);
        this.precision = mathContext.getPrecision();
        this.smallValue = jRoundingBehavior;
        this.bitLength = bitLength(jRoundingBehavior);
        this.intVal = null;
    }

    private static int roundingBehavior(int i, int i2, RoundingMode roundingMode) {
        switch (roundingMode) {
            case UNNECESSARY:
                if (i2 == 0) {
                    return 0;
                }
                throw new ArithmeticException("Rounding necessary");
            case UP:
                return Integer.signum(i2);
            case DOWN:
            default:
                return 0;
            case CEILING:
                return Math.max(Integer.signum(i2), 0);
            case FLOOR:
                return Math.min(Integer.signum(i2), 0);
            case HALF_UP:
                if (Math.abs(i2) >= 5) {
                    return Integer.signum(i2);
                }
                return 0;
            case HALF_DOWN:
                if (Math.abs(i2) > 5) {
                    return Integer.signum(i2);
                }
                return 0;
            case HALF_EVEN:
                if (Math.abs(i2) + i > 5) {
                    return Integer.signum(i2);
                }
                return 0;
        }
    }

    private long valueExact(int i) {
        BigInteger bigIntegerExact = toBigIntegerExact();
        if (bigIntegerExact.bitLength() < i) {
            return bigIntegerExact.longValue();
        }
        throw new ArithmeticException("Rounding necessary");
    }

    private int approxPrecision() {
        if (this.precision > 0) {
            return this.precision;
        }
        return ((int) (((double) (this.bitLength - 1)) * LOG10_2)) + 1;
    }

    private static int safeLongToInt(long j) {
        if (j < -2147483648L || j > 2147483647L) {
            throw new ArithmeticException("Out of int range: " + j);
        }
        return (int) j;
    }

    private static BigDecimal zeroScaledBy(long j) {
        int i = (int) j;
        if (j == i) {
            return valueOf(0L, i);
        }
        if (j >= 0) {
            return new BigDecimal(0, Integer.MAX_VALUE);
        }
        return new BigDecimal(0, Integer.MIN_VALUE);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.bitLength = this.intVal.bitLength();
        if (this.bitLength < 64) {
            this.smallValue = this.intVal.longValue();
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        getUnscaledValue();
        objectOutputStream.defaultWriteObject();
    }

    private BigInteger getUnscaledValue() {
        if (this.intVal == null) {
            this.intVal = BigInteger.valueOf(this.smallValue);
        }
        return this.intVal;
    }

    private void setUnscaledValue(BigInteger bigInteger) {
        this.intVal = bigInteger;
        this.bitLength = bigInteger.bitLength();
        if (this.bitLength < 64) {
            this.smallValue = bigInteger.longValue();
        }
    }

    private static int bitLength(long j) {
        if (j < 0) {
            j = ~j;
        }
        return 64 - Long.numberOfLeadingZeros(j);
    }

    private static int bitLength(int i) {
        if (i < 0) {
            i = ~i;
        }
        return 32 - Integer.numberOfLeadingZeros(i);
    }
}
