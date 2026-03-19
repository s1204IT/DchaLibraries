package javax.crypto;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import sun.security.jca.Providers;

public class Cipher {
    private static final String ATTRIBUTE_MODES = "SupportedModes";
    private static final String ATTRIBUTE_PADDINGS = "SupportedPaddings";
    public static final int DECRYPT_MODE = 2;
    public static final int ENCRYPT_MODE = 1;
    private static final String KEY_USAGE_EXTENSION_OID = "2.5.29.15";
    public static final int PRIVATE_KEY = 2;
    public static final int PUBLIC_KEY = 1;
    public static final int SECRET_KEY = 3;
    public static final int UNWRAP_MODE = 4;
    public static final int WRAP_MODE = 3;
    private ExemptionMechanism exmech;
    private boolean initialized = false;
    private int opmode = 0;
    private Provider provider;
    private CipherSpi spi;
    private final SpiAndProviderUpdater spiAndProviderUpdater;
    private final String[] tokenizedTransformation;
    private final String transformation;

    enum InitType {
        KEY,
        ALGORITHM_PARAMS,
        ALGORITHM_PARAM_SPEC
    }

    enum NeedToSet {
        NONE,
        MODE,
        PADDING,
        BOTH
    }

    protected Cipher(CipherSpi cipherSpi, Provider provider, String str) {
        if (cipherSpi == null) {
            throw new NullPointerException("cipherSpi == null");
        }
        if (!(cipherSpi instanceof NullCipherSpi) && provider == null) {
            throw new NullPointerException("provider == null");
        }
        this.spi = cipherSpi;
        this.provider = provider;
        this.transformation = str;
        this.tokenizedTransformation = null;
        this.spiAndProviderUpdater = new SpiAndProviderUpdater(provider, cipherSpi);
    }

    private Cipher(CipherSpi cipherSpi, Provider provider, String str, String[] strArr) {
        this.spi = cipherSpi;
        this.provider = provider;
        this.transformation = str;
        this.tokenizedTransformation = strArr;
        this.spiAndProviderUpdater = new SpiAndProviderUpdater(provider, cipherSpi);
    }

    private static String[] tokenizeTransformation(String str) throws NoSuchAlgorithmException {
        if (str == null || str.isEmpty()) {
            throw new NoSuchAlgorithmException("No transformation given");
        }
        String[] strArr = new String[3];
        StringTokenizer stringTokenizer = new StringTokenizer(str, "/");
        int i = 0;
        while (stringTokenizer.hasMoreTokens() && i < 3) {
            try {
                strArr[i] = stringTokenizer.nextToken().trim();
                i++;
            } catch (NoSuchElementException e) {
                throw new NoSuchAlgorithmException("Invalid transformation format:" + str);
            }
        }
        if (i == 0 || i == 2 || stringTokenizer.hasMoreTokens()) {
            throw new NoSuchAlgorithmException("Invalid transformation format:" + str);
        }
        if (strArr[0] == null || strArr[0].length() == 0) {
            throw new NoSuchAlgorithmException("Invalid transformation:algorithm not specified-" + str);
        }
        return strArr;
    }

    public static final Cipher getInstance(String str) throws NoSuchPaddingException, NoSuchAlgorithmException {
        return createCipher(str, null);
    }

