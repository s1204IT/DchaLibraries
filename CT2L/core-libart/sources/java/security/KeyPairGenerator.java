package java.security;

import java.security.spec.AlgorithmParameterSpec;
import org.apache.harmony.security.fortress.Engine;

public abstract class KeyPairGenerator extends KeyPairGeneratorSpi {
    private String algorithm;
    private Provider provider;
    private static final String SERVICE = "KeyPairGenerator";
    private static final Engine ENGINE = new Engine(SERVICE);
    private static final SecureRandom RANDOM = new SecureRandom();

    protected KeyPairGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public static KeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        Object spi = sap.spi;
        Provider provider = sap.provider;
        if (!(spi instanceof KeyPairGenerator)) {
            return new KeyPairGeneratorImpl((KeyPairGeneratorSpi) spi, provider, algorithm);
        }
        KeyPairGenerator result = (KeyPairGenerator) spi;
        result.algorithm = algorithm;
        result.provider = provider;
        return result;
    }

    public static KeyPairGenerator getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, impProvider);
    }

    public static KeyPairGenerator getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        if (!(spi instanceof KeyPairGenerator)) {
            return new KeyPairGeneratorImpl((KeyPairGeneratorSpi) spi, provider, algorithm);
        }
        KeyPairGenerator result = (KeyPairGenerator) spi;
        result.algorithm = algorithm;
        result.provider = provider;
        return result;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public void initialize(int keysize) {
        initialize(keysize, RANDOM);
    }

    public void initialize(AlgorithmParameterSpec param) throws InvalidAlgorithmParameterException {
        initialize(param, RANDOM);
    }

    public final KeyPair genKeyPair() {
        return generateKeyPair();
    }

    @Override
    public KeyPair generateKeyPair() {
        return null;
    }

    @Override
    public void initialize(int keysize, SecureRandom random) {
    }

    @Override
    public void initialize(AlgorithmParameterSpec param, SecureRandom random) throws InvalidAlgorithmParameterException {
    }

    private static class KeyPairGeneratorImpl extends KeyPairGenerator {
        private KeyPairGeneratorSpi spiImpl;

        private KeyPairGeneratorImpl(KeyPairGeneratorSpi keyPairGeneratorSpi, Provider provider, String algorithm) {
            super(algorithm);
            ((KeyPairGenerator) this).provider = provider;
            this.spiImpl = keyPairGeneratorSpi;
        }

        @Override
        public void initialize(int keysize, SecureRandom random) {
            this.spiImpl.initialize(keysize, random);
        }

        @Override
        public KeyPair generateKeyPair() {
            return this.spiImpl.generateKeyPair();
        }

        @Override
        public void initialize(AlgorithmParameterSpec param, SecureRandom random) throws InvalidAlgorithmParameterException {
            this.spiImpl.initialize(param, random);
        }
    }
}
