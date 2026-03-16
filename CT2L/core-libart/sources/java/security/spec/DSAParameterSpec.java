package java.security.spec;

import java.math.BigInteger;
import java.security.interfaces.DSAParams;

public class DSAParameterSpec implements AlgorithmParameterSpec, DSAParams {
    private final BigInteger g;
    private final BigInteger p;
    private final BigInteger q;

    public DSAParameterSpec(BigInteger p, BigInteger q, BigInteger g) {
        this.p = p;
        this.q = q;
        this.g = g;
    }

    @Override
    public BigInteger getG() {
        return this.g;
    }

    @Override
    public BigInteger getP() {
        return this.p;
    }

    @Override
    public BigInteger getQ() {
        return this.q;
    }
}
