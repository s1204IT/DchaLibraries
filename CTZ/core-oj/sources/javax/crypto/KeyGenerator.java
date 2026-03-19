package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Iterator;
import sun.security.jca.GetInstance;
import sun.security.jca.Providers;

public class KeyGenerator {
    private static final int I_NONE = 1;
    private static final int I_PARAMS = 3;
    private static final int I_RANDOM = 2;
    private static final int I_SIZE = 4;
    private final String algorithm;
    private int initKeySize;
    private AlgorithmParameterSpec initParams;
    private SecureRandom initRandom;
    private int initType;
    private final Object lock;
    private Provider provider;
    private Iterator<Provider.Service> serviceIterator;
    private volatile KeyGeneratorSpi spi;

    protected KeyGenerator(KeyGeneratorSpi keyGeneratorSpi, Provider provider, String str) {
        this.lock = new Object();
        this.spi = keyGeneratorSpi;
        this.provider = provider;
        this.algorithm = str;
    }

    private KeyGenerator(String str) throws NoSuchAlgorithmException {
        this.lock = new Object();
        this.algorithm = str;
        this.serviceIterator = GetInstance.getServices("KeyGenerator", str).iterator();
        this.initType = 1;
        if (nextSpi(null, false) == null) {
            throw new NoSuchAlgorithmException(str + " KeyGenerator not available");
        }
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static final KeyGenerator getInstance(String str) throws NoSuchAlgorithmException {
        return new KeyGenerator(str);
    }

    public static final KeyGenerator getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        Providers.checkBouncyCastleDeprecation(str2, "KeyGenerator", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("KeyGenerator", (Class<?>) KeyGeneratorSpi.class, str, str2);
        return new KeyGenerator((KeyGeneratorSpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    public static final KeyGenerator getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "KeyGenerator", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("KeyGenerator", (Class<?>) KeyGeneratorSpi.class, str, provider);
        return new KeyGenerator((KeyGeneratorSpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    public final Provider getProvider() {
        Provider provider;
        synchronized (this.lock) {
            disableFailover();
            provider = this.provider;
        }
        return provider;
    }

    private KeyGeneratorSpi nextSpi(KeyGeneratorSpi keyGeneratorSpi, boolean z) {
        synchronized (this.lock) {
            if (keyGeneratorSpi != null) {
                try {
                    if (keyGeneratorSpi != this.spi) {
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
                        if (objNewInstance instanceof KeyGeneratorSpi) {
                            KeyGeneratorSpi keyGeneratorSpi2 = (KeyGeneratorSpi) objNewInstance;
                            if (z) {
                                if (this.initType == 4) {
                                    keyGeneratorSpi2.engineInit(this.initKeySize, this.initRandom);
                                } else if (this.initType == 3) {
                                    keyGeneratorSpi2.engineInit(this.initParams, this.initRandom);
                                } else if (this.initType == 2) {
                                    keyGeneratorSpi2.engineInit(this.initRandom);
                                } else if (this.initType != 1) {
                                    throw new AssertionError((Object) ("KeyGenerator initType: " + this.initType));
                                }
                            }
                            this.provider = next.getProvider();
                            this.spi = keyGeneratorSpi2;
                            return keyGeneratorSpi2;
                        }
                    } catch (Exception e) {
                    }
                }
            }
            disableFailover();
            return null;
        }
    }

    void disableFailover() {
        this.serviceIterator = null;
        this.initType = 0;
        this.initParams = null;
        this.initRandom = null;
    }

    public final void init(SecureRandom secureRandom) {
        if (this.serviceIterator == null) {
            this.spi.engineInit(secureRandom);
            return;
        }
        KeyGeneratorSpi keyGeneratorSpiNextSpi = this.spi;
        RuntimeException runtimeException = null;
        do {
            try {
                keyGeneratorSpiNextSpi.engineInit(secureRandom);
                this.initType = 2;
                this.initKeySize = 0;
                this.initParams = null;
                this.initRandom = secureRandom;
                return;
            } catch (RuntimeException e) {
                if (runtimeException == null) {
                    runtimeException = e;
                }
                keyGeneratorSpiNextSpi = nextSpi(keyGeneratorSpiNextSpi, false);
            }
        } while (keyGeneratorSpiNextSpi != null);
        throw runtimeException;
    }

    public final void init(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        init(algorithmParameterSpec, JceSecurity.RANDOM);
    }

    public final void init(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        if (this.serviceIterator == null) {
            this.spi.engineInit(algorithmParameterSpec, secureRandom);
            return;
        }
        Exception exc = null;
        KeyGeneratorSpi keyGeneratorSpiNextSpi = this.spi;
        do {
            try {
                keyGeneratorSpiNextSpi.engineInit(algorithmParameterSpec, secureRandom);
                this.initType = 3;
                this.initKeySize = 0;
                this.initParams = algorithmParameterSpec;
                this.initRandom = secureRandom;
                return;
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
                keyGeneratorSpiNextSpi = nextSpi(keyGeneratorSpiNextSpi, false);
            }
        } while (keyGeneratorSpiNextSpi != null);
        if (exc instanceof InvalidAlgorithmParameterException) {
            throw ((InvalidAlgorithmParameterException) exc);
        }
        if (exc instanceof RuntimeException) {
            throw ((RuntimeException) exc);
        }
        throw new InvalidAlgorithmParameterException("init() failed", exc);
    }

    public final void init(int i) {
        init(i, JceSecurity.RANDOM);
    }

    public final void init(int i, SecureRandom secureRandom) {
        if (this.serviceIterator == null) {
            this.spi.engineInit(i, secureRandom);
            return;
        }
        KeyGeneratorSpi keyGeneratorSpiNextSpi = this.spi;
        RuntimeException runtimeException = null;
        do {
            try {
                keyGeneratorSpiNextSpi.engineInit(i, secureRandom);
                this.initType = 4;
                this.initKeySize = i;
                this.initParams = null;
                this.initRandom = secureRandom;
                return;
            } catch (RuntimeException e) {
                if (runtimeException == null) {
                    runtimeException = e;
                }
                keyGeneratorSpiNextSpi = nextSpi(keyGeneratorSpiNextSpi, false);
            }
        } while (keyGeneratorSpiNextSpi != null);
        throw runtimeException;
    }

    public final SecretKey generateKey() {
        if (this.serviceIterator == null) {
            return this.spi.engineGenerateKey();
        }
        RuntimeException runtimeException = null;
        KeyGeneratorSpi keyGeneratorSpiNextSpi = this.spi;
        do {
            try {
                return keyGeneratorSpiNextSpi.engineGenerateKey();
            } catch (RuntimeException e) {
                if (runtimeException == null) {
                    runtimeException = e;
                }
                keyGeneratorSpiNextSpi = nextSpi(keyGeneratorSpiNextSpi, true);
            }
        } while (keyGeneratorSpiNextSpi != null);
        throw runtimeException;
    }
}
