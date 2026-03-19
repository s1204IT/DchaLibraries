package android.icu.math;

import android.icu.lang.UCharacter;
import android.icu.text.PluralRules;
import java.io.Serializable;
import java.math.BigInteger;

public class BigDecimal extends Number implements Serializable, Comparable<BigDecimal> {
    private static final int MaxArg = 999999999;
    private static final int MaxExp = 999999999;
    private static final int MinArg = -999999999;
    private static final int MinExp = -999999999;
    public static final int ROUND_CEILING = 2;
    public static final int ROUND_DOWN = 1;
    public static final int ROUND_FLOOR = 3;
    public static final int ROUND_HALF_DOWN = 5;
    public static final int ROUND_HALF_EVEN = 6;
    public static final int ROUND_HALF_UP = 4;
    public static final int ROUND_UNNECESSARY = 7;
    public static final int ROUND_UP = 0;
    private static final byte isneg = -1;
    private static final byte ispos = 1;
    private static final byte iszero = 0;
    private static final long serialVersionUID = 8245355804974198832L;
    private int exp;
    private byte form;
    private byte ind;
    private byte[] mant;
    public static final BigDecimal ZERO = new BigDecimal(0L);
    public static final BigDecimal ONE = new BigDecimal(1L);
    public static final BigDecimal TEN = new BigDecimal(10);
    private static final MathContext plainMC = new MathContext(0, 0);
    private static byte[] bytecar = new byte[190];
    private static byte[] bytedig = diginit();

    public BigDecimal(java.math.BigDecimal bigDecimal) {
        this(bigDecimal.toString());
    }

    public BigDecimal(BigInteger bigInteger) {
        this(bigInteger.toString(10));
    }

    public BigDecimal(BigInteger bigInteger, int i) {
        this(bigInteger.toString(10));
        if (i < 0) {
            throw new NumberFormatException("Negative scale: " + i);
        }
        this.exp = -i;
    }

    public BigDecimal(char[] cArr) {
        this(cArr, 0, cArr.length);
    }

    public BigDecimal(char[] cArr, int i, int i2) {
        int i3;
        int i4;
        boolean z;
        int i5;
        boolean z2;
        int iDigit;
        this.form = (byte) 0;
        if (i2 <= 0) {
            bad(cArr);
        }
        this.ind = (byte) 1;
        if (cArr[i] == '-') {
            i4 = i2 - 1;
            if (i4 == 0) {
                bad(cArr);
            }
            this.ind = (byte) -1;
            i3 = i + 1;
        } else if (cArr[i] == '+') {
            i4 = i2 - 1;
            if (i4 == 0) {
                bad(cArr);
            }
            i3 = i + 1;
        } else {
            i3 = i;
            i4 = i2;
        }
        int i6 = i4;
        int i7 = 0;
        boolean z3 = false;
        int i8 = -1;
        int i9 = -1;
        int i10 = i3;
        while (i6 > 0) {
            char c = cArr[i10];
            if (c >= '0' && c <= '9') {
                i7++;
                i9 = i10;
            } else if (c == '.') {
                if (i8 >= 0) {
                    bad(cArr);
                }
                i8 = i10 - i3;
            } else {
                if (c == 'e' || c == 'E') {
                    if (i10 - i3 > i4 - 2) {
                        bad(cArr);
                    }
                    int i11 = i10 + 1;
                    if (cArr[i11] == '-') {
                        i11 = i10 + 2;
                        z2 = true;
                    } else {
                        i11 = cArr[i11] == '+' ? i10 + 2 : i11;
                        z2 = false;
                    }
                    int i12 = i4 - (i11 - i3);
                    if ((i12 == 0) | (i12 > 9)) {
                        bad(cArr);
                    }
                    while (i12 > 0) {
                        char c2 = cArr[i11];
                        if (c2 < '0') {
                            bad(cArr);
                        }
                        if (c2 > '9') {
                            if (!UCharacter.isDigit(c2)) {
                                bad(cArr);
                            }
                            iDigit = UCharacter.digit(c2, 10);
                            if (iDigit < 0) {
                                bad(cArr);
                            }
                        } else {
                            iDigit = c2 - '0';
                        }
                        this.exp = (this.exp * 10) + iDigit;
                        i12--;
                        i11++;
                    }
                    if (z2) {
                        this.exp = -this.exp;
                    }
                    z = true;
                    if (i7 == 0) {
                        bad(cArr);
                    }
                    if (i8 >= 0) {
                        this.exp = (this.exp + i8) - i7;
                    }
                    i5 = i9 - 1;
                    int i13 = i3;
                    while (i3 <= i5) {
                        char c3 = cArr[i3];
                        if (c3 != '0') {
                            if (c3 != '.') {
                                if (c3 <= '9' || UCharacter.digit(c3, 10) != 0) {
                                    break;
                                }
                                i13++;
                                i8--;
                                i7--;
                            } else {
                                i13++;
                                i8--;
                            }
                        } else {
                            i13++;
                            i8--;
                            i7--;
                        }
                        i3++;
                    }
                    this.mant = new byte[i7];
                    if (z3) {
                        int i14 = 0;
                        while (i7 > 0) {
                            if (i14 == i8) {
                                i13++;
                            }
                            this.mant[i14] = (byte) (cArr[i13] - '0');
                            i13++;
                            i7--;
                            i14++;
                        }
                    } else {
                        int i15 = 0;
                        while (i7 > 0) {
                            i13 = i15 == i8 ? i13 + 1 : i13;
                            char c4 = cArr[i13];
                            if (c4 <= '9') {
                                this.mant[i15] = (byte) (c4 - '0');
                            } else {
                                int iDigit2 = UCharacter.digit(c4, 10);
                                if (iDigit2 < 0) {
                                    bad(cArr);
                                }
                                this.mant[i15] = (byte) iDigit2;
                            }
                            i13++;
                            i7--;
                            i15++;
                        }
                    }
                    if (this.mant[0] != 0) {
                        this.ind = (byte) 0;
                        if (this.exp > 0) {
                            this.exp = 0;
                        }
                        if (z) {
                            this.mant = ZERO.mant;
                            this.exp = 0;
                            return;
                        }
                        return;
                    }
                    if (z) {
                        this.form = (byte) 1;
                        int length = (this.exp + this.mant.length) - 1;
                        if ((length < -999999999) || (length > 999999999)) {
                            bad(cArr);
                            return;
                        }
                        return;
                    }
                    return;
                }
                if (!UCharacter.isDigit(c)) {
                    bad(cArr);
                }
                i7++;
                i9 = i10;
                z3 = true;
            }
            i6--;
            i10++;
        }
        z = false;
        if (i7 == 0) {
        }
        if (i8 >= 0) {
        }
        i5 = i9 - 1;
        int i132 = i3;
        while (i3 <= i5) {
        }
        this.mant = new byte[i7];
        if (z3) {
        }
        if (this.mant[0] != 0) {
        }
    }

    public BigDecimal(double d) {
        this(new java.math.BigDecimal(d).toString());
    }

