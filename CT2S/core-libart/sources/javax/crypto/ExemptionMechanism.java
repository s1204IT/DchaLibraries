package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import org.apache.harmony.security.fortress.Engine;

public class ExemptionMechanism {
    private static final Engine ENGINE = new Engine("ExemptionMechanism");
    private boolean generated;
    private Key initKey;
    private boolean isInit = false;
    private final String mechanism;
    private final Provider provider;
    private final ExemptionMechanismSpi spiImpl;

    protected ExemptionMechanism(ExemptionMechanismSpi exmechSpi, Provider provider, String mechanism) {
        this.mechanism = mechanism;
        this.spiImpl = exmechSpi;
        this.provider = provider;
    }

    public final String getName() {
        return this.mechanism;
    }

    public static final ExemptionMechanism getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        return new ExemptionMechanism((ExemptionMechanismSpi) sap.spi, sap.provider, algorithm);
    }

    public static final ExemptionMechanism getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        return getInstance(algorithm, impProvider);
    }

    public static final ExemptionMechanism getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        return new ExemptionMechanism((ExemptionMechanismSpi) spi, provider, algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final boolean isCryptoAllowed(Key key) throws ExemptionMechanismException {
        return this.generated && (this.initKey.equals(key) || Arrays.equals(this.initKey.getEncoded(), key.getEncoded()));
    }

    public final int getOutputSize(int inputLen) throws IllegalStateException {
        if (!this.isInit) {
            throw new IllegalStateException("ExemptionMechanism is not initialized");
        }
        return this.spiImpl.engineGetOutputSize(inputLen);
    }

    public final void init(Key key) throws ExemptionMechanismException, InvalidKeyException {
        this.generated = false;
        this.spiImpl.engineInit(key);
        this.initKey = key;
        this.isInit = true;
    }

    public final void init(Key key, AlgorithmParameters param) throws ExemptionMechanismException, InvalidKeyException, InvalidAlgorithmParameterException {
        this.generated = false;
        this.spiImpl.engineInit(key, param);
        this.initKey = key;
        this.isInit = true;
    }

    public final void init(Key key, AlgorithmParameterSpec param) throws ExemptionMechanismException, InvalidKeyException, InvalidAlgorithmParameterException {
        this.generated = false;
        this.spiImpl.engineInit(key, param);
        this.initKey = key;
        this.isInit = true;
    }

    public final byte[] genExemptionBlob() throws IllegalStateException, ExemptionMechanismException {
        if (!this.isInit) {
            throw new IllegalStateException("ExemptionMechanism is not initialized");
        }
        this.generated = false;
        byte[] result = this.spiImpl.engineGenExemptionBlob();
        this.generated = true;
        return result;
    }

    public final int genExemptionBlob(byte[] output) throws IllegalStateException, ExemptionMechanismException, ShortBufferException {
        return genExemptionBlob(output, 0);
    }

    public final int genExemptionBlob(byte[] output, int outputOffset) throws IllegalStateException, ExemptionMechanismException, ShortBufferException {
        if (!this.isInit) {
            throw new IllegalStateException("ExemptionMechanism is not initialized");
        }
        this.generated = false;
        int len = this.spiImpl.engineGenExemptionBlob(output, outputOffset);
        this.generated = true;
        return len;
    }

    protected void finalize() {
        try {
            super.finalize();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
