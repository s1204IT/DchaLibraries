package java.security;

import java.security.spec.AlgorithmParameterSpec;
import org.apache.harmony.security.fortress.Engine;

public class AlgorithmParameterGenerator {
    private final String algorithm;
    private final Provider provider;
    private final AlgorithmParameterGeneratorSpi spiImpl;
    private static final String SERVICE = "AlgorithmParameterGenerator";
    private static final Engine ENGINE = new Engine(SERVICE);
    private static final SecureRandom RANDOM = new SecureRandom();

    protected AlgorithmParameterGenerator(AlgorithmParameterGeneratorSpi paramGenSpi, Provider provider, String algorithm) {
        this.provider = provider;
        this.algorithm = algorithm;
        this.spiImpl = paramGenSpi;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        return new AlgorithmParameterGenerator((AlgorithmParameterGeneratorSpi) sap.spi, sap.provider, algorithm);
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, impProvider);
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        return new AlgorithmParameterGenerator((AlgorithmParameterGeneratorSpi) spi, provider, algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final void init(int size) {
        this.spiImpl.engineInit(size, RANDOM);
    }

    public final void init(int size, SecureRandom random) {
        this.spiImpl.engineInit(size, random);
    }

    public final void init(AlgorithmParameterSpec genParamSpec) throws InvalidAlgorithmParameterException {
        this.spiImpl.engineInit(genParamSpec, RANDOM);
    }

    public final void init(AlgorithmParameterSpec genParamSpec, SecureRandom random) throws InvalidAlgorithmParameterException {
        this.spiImpl.engineInit(genParamSpec, random);
    }

    public final AlgorithmParameters generateParameters() {
        return this.spiImpl.engineGenerateParameters();
    }
}
