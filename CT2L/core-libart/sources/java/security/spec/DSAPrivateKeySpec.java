package java.security.spec;

import java.math.BigInteger;

public class DSAPrivateKeySpec implements KeySpec {
    private final BigInteger g;
    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger x;

    public DSAPrivateKeySpec(BigInteger x, BigInteger p, BigInteger q, BigInteger g) {
        this.x = x;
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

    public BigInteger getX() {
        return this.x;
    }
}
