package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

public class GCMParameterSpec implements AlgorithmParameterSpec {
    private byte[] iv;
    private int tLen;

    public GCMParameterSpec(int i, byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("src array is null");
        }
        init(i, bArr, 0, bArr.length);
    }

    public GCMParameterSpec(int i, byte[] bArr, int i2, int i3) {
        init(i, bArr, i2, i3);
    }

    private void init(int i, byte[] bArr, int i2, int i3) {
        if (i < 0) {
            throw new IllegalArgumentException("Length argument is negative");
        }
        this.tLen = i;
        if (bArr == null || i3 < 0 || i2 < 0 || i3 + i2 > bArr.length) {
            throw new IllegalArgumentException("Invalid buffer arguments");
        }
        this.iv = new byte[i3];
        System.arraycopy(bArr, i2, this.iv, 0, i3);
    }

    public int getTLen() {
        return this.tLen;
    }

    public byte[] getIV() {
        return (byte[]) this.iv.clone();
    }
}