    public BigDecimal(int i) {
        this.form = (byte) 0;
        int i2 = 9;
        if (i <= 9 && i >= -9) {
            if (i == 0) {
                this.mant = ZERO.mant;
                this.ind = (byte) 0;
                return;
            }
            if (i == 1) {
                this.mant = ONE.mant;
                this.ind = (byte) 1;
                return;
            }
            if (i == -1) {
                this.mant = ONE.mant;
                this.ind = (byte) -1;
                return;
            }
            this.mant = new byte[1];
            if (i > 0) {
                this.mant[0] = (byte) i;
                this.ind = (byte) 1;
                return;
            } else {
                this.mant[0] = (byte) (-i);
                this.ind = (byte) -1;
                return;
            }
        }
        if (i > 0) {
            this.ind = (byte) 1;
            i = -i;
        } else {
            this.ind = (byte) -1;
        }
        int i3 = i;
        while (true) {
            i3 /= 10;
            if (i3 == 0) {
                break;
            } else {
                i2--;
            }
        }
        int i4 = 10 - i2;
        this.mant = new byte[i4];
        int i5 = i4 - 1;
        while (true) {
            this.mant[i5] = (byte) (-((byte) (i % 10)));
            i /= 10;
            if (i != 0) {
                i5--;
            } else {
                return;
            }
        }
    }

    public BigDecimal(long j) {
        this.form = (byte) 0;
        if (j > 0) {
            this.ind = (byte) 1;
            j = -j;
        } else if (j == 0) {
            this.ind = (byte) 0;
        } else {
            this.ind = (byte) -1;
        }
        int i = 18;
        long j2 = j;
        while (true) {
            j2 /= 10;
            if (j2 == 0) {
                break;
            } else {
                i--;
            }
        }
        int i2 = 19 - i;
        this.mant = new byte[i2];
        int i3 = i2 - 1;
        while (true) {
            this.mant[i3] = (byte) (-((byte) (j % 10)));
            j /= 10;
            if (j != 0) {
                i3--;
            } else {
                return;
            }
        }
    }

    public BigDecimal(String str) {
        this(str.toCharArray(), 0, str.length());
    }

    private BigDecimal() {
        this.form = (byte) 0;
    }

    public BigDecimal abs() {
        return abs(plainMC);
    }

    public BigDecimal abs(MathContext mathContext) {
        if (this.ind == -1) {
            return negate(mathContext);
        }
        return plus(mathContext);
    }

    public BigDecimal add(BigDecimal bigDecimal) {
        return add(bigDecimal, plainMC);
    }

    public BigDecimal add(BigDecimal bigDecimal, MathContext mathContext) {
        byte[] bArr;
        int i;
        byte[] bArr2;
        int i2;
        int i3;
        byte b;
        byte b2;
        BigDecimal bigDecimalRound = this;
        BigDecimal bigDecimalRound2 = bigDecimal;
        if (mathContext.lostDigits) {
            bigDecimalRound.checkdigits(bigDecimalRound2, mathContext.digits);
        }
        if (bigDecimalRound.ind == 0 && mathContext.form != 0) {
            return bigDecimal.plus(mathContext);
        }
        if (bigDecimalRound2.ind == 0 && mathContext.form != 0) {
            return bigDecimalRound.plus(mathContext);
        }
        int i4 = mathContext.digits;
        if (i4 > 0) {
            if (bigDecimalRound.mant.length > i4) {
                bigDecimalRound = clone(this).round(mathContext);
            }
            if (bigDecimalRound2.mant.length > i4) {
                bigDecimalRound2 = clone(bigDecimal).round(mathContext);
            }
        }
        BigDecimal bigDecimal2 = new BigDecimal();
        byte[] bArr3 = bigDecimalRound.mant;
        int length = bigDecimalRound.mant.length;
        byte[] bArr4 = bigDecimalRound2.mant;
        int length2 = bigDecimalRound2.mant.length;
        if (bigDecimalRound.exp == bigDecimalRound2.exp) {
            bigDecimal2.exp = bigDecimalRound.exp;
        } else if (bigDecimalRound.exp > bigDecimalRound2.exp) {
            int i5 = (bigDecimalRound.exp + length) - bigDecimalRound2.exp;
            if (i5 >= length2 + i4 + 1 && i4 > 0) {
                bigDecimal2.mant = bArr3;
                bigDecimal2.exp = bigDecimalRound.exp;
                bigDecimal2.ind = bigDecimalRound.ind;
                if (length < i4) {
                    bigDecimal2.mant = extend(bigDecimalRound.mant, i4);
                    bigDecimal2.exp -= i4 - length;
                }
                return bigDecimal2.finish(mathContext, false);
            }
            bigDecimal2.exp = bigDecimalRound2.exp;
            int i6 = i4 + 1;
            if (i5 > i6 && i4 > 0) {
                int i7 = (i5 - i4) - 1;
                length2 -= i7;
                bigDecimal2.exp += i7;
                i5 = i6;
            }
            if (i5 > length) {
                length = i5;
            }
        } else {
            int i8 = (bigDecimalRound2.exp + length2) - bigDecimalRound.exp;
            if (i8 >= length + i4 + 1 && i4 > 0) {
                bigDecimal2.mant = bArr4;
                bigDecimal2.exp = bigDecimalRound2.exp;
                bigDecimal2.ind = bigDecimalRound2.ind;
                if (length2 < i4) {
                    bigDecimal2.mant = extend(bigDecimalRound2.mant, i4);
                    bigDecimal2.exp -= i4 - length2;
                }
                return bigDecimal2.finish(mathContext, false);
            }
            bigDecimal2.exp = bigDecimalRound.exp;
            int i9 = i4 + 1;
            if (i8 > i9 && i4 > 0) {
                int i10 = (i8 - i4) - 1;
                length -= i10;
                bigDecimal2.exp += i10;
                i8 = i9;
            }
            if (i8 > length2) {
                length2 = i8;
            }
        }
        if (bigDecimalRound.ind == 0) {
            bigDecimal2.ind = (byte) 1;
        } else {
            bigDecimal2.ind = bigDecimalRound.ind;
        }
        if ((bigDecimalRound.ind == -1) != (bigDecimalRound2.ind == -1)) {
            if (bigDecimalRound2.ind != 0) {
                if ((bigDecimalRound.ind == 0) | (length < length2)) {
                    bigDecimal2.ind = (byte) (-bigDecimal2.ind);
                } else {
                    if (length <= length2) {
                        int length3 = bArr3.length - 1;
                        int length4 = bArr4.length - 1;
                        int i11 = 0;
                        int i12 = 0;
                        while (true) {
                            if (i11 <= length3) {
                                b = bArr3[i11];
                            } else if (i12 > length4) {
                                if (mathContext.form != 0) {
                                    return ZERO;
                                }
                            } else {
                                b = 0;
                            }
                            if (i12 <= length4) {
                                b2 = bArr4[i12];
                            } else {
                                b2 = 0;
                            }
                            if (b != b2) {
                                if (b < b2) {
                                    bigDecimal2.ind = (byte) (-bigDecimal2.ind);
                                }
                            } else {
                                i11++;
                                i12++;
                            }
                        }
                    }
                    bArr = bArr3;
                    i = length;
                    bArr2 = bArr4;
                    i2 = length2;
                    i3 = -1;
                }
                bArr2 = bArr3;
                i2 = length;
                bArr = bArr4;
                i = length2;
                i3 = -1;
            } else {
                bArr = bArr3;
                i = length;
                bArr2 = bArr4;
                i2 = length2;
                i3 = -1;
            }
        } else {
            i = length;
            bArr2 = bArr4;
            i2 = length2;
            i3 = 1;
            bArr = bArr3;
        }
        bigDecimal2.mant = byteaddsub(bArr, i, bArr2, i2, i3, false);
        return bigDecimal2.finish(mathContext, false);
    }

