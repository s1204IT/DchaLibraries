package javax.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import org.apache.harmony.security.fortress.Engine;

public class SecretKeyFactory {
    private static final Engine ENGINE = new Engine("SecretKeyFactory");
    private final String algorithm;
    private final Provider provider;
    private final SecretKeyFactorySpi spiImpl;

    protected SecretKeyFactory(SecretKeyFactorySpi keyFacSpi, Provider provider, String algorithm) {
        this.provider = provider;
        this.algorithm = algorithm;
        this.spiImpl = keyFacSpi;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public static final SecretKeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        return new SecretKeyFactory((SecretKeyFactorySpi) sap.spi, sap.provider, algorithm);
    }

    public static final SecretKeyFactory getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider is null or empty");
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, impProvider);
    }

    public static final SecretKeyFactory getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        return new SecretKeyFactory((SecretKeyFactorySpi) spi, provider, algorithm);
    }

    public final SecretKey generateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        return this.spiImpl.engineGenerateSecret(keySpec);
    }

    public final KeySpec getKeySpec(SecretKey key, Class keySpec) throws InvalidKeySpecException {
        return this.spiImpl.engineGetKeySpec(key, keySpec);
    }

    public final SecretKey translateKey(SecretKey key) throws InvalidKeyException {
        return this.spiImpl.engineTranslateKey(key);
    }
}
