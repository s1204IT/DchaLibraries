package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Iterator;
import sun.security.jca.GetInstance;
import sun.security.jca.Providers;

public class KeyAgreement {
    private static final int I_NO_PARAMS = 1;
    private static final int I_PARAMS = 2;
    private static int warnCount = 10;
    private final String algorithm;
    private final Object lock;
    private Provider provider;
    private KeyAgreementSpi spi;

    protected KeyAgreement(KeyAgreementSpi keyAgreementSpi, Provider provider, String str) {
        this.spi = keyAgreementSpi;
        this.provider = provider;
        this.algorithm = str;
        this.lock = null;
    }

    private KeyAgreement(String str) {
        this.algorithm = str;
        this.lock = new Object();
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static final KeyAgreement getInstance(String str) throws NoSuchAlgorithmException {
        Iterator<Provider.Service> it = GetInstance.getServices("KeyAgreement", str).iterator();
        while (it.hasNext()) {
            if (JceSecurity.canUseProvider(it.next().getProvider())) {
                return new KeyAgreement(str);
            }
        }
        throw new NoSuchAlgorithmException("Algorithm " + str + " not available");
    }

    public static final KeyAgreement getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        Providers.checkBouncyCastleDeprecation(str2, "KeyAgreement", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("KeyAgreement", (Class<?>) KeyAgreementSpi.class, str, str2);
        return new KeyAgreement((KeyAgreementSpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    public static final KeyAgreement getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "KeyAgreement", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("KeyAgreement", (Class<?>) KeyAgreementSpi.class, str, provider);
        return new KeyAgreement((KeyAgreementSpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    void chooseFirstProvider() {
        if (this.spi != null) {
            return;
        }
        synchronized (this.lock) {
            if (this.spi != null) {
                return;
            }
            Exception e = null;
            for (Provider.Service service : GetInstance.getServices("KeyAgreement", this.algorithm)) {
                if (JceSecurity.canUseProvider(service.getProvider())) {
                    try {
                        Object objNewInstance = service.newInstance(null);
                        if (objNewInstance instanceof KeyAgreementSpi) {
                            this.spi = (KeyAgreementSpi) objNewInstance;
                            this.provider = service.getProvider();
                            return;
                        }
                    } catch (Exception e2) {
                        e = e2;
                    }
                }
            }
            ProviderException providerException = new ProviderException("Could not construct KeyAgreementSpi instance");
            if (e != null) {
                providerException.initCause(e);
                throw providerException;
            }
            throw providerException;
        }
    }

    private void implInit(KeyAgreementSpi keyAgreementSpi, int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (i == 1) {
            keyAgreementSpi.engineInit(key, secureRandom);
        } else {
            keyAgreementSpi.engineInit(key, algorithmParameterSpec, secureRandom);
        }
    }

    private void chooseProvider(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        synchronized (this.lock) {
            if (this.spi != null && key == null) {
                implInit(this.spi, i, key, algorithmParameterSpec, secureRandom);
                return;
            }
            Exception exc = null;
            for (Provider.Service service : GetInstance.getServices("KeyAgreement", this.algorithm)) {
                if (service.supportsParameter(key) && JceSecurity.canUseProvider(service.getProvider())) {
                    try {
                        KeyAgreementSpi keyAgreementSpi = (KeyAgreementSpi) service.newInstance(null);
                        implInit(keyAgreementSpi, i, key, algorithmParameterSpec, secureRandom);
                        this.provider = service.getProvider();
                        this.spi = keyAgreementSpi;
                        return;
                    } catch (Exception e) {
                        if (exc == null) {
                            exc = e;
                        }
                    }
                }
            }
            if (exc instanceof InvalidKeyException) {
                throw ((InvalidKeyException) exc);
            }
            if (exc instanceof InvalidAlgorithmParameterException) {
                throw ((InvalidAlgorithmParameterException) exc);
            }
            if (exc instanceof RuntimeException) {
                throw ((RuntimeException) exc);
            }
            throw new InvalidKeyException("No installed provider supports this key: " + (key != null ? key.getClass().getName() : "(null)"), exc);
        }
    }

    public final Provider getProvider() {
        chooseFirstProvider();
        return this.provider;
    }

    public final void init(Key key) throws InvalidKeyException {
        init(key, JceSecurity.RANDOM);
    }

    public final void init(Key key, SecureRandom secureRandom) throws InvalidKeyException {
        if (this.spi != null && (key == null || this.lock == null)) {
            this.spi.engineInit(key, secureRandom);
            return;
        }
        try {
            chooseProvider(1, key, null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e);
        }
    }

    public final void init(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(key, algorithmParameterSpec, JceSecurity.RANDOM);
    }

    public final void init(Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (this.spi != null) {
            this.spi.engineInit(key, algorithmParameterSpec, secureRandom);
        } else {
            chooseProvider(2, key, algorithmParameterSpec, secureRandom);
        }
    }

    public final Key doPhase(Key key, boolean z) throws IllegalStateException, InvalidKeyException {
        chooseFirstProvider();
        return this.spi.engineDoPhase(key, z);
    }

    public final byte[] generateSecret() throws IllegalStateException {
        chooseFirstProvider();
        return this.spi.engineGenerateSecret();
    }

    public final int generateSecret(byte[] bArr, int i) throws IllegalStateException, ShortBufferException {
        chooseFirstProvider();
        return this.spi.engineGenerateSecret(bArr, i);
    }

    public final SecretKey generateSecret(String str) throws IllegalStateException, NoSuchAlgorithmException, InvalidKeyException {
        chooseFirstProvider();
        return this.spi.engineGenerateSecret(str);
    }
}
