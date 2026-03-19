package java.security;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import sun.security.jca.GetInstance;
import sun.security.jca.Providers;
import sun.security.jca.ServiceId;

public abstract class Signature extends SignatureSpi {
    protected static final int SIGN = 2;
    protected static final int UNINITIALIZED = 0;
    protected static final int VERIFY = 3;
    private String algorithm;
    Provider provider;
    protected int state = 0;
    private static final String RSA_SIGNATURE = "NONEwithRSA";
    private static final String RSA_CIPHER = "RSA/ECB/PKCS1Padding";
    private static final List<ServiceId> rsaIds = Arrays.asList(new ServiceId("Signature", RSA_SIGNATURE), new ServiceId("Cipher", RSA_CIPHER), new ServiceId("Cipher", "RSA/ECB"), new ServiceId("Cipher", "RSA//PKCS1Padding"), new ServiceId("Cipher", "RSA"));
    private static final Map<String, Boolean> signatureInfo = new ConcurrentHashMap();

    protected Signature(String str) {
        this.algorithm = str;
    }

    static {
        Boolean bool = Boolean.TRUE;
        signatureInfo.put("sun.security.provider.DSA$RawDSA", bool);
        signatureInfo.put("sun.security.provider.DSA$SHA1withDSA", bool);
        signatureInfo.put("sun.security.rsa.RSASignature$MD2withRSA", bool);
        signatureInfo.put("sun.security.rsa.RSASignature$MD5withRSA", bool);
        signatureInfo.put("sun.security.rsa.RSASignature$SHA1withRSA", bool);
        signatureInfo.put("sun.security.rsa.RSASignature$SHA256withRSA", bool);
        signatureInfo.put("sun.security.rsa.RSASignature$SHA384withRSA", bool);
        signatureInfo.put("sun.security.rsa.RSASignature$SHA512withRSA", bool);
        signatureInfo.put("com.sun.net.ssl.internal.ssl.RSASignature", bool);
        signatureInfo.put("sun.security.pkcs11.P11Signature", bool);
    }

    public static Signature getInstance(String str) throws NoSuchAlgorithmException {
        List<Provider.Service> services;
        if (str.equalsIgnoreCase(RSA_SIGNATURE)) {
            services = GetInstance.getServices(rsaIds);
        } else {
            services = GetInstance.getServices("Signature", str);
        }
        Iterator<Provider.Service> it = services.iterator();
        if (!it.hasNext()) {
            throw new NoSuchAlgorithmException(str + " Signature not available");
        }
        do {
            Provider.Service next = it.next();
            if (isSpi(next)) {
                return new Delegate(str);
            }
            try {
                return getInstance(GetInstance.getInstance(next, SignatureSpi.class), str);
            } catch (NoSuchAlgorithmException e) {
            }
        } while (it.hasNext());
        throw e;
    }

    private static Signature getInstance(GetInstance.Instance instance, String str) {
        Signature delegate;
        if (instance.impl instanceof Signature) {
            delegate = (Signature) instance.impl;
            delegate.algorithm = str;
        } else {
            delegate = new Delegate((SignatureSpi) instance.impl, str);
        }
        delegate.provider = instance.provider;
        return delegate;
    }

    private static boolean isSpi(Provider.Service service) {
        boolean z = true;
        if (service.getType().equals("Cipher")) {
            return true;
        }
        String className = service.getClassName();
        Boolean boolValueOf = signatureInfo.get(className);
        if (boolValueOf == null) {
            try {
                Object objNewInstance = service.newInstance(null);
                if (!(objNewInstance instanceof SignatureSpi) || (objNewInstance instanceof Signature)) {
                    z = false;
                }
                boolValueOf = Boolean.valueOf(z);
                signatureInfo.put(className, boolValueOf);
            } catch (Exception e) {
                return false;
            }
        }
        return boolValueOf.booleanValue();
    }

