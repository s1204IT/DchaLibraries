package java.security.spec;

import java.math.BigInteger;

public class ECFieldFp implements ECField {
    private BigInteger p;

    public ECFieldFp(BigInteger bigInteger) {
        if (bigInteger.signum() != 1) {
            throw new IllegalArgumentException("p is not positive");
        }
        this.p = bigInteger;
    }

    @Override
    public int getFieldSize() {
        return this.p.bitLength();
    }

    public BigInteger getP() {
        return this.p;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ECFieldFp) {
            return this.p.equals(((ECFieldFp) obj).p);
        }
        return false;
    }

    public int hashCode() {
        return this.p.hashCode();
    }
}
