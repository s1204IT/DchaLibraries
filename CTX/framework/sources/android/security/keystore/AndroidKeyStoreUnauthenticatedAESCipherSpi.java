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
        protected ECB(int i) {
            super(1, i, false);
        }

        public static class NoPadding extends ECB {
            @Override
            public void finalize() throws Throwable {
                super.finalize();
            }

            public NoPadding() {
                super(1);
            }
        }

        public static class PKCS7Padding extends ECB {
            @Override
            public void finalize() throws Throwable {
                super.finalize();
            }

            public PKCS7Padding() {
                super(64);
            }
        }
    }

    static abstract class CBC extends AndroidKeyStoreUnauthenticatedAESCipherSpi {
        protected CBC(int i) {
            super(2, i, true);
        }

        public static class NoPadding extends CBC {
            @Override
            public void finalize() throws Throwable {
                super.finalize();
            }

            public NoPadding() {
                super(1);
            }
        }

        public static class PKCS7Padding extends CBC {
            @Override
            public void finalize() throws Throwable {
                super.finalize();
            }

            public PKCS7Padding() {
                super(64);
            }
        }
    }

    static abstract class CTR extends AndroidKeyStoreUnauthenticatedAESCipherSpi {
        protected CTR(int i) {
            super(3, i, true);
        }

        public static class NoPadding extends CTR {
            @Override
            public void finalize() throws Throwable {
                super.finalize();
            }

            public NoPadding() {
                super(1);
            }
        }
    }

    AndroidKeyStoreUnauthenticatedAESCipherSpi(int i, int i2, boolean z) {
        this.mKeymasterBlockMode = i;
        this.mKeymasterPadding = i2;
        this.mIvRequired = z;
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
    protected final void initKey(int i, Key key) throws InvalidKeyException {
        if (!(key instanceof AndroidKeyStoreSecretKey)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unsupported key: ");
            sb.append(key != null ? key.getClass().getName() : "null");
            throw new InvalidKeyException(sb.toString());
        }
        if (!"AES".equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only AES supported");
        }
        setKey((AndroidKeyStoreSecretKey) key);
    }

    @Override
    protected final void initAlgorithmSpecificParameters() throws InvalidKeyException {
        if (this.mIvRequired && !isEncrypting()) {
            throw new InvalidKeyException("IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it.");
        }
    }

    @Override
    protected final void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        if (!this.mIvRequired) {
            if (algorithmParameterSpec != null) {
                throw new InvalidAlgorithmParameterException("Unsupported parameters: " + algorithmParameterSpec);
            }
            return;
        }
        if (algorithmParameterSpec == null) {
            if (!isEncrypting()) {
                throw new InvalidAlgorithmParameterException("IvParameterSpec must be provided when decrypting");
            }
        } else {
            if (!(algorithmParameterSpec instanceof IvParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Only IvParameterSpec supported");
            }
            this.mIv = ((IvParameterSpec) algorithmParameterSpec).getIV();
            if (this.mIv == null) {
                throw new InvalidAlgorithmParameterException("Null IV in IvParameterSpec");
            }
        }
    }

    @Override
    protected final void initAlgorithmSpecificParameters(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
        if (!this.mIvRequired) {
            if (algorithmParameters != null) {
                throw new InvalidAlgorithmParameterException("Unsupported parameters: " + algorithmParameters);
            }
            return;
        }
        if (algorithmParameters == null) {
            if (!isEncrypting()) {
                throw new InvalidAlgorithmParameterException("IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it.");
            }
            return;
        }
        if (!"AES".equalsIgnoreCase(algorithmParameters.getAlgorithm())) {
            throw new InvalidAlgorithmParameterException("Unsupported AlgorithmParameters algorithm: " + algorithmParameters.getAlgorithm() + ". Supported: AES");
        }
        try {
            this.mIv = ((IvParameterSpec) algorithmParameters.getParameterSpec(IvParameterSpec.class)).getIV();
            if (this.mIv == null) {
                throw new InvalidAlgorithmParameterException("Null IV in AlgorithmParameters");
            }
        } catch (InvalidParameterSpecException e) {
            if (!isEncrypting()) {
                throw new InvalidAlgorithmParameterException("IV required when decrypting, but not found in parameters: " + algorithmParameters, e);
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
    protected final void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments) {
        if (isEncrypting() && this.mIvRequired && this.mIvHasBeenUsed) {
            throw new IllegalStateException("IV has already been used. Reusing IV in encryption mode violates security best practices.");
        }
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 32);
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockMode);
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterPadding);
        if (this.mIvRequired && this.mIv != null) {
            keymasterArguments.addBytes(KeymasterDefs.KM_TAG_NONCE, this.mIv);
        }
    }

    @Override
    protected final void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArguments) {
        this.mIvHasBeenUsed = true;
        byte[] bytes = keymasterArguments.getBytes(KeymasterDefs.KM_TAG_NONCE, null);
        if (bytes != null && bytes.length == 0) {
            bytes = null;
        }
        if (this.mIvRequired) {
            if (this.mIv == null) {
                this.mIv = bytes;
                return;
            } else {
                if (bytes != null && !Arrays.equals(bytes, this.mIv)) {
                    throw new ProviderException("IV in use differs from provided IV");
                }
                return;
            }
        }
        if (bytes != null) {
            throw new ProviderException("IV in use despite IV not being used by this transformation");
        }
    }

    @Override
    protected final int engineGetBlockSize() {
        return 16;
    }

    @Override
    protected final int engineGetOutputSize(int i) {
        return i + 48;
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
            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("AES");
            algorithmParameters.init(new IvParameterSpec(this.mIv));
            return algorithmParameters;
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException("Failed to obtain AES AlgorithmParameters", e);
        } catch (InvalidParameterSpecException e2) {
            throw new ProviderException("Failed to initialize AES AlgorithmParameters with an IV", e2);
        }
    }
}
