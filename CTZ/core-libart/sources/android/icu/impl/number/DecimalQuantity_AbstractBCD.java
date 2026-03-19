package android.icu.impl.number;

import android.icu.impl.StandardPlural;
import android.icu.text.PluralRules;
import android.icu.text.UFieldPosition;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.FieldPosition;

public abstract class DecimalQuantity_AbstractBCD implements DecimalQuantity {
    static final boolean $assertionsDisabled = false;
    private static final double[] DOUBLE_MULTIPLIERS = {1.0d, 10.0d, 100.0d, 1000.0d, 10000.0d, 100000.0d, 1000000.0d, 1.0E7d, 1.0E8d, 1.0E9d, 1.0E10d, 1.0E11d, 1.0E12d, 1.0E13d, 1.0E14d, 1.0E15d, 1.0E16d, 1.0E17d, 1.0E18d, 1.0E19d, 1.0E20d, 1.0E21d};
    protected static final int INFINITY_FLAG = 2;
    protected static final int NAN_FLAG = 4;
    protected static final int NEGATIVE_FLAG = 1;
    private static final int SECTION_LOWER_EDGE = -1;
    private static final int SECTION_UPPER_EDGE = -2;
    protected byte flags;
    protected boolean isApproximate;
    protected int origDelta;
    protected double origDouble;
    protected int precision;
    protected int scale;
    protected int lOptPos = Integer.MAX_VALUE;
    protected int lReqPos = 0;
    protected int rReqPos = 0;
    protected int rOptPos = Integer.MIN_VALUE;

    @Deprecated
    public boolean explicitExactDouble = false;

    protected abstract BigDecimal bcdToBigDecimal();

    protected abstract void compact();

    protected abstract void copyBcdFrom(DecimalQuantity decimalQuantity);

    protected abstract byte getDigitPos(int i);

    protected abstract void readBigIntegerToBcd(BigInteger bigInteger);

    protected abstract void readIntToBcd(int i);

    protected abstract void readLongToBcd(long j);

    protected abstract void setBcdToZero();

    protected abstract void setDigitPos(int i, byte b);

    protected abstract void shiftLeft(int i);

    protected abstract void shiftRight(int i);

    @Override
    public void copyFrom(DecimalQuantity decimalQuantity) {
        copyBcdFrom(decimalQuantity);
        DecimalQuantity_AbstractBCD decimalQuantity_AbstractBCD = (DecimalQuantity_AbstractBCD) decimalQuantity;
        this.lOptPos = decimalQuantity_AbstractBCD.lOptPos;
        this.lReqPos = decimalQuantity_AbstractBCD.lReqPos;
        this.rReqPos = decimalQuantity_AbstractBCD.rReqPos;
        this.rOptPos = decimalQuantity_AbstractBCD.rOptPos;
        this.scale = decimalQuantity_AbstractBCD.scale;
        this.precision = decimalQuantity_AbstractBCD.precision;
        this.flags = decimalQuantity_AbstractBCD.flags;
        this.origDouble = decimalQuantity_AbstractBCD.origDouble;
        this.origDelta = decimalQuantity_AbstractBCD.origDelta;
        this.isApproximate = decimalQuantity_AbstractBCD.isApproximate;
    }

    public DecimalQuantity_AbstractBCD clear() {
        this.lOptPos = Integer.MAX_VALUE;
        this.lReqPos = 0;
        this.rReqPos = 0;
        this.rOptPos = Integer.MIN_VALUE;
        this.flags = (byte) 0;
        setBcdToZero();
        return this;
    }

    @Override
    public void setIntegerLength(int i, int i2) {
        this.lOptPos = i2;
        this.lReqPos = i;
    }

    @Override
    public void setFractionLength(int i, int i2) {
        this.rReqPos = -i;
        this.rOptPos = -i2;
    }

    @Override
    public long getPositionFingerprint() {
        return (((((long) this.lOptPos) ^ 0) ^ ((long) (this.lReqPos << 16))) ^ (((long) this.rReqPos) << 32)) ^ (((long) this.rOptPos) << 48);
    }

