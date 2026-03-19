package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

public class DHGenParameterSpec implements AlgorithmParameterSpec {
    private int exponentSize;
    private int primeSize;

    public DHGenParameterSpec(int i, int i2) {
        this.primeSize = i;
        this.exponentSize = i2;
    }

    public int getPrimeSize() {
        return this.primeSize;
    }

    public int getExponentSize() {
        return this.exponentSize;
    }
}
