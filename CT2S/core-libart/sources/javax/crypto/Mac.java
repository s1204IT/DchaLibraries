package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.harmony.security.fortress.Engine;

public class Mac implements Cloneable {
    private final String algorithm;
    private final Object initLock = new Object();
    private boolean isInitMac = false;
    private Provider provider;
    private final Provider specifiedProvider;
    private MacSpi spiImpl;
    private static final String SERVICE = "Mac";
    private static final Engine ENGINE = new Engine(SERVICE);

    protected Mac(MacSpi macSpi, Provider provider, String algorithm) {
        this.specifiedProvider = provider;
        this.algorithm = algorithm;
        this.spiImpl = macSpi;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final Provider getProvider() {
        getSpi();
        return this.provider;
    }

    public static final Mac getInstance(String algorithm) throws NoSuchAlgorithmException {
        return getMac(algorithm, null);
    }

    public static final Mac getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider is null or empty");
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        return getMac(algorithm, impProvider);
    }

    public static final Mac getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        return getMac(algorithm, provider);
    }

    private static Mac getMac(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        if (tryAlgorithm(null, provider, algorithm) == null) {
            if (provider == null) {
                throw new NoSuchAlgorithmException("No provider found for " + algorithm);
            }
            throw new NoSuchAlgorithmException("Provider " + provider.getName() + " does not provide " + algorithm);
        }
        return new Mac(null, provider, algorithm);
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
        if (sap.spi instanceof MacSpi) {
            return sap;
        }
        return null;
    }

    private MacSpi getSpi(Key key) {
        MacSpi macSpi;
        synchronized (this.initLock) {
            if (this.spiImpl != null && this.provider != null && key == null) {
                macSpi = this.spiImpl;
            } else if (this.algorithm == null) {
                macSpi = null;
            } else {
                Engine.SpiAndProvider sap = tryAlgorithm(key, this.specifiedProvider, this.algorithm);
                if (sap == null) {
                    throw new ProviderException("No provider for " + getAlgorithm());
                }
                if (this.spiImpl == null || this.provider != null) {
                    this.spiImpl = (MacSpi) sap.spi;
                }
                this.provider = sap.provider;
                macSpi = this.spiImpl;
            }
        }
        return macSpi;
    }

    private MacSpi getSpi() {
        return getSpi(null);
    }

    public final int getMacLength() {
        return getSpi().engineGetMacLength();
    }

    public final void init(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        getSpi(key).engineInit(key, params);
        this.isInitMac = true;
    }

    public final void init(Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        try {
            getSpi(key).engineInit(key, null);
            this.isInitMac = true;
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public final void update(byte input) throws IllegalStateException {
        if (!this.isInitMac) {
            throw new IllegalStateException();
        }
        getSpi().engineUpdate(input);
    }

    public final void update(byte[] input, int offset, int len) throws IllegalStateException {
        if (!this.isInitMac) {
            throw new IllegalStateException();
        }
        if (input != null) {
            if (offset < 0 || len < 0 || offset + len > input.length) {
                throw new IllegalArgumentException("Incorrect arguments. input.length=" + input.length + " offset=" + offset + ", len=" + len);
            }
            getSpi().engineUpdate(input, offset, len);
        }
    }

    public final void update(byte[] input) throws IllegalStateException {
        if (!this.isInitMac) {
            throw new IllegalStateException();
        }
        if (input != null) {
            getSpi().engineUpdate(input, 0, input.length);
        }
    }

    public final void update(ByteBuffer input) {
        if (!this.isInitMac) {
            throw new IllegalStateException();
        }
        if (input != null) {
            getSpi().engineUpdate(input);
            return;
        }
        throw new IllegalArgumentException("input == null");
    }

    public final byte[] doFinal() throws IllegalStateException {
        if (!this.isInitMac) {
            throw new IllegalStateException();
        }
        return getSpi().engineDoFinal();
    }

    public final void doFinal(byte[] output, int outOffset) throws IllegalStateException, ShortBufferException {
        if (!this.isInitMac) {
            throw new IllegalStateException();
        }
        if (output == null) {
            throw new ShortBufferException("output == null");
        }
        if (outOffset < 0 || outOffset >= output.length) {
            throw new ShortBufferException("Incorrect outOffset: " + outOffset);
        }
        MacSpi spi = getSpi();
        int t = spi.engineGetMacLength();
        if (t > output.length - outOffset) {
            throw new ShortBufferException("Output buffer is short. Needed " + t + " bytes.");
        }
        byte[] result = spi.engineDoFinal();
        System.arraycopy(result, 0, output, outOffset, result.length);
    }

    public final byte[] doFinal(byte[] input) throws IllegalStateException {
        if (!this.isInitMac) {
            throw new IllegalStateException();
        }
        MacSpi spi = getSpi();
        if (input != null) {
            spi.engineUpdate(input, 0, input.length);
        }
        return spi.engineDoFinal();
    }

    public final void reset() {
        getSpi().engineReset();
    }

    public final Object clone() throws CloneNotSupportedException {
        MacSpi newSpiImpl = null;
        MacSpi spi = getSpi();
        if (spi != null) {
            newSpiImpl = (MacSpi) spi.clone();
        }
        Mac mac = new Mac(newSpiImpl, this.provider, this.algorithm);
        mac.isInitMac = this.isInitMac;
        return mac;
    }
}
