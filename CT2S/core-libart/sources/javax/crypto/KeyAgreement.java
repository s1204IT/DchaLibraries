package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.harmony.security.fortress.Engine;

public class KeyAgreement {
    private final String algorithm;
    private final Object initLock = new Object();
    private Provider provider;
    private final Provider specifiedProvider;
    private KeyAgreementSpi spiImpl;
    private static final String SERVICE = "KeyAgreement";
    private static final Engine ENGINE = new Engine(SERVICE);
    private static final SecureRandom RANDOM = new SecureRandom();

    protected KeyAgreement(KeyAgreementSpi keyAgreeSpi, Provider provider, String algorithm) {
        this.spiImpl = keyAgreeSpi;
        this.specifiedProvider = provider;
        this.algorithm = algorithm;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final Provider getProvider() {
        getSpi();
        return this.provider;
    }

    public static final KeyAgreement getInstance(String algorithm) throws NoSuchAlgorithmException {
        return getKeyAgreement(algorithm, null);
    }

    public static final KeyAgreement getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider is null or empty");
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getKeyAgreement(algorithm, impProvider);
    }

    public static final KeyAgreement getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        return getKeyAgreement(algorithm, provider);
    }

    private static KeyAgreement getKeyAgreement(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        if (tryAlgorithm(null, provider, algorithm) == null) {
            if (provider == null) {
                throw new NoSuchAlgorithmException("No provider found for " + algorithm);
            }
            throw new NoSuchAlgorithmException("Provider " + provider.getName() + " does not provide " + algorithm);
        }
        return new KeyAgreement(null, provider, algorithm);
    }

    private static Engine.SpiAndProvider tryAlgorithm(Key key, Provider provider, String algorithm) {
        if (provider != null) {
            Provider.Service service = provider.getService(SERVICE, algorithm);
            if (service == null) {
                return null;
            }
            return tryAlgorithmWithProvider(key, service);
        }
        ArrayList<Provider.Service> services = ENGINE.getServices(algorithm);
        if (services == null) {
            return null;
        }
        Iterator<Provider.Service> it = services.iterator();
        while (it.hasNext()) {
            Engine.SpiAndProvider sap = tryAlgorithmWithProvider(key, it.next());
            if (sap != null) {
                return sap;
            }
        }
        return null;
    }

    private static Engine.SpiAndProvider tryAlgorithmWithProvider(Key key, Provider.Service service) {
        if (key != null) {
            try {
                if (!service.supportsParameter(key)) {
                    return null;
                }
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(service, (String) null);
        if (sap.spi == null || sap.provider == null) {
            return null;
        }
        if (sap.spi instanceof KeyAgreementSpi) {
            return sap;
        }
        return null;
    }

    private KeyAgreementSpi getSpi(Key key) {
        KeyAgreementSpi keyAgreementSpi;
        synchronized (this.initLock) {
            if (this.spiImpl != null && key == null) {
                keyAgreementSpi = this.spiImpl;
            } else {
                Engine.SpiAndProvider sap = tryAlgorithm(key, this.specifiedProvider, this.algorithm);
                if (sap == null) {
                    throw new ProviderException("No provider for " + getAlgorithm());
                }
                this.spiImpl = (KeyAgreementSpi) sap.spi;
                this.provider = sap.provider;
                keyAgreementSpi = this.spiImpl;
            }
        }
        return keyAgreementSpi;
    }

    private KeyAgreementSpi getSpi() {
        return getSpi(null);
    }

    public final void init(Key key) throws InvalidKeyException {
        getSpi(key).engineInit(key, RANDOM);
    }

    public final void init(Key key, SecureRandom random) throws InvalidKeyException {
        getSpi(key).engineInit(key, random);
    }

    public final void init(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        getSpi(key).engineInit(key, params, RANDOM);
    }

    public final void init(Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        getSpi(key).engineInit(key, params, random);
    }

    public final Key doPhase(Key key, boolean lastPhase) throws IllegalStateException, InvalidKeyException {
        return getSpi().engineDoPhase(key, lastPhase);
    }

    public final byte[] generateSecret() throws IllegalStateException {
        return getSpi().engineGenerateSecret();
    }

    public final int generateSecret(byte[] sharedSecret, int offset) throws IllegalStateException, ShortBufferException {
        return getSpi().engineGenerateSecret(sharedSecret, offset);
    }

    public final SecretKey generateSecret(String algorithm) throws IllegalStateException, NoSuchAlgorithmException, InvalidKeyException {
        return getSpi().engineGenerateSecret(algorithm);
    }
}
