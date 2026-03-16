package java.security.spec;

import java.math.BigInteger;

public class RSAOtherPrimeInfo {
    private final BigInteger crtCoefficient;
    private final BigInteger prime;
    private final BigInteger primeExponent;

    public RSAOtherPrimeInfo(BigInteger prime, BigInteger primeExponent, BigInteger crtCoefficient) {
        if (prime == null) {
            throw new NullPointerException("prime == null");
        }
        if (primeExponent == null) {
            throw new NullPointerException("primeExponent == null");
        }
        if (crtCoefficient == null) {
            throw new NullPointerException("crtCoefficient == null");
        }
        this.prime = prime;
        this.primeExponent = primeExponent;
        this.crtCoefficient = crtCoefficient;
    }

    public final BigInteger getCrtCoefficient() {
        return this.crtCoefficient;
    }

    public final BigInteger getPrime() {
        return this.prime;
    }

    public final BigInteger getExponent() {
        return this.primeExponent;
    }
}
