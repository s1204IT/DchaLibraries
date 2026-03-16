package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import org.apache.harmony.security.fortress.Engine;

public class CertPathBuilder {
    private static final String DEFAULT_PROPERTY = "PKIX";
    private static final String PROPERTY_NAME = "certpathbuilder.type";
    private final String algorithm;
    private final Provider provider;
    private final CertPathBuilderSpi spiImpl;
    private static final String SERVICE = "CertPathBuilder";
    private static final Engine ENGINE = new Engine(SERVICE);

    protected CertPathBuilder(CertPathBuilderSpi builderSpi, Provider provider, String algorithm) {
        this.provider = provider;
        this.algorithm = algorithm;
        this.spiImpl = builderSpi;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public static CertPathBuilder getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        return new CertPathBuilder((CertPathBuilderSpi) sap.spi, sap.provider, algorithm);
    }

    public static CertPathBuilder getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("provider == null || provider.isEmpty()");
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, impProvider);
    }

    public static CertPathBuilder getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        return new CertPathBuilder((CertPathBuilderSpi) spi, provider, algorithm);
    }

    public final CertPathBuilderResult build(CertPathParameters params) throws CertPathBuilderException, InvalidAlgorithmParameterException {
        return this.spiImpl.engineBuild(params);
    }

    public static final String getDefaultType() {
        String defaultType = Security.getProperty(PROPERTY_NAME);
        return defaultType != null ? defaultType : DEFAULT_PROPERTY;
    }
}
