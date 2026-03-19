package javax.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Iterator;
import sun.security.jca.GetInstance;
import sun.security.jca.Providers;

public class SecretKeyFactory {
    private final String algorithm;
    private final Object lock = new Object();
    private Provider provider;
    private Iterator<Provider.Service> serviceIterator;
    private volatile SecretKeyFactorySpi spi;

    protected SecretKeyFactory(SecretKeyFactorySpi secretKeyFactorySpi, Provider provider, String str) {
        this.spi = secretKeyFactorySpi;
        this.provider = provider;
        this.algorithm = str;
    }

    private SecretKeyFactory(String str) throws NoSuchAlgorithmException {
        this.algorithm = str;
        this.serviceIterator = GetInstance.getServices("SecretKeyFactory", str).iterator();
        if (nextSpi(null) == null) {
            throw new NoSuchAlgorithmException(str + " SecretKeyFactory not available");
        }
    }

    public static final SecretKeyFactory getInstance(String str) throws NoSuchAlgorithmException {
        return new SecretKeyFactory(str);
    }

    public static final SecretKeyFactory getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        Providers.checkBouncyCastleDeprecation(str2, "SecretKeyFactory", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("SecretKeyFactory", (Class<?>) SecretKeyFactorySpi.class, str, str2);
        return new SecretKeyFactory((SecretKeyFactorySpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    public static final SecretKeyFactory getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "SecretKeyFactory", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("SecretKeyFactory", (Class<?>) SecretKeyFactorySpi.class, str, provider);
        return new SecretKeyFactory((SecretKeyFactorySpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    public final Provider getProvider() {
        Provider provider;
        synchronized (this.lock) {
            this.serviceIterator = null;
            provider = this.provider;
        }
        return provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    private SecretKeyFactorySpi nextSpi(SecretKeyFactorySpi secretKeyFactorySpi) {
        synchronized (this.lock) {
            if (secretKeyFactorySpi != null) {
                try {
                    if (secretKeyFactorySpi != this.spi) {
                        return this.spi;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (this.serviceIterator == null) {
                return null;
            }
            while (this.serviceIterator.hasNext()) {
                Provider.Service next = this.serviceIterator.next();
                if (JceSecurity.canUseProvider(next.getProvider())) {
                    try {
                        Object objNewInstance = next.newInstance(null);
                        if (objNewInstance instanceof SecretKeyFactorySpi) {
                            SecretKeyFactorySpi secretKeyFactorySpi2 = (SecretKeyFactorySpi) objNewInstance;
                            this.provider = next.getProvider();
                            this.spi = secretKeyFactorySpi2;
                            return secretKeyFactorySpi2;
                        }
                    } catch (NoSuchAlgorithmException e) {
                    }
                }
            }
            this.serviceIterator = null;
            return null;
        }
    }

    public final SecretKey generateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        if (this.serviceIterator == null) {
            return this.spi.engineGenerateSecret(keySpec);
        }
        Exception exc = null;
        SecretKeyFactorySpi secretKeyFactorySpiNextSpi = this.spi;
        do {
            try {
                return secretKeyFactorySpiNextSpi.engineGenerateSecret(keySpec);
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                secretKeyFactorySpiNextSpi = nextSpi(secretKeyFactorySpiNextSpi);
            }
        } while (secretKeyFactorySpiNextSpi != null);
        if (exc instanceof InvalidKeySpecException) {
            throw ((InvalidKeySpecException) exc);
        }
        throw new InvalidKeySpecException("Could not generate secret key", exc);
    }

    public final KeySpec getKeySpec(SecretKey secretKey, Class<?> cls) throws InvalidKeySpecException {
        if (this.serviceIterator == null) {
            return this.spi.engineGetKeySpec(secretKey, cls);
        }
        Exception exc = null;
        SecretKeyFactorySpi secretKeyFactorySpiNextSpi = this.spi;
        do {
            try {
                return secretKeyFactorySpiNextSpi.engineGetKeySpec(secretKey, cls);
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                secretKeyFactorySpiNextSpi = nextSpi(secretKeyFactorySpiNextSpi);
            }
        } while (secretKeyFactorySpiNextSpi != null);
        if (exc instanceof InvalidKeySpecException) {
            throw ((InvalidKeySpecException) exc);
        }
        throw new InvalidKeySpecException("Could not get key spec", exc);
    }

    public final SecretKey translateKey(SecretKey secretKey) throws InvalidKeyException {
        if (this.serviceIterator == null) {
            return this.spi.engineTranslateKey(secretKey);
        }
        Exception exc = null;
        SecretKeyFactorySpi secretKeyFactorySpiNextSpi = this.spi;
        do {
            try {
                return secretKeyFactorySpiNextSpi.engineTranslateKey(secretKey);
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                secretKeyFactorySpiNextSpi = nextSpi(secretKeyFactorySpiNextSpi);
            }
        } while (secretKeyFactorySpiNextSpi != null);
        if (exc instanceof InvalidKeyException) {
            throw ((InvalidKeyException) exc);
        }
        throw new InvalidKeyException("Could not translate key", exc);
    }
}
