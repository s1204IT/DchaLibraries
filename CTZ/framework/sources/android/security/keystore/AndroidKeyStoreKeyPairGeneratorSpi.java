package android.security.keystore;

import android.security.Credentials;
import android.security.KeyPairGeneratorSpec;
import android.security.KeyStore;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import com.android.internal.logging.nano.MetricsProto;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DERInteger;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.Certificate;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x509.TBSCertificate;
import com.android.org.bouncycastle.asn1.x509.Time;
import com.android.org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.org.bouncycastle.jce.X509Principal;
import com.android.org.bouncycastle.jce.provider.X509CertificateObject;
import com.android.org.bouncycastle.x509.X509V3CertificateGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import libcore.util.EmptyArray;

public abstract class AndroidKeyStoreKeyPairGeneratorSpi extends KeyPairGeneratorSpi {
    private static final int EC_DEFAULT_KEY_SIZE = 256;
    private static final int RSA_DEFAULT_KEY_SIZE = 2048;
    private static final int RSA_MAX_KEY_SIZE = 8192;
    private static final int RSA_MIN_KEY_SIZE = 512;
    private boolean mEncryptionAtRestRequired;
    private String mEntryAlias;
    private int mEntryUid;
    private String mJcaKeyAlgorithm;
    private int mKeySizeBits;
    private KeyStore mKeyStore;
    private int mKeymasterAlgorithm = -1;
    private int[] mKeymasterBlockModes;
    private int[] mKeymasterDigests;
    private int[] mKeymasterEncryptionPaddings;
    private int[] mKeymasterPurposes;
    private int[] mKeymasterSignaturePaddings;
    private final int mOriginalKeymasterAlgorithm;
    private BigInteger mRSAPublicExponent;
    private SecureRandom mRng;
    private KeyGenParameterSpec mSpec;
    private static final Map<String, Integer> SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE = new HashMap();
    private static final List<String> SUPPORTED_EC_NIST_CURVE_NAMES = new ArrayList();
    private static final List<Integer> SUPPORTED_EC_NIST_CURVE_SIZES = new ArrayList();

    public static class RSA extends AndroidKeyStoreKeyPairGeneratorSpi {
        public RSA() {
            super(1);
        }
    }

    public static class EC extends AndroidKeyStoreKeyPairGeneratorSpi {
        public EC() {
            super(3);
        }
    }

