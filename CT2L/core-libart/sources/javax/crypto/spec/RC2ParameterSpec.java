package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

public class RC2ParameterSpec implements AlgorithmParameterSpec {
    private final int effectiveKeyBits;
    private final byte[] iv;

    public RC2ParameterSpec(int effectiveKeyBits) {
        this.effectiveKeyBits = effectiveKeyBits;
        this.iv = null;
    }

    public RC2ParameterSpec(int effectiveKeyBits, byte[] iv) {
        if (iv == null) {
            throw new IllegalArgumentException("iv == null");
        }
        if (iv.length < 8) {
            throw new IllegalArgumentException("iv.length < 8");
        }
        this.effectiveKeyBits = effectiveKeyBits;
        this.iv = new byte[8];
        System.arraycopy(iv, 0, this.iv, 0, 8);
    }

    public RC2ParameterSpec(int effectiveKeyBits, byte[] iv, int offset) {
        if (iv == null) {
            throw new IllegalArgumentException("iv == null");
        }
        if (iv.length - offset < 8) {
            throw new IllegalArgumentException("iv.length - offset < 8");
        }
        this.effectiveKeyBits = effectiveKeyBits;
        this.iv = new byte[8];
        System.arraycopy(iv, offset, this.iv, 0, 8);
    }

    public int getEffectiveKeyBits() {
        return this.effectiveKeyBits;
    }

    public byte[] getIV() {
        if (this.iv == null) {
            return null;
        }
        byte[] result = new byte[this.iv.length];
        System.arraycopy(this.iv, 0, result, 0, this.iv.length);
        return result;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RC2ParameterSpec)) {
            return false;
        }
        RC2ParameterSpec ps = (RC2ParameterSpec) obj;
        return this.effectiveKeyBits == ps.effectiveKeyBits && Arrays.equals(this.iv, ps.iv);
    }

    public int hashCode() {
        int result = this.effectiveKeyBits;
        if (this.iv == null) {
            return result;
        }
        byte[] arr$ = this.iv;
        for (byte element : arr$) {
            result += element;
        }
        return result;
    }
}
