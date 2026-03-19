package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

public class PBEParameterSpec implements AlgorithmParameterSpec {
    private int iterationCount;
    private AlgorithmParameterSpec paramSpec;
    private byte[] salt;

    public PBEParameterSpec(byte[] bArr, int i) {
        this.paramSpec = null;
        this.salt = (byte[]) bArr.clone();
        this.iterationCount = i;
    }

    public PBEParameterSpec(byte[] bArr, int i, AlgorithmParameterSpec algorithmParameterSpec) {
        this.paramSpec = null;
        this.salt = (byte[]) bArr.clone();
        this.iterationCount = i;
        this.paramSpec = algorithmParameterSpec;
    }

    public byte[] getSalt() {
        return (byte[]) this.salt.clone();
    }

    public int getIterationCount() {
        return this.iterationCount;
    }

    public AlgorithmParameterSpec getParameterSpec() {
        return this.paramSpec;
    }
}
