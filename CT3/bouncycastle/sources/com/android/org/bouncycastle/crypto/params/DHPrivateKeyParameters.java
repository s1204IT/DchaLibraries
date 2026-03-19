package com.android.org.bouncycastle.crypto.params;

import java.math.BigInteger;

public class DHPrivateKeyParameters extends DHKeyParameters {
    private BigInteger x;

    public DHPrivateKeyParameters(BigInteger x, DHParameters params) {
        super(true, params);
        this.x = x;
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
        if ((obj instanceof DHPrivateKeyParameters) && obj.getX().equals(this.x)) {
            return super.equals(obj);
        }
        return false;
    }
}
