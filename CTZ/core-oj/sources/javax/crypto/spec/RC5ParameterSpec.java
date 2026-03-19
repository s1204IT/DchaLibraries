package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

public class RC5ParameterSpec implements AlgorithmParameterSpec {
    private byte[] iv;
    private int rounds;
    private int version;
    private int wordSize;

    public RC5ParameterSpec(int i, int i2, int i3) {
        this.iv = null;
        this.version = i;
        this.rounds = i2;
        this.wordSize = i3;
    }

    public RC5ParameterSpec(int i, int i2, int i3, byte[] bArr) {
        this(i, i2, i3, bArr, 0);
    }

    public RC5ParameterSpec(int i, int i2, int i3, byte[] bArr, int i4) {
        this.iv = null;
        this.version = i;
        this.rounds = i2;
        this.wordSize = i3;
        if (bArr == null) {
            throw new IllegalArgumentException("IV missing");
        }
        int i5 = (i3 / 8) * 2;
        if (bArr.length - i4 < i5) {
            throw new IllegalArgumentException("IV too short");
        }
        this.iv = new byte[i5];
        System.arraycopy(bArr, i4, this.iv, 0, i5);
    }

    public int getVersion() {
        return this.version;
    }

    public int getRounds() {
        return this.rounds;
    }

    public int getWordSize() {
        return this.wordSize;
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
        if (!(obj instanceof RC5ParameterSpec)) {
            return false;
        }
        RC5ParameterSpec rC5ParameterSpec = (RC5ParameterSpec) obj;
        return this.version == rC5ParameterSpec.version && this.rounds == rC5ParameterSpec.rounds && this.wordSize == rC5ParameterSpec.wordSize && Arrays.equals(this.iv, rC5ParameterSpec.iv);
    }

    public int hashCode() {
        int i = 0;
        if (this.iv != null) {
            for (int i2 = 1; i2 < this.iv.length; i2++) {
                i += this.iv[i2] * i2;
            }
        }
        return i + this.version + this.rounds + this.wordSize;
    }
}