    @Override
    public void roundToIncrement(BigDecimal bigDecimal, MathContext mathContext) {
        BigDecimal bigDecimalRound = toBigDecimal().divide(bigDecimal, 0, mathContext.getRoundingMode()).multiply(bigDecimal).round(mathContext);
        if (bigDecimalRound.signum() == 0) {
            setBcdToZero();
        } else {
            setToBigDecimal(bigDecimalRound);
        }
    }

    @Override
    public void multiplyBy(BigDecimal bigDecimal) {
        if (isInfinite() || isZero() || isNaN()) {
            return;
        }
        setToBigDecimal(toBigDecimal().multiply(bigDecimal));
    }

    @Override
    public int getMagnitude() throws ArithmeticException {
        if (this.precision == 0) {
            throw new ArithmeticException("Magnitude is not well-defined for zero");
        }
        return (this.scale + this.precision) - 1;
    }

    @Override
    public void adjustMagnitude(int i) {
        if (this.precision != 0) {
            this.scale += i;
            this.origDelta += i;
        }
    }

    @Override
    public StandardPlural getStandardPlural(PluralRules pluralRules) {
        if (pluralRules == null) {
            return StandardPlural.OTHER;
        }
        return StandardPlural.orOtherFromString(pluralRules.select(this));
    }

    @Override
    public double getPluralOperand(PluralRules.Operand operand) {
        switch (operand) {
            case i:
                return toLong();
            case f:
                return toFractionLong(true);
            case t:
                return toFractionLong(false);
            case v:
                return fractionCount();
            case w:
                return fractionCountWithoutTrailingZeros();
            default:
                return Math.abs(toDouble());
        }
    }

    @Override
    public void populateUFieldPosition(FieldPosition fieldPosition) {
        if (fieldPosition instanceof UFieldPosition) {
            ((UFieldPosition) fieldPosition).setFractionDigits((int) getPluralOperand(PluralRules.Operand.v), (long) getPluralOperand(PluralRules.Operand.f));
        }
    }

    @Override
    public int getUpperDisplayMagnitude() {
        int i = this.scale + this.precision;
        if (this.lReqPos > i) {
            i = this.lReqPos;
        } else if (this.lOptPos < i) {
            i = this.lOptPos;
        }
        return i - 1;
    }

    @Override
    public int getLowerDisplayMagnitude() {
        int i = this.scale;
        return this.rReqPos < i ? this.rReqPos : this.rOptPos > i ? this.rOptPos : i;
    }

    @Override
    public byte getDigit(int i) {
        return getDigitPos(i - this.scale);
    }

    private int fractionCount() {
        return -getLowerDisplayMagnitude();
    }

    private int fractionCountWithoutTrailingZeros() {
        return Math.max(-this.scale, 0);
    }

    @Override
    public boolean isNegative() {
        return (this.flags & 1) != 0;
    }

    @Override
    public boolean isInfinite() {
        return (this.flags & 2) != 0;
    }

    @Override
    public boolean isNaN() {
        return (this.flags & 4) != 0;
    }

    @Override
    public boolean isZero() {
        return this.precision == 0;
    }

