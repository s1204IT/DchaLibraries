package com.android.org.conscrypt;

import java.security.PublicKey;
import java.util.Arrays;

final class X509PublicKey implements PublicKey {
    private static final long serialVersionUID = -8610156854731664298L;
    private final String algorithm;
    private final byte[] encoded;

    X509PublicKey(String str, byte[] bArr) {
        this.algorithm = str;
        this.encoded = bArr;
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
        return "X509PublicKey [algorithm=" + this.algorithm + ", encoded=" + Arrays.toString(this.encoded) + "]";
    }

    public int hashCode() {
        return (31 * ((this.algorithm == null ? 0 : this.algorithm.hashCode()) + 31)) + Arrays.hashCode(this.encoded);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        X509PublicKey x509PublicKey = (X509PublicKey) obj;
        if (this.algorithm == null) {
            if (x509PublicKey.algorithm != null) {
                return false;
            }
        } else if (!this.algorithm.equals(x509PublicKey.algorithm)) {
            return false;
        }
        if (Arrays.equals(this.encoded, x509PublicKey.encoded)) {
            return true;
        }
        return false;
    }
}
