package java.security.spec;

import java.math.BigInteger;

public class RSAMultiPrimePrivateCrtKeySpec extends RSAPrivateKeySpec {
    private final BigInteger crtCoefficient;
    private final RSAOtherPrimeInfo[] otherPrimeInfo;
    private final BigInteger primeExponentP;
    private final BigInteger primeExponentQ;
    private final BigInteger primeP;
    private final BigInteger primeQ;
    private final BigInteger publicExponent;

    public RSAMultiPrimePrivateCrtKeySpec(BigInteger modulus, BigInteger publicExponent, BigInteger privateExponent, BigInteger primeP, BigInteger primeQ, BigInteger primeExponentP, BigInteger primeExponentQ, BigInteger crtCoefficient, RSAOtherPrimeInfo[] otherPrimeInfo) {
        super(modulus, privateExponent);
        if (modulus == null) {
            throw new NullPointerException("modulus == null");
        }
        if (privateExponent == null) {
            throw new NullPointerException("privateExponent == null");
        }
        if (publicExponent == null) {
            throw new NullPointerException("publicExponent == null");
        }
        if (primeP == null) {
            throw new NullPointerException("primeP == null");
        }
        if (primeQ == null) {
            throw new NullPointerException("primeQ == null");
        }
        if (primeExponentP == null) {
            throw new NullPointerException("primeExponentP == null");
        }
        if (primeExponentQ == null) {
            throw new NullPointerException("primeExponentQ == null");
        }
        if (crtCoefficient == null) {
            throw new NullPointerException("crtCoefficient == null");
        }
        if (otherPrimeInfo != null) {
            if (otherPrimeInfo.length == 0) {
                throw new IllegalArgumentException("otherPrimeInfo.length == 0");
            }
            this.otherPrimeInfo = new RSAOtherPrimeInfo[otherPrimeInfo.length];
            System.arraycopy(otherPrimeInfo, 0, this.otherPrimeInfo, 0, this.otherPrimeInfo.length);
        } else {
            this.otherPrimeInfo = null;
        }
        this.publicExponent = publicExponent;
        this.primeP = primeP;
        this.primeQ = primeQ;
        this.primeExponentP = primeExponentP;
        this.primeExponentQ = primeExponentQ;
        this.crtCoefficient = crtCoefficient;
    }

    public BigInteger getCrtCoefficient() {
        return this.crtCoefficient;
    }

    public RSAOtherPrimeInfo[] getOtherPrimeInfo() {
        if (this.otherPrimeInfo == null) {
            return null;
        }
        RSAOtherPrimeInfo[] ret = new RSAOtherPrimeInfo[this.otherPrimeInfo.length];
        System.arraycopy(this.otherPrimeInfo, 0, ret, 0, ret.length);
        return ret;
    }

    public BigInteger getPrimeExponentP() {
        return this.primeExponentP;
    }

    public BigInteger getPrimeExponentQ() {
        return this.primeExponentQ;
    }

    public BigInteger getPrimeP() {
        return this.primeP;
    }

    public BigInteger getPrimeQ() {
        return this.primeQ;
    }

    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }
}
