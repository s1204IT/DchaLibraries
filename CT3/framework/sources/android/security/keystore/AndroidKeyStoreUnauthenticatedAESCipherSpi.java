package android.security.keystore;

import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import javax.crypto.spec.IvParameterSpec;

class AndroidKeyStoreUnauthenticatedAESCipherSpi extends AndroidKeyStoreCipherSpiBase {
    private static final int BLOCK_SIZE_BYTES = 16;
    private byte[] mIv;
    private boolean mIvHasBeenUsed;
    private final boolean mIvRequired;
    private final int mKeymasterBlockMode;
    private final int mKeymasterPadding;

    static abstract class ECB extends AndroidKeyStoreUnauthenticatedAESCipherSpi {
        protected ECB(int keymasterPadding) {
            super(1, keymasterPadding, false);
        }

        public static class NoPadding extends ECB {
            @Override
            public void finalize() {
                super.finalize();
            }

            public NoPadding() {
                super(1);
            }
        }

        public static class PKCS7Padding extends ECB {
            @Override
            public void finalize() {
                super.finalize();
            }

            public PKCS7Padding() {
                super(64);
            }
        }
    }

    static abstract class CBC extends AndroidKeyStoreUnauthenticatedAESCipherSpi {
        protected CBC(int keymasterPadding) {
            super(2, keymasterPadding, true);
        }

        public static class NoPadding extends CBC {
            @Override
            public void finalize() {
                super.finalize();
            }

            public NoPadding() {
                super(1);
            }
        }

        public static class PKCS7Padding extends CBC {
            @Override
            public void finalize() {
                super.finalize();
            }

            public PKCS7Padding() {
                super(64);
            }
        }
    }

    static abstract class CTR extends AndroidKeyStoreUnauthenticatedAESCipherSpi {
        protected CTR(int keymasterPadding) {
            super(3, keymasterPadding, true);
        }

        public static class NoPadding extends CTR {
            @Override
            public void finalize() {
                super.finalize();
            }

            public NoPadding() {
                super(1);
            }
        }
    }

    AndroidKeyStoreUnauthenticatedAESCipherSpi(int keymasterBlockMode, int keymasterPadding, boolean ivRequired) {
        this.mKeymasterBlockMode = keymasterBlockMode;
        this.mKeymasterPadding = keymasterPadding;
        this.mIvRequired = ivRequired;
    }

    @Override
    protected final void resetAll() {
        this.mIv = null;
        this.mIvHasBeenUsed = false;
        super.resetAll();
    }

    @Override
    protected final void resetWhilePreservingInitState() {
        super.resetWhilePreservingInitState();
    }

    @Override
    protected final void initKey(int opmode, Key key) throws InvalidKeyException {
        if (!(key instanceof AndroidKeyStoreSecretKey)) {
            throw new InvalidKeyException("Unsupported key: " + (key != 0 ? key.getClass().getName() : "null"));
        }
        if (KeyProperties.KEY_ALGORITHM_AES.equalsIgnoreCase(key.getAlgorithm())) {
            setKey(key);
            return;
        }
        throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only " + KeyProperties.KEY_ALGORITHM_AES + " supported");
    }

    @Override
    protected final void initAlgorithmSpecificParameters() throws InvalidKeyException {
        if (!this.mIvRequired || isEncrypting()) {
        } else {
            throw new InvalidKeyException("IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it.");
        }
    }

    @Override
    protected final void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        if (!this.mIvRequired) {
            if (algorithmParameterSpec != 0) {
                throw new InvalidAlgorithmParameterException("Unsupported parameters: " + algorithmParameterSpec);
            }
        } else if (algorithmParameterSpec == 0) {
            if (!isEncrypting()) {
                throw new InvalidAlgorithmParameterException("IvParameterSpec must be provided when decrypting");
            }
        } else {
            if (!(algorithmParameterSpec instanceof IvParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Only IvParameterSpec supported");
            }
            this.mIv = algorithmParameterSpec.getIV();
            if (this.mIv != null) {
            } else {
                throw new InvalidAlgorithmParameterException("Null IV in IvParameterSpec");
            }
        }
    }

    @Override
    protected final void initAlgorithmSpecificParameters(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
        if (!this.mIvRequired) {
            if (params != null) {
                throw new InvalidAlgorithmParameterException("Unsupported parameters: " + params);
            }
            return;
        }
        if (params == null) {
            if (!isEncrypting()) {
                throw new InvalidAlgorithmParameterException("IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it.");
            }
            return;
        }
        if (!KeyProperties.KEY_ALGORITHM_AES.equalsIgnoreCase(params.getAlgorithm())) {
            throw new InvalidAlgorithmParameterException("Unsupported AlgorithmParameters algorithm: " + params.getAlgorithm() + ". Supported: AES");
        }
        try {
            IvParameterSpec ivSpec = (IvParameterSpec) params.getParameterSpec(IvParameterSpec.class);
            this.mIv = ivSpec.getIV();
            if (this.mIv != null) {
            } else {
                throw new InvalidAlgorithmParameterException("Null IV in AlgorithmParameters");
            }
        } catch (InvalidParameterSpecException e) {
            if (!isEncrypting()) {
                throw new InvalidAlgorithmParameterException("IV required when decrypting, but not found in parameters: " + params, e);
            }
            this.mIv = null;
        }
    }

    @Override
    protected final int getAdditionalEntropyAmountForBegin() {
        if (this.mIvRequired && this.mIv == null && isEncrypting()) {
            return 16;
        }
        return 0;
    }

    @Override
    protected final int getAdditionalEntropyAmountForFinish() {
        return 0;
    }

    @Override
    protected final void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArgs) {
        if (isEncrypting() && this.mIvRequired && this.mIvHasBeenUsed) {
            throw new IllegalStateException("IV has already been used. Reusing IV in encryption mode violates security best practices.");
        }
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 32);
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockMode);
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterPadding);
        if (!this.mIvRequired || this.mIv == null) {
            return;
        }
        keymasterArgs.addBytes(KeymasterDefs.KM_TAG_NONCE, this.mIv);
    }

    @Override
    protected final void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArgs) {
        this.mIvHasBeenUsed = true;
        byte[] returnedIv = keymasterArgs.getBytes(KeymasterDefs.KM_TAG_NONCE, null);
        if (returnedIv != null && returnedIv.length == 0) {
            returnedIv = null;
        }
        if (this.mIvRequired) {
            if (this.mIv == null) {
                this.mIv = returnedIv;
                return;
            } else if (returnedIv == null || Arrays.equals(returnedIv, this.mIv)) {
                return;
            } else {
                throw new ProviderException("IV in use differs from provided IV");
            }
        }
        if (returnedIv == null) {
        } else {
            throw new ProviderException("IV in use despite IV not being used by this transformation");
        }
    }

    @Override
    protected final int engineGetBlockSize() {
        return 16;
    }

    @Override
    protected final int engineGetOutputSize(int inputLen) {
        return inputLen + 48;
    }

    @Override
    protected final byte[] engineGetIV() {
        return ArrayUtils.cloneIfNotEmpty(this.mIv);
    }

    @Override
    protected final AlgorithmParameters engineGetParameters() {
        if (!this.mIvRequired || this.mIv == null || this.mIv.length <= 0) {
            return null;
        }
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance(KeyProperties.KEY_ALGORITHM_AES);
            params.init(new IvParameterSpec(this.mIv));
            return params;
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException("Failed to obtain AES AlgorithmParameters", e);
        } catch (InvalidParameterSpecException e2) {
            throw new ProviderException("Failed to initialize AES AlgorithmParameters with an IV", e2);
        }
    }
}
