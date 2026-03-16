package org.apache.harmony.security.x509;

import java.security.PublicKey;

public final class X509PublicKey implements PublicKey {
    private final String algorithm;
    private final byte[] encoded;
    private final byte[] keyBytes;

    public X509PublicKey(String algorithm, byte[] encoded, byte[] keyBytes) {
        this.algorithm = algorithm;
        this.encoded = encoded;
        this.keyBytes = keyBytes;
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        return this.encoded;
    }

    public String toString() {
        return "algorithm = " + this.algorithm + ", params unparsed, unparsed keybits = \n";
    }
}
