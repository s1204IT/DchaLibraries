package java.security.cert;

import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import sun.security.jca.GetInstance;

public class CertStore {
    private static final String CERTSTORE_TYPE = "certstore.type";
    private CertStoreParameters params;
    private Provider provider;
    private CertStoreSpi storeSpi;
    private String type;

    protected CertStore(CertStoreSpi certStoreSpi, Provider provider, String str, CertStoreParameters certStoreParameters) {
        this.storeSpi = certStoreSpi;
        this.provider = provider;
        this.type = str;
        if (certStoreParameters != null) {
            this.params = (CertStoreParameters) certStoreParameters.clone();
        }
    }

    public final Collection<? extends Certificate> getCertificates(CertSelector certSelector) throws CertStoreException {
        return this.storeSpi.engineGetCertificates(certSelector);
    }

    public final Collection<? extends CRL> getCRLs(CRLSelector cRLSelector) throws CertStoreException {
        return this.storeSpi.engineGetCRLs(cRLSelector);
    }

    public static CertStore getInstance(String str, CertStoreParameters certStoreParameters) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        try {
            GetInstance.Instance getInstance = GetInstance.getInstance("CertStore", (Class<?>) CertStoreSpi.class, str, certStoreParameters);
            return new CertStore((CertStoreSpi) getInstance.impl, getInstance.provider, str, certStoreParameters);
        } catch (NoSuchAlgorithmException e) {
            return handleException(e);
        }
    }

    private static CertStore handleException(NoSuchAlgorithmException noSuchAlgorithmException) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Throwable cause = noSuchAlgorithmException.getCause();
        if (cause instanceof InvalidAlgorithmParameterException) {
            throw ((InvalidAlgorithmParameterException) cause);
        }
        throw noSuchAlgorithmException;
    }

    public static CertStore getInstance(String str, CertStoreParameters certStoreParameters, String str2) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        try {
            GetInstance.Instance getInstance = GetInstance.getInstance("CertStore", (Class<?>) CertStoreSpi.class, str, certStoreParameters, str2);
            return new CertStore((CertStoreSpi) getInstance.impl, getInstance.provider, str, certStoreParameters);
        } catch (NoSuchAlgorithmException e) {
            return handleException(e);
        }
    }

    public static CertStore getInstance(String str, CertStoreParameters certStoreParameters, Provider provider) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        try {
            GetInstance.Instance getInstance = GetInstance.getInstance("CertStore", (Class<?>) CertStoreSpi.class, str, certStoreParameters, provider);
            return new CertStore((CertStoreSpi) getInstance.impl, getInstance.provider, str, certStoreParameters);
        } catch (NoSuchAlgorithmException e) {
            return handleException(e);
        }
    }

    public final CertStoreParameters getCertStoreParameters() {
        if (this.params == null) {
            return null;
        }
        return (CertStoreParameters) this.params.clone();
    }

    public final String getType() {
        return this.type;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public static final String getDefaultType() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty(CertStore.CERTSTORE_TYPE);
            }
        });
        if (str == null) {
            return "LDAP";
        }
        return str;
    }
}
