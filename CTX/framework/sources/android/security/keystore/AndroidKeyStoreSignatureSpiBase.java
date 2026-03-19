package android.security.keystore;

import android.os.IBinder;
import android.security.KeyStore;
import android.security.KeyStoreException;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.OperationResult;
import android.security.keystore.KeyStoreCryptoOperationChunkedStreamer;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.SignatureSpi;
import libcore.util.EmptyArray;

abstract class AndroidKeyStoreSignatureSpiBase extends SignatureSpi implements KeyStoreCryptoOperation {
    private Exception mCachedException;
    private AndroidKeyStoreKey mKey;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private KeyStoreCryptoOperationStreamer mMessageStreamer;
    private long mOperationHandle;
    private IBinder mOperationToken;
    private boolean mSigning;

    protected abstract void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments);

    protected abstract int getAdditionalEntropyAmountForSign();

    AndroidKeyStoreSignatureSpiBase() {
    }

    @Override
    protected final void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        engineInitSign(privateKey, null);
    }

    @Override
    protected final void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
        try {
            if (privateKey == 0) {
                throw new InvalidKeyException("Unsupported key: null");
            }
            if (!(privateKey instanceof AndroidKeyStorePrivateKey)) {
                throw new InvalidKeyException("Unsupported private key type: " + privateKey);
            }
            this.mSigning = true;
            initKey((AndroidKeyStoreKey) privateKey);
            this.appRandom = secureRandom;
            ensureKeystoreOperationInitialized();
        } finally {
            resetAll();
        }
    }

    @Override
    protected final void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        try {
            if (publicKey == null) {
                throw new InvalidKeyException("Unsupported key: null");
            }
            if (!(publicKey instanceof AndroidKeyStorePublicKey)) {
                throw new InvalidKeyException("Unsupported public key type: " + publicKey);
            }
            this.mSigning = false;
            initKey((AndroidKeyStorePublicKey) publicKey);
            this.appRandom = null;
            ensureKeystoreOperationInitialized();
        } finally {
            resetAll();
        }
    }

    protected void initKey(AndroidKeyStoreKey androidKeyStoreKey) throws InvalidKeyException {
        this.mKey = androidKeyStoreKey;
    }

    protected void resetAll() {
        IBinder iBinder = this.mOperationToken;
        if (iBinder != null) {
            this.mOperationToken = null;
            this.mKeyStore.abort(iBinder);
        }
        this.mSigning = false;
        this.mKey = null;
        this.appRandom = null;
        this.mOperationToken = null;
        this.mOperationHandle = 0L;
        this.mMessageStreamer = null;
        this.mCachedException = null;
    }

    protected void resetWhilePreservingInitState() {
        IBinder iBinder = this.mOperationToken;
        if (iBinder != null) {
            this.mOperationToken = null;
            this.mKeyStore.abort(iBinder);
        }
        this.mOperationHandle = 0L;
        this.mMessageStreamer = null;
        this.mCachedException = null;
    }

    private void ensureKeystoreOperationInitialized() throws InvalidKeyException {
        if (this.mMessageStreamer != null || this.mCachedException != null) {
            return;
        }
        if (this.mKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        KeymasterArguments keymasterArguments = new KeymasterArguments();
        addAlgorithmSpecificParametersToBegin(keymasterArguments);
        OperationResult operationResultBegin = this.mKeyStore.begin(this.mKey.getAlias(), this.mSigning ? 2 : 3, true, keymasterArguments, null, this.mKey.getUid());
        if (operationResultBegin == null) {
            throw new KeyStoreConnectException();
        }
        this.mOperationToken = operationResultBegin.token;
        this.mOperationHandle = operationResultBegin.operationHandle;
        InvalidKeyException invalidKeyExceptionForInit = KeyStoreCryptoOperationUtils.getInvalidKeyExceptionForInit(this.mKeyStore, this.mKey, operationResultBegin.resultCode);
        if (invalidKeyExceptionForInit != null) {
            throw invalidKeyExceptionForInit;
        }
        if (this.mOperationToken == null) {
            throw new ProviderException("Keystore returned null operation token");
        }
        if (this.mOperationHandle == 0) {
            throw new ProviderException("Keystore returned invalid operation handle");
        }
        this.mMessageStreamer = createMainDataStreamer(this.mKeyStore, operationResultBegin.token);
    }

    protected KeyStoreCryptoOperationStreamer createMainDataStreamer(KeyStore keyStore, IBinder iBinder) {
        return new KeyStoreCryptoOperationChunkedStreamer(new KeyStoreCryptoOperationChunkedStreamer.MainDataStream(keyStore, iBinder));
    }

    @Override
    public final long getOperationHandle() {
        return this.mOperationHandle;
    }

    @Override
    protected final void engineUpdate(byte[] bArr, int i, int i2) throws SignatureException {
        if (this.mCachedException != null) {
            throw new SignatureException(this.mCachedException);
        }
        try {
            ensureKeystoreOperationInitialized();
            if (i2 == 0) {
                return;
            }
            try {
                byte[] bArrUpdate = this.mMessageStreamer.update(bArr, i, i2);
                if (bArrUpdate.length != 0) {
                    throw new ProviderException("Update operation unexpectedly produced output: " + bArrUpdate.length + " bytes");
                }
            } catch (KeyStoreException e) {
                throw new SignatureException(e);
            }
        } catch (InvalidKeyException e2) {
            throw new SignatureException(e2);
        }
    }

    @Override
    protected final void engineUpdate(byte b) throws SignatureException {
        engineUpdate(new byte[]{b}, 0, 1);
    }

    @Override
    protected final void engineUpdate(ByteBuffer byteBuffer) {
        byte[] bArrArray;
        int iArrayOffset;
        int iRemaining = byteBuffer.remaining();
        if (byteBuffer.hasArray()) {
            bArrArray = byteBuffer.array();
            iArrayOffset = byteBuffer.arrayOffset() + byteBuffer.position();
            byteBuffer.position(byteBuffer.limit());
        } else {
            bArrArray = new byte[iRemaining];
            iArrayOffset = 0;
            byteBuffer.get(bArrArray);
        }
        try {
            engineUpdate(bArrArray, iArrayOffset, iRemaining);
        } catch (SignatureException e) {
            this.mCachedException = e;
        }
    }

    @Override
    protected final int engineSign(byte[] bArr, int i, int i2) throws SignatureException {
        return super.engineSign(bArr, i, i2);
    }

    @Override
    protected final byte[] engineSign() throws SignatureException {
        if (this.mCachedException != null) {
            throw new SignatureException(this.mCachedException);
        }
        try {
            ensureKeystoreOperationInitialized();
            byte[] bArrDoFinal = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.appRandom, getAdditionalEntropyAmountForSign()));
            resetWhilePreservingInitState();
            return bArrDoFinal;
        } catch (KeyStoreException | InvalidKeyException e) {
            throw new SignatureException(e);
        }
    }

    @Override
    protected final boolean engineVerify(byte[] bArr) throws SignatureException {
        boolean z;
        if (this.mCachedException != null) {
            throw new SignatureException(this.mCachedException);
        }
        try {
            ensureKeystoreOperationInitialized();
            try {
                byte[] bArrDoFinal = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, bArr, null);
                if (bArrDoFinal.length != 0) {
                    throw new ProviderException("Signature verification unexpected produced output: " + bArrDoFinal.length + " bytes");
                }
                z = true;
            } catch (KeyStoreException e) {
                if (e.getErrorCode() == -30) {
                    z = false;
                } else {
                    throw new SignatureException(e);
                }
            }
            resetWhilePreservingInitState();
            return z;
        } catch (InvalidKeyException e2) {
            throw new SignatureException(e2);
        }
    }

    @Override
    protected final boolean engineVerify(byte[] bArr, int i, int i2) throws SignatureException {
        return engineVerify(ArrayUtils.subarray(bArr, i, i2));
    }

    @Override
    @Deprecated
    protected final Object engineGetParameter(String str) throws InvalidParameterException {
        throw new InvalidParameterException();
    }

    @Override
    @Deprecated
    protected final void engineSetParameter(String str, Object obj) throws InvalidParameterException {
        throw new InvalidParameterException();
    }

    protected final KeyStore getKeyStore() {
        return this.mKeyStore;
    }

    protected final boolean isSigning() {
        return this.mSigning;
    }
}
