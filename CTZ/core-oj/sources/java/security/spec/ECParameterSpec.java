package java.security.spec;

import java.math.BigInteger;

public class ECParameterSpec implements AlgorithmParameterSpec {
    private final EllipticCurve curve;
    private String curveName;
    private final ECPoint g;
    private final int h;
    private final BigInteger n;

    public ECParameterSpec(EllipticCurve ellipticCurve, ECPoint eCPoint, BigInteger bigInteger, int i) {
        if (ellipticCurve == null) {
            throw new NullPointerException("curve is null");
        }
        if (eCPoint == null) {
            throw new NullPointerException("g is null");
        }
        if (bigInteger == null) {
            throw new NullPointerException("n is null");
        }
        if (bigInteger.signum() != 1) {
            throw new IllegalArgumentException("n is not positive");
        }
        if (i <= 0) {
            throw new IllegalArgumentException("h is not positive");
        }
        this.curve = ellipticCurve;
        this.g = eCPoint;
        this.n = bigInteger;
        this.h = i;
    }

    public EllipticCurve getCurve() {
        return this.curve;
    }

    public ECPoint getGenerator() {
        return this.g;
    }

    public BigInteger getOrder() {
        return this.n;
    }

    public int getCofactor() {
        return this.h;
    }

    public void setCurveName(String str) {
        this.curveName = str;
    }

    public String getCurveName() {
        return this.curveName;
    }
}
