package android.security.keystore;

import android.os.IBinder;
import android.security.KeyStore;
import android.security.KeyStoreException;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.OperationResult;
import android.security.keystore.KeyStoreCryptoOperationChunkedStreamer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import libcore.util.EmptyArray;

abstract class AndroidKeyStoreCipherSpiBase extends CipherSpi implements KeyStoreCryptoOperation {
    private KeyStoreCryptoOperationStreamer mAdditionalAuthenticationDataStreamer;
    private boolean mAdditionalAuthenticationDataStreamerClosed;
    private Exception mCachedException;
    private boolean mEncrypting;
    private AndroidKeyStoreKey mKey;
    private KeyStoreCryptoOperationStreamer mMainDataStreamer;
    private long mOperationHandle;
    private IBinder mOperationToken;
    private SecureRandom mRng;
    private int mKeymasterPurposeOverride = -1;
    private final KeyStore mKeyStore = KeyStore.getInstance();

    protected abstract void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments);

    @Override
    protected abstract AlgorithmParameters engineGetParameters();

    protected abstract int getAdditionalEntropyAmountForBegin();

    protected abstract int getAdditionalEntropyAmountForFinish();

    protected abstract void initAlgorithmSpecificParameters() throws InvalidKeyException;

    protected abstract void initAlgorithmSpecificParameters(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException;

    protected abstract void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException;

    protected abstract void initKey(int i, Key key) throws InvalidKeyException;

    protected abstract void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArguments);

    AndroidKeyStoreCipherSpiBase() {
    }

    @Override
    protected final void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        resetAll();
        boolean success = false;
        try {
            init(opmode, key, random);
            initAlgorithmSpecificParameters();
            try {
                ensureKeystoreOperationInitialized();
                success = true;
            } catch (InvalidAlgorithmParameterException e) {
                throw new InvalidKeyException(e);
            }
        } finally {
            if (!success) {
                resetAll();
            }
        }
    }

    @Override
    protected final void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        resetAll();
        boolean success = false;
        try {
            init(opmode, key, random);
            initAlgorithmSpecificParameters(params);
            ensureKeystoreOperationInitialized();
            success = true;
        } finally {
            if (!success) {
                resetAll();
            }
        }
    }

    @Override
    protected final void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        resetAll();
        boolean success = false;
        try {
            init(opmode, key, random);
            initAlgorithmSpecificParameters(params);
            ensureKeystoreOperationInitialized();
            success = true;
        } finally {
            if (!success) {
                resetAll();
            }
        }
    }

    private void init(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        switch (opmode) {
            case 1:
            case 3:
                this.mEncrypting = true;
                break;
            case 2:
            case 4:
                this.mEncrypting = false;
                break;
            default:
                throw new InvalidParameterException("Unsupported opmode: " + opmode);
        }
        initKey(opmode, key);
        if (this.mKey == null) {
            throw new ProviderException("initKey did not initialize the key");
        }
        this.mRng = random;
    }

    protected void resetAll() {
        IBinder operationToken = this.mOperationToken;
        if (operationToken != null) {
            this.mKeyStore.abort(operationToken);
        }
        this.mEncrypting = false;
        this.mKeymasterPurposeOverride = -1;
        this.mKey = null;
        this.mRng = null;
        this.mOperationToken = null;
        this.mOperationHandle = 0L;
        this.mMainDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamerClosed = false;
        this.mCachedException = null;
    }

    protected void resetWhilePreservingInitState() {
        IBinder operationToken = this.mOperationToken;
        if (operationToken != null) {
            this.mKeyStore.abort(operationToken);
        }
        this.mOperationToken = null;
        this.mOperationHandle = 0L;
        this.mMainDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamerClosed = false;
        this.mCachedException = null;
    }

    private void ensureKeystoreOperationInitialized() throws GeneralSecurityException {
        int purpose;
        if (this.mMainDataStreamer != null || this.mCachedException != null) {
            return;
        }
        if (this.mKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        KeymasterArguments keymasterInputArgs = new KeymasterArguments();
        addAlgorithmSpecificParametersToBegin(keymasterInputArgs);
        byte[] additionalEntropy = KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForBegin());
        if (this.mKeymasterPurposeOverride != -1) {
            purpose = this.mKeymasterPurposeOverride;
        } else {
            purpose = this.mEncrypting ? 0 : 1;
        }
        OperationResult opResult = this.mKeyStore.begin(this.mKey.getAlias(), purpose, true, keymasterInputArgs, additionalEntropy, this.mKey.getUid());
        if (opResult == null) {
            throw new KeyStoreConnectException();
        }
        this.mOperationToken = opResult.token;
        this.mOperationHandle = opResult.operationHandle;
        GeneralSecurityException e = KeyStoreCryptoOperationUtils.getExceptionForCipherInit(this.mKeyStore, this.mKey, opResult.resultCode);
        if (e != null) {
            if (!(e instanceof InvalidKeyException) && !(e instanceof InvalidAlgorithmParameterException)) {
                throw new ProviderException("Unexpected exception type", e);
            }
            throw e;
        }
        if (this.mOperationToken == null) {
            throw new ProviderException("Keystore returned null operation token");
        }
        if (this.mOperationHandle == 0) {
            throw new ProviderException("Keystore returned invalid operation handle");
        }
        loadAlgorithmSpecificParametersFromBeginResult(opResult.outParams);
        this.mMainDataStreamer = createMainDataStreamer(this.mKeyStore, opResult.token);
        this.mAdditionalAuthenticationDataStreamer = createAdditionalAuthenticationDataStreamer(this.mKeyStore, opResult.token);
        this.mAdditionalAuthenticationDataStreamerClosed = false;
    }

    protected KeyStoreCryptoOperationStreamer createMainDataStreamer(KeyStore keyStore, IBinder operationToken) {
        return new KeyStoreCryptoOperationChunkedStreamer(new KeyStoreCryptoOperationChunkedStreamer.MainDataStream(keyStore, operationToken));
    }

    protected KeyStoreCryptoOperationStreamer createAdditionalAuthenticationDataStreamer(KeyStore keyStore, IBinder operationToken) {
        return null;
    }

    @Override
    protected final byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        if (this.mCachedException != null) {
            return null;
        }
        try {
            ensureKeystoreOperationInitialized();
            if (inputLen == 0) {
                return null;
            }
            try {
                flushAAD();
                byte[] output = this.mMainDataStreamer.update(input, inputOffset, inputLen);
                if (output.length == 0) {
                    return null;
                }
                return output;
            } catch (KeyStoreException e) {
                this.mCachedException = e;
                return null;
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
            this.mCachedException = e2;
            return null;
        }
    }

    private void flushAAD() throws KeyStoreException {
        if (this.mAdditionalAuthenticationDataStreamer == null || this.mAdditionalAuthenticationDataStreamerClosed) {
            return;
        }
        try {
            byte[] output = this.mAdditionalAuthenticationDataStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, null);
            if (output == null || output.length <= 0) {
            } else {
                throw new ProviderException("AAD update unexpectedly returned data: " + output.length + " bytes");
            }
        } finally {
            this.mAdditionalAuthenticationDataStreamerClosed = true;
        }
    }

    @Override
    protected final int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        byte[] outputCopy = engineUpdate(input, inputOffset, inputLen);
        if (outputCopy == null) {
            return 0;
        }
        int outputAvailable = output.length - outputOffset;
        if (outputCopy.length > outputAvailable) {
            throw new ShortBufferException("Output buffer too short. Produced: " + outputCopy.length + ", available: " + outputAvailable);
        }
        System.arraycopy(outputCopy, 0, output, outputOffset, outputCopy.length);
        return outputCopy.length;
    }

    @Override
    protected final int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        byte[] outputArray;
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        if (output == null) {
            throw new NullPointerException("output == null");
        }
        int inputSize = input.remaining();
        if (input.hasArray()) {
            outputArray = engineUpdate(input.array(), input.arrayOffset() + input.position(), inputSize);
            input.position(input.position() + inputSize);
        } else {
            byte[] inputArray = new byte[inputSize];
            input.get(inputArray);
            outputArray = engineUpdate(inputArray, 0, inputSize);
        }
        int outputSize = outputArray != null ? outputArray.length : 0;
        if (outputSize > 0) {
            int outputBufferAvailable = output.remaining();
            try {
                output.put(outputArray);
            } catch (BufferOverflowException e) {
                throw new ShortBufferException("Output buffer too small. Produced: " + outputSize + ", available: " + outputBufferAvailable);
            }
        }
        return outputSize;
    }

    @Override
    protected final void engineUpdateAAD(byte[] input, int inputOffset, int inputLen) {
        if (this.mCachedException != null) {
            return;
        }
        try {
            ensureKeystoreOperationInitialized();
            if (this.mAdditionalAuthenticationDataStreamerClosed) {
                throw new IllegalStateException("AAD can only be provided before Cipher.update is invoked");
            }
            if (this.mAdditionalAuthenticationDataStreamer == null) {
                throw new IllegalStateException("This cipher does not support AAD");
            }
            try {
                byte[] output = this.mAdditionalAuthenticationDataStreamer.update(input, inputOffset, inputLen);
                if (output == null || output.length <= 0) {
                } else {
                    throw new ProviderException("AAD update unexpectedly produced output: " + output.length + " bytes");
                }
            } catch (KeyStoreException e) {
                this.mCachedException = e;
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
            this.mCachedException = e2;
        }
    }

    @Override
    protected final void engineUpdateAAD(ByteBuffer src) {
        byte[] input;
        int inputOffset;
        int inputLen;
        if (src == null) {
            throw new IllegalArgumentException("src == null");
        }
        if (!src.hasRemaining()) {
            return;
        }
        if (src.hasArray()) {
            input = src.array();
            inputOffset = src.arrayOffset() + src.position();
            inputLen = src.remaining();
            src.position(src.limit());
        } else {
            input = new byte[src.remaining()];
            inputOffset = 0;
            inputLen = input.length;
            src.get(input);
        }
        engineUpdateAAD(input, inputOffset, inputLen);
    }

    @Override
    protected final byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws BadPaddingException, IllegalBlockSizeException {
        if (this.mCachedException != null) {
            throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(this.mCachedException));
        }
        try {
            ensureKeystoreOperationInitialized();
            try {
                flushAAD();
                byte[] additionalEntropy = KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForFinish());
                byte[] output = this.mMainDataStreamer.doFinal(input, inputOffset, inputLen, null, additionalEntropy);
                resetWhilePreservingInitState();
                return output;
            } catch (KeyStoreException e) {
                switch (e.getErrorCode()) {
                    case -38:
                        throw ((BadPaddingException) new BadPaddingException().initCause(e));
                    case -30:
                        throw ((AEADBadTagException) new AEADBadTagException().initCause(e));
                    case -21:
                        throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
                    default:
                        throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
                }
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
            throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e2));
        }
    }

    @Override
    protected final int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] outputCopy = engineDoFinal(input, inputOffset, inputLen);
        if (outputCopy == null) {
            return 0;
        }
        int outputAvailable = output.length - outputOffset;
        if (outputCopy.length > outputAvailable) {
            throw new ShortBufferException("Output buffer too short. Produced: " + outputCopy.length + ", available: " + outputAvailable);
        }
        System.arraycopy(outputCopy, 0, output, outputOffset, outputCopy.length);
        return outputCopy.length;
    }

    @Override
    protected final int engineDoFinal(ByteBuffer input, ByteBuffer output) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] outputArray;
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        if (output == null) {
            throw new NullPointerException("output == null");
        }
        int inputSize = input.remaining();
        if (input.hasArray()) {
            outputArray = engineDoFinal(input.array(), input.arrayOffset() + input.position(), inputSize);
            input.position(input.position() + inputSize);
        } else {
            byte[] inputArray = new byte[inputSize];
            input.get(inputArray);
            outputArray = engineDoFinal(inputArray, 0, inputSize);
        }
        int outputSize = outputArray != null ? outputArray.length : 0;
        if (outputSize > 0) {
            int outputBufferAvailable = output.remaining();
            try {
                output.put(outputArray);
            } catch (BufferOverflowException e) {
                throw new ShortBufferException("Output buffer too small. Produced: " + outputSize + ", available: " + outputBufferAvailable);
            }
        }
        return outputSize;
    }

    @Override
    protected final byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        if (this.mKey == null) {
            throw new IllegalStateException("Not initilized");
        }
        if (!isEncrypting()) {
            throw new IllegalStateException("Cipher must be initialized in Cipher.WRAP_MODE to wrap keys");
        }
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        byte[] encoded = null;
        if (key instanceof SecretKey) {
            if ("RAW".equalsIgnoreCase(key.getFormat())) {
                encoded = key.getEncoded();
            }
            if (encoded == null) {
                try {
                    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(key.getAlgorithm());
                    SecretKeySpec spec = (SecretKeySpec) keyFactory.getKeySpec((SecretKey) key, SecretKeySpec.class);
                    encoded = spec.getEncoded();
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new InvalidKeyException("Failed to wrap key because it does not export its key material", e);
                }
            }
        } else if (key instanceof PrivateKey) {
            if ("PKCS8".equalsIgnoreCase(key.getFormat())) {
                encoded = key.getEncoded();
            }
            if (encoded == null) {
                try {
                    KeyFactory keyFactory2 = KeyFactory.getInstance(key.getAlgorithm());
                    PKCS8EncodedKeySpec spec2 = (PKCS8EncodedKeySpec) keyFactory2.getKeySpec(key, PKCS8EncodedKeySpec.class);
                    encoded = spec2.getEncoded();
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e2) {
                    throw new InvalidKeyException("Failed to wrap key because it does not export its key material", e2);
                }
            }
        } else if (key instanceof PublicKey) {
            if ("X.509".equalsIgnoreCase(key.getFormat())) {
                encoded = key.getEncoded();
            }
            if (encoded == null) {
                try {
                    KeyFactory keyFactory3 = KeyFactory.getInstance(key.getAlgorithm());
                    X509EncodedKeySpec spec3 = (X509EncodedKeySpec) keyFactory3.getKeySpec(key, X509EncodedKeySpec.class);
                    encoded = spec3.getEncoded();
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e3) {
                    throw new InvalidKeyException("Failed to wrap key because it does not export its key material", e3);
                }
            }
        } else {
            throw new InvalidKeyException("Unsupported key type: " + key.getClass().getName());
        }
        if (encoded == null) {
            throw new InvalidKeyException("Failed to wrap key because it does not export its key material");
        }
        try {
            return engineDoFinal(encoded, 0, encoded.length);
        } catch (BadPaddingException e4) {
            throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e4));
        }
    }

    @Override
    protected final Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws NoSuchAlgorithmException, InvalidKeyException {
        if (this.mKey == null) {
            throw new IllegalStateException("Not initilized");
        }
        if (isEncrypting()) {
            throw new IllegalStateException("Cipher must be initialized in Cipher.WRAP_MODE to wrap keys");
        }
        if (wrappedKey == null) {
            throw new NullPointerException("wrappedKey == null");
        }
        try {
            byte[] encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            switch (wrappedKeyType) {
                case 1:
                    KeyFactory keyFactory = KeyFactory.getInstance(wrappedKeyAlgorithm);
                    try {
                        return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
                    } catch (InvalidKeySpecException e) {
                        throw new InvalidKeyException("Failed to create public key from its X.509 encoded form", e);
                    }
                case 2:
                    KeyFactory keyFactory2 = KeyFactory.getInstance(wrappedKeyAlgorithm);
                    try {
                        return keyFactory2.generatePrivate(new PKCS8EncodedKeySpec(encoded));
                    } catch (InvalidKeySpecException e2) {
                        throw new InvalidKeyException("Failed to create private key from its PKCS#8 encoded form", e2);
                    }
                case 3:
                    return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
                default:
                    throw new InvalidParameterException("Unsupported wrappedKeyType: " + wrappedKeyType);
            }
        } catch (BadPaddingException | IllegalBlockSizeException e3) {
            throw new InvalidKeyException("Failed to unwrap key", e3);
        }
    }

    @Override
    protected final void engineSetMode(String mode) throws NoSuchAlgorithmException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final void engineSetPadding(String arg0) throws NoSuchPaddingException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    public void finalize() throws Throwable {
        try {
            IBinder operationToken = this.mOperationToken;
            if (operationToken != null) {
                this.mKeyStore.abort(operationToken);
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public final long getOperationHandle() {
        return this.mOperationHandle;
    }

    protected final void setKey(AndroidKeyStoreKey key) {
        this.mKey = key;
    }

    protected final void setKeymasterPurposeOverride(int keymasterPurpose) {
        this.mKeymasterPurposeOverride = keymasterPurpose;
    }

    protected final int getKeymasterPurposeOverride() {
        return this.mKeymasterPurposeOverride;
    }

    protected final boolean isEncrypting() {
        return this.mEncrypting;
    }

    protected final KeyStore getKeyStore() {
        return this.mKeyStore;
    }

    protected final long getConsumedInputSizeBytes() {
        if (this.mMainDataStreamer == null) {
            throw new IllegalStateException("Not initialized");
        }
        return this.mMainDataStreamer.getConsumedInputSizeBytes();
    }

    protected final long getProducedOutputSizeBytes() {
        if (this.mMainDataStreamer == null) {
            throw new IllegalStateException("Not initialized");
        }
        return this.mMainDataStreamer.getProducedOutputSizeBytes();
    }

    static String opmodeToString(int opmode) {
        switch (opmode) {
            case 1:
                return "ENCRYPT_MODE";
            case 2:
                return "DECRYPT_MODE";
            case 3:
                return "WRAP_MODE";
            case 4:
                return "UNWRAP_MODE";
            default:
                return String.valueOf(opmode);
        }
    }
}
