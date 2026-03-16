package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import org.apache.harmony.security.fortress.Engine;

public class KeyGenerator {
    private static final Engine ENGINE = new Engine("KeyGenerator");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final String algorithm;
    private final Provider provider;
    private final KeyGeneratorSpi spiImpl;

    protected KeyGenerator(KeyGeneratorSpi keyGenSpi, Provider provider, String algorithm) {
        this.provider = provider;
        this.algorithm = algorithm;
        this.spiImpl = keyGenSpi;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public static final KeyGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        return new KeyGenerator((KeyGeneratorSpi) sap.spi, sap.provider, algorithm);
    }

    public static final KeyGenerator getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider is null or empty");
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, impProvider);
    }

    public static final KeyGenerator getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        return new KeyGenerator((KeyGeneratorSpi) spi, provider, algorithm);
    }

    public final SecretKey generateKey() {
        return this.spiImpl.engineGenerateKey();
    }

    public final void init(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
        this.spiImpl.engineInit(params, RANDOM);
    }

    public final void init(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        this.spiImpl.engineInit(params, random);
    }

    public final void init(int keysize) {
        this.spiImpl.engineInit(keysize, RANDOM);
    }

    public final void init(int keysize, SecureRandom random) {
        this.spiImpl.engineInit(keysize, random);
    }

    public final void init(SecureRandom random) {
        this.spiImpl.engineInit(random);
    }
}
