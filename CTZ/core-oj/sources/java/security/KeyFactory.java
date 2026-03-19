package java.security;

import java.security.Provider;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Iterator;
import sun.security.jca.GetInstance;
import sun.security.jca.Providers;
import sun.security.util.Debug;

public class KeyFactory {
    private static final Debug debug = Debug.getInstance("jca", "KeyFactory");
    private final String algorithm;
    private final Object lock = new Object();
    private Provider provider;
    private Iterator<Provider.Service> serviceIterator;
    private volatile KeyFactorySpi spi;

    protected KeyFactory(KeyFactorySpi keyFactorySpi, Provider provider, String str) {
        this.spi = keyFactorySpi;
        this.provider = provider;
        this.algorithm = str;
    }

    private KeyFactory(String str) throws NoSuchAlgorithmException {
        this.algorithm = str;
        this.serviceIterator = GetInstance.getServices("KeyFactory", str).iterator();
        if (nextSpi(null) == null) {
            throw new NoSuchAlgorithmException(str + " KeyFactory not available");
        }
    }

    public static KeyFactory getInstance(String str) throws NoSuchAlgorithmException {
        return new KeyFactory(str);
    }

    public static KeyFactory getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        Providers.checkBouncyCastleDeprecation(str2, "KeyFactory", str);
        GetInstance.Instance getInstance = GetInstance.getInstance("KeyFactory", (Class<?>) KeyFactorySpi.class, str, str2);
        return new KeyFactory((KeyFactorySpi) getInstance.impl, getInstance.provider, str);
    }

    public static KeyFactory getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "KeyFactory", str);
        GetInstance.Instance getInstance = GetInstance.getInstance("KeyFactory", (Class<?>) KeyFactorySpi.class, str, provider);
        return new KeyFactory((KeyFactorySpi) getInstance.impl, getInstance.provider, str);
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

    private KeyFactorySpi nextSpi(KeyFactorySpi keyFactorySpi) {
        synchronized (this.lock) {
            if (keyFactorySpi != null) {
                try {
                    if (keyFactorySpi != this.spi) {
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
                try {
                    Object objNewInstance = next.newInstance(null);
                    if (objNewInstance instanceof KeyFactorySpi) {
                        KeyFactorySpi keyFactorySpi2 = (KeyFactorySpi) objNewInstance;
                        this.provider = next.getProvider();
                        this.spi = keyFactorySpi2;
                        return keyFactorySpi2;
                    }
                } catch (NoSuchAlgorithmException e) {
                }
            }
            this.serviceIterator = null;
            return null;
        }
    }

    public final PublicKey generatePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (this.serviceIterator == null) {
            return this.spi.engineGeneratePublic(keySpec);
        }
        Exception exc = null;
        KeyFactorySpi keyFactorySpiNextSpi = this.spi;
        do {
            try {
                return keyFactorySpiNextSpi.engineGeneratePublic(keySpec);
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                keyFactorySpiNextSpi = nextSpi(keyFactorySpiNextSpi);
            }
        } while (keyFactorySpiNextSpi != null);
        if (exc instanceof RuntimeException) {
            throw ((RuntimeException) exc);
        }
        if (exc instanceof InvalidKeySpecException) {
            throw ((InvalidKeySpecException) exc);
        }
        throw new InvalidKeySpecException("Could not generate public key", exc);
    }

    public final PrivateKey generatePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (this.serviceIterator == null) {
            return this.spi.engineGeneratePrivate(keySpec);
        }
        Exception exc = null;
        KeyFactorySpi keyFactorySpiNextSpi = this.spi;
        do {
            try {
                return keyFactorySpiNextSpi.engineGeneratePrivate(keySpec);
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                keyFactorySpiNextSpi = nextSpi(keyFactorySpiNextSpi);
            }
        } while (keyFactorySpiNextSpi != null);
        if (exc instanceof RuntimeException) {
            throw ((RuntimeException) exc);
        }
        if (exc instanceof InvalidKeySpecException) {
            throw ((InvalidKeySpecException) exc);
        }
        throw new InvalidKeySpecException("Could not generate private key", exc);
    }

    public final <T extends KeySpec> T getKeySpec(Key key, Class<T> cls) throws InvalidKeySpecException {
        if (this.serviceIterator == null) {
            return (T) this.spi.engineGetKeySpec(key, cls);
        }
        Exception exc = null;
        KeyFactorySpi keyFactorySpiNextSpi = this.spi;
        do {
            try {
                return (T) keyFactorySpiNextSpi.engineGetKeySpec(key, cls);
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                keyFactorySpiNextSpi = nextSpi(keyFactorySpiNextSpi);
            }
        } while (keyFactorySpiNextSpi != null);
        if (exc instanceof RuntimeException) {
            throw ((RuntimeException) exc);
        }
        if (exc instanceof InvalidKeySpecException) {
            throw ((InvalidKeySpecException) exc);
        }
        throw new InvalidKeySpecException("Could not get key spec", exc);
    }

    public final Key translateKey(Key key) throws InvalidKeyException {
        if (this.serviceIterator == null) {
            return this.spi.engineTranslateKey(key);
        }
        Exception exc = null;
        KeyFactorySpi keyFactorySpiNextSpi = this.spi;
        do {
            try {
                return keyFactorySpiNextSpi.engineTranslateKey(key);
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                keyFactorySpiNextSpi = nextSpi(keyFactorySpiNextSpi);
            }
        } while (keyFactorySpiNextSpi != null);
        if (exc instanceof RuntimeException) {
            throw ((RuntimeException) exc);
        }
        if (exc instanceof InvalidKeyException) {
            throw ((InvalidKeyException) exc);
        }
        throw new InvalidKeyException("Could not translate key", exc);
    }
}
