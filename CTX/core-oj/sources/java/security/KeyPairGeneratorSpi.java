package java.security;

import java.security.spec.AlgorithmParameterSpec;

public abstract class KeyPairGeneratorSpi {
    public abstract KeyPair generateKeyPair();

    public abstract void initialize(int i, SecureRandom secureRandom);

    public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException();
    }
}
