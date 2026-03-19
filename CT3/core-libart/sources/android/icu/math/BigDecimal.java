package android.icu.math;

import android.icu.lang.UCharacter;
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

    public BigDecimal(java.math.BigDecimal bd) {
        this(bd.toString());
    }

    public BigDecimal(BigInteger bi) {
        this(bi.toString(10));
    }

    public BigDecimal(BigInteger bi, int scale) {
        this(bi.toString(10));
        if (scale < 0) {
            throw new NumberFormatException("Negative scale: " + scale);
        }
        this.exp = -scale;
    }

    public BigDecimal(char[] inchars) {
        this(inchars, 0, inchars.length);
    }

    public BigDecimal(char[] inchars, int offset, int length) {
        int $3;
        int i;
        int k;
        int dvalue;
        this.form = (byte) 0;
        if (length <= 0) {
            bad(inchars);
        }
        this.ind = (byte) 1;
        if (inchars[offset] == '-') {
            length--;
            if (length == 0) {
                bad(inchars);
            }
            this.ind = (byte) -1;
            offset++;
        } else if (inchars[offset] == '+') {
            length--;
            if (length == 0) {
                bad(inchars);
            }
            offset++;
        }
        boolean exotic = false;
        boolean hadexp = false;
        int d = 0;
        int dotoff = -1;
        int last = -1;
        int $1 = length;
        int i2 = offset;
        while ($1 > 0) {
            char si = inchars[i2];
            if (si >= '0' && si <= '9') {
                last = i2;
                d++;
            } else if (si == '.') {
                if (dotoff >= 0) {
                    bad(inchars);
                }
                dotoff = i2 - offset;
            } else if (si != 'e' && si != 'E') {
                if (!UCharacter.isDigit(si)) {
                    bad(inchars);
                }
                exotic = true;
                last = i2;
                d++;
            } else {
                if (i2 - offset > length - 2) {
                    bad(inchars);
                }
                boolean eneg = false;
                if (inchars[i2 + 1] == '-') {
                    eneg = true;
                    k = i2 + 2;
                } else if (inchars[i2 + 1] == '+') {
                    k = i2 + 2;
                } else {
                    k = i2 + 1;
                }
                int elen = length - (k - offset);
                if ((elen > 9) | (elen == 0)) {
                    bad(inchars);
                }
                int $2 = elen;
                int j = k;
                while ($2 > 0) {
                    char sj = inchars[j];
                    if (sj < '0') {
                        bad(inchars);
                    }
                    if (sj > '9') {
                        if (!UCharacter.isDigit(sj)) {
                            bad(inchars);
                        }
                        dvalue = UCharacter.digit(sj, 10);
                        if (dvalue < 0) {
                            bad(inchars);
                        }
                    } else {
                        dvalue = sj - '0';
                    }
                    this.exp = (this.exp * 10) + dvalue;
                    $2--;
                    j++;
                }
                if (eneg) {
                    this.exp = -this.exp;
                }
                hadexp = true;
                if (d == 0) {
                    bad(inchars);
                }
                if (dotoff >= 0) {
                    this.exp = (this.exp + dotoff) - d;
                }
                $3 = last - 1;
                for (i = offset; i <= $3; i++) {
                    char si2 = inchars[i];
                    if (si2 == '0') {
                        offset++;
                        dotoff--;
                        d--;
                    } else if (si2 == '.') {
                        offset++;
                        dotoff--;
                    } else {
                        if (si2 <= '9' || UCharacter.digit(si2, 10) != 0) {
                            break;
                        }
                        offset++;
                        dotoff--;
                        d--;
                    }
                }
                this.mant = new byte[d];
                int j2 = offset;
                if (!exotic) {
                    int $4 = d;
                    int i3 = 0;
                    while ($4 > 0) {
                        j2 = i3 == dotoff ? j2 + 1 : j2;
                        char sj2 = inchars[j2];
                        if (sj2 <= '9') {
                            this.mant[i3] = (byte) (sj2 - '0');
                        } else {
                            int dvalue2 = UCharacter.digit(sj2, 10);
                            if (dvalue2 < 0) {
                                bad(inchars);
                            }
                            this.mant[i3] = (byte) dvalue2;
                        }
                        j2++;
                        $4--;
                        i3++;
                    }
                } else {
                    int $5 = d;
                    int i4 = 0;
                    while ($5 > 0) {
                        if (i4 == dotoff) {
                            j2++;
                        }
                        this.mant[i4] = (byte) (inchars[j2] - '0');
                        j2++;
                        $5--;
                        i4++;
                    }
                }
                if (this.mant[0] != 0) {
                    this.ind = (byte) 0;
                    if (this.exp > 0) {
                        this.exp = 0;
                    }
                    if (hadexp) {
                        this.mant = ZERO.mant;
                        this.exp = 0;
                        return;
                    }
                    return;
                }
                if (hadexp) {
                    this.form = (byte) 1;
                    int mag = (this.exp + this.mant.length) - 1;
                    if ((mag > 999999999) | (mag < -999999999)) {
                        bad(inchars);
                        return;
                    }
                    return;
                }
                return;
            }
            $1--;
            i2++;
        }
        if (d == 0) {
        }
        if (dotoff >= 0) {
        }
        $3 = last - 1;
        while (i <= $3) {
        }
        this.mant = new byte[d];
        int j22 = offset;
        if (!exotic) {
        }
        if (this.mant[0] != 0) {
        }
    }

    public BigDecimal(double num) {
        this(new java.math.BigDecimal(num).toString());
    }

    public BigDecimal(int num) {
        this.form = (byte) 0;
        if (num <= 9 && num >= -9) {
            if (num == 0) {
                this.mant = ZERO.mant;
                this.ind = (byte) 0;
                return;
            }
            if (num == 1) {
                this.mant = ONE.mant;
                this.ind = (byte) 1;
                return;
            }
            if (num == -1) {
                this.mant = ONE.mant;
                this.ind = (byte) -1;
                return;
            }
            this.mant = new byte[1];
            if (num > 0) {
                this.mant[0] = (byte) num;
                this.ind = (byte) 1;
                return;
            } else {
                this.mant[0] = (byte) (-num);
                this.ind = (byte) -1;
                return;
            }
        }
        if (num > 0) {
            this.ind = (byte) 1;
            num = -num;
        } else {
            this.ind = (byte) -1;
        }
        int mun = num;
        int i = 9;
        while (true) {
            mun /= 10;
            if (mun == 0) {
                break;
            } else {
                i--;
            }
        }
        this.mant = new byte[10 - i];
        int i2 = (10 - i) - 1;
        while (true) {
            this.mant[i2] = (byte) (-((byte) (num % 10)));
            num /= 10;
            if (num != 0) {
                i2--;
            } else {
                return;
            }
        }
    }

    public BigDecimal(long num) {
        this.form = (byte) 0;
        if (num > 0) {
            this.ind = (byte) 1;
            num = -num;
        } else if (num == 0) {
            this.ind = (byte) 0;
        } else {
            this.ind = (byte) -1;
        }
        long mun = num;
        int i = 18;
        while (true) {
            mun /= 10;
            if (mun == 0) {
                break;
            } else {
                i--;
            }
        }
        this.mant = new byte[19 - i];
        int i2 = (19 - i) - 1;
        while (true) {
            this.mant[i2] = (byte) (-((byte) (num % 10)));
            num /= 10;
            if (num != 0) {
                i2--;
            } else {
                return;
            }
        }
    }

    public BigDecimal(String string) {
        this(string.toCharArray(), 0, string.length());
    }

    private BigDecimal() {
        this.form = (byte) 0;
    }

    public BigDecimal abs() {
        return abs(plainMC);
    }

    public BigDecimal abs(MathContext set) {
        if (this.ind == -1) {
            return negate(set);
        }
        return plus(set);
    }

    public BigDecimal add(BigDecimal rhs) {
        return add(rhs, plainMC);
    }

    public BigDecimal add(BigDecimal rhs, MathContext set) {
        int mult;
        byte b;
        byte b2;
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        BigDecimal lhs = this;
        if (this.ind == 0 && set.form != 0) {
            return rhs.plus(set);
        }
        if (rhs.ind == 0 && set.form != 0) {
            return plus(set);
        }
        int reqdig = set.digits;
        if (reqdig > 0) {
            if (this.mant.length > reqdig) {
                lhs = clone(this).round(set);
            }
            if (rhs.mant.length > reqdig) {
                rhs = clone(rhs).round(set);
            }
        }
        BigDecimal res = new BigDecimal();
        byte[] usel = lhs.mant;
        int usellen = lhs.mant.length;
        byte[] user = rhs.mant;
        int userlen = rhs.mant.length;
        if (lhs.exp == rhs.exp) {
            res.exp = lhs.exp;
        } else if (lhs.exp > rhs.exp) {
            int newlen = (lhs.exp + usellen) - rhs.exp;
            if (newlen >= userlen + reqdig + 1 && reqdig > 0) {
                res.mant = usel;
                res.exp = lhs.exp;
                res.ind = lhs.ind;
                if (usellen < reqdig) {
                    res.mant = extend(lhs.mant, reqdig);
                    res.exp -= reqdig - usellen;
                }
                return res.finish(set, false);
            }
            res.exp = rhs.exp;
            if (newlen > reqdig + 1 && reqdig > 0) {
                int tlen = (newlen - reqdig) - 1;
                userlen -= tlen;
                res.exp += tlen;
                newlen = reqdig + 1;
            }
            if (newlen > usellen) {
                usellen = newlen;
            }
        } else {
            int newlen2 = (rhs.exp + userlen) - lhs.exp;
            if (newlen2 >= usellen + reqdig + 1 && reqdig > 0) {
                res.mant = user;
                res.exp = rhs.exp;
                res.ind = rhs.ind;
                if (userlen < reqdig) {
                    res.mant = extend(rhs.mant, reqdig);
                    res.exp -= reqdig - userlen;
                }
                return res.finish(set, false);
            }
            res.exp = lhs.exp;
            if (newlen2 > reqdig + 1 && reqdig > 0) {
                int tlen2 = (newlen2 - reqdig) - 1;
                usellen -= tlen2;
                res.exp += tlen2;
                newlen2 = reqdig + 1;
            }
            if (newlen2 > userlen) {
                userlen = newlen2;
            }
        }
        if (lhs.ind == 0) {
            res.ind = (byte) 1;
        } else {
            res.ind = lhs.ind;
        }
        if ((lhs.ind == -1) == (rhs.ind == -1)) {
            mult = 1;
        } else {
            mult = -1;
            if (rhs.ind != 0) {
                if ((usellen < userlen) | (lhs.ind == 0)) {
                    usel = user;
                    user = usel;
                    int tlen3 = usellen;
                    usellen = userlen;
                    userlen = tlen3;
                    res.ind = (byte) (-res.ind);
                } else if (usellen <= userlen) {
                    int ia = 0;
                    int ib = 0;
                    int ea = usel.length - 1;
                    int eb = user.length - 1;
                    while (true) {
                        if (ia <= ea) {
                            b = usel[ia];
                        } else if (ib > eb) {
                            if (set.form != 0) {
                                return ZERO;
                            }
                        } else {
                            b = 0;
                        }
                        if (ib <= eb) {
                            b2 = user[ib];
                        } else {
                            b2 = 0;
                        }
                        if (b != b2) {
                            if (b < b2) {
                                usel = user;
                                user = usel;
                                int tlen4 = usellen;
                                usellen = userlen;
                                userlen = tlen4;
                                res.ind = (byte) (-res.ind);
                            }
                        } else {
                            ia++;
                            ib++;
                        }
                    }
                }
            }
        }
        res.mant = byteaddsub(usel, usellen, user, userlen, mult, false);
        return res.finish(set, false);
    }

    @Override
    public int compareTo(BigDecimal rhs) {
        return compareTo(rhs, plainMC);
    }

    public int compareTo(BigDecimal rhs, MathContext set) {
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        if ((this.ind == rhs.ind) & (this.exp == rhs.exp)) {
            int thislength = this.mant.length;
            if (thislength < rhs.mant.length) {
                return (byte) (-this.ind);
            }
            if (thislength > rhs.mant.length) {
                return this.ind;
            }
            if ((thislength <= set.digits) | (set.digits == 0)) {
                int $6 = thislength;
                int i = 0;
                while ($6 > 0) {
                    if (this.mant[i] < rhs.mant[i]) {
                        return (byte) (-this.ind);
                    }
                    if (this.mant[i] <= rhs.mant[i]) {
                        $6--;
                        i++;
                    } else {
                        return this.ind;
                    }
                }
                return 0;
            }
        } else {
            if (this.ind < rhs.ind) {
                return -1;
            }
            if (this.ind > rhs.ind) {
                return 1;
            }
        }
        BigDecimal newrhs = clone(rhs);
        newrhs.ind = (byte) (-newrhs.ind);
        return add(newrhs, set).ind;
    }

    public BigDecimal divide(BigDecimal rhs) {
        return dodivide('D', rhs, plainMC, -1);
    }

    public BigDecimal divide(BigDecimal rhs, int round) {
        MathContext set = new MathContext(0, 0, false, round);
        return dodivide('D', rhs, set, -1);
    }

    public BigDecimal divide(BigDecimal rhs, int scale, int round) {
        if (scale < 0) {
            throw new ArithmeticException("Negative scale: " + scale);
        }
        MathContext set = new MathContext(0, 0, false, round);
        return dodivide('D', rhs, set, scale);
    }

    public BigDecimal divide(BigDecimal rhs, MathContext set) {
        return dodivide('D', rhs, set, -1);
    }

    public BigDecimal divideInteger(BigDecimal rhs) {
        return dodivide('I', rhs, plainMC, 0);
    }

    public BigDecimal divideInteger(BigDecimal rhs, MathContext set) {
        return dodivide('I', rhs, set, 0);
    }

    public BigDecimal max(BigDecimal rhs) {
        return max(rhs, plainMC);
    }

    public BigDecimal max(BigDecimal rhs, MathContext set) {
        if (compareTo(rhs, set) >= 0) {
            return plus(set);
        }
        return rhs.plus(set);
    }

    public BigDecimal min(BigDecimal rhs) {
        return min(rhs, plainMC);
    }

    public BigDecimal min(BigDecimal rhs, MathContext set) {
        if (compareTo(rhs, set) <= 0) {
            return plus(set);
        }
        return rhs.plus(set);
    }

    public BigDecimal multiply(BigDecimal rhs) {
        return multiply(rhs, plainMC);
    }

    public BigDecimal multiply(BigDecimal rhs, MathContext set) {
        byte[] multer;
        byte[] multand;
        int acclen;
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        BigDecimal lhs = this;
        int padding = 0;
        int reqdig = set.digits;
        if (reqdig > 0) {
            if (this.mant.length > reqdig) {
                lhs = clone(this).round(set);
            }
            if (rhs.mant.length > reqdig) {
                rhs = clone(rhs).round(set);
            }
        } else {
            if (this.exp > 0) {
                padding = this.exp + 0;
            }
            if (rhs.exp > 0) {
                padding += rhs.exp;
            }
        }
        if (lhs.mant.length < rhs.mant.length) {
            multer = lhs.mant;
            multand = rhs.mant;
        } else {
            multer = rhs.mant;
            multand = lhs.mant;
        }
        int multandlen = (multer.length + multand.length) - 1;
        if (multer[0] * multand[0] > 9) {
            acclen = multandlen + 1;
        } else {
            acclen = multandlen;
        }
        BigDecimal res = new BigDecimal();
        byte[] acc = new byte[acclen];
        int $7 = multer.length;
        int n = 0;
        while ($7 > 0) {
            byte mult = multer[n];
            if (mult != 0) {
                acc = byteaddsub(acc, acc.length, multand, multandlen, mult, true);
            }
            multandlen--;
            $7--;
            n++;
        }
        res.ind = (byte) (lhs.ind * rhs.ind);
        res.exp = (lhs.exp + rhs.exp) - padding;
        if (padding == 0) {
            res.mant = acc;
        } else {
            res.mant = extend(acc, acc.length + padding);
        }
        return res.finish(set, false);
    }

    public BigDecimal negate() {
        return negate(plainMC);
    }

    public BigDecimal negate(MathContext set) {
        if (set.lostDigits) {
            checkdigits((BigDecimal) null, set.digits);
        }
        BigDecimal res = clone(this);
        res.ind = (byte) (-res.ind);
        return res.finish(set, false);
    }

    public BigDecimal plus() {
        return plus(plainMC);
    }

    public BigDecimal plus(MathContext set) {
        if (set.lostDigits) {
            checkdigits((BigDecimal) null, set.digits);
        }
        if (set.form == 0 && this.form == 0 && (this.mant.length <= set.digits || set.digits == 0)) {
            return this;
        }
        return clone(this).finish(set, false);
    }

    public BigDecimal pow(BigDecimal rhs) {
        return pow(rhs, plainMC);
    }

    public BigDecimal pow(BigDecimal rhs, MathContext set) {
        int workdigits;
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        int n = rhs.intcheck(-999999999, 999999999);
        BigDecimal lhs = this;
        int reqdig = set.digits;
        if (reqdig == 0) {
            if (rhs.ind == -1) {
                throw new ArithmeticException("Negative power: " + rhs.toString());
            }
            workdigits = 0;
        } else {
            if (rhs.mant.length + rhs.exp > reqdig) {
                throw new ArithmeticException("Too many digits: " + rhs.toString());
            }
            if (this.mant.length > reqdig) {
                lhs = clone(this).round(set);
            }
            int L = rhs.mant.length + rhs.exp;
            workdigits = reqdig + L + 1;
        }
        MathContext workset = new MathContext(workdigits, set.form, false, set.roundingMode);
        BigDecimal res = ONE;
        if (n == 0) {
            return res;
        }
        if (n < 0) {
            n = -n;
        }
        boolean seenbit = false;
        int i = 1;
        while (true) {
            n += n;
            if (n < 0) {
                seenbit = true;
                res = res.multiply(lhs, workset);
            }
            if (i == 31) {
                break;
            }
            if (seenbit) {
                res = res.multiply(res, workset);
            }
            i++;
        }
        if (rhs.ind < 0) {
            res = ONE.divide(res, workset);
        }
        return res.finish(set, true);
    }

    public BigDecimal remainder(BigDecimal rhs) {
        return dodivide('R', rhs, plainMC, -1);
    }

    public BigDecimal remainder(BigDecimal rhs, MathContext set) {
        return dodivide('R', rhs, set, -1);
    }

    public BigDecimal subtract(BigDecimal rhs) {
        return subtract(rhs, plainMC);
    }

    public BigDecimal subtract(BigDecimal rhs, MathContext set) {
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        BigDecimal newrhs = clone(rhs);
        newrhs.ind = (byte) (-newrhs.ind);
        return add(newrhs, set);
    }

    public byte byteValueExact() {
        int num = intValueExact();
        if ((num < -128) | (num > 127)) {
            throw new ArithmeticException("Conversion overflow: " + toString());
        }
        return (byte) num;
    }

    @Override
    public double doubleValue() {
        return Double.valueOf(toString()).doubleValue();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BigDecimal)) {
            return false;
        }
        BigDecimal rhs = (BigDecimal) obj;
        if (this.ind != rhs.ind) {
            return false;
        }
        if ((this.form == rhs.form) & (this.exp == rhs.exp) & (this.mant.length == rhs.mant.length)) {
            int $8 = this.mant.length;
            int i = 0;
            while ($8 > 0) {
                if (this.mant[i] != rhs.mant[i]) {
                    return false;
                }
                $8--;
                i++;
            }
        } else {
            char[] lca = layout();
            char[] rca = rhs.layout();
            if (lca.length != rca.length) {
                return false;
            }
            int $9 = lca.length;
            int i2 = 0;
            while ($9 > 0) {
                if (lca[i2] != rca[i2]) {
                    return false;
                }
                $9--;
                i2++;
            }
        }
        return true;
    }

    @Override
    public float floatValue() {
        return Float.valueOf(toString()).floatValue();
    }

    public String format(int before, int after) {
        return format(before, after, -1, -1, 1, 4);
    }

    public String format(int before, int after, int explaces, int exdigits, int exformint, int exround) {
        int thisafter;
        if ((before == 0) | (before < -1)) {
            badarg("format", 1, String.valueOf(before));
        }
        if (after < -1) {
            badarg("format", 2, String.valueOf(after));
        }
        if ((explaces == 0) | (explaces < -1)) {
            badarg("format", 3, String.valueOf(explaces));
        }
        if (exdigits < -1) {
            badarg("format", 4, String.valueOf(explaces));
        }
        if (exformint != 1 && exformint != 2) {
            if (exformint == -1) {
                exformint = 1;
            } else {
                badarg("format", 5, String.valueOf(exformint));
            }
        }
        if (exround != 4) {
            if (exround == -1) {
                exround = 4;
            } else {
                try {
                    new MathContext(9, 1, false, exround);
                } catch (IllegalArgumentException e) {
                    badarg("format", 6, String.valueOf(exround));
                }
            }
        }
        BigDecimal num = clone(this);
        if (exdigits == -1 || num.ind == 0) {
            num.form = (byte) 0;
        } else {
            int mag = num.exp + num.mant.length;
            if (mag > exdigits || mag < -5) {
                num.form = (byte) exformint;
            } else {
                num.form = (byte) 0;
            }
        }
        if (after >= 0) {
            while (true) {
                if (num.form == 0) {
                    thisafter = -num.exp;
                } else if (num.form == 1) {
                    thisafter = num.mant.length - 1;
                } else {
                    int lead = ((num.exp + num.mant.length) - 1) % 3;
                    if (lead < 0) {
                        lead += 3;
                    }
                    int lead2 = lead + 1;
                    if (lead2 >= num.mant.length) {
                        thisafter = 0;
                    } else {
                        thisafter = num.mant.length - lead2;
                    }
                }
                if (thisafter == after) {
                    break;
                }
                if (thisafter < after) {
                    byte[] newmant = extend(num.mant, (num.mant.length + after) - thisafter);
                    num.mant = newmant;
                    num.exp -= after - thisafter;
                    if (num.exp < -999999999) {
                        throw new ArithmeticException("Exponent Overflow: " + num.exp);
                    }
                } else {
                    int chop = thisafter - after;
                    if (chop > num.mant.length) {
                        num.mant = ZERO.mant;
                        num.ind = (byte) 0;
                        num.exp = 0;
                    } else {
                        int need = num.mant.length - chop;
                        int oldexp = num.exp;
                        num.round(need, exround);
                        if (num.exp - oldexp == chop) {
                            break;
                        }
                    }
                }
            }
        }
        char[] a = num.layout();
        if (before > 0) {
            int $11 = a.length;
            int p = 0;
            while ($11 > 0 && a[p] != '.' && a[p] != 'E') {
                $11--;
                p++;
            }
            if (p > before) {
                badarg("format", 1, String.valueOf(before));
            }
            if (p < before) {
                char[] newa = new char[(a.length + before) - p];
                int $12 = before - p;
                int i = 0;
                while ($12 > 0) {
                    newa[i] = ' ';
                    $12--;
                    i++;
                }
                System.arraycopy(a, 0, newa, i, a.length);
                a = newa;
            }
        }
        if (explaces > 0) {
            int $13 = a.length - 1;
            int p2 = a.length - 1;
            while ($13 > 0 && a[p2] != 'E') {
                $13--;
                p2--;
            }
            if (p2 == 0) {
                char[] newa2 = new char[a.length + explaces + 2];
                System.arraycopy(a, 0, newa2, 0, a.length);
                int $14 = explaces + 2;
                int i2 = a.length;
                while ($14 > 0) {
                    newa2[i2] = ' ';
                    $14--;
                    i2++;
                }
                a = newa2;
            } else {
                int places = (a.length - p2) - 2;
                if (places > explaces) {
                    badarg("format", 3, String.valueOf(explaces));
                }
                if (places < explaces) {
                    char[] newa3 = new char[(a.length + explaces) - places];
                    System.arraycopy(a, 0, newa3, 0, p2 + 2);
                    int $15 = explaces - places;
                    int i3 = p2 + 2;
                    while ($15 > 0) {
                        newa3[i3] = '0';
                        $15--;
                        i3++;
                    }
                    System.arraycopy(a, p2 + 2, newa3, i3, places);
                    a = newa3;
                }
            }
        }
        return new String(a);
    }

    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public int intValue() {
        return toBigInteger().intValue();
    }

    public int intValueExact() {
        int useexp;
        if (this.ind == 0) {
            return 0;
        }
        int lodigit = this.mant.length - 1;
        if (this.exp < 0) {
            lodigit += this.exp;
            if (!allzero(this.mant, lodigit + 1)) {
                throw new ArithmeticException("Decimal part non-zero: " + toString());
            }
            if (lodigit < 0) {
                return 0;
            }
            useexp = 0;
        } else {
            if (this.exp + lodigit > 9) {
                throw new ArithmeticException("Conversion overflow: " + toString());
            }
            useexp = this.exp;
        }
        int result = 0;
        int $16 = lodigit + useexp;
        for (int i = 0; i <= $16; i++) {
            result *= 10;
            if (i <= lodigit) {
                result += this.mant[i];
            }
        }
        if (lodigit + useexp == 9) {
            int topdig = result / 1000000000;
            if (topdig != this.mant[0]) {
                if (result == Integer.MIN_VALUE && this.ind == -1 && this.mant[0] == 2) {
                    return result;
                }
                throw new ArithmeticException("Conversion overflow: " + toString());
            }
        }
        if (this.ind == 1) {
            return result;
        }
        return -result;
    }

    @Override
    public long longValue() {
        return toBigInteger().longValue();
    }

    public long longValueExact() {
        int useexp;
        int cstart;
        if (this.ind == 0) {
            return 0L;
        }
        int lodigit = this.mant.length - 1;
        if (this.exp < 0) {
            lodigit += this.exp;
            if (lodigit < 0) {
                cstart = 0;
            } else {
                cstart = lodigit + 1;
            }
            if (!allzero(this.mant, cstart)) {
                throw new ArithmeticException("Decimal part non-zero: " + toString());
            }
            if (lodigit < 0) {
                return 0L;
            }
            useexp = 0;
        } else {
            if (this.exp + this.mant.length > 18) {
                throw new ArithmeticException("Conversion overflow: " + toString());
            }
            useexp = this.exp;
        }
        long result = 0;
        int $17 = lodigit + useexp;
        for (int i = 0; i <= $17; i++) {
            result *= 10;
            if (i <= lodigit) {
                result += (long) this.mant[i];
            }
        }
        if (lodigit + useexp == 18) {
            long topdig = result / 1000000000000000000L;
            if (topdig != this.mant[0]) {
                if (result == Long.MIN_VALUE && this.ind == -1 && this.mant[0] == 9) {
                    return result;
                }
                throw new ArithmeticException("Conversion overflow: " + toString());
            }
        }
        if (this.ind == 1) {
            return result;
        }
        return -result;
    }

    public BigDecimal movePointLeft(int n) {
        BigDecimal res = clone(this);
        res.exp -= n;
        return res.finish(plainMC, false);
    }

    public BigDecimal movePointRight(int n) {
        BigDecimal res = clone(this);
        res.exp += n;
        return res.finish(plainMC, false);
    }

    public int scale() {
        if (this.exp >= 0) {
            return 0;
        }
        return -this.exp;
    }

    public BigDecimal setScale(int scale) {
        return setScale(scale, 7);
    }

    public BigDecimal setScale(int scale, int round) {
        int padding;
        int ourscale = scale();
        if (ourscale == scale && this.form == 0) {
            return this;
        }
        BigDecimal res = clone(this);
        if (ourscale <= scale) {
            if (ourscale == 0) {
                padding = res.exp + scale;
            } else {
                padding = scale - ourscale;
            }
            res.mant = extend(res.mant, res.mant.length + padding);
            res.exp = -scale;
        } else {
            if (scale < 0) {
                throw new ArithmeticException("Negative scale: " + scale);
            }
            int newlen = res.mant.length - (ourscale - scale);
            res = res.round(newlen, round);
            if (res.exp != (-scale)) {
                res.mant = extend(res.mant, res.mant.length + 1);
                res.exp--;
            }
        }
        res.form = (byte) 0;
        return res;
    }

    public short shortValueExact() {
        int num = intValueExact();
        if ((num < -32768) | (num > 32767)) {
            throw new ArithmeticException("Conversion overflow: " + toString());
        }
        return (short) num;
    }

    public int signum() {
        return this.ind;
    }

    public java.math.BigDecimal toBigDecimal() {
        return new java.math.BigDecimal(unscaledValue(), scale());
    }

    public BigInteger toBigInteger() {
        BigDecimal res;
        if ((this.exp >= 0) & (this.form == 0)) {
            res = this;
        } else if (this.exp >= 0) {
            res = clone(this);
            res.form = (byte) 0;
        } else if ((-this.exp) >= this.mant.length) {
            res = ZERO;
        } else {
            res = clone(this);
            int newlen = res.mant.length + res.exp;
            byte[] newmant = new byte[newlen];
            System.arraycopy(res.mant, 0, newmant, 0, newlen);
            res.mant = newmant;
            res.form = (byte) 0;
            res.exp = 0;
        }
        return new BigInteger(new String(res.layout()));
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
        BigDecimal res;
        if (this.exp >= 0) {
            res = this;
        } else {
            res = clone(this);
            res.exp = 0;
        }
        return res.toBigInteger();
    }

    public static BigDecimal valueOf(double dub) {
        return new BigDecimal(new Double(dub).toString());
    }

    public static BigDecimal valueOf(long lint) {
        return valueOf(lint, 0);
    }

    public static BigDecimal valueOf(long lint, int scale) {
        BigDecimal res;
        if (lint == 0) {
            res = ZERO;
        } else if (lint == 1) {
            res = ONE;
        } else if (lint == 10) {
            res = TEN;
        } else {
            res = new BigDecimal(lint);
        }
        if (scale == 0) {
            return res;
        }
        if (scale < 0) {
            throw new NumberFormatException("Negative scale: " + scale);
        }
        BigDecimal res2 = clone(res);
        res2.exp = -scale;
        return res2;
    }

    private char[] layout() {
        char csign;
        char[] cmant = new char[this.mant.length];
        int $18 = this.mant.length;
        int i = 0;
        while ($18 > 0) {
            cmant[i] = (char) (this.mant[i] + 48);
            $18--;
            i++;
        }
        if (this.form != 0) {
            StringBuilder sb = new StringBuilder(cmant.length + 15);
            if (this.ind == -1) {
                sb.append('-');
            }
            int euse = (this.exp + cmant.length) - 1;
            if (this.form == 1) {
                sb.append(cmant[0]);
                if (cmant.length > 1) {
                    sb.append('.').append(cmant, 1, cmant.length - 1);
                }
            } else {
                int sig = euse % 3;
                if (sig < 0) {
                    sig += 3;
                }
                euse -= sig;
                int sig2 = sig + 1;
                if (sig2 >= cmant.length) {
                    sb.append(cmant, 0, cmant.length);
                    for (int $19 = sig2 - cmant.length; $19 > 0; $19--) {
                        sb.append('0');
                    }
                } else {
                    sb.append(cmant, 0, sig2).append('.').append(cmant, sig2, cmant.length - sig2);
                }
            }
            if (euse != 0) {
                if (euse < 0) {
                    csign = '-';
                    euse = -euse;
                } else {
                    csign = '+';
                }
                sb.append('E').append(csign).append(euse);
            }
            char[] rec = new char[sb.length()];
            int srcEnd = sb.length();
            if (srcEnd != 0) {
                sb.getChars(0, srcEnd, rec, 0);
            }
            return rec;
        }
        if (this.exp == 0) {
            if (this.ind >= 0) {
                return cmant;
            }
            char[] rec2 = new char[cmant.length + 1];
            rec2[0] = '-';
            System.arraycopy(cmant, 0, rec2, 1, cmant.length);
            return rec2;
        }
        int needsign = this.ind == -1 ? 1 : 0;
        int mag = this.exp + cmant.length;
        if (mag < 1) {
            int len = (needsign + 2) - this.exp;
            char[] rec3 = new char[len];
            if (needsign != 0) {
                rec3[0] = '-';
            }
            rec3[needsign] = '0';
            rec3[needsign + 1] = '.';
            int $20 = -mag;
            int i2 = needsign + 2;
            while ($20 > 0) {
                rec3[i2] = '0';
                $20--;
                i2++;
            }
            System.arraycopy(cmant, 0, rec3, (needsign + 2) - mag, cmant.length);
            return rec3;
        }
        if (mag > cmant.length) {
            int len2 = needsign + mag;
            char[] rec4 = new char[len2];
            if (needsign != 0) {
                rec4[0] = '-';
            }
            System.arraycopy(cmant, 0, rec4, needsign, cmant.length);
            int $21 = mag - cmant.length;
            int i3 = needsign + cmant.length;
            while ($21 > 0) {
                rec4[i3] = '0';
                $21--;
                i3++;
            }
            return rec4;
        }
        int len3 = needsign + 1 + cmant.length;
        char[] rec5 = new char[len3];
        if (needsign != 0) {
            rec5[0] = '-';
        }
        System.arraycopy(cmant, 0, rec5, needsign, mag);
        rec5[needsign + mag] = '.';
        System.arraycopy(cmant, mag, rec5, needsign + mag + 1, cmant.length - mag);
        return rec5;
    }

    private int intcheck(int min, int max) {
        int i = intValueExact();
        if ((i > max) | (i < min)) {
            throw new ArithmeticException("Conversion overflow: " + i);
        }
        return i;
    }

    private BigDecimal dodivide(char code, BigDecimal rhs, MathContext set, int scale) {
        int i;
        char c;
        if (set.lostDigits) {
            checkdigits(rhs, set.digits);
        }
        BigDecimal lhs = this;
        if (rhs.ind == 0) {
            throw new ArithmeticException("Divide by 0");
        }
        if (this.ind == 0) {
            if (set.form != 0) {
                return ZERO;
            }
            if (scale == -1) {
                return this;
            }
            return setScale(scale);
        }
        int reqdig = set.digits;
        if (reqdig > 0) {
            if (this.mant.length > reqdig) {
                lhs = clone(this).round(set);
            }
            if (rhs.mant.length > reqdig) {
                rhs = clone(rhs).round(set);
            }
        } else {
            if (scale == -1) {
                scale = scale();
            }
            int reqdig2 = this.mant.length;
            if (scale != (-this.exp)) {
                reqdig2 = reqdig2 + scale + this.exp;
            }
            reqdig = (reqdig2 - (rhs.mant.length - 1)) - rhs.exp;
            if (reqdig < this.mant.length) {
                reqdig = this.mant.length;
            }
            if (reqdig < rhs.mant.length) {
                reqdig = rhs.mant.length;
            }
        }
        int newexp = ((lhs.exp - rhs.exp) + lhs.mant.length) - rhs.mant.length;
        if (newexp < 0 && code != 'D') {
            if (code == 'I') {
                return ZERO;
            }
            return clone(lhs).finish(set, false);
        }
        BigDecimal res = new BigDecimal();
        res.ind = (byte) (lhs.ind * rhs.ind);
        res.exp = newexp;
        res.mant = new byte[reqdig + 1];
        int newlen = reqdig + reqdig + 1;
        byte[] bArrExtend = extend(lhs.mant, newlen);
        int var1len = newlen;
        byte[] var2 = rhs.mant;
        int var2len = newlen;
        int b2b = (var2[0] * 10) + 1;
        if (var2.length > 1) {
            b2b += var2[1];
        }
        int have = 0;
        loop0: while (true) {
            int thisdigit = 0;
            while (var1len >= var2len) {
                if (var1len == var2len) {
                    int $22 = var1len;
                    int i2 = 0;
                    while ($22 > 0) {
                        if (i2 < var2.length) {
                            c = var2[i2];
                        } else {
                            c = 0;
                        }
                        if (bArrExtend[i2] < c) {
                            break;
                        }
                        if (bArrExtend[i2] <= c) {
                            $22--;
                            i2++;
                        } else {
                            i = bArrExtend[0];
                        }
                    }
                    res.mant[have] = (byte) (thisdigit + 1);
                    have++;
                    bArrExtend[0] = 0;
                    break loop0;
                }
                i = bArrExtend[0] * 10;
                if (var1len > 1) {
                    i += bArrExtend[1];
                }
                int mult = (i * 10) / b2b;
                if (mult == 0) {
                    mult = 1;
                }
                thisdigit += mult;
                bArrExtend = byteaddsub(bArrExtend, var1len, var2, var2len, -mult, true);
                if (bArrExtend[0] == 0) {
                    int $23 = var1len - 2;
                    int start = 0;
                    while (start <= $23 && bArrExtend[start] == 0) {
                        var1len--;
                        start++;
                    }
                    if (start != 0) {
                        System.arraycopy(bArrExtend, start, bArrExtend, 0, var1len);
                    }
                }
            }
            if ((thisdigit != 0) | (have != 0)) {
                res.mant[have] = (byte) thisdigit;
                have++;
                if (have == reqdig + 1 || bArrExtend[0] == 0) {
                    break;
                }
                if ((scale >= 0 && (-res.exp) > scale) || (code != 'D' && res.exp <= 0)) {
                    break;
                }
                res.exp--;
                var2len--;
            }
        }
        if (have == 0) {
            have = 1;
        }
        if ((code == 'R') | (code == 'I')) {
            if (res.exp + have > reqdig) {
                throw new ArithmeticException("Integer overflow");
            }
            if (code == 'R') {
                if (res.mant[0] == 0) {
                    return clone(lhs).finish(set, false);
                }
                if (bArrExtend[0] == 0) {
                    return ZERO;
                }
                res.ind = lhs.ind;
                int padding = ((reqdig + reqdig) + 1) - lhs.mant.length;
                res.exp = (res.exp - padding) + lhs.exp;
                int d = var1len;
                for (int i3 = var1len - 1; i3 >= 1; i3--) {
                    if (((res.exp >= lhs.exp) || (res.exp >= rhs.exp)) || bArrExtend[i3] != 0) {
                        break;
                    }
                    d--;
                    res.exp++;
                }
                if (d < bArrExtend.length) {
                    byte[] newvar1 = new byte[d];
                    System.arraycopy(bArrExtend, 0, newvar1, 0, d);
                    bArrExtend = newvar1;
                }
                res.mant = bArrExtend;
                return res.finish(set, false);
            }
        } else if (bArrExtend[0] != 0) {
            byte lasthave = res.mant[have - 1];
            if (lasthave % 5 == 0) {
                res.mant[have - 1] = (byte) (lasthave + 1);
            }
        }
        if (scale >= 0) {
            if (have != res.mant.length) {
                res.exp -= res.mant.length - have;
            }
            int actdig = res.mant.length - ((-res.exp) - scale);
            res.round(actdig, set.roundingMode);
            if (res.exp != (-scale)) {
                res.mant = extend(res.mant, res.mant.length + 1);
                res.exp--;
            }
            return res.finish(set, true);
        }
        if (have == res.mant.length) {
            res.round(set);
        } else {
            if (res.mant[0] == 0) {
                return ZERO;
            }
            byte[] newmant = new byte[have];
            System.arraycopy(res.mant, 0, newmant, 0, have);
            res.mant = newmant;
        }
        return res.finish(set, true);
    }

    private void bad(char[] s) {
        throw new NumberFormatException("Not a number: " + String.valueOf(s));
    }

    private void badarg(String name, int pos, String value) {
        throw new IllegalArgumentException("Bad argument " + pos + " to " + name + ": " + value);
    }

    private static final byte[] extend(byte[] inarr, int newlen) {
        if (inarr.length == newlen) {
            return inarr;
        }
        byte[] newarr = new byte[newlen];
        System.arraycopy(inarr, 0, newarr, 0, inarr.length);
        return newarr;
    }

    private static final byte[] byteaddsub(byte[] a, int avlen, byte[] b, int bvlen, int m, boolean reuse) {
        int alength = a.length;
        int blength = b.length;
        int ap = avlen - 1;
        int bp = bvlen - 1;
        int maxarr = bp;
        if (bp < ap) {
            maxarr = ap;
        }
        byte[] reb = (byte[]) null;
        if (reuse && maxarr + 1 == alength) {
            reb = a;
        }
        if (reb == null) {
            reb = new byte[maxarr + 1];
        }
        boolean quickm = false;
        if (m == 1 || m == -1) {
            quickm = true;
        }
        int i = 0;
        for (int op = maxarr; op >= 0; op--) {
            if (ap >= 0) {
                if (ap < alength) {
                    i += a[ap];
                }
                ap--;
            }
            if (bp >= 0) {
                if (bp < blength) {
                    if (quickm) {
                        if (m > 0) {
                            i += b[bp];
                        } else {
                            i -= b[bp];
                        }
                    } else {
                        i += b[bp] * m;
                    }
                }
                bp--;
            }
            if (i < 10 && i >= 0) {
                reb[op] = (byte) i;
                i = 0;
            } else {
                int dp90 = i + 90;
                reb[op] = bytedig[dp90];
                i = bytecar[dp90];
            }
        }
        if (i == 0) {
            return reb;
        }
        byte[] newarr = (byte[]) null;
        if (reuse && maxarr + 2 == a.length) {
            newarr = a;
        }
        if (newarr == null) {
            newarr = new byte[maxarr + 2];
        }
        newarr[0] = (byte) i;
        if (maxarr < 10) {
            int $24 = maxarr + 1;
            int i2 = 0;
            while ($24 > 0) {
                newarr[i2 + 1] = reb[i2];
                $24--;
                i2++;
            }
        } else {
            System.arraycopy(reb, 0, newarr, 1, maxarr + 1);
        }
        return newarr;
    }

    private static final byte[] diginit() {
        byte[] work = new byte[190];
        for (int op = 0; op <= 189; op++) {
            int digit = op - 90;
            if (digit >= 0) {
                work[op] = (byte) (digit % 10);
                bytecar[op] = (byte) (digit / 10);
            } else {
                work[op] = (byte) ((digit + 100) % 10);
                bytecar[op] = (byte) ((r0 / 10) - 10);
            }
        }
        return work;
    }

    private static final BigDecimal clone(BigDecimal dec) {
        BigDecimal copy = new BigDecimal();
        copy.ind = dec.ind;
        copy.exp = dec.exp;
        copy.form = dec.form;
        copy.mant = dec.mant;
        return copy;
    }

    private void checkdigits(BigDecimal rhs, int dig) {
        if (dig == 0) {
            return;
        }
        if (this.mant.length > dig && !allzero(this.mant, dig)) {
            throw new ArithmeticException("Too many digits: " + toString());
        }
        if (rhs == null || rhs.mant.length <= dig || allzero(rhs.mant, dig)) {
        } else {
            throw new ArithmeticException("Too many digits: " + rhs.toString());
        }
    }

    private BigDecimal round(MathContext set) {
        return round(set.digits, set.roundingMode);
    }

    private BigDecimal round(int len, int mode) {
        boolean reuse;
        byte first;
        int adjust = this.mant.length - len;
        if (adjust <= 0) {
            return this;
        }
        this.exp += adjust;
        int sign = this.ind;
        byte[] oldmant = this.mant;
        if (len > 0) {
            this.mant = new byte[len];
            System.arraycopy(oldmant, 0, this.mant, 0, len);
            reuse = true;
            first = oldmant[len];
        } else {
            this.mant = ZERO.mant;
            this.ind = (byte) 0;
            reuse = false;
            if (len == 0) {
                first = oldmant[0];
            } else {
                first = 0;
            }
        }
        int increment = 0;
        if (mode == 4) {
            if (first >= 5) {
                increment = sign;
            }
        } else if (mode == 7) {
            if (!allzero(oldmant, len)) {
                throw new ArithmeticException("Rounding necessary");
            }
        } else if (mode == 5) {
            if (first > 5) {
                increment = sign;
            } else if (first == 5 && !allzero(oldmant, len + 1)) {
                increment = sign;
            }
        } else if (mode == 6) {
            if (first > 5) {
                increment = sign;
            } else if (first == 5 && (!allzero(oldmant, len + 1) || this.mant[this.mant.length - 1] % 2 != 0)) {
                increment = sign;
            }
        } else if (mode != 1) {
            if (mode == 0) {
                if (!allzero(oldmant, len)) {
                    increment = sign;
                }
            } else if (mode == 2) {
                if (sign > 0 && !allzero(oldmant, len)) {
                    increment = sign;
                }
            } else if (mode == 3) {
                if (sign < 0 && !allzero(oldmant, len)) {
                    increment = sign;
                }
            } else {
                throw new IllegalArgumentException("Bad round value: " + mode);
            }
        }
        if (increment != 0) {
            if (this.ind == 0) {
                this.mant = ONE.mant;
                this.ind = (byte) increment;
            } else {
                if (this.ind == -1) {
                    increment = -increment;
                }
                byte[] newmant = byteaddsub(this.mant, this.mant.length, ONE.mant, 1, increment, reuse);
                if (newmant.length > this.mant.length) {
                    this.exp++;
                    System.arraycopy(newmant, 0, this.mant, 0, this.mant.length);
                } else {
                    this.mant = newmant;
                }
            }
        }
        if (this.exp > 999999999) {
            throw new ArithmeticException("Exponent Overflow: " + this.exp);
        }
        return this;
    }

    private static final boolean allzero(byte[] array, int start) {
        if (start < 0) {
            start = 0;
        }
        int $25 = array.length - 1;
        for (int i = start; i <= $25; i++) {
            if (array[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal finish(MathContext set, boolean strip) {
        if (set.digits != 0 && this.mant.length > set.digits) {
            round(set);
        }
        if (strip && set.form != 0) {
            int d = this.mant.length;
            for (int i = d - 1; i >= 1 && this.mant[i] == 0; i--) {
                d--;
                this.exp++;
            }
            if (d < this.mant.length) {
                byte[] newmant = new byte[d];
                System.arraycopy(this.mant, 0, newmant, 0, d);
                this.mant = newmant;
            }
        }
        this.form = (byte) 0;
        int $26 = this.mant.length;
        int i2 = 0;
        while ($26 > 0) {
            if (this.mant[i2] != 0) {
                if (i2 > 0) {
                    byte[] newmant2 = new byte[this.mant.length - i2];
                    System.arraycopy(this.mant, i2, newmant2, 0, this.mant.length - i2);
                    this.mant = newmant2;
                }
                int mag = this.exp + this.mant.length;
                if (mag > 0) {
                    if (mag > set.digits && set.digits != 0) {
                        this.form = (byte) set.form;
                    }
                    if (mag - 1 <= 999999999) {
                        return this;
                    }
                } else if (mag < -5) {
                    this.form = (byte) set.form;
                }
                int mag2 = mag - 1;
                if ((mag2 > 999999999) | (mag2 < -999999999)) {
                    if (this.form == 2) {
                        int sig = mag2 % 3;
                        if (sig < 0) {
                            sig += 3;
                        }
                        mag2 -= sig;
                        if (mag2 >= -999999999) {
                        }
                    }
                    throw new ArithmeticException("Exponent Overflow: " + mag2);
                }
                return this;
            }
            $26--;
            i2++;
        }
        this.ind = (byte) 0;
        if (set.form != 0 || this.exp > 0) {
            this.exp = 0;
        } else if (this.exp < -999999999) {
            throw new ArithmeticException("Exponent Overflow: " + this.exp);
        }
        this.mant = ZERO.mant;
        return this;
    }
}