    public static Signature getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (str.equalsIgnoreCase(RSA_SIGNATURE)) {
            if (str2 == null || str2.length() == 0) {
                throw new IllegalArgumentException("missing provider");
            }
            Provider provider = Security.getProvider(str2);
            if (provider == null) {
                throw new NoSuchProviderException("no such provider: " + str2);
            }
            return getInstanceRSA(provider);
        }
        Providers.checkBouncyCastleDeprecation(str2, "Signature", str);
        return getInstance(GetInstance.getInstance("Signature", (Class<?>) SignatureSpi.class, str, str2), str);
    }

    public static Signature getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        if (str.equalsIgnoreCase(RSA_SIGNATURE)) {
            if (provider == null) {
                throw new IllegalArgumentException("missing provider");
            }
            return getInstanceRSA(provider);
        }
        Providers.checkBouncyCastleDeprecation(provider, "Signature", str);
        return getInstance(GetInstance.getInstance("Signature", (Class<?>) SignatureSpi.class, str, provider), str);
    }

    private static Signature getInstanceRSA(Provider provider) throws NoSuchAlgorithmException {
        Provider.Service service = provider.getService("Signature", RSA_SIGNATURE);
        if (service != null) {
            return getInstance(GetInstance.getInstance(service, SignatureSpi.class), RSA_SIGNATURE);
        }
        try {
            return new Delegate(new CipherAdapter(Cipher.getInstance(RSA_CIPHER, provider)), RSA_SIGNATURE);
        } catch (GeneralSecurityException e) {
            throw new NoSuchAlgorithmException("no such algorithm: NONEwithRSA for provider " + provider.getName(), e);
        }
    }

    public final Provider getProvider() {
        chooseFirstProvider();
        return this.provider;
    }

    void chooseFirstProvider() {
    }

    public final void initVerify(PublicKey publicKey) throws InvalidKeyException {
        engineInitVerify(publicKey);
        this.state = 3;
    }

    public final void initVerify(java.security.cert.Certificate certificate) throws InvalidKeyException {
        X509Certificate x509Certificate;
        Set<String> criticalExtensionOIDs;
        boolean[] keyUsage;
        if ((certificate instanceof X509Certificate) && (criticalExtensionOIDs = (x509Certificate = (X509Certificate) certificate).getCriticalExtensionOIDs()) != null && !criticalExtensionOIDs.isEmpty() && criticalExtensionOIDs.contains("2.5.29.15") && (keyUsage = x509Certificate.getKeyUsage()) != null && !keyUsage[0]) {
            throw new InvalidKeyException("Wrong key usage");
        }
        engineInitVerify(certificate.getPublicKey());
        this.state = 3;
    }

    public final void initSign(PrivateKey privateKey) throws InvalidKeyException {
        engineInitSign(privateKey);
        this.state = 2;
    }

    public final void initSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
        engineInitSign(privateKey, secureRandom);
        this.state = 2;
    }

    public final byte[] sign() throws SignatureException {
        if (this.state == 2) {
            return engineSign();
        }
        throw new SignatureException("object not initialized for signing");
    }

    public final int sign(byte[] bArr, int i, int i2) throws SignatureException {
        if (bArr == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (i < 0 || i2 < 0) {
            throw new IllegalArgumentException("offset or len is less than 0");
        }
        if (bArr.length - i < i2) {
            throw new IllegalArgumentException("Output buffer too small for specified offset and length");
        }
        if (this.state != 2) {
            throw new SignatureException("object not initialized for signing");
        }
        return engineSign(bArr, i, i2);
    }

    public final boolean verify(byte[] bArr) throws SignatureException {
        if (this.state == 3) {
            return engineVerify(bArr);
        }
        throw new SignatureException("object not initialized for verification");
    }

    public final boolean verify(byte[] bArr, int i, int i2) throws SignatureException {
        if (this.state == 3) {
            if (bArr == null) {
                throw new IllegalArgumentException("signature is null");
            }
            if (i < 0 || i2 < 0) {
                throw new IllegalArgumentException("offset or length is less than 0");
            }
            if (bArr.length - i < i2) {
                throw new IllegalArgumentException("signature too small for specified offset and length");
            }
            return engineVerify(bArr, i, i2);
        }
        throw new SignatureException("object not initialized for verification");
    }

    public final void update(byte b) throws SignatureException {
        if (this.state == 3 || this.state == 2) {
            engineUpdate(b);
            return;
        }
        throw new SignatureException("object not initialized for signature or verification");
    }

    public final void update(byte[] bArr) throws SignatureException {
        update(bArr, 0, bArr.length);
    }

    public final void update(byte[] bArr, int i, int i2) throws SignatureException {
        if (this.state == 2 || this.state == 3) {
            if (bArr == null) {
                throw new IllegalArgumentException("data is null");
            }
            if (i < 0 || i2 < 0) {
                throw new IllegalArgumentException("off or len is less than 0");
            }
            if (bArr.length - i < i2) {
                throw new IllegalArgumentException("data too small for specified offset and length");
            }
            engineUpdate(bArr, i, i2);
            return;
        }
        throw new SignatureException("object not initialized for signature or verification");
    }

    public final void update(ByteBuffer byteBuffer) throws SignatureException {
        if (this.state != 2 && this.state != 3) {
            throw new SignatureException("object not initialized for signature or verification");
        }
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        engineUpdate(byteBuffer);
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public String toString() {
        String str = "";
        int i = this.state;
        if (i == 0) {
            str = "<not initialized>";
        } else {
            switch (i) {
                case 2:
                    str = "<initialized for signing>";
                    break;
                case 3:
                    str = "<initialized for verifying>";
                    break;
            }
        }
        return "Signature object: " + getAlgorithm() + str;
    }

    @Deprecated
    public final void setParameter(String str, Object obj) throws InvalidParameterException {
        engineSetParameter(str, obj);
    }

    public final void setParameter(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        engineSetParameter(algorithmParameterSpec);
    }

    public final AlgorithmParameters getParameters() {
        return engineGetParameters();
    }

    @Deprecated
    public final Object getParameter(String str) throws InvalidParameterException {
        return engineGetParameter(str);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }

    public SignatureSpi getCurrentSpi() {
        return null;
    }

    private static class Delegate extends Signature {
        private static final int I_PRIV = 2;
        private static final int I_PRIV_SR = 3;
        private static final int I_PUB = 1;
        private static int warnCount = 10;
        private final Object lock;
        private SignatureSpi sigSpi;

        Delegate(SignatureSpi signatureSpi, String str) {
            super(str);
            this.sigSpi = signatureSpi;
            this.lock = null;
        }

        Delegate(String str) {
            super(str);
            this.lock = new Object();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            chooseFirstProvider();
            if (this.sigSpi instanceof Cloneable) {
                Delegate delegate = new Delegate((SignatureSpi) this.sigSpi.clone(), ((Signature) this).algorithm);
                delegate.provider = this.provider;
                return delegate;
            }
            throw new CloneNotSupportedException();
        }

        private static SignatureSpi newInstance(Provider.Service service) throws NoSuchAlgorithmException {
            if (service.getType().equals("Cipher")) {
                try {
                    return new CipherAdapter(Cipher.getInstance(Signature.RSA_CIPHER, service.getProvider()));
                } catch (NoSuchPaddingException e) {
                    throw new NoSuchAlgorithmException(e);
                }
            }
            Object objNewInstance = service.newInstance(null);
            if (!(objNewInstance instanceof SignatureSpi)) {
                throw new NoSuchAlgorithmException("Not a SignatureSpi: " + objNewInstance.getClass().getName());
            }
            return (SignatureSpi) objNewInstance;
        }

        @Override
        void chooseFirstProvider() {
            List<Provider.Service> services;
            if (this.sigSpi != null) {
                return;
            }
            synchronized (this.lock) {
                if (this.sigSpi != null) {
                    return;
                }
                NoSuchAlgorithmException e = null;
                if (((Signature) this).algorithm.equalsIgnoreCase(Signature.RSA_SIGNATURE)) {
                    services = GetInstance.getServices(Signature.rsaIds);
                } else {
                    services = GetInstance.getServices("Signature", ((Signature) this).algorithm);
                }
                for (Provider.Service service : services) {
                    if (Signature.isSpi(service)) {
                        try {
                            this.sigSpi = newInstance(service);
                            this.provider = service.getProvider();
                            return;
                        } catch (NoSuchAlgorithmException e2) {
                            e = e2;
                        }
                    }
                }
                ProviderException providerException = new ProviderException("Could not construct SignatureSpi instance");
                if (e != null) {
                    providerException.initCause(e);
                    throw providerException;
                }
                throw providerException;
            }
        }

        private void chooseProvider(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
            List<Provider.Service> services;
            synchronized (this.lock) {
                if (this.sigSpi != null && key == null) {
                    init(this.sigSpi, i, key, secureRandom);
                    return;
                }
                Exception exc = null;
                if (((Signature) this).algorithm.equalsIgnoreCase(Signature.RSA_SIGNATURE)) {
                    services = GetInstance.getServices(Signature.rsaIds);
                } else {
                    services = GetInstance.getServices("Signature", ((Signature) this).algorithm);
                }
                for (Provider.Service service : services) {
                    if (service.supportsParameter(key) && Signature.isSpi(service)) {
                        try {
                            SignatureSpi signatureSpiNewInstance = newInstance(service);
                            init(signatureSpiNewInstance, i, key, secureRandom);
                            this.provider = service.getProvider();
                            this.sigSpi = signatureSpiNewInstance;
                            return;
                        } catch (Exception e) {
                            if (exc == null) {
                                exc = e;
                            }
                            if (exc instanceof InvalidKeyException) {
                                throw ((InvalidKeyException) exc);
                            }
                        }
                    }
                }
                if (exc instanceof InvalidKeyException) {
                    throw ((InvalidKeyException) exc);
                }
                if (exc instanceof RuntimeException) {
                    throw ((RuntimeException) exc);
                }
                throw new InvalidKeyException("No installed provider supports this key: " + (key != null ? key.getClass().getName() : "(null)"), exc);
            }
        }

        private void init(SignatureSpi signatureSpi, int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
            switch (i) {
                case 1:
                    signatureSpi.engineInitVerify((PublicKey) key);
                    return;
                case 2:
                    signatureSpi.engineInitSign((PrivateKey) key);
                    return;
                case 3:
                    signatureSpi.engineInitSign((PrivateKey) key, secureRandom);
                    return;
                default:
                    throw new AssertionError((Object) ("Internal error: " + i));
            }
        }

        @Override
        protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
            if (this.sigSpi != null && (this.lock == null || publicKey == null)) {
                this.sigSpi.engineInitVerify(publicKey);
            } else {
                chooseProvider(1, publicKey, null);
            }
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
            if (this.sigSpi != null && (this.lock == null || privateKey == null)) {
                this.sigSpi.engineInitSign(privateKey);
            } else {
                chooseProvider(2, privateKey, null);
            }
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
            if (this.sigSpi != null && (this.lock == null || privateKey == null)) {
                this.sigSpi.engineInitSign(privateKey, secureRandom);
            } else {
                chooseProvider(3, privateKey, secureRandom);
            }
        }

        @Override
        protected void engineUpdate(byte b) throws SignatureException {
            chooseFirstProvider();
            this.sigSpi.engineUpdate(b);
        }

        @Override
        protected void engineUpdate(byte[] bArr, int i, int i2) throws SignatureException {
            chooseFirstProvider();
            this.sigSpi.engineUpdate(bArr, i, i2);
        }

        @Override
        protected void engineUpdate(ByteBuffer byteBuffer) {
            chooseFirstProvider();
            this.sigSpi.engineUpdate(byteBuffer);
        }

        @Override
        protected byte[] engineSign() throws SignatureException {
            chooseFirstProvider();
            return this.sigSpi.engineSign();
        }

        @Override
        protected int engineSign(byte[] bArr, int i, int i2) throws SignatureException {
            chooseFirstProvider();
            return this.sigSpi.engineSign(bArr, i, i2);
        }

        @Override
        protected boolean engineVerify(byte[] bArr) throws SignatureException {
            chooseFirstProvider();
            return this.sigSpi.engineVerify(bArr);
        }

        @Override
        protected boolean engineVerify(byte[] bArr, int i, int i2) throws SignatureException {
            chooseFirstProvider();
            return this.sigSpi.engineVerify(bArr, i, i2);
        }

        @Override
        protected void engineSetParameter(String str, Object obj) throws InvalidParameterException {
            chooseFirstProvider();
            this.sigSpi.engineSetParameter(str, obj);
        }

        @Override
        protected void engineSetParameter(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
            chooseFirstProvider();
            this.sigSpi.engineSetParameter(algorithmParameterSpec);
        }

        @Override
        protected Object engineGetParameter(String str) throws InvalidParameterException {
            chooseFirstProvider();
            return this.sigSpi.engineGetParameter(str);
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            chooseFirstProvider();
            return this.sigSpi.engineGetParameters();
        }

        @Override
        public SignatureSpi getCurrentSpi() {
            SignatureSpi signatureSpi;
            if (this.lock == null) {
                return this.sigSpi;
            }
            synchronized (this.lock) {
                signatureSpi = this.sigSpi;
            }
            return signatureSpi;
        }
    }

    private static class CipherAdapter extends SignatureSpi {
        private final Cipher cipher;
        private ByteArrayOutputStream data;

        CipherAdapter(Cipher cipher) {
            this.cipher = cipher;
        }

        @Override
        protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
            this.cipher.init(2, publicKey);
            if (this.data == null) {
                this.data = new ByteArrayOutputStream(128);
            } else {
                this.data.reset();
            }
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
            this.cipher.init(1, privateKey);
            this.data = null;
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
            this.cipher.init(1, privateKey, secureRandom);
            this.data = null;
        }

        @Override
        protected void engineUpdate(byte b) throws SignatureException {
            engineUpdate(new byte[]{b}, 0, 1);
        }

        @Override
        protected void engineUpdate(byte[] bArr, int i, int i2) throws SignatureException {
            if (this.data != null) {
                this.data.write(bArr, i, i2);
                return;
            }
            byte[] bArrUpdate = this.cipher.update(bArr, i, i2);
            if (bArrUpdate != null && bArrUpdate.length != 0) {
                throw new SignatureException("Cipher unexpectedly returned data");
            }
        }

        @Override
        protected byte[] engineSign() throws SignatureException {
            try {
                return this.cipher.doFinal();
            } catch (BadPaddingException e) {
                throw new SignatureException("doFinal() failed", e);
            } catch (IllegalBlockSizeException e2) {
                throw new SignatureException("doFinal() failed", e2);
            }
        }

        @Override
        protected boolean engineVerify(byte[] bArr) throws SignatureException {
            try {
                byte[] bArrDoFinal = this.cipher.doFinal(bArr);
                byte[] byteArray = this.data.toByteArray();
                this.data.reset();
                return MessageDigest.isEqual(bArrDoFinal, byteArray);
            } catch (BadPaddingException e) {
                return false;
            } catch (IllegalBlockSizeException e2) {
                throw new SignatureException("doFinal() failed", e2);
            }
        }

        @Override
        protected void engineSetParameter(String str, Object obj) throws InvalidParameterException {
            throw new InvalidParameterException("Parameters not supported");
        }

        @Override
        protected Object engineGetParameter(String str) throws InvalidParameterException {
            throw new InvalidParameterException("Parameters not supported");
        }
    }
}
