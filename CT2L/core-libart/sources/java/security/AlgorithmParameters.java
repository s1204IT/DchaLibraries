package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import org.apache.harmony.security.fortress.Engine;

public class AlgorithmParameters {
    private final String algorithm;
    private boolean initialized;
    private final Provider provider;
    private final AlgorithmParametersSpi spiImpl;
    private static final String SEVICE = "AlgorithmParameters";
    private static final Engine ENGINE = new Engine(SEVICE);

    protected AlgorithmParameters(AlgorithmParametersSpi algPramSpi, Provider provider, String algorithm) {
        this.provider = provider;
        this.algorithm = algorithm;
        this.spiImpl = algPramSpi;
    }

    public static AlgorithmParameters getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        return new AlgorithmParameters((AlgorithmParametersSpi) sap.spi, sap.provider, algorithm);
    }

    public static AlgorithmParameters getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("provider == null || provider.isEmpty()");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, p);
    }

    public static AlgorithmParameters getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        return new AlgorithmParameters((AlgorithmParametersSpi) spi, provider, algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final void init(AlgorithmParameterSpec paramSpec) throws InvalidParameterSpecException {
        if (this.initialized) {
            throw new InvalidParameterSpecException("Parameter has already been initialized");
        }
        this.spiImpl.engineInit(paramSpec);
        this.initialized = true;
    }

    public final void init(byte[] params) throws IOException {
        if (this.initialized) {
            throw new IOException("Parameter has already been initialized");
        }
        this.spiImpl.engineInit(params);
        this.initialized = true;
    }

    public final void init(byte[] params, String format) throws IOException {
        if (this.initialized) {
            throw new IOException("Parameter has already been initialized");
        }
        this.spiImpl.engineInit(params, format);
        this.initialized = true;
    }

    public final <T extends AlgorithmParameterSpec> T getParameterSpec(Class<T> cls) throws InvalidParameterSpecException {
        if (!this.initialized) {
            throw new InvalidParameterSpecException("Parameter has not been initialized");
        }
        return (T) this.spiImpl.engineGetParameterSpec(cls);
    }

    public final byte[] getEncoded() throws IOException {
        if (!this.initialized) {
            throw new IOException("Parameter has not been initialized");
        }
        return this.spiImpl.engineGetEncoded();
    }

    public final byte[] getEncoded(String format) throws IOException {
        if (!this.initialized) {
            throw new IOException("Parameter has not been initialized");
        }
        return this.spiImpl.engineGetEncoded(format);
    }

    public final String toString() {
        if (this.initialized) {
            return this.spiImpl.engineToString();
        }
        return null;
    }
}
