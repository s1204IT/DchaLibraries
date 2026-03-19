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
        public void finalize() {
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
        protected void initAlgorithmSpecificParameters(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params == null) {
            } else {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
            }
        }

        @Override
        protected void initAlgorithmSpecificParameters(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
            if (params == null) {
            } else {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
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
        public void finalize() {
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
        protected void initAlgorithmSpecificParameters(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params == null) {
            } else {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
            }
        }

        @Override
        protected void initAlgorithmSpecificParameters(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
            if (params == null) {
            } else {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
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

        OAEPWithMGF1Padding(int keymasterDigest) {
            super(2);
            this.mKeymasterDigest = -1;
            this.mKeymasterDigest = keymasterDigest;
            this.mDigestOutputSizeBytes = (KeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
        }

        @Override
        protected final void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        @Override
        protected final void initAlgorithmSpecificParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
            if (algorithmParameterSpec == 0) {
                return;
            }
            if (!(algorithmParameterSpec instanceof OAEPParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Unsupported parameter spec: " + algorithmParameterSpec + ". Only OAEPParameterSpec supported");
            }
            if (!MGF_ALGORITGM_MGF1.equalsIgnoreCase(algorithmParameterSpec.getMGFAlgorithm())) {
                throw new InvalidAlgorithmParameterException("Unsupported MGF: " + algorithmParameterSpec.getMGFAlgorithm() + ". Only " + MGF_ALGORITGM_MGF1 + " supported");
            }
            String jcaDigest = algorithmParameterSpec.getDigestAlgorithm();
            try {
                int keymasterDigest = KeyProperties.Digest.toKeymaster(jcaDigest);
                switch (keymasterDigest) {
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        ?? mGFParameters = algorithmParameterSpec.getMGFParameters();
                        if (mGFParameters == 0) {
                            throw new InvalidAlgorithmParameterException("MGF parameters must be provided");
                        }
                        if (!(mGFParameters instanceof MGF1ParameterSpec)) {
                            throw new InvalidAlgorithmParameterException("Unsupported MGF parameters: " + ((Object) mGFParameters) + ". Only MGF1ParameterSpec supported");
                        }
                        String mgf1JcaDigest = mGFParameters.getDigestAlgorithm();
                        if (!KeyProperties.DIGEST_SHA1.equalsIgnoreCase(mgf1JcaDigest)) {
                            throw new InvalidAlgorithmParameterException("Unsupported MGF1 digest: " + mgf1JcaDigest + ". Only " + KeyProperties.DIGEST_SHA1 + " supported");
                        }
                        ?? pSource = algorithmParameterSpec.getPSource();
                        if (!(pSource instanceof PSource.PSpecified)) {
                            throw new InvalidAlgorithmParameterException("Unsupported source of encoding input P: " + ((Object) pSource) + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                        }
                        byte[] pSourceValue = pSource.getValue();
                        if (pSourceValue != null && pSourceValue.length > 0) {
                            throw new InvalidAlgorithmParameterException("Unsupported source of encoding input P: " + ((Object) pSource) + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                        }
                        this.mKeymasterDigest = keymasterDigest;
                        this.mDigestOutputSizeBytes = (KeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
                        return;
                    default:
                        throw new InvalidAlgorithmParameterException("Unsupported digest: " + jcaDigest);
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidAlgorithmParameterException("Unsupported digest: " + jcaDigest, e);
            }
        }

        @Override
        protected final void initAlgorithmSpecificParameters(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
            if (params == null) {
                return;
            }
            try {
                OAEPParameterSpec spec = (OAEPParameterSpec) params.getParameterSpec(OAEPParameterSpec.class);
                if (spec == null) {
                    throw new InvalidAlgorithmParameterException("OAEP parameters required, but not provided in parameters: " + params);
                }
                initAlgorithmSpecificParameters(spec);
            } catch (InvalidParameterSpecException e) {
                throw new InvalidAlgorithmParameterException("OAEP parameters required, but not found in parameters: " + params, e);
            }
        }

        @Override
        protected final AlgorithmParameters engineGetParameters() {
            OAEPParameterSpec spec = new OAEPParameterSpec(KeyProperties.Digest.fromKeymaster(this.mKeymasterDigest), MGF_ALGORITGM_MGF1, MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            try {
                AlgorithmParameters params = AlgorithmParameters.getInstance("OAEP");
                params.init(spec);
                return params;
            } catch (NoSuchAlgorithmException e) {
                throw new ProviderException("Failed to obtain OAEP AlgorithmParameters", e);
            } catch (InvalidParameterSpecException e2) {
                throw new ProviderException("Failed to initialize OAEP AlgorithmParameters with an IV", e2);
            }
        }

        @Override
        protected final void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArgs) {
            super.addAlgorithmSpecificParametersToBegin(keymasterArgs);
            keymasterArgs.addEnum(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigest);
        }

        @Override
        protected final void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArgs) {
            super.loadAlgorithmSpecificParametersFromBeginResult(keymasterArgs);
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
        public void finalize() {
            super.finalize();
        }

        public OAEPWithSHA1AndMGF1Padding() {
            super(2);
        }
    }

    public static class OAEPWithSHA224AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() {
            super.finalize();
        }

        public OAEPWithSHA224AndMGF1Padding() {
            super(3);
        }
    }

    public static class OAEPWithSHA256AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() {
            super.finalize();
        }

        public OAEPWithSHA256AndMGF1Padding() {
            super(4);
        }
    }

    public static class OAEPWithSHA384AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() {
            super.finalize();
        }

        public OAEPWithSHA384AndMGF1Padding() {
            super(5);
        }
    }

    public static class OAEPWithSHA512AndMGF1Padding extends OAEPWithMGF1Padding {
        @Override
        public void finalize() {
            super.finalize();
        }

        public OAEPWithSHA512AndMGF1Padding() {
            super(6);
        }
    }

    AndroidKeyStoreRSACipherSpi(int keymasterPadding) {
        this.mKeymasterPadding = keymasterPadding;
    }

    @Override
    protected final void initKey(int opmode, Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("Unsupported key: null");
        }
        if (!KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only " + KeyProperties.KEY_ALGORITHM_RSA + " supported");
        }
        if (!(key instanceof AndroidKeyStorePrivateKey) && !(key instanceof AndroidKeyStorePublicKey)) {
            throw new InvalidKeyException("Unsupported key type: " + key);
        }
        ?? r7 = key;
        if (r7 instanceof PrivateKey) {
            switch (opmode) {
                case 1:
                case 3:
                    if (!adjustConfigForEncryptingWithPrivateKey()) {
                        throw new InvalidKeyException("RSA private keys cannot be used with " + opmodeToString(opmode) + " and padding " + KeyProperties.EncryptionPadding.fromKeymaster(this.mKeymasterPadding) + ". Only RSA public keys supported for this mode");
                    }
                    break;
                case 2:
                case 4:
                    break;
                default:
                    throw new InvalidKeyException("RSA private keys cannot be used with opmode: " + opmode);
            }
        } else {
            switch (opmode) {
                case 1:
                case 3:
                    break;
                case 2:
                case 4:
                    throw new InvalidKeyException("RSA public keys cannot be used with " + opmodeToString(opmode) + " and padding " + KeyProperties.EncryptionPadding.fromKeymaster(this.mKeymasterPadding) + ". Only RSA private keys supported for this opmode.");
                default:
                    throw new InvalidKeyException("RSA public keys cannot be used with " + opmodeToString(opmode));
            }
        }
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        int errorCode = getKeyStore().getKeyCharacteristics(r7.getAlias(), null, null, r7.getUid(), keyCharacteristics);
        if (errorCode != 1) {
            throw getKeyStore().getInvalidKeyException(r7.getAlias(), r7.getUid(), errorCode);
        }
        long keySizeBits = keyCharacteristics.getUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, -1L);
        if (keySizeBits == -1) {
            throw new InvalidKeyException("Size of key not known");
        }
        if (keySizeBits > 2147483647L) {
            throw new InvalidKeyException("Key too large: " + keySizeBits + " bits");
        }
        this.mModulusSizeBytes = (int) ((7 + keySizeBits) / 8);
        setKey(r7);
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
    protected void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArgs) {
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 1);
        int keymasterPadding = getKeymasterPaddingOverride();
        if (keymasterPadding == -1) {
            keymasterPadding = this.mKeymasterPadding;
        }
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_PADDING, keymasterPadding);
        int purposeOverride = getKeymasterPurposeOverride();
        if (purposeOverride == -1) {
            return;
        }
        if (purposeOverride != 2 && purposeOverride != 3) {
            return;
        }
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_DIGEST, 0);
    }

    @Override
    protected void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArgs) {
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
    protected final int engineGetOutputSize(int inputLen) {
        return getModulusSizeBytes();
    }

    protected final int getModulusSizeBytes() {
        if (this.mModulusSizeBytes == -1) {
            throw new IllegalStateException("Not initialized");
        }
        return this.mModulusSizeBytes;
    }

    protected final void setKeymasterPaddingOverride(int keymasterPadding) {
        this.mKeymasterPaddingOverride = keymasterPadding;
    }

    protected final int getKeymasterPaddingOverride() {
        return this.mKeymasterPaddingOverride;
    }
}
