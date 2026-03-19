package android.util;

import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;

public final class Rational extends Number implements Comparable<Rational> {
    private static final long serialVersionUID = 1;
    private final int mDenominator;
    private final int mNumerator;
    public static final Rational NaN = new Rational(0, 0);
    public static final Rational POSITIVE_INFINITY = new Rational(1, 0);
    public static final Rational NEGATIVE_INFINITY = new Rational(-1, 0);
    public static final Rational ZERO = new Rational(0, 1);

    public Rational(int i, int i2) {
        if (i2 < 0) {
            i = -i;
            i2 = -i2;
        }
        if (i2 == 0 && i > 0) {
            this.mNumerator = 1;
            this.mDenominator = 0;
            return;
        }
        if (i2 == 0 && i < 0) {
            this.mNumerator = -1;
            this.mDenominator = 0;
            return;
        }
        if (i2 == 0 && i == 0) {
            this.mNumerator = 0;
            this.mDenominator = 0;
        } else if (i == 0) {
            this.mNumerator = 0;
            this.mDenominator = 1;
        } else {
            int iGcd = gcd(i, i2);
            this.mNumerator = i / iGcd;
            this.mDenominator = i2 / iGcd;
        }
    }

    public int getNumerator() {
        return this.mNumerator;
    }

    public int getDenominator() {
        return this.mDenominator;
    }

    public boolean isNaN() {
        return this.mDenominator == 0 && this.mNumerator == 0;
    }

    public boolean isInfinite() {
        return this.mNumerator != 0 && this.mDenominator == 0;
    }

    public boolean isFinite() {
        return this.mDenominator != 0;
    }

    public boolean isZero() {
        return isFinite() && this.mNumerator == 0;
    }

    private boolean isPosInf() {
        return this.mDenominator == 0 && this.mNumerator > 0;
    }

    private boolean isNegInf() {
        return this.mDenominator == 0 && this.mNumerator < 0;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Rational) && equals((Rational) obj);
    }

    private boolean equals(Rational rational) {
        return this.mNumerator == rational.mNumerator && this.mDenominator == rational.mDenominator;
    }

    public String toString() {
        if (isNaN()) {
            return "NaN";
        }
        if (isPosInf()) {
            return "Infinity";
        }
        if (isNegInf()) {
            return "-Infinity";
        }
        return this.mNumerator + "/" + this.mDenominator;
    }

    public float toFloat() {
        return floatValue();
    }

    public int hashCode() {
        return ((this.mNumerator << 16) | (this.mNumerator >>> 16)) ^ this.mDenominator;
    }

    public static int gcd(int i, int i2) {
        while (true) {
            int i3 = i2;
            int i4 = i;
            i = i3;
            if (i != 0) {
                i2 = i4 % i;
            } else {
                return Math.abs(i4);
            }
        }
    }

    @Override
    public double doubleValue() {
        return ((double) this.mNumerator) / ((double) this.mDenominator);
    }

    @Override
    public float floatValue() {
        return this.mNumerator / this.mDenominator;
    }

    @Override
    public int intValue() {
        if (isPosInf()) {
            return Integer.MAX_VALUE;
        }
        if (isNegInf()) {
            return Integer.MIN_VALUE;
        }
        if (isNaN()) {
            return 0;
        }
        return this.mNumerator / this.mDenominator;
    }

    @Override
    public long longValue() {
        if (isPosInf()) {
            return Long.MAX_VALUE;
        }
        if (isNegInf()) {
            return Long.MIN_VALUE;
        }
        if (isNaN()) {
            return 0L;
        }
        return this.mNumerator / this.mDenominator;
    }

    @Override
    public short shortValue() {
        return (short) intValue();
    }

    @Override
    public int compareTo(Rational rational) {
        Preconditions.checkNotNull(rational, "another must not be null");
        if (equals(rational)) {
            return 0;
        }
        if (isNaN()) {
            return 1;
        }
        if (rational.isNaN()) {
            return -1;
        }
        if (isPosInf() || rational.isNegInf()) {
            return 1;
        }
        if (isNegInf() || rational.isPosInf()) {
            return -1;
        }
        long j = ((long) this.mNumerator) * ((long) rational.mDenominator);
        long j2 = ((long) rational.mNumerator) * ((long) this.mDenominator);
        if (j < j2) {
            return -1;
        }
        return j > j2 ? 1 : 0;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.mNumerator == 0) {
            if (this.mDenominator == 1 || this.mDenominator == 0) {
            } else {
                throw new InvalidObjectException("Rational must be deserialized from a reduced form for zero values");
            }
        } else if (this.mDenominator == 0) {
            if (this.mNumerator == 1 || this.mNumerator == -1) {
            } else {
                throw new InvalidObjectException("Rational must be deserialized from a reduced form for infinity values");
            }
        } else if (gcd(this.mNumerator, this.mDenominator) > 1) {
            throw new InvalidObjectException("Rational must be deserialized from a reduced form for finite values");
        }
    }

    private static NumberFormatException invalidRational(String str) {
        throw new NumberFormatException("Invalid Rational: \"" + str + "\"");
    }

    public static Rational parseRational(String str) throws NumberFormatException {
        Preconditions.checkNotNull(str, "string must not be null");
        if (str.equals("NaN")) {
            return NaN;
        }
        if (str.equals("Infinity")) {
            return POSITIVE_INFINITY;
        }
        if (str.equals("-Infinity")) {
            return NEGATIVE_INFINITY;
        }
        int iIndexOf = str.indexOf(58);
        if (iIndexOf < 0) {
            iIndexOf = str.indexOf(47);
        }
        if (iIndexOf < 0) {
            throw invalidRational(str);
        }
        try {
            return new Rational(Integer.parseInt(str.substring(0, iIndexOf)), Integer.parseInt(str.substring(iIndexOf + 1)));
        } catch (NumberFormatException e) {
            throw invalidRational(str);
        }
    }
}
