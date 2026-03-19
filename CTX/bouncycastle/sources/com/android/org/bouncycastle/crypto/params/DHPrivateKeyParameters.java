package com.android.org.bouncycastle.crypto.params;

import java.math.BigInteger;

public class DHPrivateKeyParameters extends DHKeyParameters {
    private BigInteger x;

    public DHPrivateKeyParameters(BigInteger bigInteger, DHParameters dHParameters) {
        super(true, dHParameters);
        this.x = bigInteger;
    }

    public BigInteger getX() {
        return this.x;
    }

    @Override
    public int hashCode() {
        return this.x.hashCode() ^ super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof DHPrivateKeyParameters) && ((DHPrivateKeyParameters) obj).getX().equals(this.x) && super.equals(obj);
    }
}
