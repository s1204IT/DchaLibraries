package com.android.org.conscrypt;

import java.io.Serializable;
import java.security.SecureRandomSpi;

public final class OpenSSLRandom extends SecureRandomSpi implements Serializable {
    private static final long serialVersionUID = 8506210602917522861L;

    @Override
    protected void engineSetSeed(byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("seed == null");
        }
    }

    @Override
    protected void engineNextBytes(byte[] bArr) {
        NativeCrypto.RAND_bytes(bArr);
    }

    @Override
    protected byte[] engineGenerateSeed(int i) {
        byte[] bArr = new byte[i];
        NativeCrypto.RAND_bytes(bArr);
        return bArr;
    }
}
