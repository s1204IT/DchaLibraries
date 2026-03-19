package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

public class IvParameterSpec implements AlgorithmParameterSpec {
    private byte[] iv;

    public IvParameterSpec(byte[] bArr) {
        this(bArr, 0, bArr.length);
    }

    public IvParameterSpec(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new IllegalArgumentException("IV missing");
        }
        if (bArr.length - i < i2) {
            throw new IllegalArgumentException("IV buffer too short for given offset/length combination");
        }
        if (i2 < 0) {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
        this.iv = new byte[i2];
        System.arraycopy(bArr, i, this.iv, 0, i2);
    }

    public byte[] getIV() {
        return (byte[]) this.iv.clone();
    }
}