    @Override
    public int compareTo(BigDecimal bigDecimal) {
        return compareTo(bigDecimal, plainMC);
    }

    public int compareTo(BigDecimal bigDecimal, MathContext mathContext) {
        if (mathContext.lostDigits) {
            checkdigits(bigDecimal, mathContext.digits);
        }
        if ((this.ind == bigDecimal.ind) & (this.exp == bigDecimal.exp)) {
            int length = this.mant.length;
            if (length < bigDecimal.mant.length) {
                return (byte) (-this.ind);
            }
            if (length > bigDecimal.mant.length) {
                return this.ind;
            }
            if ((length <= mathContext.digits) | (mathContext.digits == 0)) {
                int i = 0;
                while (length > 0) {
                    if (this.mant[i] < bigDecimal.mant[i]) {
                        return (byte) (-this.ind);
                    }
                    if (this.mant[i] <= bigDecimal.mant[i]) {
                        length--;
                        i++;
                    } else {
                        return this.ind;
                    }
                }
                return 0;
            }
        } else {
            if (this.ind < bigDecimal.ind) {
                return -1;
            }
            if (this.ind > bigDecimal.ind) {
                return 1;
            }
        }
        BigDecimal bigDecimalClone = clone(bigDecimal);
        bigDecimalClone.ind = (byte) (-bigDecimalClone.ind);
        return add(bigDecimalClone, mathContext).ind;
    }

    public BigDecimal divide(BigDecimal bigDecimal) {
        return dodivide('D', bigDecimal, plainMC, -1);
    }

    public BigDecimal divide(BigDecimal bigDecimal, int i) {
        return dodivide('D', bigDecimal, new MathContext(0, 0, false, i), -1);
    }

    public BigDecimal divide(BigDecimal bigDecimal, int i, int i2) {
        if (i < 0) {
            throw new ArithmeticException("Negative scale: " + i);
        }
        return dodivide('D', bigDecimal, new MathContext(0, 0, false, i2), i);
    }

    public BigDecimal divide(BigDecimal bigDecimal, MathContext mathContext) {
        return dodivide('D', bigDecimal, mathContext, -1);
    }

    public BigDecimal divideInteger(BigDecimal bigDecimal) {
        return dodivide('I', bigDecimal, plainMC, 0);
    }

    public BigDecimal divideInteger(BigDecimal bigDecimal, MathContext mathContext) {
        return dodivide('I', bigDecimal, mathContext, 0);
    }

    public BigDecimal max(BigDecimal bigDecimal) {
        return max(bigDecimal, plainMC);
    }

    public BigDecimal max(BigDecimal bigDecimal, MathContext mathContext) {
        if (compareTo(bigDecimal, mathContext) >= 0) {
            return plus(mathContext);
        }
        return bigDecimal.plus(mathContext);
    }

    public BigDecimal min(BigDecimal bigDecimal) {
        return min(bigDecimal, plainMC);
    }

    public BigDecimal min(BigDecimal bigDecimal, MathContext mathContext) {
        if (compareTo(bigDecimal, mathContext) <= 0) {
            return plus(mathContext);
        }
        return bigDecimal.plus(mathContext);
    }

    public BigDecimal multiply(BigDecimal bigDecimal) {
        return multiply(bigDecimal, plainMC);
    }

    public BigDecimal multiply(BigDecimal bigDecimal, MathContext mathContext) {
        int i;
        byte[] bArr;
        byte[] bArr2;
        int i2;
        BigDecimal bigDecimalRound = this;
        BigDecimal bigDecimalRound2 = bigDecimal;
        if (mathContext.lostDigits) {
            bigDecimalRound.checkdigits(bigDecimalRound2, mathContext.digits);
        }
        int i3 = mathContext.digits;
        if (i3 > 0) {
            if (bigDecimalRound.mant.length > i3) {
                bigDecimalRound = clone(this).round(mathContext);
            }
            if (bigDecimalRound2.mant.length > i3) {
                bigDecimalRound2 = clone(bigDecimal).round(mathContext);
            }
            i = 0;
        } else {
            if (bigDecimalRound.exp > 0) {
                i = bigDecimalRound.exp + 0;
            } else {
                i = 0;
            }
            if (bigDecimalRound2.exp > 0) {
                i += bigDecimalRound2.exp;
            }
        }
        if (bigDecimalRound.mant.length < bigDecimalRound2.mant.length) {
            bArr = bigDecimalRound.mant;
            bArr2 = bigDecimalRound2.mant;
        } else {
            bArr = bigDecimalRound2.mant;
            bArr2 = bigDecimalRound.mant;
        }
        byte[] bArr3 = bArr2;
        int length = (bArr.length + bArr3.length) - 1;
        if (bArr[0] * bArr3[0] > 9) {
            i2 = length + 1;
        } else {
            i2 = length;
        }
        BigDecimal bigDecimal2 = new BigDecimal();
        int i4 = 0;
        int i5 = length;
        byte[] bArrByteaddsub = new byte[i2];
        int length2 = bArr.length;
        while (length2 > 0) {
            byte b = bArr[i4];
            if (b != 0) {
                bArrByteaddsub = byteaddsub(bArrByteaddsub, bArrByteaddsub.length, bArr3, i5, b, true);
            }
            i5--;
            length2--;
            i4++;
        }
        bigDecimal2.ind = (byte) (bigDecimalRound.ind * bigDecimalRound2.ind);
        bigDecimal2.exp = (bigDecimalRound.exp + bigDecimalRound2.exp) - i;
        if (i == 0) {
            bigDecimal2.mant = bArrByteaddsub;
        } else {
            bigDecimal2.mant = extend(bArrByteaddsub, bArrByteaddsub.length + i);
        }
        return bigDecimal2.finish(mathContext, false);
    }

    public BigDecimal negate() {
        return negate(plainMC);
    }

    public BigDecimal negate(MathContext mathContext) {
        if (mathContext.lostDigits) {
            checkdigits((BigDecimal) null, mathContext.digits);
        }
        BigDecimal bigDecimalClone = clone(this);
        bigDecimalClone.ind = (byte) (-bigDecimalClone.ind);
        return bigDecimalClone.finish(mathContext, false);
    }

    public BigDecimal plus() {
        return plus(plainMC);
    }

    public BigDecimal plus(MathContext mathContext) {
        if (mathContext.lostDigits) {
            checkdigits((BigDecimal) null, mathContext.digits);
        }
        if (mathContext.form == 0 && this.form == 0 && (this.mant.length <= mathContext.digits || mathContext.digits == 0)) {
            return this;
        }
        return clone(this).finish(mathContext, false);
    }

    public BigDecimal pow(BigDecimal bigDecimal) {
        return pow(bigDecimal, plainMC);
    }

