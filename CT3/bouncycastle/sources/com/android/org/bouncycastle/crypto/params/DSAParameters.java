package com.android.org.bouncycastle.crypto.params;

import com.android.org.bouncycastle.crypto.CipherParameters;
import java.math.BigInteger;

public class DSAParameters implements CipherParameters {
    private BigInteger g;
    private BigInteger p;
    private BigInteger q;
    private DSAValidationParameters validation;

    public DSAParameters(BigInteger p, BigInteger q, BigInteger g) {
        this.g = g;
        this.p = p;
        this.q = q;
    }

    public DSAParameters(BigInteger p, BigInteger q, BigInteger g, DSAValidationParameters params) {
        this.g = g;
        this.p = p;
        this.q = q;
        this.validation = params;
    }

    public BigInteger getP() {
        return this.p;
    }

    public BigInteger getQ() {
        return this.q;
    }

    public BigInteger getG() {
        return this.g;
    }

    public DSAValidationParameters getValidationParameters() {
        return this.validation;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof DSAParameters) && obj.getP().equals(this.p) && obj.getQ().equals(this.q)) {
            return obj.getG().equals(this.g);
        }
        return false;
    }

    public int hashCode() {
        return (getP().hashCode() ^ getQ().hashCode()) ^ getG().hashCode();
    }
}
