package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

public class RC2ParameterSpec implements AlgorithmParameterSpec {
    private int effectiveKeyBits;
    private byte[] iv;

    public RC2ParameterSpec(int i) {
        this.iv = null;
        this.effectiveKeyBits = i;
    }

    public RC2ParameterSpec(int i, byte[] bArr) {
        this(i, bArr, 0);
    }

    public RC2ParameterSpec(int i, byte[] bArr, int i2) {
        this.iv = null;
        this.effectiveKeyBits = i;
        if (bArr == null) {
            throw new IllegalArgumentException("IV missing");
        }
        if (bArr.length - i2 < 8) {
            throw new IllegalArgumentException("IV too short");
        }
        this.iv = new byte[8];
        System.arraycopy(bArr, i2, this.iv, 0, 8);
    }

    public int getEffectiveKeyBits() {
        return this.effectiveKeyBits;
    }

    public byte[] getIV() {
        if (this.iv == null) {
            return null;
        }
        return (byte[]) this.iv.clone();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RC2ParameterSpec)) {
            return false;
        }
        RC2ParameterSpec rC2ParameterSpec = (RC2ParameterSpec) obj;
        return this.effectiveKeyBits == rC2ParameterSpec.effectiveKeyBits && Arrays.equals(this.iv, rC2ParameterSpec.iv);
    }

    public int hashCode() {
        int i = 0;
        if (this.iv != null) {
            for (int i2 = 1; i2 < this.iv.length; i2++) {
                i += this.iv[i2] * i2;
            }
        }
        return i + this.effectiveKeyBits;
    }
}
