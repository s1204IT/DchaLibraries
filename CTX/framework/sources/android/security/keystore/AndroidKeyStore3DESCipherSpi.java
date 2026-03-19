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

public class AndroidKeyStore3DESCipherSpi extends AndroidKeyStoreCipherSpiBase {
    private static final int BLOCK_SIZE_BYTES = 8;
    private byte[] mIv;
    private boolean mIvHasBeenUsed;
    private final boolean mIvRequired;
    private final int mKeymasterBlockMode;
    private final int mKeymasterPadding;

    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }

    AndroidKeyStore3DESCipherSpi(int i, int i2, boolean z) {
        this.mKeymasterBlockMode = i;
        this.mKeymasterPadding = i2;
        this.mIvRequired = z;
    }

    static abstract class ECB extends AndroidKeyStore3DESCipherSpi {
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

    static abstract class CBC extends AndroidKeyStore3DESCipherSpi {
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

    @Override
    protected void initKey(int i, Key key) throws InvalidKeyException {
        if (!(key instanceof AndroidKeyStoreSecretKey)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unsupported key: ");
            sb.append(key != null ? key.getClass().getName() : "null");
            throw new InvalidKeyException(sb.toString());
        }
        if (!KeyProperties.KEY_ALGORITHM_3DES.equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only " + KeyProperties.KEY_ALGORITHM_3DES + " supported");
        }
        setKey((AndroidKeyStoreSecretKey) key);
    }

    @Override
    protected int engineGetBlockSize() {
        return 8;
    }

    @Override
    protected int engineGetOutputSize(int i) {
        return i + 24;
    }

    @Override
    protected final byte[] engineGetIV() {
        return ArrayUtils.cloneIfNotEmpty(this.mIv);
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (!this.mIvRequired || this.mIv == null || this.mIv.length <= 0) {
            return null;
        }
        try {
            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance(KeyProperties.KEY_ALGORITHM_3DES);
            algorithmParameters.init(new IvParameterSpec(this.mIv));
            return algorithmParameters;
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException("Failed to obtain 3DES AlgorithmParameters", e);
        } catch (InvalidParameterSpecException e2) {
            throw new ProviderException("Failed to initialize 3DES AlgorithmParameters with an IV", e2);
        }
    }

    @Override
    protected void initAlgorithmSpecificParameters() throws InvalidKeyException {
        if (this.mIvRequired && !isEncrypting()) {
            throw new InvalidKeyException("IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it.");
        }
    }

    @Override
    protected void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
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
    protected void initAlgorithmSpecificParameters(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
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
        if (!KeyProperties.KEY_ALGORITHM_3DES.equalsIgnoreCase(algorithmParameters.getAlgorithm())) {
            throw new InvalidAlgorithmParameterException("Unsupported AlgorithmParameters algorithm: " + algorithmParameters.getAlgorithm() + ". Supported: DESede");
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
            return 8;
        }
        return 0;
    }

    @Override
    protected int getAdditionalEntropyAmountForFinish() {
        return 0;
    }

    @Override
    protected void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments) {
        if (isEncrypting() && this.mIvRequired && this.mIvHasBeenUsed) {
            throw new IllegalStateException("IV has already been used. Reusing IV in encryption mode violates security best practices.");
        }
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 33);
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockMode);
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterPadding);
        if (this.mIvRequired && this.mIv != null) {
            keymasterArguments.addBytes(KeymasterDefs.KM_TAG_NONCE, this.mIv);
        }
    }

    @Override
    protected void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArguments) {
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
    protected final void resetAll() {
        this.mIv = null;
        this.mIvHasBeenUsed = false;
        super.resetAll();
    }
}
