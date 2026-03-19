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
    protected final void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        resetAll();
        try {
            init(i, key, secureRandom);
            initAlgorithmSpecificParameters();
            try {
                ensureKeystoreOperationInitialized();
            } catch (InvalidAlgorithmParameterException e) {
                throw new InvalidKeyException(e);
            }
        } catch (Throwable th) {
            resetAll();
            throw th;
        }
    }

    @Override
    protected final void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            init(i, key, secureRandom);
            initAlgorithmSpecificParameters(algorithmParameters);
            ensureKeystoreOperationInitialized();
        } finally {
            resetAll();
        }
    }

    @Override
    protected final void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            init(i, key, secureRandom);
            initAlgorithmSpecificParameters(algorithmParameterSpec);
            ensureKeystoreOperationInitialized();
        } finally {
            resetAll();
        }
    }

    private void init(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        switch (i) {
            case 1:
            case 3:
                this.mEncrypting = true;
                break;
            case 2:
            case 4:
                this.mEncrypting = false;
                break;
            default:
                throw new InvalidParameterException("Unsupported opmode: " + i);
        }
        initKey(i, key);
        if (this.mKey == null) {
            throw new ProviderException("initKey did not initialize the key");
        }
        this.mRng = secureRandom;
    }

    protected void resetAll() {
        IBinder iBinder = this.mOperationToken;
        if (iBinder != null) {
            this.mKeyStore.abort(iBinder);
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
        IBinder iBinder = this.mOperationToken;
        if (iBinder != null) {
            this.mKeyStore.abort(iBinder);
        }
        this.mOperationToken = null;
        this.mOperationHandle = 0L;
        this.mMainDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamerClosed = false;
        this.mCachedException = null;
    }

    private void ensureKeystoreOperationInitialized() throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (this.mMainDataStreamer == null && this.mCachedException == null) {
            if (this.mKey == null) {
                throw new IllegalStateException("Not initialized");
            }
            KeymasterArguments keymasterArguments = new KeymasterArguments();
            addAlgorithmSpecificParametersToBegin(keymasterArguments);
            OperationResult operationResultBegin = this.mKeyStore.begin(this.mKey.getAlias(), this.mKeymasterPurposeOverride != -1 ? this.mKeymasterPurposeOverride : this.mEncrypting ? 0 : 1, true, keymasterArguments, KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForBegin()), this.mKey.getUid());
            if (operationResultBegin == null) {
                throw new KeyStoreConnectException();
            }
            this.mOperationToken = operationResultBegin.token;
            this.mOperationHandle = operationResultBegin.operationHandle;
            GeneralSecurityException exceptionForCipherInit = KeyStoreCryptoOperationUtils.getExceptionForCipherInit(this.mKeyStore, this.mKey, operationResultBegin.resultCode);
            if (exceptionForCipherInit != null) {
                if (exceptionForCipherInit instanceof InvalidKeyException) {
                    throw ((InvalidKeyException) exceptionForCipherInit);
                }
                if (!(exceptionForCipherInit instanceof InvalidAlgorithmParameterException)) {
                    throw new ProviderException("Unexpected exception type", exceptionForCipherInit);
                }
                throw ((InvalidAlgorithmParameterException) exceptionForCipherInit);
            }
            if (this.mOperationToken == null) {
                throw new ProviderException("Keystore returned null operation token");
            }
            if (this.mOperationHandle == 0) {
                throw new ProviderException("Keystore returned invalid operation handle");
            }
            loadAlgorithmSpecificParametersFromBeginResult(operationResultBegin.outParams);
            this.mMainDataStreamer = createMainDataStreamer(this.mKeyStore, operationResultBegin.token);
            this.mAdditionalAuthenticationDataStreamer = createAdditionalAuthenticationDataStreamer(this.mKeyStore, operationResultBegin.token);
            this.mAdditionalAuthenticationDataStreamerClosed = false;
        }
    }

    protected KeyStoreCryptoOperationStreamer createMainDataStreamer(KeyStore keyStore, IBinder iBinder) {
        return new KeyStoreCryptoOperationChunkedStreamer(new KeyStoreCryptoOperationChunkedStreamer.MainDataStream(keyStore, iBinder));
    }

    protected KeyStoreCryptoOperationStreamer createAdditionalAuthenticationDataStreamer(KeyStore keyStore, IBinder iBinder) {
        return null;
    }

    @Override
    protected final byte[] engineUpdate(byte[] bArr, int i, int i2) {
        if (this.mCachedException != null) {
            return null;
        }
        try {
            ensureKeystoreOperationInitialized();
            if (i2 == 0) {
                return null;
            }
            try {
                flushAAD();
                byte[] bArrUpdate = this.mMainDataStreamer.update(bArr, i, i2);
                if (bArrUpdate.length == 0) {
                    return null;
                }
                return bArrUpdate;
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
        if (this.mAdditionalAuthenticationDataStreamer != null && !this.mAdditionalAuthenticationDataStreamerClosed) {
            try {
                byte[] bArrDoFinal = this.mAdditionalAuthenticationDataStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, null);
                if (bArrDoFinal != null && bArrDoFinal.length > 0) {
                    throw new ProviderException("AAD update unexpectedly returned data: " + bArrDoFinal.length + " bytes");
                }
            } finally {
                this.mAdditionalAuthenticationDataStreamerClosed = true;
            }
        }
    }

    @Override
    protected final int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException {
        byte[] bArrEngineUpdate = engineUpdate(bArr, i, i2);
        if (bArrEngineUpdate == null) {
            return 0;
        }
        int length = bArr2.length - i3;
        if (bArrEngineUpdate.length <= length) {
            System.arraycopy(bArrEngineUpdate, 0, bArr2, i3, bArrEngineUpdate.length);
            return bArrEngineUpdate.length;
        }
        throw new ShortBufferException("Output buffer too short. Produced: " + bArrEngineUpdate.length + ", available: " + length);
    }

    @Override
    protected final int engineUpdate(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws ShortBufferException {
        byte[] bArrEngineUpdate;
        if (byteBuffer == null) {
            throw new NullPointerException("input == null");
        }
        if (byteBuffer2 == null) {
            throw new NullPointerException("output == null");
        }
        int iRemaining = byteBuffer.remaining();
        if (byteBuffer.hasArray()) {
            bArrEngineUpdate = engineUpdate(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), iRemaining);
            byteBuffer.position(byteBuffer.position() + iRemaining);
        } else {
            byte[] bArr = new byte[iRemaining];
            byteBuffer.get(bArr);
            bArrEngineUpdate = engineUpdate(bArr, 0, iRemaining);
        }
        int length = bArrEngineUpdate != null ? bArrEngineUpdate.length : 0;
        if (length > 0) {
            int iRemaining2 = byteBuffer2.remaining();
            try {
                byteBuffer2.put(bArrEngineUpdate);
            } catch (BufferOverflowException e) {
                throw new ShortBufferException("Output buffer too small. Produced: " + length + ", available: " + iRemaining2);
            }
        }
        return length;
    }

    @Override
    protected final void engineUpdateAAD(byte[] bArr, int i, int i2) {
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
                byte[] bArrUpdate = this.mAdditionalAuthenticationDataStreamer.update(bArr, i, i2);
                if (bArrUpdate != null && bArrUpdate.length > 0) {
                    throw new ProviderException("AAD update unexpectedly produced output: " + bArrUpdate.length + " bytes");
                }
            } catch (KeyStoreException e) {
                this.mCachedException = e;
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
            this.mCachedException = e2;
        }
    }

    @Override
    protected final void engineUpdateAAD(ByteBuffer byteBuffer) {
        byte[] bArrArray;
        int iArrayOffset;
        int length;
        if (byteBuffer == null) {
            throw new IllegalArgumentException("src == null");
        }
        if (!byteBuffer.hasRemaining()) {
            return;
        }
        if (byteBuffer.hasArray()) {
            bArrArray = byteBuffer.array();
            iArrayOffset = byteBuffer.arrayOffset() + byteBuffer.position();
            length = byteBuffer.remaining();
            byteBuffer.position(byteBuffer.limit());
        } else {
            bArrArray = new byte[byteBuffer.remaining()];
            iArrayOffset = 0;
            length = bArrArray.length;
            byteBuffer.get(bArrArray);
        }
        engineUpdateAAD(bArrArray, iArrayOffset, length);
    }

    @Override
    protected final byte[] engineDoFinal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException {
        if (this.mCachedException != null) {
            throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(this.mCachedException));
        }
        try {
            ensureKeystoreOperationInitialized();
            try {
                flushAAD();
                byte[] bArrDoFinal = this.mMainDataStreamer.doFinal(bArr, i, i2, null, KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForFinish()));
                resetWhilePreservingInitState();
                return bArrDoFinal;
            } catch (KeyStoreException e) {
                int errorCode = e.getErrorCode();
                if (errorCode == -38) {
                    throw ((BadPaddingException) new BadPaddingException().initCause(e));
                }
                if (errorCode == -30) {
                    throw ((AEADBadTagException) new AEADBadTagException().initCause(e));
                }
                if (errorCode == -21) {
                    throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
                }
                throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
            throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e2));
        }
    }

    @Override
    protected final int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] bArrEngineDoFinal = engineDoFinal(bArr, i, i2);
        if (bArrEngineDoFinal == null) {
            return 0;
        }
        int length = bArr2.length - i3;
        if (bArrEngineDoFinal.length <= length) {
            System.arraycopy(bArrEngineDoFinal, 0, bArr2, i3, bArrEngineDoFinal.length);
            return bArrEngineDoFinal.length;
        }
        throw new ShortBufferException("Output buffer too short. Produced: " + bArrEngineDoFinal.length + ", available: " + length);
    }

    @Override
    protected final int engineDoFinal(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] bArrEngineDoFinal;
        if (byteBuffer == null) {
            throw new NullPointerException("input == null");
        }
        if (byteBuffer2 == null) {
            throw new NullPointerException("output == null");
        }
        int iRemaining = byteBuffer.remaining();
        if (byteBuffer.hasArray()) {
            bArrEngineDoFinal = engineDoFinal(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), iRemaining);
            byteBuffer.position(byteBuffer.position() + iRemaining);
        } else {
            byte[] bArr = new byte[iRemaining];
            byteBuffer.get(bArr);
            bArrEngineDoFinal = engineDoFinal(bArr, 0, iRemaining);
        }
        int length = bArrEngineDoFinal != null ? bArrEngineDoFinal.length : 0;
        if (length > 0) {
            int iRemaining2 = byteBuffer2.remaining();
            try {
                byteBuffer2.put(bArrEngineDoFinal);
            } catch (BufferOverflowException e) {
                throw new ShortBufferException("Output buffer too small. Produced: " + length + ", available: " + iRemaining2);
            }
        }
        return length;
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
                    encoded = ((SecretKeySpec) SecretKeyFactory.getInstance(key.getAlgorithm()).getKeySpec((SecretKey) key, SecretKeySpec.class)).getEncoded();
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
                    encoded = ((PKCS8EncodedKeySpec) KeyFactory.getInstance(key.getAlgorithm()).getKeySpec(key, PKCS8EncodedKeySpec.class)).getEncoded();
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
                    encoded = ((X509EncodedKeySpec) KeyFactory.getInstance(key.getAlgorithm()).getKeySpec(key, X509EncodedKeySpec.class)).getEncoded();
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
    protected final Key engineUnwrap(byte[] bArr, String str, int i) throws NoSuchAlgorithmException, InvalidKeyException {
        if (this.mKey == null) {
            throw new IllegalStateException("Not initilized");
        }
        if (isEncrypting()) {
            throw new IllegalStateException("Cipher must be initialized in Cipher.WRAP_MODE to wrap keys");
        }
        if (bArr == null) {
            throw new NullPointerException("wrappedKey == null");
        }
        try {
            byte[] bArrEngineDoFinal = engineDoFinal(bArr, 0, bArr.length);
            switch (i) {
                case 1:
                    try {
                        return KeyFactory.getInstance(str).generatePublic(new X509EncodedKeySpec(bArrEngineDoFinal));
                    } catch (InvalidKeySpecException e) {
                        throw new InvalidKeyException("Failed to create public key from its X.509 encoded form", e);
                    }
                case 2:
                    try {
                        return KeyFactory.getInstance(str).generatePrivate(new PKCS8EncodedKeySpec(bArrEngineDoFinal));
                    } catch (InvalidKeySpecException e2) {
                        throw new InvalidKeyException("Failed to create private key from its PKCS#8 encoded form", e2);
                    }
                case 3:
                    return new SecretKeySpec(bArrEngineDoFinal, str);
                default:
                    throw new InvalidParameterException("Unsupported wrappedKeyType: " + i);
            }
        } catch (BadPaddingException | IllegalBlockSizeException e3) {
            throw new InvalidKeyException("Failed to unwrap key", e3);
        }
    }

    @Override
    protected final void engineSetMode(String str) throws NoSuchAlgorithmException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final void engineSetPadding(String str) throws NoSuchPaddingException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    public void finalize() throws Throwable {
        try {
            IBinder iBinder = this.mOperationToken;
            if (iBinder != null) {
                this.mKeyStore.abort(iBinder);
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public final long getOperationHandle() {
        return this.mOperationHandle;
    }

    protected final void setKey(AndroidKeyStoreKey androidKeyStoreKey) {
        this.mKey = androidKeyStoreKey;
    }

    protected final void setKeymasterPurposeOverride(int i) {
        this.mKeymasterPurposeOverride = i;
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

    static String opmodeToString(int i) {
        switch (i) {
            case 1:
                return "ENCRYPT_MODE";
            case 2:
                return "DECRYPT_MODE";
            case 3:
                return "WRAP_MODE";
            case 4:
                return "UNWRAP_MODE";
            default:
                return String.valueOf(i);
        }
    }
}
