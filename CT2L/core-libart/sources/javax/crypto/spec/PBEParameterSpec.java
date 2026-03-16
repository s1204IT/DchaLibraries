package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

public class PBEParameterSpec implements AlgorithmParameterSpec {
    private final int iterationCount;
    private final byte[] salt;

    public PBEParameterSpec(byte[] salt, int iterationCount) {
        if (salt == null) {
            throw new NullPointerException("salt == null");
        }
        this.salt = new byte[salt.length];
        System.arraycopy(salt, 0, this.salt, 0, salt.length);
        this.iterationCount = iterationCount;
    }

    public byte[] getSalt() {
        byte[] result = new byte[this.salt.length];
        System.arraycopy(this.salt, 0, result, 0, this.salt.length);
        return result;
    }

    public int getIterationCount() {
        return this.iterationCount;
    }
}
