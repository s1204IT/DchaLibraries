package java.security;

import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Iterator;
import sun.security.jca.GetInstance;
import sun.security.jca.JCAUtil;
import sun.security.jca.Providers;

public abstract class KeyPairGenerator extends KeyPairGeneratorSpi {
    private final String algorithm;
    Provider provider;

    protected KeyPairGenerator(String str) {
        this.algorithm = str;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    private static KeyPairGenerator getInstance(GetInstance.Instance instance, String str) {
        KeyPairGenerator delegate;
        if (instance.impl instanceof KeyPairGenerator) {
            delegate = (KeyPairGenerator) instance.impl;
        } else {
            delegate = new Delegate((KeyPairGeneratorSpi) instance.impl, str);
        }
        delegate.provider = instance.provider;
        return delegate;
    }

    public static KeyPairGenerator getInstance(String str) throws NoSuchAlgorithmException {
        Iterator<Provider.Service> it = GetInstance.getServices("KeyPairGenerator", str).iterator();
        if (!it.hasNext()) {
            throw new NoSuchAlgorithmException(str + " KeyPairGenerator not available");
        }
        NoSuchAlgorithmException noSuchAlgorithmException = null;
        do {
            try {
                GetInstance.Instance getInstance = GetInstance.getInstance(it.next(), KeyPairGeneratorSpi.class);
                if (getInstance.impl instanceof KeyPairGenerator) {
                    return getInstance(getInstance, str);
                }
                return new Delegate(getInstance, it, str);
            } catch (NoSuchAlgorithmException e) {
                if (noSuchAlgorithmException == null) {
                    noSuchAlgorithmException = e;
                }
            }
        } while (it.hasNext());
        throw noSuchAlgorithmException;
    }

    public static KeyPairGenerator getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        Providers.checkBouncyCastleDeprecation(str2, "KeyPairGenerator", str);
        return getInstance(GetInstance.getInstance("KeyPairGenerator", (Class<?>) KeyPairGeneratorSpi.class, str, str2), str);
    }

