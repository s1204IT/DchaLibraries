package java.security.spec;

import java.math.BigInteger;

public class DSAPublicKeySpec implements KeySpec {
    private final BigInteger g;
    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger y;

    public DSAPublicKeySpec(BigInteger y, BigInteger p, BigInteger q, BigInteger g) {
        this.y = y;
        this.p = p;
        this.q = q;
        this.g = g;
    }

    public BigInteger getG() {
        return this.g;
    }

    public BigInteger getP() {
        return this.p;
    }

    public BigInteger getQ() {
        return this.q;
    }

    public BigInteger getY() {
        return this.y;
    }
}