    public BigDecimal pow(BigDecimal bigDecimal, MathContext mathContext) {
        BigDecimal bigDecimalRound;
        int length;
        if (mathContext.lostDigits) {
            checkdigits(bigDecimal, mathContext.digits);
        }
        int iIntcheck = bigDecimal.intcheck(-999999999, 999999999);
        int i = mathContext.digits;
        if (i == 0) {
            if (bigDecimal.ind == -1) {
                throw new ArithmeticException("Negative power: " + bigDecimal.toString());
            }
            bigDecimalRound = this;
            length = 0;
        } else {
            if (bigDecimal.mant.length + bigDecimal.exp > i) {
                throw new ArithmeticException("Too many digits: " + bigDecimal.toString());
            }
            if (this.mant.length > i) {
                bigDecimalRound = clone(this).round(mathContext);
            } else {
                bigDecimalRound = this;
            }
            length = i + bigDecimal.mant.length + bigDecimal.exp + 1;
        }
        MathContext mathContext2 = new MathContext(length, mathContext.form, false, mathContext.roundingMode);
        BigDecimal bigDecimalDivide = ONE;
        if (iIntcheck == 0) {
            return bigDecimalDivide;
        }
        if (iIntcheck < 0) {
            iIntcheck = -iIntcheck;
        }
        boolean z = false;
        int i2 = 1;
        while (true) {
            iIntcheck += iIntcheck;
            if (iIntcheck < 0) {
                bigDecimalDivide = bigDecimalDivide.multiply(bigDecimalRound, mathContext2);
                z = true;
            }
            if (i2 == 31) {
                break;
            }
            if (z) {
                bigDecimalDivide = bigDecimalDivide.multiply(bigDecimalDivide, mathContext2);
            }
            i2++;
        }
        if (bigDecimal.ind < 0) {
            bigDecimalDivide = ONE.divide(bigDecimalDivide, mathContext2);
        }
        return bigDecimalDivide.finish(mathContext, true);
    }

    public BigDecimal remainder(BigDecimal bigDecimal) {
        return dodivide('R', bigDecimal, plainMC, -1);
    }

    public BigDecimal remainder(BigDecimal bigDecimal, MathContext mathContext) {
        return dodivide('R', bigDecimal, mathContext, -1);
    }

    public BigDecimal subtract(BigDecimal bigDecimal) {
        return subtract(bigDecimal, plainMC);
    }

    public BigDecimal subtract(BigDecimal bigDecimal, MathContext mathContext) {
        if (mathContext.lostDigits) {
            checkdigits(bigDecimal, mathContext.digits);
        }
        BigDecimal bigDecimalClone = clone(bigDecimal);
        bigDecimalClone.ind = (byte) (-bigDecimalClone.ind);
        return add(bigDecimalClone, mathContext);
    }

    public byte byteValueExact() {
        int iIntValueExact = intValueExact();
        if ((iIntValueExact < -128) | (iIntValueExact > 127)) {
            throw new ArithmeticException("Conversion overflow: " + toString());
        }
        return (byte) iIntValueExact;
    }

