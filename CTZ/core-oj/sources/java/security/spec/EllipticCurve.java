package java.security.spec;

import java.math.BigInteger;

public class EllipticCurve {
    private final BigInteger a;
    private final BigInteger b;
    private final ECField field;
    private final byte[] seed;

    private static void checkValidity(ECField eCField, BigInteger bigInteger, String str) {
        if (eCField instanceof ECFieldFp) {
            if (((ECFieldFp) eCField).getP().compareTo(bigInteger) != 1) {
                throw new IllegalArgumentException(str + " is too large");
            }
            if (bigInteger.signum() < 0) {
                throw new IllegalArgumentException(str + " is negative");
            }
            return;
        }
        if (eCField instanceof ECFieldF2m) {
            if (bigInteger.bitLength() > ((ECFieldF2m) eCField).getM()) {
                throw new IllegalArgumentException(str + " is too large");
            }
        }
    }

    public EllipticCurve(ECField eCField, BigInteger bigInteger, BigInteger bigInteger2) {
        this(eCField, bigInteger, bigInteger2, null);
    }

    public EllipticCurve(ECField eCField, BigInteger bigInteger, BigInteger bigInteger2, byte[] bArr) {
        if (eCField == null) {
            throw new NullPointerException("field is null");
        }
        if (bigInteger == null) {
            throw new NullPointerException("first coefficient is null");
        }
        if (bigInteger2 == null) {
            throw new NullPointerException("second coefficient is null");
        }
        checkValidity(eCField, bigInteger, "first coefficient");
        checkValidity(eCField, bigInteger2, "second coefficient");
        this.field = eCField;
        this.a = bigInteger;
        this.b = bigInteger2;
        if (bArr != null) {
            this.seed = (byte[]) bArr.clone();
        } else {
            this.seed = null;
        }
    }

    public ECField getField() {
        return this.field;
    }

    public BigInteger getA() {
        return this.a;
    }

    public BigInteger getB() {
        return this.b;
    }

    public byte[] getSeed() {
        if (this.seed == null) {
            return null;
        }
        return (byte[]) this.seed.clone();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EllipticCurve) {
            EllipticCurve ellipticCurve = (EllipticCurve) obj;
            if (this.field.equals(ellipticCurve.field) && this.a.equals(ellipticCurve.a) && this.b.equals(ellipticCurve.b)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public int hashCode() {
        return this.field.hashCode() << ((6 + (this.a.hashCode() << 4)) + (this.b.hashCode() << 2));
    }
}
