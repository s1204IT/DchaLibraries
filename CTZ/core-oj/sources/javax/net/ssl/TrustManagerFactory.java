package javax.net.ssl;

import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import sun.security.jca.GetInstance;

public class TrustManagerFactory {
    private String algorithm;
    private TrustManagerFactorySpi factorySpi;
    private Provider provider;

    public static final String getDefaultAlgorithm() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty("ssl.TrustManagerFactory.algorithm");
            }
        });
        if (str == null) {
            return "SunX509";
        }
        return str;
    }

    protected TrustManagerFactory(TrustManagerFactorySpi trustManagerFactorySpi, Provider provider, String str) {
        this.factorySpi = trustManagerFactorySpi;
        this.provider = provider;
        this.algorithm = str;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static final TrustManagerFactory getInstance(String str) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("TrustManagerFactory", (Class<?>) TrustManagerFactorySpi.class, str);
        return new TrustManagerFactory((TrustManagerFactorySpi) getInstance.impl, getInstance.provider, str);
    }

    public static final TrustManagerFactory getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        GetInstance.Instance getInstance = GetInstance.getInstance("TrustManagerFactory", (Class<?>) TrustManagerFactorySpi.class, str, str2);
        return new TrustManagerFactory((TrustManagerFactorySpi) getInstance.impl, getInstance.provider, str);
    }

    public static final TrustManagerFactory getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("TrustManagerFactory", (Class<?>) TrustManagerFactorySpi.class, str, provider);
        return new TrustManagerFactory((TrustManagerFactorySpi) getInstance.impl, getInstance.provider, str);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final void init(KeyStore keyStore) throws KeyStoreException {
        this.factorySpi.engineInit(keyStore);
    }

    public final void init(ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
        this.factorySpi.engineInit(managerFactoryParameters);
    }

    public final TrustManager[] getTrustManagers() {
        return this.factorySpi.engineGetTrustManagers();
    }
}