    @Override
    public double doubleValue() {
        return Double.valueOf(toString()).doubleValue();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BigDecimal)) {
            return false;
        }
        BigDecimal bigDecimal = (BigDecimal) obj;
        if (this.ind != bigDecimal.ind) {
            return false;
        }
        if ((this.mant.length == bigDecimal.mant.length) & (this.exp == bigDecimal.exp) & (this.form == bigDecimal.form)) {
            int length = this.mant.length;
            int i = 0;
            while (length > 0) {
                if (this.mant[i] != bigDecimal.mant[i]) {
                    return false;
                }
                length--;
                i++;
            }
        } else {
            char[] cArrLayout = layout();
            char[] cArrLayout2 = bigDecimal.layout();
            if (cArrLayout.length != cArrLayout2.length) {
                return false;
            }
            int length2 = cArrLayout.length;
            int i2 = 0;
            while (length2 > 0) {
                if (cArrLayout[i2] != cArrLayout2[i2]) {
                    return false;
                }
                length2--;
                i2++;
            }
        }
        return true;
    }

    @Override
    public float floatValue() {
        return Float.valueOf(toString()).floatValue();
    }

    public String format(int i, int i2) {
        return format(i, i2, -1, -1, 1, 4);
    }

    public String format(int i, int i2, int i3, int i4, int i5, int i6) {
        char[] cArr;
        int length;
        if ((i < -1) | (i == 0)) {
            badarg("format", 1, String.valueOf(i));
        }
        if (i2 < -1) {
            badarg("format", 2, String.valueOf(i2));
        }
        if ((i3 < -1) | (i3 == 0)) {
            badarg("format", 3, String.valueOf(i3));
        }
        if (i4 < -1) {
            badarg("format", 4, String.valueOf(i3));
        }
        if (i5 != 1 && i5 != 2) {
            if (i5 != -1) {
                badarg("format", 5, String.valueOf(i5));
            } else {
                i5 = 1;
            }
        }
        if (i6 != 4) {
            if (i6 != -1) {
                try {
                    new MathContext(9, 1, false, i6);
                } catch (IllegalArgumentException e) {
                    badarg("format", 6, String.valueOf(i6));
                }
            } else {
                i6 = 4;
            }
        }
        BigDecimal bigDecimalClone = clone(this);
        if (i4 == -1 || bigDecimalClone.ind == 0) {
            bigDecimalClone.form = (byte) 0;
        } else {
            int length2 = bigDecimalClone.exp + bigDecimalClone.mant.length;
            if (length2 > i4 || length2 < -5) {
                bigDecimalClone.form = (byte) i5;
            } else {
                bigDecimalClone.form = (byte) 0;
            }
        }
        if (i2 >= 0) {
            while (true) {
                if (bigDecimalClone.form == 0) {
                    length = -bigDecimalClone.exp;
                } else if (bigDecimalClone.form == 1) {
                    length = bigDecimalClone.mant.length - 1;
                } else {
                    int length3 = ((bigDecimalClone.exp + bigDecimalClone.mant.length) - 1) % 3;
                    if (length3 < 0) {
                        length3 += 3;
                    }
                    int i7 = length3 + 1;
                    if (i7 < bigDecimalClone.mant.length) {
                        length = bigDecimalClone.mant.length - i7;
                    } else {
                        length = 0;
                    }
                }
                if (length == i2) {
                    break;
                }
                if (length < i2) {
                    bigDecimalClone.mant = extend(bigDecimalClone.mant, (bigDecimalClone.mant.length + i2) - length);
                    bigDecimalClone.exp -= i2 - length;
                    if (bigDecimalClone.exp < -999999999) {
                        throw new ArithmeticException("Exponent Overflow: " + bigDecimalClone.exp);
                    }
                } else {
                    int i8 = length - i2;
                    if (i8 > bigDecimalClone.mant.length) {
                        bigDecimalClone.mant = ZERO.mant;
                        bigDecimalClone.ind = (byte) 0;
                        bigDecimalClone.exp = 0;
                    } else {
                        int length4 = bigDecimalClone.mant.length - i8;
                        int i9 = bigDecimalClone.exp;
                        bigDecimalClone.round(length4, i6);
                        if (bigDecimalClone.exp - i9 == i8) {
                            break;
                        }
                    }
                }
            }
        }
        char[] cArrLayout = bigDecimalClone.layout();
        if (i > 0) {
            int length5 = cArrLayout.length;
            int i10 = 0;
            while (length5 > 0 && cArrLayout[i10] != '.' && cArrLayout[i10] != 'E') {
                length5--;
                i10++;
            }
            if (i10 > i) {
                badarg("format", 1, String.valueOf(i));
            }
            if (i10 < i) {
                char[] cArr2 = new char[(cArrLayout.length + i) - i10];
                int i11 = i - i10;
                int i12 = 0;
                while (i11 > 0) {
                    cArr2[i12] = ' ';
                    i11--;
                    i12++;
                }
                System.arraycopy(cArrLayout, 0, cArr2, i12, cArrLayout.length);
                cArrLayout = cArr2;
            }
        }
        if (i3 > 0) {
            int length6 = cArrLayout.length - 1;
            int length7 = cArrLayout.length - 1;
            while (length6 > 0 && cArrLayout[length7] != 'E') {
                length6--;
                length7--;
            }
            if (length7 == 0) {
                cArr = new char[cArrLayout.length + i3 + 2];
                System.arraycopy(cArrLayout, 0, cArr, 0, cArrLayout.length);
                int i13 = i3 + 2;
                int length8 = cArrLayout.length;
                while (i13 > 0) {
                    cArr[length8] = ' ';
                    i13--;
                    length8++;
                }
            } else {
                int length9 = (cArrLayout.length - length7) - 2;
                if (length9 > i3) {
                    badarg("format", 3, String.valueOf(i3));
                }
                if (length9 < i3) {
                    char[] cArr3 = new char[(cArrLayout.length + i3) - length9];
                    int i14 = length7 + 2;
                    System.arraycopy(cArrLayout, 0, cArr3, 0, i14);
                    int i15 = i3 - length9;
                    int i16 = i14;
                    while (i15 > 0) {
                        cArr3[i16] = '0';
                        i15--;
                        i16++;
                    }
                    System.arraycopy(cArrLayout, i14, cArr3, i16, length9);
                    cArr = cArr3;
                } else {
                    cArr = cArrLayout;
                }
            }
        }
        return new String(cArr);
    }

    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public int intValue() {
        return toBigInteger().intValue();
    }

    public int intValueExact() {
        int i;
        if (this.ind == 0) {
            return 0;
        }
        int length = this.mant.length - 1;
        if (this.exp < 0) {
            length += this.exp;
            if (!allzero(this.mant, length + 1)) {
                throw new ArithmeticException("Decimal part non-zero: " + toString());
            }
            if (length < 0) {
                return 0;
            }
            i = 0;
        } else {
            if (this.exp + length > 9) {
                throw new ArithmeticException("Conversion overflow: " + toString());
            }
            i = this.exp;
        }
        int i2 = i + length;
        int i3 = 0;
        for (int i4 = 0; i4 <= i2; i4++) {
            i3 *= 10;
            if (i4 <= length) {
                i3 += this.mant[i4];
            }
        }
        if (i2 == 9 && i3 / 1000000000 != this.mant[0]) {
            if (i3 == Integer.MIN_VALUE && this.ind == -1 && this.mant[0] == 2) {
                return i3;
            }
            throw new ArithmeticException("Conversion overflow: " + toString());
        }
        if (this.ind == 1) {
            return i3;
        }
        return -i3;
    }

    @Override
    public long longValue() {
        return toBigInteger().longValue();
    }

    public long longValueExact() {
        int i;
        int i2;
        if (this.ind == 0) {
            return 0L;
        }
        int length = this.mant.length - 1;
        if (this.exp < 0) {
            length += this.exp;
            if (length >= 0) {
                i2 = length + 1;
            } else {
                i2 = 0;
            }
            if (!allzero(this.mant, i2)) {
                throw new ArithmeticException("Decimal part non-zero: " + toString());
            }
            if (length < 0) {
                return 0L;
            }
            i = 0;
        } else {
            if (this.exp + this.mant.length > 18) {
                throw new ArithmeticException("Conversion overflow: " + toString());
            }
            i = this.exp;
        }
        int i3 = i + length;
        long j = 0;
        for (int i4 = 0; i4 <= i3; i4++) {
            j *= 10;
            if (i4 <= length) {
                j += (long) this.mant[i4];
            }
        }
        if (i3 == 18 && j / 1000000000000000000L != this.mant[0]) {
            if (j == Long.MIN_VALUE && this.ind == -1 && this.mant[0] == 9) {
                return j;
            }
            throw new ArithmeticException("Conversion overflow: " + toString());
        }
        if (this.ind == 1) {
            return j;
        }
        return -j;
    }

    public BigDecimal movePointLeft(int i) {
        BigDecimal bigDecimalClone = clone(this);
        bigDecimalClone.exp -= i;
        return bigDecimalClone.finish(plainMC, false);
    }

    public BigDecimal movePointRight(int i) {
        BigDecimal bigDecimalClone = clone(this);
        bigDecimalClone.exp += i;
        return bigDecimalClone.finish(plainMC, false);
    }

    public int scale() {
        if (this.exp >= 0) {
            return 0;
        }
        return -this.exp;
    }

    public BigDecimal setScale(int i) {
        return setScale(i, 7);
    }

    public BigDecimal setScale(int i, int i2) {
        int i3;
        int iScale = scale();
        if (iScale == i && this.form == 0) {
            return this;
        }
        BigDecimal bigDecimalClone = clone(this);
        if (iScale <= i) {
            if (iScale == 0) {
                i3 = bigDecimalClone.exp + i;
            } else {
                i3 = i - iScale;
            }
            bigDecimalClone.mant = extend(bigDecimalClone.mant, bigDecimalClone.mant.length + i3);
            bigDecimalClone.exp = -i;
        } else {
            if (i < 0) {
                throw new ArithmeticException("Negative scale: " + i);
            }
            bigDecimalClone = bigDecimalClone.round(bigDecimalClone.mant.length - (iScale - i), i2);
            if (bigDecimalClone.exp != (-i)) {
                bigDecimalClone.mant = extend(bigDecimalClone.mant, bigDecimalClone.mant.length + 1);
                bigDecimalClone.exp--;
            }
        }
        bigDecimalClone.form = (byte) 0;
        return bigDecimalClone;
    }

    public short shortValueExact() {
        int iIntValueExact = intValueExact();
        if ((iIntValueExact < -32768) | (iIntValueExact > 32767)) {
            throw new ArithmeticException("Conversion overflow: " + toString());
        }
        return (short) iIntValueExact;
    }

    public int signum() {
        return this.ind;
    }

    public java.math.BigDecimal toBigDecimal() {
        return new java.math.BigDecimal(unscaledValue(), scale());
    }

    public BigInteger toBigInteger() {
        BigDecimal bigDecimalClone;
        if (!((this.exp >= 0) & (this.form == 0))) {
            if (this.exp >= 0) {
                bigDecimalClone = clone(this);
                bigDecimalClone.form = (byte) 0;
            } else if ((-this.exp) >= this.mant.length) {
                bigDecimalClone = ZERO;
            } else {
                bigDecimalClone = clone(this);
                int length = bigDecimalClone.mant.length + bigDecimalClone.exp;
                byte[] bArr = new byte[length];
                System.arraycopy(bigDecimalClone.mant, 0, bArr, 0, length);
                bigDecimalClone.mant = bArr;
                bigDecimalClone.form = (byte) 0;
                bigDecimalClone.exp = 0;
            }
        } else {
            bigDecimalClone = this;
        }
        return new BigInteger(new String(bigDecimalClone.layout()));
    }

    public BigInteger toBigIntegerExact() {
        if (this.exp < 0 && !allzero(this.mant, this.mant.length + this.exp)) {
            throw new ArithmeticException("Decimal part non-zero: " + toString());
        }
        return toBigInteger();
    }

    public char[] toCharArray() {
        return layout();
    }

    public String toString() {
        return new String(layout());
    }

    public BigInteger unscaledValue() {
        BigDecimal bigDecimalClone;
        if (this.exp < 0) {
            bigDecimalClone = clone(this);
            bigDecimalClone.exp = 0;
        } else {
            bigDecimalClone = this;
        }
        return bigDecimalClone.toBigInteger();
    }

    public static BigDecimal valueOf(double d) {
        return new BigDecimal(new Double(d).toString());
    }

    public static BigDecimal valueOf(long j) {
        return valueOf(j, 0);
    }

    public static BigDecimal valueOf(long j, int i) {
        BigDecimal bigDecimal;
        if (j == 0) {
            bigDecimal = ZERO;
        } else if (j == 1) {
            bigDecimal = ONE;
        } else if (j == 10) {
            bigDecimal = TEN;
        } else {
            bigDecimal = new BigDecimal(j);
        }
        if (i == 0) {
            return bigDecimal;
        }
        if (i < 0) {
            throw new NumberFormatException("Negative scale: " + i);
        }
        BigDecimal bigDecimalClone = clone(bigDecimal);
        bigDecimalClone.exp = -i;
        return bigDecimalClone;
    }

    private char[] layout() {
        char[] cArr = new char[this.mant.length];
        int length = this.mant.length;
        int i = 0;
        while (length > 0) {
            cArr[i] = (char) (this.mant[i] + 48);
            length--;
            i++;
        }
        char c = '-';
        if (this.form != 0) {
            StringBuilder sb = new StringBuilder(cArr.length + 15);
            if (this.ind == -1) {
                sb.append('-');
            }
            int length2 = (this.exp + cArr.length) - 1;
            if (this.form == 1) {
                sb.append(cArr[0]);
                if (cArr.length > 1) {
                    sb.append('.');
                    sb.append(cArr, 1, cArr.length - 1);
                }
            } else {
                int i2 = length2 % 3;
                if (i2 < 0) {
                    i2 += 3;
                }
                length2 -= i2;
                int i3 = i2 + 1;
                if (i3 >= cArr.length) {
                    sb.append(cArr, 0, cArr.length);
                    for (int length3 = i3 - cArr.length; length3 > 0; length3--) {
                        sb.append('0');
                    }
                } else {
                    sb.append(cArr, 0, i3);
                    sb.append('.');
                    sb.append(cArr, i3, cArr.length - i3);
                }
            }
            if (length2 != 0) {
                if (length2 < 0) {
                    length2 = -length2;
                } else {
                    c = '+';
                }
                sb.append('E');
                sb.append(c);
                sb.append(length2);
            }
            char[] cArr2 = new char[sb.length()];
            int length4 = sb.length();
            if (length4 != 0) {
                sb.getChars(0, length4, cArr2, 0);
            }
            return cArr2;
        }
        if (this.exp == 0) {
            if (this.ind >= 0) {
                return cArr;
            }
            char[] cArr3 = new char[cArr.length + 1];
            cArr3[0] = '-';
            System.arraycopy(cArr, 0, cArr3, 1, cArr.length);
            return cArr3;
        }
        int i4 = this.ind == -1 ? 1 : 0;
        int length5 = this.exp + cArr.length;
        if (length5 < 1) {
            int i5 = i4 + 2;
            char[] cArr4 = new char[i5 - this.exp];
            if (i4 != 0) {
                cArr4[0] = '-';
            }
            cArr4[i4] = '0';
            cArr4[i4 + 1] = '.';
            int i6 = -length5;
            int i7 = i5;
            while (i6 > 0) {
                cArr4[i7] = '0';
                i6--;
                i7++;
            }
            System.arraycopy(cArr, 0, cArr4, i5 - length5, cArr.length);
            return cArr4;
        }
        if (length5 > cArr.length) {
            char[] cArr5 = new char[i4 + length5];
            if (i4 != 0) {
                cArr5[0] = '-';
            }
            System.arraycopy(cArr, 0, cArr5, i4, cArr.length);
            int length6 = length5 - cArr.length;
            int length7 = i4 + cArr.length;
            while (length6 > 0) {
                cArr5[length7] = '0';
                length6--;
                length7++;
            }
            return cArr5;
        }
        char[] cArr6 = new char[i4 + 1 + cArr.length];
        if (i4 != 0) {
            cArr6[0] = '-';
        }
        System.arraycopy(cArr, 0, cArr6, i4, length5);
        int i8 = i4 + length5;
        cArr6[i8] = '.';
        System.arraycopy(cArr, length5, cArr6, i8 + 1, cArr.length - length5);
        return cArr6;
    }

    private int intcheck(int i, int i2) {
        int iIntValueExact = intValueExact();
        if ((iIntValueExact < i) | (iIntValueExact > i2)) {
            throw new ArithmeticException("Conversion overflow: " + iIntValueExact);
        }
        return iIntValueExact;
    }

    private BigDecimal dodivide(char c, BigDecimal bigDecimal, MathContext mathContext, int i) {
        byte[] bArr;
        int i2;
        byte[] bArr2;
        boolean z;
        boolean z2;
        byte[] bArr3;
        int i3;
        int i4;
        int i5;
        BigDecimal bigDecimalRound = this;
        BigDecimal bigDecimalRound2 = bigDecimal;
        int iScale = i;
        if (mathContext.lostDigits) {
            bigDecimalRound.checkdigits(bigDecimalRound2, mathContext.digits);
        }
        if (bigDecimalRound2.ind == 0) {
            throw new ArithmeticException("Divide by 0");
        }
        if (bigDecimalRound.ind == 0) {
            return mathContext.form != 0 ? ZERO : iScale == -1 ? bigDecimalRound : bigDecimalRound.setScale(iScale);
        }
        int length = mathContext.digits;
        if (length > 0) {
            if (bigDecimalRound.mant.length > length) {
                bigDecimalRound = clone(this).round(mathContext);
            }
            if (bigDecimalRound2.mant.length > length) {
                bigDecimalRound2 = clone(bigDecimal).round(mathContext);
            }
        } else {
            if (iScale == -1) {
                iScale = scale();
            }
            int length2 = bigDecimalRound.mant.length;
            if (iScale != (-bigDecimalRound.exp)) {
                length2 = length2 + iScale + bigDecimalRound.exp;
            }
            length = (length2 - (bigDecimalRound2.mant.length - 1)) - bigDecimalRound2.exp;
            if (length < bigDecimalRound.mant.length) {
                length = bigDecimalRound.mant.length;
            }
            if (length < bigDecimalRound2.mant.length) {
                length = bigDecimalRound2.mant.length;
            }
        }
        int length3 = ((bigDecimalRound.exp - bigDecimalRound2.exp) + bigDecimalRound.mant.length) - bigDecimalRound2.mant.length;
        int i6 = 0;
        if (length3 < 0 && c != 'D') {
            return c == 'I' ? ZERO : clone(bigDecimalRound).finish(mathContext, false);
        }
        BigDecimal bigDecimal2 = new BigDecimal();
        bigDecimal2.ind = (byte) (bigDecimalRound.ind * bigDecimalRound2.ind);
        bigDecimal2.exp = length3;
        int i7 = length + 1;
        bigDecimal2.mant = new byte[i7];
        int i8 = length + length + 1;
        byte[] bArrExtend = extend(bigDecimalRound.mant, i8);
        byte[] bArr4 = bigDecimalRound2.mant;
        int i9 = (bArr4[0] * 10) + 1;
        if (bArr4.length > 1) {
            i9 += bArr4[1];
        }
        int i10 = i9;
        int i11 = 0;
        int i12 = i8;
        byte[] bArr5 = bArrExtend;
        int i13 = i12;
        loop0: while (true) {
            int i14 = i6;
            bArr = bArr5;
            while (true) {
                if (i13 < i12) {
                    i2 = i10;
                    bArr2 = bArr4;
                    break;
                }
                if (i13 == i12) {
                    int i15 = i13;
                    while (i15 > 0) {
                        i2 = i10;
                        char c2 = i6 < bArr4.length ? bArr4[i6] : (char) 0;
                        bArr2 = bArr4;
                        if (bArr[i6] < c2) {
                            break;
                        }
                        if (bArr[i6] > c2) {
                            i4 = bArr[0];
                            i3 = 1;
                        } else {
                            i15--;
                            i6++;
                            i10 = i2;
                            bArr4 = bArr2;
                        }
                    }
                    bigDecimal2.mant[i11] = (byte) (i14 + 1);
                    i11++;
                    bArr[0] = 0;
                    break loop0;
                }
                i2 = i10;
                bArr2 = bArr4;
                i3 = 1;
                i4 = bArr[i6] * 10;
                if (i13 > 1) {
                    i4 += bArr[1];
                }
                int i16 = (i4 * 10) / i2;
                if (i16 == 0) {
                    i16 = i3;
                }
                i14 += i16;
                int i17 = i12;
                byte[] bArr6 = bArr;
                byte[] bArr7 = bArr2;
                byte[] bArrByteaddsub = byteaddsub(bArr6, i13, bArr7, i17, -i16, true);
                if (bArrByteaddsub[0] != 0) {
                    bArr4 = bArr7;
                    bArr = bArrByteaddsub;
                    i12 = i17;
                    i10 = i2;
                    i6 = 0;
                } else {
                    int i18 = i13 - 2;
                    int i19 = i13;
                    int i20 = 0;
                    while (i20 <= i18 && bArrByteaddsub[i20] == 0) {
                        i19--;
                        i20++;
                    }
                    if (i20 == 0) {
                        i5 = 0;
                    } else {
                        i5 = 0;
                        System.arraycopy(bArrByteaddsub, i20, bArrByteaddsub, 0, i19);
                    }
                    i13 = i19;
                    i10 = i2;
                    bArr4 = bArr7;
                    bArr = bArrByteaddsub;
                    i6 = i5;
                    i12 = i17;
                }
            }
            if ((i11 != 0) || (i14 != 0)) {
                bigDecimal2.mant[i11] = (byte) i14;
                int i21 = i11 + 1;
                if (i21 == i7 || bArr[0] == 0) {
                    break;
                }
                i11 = i21;
                if ((iScale < 0 && (-bigDecimal2.exp) > iScale) || (c != 'D' && bigDecimal2.exp <= 0)) {
                    break;
                }
                bigDecimal2.exp--;
                i12--;
                bArr5 = bArr;
                i10 = i2;
                bArr4 = bArr2;
                i6 = 0;
            } else if (iScale < 0) {
                bigDecimal2.exp--;
                i12--;
                bArr5 = bArr;
                i10 = i2;
                bArr4 = bArr2;
                i6 = 0;
            } else {
                bigDecimal2.exp--;
                i12--;
                bArr5 = bArr;
                i10 = i2;
                bArr4 = bArr2;
                i6 = 0;
            }
        }
        int i22 = i11 == 0 ? 1 : i11;
        if ((c == 'I') || (c == 'R')) {
            if (bigDecimal2.exp + i22 > length) {
                throw new ArithmeticException("Integer overflow");
            }
            if (c == 'R') {
                if (bigDecimal2.mant[0] == 0) {
                    return clone(bigDecimalRound).finish(mathContext, false);
                }
                if (bArr[0] == 0) {
                    return ZERO;
                }
                bigDecimal2.ind = bigDecimalRound.ind;
                bigDecimal2.exp = (bigDecimal2.exp - (i8 - bigDecimalRound.mant.length)) + bigDecimalRound.exp;
                for (int i23 = i13 - 1; i23 >= 1; i23--) {
                    if ((!(bigDecimal2.exp < bigDecimalRound.exp) || !(bigDecimal2.exp < bigDecimalRound2.exp)) || bArr[i23] != 0) {
                        break;
                    }
                    i13--;
                    bigDecimal2.exp++;
                }
                if (i13 < bArr.length) {
                    bArr3 = new byte[i13];
                    z2 = false;
                    System.arraycopy(bArr, 0, bArr3, 0, i13);
                } else {
                    z2 = false;
                    bArr3 = bArr;
                }
                bigDecimal2.mant = bArr3;
                return bigDecimal2.finish(mathContext, z2);
            }
        } else if (bArr[0] != 0) {
            int i24 = i22 - 1;
            byte b = bigDecimal2.mant[i24];
            if (b % 5 == 0) {
                bigDecimal2.mant[i24] = (byte) (b + 1);
            }
        }
        if (iScale < 0) {
            if (i22 == bigDecimal2.mant.length) {
                bigDecimal2.round(mathContext);
            } else {
                if (bigDecimal2.mant[0] == 0) {
                    return ZERO;
                }
                byte[] bArr8 = new byte[i22];
                System.arraycopy(bigDecimal2.mant, 0, bArr8, 0, i22);
                bigDecimal2.mant = bArr8;
            }
            return bigDecimal2.finish(mathContext, true);
        }
        if (i22 != bigDecimal2.mant.length) {
            bigDecimal2.exp -= bigDecimal2.mant.length - i22;
        }
        bigDecimal2.round(bigDecimal2.mant.length - ((-bigDecimal2.exp) - iScale), mathContext.roundingMode);
        if (bigDecimal2.exp != (-iScale)) {
            z = true;
            bigDecimal2.mant = extend(bigDecimal2.mant, bigDecimal2.mant.length + 1);
            bigDecimal2.exp--;
        } else {
            z = true;
        }
        return bigDecimal2.finish(mathContext, z);
    }

    private void bad(char[] cArr) {
        throw new NumberFormatException("Not a number: " + String.valueOf(cArr));
    }

    private void badarg(String str, int i, String str2) {
        throw new IllegalArgumentException("Bad argument " + i + " to " + str + PluralRules.KEYWORD_RULE_SEPARATOR + str2);
    }

    private static final byte[] extend(byte[] bArr, int i) {
        if (bArr.length == i) {
            return bArr;
        }
        byte[] bArr2 = new byte[i];
        System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
        return bArr2;
    }

    private static final byte[] byteaddsub(byte[] bArr, int i, byte[] bArr2, int i2, int i3, boolean z) {
        byte[] bArr3;
        int length = bArr.length;
        int length2 = bArr2.length;
        int i4 = i - 1;
        int i5 = i2 - 1;
        int i6 = i5 < i4 ? i4 : i5;
        byte[] bArr4 = (z && i6 + 1 == length) ? bArr : null;
        if (bArr4 == null) {
            bArr4 = new byte[i6 + 1];
        }
        int i7 = 0;
        boolean z2 = i3 == 1 || i3 == -1;
        int i8 = i5;
        int i9 = 0;
        int i10 = i4;
        for (int i11 = i6; i11 >= 0; i11--) {
            if (i10 >= 0) {
                if (i10 < length) {
                    i9 += bArr[i10];
                }
                i10--;
            }
            if (i8 >= 0) {
                if (i8 < length2) {
                    if (z2) {
                        if (i3 > 0) {
                            i9 += bArr2[i8];
                        } else {
                            i9 -= bArr2[i8];
                        }
                    } else {
                        i9 += bArr2[i8] * i3;
                    }
                }
                i8--;
            }
            if (i9 < 10 && i9 >= 0) {
                bArr4[i11] = (byte) i9;
                i9 = 0;
            } else {
                int i12 = i9 + 90;
                bArr4[i11] = bytedig[i12];
                i9 = bytecar[i12];
            }
        }
        if (i9 == 0) {
            return bArr4;
        }
        byte[] bArr5 = (z && i6 + 2 == bArr.length) ? bArr : null;
        if (bArr5 == null) {
            bArr3 = new byte[i6 + 2];
        } else {
            bArr3 = bArr5;
        }
        bArr3[0] = (byte) i9;
        if (i6 >= 10) {
            System.arraycopy(bArr4, 0, bArr3, 1, i6 + 1);
        } else {
            int i13 = i6 + 1;
            while (i13 > 0) {
                int i14 = i7 + 1;
                bArr3[i14] = bArr4[i7];
                i13--;
                i7 = i14;
            }
        }
        return bArr3;
    }

    private static final byte[] diginit() {
        byte[] bArr = new byte[190];
        for (int i = 0; i <= 189; i++) {
            int i2 = i - 90;
            if (i2 >= 0) {
                bArr[i] = (byte) (i2 % 10);
                bytecar[i] = (byte) (i2 / 10);
            } else {
                bArr[i] = (byte) ((i2 + 100) % 10);
                bytecar[i] = (byte) ((r2 / 10) - 10);
            }
        }
        return bArr;
    }

    private static final BigDecimal clone(BigDecimal bigDecimal) {
        BigDecimal bigDecimal2 = new BigDecimal();
        bigDecimal2.ind = bigDecimal.ind;
        bigDecimal2.exp = bigDecimal.exp;
        bigDecimal2.form = bigDecimal.form;
        bigDecimal2.mant = bigDecimal.mant;
        return bigDecimal2;
    }

    private void checkdigits(BigDecimal bigDecimal, int i) {
        if (i == 0) {
            return;
        }
        if (this.mant.length > i && !allzero(this.mant, i)) {
            throw new ArithmeticException("Too many digits: " + toString());
        }
        if (bigDecimal != null && bigDecimal.mant.length > i && !allzero(bigDecimal.mant, i)) {
            throw new ArithmeticException("Too many digits: " + bigDecimal.toString());
        }
    }

    private BigDecimal round(MathContext mathContext) {
        return round(mathContext.digits, mathContext.roundingMode);
    }

    private BigDecimal round(int i, int i2) {
        byte b;
        boolean z;
        int length = this.mant.length - i;
        if (length <= 0) {
            return this;
        }
        this.exp += length;
        byte b2 = this.ind;
        byte[] bArr = this.mant;
        if (i > 0) {
            this.mant = new byte[i];
            System.arraycopy(bArr, 0, this.mant, 0, i);
            b = bArr[i];
            z = true;
        } else {
            this.mant = ZERO.mant;
            this.ind = (byte) 0;
            if (i == 0) {
                b = bArr[0];
                z = false;
            } else {
                b = 0;
                z = false;
            }
        }
        if (i2 == 4) {
            if (b < 5) {
                b2 = 0;
            }
        } else {
            if (i2 == 7) {
                if (!allzero(bArr, i)) {
                    throw new ArithmeticException("Rounding necessary");
                }
            } else if (i2 == 5) {
                if (b <= 5 && (b != 5 || allzero(bArr, i + 1))) {
                }
            } else if (i2 == 6) {
                if (b <= 5 && (b != 5 || (allzero(bArr, i + 1) && this.mant[this.mant.length - 1] % 2 == 0))) {
                }
            } else if (i2 != 1) {
                if (i2 == 0) {
                    if (allzero(bArr, i)) {
                    }
                } else if (i2 == 2) {
                    if (b2 <= 0 || allzero(bArr, i)) {
                    }
                } else if (i2 == 3) {
                    if (b2 >= 0 || allzero(bArr, i)) {
                    }
                } else {
                    throw new IllegalArgumentException("Bad round value: " + i2);
                }
            }
            b2 = 0;
        }
        if (b2 != 0) {
            if (this.ind == 0) {
                this.mant = ONE.mant;
                this.ind = b2;
            } else {
                byte[] bArrByteaddsub = byteaddsub(this.mant, this.mant.length, ONE.mant, 1, this.ind == -1 ? -b2 : b2, z);
                if (bArrByteaddsub.length > this.mant.length) {
                    this.exp++;
                    System.arraycopy(bArrByteaddsub, 0, this.mant, 0, this.mant.length);
                } else {
                    this.mant = bArrByteaddsub;
                }
            }
        }
        if (this.exp > 999999999) {
            throw new ArithmeticException("Exponent Overflow: " + this.exp);
        }
        return this;
    }

    private static final boolean allzero(byte[] bArr, int i) {
        if (i < 0) {
            i = 0;
        }
        int length = bArr.length - 1;
        while (i <= length) {
            if (bArr[i] != 0) {
                return false;
            }
            i++;
        }
        return true;
    }

    private BigDecimal finish(MathContext mathContext, boolean z) {
        if (mathContext.digits != 0 && this.mant.length > mathContext.digits) {
            round(mathContext);
        }
        if (z && mathContext.form != 0) {
            int length = this.mant.length;
            for (int i = length - 1; i >= 1 && this.mant[i] == 0; i--) {
                length--;
                this.exp++;
            }
            if (length < this.mant.length) {
                byte[] bArr = new byte[length];
                System.arraycopy(this.mant, 0, bArr, 0, length);
                this.mant = bArr;
            }
        }
        this.form = (byte) 0;
        int length2 = this.mant.length;
        int i2 = 0;
        while (length2 > 0) {
            if (this.mant[i2] != 0) {
                if (i2 > 0) {
                    byte[] bArr2 = new byte[this.mant.length - i2];
                    System.arraycopy(this.mant, i2, bArr2, 0, this.mant.length - i2);
                    this.mant = bArr2;
                }
                int length3 = this.exp + this.mant.length;
                if (length3 > 0) {
                    if (length3 > mathContext.digits && mathContext.digits != 0) {
                        this.form = (byte) mathContext.form;
                    }
                    if (length3 - 1 <= 999999999) {
                        return this;
                    }
                } else if (length3 < -5) {
                    this.form = (byte) mathContext.form;
                }
                int i3 = length3 - 1;
                if ((i3 < -999999999) | (i3 > 999999999)) {
                    if (this.form == 2) {
                        int i4 = i3 % 3;
                        if (i4 < 0) {
                            i4 += 3;
                        }
                        i3 -= i4;
                        if (i3 >= -999999999) {
                        }
                    }
                    throw new ArithmeticException("Exponent Overflow: " + i3);
                }
                return this;
            }
            length2--;
            i2++;
        }
        this.ind = (byte) 0;
        if (mathContext.form != 0 || this.exp > 0) {
            this.exp = 0;
        } else if (this.exp < -999999999) {
            throw new ArithmeticException("Exponent Overflow: " + this.exp);
        }
        this.mant = ZERO.mant;
        return this;
    }
}
