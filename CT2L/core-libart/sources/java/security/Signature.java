package java.security;

import java.nio.ByteBuffer;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import org.apache.harmony.security.fortress.Engine;

public abstract class Signature extends SignatureSpi {
    protected static final int SIGN = 2;
    protected static final int UNINITIALIZED = 0;
    protected static final int VERIFY = 3;
    final String algorithm;
    Provider provider;
    protected int state = 0;
    private static final String SERVICE = "Signature";
    private static final Engine ENGINE = new Engine(SERVICE);

    protected Signature(String algorithm) {
        this.algorithm = algorithm;
    }

    public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        return getSignature(algorithm, null);
    }

    public static Signature getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException(provider);
        }
        return getSignature(algorithm, p);
    }

    public static Signature getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        return getSignature(algorithm, provider);
    }

    private static Signature getSignature(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (algorithm == null || algorithm.isEmpty()) {
            throw new NoSuchAlgorithmException("Unknown algorithm: " + algorithm);
        }
        Engine.SpiAndProvider spiAndProvider = tryAlgorithm(null, provider, algorithm);
        if (spiAndProvider != null) {
            return spiAndProvider.spi instanceof Signature ? (Signature) spiAndProvider.spi : new SignatureImpl(algorithm, provider);
        }
        if (provider == null) {
            throw new NoSuchAlgorithmException("No provider found for " + algorithm);
        }
        throw new NoSuchAlgorithmException("Provider " + provider.getName() + " does not provide " + algorithm);
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
        if (sap.spi instanceof SignatureSpi) {
            return sap;
        }
        return null;
    }

    public final Provider getProvider() {
        ensureProviderChosen();
        return this.provider;
    }

    void ensureProviderChosen() {
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final void initVerify(PublicKey publicKey) throws InvalidKeyException {
        engineInitVerify(publicKey);
        this.state = 3;
    }

    public final void initVerify(java.security.cert.Certificate certificate) throws InvalidKeyException {
        boolean[] keyUsage;
        if (certificate instanceof X509Certificate) {
            Set<String> ce = ((X509Certificate) certificate).getCriticalExtensionOIDs();
            boolean critical = false;
            if (ce != null && !ce.isEmpty()) {
                Iterator<String> i = ce.iterator();
                while (true) {
                    if (!i.hasNext()) {
                        break;
                    } else if ("2.5.29.15".equals(i.next())) {
                        critical = true;
                        break;
                    }
                }
                if (critical && (keyUsage = ((X509Certificate) certificate).getKeyUsage()) != null && !keyUsage[0]) {
                    throw new InvalidKeyException("The public key in the certificate cannot be used for digital signature purposes");
                }
            }
        }
        engineInitVerify(certificate.getPublicKey());
        this.state = 3;
    }

    public final void initSign(PrivateKey privateKey) throws InvalidKeyException {
        engineInitSign(privateKey);
        this.state = 2;
    }

    public final void initSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
        engineInitSign(privateKey, random);
        this.state = 2;
    }

    public final byte[] sign() throws SignatureException {
        if (this.state != 2) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        return engineSign();
    }

    public final int sign(byte[] outbuf, int offset, int len) throws SignatureException {
        if (outbuf == null || offset < 0 || len < 0 || offset + len > outbuf.length) {
            throw new IllegalArgumentException();
        }
        if (this.state != 2) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        return engineSign(outbuf, offset, len);
    }

    public final boolean verify(byte[] signature) throws SignatureException {
        if (this.state != 3) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        return engineVerify(signature);
    }

    public final boolean verify(byte[] signature, int offset, int length) throws SignatureException {
        if (this.state != 3) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        if (signature == null || offset < 0 || length < 0 || offset + length > signature.length) {
            throw new IllegalArgumentException();
        }
        return engineVerify(signature, offset, length);
    }

    public final void update(byte b) throws SignatureException {
        if (this.state == 0) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        engineUpdate(b);
    }

    public final void update(byte[] data) throws SignatureException {
        if (this.state == 0) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        engineUpdate(data, 0, data.length);
    }

    public final void update(byte[] data, int off, int len) throws SignatureException {
        if (this.state == 0) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        if (data == null || off < 0 || len < 0 || off + len > data.length) {
            throw new IllegalArgumentException();
        }
        engineUpdate(data, off, len);
    }

    public final void update(ByteBuffer data) throws SignatureException {
        if (this.state == 0) {
            throw new SignatureException("Signature object is not initialized properly");
        }
        engineUpdate(data);
    }

    public String toString() {
        return "SIGNATURE " + this.algorithm + " state: " + stateToString(this.state);
    }

    private String stateToString(int state) {
        switch (state) {
            case 0:
                return "UNINITIALIZED";
            case 1:
            default:
                return "";
            case 2:
                return "SIGN";
            case 3:
                return "VERIFY";
        }
    }

    @Deprecated
    public final void setParameter(String param, Object value) throws InvalidParameterException {
        engineSetParameter(param, value);
    }

    public final void setParameter(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
        engineSetParameter(params);
    }

    public final AlgorithmParameters getParameters() {
        return engineGetParameters();
    }

    @Deprecated
    public final Object getParameter(String param) throws InvalidParameterException {
        return engineGetParameter(param);
    }

    private static class SignatureImpl extends Signature {
        private final Object initLock;
        private final Provider specifiedProvider;
        private SignatureSpi spiImpl;

        public SignatureImpl(String algorithm, Provider provider) {
            super(algorithm);
            this.initLock = new Object();
            this.specifiedProvider = provider;
        }

        private SignatureImpl(String algorithm, Provider provider, SignatureSpi spi) {
            this(algorithm, provider);
            this.spiImpl = spi;
        }

        @Override
        void ensureProviderChosen() {
            getSpi(null);
        }

        @Override
        protected byte[] engineSign() throws SignatureException {
            return getSpi().engineSign();
        }

        @Override
        protected void engineUpdate(byte arg0) throws SignatureException {
            getSpi().engineUpdate(arg0);
        }

        @Override
        protected boolean engineVerify(byte[] arg0) throws SignatureException {
            return getSpi().engineVerify(arg0);
        }

        @Override
        protected void engineUpdate(byte[] arg0, int arg1, int arg2) throws SignatureException {
            getSpi().engineUpdate(arg0, arg1, arg2);
        }

        @Override
        protected void engineInitSign(PrivateKey arg0) throws InvalidKeyException {
            getSpi(arg0).engineInitSign(arg0);
        }

        @Override
        protected void engineInitVerify(PublicKey arg0) throws InvalidKeyException {
            getSpi(arg0).engineInitVerify(arg0);
        }

        @Override
        protected Object engineGetParameter(String arg0) throws InvalidParameterException {
            return getSpi().engineGetParameter(arg0);
        }

        @Override
        protected void engineSetParameter(String arg0, Object arg1) throws InvalidParameterException {
            getSpi().engineSetParameter(arg0, arg1);
        }

        @Override
        protected void engineSetParameter(AlgorithmParameterSpec arg0) throws InvalidAlgorithmParameterException {
            getSpi().engineSetParameter(arg0);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            SignatureSpi spi = this.spiImpl != null ? (SignatureSpi) this.spiImpl.clone() : null;
            return new SignatureImpl(getAlgorithm(), getProvider(), spi);
        }

        private SignatureSpi getSpi(Key key) {
            SignatureSpi signatureSpi;
            synchronized (this.initLock) {
                if (this.spiImpl == null || key != null) {
                    Engine.SpiAndProvider sap = Signature.tryAlgorithm(key, this.specifiedProvider, this.algorithm);
                    if (sap == null) {
                        throw new ProviderException("No provider for " + getAlgorithm());
                    }
                    this.spiImpl = (SignatureSpi) sap.spi;
                    this.provider = sap.provider;
                    signatureSpi = this.spiImpl;
                } else {
                    signatureSpi = this.spiImpl;
                }
            }
            return signatureSpi;
        }

        private SignatureSpi getSpi() {
            return getSpi(null);
        }
    }
}
