package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

public class RC5ParameterSpec implements AlgorithmParameterSpec {
    private final byte[] iv;
    private final int rounds;
    private final int version;
    private final int wordSize;

    public RC5ParameterSpec(int version, int rounds, int wordSize) {
        this.version = version;
        this.rounds = rounds;
        this.wordSize = wordSize;
        this.iv = null;
    }

    public RC5ParameterSpec(int version, int rounds, int wordSize, byte[] iv) {
        if (iv == null) {
            throw new IllegalArgumentException("iv == null");
        }
        if (iv.length < (wordSize / 8) * 2) {
            throw new IllegalArgumentException("iv.length < 2 * (wordSize / 8)");
        }
        this.version = version;
        this.rounds = rounds;
        this.wordSize = wordSize;
        this.iv = new byte[(wordSize / 8) * 2];
        System.arraycopy(iv, 0, this.iv, 0, (wordSize / 8) * 2);
    }

    public RC5ParameterSpec(int version, int rounds, int wordSize, byte[] iv, int offset) {
        if (iv == null) {
            throw new IllegalArgumentException("iv == null");
        }
        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException("offset < 0: " + offset);
        }
        if (iv.length - offset < (wordSize / 8) * 2) {
            throw new IllegalArgumentException("iv.length - offset < 2 * (wordSize / 8)");
        }
        this.version = version;
        this.rounds = rounds;
        this.wordSize = wordSize;
        this.iv = new byte[((wordSize / 8) * 2) + offset];
        System.arraycopy(iv, offset, this.iv, 0, (wordSize / 8) * 2);
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
        byte[] result = new byte[this.iv.length];
        System.arraycopy(this.iv, 0, result, 0, this.iv.length);
        return result;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RC5ParameterSpec)) {
            return false;
        }
        RC5ParameterSpec ps = (RC5ParameterSpec) obj;
        return this.version == ps.version && this.rounds == ps.rounds && this.wordSize == ps.wordSize && Arrays.equals(this.iv, ps.iv);
    }

    public int hashCode() {
        int result = this.version + this.rounds + this.wordSize;
        if (this.iv == null) {
            return result;
        }
        byte[] arr$ = this.iv;
        for (byte element : arr$) {
            result += element & Character.DIRECTIONALITY_UNDEFINED;
        }
        return result;
    }
}