    public static final Cipher getInstance(String str, String str2) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        if (str2 == null || str2.length() == 0) {
            throw new IllegalArgumentException("Missing provider");
        }
        Provider provider = Security.getProvider(str2);
        if (provider == null) {
            throw new NoSuchProviderException("No such provider: " + str2);
        }
        return getInstance(str, provider);
    }

    public static final Cipher getInstance(String str, Provider provider) throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("Missing provider");
        }
        return createCipher(str, provider);
    }

    static final Cipher createCipher(String str, Provider provider) throws NoSuchPaddingException, NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "Cipher", str);
        String[] strArr = tokenizeTransformation(str);
        try {
            if (tryCombinations(null, provider, strArr) == null) {
                if (provider == null) {
                    throw new NoSuchAlgorithmException("No provider found for " + str);
                }
                throw new NoSuchAlgorithmException("Provider " + provider.getName() + " does not provide " + str);
            }
            return new Cipher(null, provider, str, strArr);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new IllegalStateException("Key/Algorithm excepton despite not passing one", e);
        }
    }

    void updateProviderIfNeeded() {
        try {
            this.spiAndProviderUpdater.updateAndGetSpiAndProvider(null, this.spi, this.provider);
        } catch (Exception e) {
            ProviderException providerException = new ProviderException("Could not construct CipherSpi instance");
            providerException.initCause(e);
            throw providerException;
        }
    }

    private void chooseProvider(InitType initType, int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            this.spiAndProviderUpdater.updateAndGetSpiAndProvider(new InitParams(initType, i, key, secureRandom, algorithmParameterSpec, algorithmParameters), this.spi, this.provider);
        } catch (Exception e) {
            if (e instanceof InvalidKeyException) {
                throw ((InvalidKeyException) e);
            }
            if (e instanceof InvalidAlgorithmParameterException) {
                throw ((InvalidAlgorithmParameterException) e);
            }
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            }
            throw new InvalidKeyException("No installed provider supports this key: " + (key != null ? key.getClass().getName() : "(null)"), e);
        }
    }

    public final Provider getProvider() {
        updateProviderIfNeeded();
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.transformation;
    }

    public final int getBlockSize() {
        updateProviderIfNeeded();
        return this.spi.engineGetBlockSize();
    }

    public final int getOutputSize(int i) {
        if (!this.initialized && !(this instanceof NullCipher)) {
            throw new IllegalStateException("Cipher not initialized");
        }
        if (i < 0) {
            throw new IllegalArgumentException("Input size must be equal to or greater than zero");
        }
        updateProviderIfNeeded();
        return this.spi.engineGetOutputSize(i);
    }

    public final byte[] getIV() {
        updateProviderIfNeeded();
        return this.spi.engineGetIV();
    }

    public final AlgorithmParameters getParameters() {
        updateProviderIfNeeded();
        return this.spi.engineGetParameters();
    }

    public final ExemptionMechanism getExemptionMechanism() {
        updateProviderIfNeeded();
        return this.exmech;
    }

    private static void checkOpmode(int i) {
        if (i < 1 || i > 4) {
            throw new InvalidParameterException("Invalid operation mode");
        }
    }

    private static String getOpmodeString(int i) {
        switch (i) {
            case 1:
                return "encryption";
            case 2:
                return "decryption";
            case 3:
                return "key wrapping";
            case 4:
                return "key unwrapping";
            default:
                return "";
        }
    }

    public final void init(int i, Key key) throws InvalidKeyException {
        init(i, key, JceSecurity.RANDOM);
    }

    public final void init(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        this.initialized = false;
        checkOpmode(i);
        try {
            chooseProvider(InitType.KEY, i, key, null, null, secureRandom);
            this.initialized = true;
            this.opmode = i;
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e);
        }
    }

    public final void init(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(i, key, algorithmParameterSpec, JceSecurity.RANDOM);
    }

    public final void init(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        this.initialized = false;
        checkOpmode(i);
        chooseProvider(InitType.ALGORITHM_PARAM_SPEC, i, key, algorithmParameterSpec, null, secureRandom);
        this.initialized = true;
        this.opmode = i;
    }

    public final void init(int i, Key key, AlgorithmParameters algorithmParameters) throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(i, key, algorithmParameters, JceSecurity.RANDOM);
    }

    public final void init(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        this.initialized = false;
        checkOpmode(i);
        chooseProvider(InitType.ALGORITHM_PARAMS, i, key, null, algorithmParameters, secureRandom);
        this.initialized = true;
        this.opmode = i;
    }

    public final void init(int i, Certificate certificate) throws InvalidKeyException {
        init(i, certificate, JceSecurity.RANDOM);
    }

    public final void init(int i, Certificate certificate, SecureRandom secureRandom) throws InvalidKeyException {
        X509Certificate x509Certificate;
        Set<String> criticalExtensionOIDs;
        boolean[] keyUsage;
        this.initialized = false;
        checkOpmode(i);
        if ((certificate instanceof X509Certificate) && (criticalExtensionOIDs = (x509Certificate = (X509Certificate) certificate).getCriticalExtensionOIDs()) != null && !criticalExtensionOIDs.isEmpty() && criticalExtensionOIDs.contains(KEY_USAGE_EXTENSION_OID) && (keyUsage = x509Certificate.getKeyUsage()) != null && ((i == 1 && keyUsage.length > 3 && !keyUsage[3]) || (i == 3 && keyUsage.length > 2 && !keyUsage[2]))) {
            throw new InvalidKeyException("Wrong key usage");
        }
        try {
            chooseProvider(InitType.KEY, i, certificate == null ? null : certificate.getPublicKey(), null, null, secureRandom);
            this.initialized = true;
            this.opmode = i;
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e);
        }
    }

    private void checkCipherState() {
        if (!(this instanceof NullCipher)) {
            if (!this.initialized) {
                throw new IllegalStateException("Cipher not initialized");
            }
            if (this.opmode != 1 && this.opmode != 2) {
                throw new IllegalStateException("Cipher not initialized for encryption/decryption");
            }
        }
    }

    public final byte[] update(byte[] bArr) {
        checkCipherState();
        if (bArr == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        updateProviderIfNeeded();
        if (bArr.length == 0) {
            return null;
        }
        return this.spi.engineUpdate(bArr, 0, bArr.length);
    }

    public final byte[] update(byte[] bArr, int i, int i2) {
        checkCipherState();
        if (bArr == null || i < 0 || i2 > bArr.length - i || i2 < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        if (i2 == 0) {
            return null;
        }
        return this.spi.engineUpdate(bArr, i, i2);
    }

    public final int update(byte[] bArr, int i, int i2, byte[] bArr2) throws ShortBufferException {
        checkCipherState();
        if (bArr == null || i < 0 || i2 > bArr.length - i || i2 < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        if (i2 == 0) {
            return 0;
        }
        return this.spi.engineUpdate(bArr, i, i2, bArr2, 0);
    }

    public final int update(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException {
        checkCipherState();
        if (bArr == null || i < 0 || i2 > bArr.length - i || i2 < 0 || i3 < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        if (i2 == 0) {
            return 0;
        }
        return this.spi.engineUpdate(bArr, i, i2, bArr2, i3);
    }

    public final int update(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws ShortBufferException {
        checkCipherState();
        if (byteBuffer == null || byteBuffer2 == null) {
            throw new IllegalArgumentException("Buffers must not be null");
        }
        if (byteBuffer == byteBuffer2) {
            throw new IllegalArgumentException("Input and output buffers must not be the same object, consider using buffer.duplicate()");
        }
        if (byteBuffer2.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        updateProviderIfNeeded();
        return this.spi.engineUpdate(byteBuffer, byteBuffer2);
    }

    public final byte[] doFinal() throws BadPaddingException, IllegalBlockSizeException {
        checkCipherState();
        updateProviderIfNeeded();
        return this.spi.engineDoFinal(null, 0, 0);
    }

    public final int doFinal(byte[] bArr, int i) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        checkCipherState();
        if (bArr == null || i < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        return this.spi.engineDoFinal(null, 0, 0, bArr, i);
    }

    public final byte[] doFinal(byte[] bArr) throws BadPaddingException, IllegalBlockSizeException {
        checkCipherState();
        if (bArr == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        updateProviderIfNeeded();
        return this.spi.engineDoFinal(bArr, 0, bArr.length);
    }

    public final byte[] doFinal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException {
        checkCipherState();
        if (bArr == null || i < 0 || i2 > bArr.length - i || i2 < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        return this.spi.engineDoFinal(bArr, i, i2);
    }

    public final int doFinal(byte[] bArr, int i, int i2, byte[] bArr2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        checkCipherState();
        if (bArr == null || i < 0 || i2 > bArr.length - i || i2 < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        return this.spi.engineDoFinal(bArr, i, i2, bArr2, 0);
    }

    public final int doFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        checkCipherState();
        if (bArr == null || i < 0 || i2 > bArr.length - i || i2 < 0 || i3 < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        return this.spi.engineDoFinal(bArr, i, i2, bArr2, i3);
    }

    public final int doFinal(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        checkCipherState();
        if (byteBuffer == null || byteBuffer2 == null) {
            throw new IllegalArgumentException("Buffers must not be null");
        }
        if (byteBuffer == byteBuffer2) {
            throw new IllegalArgumentException("Input and output buffers must not be the same object, consider using buffer.duplicate()");
        }
        if (byteBuffer2.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        updateProviderIfNeeded();
        return this.spi.engineDoFinal(byteBuffer, byteBuffer2);
    }

    public final byte[] wrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        if (!(this instanceof NullCipher)) {
            if (!this.initialized) {
                throw new IllegalStateException("Cipher not initialized");
            }
            if (this.opmode != 3) {
                throw new IllegalStateException("Cipher not initialized for wrapping keys");
            }
        }
        updateProviderIfNeeded();
        return this.spi.engineWrap(key);
    }

    public final Key unwrap(byte[] bArr, String str, int i) throws NoSuchAlgorithmException, InvalidKeyException {
        if (!(this instanceof NullCipher)) {
            if (!this.initialized) {
                throw new IllegalStateException("Cipher not initialized");
            }
            if (this.opmode != 4) {
                throw new IllegalStateException("Cipher not initialized for unwrapping keys");
            }
        }
        if (i != 3 && i != 2 && i != 1) {
            throw new InvalidParameterException("Invalid key type");
        }
        updateProviderIfNeeded();
        return this.spi.engineUnwrap(bArr, str, i);
    }

    private AlgorithmParameterSpec getAlgorithmParameterSpec(AlgorithmParameters algorithmParameters) throws InvalidParameterSpecException {
        if (algorithmParameters == null) {
            return null;
        }
        String upperCase = algorithmParameters.getAlgorithm().toUpperCase(Locale.ENGLISH);
        if (upperCase.equalsIgnoreCase("RC2")) {
            return algorithmParameters.getParameterSpec(RC2ParameterSpec.class);
        }
        if (upperCase.equalsIgnoreCase("RC5")) {
            return algorithmParameters.getParameterSpec(RC5ParameterSpec.class);
        }
        if (upperCase.startsWith("PBE")) {
            return algorithmParameters.getParameterSpec(PBEParameterSpec.class);
        }
        if (!upperCase.startsWith("DES")) {
            return null;
        }
        return algorithmParameters.getParameterSpec(IvParameterSpec.class);
    }

    public static final int getMaxAllowedKeyLength(String str) throws NoSuchAlgorithmException {
        if (str == null) {
            throw new NullPointerException("transformation == null");
        }
        tokenizeTransformation(str);
        return Integer.MAX_VALUE;
    }

    public static final AlgorithmParameterSpec getMaxAllowedParameterSpec(String str) throws NoSuchAlgorithmException {
        if (str == null) {
            throw new NullPointerException("transformation == null");
        }
        tokenizeTransformation(str);
        return null;
    }

    public final void updateAAD(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("src buffer is null");
        }
        updateAAD(bArr, 0, bArr.length);
    }

    public final void updateAAD(byte[] bArr, int i, int i2) {
        checkCipherState();
        if (bArr == null || i < 0 || i2 < 0 || i2 + i > bArr.length) {
            throw new IllegalArgumentException("Bad arguments");
        }
        updateProviderIfNeeded();
        if (i2 == 0) {
            return;
        }
        this.spi.engineUpdateAAD(bArr, i, i2);
    }

    public final void updateAAD(ByteBuffer byteBuffer) {
        checkCipherState();
        if (byteBuffer == null) {
            throw new IllegalArgumentException("src ByteBuffer is null");
        }
        updateProviderIfNeeded();
        if (byteBuffer.remaining() == 0) {
            return;
        }
        this.spi.engineUpdateAAD(byteBuffer);
    }

    public CipherSpi getCurrentSpi() {
        return this.spi;
    }

    static boolean matchAttribute(Provider.Service service, String str, String str2) {
        String attribute;
        if (str2 == null || (attribute = service.getAttribute(str)) == null) {
            return true;
        }
        return str2.toUpperCase(Locale.US).matches(attribute.toUpperCase(Locale.US));
    }

    static class Transform {
        private final String name;
        private final NeedToSet needToSet;

        public Transform(String str, NeedToSet needToSet) {
            this.name = str;
            this.needToSet = needToSet;
        }
    }

    static class InitParams {
        final InitType initType;
        final Key key;
        final int opmode;
        final AlgorithmParameters params;
        final SecureRandom random;
        final AlgorithmParameterSpec spec;

        InitParams(InitType initType, int i, Key key, SecureRandom secureRandom, AlgorithmParameterSpec algorithmParameterSpec, AlgorithmParameters algorithmParameters) {
            this.initType = initType;
            this.opmode = i;
            this.key = key;
            this.random = secureRandom;
            this.spec = algorithmParameterSpec;
            this.params = algorithmParameters;
        }
    }

    class SpiAndProviderUpdater {
        private final Object initSpiLock = new Object();
        private final Provider specifiedProvider;
        private final CipherSpi specifiedSpi;

        SpiAndProviderUpdater(Provider provider, CipherSpi cipherSpi) {
            this.specifiedProvider = provider;
            this.specifiedSpi = cipherSpi;
        }

        void setCipherSpiImplAndProvider(CipherSpi cipherSpi, Provider provider) {
            Cipher.this.spi = cipherSpi;
            Cipher.this.provider = provider;
        }

        CipherSpiAndProvider updateAndGetSpiAndProvider(InitParams initParams, CipherSpi cipherSpi, Provider provider) throws InvalidKeyException, InvalidAlgorithmParameterException {
            if (this.specifiedSpi != null) {
                return new CipherSpiAndProvider(this.specifiedSpi, provider);
            }
            synchronized (this.initSpiLock) {
                if (cipherSpi != null && initParams == null) {
                    return new CipherSpiAndProvider(cipherSpi, provider);
                }
                CipherSpiAndProvider cipherSpiAndProviderTryCombinations = Cipher.tryCombinations(initParams, this.specifiedProvider, Cipher.this.tokenizedTransformation);
                if (cipherSpiAndProviderTryCombinations == null) {
                    throw new ProviderException("No provider found for " + Arrays.toString(Cipher.this.tokenizedTransformation));
                }
                setCipherSpiImplAndProvider(cipherSpiAndProviderTryCombinations.cipherSpi, cipherSpiAndProviderTryCombinations.provider);
                return new CipherSpiAndProvider(cipherSpiAndProviderTryCombinations.cipherSpi, cipherSpiAndProviderTryCombinations.provider);
            }
        }

        CipherSpiAndProvider updateAndGetSpiAndProvider(CipherSpi cipherSpi, Provider provider) {
            try {
                return updateAndGetSpiAndProvider(null, cipherSpi, provider);
            } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
                throw new ProviderException("Exception thrown when params == null", e);
            }
        }

        CipherSpi getCurrentSpi(CipherSpi cipherSpi) {
            if (this.specifiedSpi != null) {
                return this.specifiedSpi;
            }
            synchronized (this.initSpiLock) {
            }
            return cipherSpi;
        }
    }

    static CipherSpiAndProvider tryCombinations(InitParams initParams, Provider provider, String[] strArr) throws InvalidKeyException, InvalidAlgorithmParameterException {
        Exception exc;
        ArrayList<Transform> arrayList = new ArrayList();
        if (strArr[1] != null && strArr[2] != null) {
            arrayList.add(new Transform(strArr[0] + "/" + strArr[1] + "/" + strArr[2], NeedToSet.NONE));
        }
        if (strArr[1] != null) {
            arrayList.add(new Transform(strArr[0] + "/" + strArr[1], NeedToSet.PADDING));
        }
        if (strArr[2] != null) {
            arrayList.add(new Transform(strArr[0] + "//" + strArr[2], NeedToSet.MODE));
        }
        arrayList.add(new Transform(strArr[0], NeedToSet.BOTH));
        if (provider == null) {
            exc = null;
            for (Provider provider2 : Security.getProviders()) {
                for (Transform transform : arrayList) {
                    Provider.Service service = provider2.getService("Cipher", transform.name);
                    if (service != null && (initParams == null || initParams.key == null || service.supportsParameter(initParams.key))) {
                        try {
                            CipherSpiAndProvider cipherSpiAndProviderTryTransformWithProvider = tryTransformWithProvider(initParams, strArr, transform.needToSet, service);
                            if (cipherSpiAndProviderTryTransformWithProvider != null) {
                                return cipherSpiAndProviderTryTransformWithProvider;
                            }
                        } catch (Exception e) {
                            if (exc == null) {
                                exc = e;
                            }
                        }
                    }
                }
            }
        } else {
            for (Transform transform2 : arrayList) {
                Provider.Service service2 = provider.getService("Cipher", transform2.name);
                if (service2 != null) {
                    return tryTransformWithProvider(initParams, strArr, transform2.needToSet, service2);
                }
            }
            exc = null;
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
        if (exc != null) {
            throw new InvalidKeyException("No provider can be initialized with given key", exc);
        }
        if (initParams == null || initParams.key == null) {
            return null;
        }
        throw new InvalidKeyException("No provider offers " + Arrays.toString(strArr) + " for " + initParams.key.getAlgorithm() + " key of class " + initParams.key.getClass().getName() + " and export format " + initParams.key.getFormat());
    }

    static class CipherSpiAndProvider {
        CipherSpi cipherSpi;
        Provider provider;

        CipherSpiAndProvider(CipherSpi cipherSpi, Provider provider) {
            this.cipherSpi = cipherSpi;
            this.provider = provider;
        }
    }

    static CipherSpiAndProvider tryTransformWithProvider(InitParams initParams, String[] strArr, NeedToSet needToSet, Provider.Service service) throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            if (matchAttribute(service, ATTRIBUTE_MODES, strArr[1]) && matchAttribute(service, ATTRIBUTE_PADDINGS, strArr[2])) {
                CipherSpiAndProvider cipherSpiAndProvider = new CipherSpiAndProvider((CipherSpi) service.newInstance(null), service.getProvider());
                if (cipherSpiAndProvider.cipherSpi != null && cipherSpiAndProvider.provider != null) {
                    CipherSpi cipherSpi = cipherSpiAndProvider.cipherSpi;
                    if ((needToSet == NeedToSet.MODE || needToSet == NeedToSet.BOTH) && strArr[1] != null) {
                        cipherSpi.engineSetMode(strArr[1]);
                    }
                    if ((needToSet == NeedToSet.PADDING || needToSet == NeedToSet.BOTH) && strArr[2] != null) {
                        cipherSpi.engineSetPadding(strArr[2]);
                    }
                    if (initParams != null) {
                        switch (initParams.initType) {
                            case ALGORITHM_PARAMS:
                                cipherSpi.engineInit(initParams.opmode, initParams.key, initParams.params, initParams.random);
                                break;
                            case ALGORITHM_PARAM_SPEC:
                                cipherSpi.engineInit(initParams.opmode, initParams.key, initParams.spec, initParams.random);
                                break;
                            case KEY:
                                cipherSpi.engineInit(initParams.opmode, initParams.key, initParams.random);
                                break;
                            default:
                                throw new AssertionError((Object) "This should never be reached");
                        }
                    }
                    return new CipherSpiAndProvider(cipherSpi, cipherSpiAndProvider.provider);
                }
                return null;
            }
            return null;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            return null;
        }
    }
}
