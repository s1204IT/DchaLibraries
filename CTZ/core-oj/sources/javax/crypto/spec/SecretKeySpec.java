package javax.crypto.spec;

import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Locale;
import javax.crypto.SecretKey;

public class SecretKeySpec implements KeySpec, SecretKey {
    private static final long serialVersionUID = 6577238317307289933L;
    private String algorithm;
    private byte[] key;

    public SecretKeySpec(byte[] bArr, String str) {
        if (bArr == null || str == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (bArr.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        this.key = (byte[]) bArr.clone();
        this.algorithm = str;
    }

    public SecretKeySpec(byte[] bArr, int i, int i2, String str) {
        if (bArr == null || str == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (bArr.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        if (bArr.length - i < i2) {
            throw new IllegalArgumentException("Invalid offset/length combination");
        }
        if (i2 < 0) {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
        this.key = new byte[i2];
        System.arraycopy(bArr, i, this.key, 0, i2);
        this.algorithm = str;
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String getFormat() {
        return "RAW";
    }

    @Override
    public byte[] getEncoded() {
        return (byte[]) this.key.clone();
    }

    public int hashCode() {
        int i = 0;
        for (int i2 = 1; i2 < this.key.length; i2++) {
            i += this.key[i2] * i2;
        }
        if (this.algorithm.equalsIgnoreCase("TripleDES")) {
            return "desede".hashCode() ^ i;
        }
        return this.algorithm.toLowerCase(Locale.ENGLISH).hashCode() ^ i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SecretKey)) {
            return false;
        }
        SecretKey secretKey = (SecretKey) obj;
        String algorithm = secretKey.getAlgorithm();
        if (!algorithm.equalsIgnoreCase(this.algorithm) && ((!algorithm.equalsIgnoreCase("DESede") || !this.algorithm.equalsIgnoreCase("TripleDES")) && (!algorithm.equalsIgnoreCase("TripleDES") || !this.algorithm.equalsIgnoreCase("DESede")))) {
            return false;
        }
        return MessageDigest.isEqual(this.key, secretKey.getEncoded());
    }
}
