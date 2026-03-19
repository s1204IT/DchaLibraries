package android.security.keystore;

import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

abstract class AndroidKeyStoreRSACipherSpi extends AndroidKeyStoreCipherSpiBase {
    private final int mKeymasterPadding;
    private int mKeymasterPaddingOverride;
    private int mModulusSizeBytes = -1;

    public static final class NoPadding extends AndroidKeyStoreRSACipherSpi {
        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }

        public NoPadding() {
            super(1);
        }

        @Override
        protected boolean adjustConfigForEncryptingWithPrivateKey() {
            setKeymasterPurposeOverride(2);
            return true;
        }

        @Override
        protected void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        @Override
        protected void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
            if (algorithmParameterSpec != null) {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + algorithmParameterSpec + ". No parameters supported");
            }
        }

        @Override
        protected void initAlgorithmSpecificParameters(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
            if (algorithmParameters != null) {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + algorithmParameters + ". No parameters supported");
            }
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            return null;
        }

        @Override
        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        @Override
        protected final int getAdditionalEntropyAmountForFinish() {
            return 0;
        }
    }

    public static final class PKCS1Padding extends AndroidKeyStoreRSACipherSpi {
        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }

        public PKCS1Padding() {
            super(4);
        }

        @Override
        protected boolean adjustConfigForEncryptingWithPrivateKey() {
            setKeymasterPurposeOverride(2);
            setKeymasterPaddingOverride(5);
            return true;
        }

        @Override
        protected void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        @Override
        protected void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
            if (algorithmParameterSpec != null) {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + algorithmParameterSpec + ". No parameters supported");
            }
        }

        @Override
        protected void initAlgorithmSpecificParameters(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
            if (algorithmParameters != null) {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + algorithmParameters + ". No parameters supported");
            }
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            return null;
        }

        @Override
        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        @Override
        protected final int getAdditionalEntropyAmountForFinish() {
            if (isEncrypting()) {
                return getModulusSizeBytes();
            }
            return 0;
        }
    }

    static abstract class OAEPWithMGF1Padding extends AndroidKeyStoreRSACipherSpi {
        private static final String MGF_ALGORITGM_MGF1 = "MGF1";
        private int mDigestOutputSizeBytes;
        private int mKeymasterDigest;

        OAEPWithMGF1Padding(int i) {
            super(2);
            this.mKeymasterDigest = -1;
            this.mKeymasterDigest = i;
            this.mDigestOutputSizeBytes = (KeymasterUtils.getDigestOutputSizeBits(i) + 7) / 8;
        }

        @Override
        protected final void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        @Override
        protected final void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
            if (algorithmParameterSpec == null) {
                return;
            }
            if (!(algorithmParameterSpec instanceof OAEPParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Unsupported parameter spec: " + algorithmParameterSpec + ". Only OAEPParameterSpec supported");
            }
            OAEPParameterSpec oAEPParameterSpec = (OAEPParameterSpec) algorithmParameterSpec;
            if (!MGF_ALGORITGM_MGF1.equalsIgnoreCase(oAEPParameterSpec.getMGFAlgorithm())) {
                throw new InvalidAlgorithmParameterException("Unsupported MGF: " + oAEPParameterSpec.getMGFAlgorithm() + ". Only " + MGF_ALGORITGM_MGF1 + " supported");
            }
            String digestAlgorithm = oAEPParameterSpec.getDigestAlgorithm();
            try {
                int keymaster = KeyProperties.Digest.toKeymaster(digestAlgorithm);
                switch (keymaster) {
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        AlgorithmParameterSpec mGFParameters = oAEPParameterSpec.getMGFParameters();
                        if (mGFParameters == null) {
                            throw new InvalidAlgorithmParameterException("MGF parameters must be provided");
                        }
                        if (!(mGFParameters instanceof MGF1ParameterSpec)) {
                            throw new InvalidAlgorithmParameterException("Unsupported MGF parameters: " + mGFParameters + ". Only MGF1ParameterSpec supported");
                        }
                        String digestAlgorithm2 = ((MGF1ParameterSpec) mGFParameters).getDigestAlgorithm();
                        if (!KeyProperties.DIGEST_SHA1.equalsIgnoreCase(digestAlgorithm2)) {
                            throw new InvalidAlgorithmParameterException("Unsupported MGF1 digest: " + digestAlgorithm2 + ". Only " + KeyProperties.DIGEST_SHA1 + " supported");
                        }
                        PSource pSource = oAEPParameterSpec.getPSource();
                        if (!(pSource instanceof PSource.PSpecified)) {
                            throw new InvalidAlgorithmParameterException("Unsupported source of encoding input P: " + pSource + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                        }
                        byte[] value = ((PSource.PSpecified) pSource).getValue();
                        if (value != null && value.length > 0) {
                            throw new InvalidAlgorithmParameterException("Unsupported source of encoding input P: " + pSource + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                        }
                        this.mKeymasterDigest = keymaster;
                        this.mDigestOutputSizeBytes = (KeymasterUtils.getDigestOutputSizeBits(keymaster) + 7) / 8;
                        return;
                    default:
                        throw new InvalidAlgorithmParameterException("Unsupported digest: " + digestAlgorithm);
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidAlgorithmParameterException("Unsupported digest: " + digestAlgorithm, e);
            }
        }

        @Override
        protected final void initAlgorithmSpecificParameters(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
            if (algorithmParameters == null) {
                return;
            }
            try {
                OAEPParameterSpec oAEPParameterSpec = (OAEPParameterSpec) algorithmParameters.getParameterSpec(OAEPParameterSpec.class);
                if (oAEPParameterSpec == null) {
                    throw new InvalidAlgorithmParameterException("OAEP parameters required, but not provided in parameters: " + algorithmParameters);
                }
                initAlgorithmSpecificParameters(oAEPParameterSpec);
            } catch (InvalidParameterSpecException e) {
                throw new InvalidAlgorithmParameterException("OAEP parameters required, but not found in parameters: " + algorithmParameters, e);
            }
        }

        @Override
        protected final AlgorithmParameters engineGetParameters() {
            OAEPParameterSpec oAEPParameterSpec = new OAEPParameterSpec(KeyProperties.Digest.fromKeymaster(this.mKeymasterDigest), MGF_ALGORITGM_MGF1, MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            try {
                AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("OAEP");
                algorithmParameters.init(oAEPParameterSpec);
                return algorithmParameters;
            } catch (NoSuchAlgorithmException e) {
                throw new ProviderException("Failed to obtain OAEP AlgorithmParameters", e);
            } catch (InvalidParameterSpecException e2) {
                throw new ProviderException("Failed to initialize OAEP AlgorithmParameters with an IV", e2);
            }
        }

        @Override
        protected final void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments) {
            super.addAlgorithmSpecificParametersToBegin(keymasterArguments);
            keymasterArguments.addEnum(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigest);
        }

        @Override
        protected final void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArguments) {
            super.loadAlgorithmSpecificParametersFromBeginResult(keymasterArguments);
        }

        @Override
        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        @Override
        protected final int getAdditionalEntropyAmountForFinish() {
            if (isEncrypting()) {
                return this.mDigestOutputSizeBytes;
            }
            return 0;
        }
    }

    public static class OAEPWithSHA1AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA1AndMGF1Padding() {
            super(2);
        }
    }

    public static class OAEPWithSHA224AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA224AndMGF1Padding() {
            super(3);
        }
    }

    public static class OAEPWithSHA256AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA256AndMGF1Padding() {
            super(4);
        }
    }

    public static class OAEPWithSHA384AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA384AndMGF1Padding() {
            super(5);
        }
    }

    public static class OAEPWithSHA512AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA512AndMGF1Padding() {
            super(6);
        }
    }

    AndroidKeyStoreRSACipherSpi(int i) {
        this.mKeymasterPadding = i;
    }

    @Override
    protected final void initKey(int i, Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("Unsupported key: null");
        }
        if (!KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only " + KeyProperties.KEY_ALGORITHM_RSA + " supported");
        }
        if ((key instanceof AndroidKeyStorePrivateKey) || (key instanceof AndroidKeyStorePublicKey)) {
            AndroidKeyStoreKey androidKeyStoreKey = (AndroidKeyStoreKey) key;
            if (androidKeyStoreKey instanceof PrivateKey) {
                switch (i) {
                    case 1:
                    case 3:
                        if (!adjustConfigForEncryptingWithPrivateKey()) {
                            throw new InvalidKeyException("RSA private keys cannot be used with " + opmodeToString(i) + " and padding " + KeyProperties.EncryptionPadding.fromKeymaster(this.mKeymasterPadding) + ". Only RSA public keys supported for this mode");
                        }
                        break;
                    case 2:
                    case 4:
                        break;
                    default:
                        throw new InvalidKeyException("RSA private keys cannot be used with opmode: " + i);
                }
            } else {
                switch (i) {
                    case 1:
                    case 3:
                        break;
                    case 2:
                    case 4:
                        throw new InvalidKeyException("RSA public keys cannot be used with " + opmodeToString(i) + " and padding " + KeyProperties.EncryptionPadding.fromKeymaster(this.mKeymasterPadding) + ". Only RSA private keys supported for this opmode.");
                    default:
                        throw new InvalidKeyException("RSA public keys cannot be used with " + opmodeToString(i));
                }
            }
            KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
            int keyCharacteristics2 = getKeyStore().getKeyCharacteristics(androidKeyStoreKey.getAlias(), null, null, androidKeyStoreKey.getUid(), keyCharacteristics);
            if (keyCharacteristics2 != 1) {
                throw getKeyStore().getInvalidKeyException(androidKeyStoreKey.getAlias(), androidKeyStoreKey.getUid(), keyCharacteristics2);
            }
            long unsignedInt = keyCharacteristics.getUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, -1L);
            if (unsignedInt == -1) {
                throw new InvalidKeyException("Size of key not known");
            }
            if (unsignedInt > 2147483647L) {
                throw new InvalidKeyException("Key too large: " + unsignedInt + " bits");
            }
            this.mModulusSizeBytes = (int) ((unsignedInt + 7) / 8);
            setKey(androidKeyStoreKey);
            return;
        }
        throw new InvalidKeyException("Unsupported key type: " + key);
    }

    protected boolean adjustConfigForEncryptingWithPrivateKey() {
        return false;
    }

    @Override
    protected final void resetAll() {
        this.mModulusSizeBytes = -1;
        this.mKeymasterPaddingOverride = -1;
        super.resetAll();
    }

    @Override
    protected final void resetWhilePreservingInitState() {
        super.resetWhilePreservingInitState();
    }

    @Override
    protected void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments) {
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 1);
        int keymasterPaddingOverride = getKeymasterPaddingOverride();
        if (keymasterPaddingOverride == -1) {
            keymasterPaddingOverride = this.mKeymasterPadding;
        }
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_PADDING, keymasterPaddingOverride);
        int keymasterPurposeOverride = getKeymasterPurposeOverride();
        if (keymasterPurposeOverride != -1) {
            if (keymasterPurposeOverride == 2 || keymasterPurposeOverride == 3) {
                keymasterArguments.addEnum(KeymasterDefs.KM_TAG_DIGEST, 0);
            }
        }
    }

    @Override
    protected void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArguments) {
    }

    @Override
    protected final int engineGetBlockSize() {
        return 0;
    }

    @Override
    protected final byte[] engineGetIV() {
        return null;
    }

    @Override
    protected final int engineGetOutputSize(int i) {
        return getModulusSizeBytes();
    }

    protected final int getModulusSizeBytes() {
        if (this.mModulusSizeBytes == -1) {
            throw new IllegalStateException("Not initialized");
        }
        return this.mModulusSizeBytes;
    }

    protected final void setKeymasterPaddingOverride(int i) {
        this.mKeymasterPaddingOverride = i;
    }

    protected final int getKeymasterPaddingOverride() {
        return this.mKeymasterPaddingOverride;
    }
}
