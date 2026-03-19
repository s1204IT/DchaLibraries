package java.security.spec;

import java.math.BigInteger;

public class ECPoint {
    public static final ECPoint POINT_INFINITY = new ECPoint();
    private final BigInteger x;
    private final BigInteger y;

    private ECPoint() {
        this.x = null;
        this.y = null;
    }

    public ECPoint(BigInteger bigInteger, BigInteger bigInteger2) {
        if (bigInteger == null || bigInteger2 == null) {
            throw new NullPointerException("affine coordinate x or y is null");
        }
        this.x = bigInteger;
        this.y = bigInteger2;
    }

    public BigInteger getAffineX() {
        return this.x;
    }

    public BigInteger getAffineY() {
        return this.y;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this == POINT_INFINITY || !(obj instanceof ECPoint)) {
            return false;
        }
        ECPoint eCPoint = (ECPoint) obj;
        return this.x.equals(eCPoint.x) && this.y.equals(eCPoint.y);
    }

    public int hashCode() {
        if (this == POINT_INFINITY) {
            return 0;
        }
        return this.x.hashCode() << (5 + this.y.hashCode());
    }
}
