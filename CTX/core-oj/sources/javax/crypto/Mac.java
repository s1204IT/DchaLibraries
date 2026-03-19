package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Iterator;
import sun.security.jca.GetInstance;
import sun.security.jca.Providers;

public class Mac implements Cloneable {
    private static int warnCount = 10;
    private final String algorithm;
    private boolean initialized;
    private final Object lock;
    private Provider provider;
    private MacSpi spi;

    protected Mac(MacSpi macSpi, Provider provider, String str) {
        this.initialized = false;
        this.spi = macSpi;
        this.provider = provider;
        this.algorithm = str;
        this.lock = null;
    }

    private Mac(String str) {
        this.initialized = false;
        this.algorithm = str;
        this.lock = new Object();
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static final Mac getInstance(String str) throws NoSuchAlgorithmException {
        Iterator<Provider.Service> it = GetInstance.getServices("Mac", str).iterator();
        while (it.hasNext()) {
            if (JceSecurity.canUseProvider(it.next().getProvider())) {
                return new Mac(str);
            }
        }
        throw new NoSuchAlgorithmException("Algorithm " + str + " not available");
    }

    public static final Mac getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        Providers.checkBouncyCastleDeprecation(str2, "Mac", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("Mac", (Class<?>) MacSpi.class, str, str2);
        return new Mac((MacSpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    public static final Mac getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "Mac", str);
        GetInstance.Instance jceSecurity = JceSecurity.getInstance("Mac", (Class<?>) MacSpi.class, str, provider);
        return new Mac((MacSpi) jceSecurity.impl, jceSecurity.provider, str);
    }

    void chooseFirstProvider() {
        if (this.spi != null || this.lock == null) {
            return;
        }
        synchronized (this.lock) {
            if (this.spi != null) {
                return;
            }
            NoSuchAlgorithmException e = null;
            for (Provider.Service service : GetInstance.getServices("Mac", this.algorithm)) {
                if (JceSecurity.canUseProvider(service.getProvider())) {
                    try {
                        Object objNewInstance = service.newInstance(null);
                        if (objNewInstance instanceof MacSpi) {
                            this.spi = (MacSpi) objNewInstance;
                            this.provider = service.getProvider();
                            return;
                        }
                    } catch (NoSuchAlgorithmException e2) {
                        e = e2;
                    }
                }
            }
            ProviderException providerException = new ProviderException("Could not construct MacSpi instance");
            if (e != null) {
                providerException.initCause(e);
                throw providerException;
            }
            throw providerException;
        }
    }

    private void chooseProvider(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        synchronized (this.lock) {
            if (this.spi != null && (key == null || this.lock == null)) {
                this.spi.engineInit(key, algorithmParameterSpec);
                return;
            }
            Exception exc = null;
            for (Provider.Service service : GetInstance.getServices("Mac", this.algorithm)) {
                if (service.supportsParameter(key) && JceSecurity.canUseProvider(service.getProvider())) {
                    try {
                        MacSpi macSpi = (MacSpi) service.newInstance(null);
                        macSpi.engineInit(key, algorithmParameterSpec);
                        this.provider = service.getProvider();
                        this.spi = macSpi;
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

    public final int getMacLength() {
        chooseFirstProvider();
        return this.spi.engineGetMacLength();
    }

    public final void init(Key key) throws InvalidKeyException {
        try {
            if (this.spi != null && (key == null || this.lock == null)) {
                this.spi.engineInit(key, null);
            } else {
                chooseProvider(key, null);
            }
            this.initialized = true;
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("init() failed", e);
        }
    }

    public final void init(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (this.spi != null && (key == null || this.lock == null)) {
            this.spi.engineInit(key, algorithmParameterSpec);
        } else {
            chooseProvider(key, algorithmParameterSpec);
        }
        this.initialized = true;
    }

    public final void update(byte b) throws IllegalStateException {
        chooseFirstProvider();
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        this.spi.engineUpdate(b);
    }

    public final void update(byte[] bArr) throws IllegalStateException {
        chooseFirstProvider();
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        if (bArr != null) {
            this.spi.engineUpdate(bArr, 0, bArr.length);
        }
    }

    public final void update(byte[] bArr, int i, int i2) throws IllegalStateException {
        chooseFirstProvider();
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        if (bArr != null) {
            if (i < 0 || i2 > bArr.length - i || i2 < 0) {
                throw new IllegalArgumentException("Bad arguments");
            }
            this.spi.engineUpdate(bArr, i, i2);
        }
    }

    public final void update(ByteBuffer byteBuffer) {
        chooseFirstProvider();
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        if (byteBuffer == null) {
            throw new IllegalArgumentException("Buffer must not be null");
        }
        this.spi.engineUpdate(byteBuffer);
    }

    public final byte[] doFinal() throws IllegalStateException {
        chooseFirstProvider();
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        byte[] bArrEngineDoFinal = this.spi.engineDoFinal();
        this.spi.engineReset();
        return bArrEngineDoFinal;
    }

    public final void doFinal(byte[] bArr, int i) throws IllegalStateException, ShortBufferException {
        chooseFirstProvider();
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        int macLength = getMacLength();
        if (bArr == null || bArr.length - i < macLength) {
            throw new ShortBufferException("Cannot store MAC in output buffer");
        }
        System.arraycopy(doFinal(), 0, bArr, i, macLength);
    }

    public final byte[] doFinal(byte[] bArr) throws IllegalStateException {
        chooseFirstProvider();
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        update(bArr);
        return doFinal();
    }

    public final void reset() {
        chooseFirstProvider();
        this.spi.engineReset();
    }

    public final Object clone() throws CloneNotSupportedException {
        chooseFirstProvider();
        Mac mac = (Mac) super.clone();
        mac.spi = (MacSpi) this.spi.clone();
        return mac;
    }

    public MacSpi getCurrentSpi() {
        return this.spi;
    }
}
