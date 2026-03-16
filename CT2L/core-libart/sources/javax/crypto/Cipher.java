package javax.crypto;

import java.nio.ByteBuffer;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import org.apache.harmony.crypto.internal.NullCipherSpi;
import org.apache.harmony.security.fortress.Engine;

public class Cipher {
    private static final String ATTRIBUTE_MODES = "SupportedModes";
    private static final String ATTRIBUTE_PADDINGS = "SupportedPaddings";
    public static final int DECRYPT_MODE = 2;
    public static final int ENCRYPT_MODE = 1;
    public static final int PRIVATE_KEY = 2;
    public static final int PUBLIC_KEY = 1;
    public static final int SECRET_KEY = 3;
    public static final int UNWRAP_MODE = 4;
    public static final int WRAP_MODE = 3;
    private static SecureRandom secureRandom;
    private final Object initLock;
    private int mode;
    private Provider provider;
    private final Provider specifiedProvider;
    private final CipherSpi specifiedSpi;
    private CipherSpi spiImpl;
    private final String[] transformParts;
    private final String transformation;
    private static final String SERVICE = "Cipher";
    private static final Engine ENGINE = new Engine(SERVICE);

    private enum NeedToSet {
        NONE,
        MODE,
        PADDING,
        BOTH
    }

    protected Cipher(CipherSpi cipherSpi, Provider provider, String transformation) {
        this.initLock = new Object();
        if (cipherSpi == null) {
            throw new NullPointerException("cipherSpi == null");
        }
        if (!(cipherSpi instanceof NullCipherSpi) && provider == null) {
            throw new NullPointerException("provider == null");
        }
        this.specifiedProvider = provider;
        this.specifiedSpi = cipherSpi;
        this.transformation = transformation;
        this.transformParts = null;
    }

    private Cipher(String transformation, String[] transformParts, Provider provider) {
        this.initLock = new Object();
        this.transformation = transformation;
        this.transformParts = transformParts;
        this.specifiedProvider = provider;
        this.specifiedSpi = null;
    }

    public static final Cipher getInstance(String transformation) throws NoSuchPaddingException, NoSuchAlgorithmException {
        return getCipher(transformation, null);
    }