    static {
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-224", 224);
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp224r1", 224);
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-256", 256);
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp256r1", 256);
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("prime256v1", 256);
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-384", Integer.valueOf(MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp384r1", Integer.valueOf(MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-521", 521);
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp521r1", 521);
        SUPPORTED_EC_NIST_CURVE_NAMES.addAll(SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.keySet());
        Collections.sort(SUPPORTED_EC_NIST_CURVE_NAMES);
        SUPPORTED_EC_NIST_CURVE_SIZES.addAll(new HashSet(SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.values()));
        Collections.sort(SUPPORTED_EC_NIST_CURVE_SIZES);
    }

    protected AndroidKeyStoreKeyPairGeneratorSpi(int i) {
        this.mOriginalKeymasterAlgorithm = i;
    }

    @Override
    public void initialize(int i, SecureRandom secureRandom) {
        throw new IllegalArgumentException(KeyGenParameterSpec.class.getName() + " or " + KeyPairGeneratorSpec.class.getName() + " required to initialize this KeyPairGenerator");
    }

    @Override
    public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        KeyGenParameterSpec.Builder builder;
        boolean z;
        KeyGenParameterSpec keyGenParameterSpec;
        try {
            if (algorithmParameterSpec == null) {
                throw new InvalidAlgorithmParameterException("Must supply params of type " + KeyGenParameterSpec.class.getName() + " or " + KeyPairGeneratorSpec.class.getName());
            }
            int keymasterAsymmetricKeyAlgorithm = this.mOriginalKeymasterAlgorithm;
            if (algorithmParameterSpec instanceof KeyGenParameterSpec) {
                keyGenParameterSpec = (KeyGenParameterSpec) algorithmParameterSpec;
                z = false;
            } else if (algorithmParameterSpec instanceof KeyPairGeneratorSpec) {
                KeyPairGeneratorSpec keyPairGeneratorSpec = (KeyPairGeneratorSpec) algorithmParameterSpec;
                try {
                    String keyType = keyPairGeneratorSpec.getKeyType();
                    if (keyType != null) {
                        try {
                            keymasterAsymmetricKeyAlgorithm = KeyProperties.KeyAlgorithm.toKeymasterAsymmetricKeyAlgorithm(keyType);
                        } catch (IllegalArgumentException e) {
                            throw new InvalidAlgorithmParameterException("Invalid key type in parameters", e);
                        }
                    }
                    if (keymasterAsymmetricKeyAlgorithm == 1) {
                        builder = new KeyGenParameterSpec.Builder(keyPairGeneratorSpec.getKeystoreAlias(), 15);
                        builder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
                        builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);
                        builder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS);
                        builder.setRandomizedEncryptionRequired(false);
                    } else if (keymasterAsymmetricKeyAlgorithm == 3) {
                        builder = new KeyGenParameterSpec.Builder(keyPairGeneratorSpec.getKeystoreAlias(), 12);
                        builder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
                    } else {
                        throw new ProviderException("Unsupported algorithm: " + this.mKeymasterAlgorithm);
                    }
                    if (keyPairGeneratorSpec.getKeySize() != -1) {
                        builder.setKeySize(keyPairGeneratorSpec.getKeySize());
                    }
                    if (keyPairGeneratorSpec.getAlgorithmParameterSpec() != null) {
                        builder.setAlgorithmParameterSpec(keyPairGeneratorSpec.getAlgorithmParameterSpec());
                    }
                    builder.setCertificateSubject(keyPairGeneratorSpec.getSubjectDN());
                    builder.setCertificateSerialNumber(keyPairGeneratorSpec.getSerialNumber());
                    builder.setCertificateNotBefore(keyPairGeneratorSpec.getStartDate());
                    builder.setCertificateNotAfter(keyPairGeneratorSpec.getEndDate());
                    boolean zIsEncryptionRequired = keyPairGeneratorSpec.isEncryptionRequired();
                    builder.setUserAuthenticationRequired(false);
                    KeyGenParameterSpec keyGenParameterSpecBuild = builder.build();
                    z = zIsEncryptionRequired;
                    keyGenParameterSpec = keyGenParameterSpecBuild;
                } catch (IllegalArgumentException | NullPointerException e2) {
                    throw new InvalidAlgorithmParameterException(e2);
                }
            } else {
                throw new InvalidAlgorithmParameterException("Unsupported params class: " + algorithmParameterSpec.getClass().getName() + ". Supported: " + KeyGenParameterSpec.class.getName() + ", " + KeyPairGeneratorSpec.class.getName());
            }
            this.mEntryAlias = keyGenParameterSpec.getKeystoreAlias();
            this.mEntryUid = keyGenParameterSpec.getUid();
            this.mSpec = keyGenParameterSpec;
            this.mKeymasterAlgorithm = keymasterAsymmetricKeyAlgorithm;
            this.mEncryptionAtRestRequired = z;
            this.mKeySizeBits = keyGenParameterSpec.getKeySize();
            initAlgorithmSpecificParameters();
            if (this.mKeySizeBits == -1) {
                this.mKeySizeBits = getDefaultKeySize(keymasterAsymmetricKeyAlgorithm);
            }
            checkValidKeySize(keymasterAsymmetricKeyAlgorithm, this.mKeySizeBits);
            if (keyGenParameterSpec.getKeystoreAlias() == null) {
                throw new InvalidAlgorithmParameterException("KeyStore entry alias not provided");
            }
            try {
                String strFromKeymasterAsymmetricKeyAlgorithm = KeyProperties.KeyAlgorithm.fromKeymasterAsymmetricKeyAlgorithm(keymasterAsymmetricKeyAlgorithm);
                this.mKeymasterPurposes = KeyProperties.Purpose.allToKeymaster(keyGenParameterSpec.getPurposes());
                this.mKeymasterBlockModes = KeyProperties.BlockMode.allToKeymaster(keyGenParameterSpec.getBlockModes());
                this.mKeymasterEncryptionPaddings = KeyProperties.EncryptionPadding.allToKeymaster(keyGenParameterSpec.getEncryptionPaddings());
                if ((keyGenParameterSpec.getPurposes() & 1) != 0 && keyGenParameterSpec.isRandomizedEncryptionRequired()) {
                    for (int i : this.mKeymasterEncryptionPaddings) {
                        if (!KeymasterUtils.isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(i)) {
                            throw new InvalidAlgorithmParameterException("Randomized encryption (IND-CPA) required but may be violated by padding scheme: " + KeyProperties.EncryptionPadding.fromKeymaster(i) + ". See " + KeyGenParameterSpec.class.getName() + " documentation.");
                        }
                    }
                }
                this.mKeymasterSignaturePaddings = KeyProperties.SignaturePadding.allToKeymaster(keyGenParameterSpec.getSignaturePaddings());
                if (keyGenParameterSpec.isDigestsSpecified()) {
                    this.mKeymasterDigests = KeyProperties.Digest.allToKeymaster(keyGenParameterSpec.getDigests());
                } else {
                    this.mKeymasterDigests = EmptyArray.INT;
                }
                KeymasterUtils.addUserAuthArgs(new KeymasterArguments(), this.mSpec);
                this.mJcaKeyAlgorithm = strFromKeymasterAsymmetricKeyAlgorithm;
                this.mRng = secureRandom;
                this.mKeyStore = KeyStore.getInstance();
            } catch (IllegalArgumentException | IllegalStateException e3) {
                throw new InvalidAlgorithmParameterException(e3);
            }
        } finally {
            resetAll();
        }
    }

    private void resetAll() {
        this.mEntryAlias = null;
        this.mEntryUid = -1;
        this.mJcaKeyAlgorithm = null;
        this.mKeymasterAlgorithm = -1;
        this.mKeymasterPurposes = null;
        this.mKeymasterBlockModes = null;
        this.mKeymasterEncryptionPaddings = null;
        this.mKeymasterSignaturePaddings = null;
        this.mKeymasterDigests = null;
        this.mKeySizeBits = 0;
        this.mSpec = null;
        this.mRSAPublicExponent = null;
        this.mEncryptionAtRestRequired = false;
        this.mRng = null;
        this.mKeyStore = null;
    }

    private void initAlgorithmSpecificParameters() throws InvalidAlgorithmParameterException {
        AlgorithmParameterSpec algorithmParameterSpec = this.mSpec.getAlgorithmParameterSpec();
        int i = this.mKeymasterAlgorithm;
        if (i != 1) {
            if (i == 3) {
                if (algorithmParameterSpec instanceof ECGenParameterSpec) {
                    String name = ((ECGenParameterSpec) algorithmParameterSpec).getName();
                    Integer num = SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.get(name.toLowerCase(Locale.US));
                    if (num == null) {
                        throw new InvalidAlgorithmParameterException("Unsupported EC curve name: " + name + ". Supported: " + SUPPORTED_EC_NIST_CURVE_NAMES);
                    }
                    if (this.mKeySizeBits == -1) {
                        this.mKeySizeBits = num.intValue();
                        return;
                    }
                    if (this.mKeySizeBits != num.intValue()) {
                        throw new InvalidAlgorithmParameterException("EC key size must match  between " + this.mSpec + " and " + algorithmParameterSpec + ": " + this.mKeySizeBits + " vs " + num);
                    }
                    return;
                }
                if (algorithmParameterSpec != null) {
                    throw new InvalidAlgorithmParameterException("EC may only use ECGenParameterSpec");
                }
                return;
            }
            throw new ProviderException("Unsupported algorithm: " + this.mKeymasterAlgorithm);
        }
        BigInteger publicExponent = null;
        if (algorithmParameterSpec instanceof RSAKeyGenParameterSpec) {
            RSAKeyGenParameterSpec rSAKeyGenParameterSpec = (RSAKeyGenParameterSpec) algorithmParameterSpec;
            if (this.mKeySizeBits == -1) {
                this.mKeySizeBits = rSAKeyGenParameterSpec.getKeysize();
            } else if (this.mKeySizeBits != rSAKeyGenParameterSpec.getKeysize()) {
                throw new InvalidAlgorithmParameterException("RSA key size must match  between " + this.mSpec + " and " + algorithmParameterSpec + ": " + this.mKeySizeBits + " vs " + rSAKeyGenParameterSpec.getKeysize());
            }
            publicExponent = rSAKeyGenParameterSpec.getPublicExponent();
        } else if (algorithmParameterSpec != null) {
            throw new InvalidAlgorithmParameterException("RSA may only use RSAKeyGenParameterSpec");
        }
        if (publicExponent == null) {
            publicExponent = RSAKeyGenParameterSpec.F4;
        }
        if (publicExponent.compareTo(BigInteger.ZERO) < 1) {
            throw new InvalidAlgorithmParameterException("RSA public exponent must be positive: " + publicExponent);
        }
        if (publicExponent.compareTo(KeymasterArguments.UINT64_MAX_VALUE) > 0) {
            throw new InvalidAlgorithmParameterException("Unsupported RSA public exponent: " + publicExponent + ". Maximum supported value: " + KeymasterArguments.UINT64_MAX_VALUE);
        }
        this.mRSAPublicExponent = publicExponent;
    }

    @Override
    public KeyPair generateKeyPair() {
        if (this.mKeyStore == null || this.mSpec == null) {
            throw new IllegalStateException("Not initialized");
        }
        boolean z = this.mEncryptionAtRestRequired;
        if (((z ? 1 : 0) & 1) != 0 && this.mKeyStore.state() != KeyStore.State.UNLOCKED) {
            throw new IllegalStateException("Encryption at rest using secure lock screen credential requested for key pair, but the user has not yet entered the credential");
        }
        int i = z;
        if (this.mSpec.isStrongBoxBacked()) {
            i = (z ? 1 : 0) | 16;
        }
        byte[] randomBytesToMixIntoKeystoreRng = KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, (this.mKeySizeBits + 7) / 8);
        Credentials.deleteAllTypesForAlias(this.mKeyStore, this.mEntryAlias, this.mEntryUid);
        String str = Credentials.USER_PRIVATE_KEY + this.mEntryAlias;
        try {
            try {
                generateKeystoreKeyPair(str, constructKeyGenerationArguments(), randomBytesToMixIntoKeystoreRng, i);
                KeyPair keyPairLoadKeystoreKeyPair = loadKeystoreKeyPair(str);
                storeCertificateChain(i, createCertificateChain(str, keyPairLoadKeystoreKeyPair));
                return keyPairLoadKeystoreKeyPair;
            } catch (ProviderException e) {
                if ((this.mSpec.getPurposes() & 32) != 0) {
                    throw new SecureKeyImportUnavailableException(e);
                }
                throw e;
            }
        } catch (Throwable th) {
            Credentials.deleteAllTypesForAlias(this.mKeyStore, this.mEntryAlias, this.mEntryUid);
            throw th;
        }
    }

    private Iterable<byte[]> createCertificateChain(String str, KeyPair keyPair) throws ProviderException {
        byte[] attestationChallenge = this.mSpec.getAttestationChallenge();
        if (attestationChallenge != null) {
            KeymasterArguments keymasterArguments = new KeymasterArguments();
            keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_CHALLENGE, attestationChallenge);
            return getAttestationChain(str, keyPair, keymasterArguments);
        }
        return Collections.singleton(generateSelfSignedCertificateBytes(keyPair));
    }

    private void generateKeystoreKeyPair(String str, KeymasterArguments keymasterArguments, byte[] bArr, int i) throws ProviderException {
        int iGenerateKey = this.mKeyStore.generateKey(str, keymasterArguments, bArr, this.mEntryUid, i, new KeyCharacteristics());
        if (iGenerateKey != 1) {
            if (iGenerateKey == -68) {
                throw new StrongBoxUnavailableException("Failed to generate key pair");
            }
            throw new ProviderException("Failed to generate key pair", KeyStore.getKeyStoreException(iGenerateKey));
        }
    }

    private KeyPair loadKeystoreKeyPair(String str) throws ProviderException {
        try {
            KeyPair keyPairLoadAndroidKeyStoreKeyPairFromKeystore = AndroidKeyStoreProvider.loadAndroidKeyStoreKeyPairFromKeystore(this.mKeyStore, str, this.mEntryUid);
            if (!this.mJcaKeyAlgorithm.equalsIgnoreCase(keyPairLoadAndroidKeyStoreKeyPairFromKeystore.getPrivate().getAlgorithm())) {
                throw new ProviderException("Generated key pair algorithm does not match requested algorithm: " + keyPairLoadAndroidKeyStoreKeyPairFromKeystore.getPrivate().getAlgorithm() + " vs " + this.mJcaKeyAlgorithm);
            }
            return keyPairLoadAndroidKeyStoreKeyPairFromKeystore;
        } catch (UnrecoverableKeyException e) {
            throw new ProviderException("Failed to load generated key pair from keystore", e);
        }
    }

    private KeymasterArguments constructKeyGenerationArguments() {
        KeymasterArguments keymasterArguments = new KeymasterArguments();
        keymasterArguments.addUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, this.mKeySizeBits);
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, this.mKeymasterAlgorithm);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PURPOSE, this.mKeymasterPurposes);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockModes);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterEncryptionPaddings);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterSignaturePaddings);
        keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigests);
        KeymasterUtils.addUserAuthArgs(keymasterArguments, this.mSpec);
        keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, this.mSpec.getKeyValidityStart());
        keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, this.mSpec.getKeyValidityForOriginationEnd());
        keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, this.mSpec.getKeyValidityForConsumptionEnd());
        addAlgorithmSpecificParameters(keymasterArguments);
        if (this.mSpec.isUniqueIdIncluded()) {
            keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_INCLUDE_UNIQUE_ID);
        }
        return keymasterArguments;
    }

    private void storeCertificateChain(int i, Iterable<byte[]> iterable) throws ProviderException {
        Iterator<byte[]> it = iterable.iterator();
        storeCertificate(Credentials.USER_CERTIFICATE, it.next(), i, "Failed to store certificate");
        if (!it.hasNext()) {
            return;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (it.hasNext()) {
            byte[] next = it.next();
            byteArrayOutputStream.write(next, 0, next.length);
        }
        storeCertificate(Credentials.CA_CERTIFICATE, byteArrayOutputStream.toByteArray(), i, "Failed to store attestation CA certificate");
    }

    private void storeCertificate(String str, byte[] bArr, int i, String str2) throws ProviderException {
        int iInsert = this.mKeyStore.insert(str + this.mEntryAlias, bArr, this.mEntryUid, i);
        if (iInsert != 1) {
            throw new ProviderException(str2, KeyStore.getKeyStoreException(iInsert));
        }
    }

    private byte[] generateSelfSignedCertificateBytes(KeyPair keyPair) throws ProviderException {
        try {
            return generateSelfSignedCertificate(keyPair.getPrivate(), keyPair.getPublic()).getEncoded();
        } catch (IOException | CertificateParsingException e) {
            throw new ProviderException("Failed to generate self-signed certificate", e);
        } catch (CertificateEncodingException e2) {
            throw new ProviderException("Failed to obtain encoded form of self-signed certificate", e2);
        }
    }

    private Iterable<byte[]> getAttestationChain(String str, KeyPair keyPair, KeymasterArguments keymasterArguments) throws ProviderException {
        KeymasterCertificateChain keymasterCertificateChain = new KeymasterCertificateChain();
        int iAttestKey = this.mKeyStore.attestKey(str, keymasterArguments, keymasterCertificateChain);
        if (iAttestKey != 1) {
            throw new ProviderException("Failed to generate attestation certificate chain", KeyStore.getKeyStoreException(iAttestKey));
        }
        List<byte[]> certificates = keymasterCertificateChain.getCertificates();
        if (certificates.size() < 2) {
            throw new ProviderException("Attestation certificate chain contained " + certificates.size() + " entries. At least two are required.");
        }
        return certificates;
    }

    private void addAlgorithmSpecificParameters(KeymasterArguments keymasterArguments) {
        int i = this.mKeymasterAlgorithm;
        if (i == 1) {
            keymasterArguments.addUnsignedLong(KeymasterDefs.KM_TAG_RSA_PUBLIC_EXPONENT, this.mRSAPublicExponent);
        } else if (i != 3) {
            throw new ProviderException("Unsupported algorithm: " + this.mKeymasterAlgorithm);
        }
    }

    private X509Certificate generateSelfSignedCertificate(PrivateKey privateKey, PublicKey publicKey) throws CertificateParsingException, IOException {
        String certificateSignatureAlgorithm = getCertificateSignatureAlgorithm(this.mKeymasterAlgorithm, this.mKeySizeBits, this.mSpec);
        if (certificateSignatureAlgorithm == null) {
            return generateSelfSignedCertificateWithFakeSignature(publicKey);
        }
        try {
            return generateSelfSignedCertificateWithValidSignature(privateKey, publicKey, certificateSignatureAlgorithm);
        } catch (Exception e) {
            return generateSelfSignedCertificateWithFakeSignature(publicKey);
        }
    }

    private X509Certificate generateSelfSignedCertificateWithValidSignature(PrivateKey privateKey, PublicKey publicKey, String str) throws Exception {
        X509V3CertificateGenerator x509V3CertificateGenerator = new X509V3CertificateGenerator();
        x509V3CertificateGenerator.setPublicKey(publicKey);
        x509V3CertificateGenerator.setSerialNumber(this.mSpec.getCertificateSerialNumber());
        x509V3CertificateGenerator.setSubjectDN(this.mSpec.getCertificateSubject());
        x509V3CertificateGenerator.setIssuerDN(this.mSpec.getCertificateSubject());
        x509V3CertificateGenerator.setNotBefore(this.mSpec.getCertificateNotBefore());
        x509V3CertificateGenerator.setNotAfter(this.mSpec.getCertificateNotAfter());
        x509V3CertificateGenerator.setSignatureAlgorithm(str);
        return x509V3CertificateGenerator.generate(privateKey);
    }

    private X509Certificate generateSelfSignedCertificateWithFakeSignature(PublicKey publicKey) throws CertificateParsingException, IOException {
        byte[] encoded;
        AlgorithmIdentifier algorithmIdentifier;
        V3TBSCertificateGenerator v3TBSCertificateGenerator = new V3TBSCertificateGenerator();
        int i = this.mKeymasterAlgorithm;
        if (i == 1) {
            encoded = new byte[1];
            algorithmIdentifier = new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption, DERNull.INSTANCE);
        } else if (i == 3) {
            algorithmIdentifier = new AlgorithmIdentifier(X9ObjectIdentifiers.ecdsa_with_SHA256);
            ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
            aSN1EncodableVector.add(new DERInteger(0L));
            aSN1EncodableVector.add(new DERInteger(0L));
            encoded = new DERSequence().getEncoded();
        } else {
            throw new ProviderException("Unsupported key algorithm: " + this.mKeymasterAlgorithm);
        }
        ASN1InputStream aSN1InputStream = new ASN1InputStream(publicKey.getEncoded());
        Throwable th = null;
        try {
            v3TBSCertificateGenerator.setSubjectPublicKeyInfo(SubjectPublicKeyInfo.getInstance(aSN1InputStream.readObject()));
            aSN1InputStream.close();
            v3TBSCertificateGenerator.setSerialNumber(new ASN1Integer(this.mSpec.getCertificateSerialNumber()));
            X509Principal x509Principal = new X509Principal(this.mSpec.getCertificateSubject().getEncoded());
            v3TBSCertificateGenerator.setSubject(x509Principal);
            v3TBSCertificateGenerator.setIssuer(x509Principal);
            v3TBSCertificateGenerator.setStartDate(new Time(this.mSpec.getCertificateNotBefore()));
            v3TBSCertificateGenerator.setEndDate(new Time(this.mSpec.getCertificateNotAfter()));
            v3TBSCertificateGenerator.setSignature(algorithmIdentifier);
            TBSCertificate tBSCertificateGenerateTBSCertificate = v3TBSCertificateGenerator.generateTBSCertificate();
            ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
            aSN1EncodableVector2.add(tBSCertificateGenerateTBSCertificate);
            aSN1EncodableVector2.add(algorithmIdentifier);
            aSN1EncodableVector2.add(new DERBitString(encoded));
            return new X509CertificateObject(Certificate.getInstance(new DERSequence(aSN1EncodableVector2)));
        } catch (Throwable th2) {
            if (th != null) {
                try {
                    aSN1InputStream.close();
                } catch (Throwable th3) {
                    th.addSuppressed(th3);
                }
            } else {
                aSN1InputStream.close();
            }
            throw th2;
        }
    }

    private static int getDefaultKeySize(int i) {
        if (i == 1) {
            return 2048;
        }
        if (i == 3) {
            return 256;
        }
        throw new ProviderException("Unsupported algorithm: " + i);
    }

    private static void checkValidKeySize(int i, int i2) throws InvalidAlgorithmParameterException {
        if (i == 1) {
            if (i2 < 512 || i2 > 8192) {
                throw new InvalidAlgorithmParameterException("RSA key size must be >= 512 and <= 8192");
            }
        } else {
            if (i == 3) {
                if (!SUPPORTED_EC_NIST_CURVE_SIZES.contains(Integer.valueOf(i2))) {
                    throw new InvalidAlgorithmParameterException("Unsupported EC key size: " + i2 + " bits. Supported: " + SUPPORTED_EC_NIST_CURVE_SIZES);
                }
                return;
            }
            throw new ProviderException("Unsupported algorithm: " + i);
        }
    }

    private static String getCertificateSignatureAlgorithm(int i, int i2, KeyGenParameterSpec keyGenParameterSpec) {
        if ((keyGenParameterSpec.getPurposes() & 4) == 0 || keyGenParameterSpec.isUserAuthenticationRequired() || !keyGenParameterSpec.isDigestsSpecified()) {
            return null;
        }
        if (i == 1) {
            if (!com.android.internal.util.ArrayUtils.contains(KeyProperties.SignaturePadding.allToKeymaster(keyGenParameterSpec.getSignaturePaddings()), 5)) {
                return null;
            }
            int i3 = i2 - 240;
            Iterator<Integer> it = getAvailableKeymasterSignatureDigests(keyGenParameterSpec.getDigests(), AndroidKeyStoreBCWorkaroundProvider.getSupportedEcdsaSignatureDigests()).iterator();
            int i4 = -1;
            int i5 = -1;
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                int digestOutputSizeBits = KeymasterUtils.getDigestOutputSizeBits(iIntValue);
                if (digestOutputSizeBits <= i3 && (i4 == -1 || digestOutputSizeBits > i5)) {
                    i4 = iIntValue;
                    i5 = digestOutputSizeBits;
                }
            }
            if (i4 == -1) {
                return null;
            }
            return KeyProperties.Digest.fromKeymasterToSignatureAlgorithmDigest(i4) + "WithRSA";
        }
        if (i == 3) {
            Iterator<Integer> it2 = getAvailableKeymasterSignatureDigests(keyGenParameterSpec.getDigests(), AndroidKeyStoreBCWorkaroundProvider.getSupportedEcdsaSignatureDigests()).iterator();
            int i6 = -1;
            int i7 = -1;
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                int iIntValue2 = it2.next().intValue();
                int digestOutputSizeBits2 = KeymasterUtils.getDigestOutputSizeBits(iIntValue2);
                if (digestOutputSizeBits2 != i2) {
                    if (i6 != -1) {
                        if (i7 < i2) {
                            if (digestOutputSizeBits2 > i7) {
                            }
                        } else if (digestOutputSizeBits2 >= i7 || digestOutputSizeBits2 < i2) {
                        }
                    }
                    i6 = iIntValue2;
                    i7 = digestOutputSizeBits2;
                } else {
                    i6 = iIntValue2;
                    break;
                }
            }
            if (i6 == -1) {
                return null;
            }
            return KeyProperties.Digest.fromKeymasterToSignatureAlgorithmDigest(i6) + "WithECDSA";
        }
        throw new ProviderException("Unsupported algorithm: " + i);
    }

    private static Set<Integer> getAvailableKeymasterSignatureDigests(String[] strArr, String[] strArr2) {
        HashSet hashSet = new HashSet();
        for (int i : KeyProperties.Digest.allToKeymaster(strArr)) {
            hashSet.add(Integer.valueOf(i));
        }
        HashSet hashSet2 = new HashSet();
        for (int i2 : KeyProperties.Digest.allToKeymaster(strArr2)) {
            hashSet2.add(Integer.valueOf(i2));
        }
        HashSet hashSet3 = new HashSet(hashSet2);
        hashSet3.retainAll(hashSet);
        return hashSet3;
    }
}
