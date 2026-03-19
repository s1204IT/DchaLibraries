package android.security.keystore;

import android.security.Credentials;
import android.security.KeyStore;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import java.security.InvalidAlgorithmParameterException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import libcore.util.EmptyArray;

public abstract class AndroidKeyStoreKeyGeneratorSpi extends KeyGeneratorSpi {
    private final int mDefaultKeySizeBits;
    protected int mKeySizeBits;
    private final KeyStore mKeyStore;
    private final int mKeymasterAlgorithm;
    private int[] mKeymasterBlockModes;
    private final int mKeymasterDigest;
    private int[] mKeymasterDigests;
    private int[] mKeymasterPaddings;
    private int[] mKeymasterPurposes;
    private SecureRandom mRng;
    private KeyGenParameterSpec mSpec;

    public static class AES extends AndroidKeyStoreKeyGeneratorSpi {
        public AES() {
            super(32, 128);
        }

        @Override
        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            super.engineInit(algorithmParameterSpec, secureRandom);
            if (this.mKeySizeBits != 128 && this.mKeySizeBits != 192 && this.mKeySizeBits != 256) {
                throw new InvalidAlgorithmParameterException("Unsupported key size: " + this.mKeySizeBits + ". Supported: 128, 192, 256.");
            }
        }
    }

    public static class DESede extends AndroidKeyStoreKeyGeneratorSpi {
        public DESede() {
            super(33, 168);
        }
    }

    protected static abstract class HmacBase extends AndroidKeyStoreKeyGeneratorSpi {
        protected HmacBase(int i) {
            super(128, i, KeymasterUtils.getDigestOutputSizeBits(i));
        }
    }

    public static class HmacSHA1 extends HmacBase {
        public HmacSHA1() {
            super(2);
        }
    }

    public static class HmacSHA224 extends HmacBase {
        public HmacSHA224() {
            super(3);
        }
    }

    public static class HmacSHA256 extends HmacBase {
        public HmacSHA256() {
            super(4);
        }
    }

    public static class HmacSHA384 extends HmacBase {
        public HmacSHA384() {
            super(5);
        }
    }

    public static class HmacSHA512 extends HmacBase {
        public HmacSHA512() {
            super(6);
        }
    }

    protected AndroidKeyStoreKeyGeneratorSpi(int i, int i2) {
        this(i, -1, i2);
    }

    protected AndroidKeyStoreKeyGeneratorSpi(int i, int i2, int i3) {
        this.mKeyStore = KeyStore.getInstance();
        this.mKeymasterAlgorithm = i;
        this.mKeymasterDigest = i2;
        this.mDefaultKeySizeBits = i3;
        if (this.mDefaultKeySizeBits <= 0) {
            throw new IllegalArgumentException("Default key size must be positive");
        }
        if (this.mKeymasterAlgorithm == 128 && this.mKeymasterDigest == -1) {
            throw new IllegalArgumentException("Digest algorithm must be specified for HMAC key");
        }
    }

    @Override
    protected void engineInit(SecureRandom secureRandom) {
        throw new UnsupportedOperationException("Cannot initialize without a " + KeyGenParameterSpec.class.getName() + " parameter");
    }

    @Override
    protected void engineInit(int i, SecureRandom secureRandom) {
        throw new UnsupportedOperationException("Cannot initialize without a " + KeyGenParameterSpec.class.getName() + " parameter");
    }

    @Override
    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        resetAll();
        if (algorithmParameterSpec != null) {
            try {
                if (algorithmParameterSpec instanceof KeyGenParameterSpec) {
                    KeyGenParameterSpec keyGenParameterSpec = (KeyGenParameterSpec) algorithmParameterSpec;
                    if (keyGenParameterSpec.getKeystoreAlias() == null) {
                        throw new InvalidAlgorithmParameterException("KeyStore entry alias not provided");
                    }
                    this.mRng = secureRandom;
                    this.mSpec = keyGenParameterSpec;
                    this.mKeySizeBits = keyGenParameterSpec.getKeySize() != -1 ? keyGenParameterSpec.getKeySize() : this.mDefaultKeySizeBits;
                    if (this.mKeySizeBits <= 0) {
                        throw new InvalidAlgorithmParameterException("Key size must be positive: " + this.mKeySizeBits);
                    }
                    if (this.mKeySizeBits % 8 != 0) {
                        throw new InvalidAlgorithmParameterException("Key size must be a multiple of 8: " + this.mKeySizeBits);
                    }
                    try {
                        this.mKeymasterPurposes = KeyProperties.Purpose.allToKeymaster(keyGenParameterSpec.getPurposes());
                        this.mKeymasterPaddings = KeyProperties.EncryptionPadding.allToKeymaster(keyGenParameterSpec.getEncryptionPaddings());
                        if (keyGenParameterSpec.getSignaturePaddings().length > 0) {
                            throw new InvalidAlgorithmParameterException("Signature paddings not supported for symmetric key algorithms");
                        }
                        this.mKeymasterBlockModes = KeyProperties.BlockMode.allToKeymaster(keyGenParameterSpec.getBlockModes());
                        if ((keyGenParameterSpec.getPurposes() & 1) != 0 && keyGenParameterSpec.isRandomizedEncryptionRequired()) {
                            for (int i : this.mKeymasterBlockModes) {
                                if (!KeymasterUtils.isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(i)) {
                                    throw new InvalidAlgorithmParameterException("Randomized encryption (IND-CPA) required but may be violated by block mode: " + KeyProperties.BlockMode.fromKeymaster(i) + ". See " + KeyGenParameterSpec.class.getName() + " documentation.");
                                }
                            }
                        }
                        if (this.mKeymasterAlgorithm == 128) {
                            if (this.mKeySizeBits < 64) {
                                throw new InvalidAlgorithmParameterException("HMAC key size must be at least 64 bits.");
                            }
                            this.mKeymasterDigests = new int[]{this.mKeymasterDigest};
                            if (keyGenParameterSpec.isDigestsSpecified()) {
                                int[] iArrAllToKeymaster = KeyProperties.Digest.allToKeymaster(keyGenParameterSpec.getDigests());
                                if (iArrAllToKeymaster.length != 1 || iArrAllToKeymaster[0] != this.mKeymasterDigest) {
                                    throw new InvalidAlgorithmParameterException("Unsupported digests specification: " + Arrays.asList(keyGenParameterSpec.getDigests()) + ". Only " + KeyProperties.Digest.fromKeymaster(this.mKeymasterDigest) + " supported for this HMAC key algorithm");
                                }
                            }
                        } else if (keyGenParameterSpec.isDigestsSpecified()) {
                            this.mKeymasterDigests = KeyProperties.Digest.allToKeymaster(keyGenParameterSpec.getDigests());
                        } else {
                            this.mKeymasterDigests = EmptyArray.INT;
                        }
                        KeymasterUtils.addUserAuthArgs(new KeymasterArguments(), keyGenParameterSpec);
                        return;
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        throw new InvalidAlgorithmParameterException(e);
                    }
                }
            } catch (Throwable th) {
                resetAll();
                throw th;
            }
        }
        throw new InvalidAlgorithmParameterException("Cannot initialize without a " + KeyGenParameterSpec.class.getName() + " parameter");
    }

    private void resetAll() {
        this.mSpec = null;
        this.mRng = null;
        this.mKeySizeBits = -1;
        this.mKeymasterPurposes = null;
        this.mKeymasterPaddings = null;
        this.mKeymasterBlockModes = null;
    }

    @Override
    protected SecretKey engineGenerateKey() {
        KeyGenParameterSpec keyGenParameterSpec = this.mSpec;
        if (keyGenParameterSpec == null) {
            throw new IllegalStateException("Not initialized");
        }
        KeymasterArguments keymasterArguments = new KeymasterArguments();
        keymasterArguments.addUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, this.mKeySizeBits);
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, this.mKeymasterAlgorithm);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PURPOSE, this.mKeymasterPurposes);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockModes);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterPaddings);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigests);
        KeymasterUtils.addUserAuthArgs(keymasterArguments, keyGenParameterSpec);
        KeymasterUtils.addMinMacLengthAuthorizationIfNecessary(keymasterArguments, this.mKeymasterAlgorithm, this.mKeymasterBlockModes, this.mKeymasterDigests);
        keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, keyGenParameterSpec.getKeyValidityStart());
        keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, keyGenParameterSpec.getKeyValidityForOriginationEnd());
        keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, keyGenParameterSpec.getKeyValidityForConsumptionEnd());
        if ((keyGenParameterSpec.getPurposes() & 1) != 0 && !keyGenParameterSpec.isRandomizedEncryptionRequired()) {
            keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_CALLER_NONCE);
        }
        byte[] randomBytesToMixIntoKeystoreRng = KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, (this.mKeySizeBits + 7) / 8);
        int i = 0;
        if (keyGenParameterSpec.isStrongBoxBacked()) {
            i = 16;
        }
        int i2 = i;
        String str = Credentials.USER_PRIVATE_KEY + keyGenParameterSpec.getKeystoreAlias();
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        try {
            Credentials.deleteAllTypesForAlias(this.mKeyStore, keyGenParameterSpec.getKeystoreAlias(), keyGenParameterSpec.getUid());
            int iGenerateKey = this.mKeyStore.generateKey(str, keymasterArguments, randomBytesToMixIntoKeystoreRng, keyGenParameterSpec.getUid(), i2, keyCharacteristics);
            if (iGenerateKey != 1) {
                if (iGenerateKey == -68) {
                    throw new StrongBoxUnavailableException("Failed to generate key");
                }
                throw new ProviderException("Keystore operation failed", KeyStore.getKeyStoreException(iGenerateKey));
            }
            try {
                return new AndroidKeyStoreSecretKey(str, keyGenParameterSpec.getUid(), KeyProperties.KeyAlgorithm.fromKeymasterSecretKeyAlgorithm(this.mKeymasterAlgorithm, this.mKeymasterDigest));
            } catch (IllegalArgumentException e) {
                throw new ProviderException("Failed to obtain JCA secret key algorithm name", e);
            }
        } catch (Throwable th) {
            Credentials.deleteAllTypesForAlias(this.mKeyStore, keyGenParameterSpec.getKeystoreAlias(), keyGenParameterSpec.getUid());
            throw th;
        }
    }
}