    public static final Cipher getInstance(String transformation, String provider) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("Provider not available: " + provider);
        }
        return getInstance(transformation, p);
    }

    public static final Cipher getInstance(String transformation, Provider provider) throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        return getCipher(transformation, provider);
    }

    private static NoSuchAlgorithmException invalidTransformation(String transformation) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException("Invalid transformation: " + transformation);
    }

    private static Cipher getCipher(String transformation, Provider provider) throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (transformation == null || transformation.isEmpty()) {
            throw invalidTransformation(transformation);
        }
        String[] transformParts = checkTransformation(transformation);
        if (tryCombinations(null, provider, transformParts) == null) {
            if (provider == null) {
                throw new NoSuchAlgorithmException("No provider found for " + transformation);
            }
            throw new NoSuchAlgorithmException("Provider " + provider.getName() + " does not provide " + transformation);
        }
        return new Cipher(transformation, transformParts, provider);
    }

    private static String[] checkTransformation(String transformation) throws NoSuchAlgorithmException {
        if (transformation.startsWith("/")) {
            transformation = transformation.substring(1);
        }
        String[] pieces = transformation.split("/");
        if (pieces.length > 3) {
            throw invalidTransformation(transformation);
        }
        String[] result = new String[3];
        for (int i = 0; i < pieces.length; i++) {
            String piece = pieces[i].trim();
            if (!piece.isEmpty()) {
                result[i] = piece;
            }
        }
        if (result[0] == null) {
            throw invalidTransformation(transformation);
        }
        if ((result[1] != null || result[2] != null) && (result[1] == null || result[2] == null)) {
            throw invalidTransformation(transformation);
        }
        return result;
    }

    private CipherSpi getSpi(Key key) {
        if (this.specifiedSpi != null) {
            return this.specifiedSpi;
        }
        synchronized (this.initLock) {
            if (this.spiImpl != null && key == null) {
                return this.spiImpl;
            }
            Engine.SpiAndProvider sap = tryCombinations(key, this.specifiedProvider, this.transformParts);
            if (sap == null) {
                throw new ProviderException("No provider for " + this.transformation);
            }
            this.spiImpl = (CipherSpi) sap.spi;
            this.provider = sap.provider;
            return this.spiImpl;
        }
    }

    private CipherSpi getSpi() {
        return getSpi(null);
    }

    private static Engine.SpiAndProvider tryCombinations(Key key, Provider provider, String[] transformParts) {
        Engine.SpiAndProvider sap;
        Engine.SpiAndProvider sap2;
        Engine.SpiAndProvider sap3;
        return (transformParts[1] == null || transformParts[2] == null || (sap3 = tryTransform(key, provider, new StringBuilder().append(transformParts[0]).append("/").append(transformParts[1]).append("/").append(transformParts[2]).toString(), transformParts, NeedToSet.NONE)) == null) ? (transformParts[1] == null || (sap2 = tryTransform(key, provider, new StringBuilder().append(transformParts[0]).append("/").append(transformParts[1]).toString(), transformParts, NeedToSet.PADDING)) == null) ? (transformParts[2] == null || (sap = tryTransform(key, provider, new StringBuilder().append(transformParts[0]).append("//").append(transformParts[2]).toString(), transformParts, NeedToSet.MODE)) == null) ? tryTransform(key, provider, transformParts[0], transformParts, NeedToSet.BOTH) : sap : sap2 : sap3;
    }

    private static Engine.SpiAndProvider tryTransform(Key key, Provider provider, String transform, String[] transformParts, NeedToSet type) {
        if (provider != null) {
            Provider.Service service = provider.getService(SERVICE, transform);
            if (service == null) {
                return null;
            }
            return tryTransformWithProvider(key, transformParts, type, service);
        }
        ArrayList<Provider.Service> services = ENGINE.getServices(transform);
        if (services == null) {
            return null;
        }
        Iterator<Provider.Service> it = services.iterator();
        while (it.hasNext()) {
            Engine.SpiAndProvider sap = tryTransformWithProvider(key, transformParts, type, it.next());
            if (sap != null) {
                return sap;
            }
        }
        return null;
    }

    private static Engine.SpiAndProvider tryTransformWithProvider(Key key, String[] transformParts, NeedToSet type, Provider.Service service) {
        if (key != null) {
            try {
                if (!service.supportsParameter(key)) {
                    return null;
                }
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                return null;
            }
        }
        if (!matchAttribute(service, ATTRIBUTE_MODES, transformParts[1]) || !matchAttribute(service, ATTRIBUTE_PADDINGS, transformParts[2])) {
            return null;
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(service, (String) null);
        if (sap.spi == null || sap.provider == null) {
            return null;
        }
        if (!(sap.spi instanceof CipherSpi)) {
            return null;
        }
        CipherSpi spi = (CipherSpi) sap.spi;
        if ((type == NeedToSet.MODE || type == NeedToSet.BOTH) && transformParts[1] != null) {
            spi.engineSetMode(transformParts[1]);
        }
        if ((type == NeedToSet.PADDING || type == NeedToSet.BOTH) && transformParts[2] != null) {
            spi.engineSetPadding(transformParts[2]);
            return sap;
        }
        return sap;
    }

    private static boolean matchAttribute(Provider.Service service, String attr, String value) {
        String pattern;
        if (value == null || (pattern = service.getAttribute(attr)) == null) {
            return true;
        }
        String valueUc = value.toUpperCase(Locale.US);
        return valueUc.matches(pattern.toUpperCase(Locale.US));
    }

    public final Provider getProvider() {
        getSpi();
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.transformation;
    }

    public final int getBlockSize() {
        return getSpi().engineGetBlockSize();
    }

    public final int getOutputSize(int inputLen) {
        if (this.mode == 0) {
            throw new IllegalStateException("Cipher has not yet been initialized");
        }
        return getSpi().engineGetOutputSize(inputLen);
    }

    public final byte[] getIV() {
        return getSpi().engineGetIV();
    }

    public final AlgorithmParameters getParameters() {
        return getSpi().engineGetParameters();
    }

    public final ExemptionMechanism getExemptionMechanism() {
        return null;
    }

    private void checkMode(int mode) {
        if (mode != 1 && mode != 2 && mode != 4 && mode != 3) {
            throw new InvalidParameterException("Invalid mode: " + mode);
        }
    }

    public final void init(int opmode, Key key) throws InvalidKeyException {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        init(opmode, key, secureRandom);
    }

    public final void init(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        checkMode(opmode);
        getSpi(key).engineInit(opmode, key, random);
        this.mode = opmode;
    }

    public final void init(int opmode, Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        init(opmode, key, params, secureRandom);
    }

    public final void init(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        checkMode(opmode);
        getSpi(key).engineInit(opmode, key, params, random);
        this.mode = opmode;
    }

    public final void init(int opmode, Key key, AlgorithmParameters params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        init(opmode, key, params, secureRandom);
    }

    public final void init(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        checkMode(opmode);
        getSpi(key).engineInit(opmode, key, params, random);
        this.mode = opmode;
    }

    public final void init(int opmode, Certificate certificate) throws InvalidKeyException {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        init(opmode, certificate, secureRandom);
    }

    public final void init(int opmode, Certificate certificate, SecureRandom random) throws InvalidKeyException {
        boolean[] keyUsage;
        checkMode(opmode);
        if (certificate instanceof X509Certificate) {
            Set<String> ce = ((X509Certificate) certificate).getCriticalExtensionOIDs();
            boolean critical = false;
            if (ce != null && !ce.isEmpty()) {
                Iterator<String> it = ce.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    String oid = it.next();
                    if (oid.equals("2.5.29.15")) {
                        critical = true;
                        break;
                    }
                }
                if (critical && (keyUsage = ((X509Certificate) certificate).getKeyUsage()) != null) {
                    if (opmode == 1 && !keyUsage[3]) {
                        throw new InvalidKeyException("The public key in the certificate cannot be used for ENCRYPT_MODE");
                    }
                    if (opmode == 3 && !keyUsage[2]) {
                        throw new InvalidKeyException("The public key in the certificate cannot be used for WRAP_MODE");
                    }
                }
            }
        }
        Key key = certificate.getPublicKey();
        getSpi(key).engineInit(opmode, key, random);
        this.mode = opmode;
    }

    public final byte[] update(byte[] input) {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        if (input.length == 0) {
            return null;
        }
        return getSpi().engineUpdate(input, 0, input.length);
    }

    public final byte[] update(byte[] input, int inputOffset, int inputLen) {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        checkInputOffsetAndCount(input.length, inputOffset, inputLen);
        if (input.length == 0) {
            return null;
        }
        return getSpi().engineUpdate(input, inputOffset, inputLen);
    }

    private static void checkInputOffsetAndCount(int inputArrayLength, int inputOffset, int inputLen) {
        if ((inputOffset | inputLen) < 0 || inputOffset > inputArrayLength || inputArrayLength - inputOffset < inputLen) {
            throw new IllegalArgumentException("input.length=" + inputArrayLength + "; inputOffset=" + inputOffset + "; inputLen=" + inputLen);
        }
    }

    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output) throws ShortBufferException {
        return update(input, inputOffset, inputLen, output, 0);
    }

    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        if (output == null) {
            throw new IllegalArgumentException("output == null");
        }
        if (outputOffset < 0) {
            throw new IllegalArgumentException("outputOffset < 0. outputOffset=" + outputOffset);
        }
        checkInputOffsetAndCount(input.length, inputOffset, inputLen);
        if (input.length == 0) {
            return 0;
        }
        return getSpi().engineUpdate(input, inputOffset, inputLen, output, outputOffset);
    }

    public final int update(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (input == output) {
            throw new IllegalArgumentException("input == output");
        }
        return getSpi().engineUpdate(input, output);
    }

    public final void updateAAD(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (input.length != 0) {
            getSpi().engineUpdateAAD(input, 0, input.length);
        }
    }

    public final void updateAAD(byte[] input, int inputOffset, int inputLen) {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        checkInputOffsetAndCount(input.length, inputOffset, inputLen);
        if (input.length != 0) {
            getSpi().engineUpdateAAD(input, inputOffset, inputLen);
        }
    }

    public final void updateAAD(ByteBuffer input) {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException("Cipher is not initialized");
        }
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        getSpi().engineUpdateAAD(input);
    }

    public final byte[] doFinal() throws BadPaddingException, IllegalBlockSizeException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        return getSpi().engineDoFinal(null, 0, 0);
    }

    public final int doFinal(byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (outputOffset < 0) {
            throw new IllegalArgumentException("outputOffset < 0. outputOffset=" + outputOffset);
        }
        return getSpi().engineDoFinal(null, 0, 0, output, outputOffset);
    }

    public final byte[] doFinal(byte[] input) throws BadPaddingException, IllegalBlockSizeException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        return getSpi().engineDoFinal(input, 0, input.length);
    }

    public final byte[] doFinal(byte[] input, int inputOffset, int inputLen) throws BadPaddingException, IllegalBlockSizeException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        checkInputOffsetAndCount(input.length, inputOffset, inputLen);
        return getSpi().engineDoFinal(input, inputOffset, inputLen);
    }

    public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        return doFinal(input, inputOffset, inputLen, output, 0);
    }

    public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        checkInputOffsetAndCount(input.length, inputOffset, inputLen);
        return getSpi().engineDoFinal(input, inputOffset, inputLen, output, outputOffset);
    }

    public final int doFinal(ByteBuffer input, ByteBuffer output) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        if (this.mode != 1 && this.mode != 2) {
            throw new IllegalStateException();
        }
        if (input == output) {
            throw new IllegalArgumentException("input == output");
        }
        return getSpi().engineDoFinal(input, output);
    }

    public final byte[] wrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        if (this.mode != 3) {
            throw new IllegalStateException();
        }
        return getSpi().engineWrap(key);
    }

    public final Key unwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws NoSuchAlgorithmException, InvalidKeyException {
        if (this.mode != 4) {
            throw new IllegalStateException();
        }
        return getSpi().engineUnwrap(wrappedKey, wrappedKeyAlgorithm, wrappedKeyType);
    }

    public static final int getMaxAllowedKeyLength(String transformation) throws NoSuchAlgorithmException {
        if (transformation == null) {
            throw new NullPointerException("transformation == null");
        }
        checkTransformation(transformation);
        return Integer.MAX_VALUE;
    }

    public static final AlgorithmParameterSpec getMaxAllowedParameterSpec(String transformation) throws NoSuchAlgorithmException {
        if (transformation == null) {
            throw new NullPointerException("transformation == null");
        }
        checkTransformation(transformation);
        return null;
    }
}
