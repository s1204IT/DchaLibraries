package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import org.apache.harmony.security.fortress.Engine;

public class CertStore {
    private static final String DEFAULT_PROPERTY = "LDAP";
    private static final String PROPERTY_NAME = "certstore.type";
    private final CertStoreParameters certStoreParams;
    private final Provider provider;
    private final CertStoreSpi spiImpl;
    private final String type;
    private static final String SERVICE = "CertStore";
    private static final Engine ENGINE = new Engine(SERVICE);

    protected CertStore(CertStoreSpi storeSpi, Provider provider, String type, CertStoreParameters params) {
        this.provider = provider;
        this.type = type;
        this.spiImpl = storeSpi;
        this.certStoreParams = params;
    }

    public static CertStore getInstance(String type, CertStoreParameters params) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        try {
            Engine.SpiAndProvider sap = ENGINE.getInstance(type, params);
            return new CertStore((CertStoreSpi) sap.spi, sap.provider, type, params);
        } catch (NoSuchAlgorithmException e) {
            Throwable th = e.getCause();
            if (th == null) {
                throw e;
            }
            throw new InvalidAlgorithmParameterException(e.getMessage(), th);
        }
    }

    public static CertStore getInstance(String type, CertStoreParameters params, String provider) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("provider == null || provider.isEmpty()");
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(type, params, impProvider);
    }

    public static CertStore getInstance(String type, CertStoreParameters params, Provider provider) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        try {
            Object spi = ENGINE.getInstance(type, provider, params);
            return new CertStore((CertStoreSpi) spi, provider, type, params);
        } catch (NoSuchAlgorithmException e) {
            Throwable th = e.getCause();
            if (th == null) {
                throw e;
            }
            throw new InvalidAlgorithmParameterException(e.getMessage(), th);
        }
    }

    public final String getType() {
        return this.type;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final CertStoreParameters getCertStoreParameters() {
        if (this.certStoreParams == null) {
            return null;
        }
        return (CertStoreParameters) this.certStoreParams.clone();
    }

    public final Collection<? extends Certificate> getCertificates(CertSelector selector) throws CertStoreException {
        return this.spiImpl.engineGetCertificates(selector);
    }

    public final Collection<? extends CRL> getCRLs(CRLSelector selector) throws CertStoreException {
        return this.spiImpl.engineGetCRLs(selector);
    }

    public static final String getDefaultType() {
        String defaultType = Security.getProperty(PROPERTY_NAME);
        return defaultType == null ? DEFAULT_PROPERTY : defaultType;
    }
}