    public static KeyPairGenerator getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "KeyPairGenerator", str);
        return getInstance(GetInstance.getInstance("KeyPairGenerator", (Class<?>) KeyPairGeneratorSpi.class, str, provider), str);
    }

    public final Provider getProvider() {
        disableFailover();
        return this.provider;
    }

    void disableFailover() {
    }

    public void initialize(int i) {
        initialize(i, JCAUtil.getSecureRandom());
    }

    @Override
    public void initialize(int i, SecureRandom secureRandom) {
    }

    public void initialize(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        initialize(algorithmParameterSpec, JCAUtil.getSecureRandom());
    }

    @Override
    public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
    }

    public final KeyPair genKeyPair() {
        return generateKeyPair();
    }

    @Override
    public KeyPair generateKeyPair() {
        return null;
    }

    private static final class Delegate extends KeyPairGenerator {
        private static final int I_NONE = 1;
        private static final int I_PARAMS = 3;
        private static final int I_SIZE = 2;
        private int initKeySize;
        private AlgorithmParameterSpec initParams;
        private SecureRandom initRandom;
        private int initType;
        private final Object lock;
        private Iterator<Provider.Service> serviceIterator;
        private volatile KeyPairGeneratorSpi spi;

        Delegate(KeyPairGeneratorSpi keyPairGeneratorSpi, String str) {
            super(str);
            this.lock = new Object();
            this.spi = keyPairGeneratorSpi;
        }

        Delegate(GetInstance.Instance instance, Iterator<Provider.Service> it, String str) {
            super(str);
            this.lock = new Object();
            this.spi = (KeyPairGeneratorSpi) instance.impl;
            this.provider = instance.provider;
            this.serviceIterator = it;
            this.initType = 1;
        }

        private KeyPairGeneratorSpi nextSpi(KeyPairGeneratorSpi keyPairGeneratorSpi, boolean z) {
            synchronized (this.lock) {
                if (keyPairGeneratorSpi != null) {
                    try {
                        if (keyPairGeneratorSpi != this.spi) {
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
                        if ((objNewInstance instanceof KeyPairGeneratorSpi) && !(objNewInstance instanceof KeyPairGenerator)) {
                            KeyPairGeneratorSpi keyPairGeneratorSpi2 = (KeyPairGeneratorSpi) objNewInstance;
                            if (z) {
                                if (this.initType == 2) {
                                    keyPairGeneratorSpi2.initialize(this.initKeySize, this.initRandom);
                                } else if (this.initType == 3) {
                                    keyPairGeneratorSpi2.initialize(this.initParams, this.initRandom);
                                } else if (this.initType != 1) {
                                    throw new AssertionError((Object) ("KeyPairGenerator initType: " + this.initType));
                                }
                            }
                            this.provider = next.getProvider();
                            this.spi = keyPairGeneratorSpi2;
                            return keyPairGeneratorSpi2;
                        }
                    } catch (Exception e) {
                    }
                }
                disableFailover();
                return null;
            }
        }

        @Override
        void disableFailover() {
            this.serviceIterator = null;
            this.initType = 0;
            this.initParams = null;
            this.initRandom = null;
        }

        @Override
        public void initialize(int i, SecureRandom secureRandom) {
            if (this.serviceIterator == null) {
                this.spi.initialize(i, secureRandom);
                return;
            }
            KeyPairGeneratorSpi keyPairGeneratorSpiNextSpi = this.spi;
            RuntimeException runtimeException = null;
            do {
                try {
                    keyPairGeneratorSpiNextSpi.initialize(i, secureRandom);
                    this.initType = 2;
                    this.initKeySize = i;
                    this.initParams = null;
                    this.initRandom = secureRandom;
                    return;
                } catch (RuntimeException e) {
                    if (runtimeException == null) {
                        runtimeException = e;
                    }
                    keyPairGeneratorSpiNextSpi = nextSpi(keyPairGeneratorSpiNextSpi, false);
                }
            } while (keyPairGeneratorSpiNextSpi != null);
            throw runtimeException;
        }

        @Override
        public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            if (this.serviceIterator == null) {
                this.spi.initialize(algorithmParameterSpec, secureRandom);
                return;
            }
            Exception exc = null;
            KeyPairGeneratorSpi keyPairGeneratorSpiNextSpi = this.spi;
            do {
                try {
                    keyPairGeneratorSpiNextSpi.initialize(algorithmParameterSpec, secureRandom);
                    this.initType = 3;
                    this.initKeySize = 0;
                    this.initParams = algorithmParameterSpec;
                    this.initRandom = secureRandom;
                    return;
                } catch (Exception e) {
                    if (exc == null) {
                        exc = e;
                    }
                    keyPairGeneratorSpiNextSpi = nextSpi(keyPairGeneratorSpiNextSpi, false);
                }
            } while (keyPairGeneratorSpiNextSpi != null);
            if (exc instanceof RuntimeException) {
                throw ((RuntimeException) exc);
            }
            throw ((InvalidAlgorithmParameterException) exc);
        }

        @Override
        public KeyPair generateKeyPair() {
            if (this.serviceIterator == null) {
                return this.spi.generateKeyPair();
            }
            RuntimeException runtimeException = null;
            KeyPairGeneratorSpi keyPairGeneratorSpiNextSpi = this.spi;
            do {
                try {
                    return keyPairGeneratorSpiNextSpi.generateKeyPair();
                } catch (RuntimeException e) {
                    if (runtimeException == null) {
                        runtimeException = e;
                    }
                    keyPairGeneratorSpiNextSpi = nextSpi(keyPairGeneratorSpiNextSpi, true);
                }
            } while (keyPairGeneratorSpiNextSpi != null);
            throw runtimeException;
        }
    }
}