    public void setToInt(int i) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (i < 0) {
            this.flags = (byte) (this.flags | 1);
            i = -i;
        }
        if (i != 0) {
            _setToInt(i);
            compact();
        }
    }

    private void _setToInt(int i) {
        if (i == Integer.MIN_VALUE) {
            readLongToBcd(-i);
        } else {
            readIntToBcd(i);
        }
    }

    public void setToLong(long j) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (j < 0) {
            this.flags = (byte) (this.flags | 1);
            j = -j;
        }
        if (j != 0) {
            _setToLong(j);
            compact();
        }
    }

    private void _setToLong(long j) {
        if (j == Long.MIN_VALUE) {
            readBigIntegerToBcd(BigInteger.valueOf(j).negate());
        } else if (j <= 2147483647L) {
            readIntToBcd((int) j);
        } else {
            readLongToBcd(j);
        }
    }

    public void setToBigInteger(BigInteger bigInteger) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (bigInteger.signum() == -1) {
            this.flags = (byte) (this.flags | 1);
            bigInteger = bigInteger.negate();
        }
        if (bigInteger.signum() != 0) {
            _setToBigInteger(bigInteger);
            compact();
        }
    }

    private void _setToBigInteger(BigInteger bigInteger) {
        if (bigInteger.bitLength() < 32) {
            readIntToBcd(bigInteger.intValue());
        } else if (bigInteger.bitLength() < 64) {
            readLongToBcd(bigInteger.longValue());
        } else {
            readBigIntegerToBcd(bigInteger);
        }
    }

    public void setToDouble(double d) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (Double.compare(d, 0.0d) < 0) {
            this.flags = (byte) (this.flags | 1);
            d = -d;
        }
        if (Double.isNaN(d)) {
            this.flags = (byte) (this.flags | 4);
            return;
        }
        if (Double.isInfinite(d)) {
            this.flags = (byte) (this.flags | 2);
        } else if (d != 0.0d) {
            _setToDoubleFast(d);
            compact();
        }
    }

    private void _setToDoubleFast(double d) {
        double d2;
        this.isApproximate = true;
        this.origDouble = d;
        this.origDelta = 0;
        int iDoubleToLongBits = ((int) ((Double.doubleToLongBits(d) & 9218868437227405312L) >> 52)) - 1023;
        if (iDoubleToLongBits <= 52) {
            long j = (long) d;
            if (j == d) {
                _setToLong(j);
                return;
            }
        }
        int i = (int) (((double) (52 - iDoubleToLongBits)) / 3.32192809489d);
        if (i >= 0) {
            double d3 = d;
            int i2 = i;
            while (i2 >= 22) {
                d3 *= 1.0E22d;
                i2 -= 22;
            }
            d2 = d3 * DOUBLE_MULTIPLIERS[i2];
        } else {
            double d4 = d;
            int i3 = i;
            while (i3 <= -22) {
                d4 /= 1.0E22d;
                i3 += 22;
            }
            d2 = d4 / DOUBLE_MULTIPLIERS[-i3];
        }
        long jRound = Math.round(d2);
        if (jRound != 0) {
            _setToLong(jRound);
            this.scale -= i;
        }
    }

    private void convertToAccurateDouble() {
        double d = this.origDouble;
        int i = this.origDelta;
        setBcdToZero();
        String string = Double.toString(d);
        if (string.indexOf(69) != -1) {
            int iIndexOf = string.indexOf(69);
            _setToLong(Long.parseLong(string.charAt(0) + string.substring(2, iIndexOf)));
            this.scale = this.scale + (Integer.parseInt(string.substring(iIndexOf + 1)) - (iIndexOf - 1)) + 1;
        } else if (string.charAt(0) == '0') {
            _setToLong(Long.parseLong(string.substring(2)));
            this.scale += 2 - string.length();
        } else if (string.charAt(string.length() - 1) == '0') {
            _setToLong(Long.parseLong(string.substring(0, string.length() - 2)));
        } else {
            int iIndexOf2 = string.indexOf(46);
            _setToLong(Long.parseLong(string.substring(0, iIndexOf2) + string.substring(iIndexOf2 + 1)));
            this.scale = this.scale + (iIndexOf2 - string.length()) + 1;
        }
        this.scale += i;
        compact();
        this.explicitExactDouble = true;
    }

    @Override
    public void setToBigDecimal(BigDecimal bigDecimal) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (bigDecimal.signum() == -1) {
            this.flags = (byte) (this.flags | 1);
            bigDecimal = bigDecimal.negate();
        }
        if (bigDecimal.signum() != 0) {
            _setToBigDecimal(bigDecimal);
            compact();
        }
    }

    private void _setToBigDecimal(BigDecimal bigDecimal) {
        int iScale = bigDecimal.scale();
        _setToBigInteger(bigDecimal.scaleByPowerOfTen(iScale).toBigInteger());
        this.scale -= iScale;
    }

    protected long toLong() {
        long digitPos = 0;
        for (int i = (this.scale + this.precision) - 1; i >= 0; i--) {
            digitPos = (digitPos * 10) + ((long) getDigitPos(i - this.scale));
        }
        return digitPos;
    }

    protected long toFractionLong(boolean z) {
        int i = -1;
        long digitPos = 0;
        while (true) {
            if ((i < this.scale && (!z || i < this.rReqPos)) || i < this.rOptPos) {
                break;
            }
            digitPos = (digitPos * 10) + ((long) getDigitPos(i - this.scale));
            i--;
        }
        return digitPos;
    }

    @Override
    public double toDouble() {
        double d;
        if (this.isApproximate) {
            return toDoubleFromOriginal();
        }
        if (isNaN()) {
            return Double.NaN;
        }
        if (isInfinite()) {
            return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        long digitPos = 0;
        int iMin = this.precision - Math.min(this.precision, 17);
        for (int i = this.precision - 1; i >= iMin; i--) {
            digitPos = (digitPos * 10) + ((long) getDigitPos(i));
        }
        double d2 = digitPos;
        int i2 = this.scale + iMin;
        if (i2 >= 0) {
            while (i2 >= 22) {
                d2 *= 1.0E22d;
                i2 -= 22;
            }
            d = d2 * DOUBLE_MULTIPLIERS[i2];
        } else {
            while (i2 <= -22) {
                d2 /= 1.0E22d;
                i2 += 22;
            }
            d = d2 / DOUBLE_MULTIPLIERS[-i2];
        }
        return isNegative() ? -d : d;
    }

    @Override
    public BigDecimal toBigDecimal() {
        if (this.isApproximate) {
            convertToAccurateDouble();
        }
        return bcdToBigDecimal();
    }

    protected double toDoubleFromOriginal() {
        double d;
        double d2 = this.origDouble;
        int i = this.origDelta;
        if (i >= 0) {
            while (i >= 22) {
                d2 *= 1.0E22d;
                i -= 22;
            }
            d = d2 * DOUBLE_MULTIPLIERS[i];
        } else {
            while (i <= -22) {
                d2 /= 1.0E22d;
                i += 22;
            }
            d = d2 / DOUBLE_MULTIPLIERS[-i];
        }
        return isNegative() ? d * (-1.0d) : d;
    }

    private static int safeSubtract(int i, int i2) {
        int i3 = i - i2;
        if (i2 < 0 && i3 < i) {
            return Integer.MAX_VALUE;
        }
        if (i2 <= 0 || i3 <= i) {
            return i3;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public void roundToMagnitude(int i, MathContext mathContext) {
        int i2;
        int iSafeSubtract = safeSubtract(i, this.scale);
        int precision = mathContext.getPrecision();
        if (i == Integer.MAX_VALUE || (precision > 0 && this.precision - iSafeSubtract > precision)) {
            iSafeSubtract = this.precision - precision;
        }
        if ((iSafeSubtract > 0 || this.isApproximate) && this.precision != 0) {
            byte digitPos = getDigitPos(safeSubtract(iSafeSubtract, 1));
            byte digitPos2 = getDigitPos(iSafeSubtract);
            int i3 = 2;
            int i4 = 3;
            if (this.isApproximate) {
                int iSafeSubtract2 = safeSubtract(iSafeSubtract, 2);
                int iMax = Math.max(0, this.precision - 14);
                if (digitPos != 0) {
                    if (digitPos != 4) {
                        if (digitPos != 5) {
                            if (digitPos != 9) {
                                if (digitPos >= 5) {
                                    i2 = 1;
                                    i2 = 3;
                                    break;
                                } else {
                                    i2 = 1;
                                    break;
                                }
                            }
                            while (iSafeSubtract2 >= iMax) {
                                if (getDigitPos(iSafeSubtract2) == 9) {
                                    iSafeSubtract2--;
                                }
                            }
                            i2 = -2;
                        } else {
                            while (iSafeSubtract2 >= iMax) {
                                if (getDigitPos(iSafeSubtract2) == 0) {
                                    iSafeSubtract2--;
                                }
                            }
                            i2 = 2;
                        }
                        i2 = 3;
                        break;
                    } else {
                        while (iSafeSubtract2 >= iMax) {
                            if (getDigitPos(iSafeSubtract2) != 9) {
                                i2 = 1;
                                break;
                            }
                            iSafeSubtract2--;
                        }
                        i2 = 2;
                    }
                    boolean zRoundsAtMidpoint = RoundingUtils.roundsAtMidpoint(mathContext.getRoundingMode().ordinal());
                    if (safeSubtract(iSafeSubtract, 1) >= this.precision - 14) {
                    }
                    convertToAccurateDouble();
                    roundToMagnitude(i, mathContext);
                    return;
                }
                while (iSafeSubtract2 >= iMax) {
                    if (getDigitPos(iSafeSubtract2) != 0) {
                        i2 = 1;
                        break;
                    }
                    iSafeSubtract2--;
                }
                i2 = -1;
                boolean zRoundsAtMidpoint2 = RoundingUtils.roundsAtMidpoint(mathContext.getRoundingMode().ordinal());
                if (safeSubtract(iSafeSubtract, 1) >= this.precision - 14 || ((zRoundsAtMidpoint2 && i2 == 2) || (!zRoundsAtMidpoint2 && i2 < 0))) {
                    convertToAccurateDouble();
                    roundToMagnitude(i, mathContext);
                    return;
                }
                this.isApproximate = false;
                this.origDouble = 0.0d;
                this.origDelta = 0;
                if (iSafeSubtract <= 0) {
                    return;
                }
                if (i2 == -1) {
                    i2 = 1;
                }
                if (i2 != -2) {
                    i4 = i2;
                }
            } else if (digitPos < 5) {
                i4 = 1;
            } else if (digitPos <= 5) {
                int iSafeSubtract3 = safeSubtract(iSafeSubtract, 2);
                while (true) {
                    if (iSafeSubtract3 < 0) {
                        break;
                    }
                    if (getDigitPos(iSafeSubtract3) != 0) {
                        i3 = 3;
                        break;
                    }
                    iSafeSubtract3--;
                }
                i4 = i3;
            }
            boolean roundingDirection = RoundingUtils.getRoundingDirection(digitPos2 % 2 == 0, isNegative(), i4, mathContext.getRoundingMode().ordinal(), this);
            if (iSafeSubtract >= this.precision) {
                setBcdToZero();
                this.scale = i;
            } else {
                shiftRight(iSafeSubtract);
            }
            if (!roundingDirection) {
                if (digitPos2 == 9) {
                    int i5 = 0;
                    while (getDigitPos(i5) == 9) {
                        i5++;
                    }
                    shiftRight(i5);
                }
                setDigitPos(0, (byte) (getDigitPos(0) + 1));
                this.precision++;
            }
            compact();
        }
    }

    @Override
    public void roundToInfinity() {
        if (this.isApproximate) {
            convertToAccurateDouble();
        }
    }

    @Deprecated
    public void appendDigit(byte b, int i, boolean z) {
        if (b == 0) {
            if (z && this.precision != 0) {
                this.scale += i + 1;
                return;
            }
            return;
        }
        if (this.scale > 0) {
            i += this.scale;
            if (z) {
                this.scale = 0;
            }
        }
        int i2 = i + 1;
        shiftLeft(i2);
        setDigitPos(0, b);
        if (z) {
            this.scale += i2;
        }
    }

    @Override
    public String toPlainString() {
        StringBuilder sb = new StringBuilder();
        if (isNegative()) {
            sb.append('-');
        }
        for (int upperDisplayMagnitude = getUpperDisplayMagnitude(); upperDisplayMagnitude >= getLowerDisplayMagnitude(); upperDisplayMagnitude--) {
            sb.append((int) getDigit(upperDisplayMagnitude));
            if (upperDisplayMagnitude == 0) {
                sb.append('.');
            }
        }
        return sb.toString();
    }
}
