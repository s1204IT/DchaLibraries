package android.security.keystore;

import android.security.Credentials;
import android.security.GateKeeper;
import android.security.KeyStore;
import android.security.KeyStoreParameter;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.crypto.SecretKey;
import libcore.util.EmptyArray;

public class AndroidKeyStoreSpi extends KeyStoreSpi {
    public static final String NAME = "AndroidKeyStore";
    private KeyStore mKeyStore;
    private int mUid = -1;

    @Override
    public Key engineGetKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        String str2 = Credentials.USER_PRIVATE_KEY + str;
        if (!this.mKeyStore.contains(str2, this.mUid)) {
            str2 = Credentials.USER_SECRET_KEY + str;
            if (!this.mKeyStore.contains(str2, this.mUid)) {
                return null;
            }
        }
        return AndroidKeyStoreProvider.loadAndroidKeyStoreKeyFromKeystore(this.mKeyStore, str2, this.mUid);
    }

    @Override
    public Certificate[] engineGetCertificateChain(String str) {
        Certificate[] certificateArr;
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        X509Certificate x509Certificate = (X509Certificate) engineGetCertificate(str);
        if (x509Certificate == null) {
            return null;
        }
        byte[] bArr = this.mKeyStore.get(Credentials.CA_CERTIFICATE + str, this.mUid);
        int i = 1;
        if (bArr != null) {
            Collection<X509Certificate> certificates = toCertificates(bArr);
            certificateArr = new Certificate[certificates.size() + 1];
            Iterator<X509Certificate> it = certificates.iterator();
            while (it.hasNext()) {
                certificateArr[i] = it.next();
                i++;
            }
        } else {
            certificateArr = new Certificate[1];
        }
        certificateArr[0] = x509Certificate;
        return certificateArr;
    }

    @Override
    public Certificate engineGetCertificate(String str) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        byte[] bArr = this.mKeyStore.get(Credentials.USER_CERTIFICATE + str, this.mUid);
        if (bArr != null) {
            return getCertificateForPrivateKeyEntry(str, bArr);
        }
        byte[] bArr2 = this.mKeyStore.get(Credentials.CA_CERTIFICATE + str, this.mUid);
        if (bArr2 != null) {
            return getCertificateForTrustedCertificateEntry(bArr2);
        }
        return null;
    }

    private Certificate getCertificateForTrustedCertificateEntry(byte[] bArr) {
        return toCertificate(bArr);
    }

    private Certificate getCertificateForPrivateKeyEntry(String str, byte[] bArr) {
        X509Certificate certificate = toCertificate(bArr);
        if (certificate == null) {
            return null;
        }
        String str2 = Credentials.USER_PRIVATE_KEY + str;
        if (this.mKeyStore.contains(str2, this.mUid)) {
            return wrapIntoKeyStoreCertificate(str2, this.mUid, certificate);
        }
        return certificate;
    }

    private static KeyStoreX509Certificate wrapIntoKeyStoreCertificate(String str, int i, X509Certificate x509Certificate) {
        if (x509Certificate != null) {
            return new KeyStoreX509Certificate(str, i, x509Certificate);
        }
        return null;
    }

    private static X509Certificate toCertificate(byte[] bArr) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr));
        } catch (CertificateException e) {
            Log.w("AndroidKeyStore", "Couldn't parse certificate in keystore", e);
            return null;
        }
    }

    private static Collection<X509Certificate> toCertificates(byte[] bArr) {
        try {
            return CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(bArr));
        } catch (CertificateException e) {
            Log.w("AndroidKeyStore", "Couldn't parse certificates in keystore", e);
            return new ArrayList();
        }
    }

    private Date getModificationDate(String str) {
        long j = this.mKeyStore.getmtime(str, this.mUid);
        if (j == -1) {
            return null;
        }
        return new Date(j);
    }

    @Override
    public Date engineGetCreationDate(String str) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        Date modificationDate = getModificationDate(Credentials.USER_PRIVATE_KEY + str);
        if (modificationDate != null) {
            return modificationDate;
        }
        Date modificationDate2 = getModificationDate(Credentials.USER_SECRET_KEY + str);
        if (modificationDate2 != null) {
            return modificationDate2;
        }
        Date modificationDate3 = getModificationDate(Credentials.USER_CERTIFICATE + str);
        if (modificationDate3 != null) {
            return modificationDate3;
        }
        return getModificationDate(Credentials.CA_CERTIFICATE + str);
    }

    @Override
    public void engineSetKeyEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) throws KeyStoreException {
        if (cArr != null && cArr.length > 0) {
            throw new KeyStoreException("entries cannot be protected with passwords");
        }
        if (key instanceof PrivateKey) {
            setPrivateKeyEntry(str, (PrivateKey) key, certificateArr, null);
        } else {
            if (key instanceof SecretKey) {
                setSecretKeyEntry(str, (SecretKey) key, null);
                return;
            }
            throw new KeyStoreException("Only PrivateKey and SecretKey are supported");
        }
    }

    private static KeyProtection getLegacyKeyProtectionParameter(PrivateKey privateKey) throws KeyStoreException {
        KeyProtection.Builder builder;
        String algorithm = privateKey.getAlgorithm();
        if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(algorithm)) {
            builder = new KeyProtection.Builder(12);
            builder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
        } else if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(algorithm)) {
            builder = new KeyProtection.Builder(15);
            builder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
            builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);
            builder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS);
            builder.setRandomizedEncryptionRequired(false);
        } else {
            throw new KeyStoreException("Unsupported key algorithm: " + algorithm);
        }
        builder.setUserAuthenticationRequired(false);
        return builder.build();
    }

    private void setPrivateKeyEntry(String str, PrivateKey privateKey, Certificate[] certificateArr, KeyStore.ProtectionParameter protectionParameter) throws KeyStoreException {
        KeyProtection legacyKeyProtectionParameter;
        int i;
        int i2;
        int i3;
        byte[] bArr;
        String alias;
        byte[] bArr2;
        KeymasterArguments keymasterArguments;
        byte[] bArr3;
        int i4 = 0;
        if (protectionParameter == null) {
            legacyKeyProtectionParameter = getLegacyKeyProtectionParameter(privateKey);
            i2 = 0;
        } else if (protectionParameter instanceof KeyStoreParameter) {
            KeyProtection legacyKeyProtectionParameter2 = getLegacyKeyProtectionParameter(privateKey);
            if (!((KeyStoreParameter) protectionParameter).isEncryptionRequired()) {
                i3 = 0;
            } else {
                i3 = 1;
            }
            i2 = i3;
            legacyKeyProtectionParameter = legacyKeyProtectionParameter2;
        } else if (protectionParameter instanceof KeyProtection) {
            legacyKeyProtectionParameter = (KeyProtection) protectionParameter;
            if (legacyKeyProtectionParameter.isCriticalToDeviceEncryption()) {
                i = 8;
            } else {
                i = 0;
            }
            if (legacyKeyProtectionParameter.isStrongBoxBacked()) {
                i |= 16;
            }
            i2 = i;
        } else {
            throw new KeyStoreException("Unsupported protection parameter class:" + protectionParameter.getClass().getName() + ". Supported: " + KeyProtection.class.getName() + ", " + KeyStoreParameter.class.getName());
        }
        if (certificateArr == null || certificateArr.length == 0) {
            throw new KeyStoreException("Must supply at least one Certificate with PrivateKey");
        }
        X509Certificate[] x509CertificateArr = new X509Certificate[certificateArr.length];
        for (int i5 = 0; i5 < certificateArr.length; i5++) {
            if (!"X.509".equals(certificateArr[i5].getType())) {
                throw new KeyStoreException("Certificates must be in X.509 format: invalid cert #" + i5);
            }
            if (!(certificateArr[i5] instanceof X509Certificate)) {
                throw new KeyStoreException("Certificates must be in X.509 format: invalid cert #" + i5);
            }
            x509CertificateArr[i5] = (X509Certificate) certificateArr[i5];
        }
        try {
            byte[] encoded = x509CertificateArr[0].getEncoded();
            if (certificateArr.length > 1) {
                byte[][] bArr4 = new byte[x509CertificateArr.length - 1][];
                int i6 = 0;
                int length = 0;
                while (i6 < bArr4.length) {
                    int i7 = i6 + 1;
                    try {
                        bArr4[i6] = x509CertificateArr[i7].getEncoded();
                        length += bArr4[i6].length;
                        i6 = i7;
                    } catch (CertificateEncodingException e) {
                        throw new KeyStoreException("Failed to encode certificate #" + i6, e);
                    }
                }
                byte[] bArr5 = new byte[length];
                int i8 = 0;
                for (int i9 = 0; i9 < bArr4.length; i9++) {
                    int length2 = bArr4[i9].length;
                    System.arraycopy(bArr4[i9], 0, bArr5, i8, length2);
                    i8 += length2;
                    bArr4[i9] = null;
                }
                bArr = bArr5;
            } else {
                bArr = null;
            }
            if (privateKey instanceof AndroidKeyStorePrivateKey) {
                alias = ((AndroidKeyStoreKey) privateKey).getAlias();
            } else {
                alias = null;
            }
            if (alias != null && alias.startsWith(Credentials.USER_PRIVATE_KEY)) {
                String strSubstring = alias.substring(Credentials.USER_PRIVATE_KEY.length());
                if (!str.equals(strSubstring)) {
                    throw new KeyStoreException("Can only replace keys with same alias: " + str + " != " + strSubstring);
                }
                keymasterArguments = null;
                bArr2 = null;
            } else {
                String format = privateKey.getFormat();
                if (format == null || !"PKCS#8".equals(format)) {
                    throw new KeyStoreException("Unsupported private key export format: " + format + ". Only private keys which export their key material in PKCS#8 format are supported.");
                }
                byte[] encoded2 = privateKey.getEncoded();
                if (encoded2 == null) {
                    throw new KeyStoreException("Private key did not export any key material");
                }
                KeymasterArguments keymasterArguments2 = new KeymasterArguments();
                try {
                    keymasterArguments2.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, KeyProperties.KeyAlgorithm.toKeymasterAsymmetricKeyAlgorithm(privateKey.getAlgorithm()));
                    int purposes = legacyKeyProtectionParameter.getPurposes();
                    keymasterArguments2.addEnums(KeymasterDefs.KM_TAG_PURPOSE, KeyProperties.Purpose.allToKeymaster(purposes));
                    if (legacyKeyProtectionParameter.isDigestsSpecified()) {
                        keymasterArguments2.addEnums(KeymasterDefs.KM_TAG_DIGEST, KeyProperties.Digest.allToKeymaster(legacyKeyProtectionParameter.getDigests()));
                    }
                    keymasterArguments2.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, KeyProperties.BlockMode.allToKeymaster(legacyKeyProtectionParameter.getBlockModes()));
                    int[] iArrAllToKeymaster = KeyProperties.EncryptionPadding.allToKeymaster(legacyKeyProtectionParameter.getEncryptionPaddings());
                    if ((purposes & 1) != 0 && legacyKeyProtectionParameter.isRandomizedEncryptionRequired()) {
                        int length3 = iArrAllToKeymaster.length;
                        while (i4 < length3) {
                            int i10 = iArrAllToKeymaster[i4];
                            if (KeymasterUtils.isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(i10)) {
                                i4++;
                            } else {
                                throw new KeyStoreException("Randomized encryption (IND-CPA) required but is violated by encryption padding mode: " + KeyProperties.EncryptionPadding.fromKeymaster(i10) + ". See KeyProtection documentation.");
                            }
                        }
                    }
                    keymasterArguments2.addEnums(KeymasterDefs.KM_TAG_PADDING, iArrAllToKeymaster);
                    keymasterArguments2.addEnums(KeymasterDefs.KM_TAG_PADDING, KeyProperties.SignaturePadding.allToKeymaster(legacyKeyProtectionParameter.getSignaturePaddings()));
                    KeymasterUtils.addUserAuthArgs(keymasterArguments2, legacyKeyProtectionParameter);
                    keymasterArguments2.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, legacyKeyProtectionParameter.getKeyValidityStart());
                    keymasterArguments2.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, legacyKeyProtectionParameter.getKeyValidityForOriginationEnd());
                    keymasterArguments2.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, legacyKeyProtectionParameter.getKeyValidityForConsumptionEnd());
                    i4 = 1;
                    bArr2 = encoded2;
                    keymasterArguments = keymasterArguments2;
                } catch (IllegalArgumentException | IllegalStateException e2) {
                    throw new KeyStoreException(e2);
                }
            }
            try {
                if (i4 != 0) {
                    Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
                    KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
                    bArr3 = encoded;
                    int iImportKey = this.mKeyStore.importKey(Credentials.USER_PRIVATE_KEY + str, keymasterArguments, 1, bArr2, this.mUid, i2, keyCharacteristics);
                    if (iImportKey != 1) {
                        throw new KeyStoreException("Failed to store private key", android.security.KeyStore.getKeyStoreException(iImportKey));
                    }
                } else {
                    bArr3 = encoded;
                    Credentials.deleteCertificateTypesForAlias(this.mKeyStore, str, this.mUid);
                    Credentials.deleteLegacyKeyForAlias(this.mKeyStore, str, this.mUid);
                }
                int iInsert = this.mKeyStore.insert(Credentials.USER_CERTIFICATE + str, bArr3, this.mUid, i2);
                if (iInsert != 1) {
                    throw new KeyStoreException("Failed to store certificate #0", android.security.KeyStore.getKeyStoreException(iInsert));
                }
                int iInsert2 = this.mKeyStore.insert(Credentials.CA_CERTIFICATE + str, bArr, this.mUid, i2);
                if (iInsert2 != 1) {
                    throw new KeyStoreException("Failed to store certificate chain", android.security.KeyStore.getKeyStoreException(iInsert2));
                }
            } finally {
                if (i4 != 0) {
                    Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
                } else {
                    Credentials.deleteCertificateTypesForAlias(this.mKeyStore, str, this.mUid);
                    Credentials.deleteLegacyKeyForAlias(this.mKeyStore, str, this.mUid);
                }
            }
        } catch (CertificateEncodingException e3) {
            throw new KeyStoreException("Failed to encode certificate #0", e3);
        }
    }

    private void setSecretKeyEntry(String str, SecretKey secretKey, KeyStore.ProtectionParameter protectionParameter) throws KeyStoreException {
        int[] iArrAllToKeymaster;
        if (protectionParameter != null && !(protectionParameter instanceof KeyProtection)) {
            throw new KeyStoreException("Unsupported protection parameter class: " + protectionParameter.getClass().getName() + ". Supported: " + KeyProtection.class.getName());
        }
        KeyProtection keyProtection = (KeyProtection) protectionParameter;
        if (secretKey instanceof AndroidKeyStoreSecretKey) {
            String alias = ((AndroidKeyStoreSecretKey) secretKey).getAlias();
            if (alias == null) {
                throw new KeyStoreException("KeyStore-backed secret key does not have an alias");
            }
            String str2 = Credentials.USER_PRIVATE_KEY;
            if (!alias.startsWith(Credentials.USER_PRIVATE_KEY)) {
                str2 = Credentials.USER_SECRET_KEY;
                if (!alias.startsWith(Credentials.USER_SECRET_KEY)) {
                    throw new KeyStoreException("KeyStore-backed secret key has invalid alias: " + alias);
                }
            }
            String strSubstring = alias.substring(str2.length());
            if (!str.equals(strSubstring)) {
                throw new KeyStoreException("Can only replace KeyStore-backed keys with same alias: " + str + " != " + strSubstring);
            }
            if (keyProtection != null) {
                throw new KeyStoreException("Modifying KeyStore-backed key using protection parameters not supported");
            }
            return;
        }
        if (keyProtection == null) {
            throw new KeyStoreException("Protection parameters must be specified when importing a symmetric key");
        }
        String format = secretKey.getFormat();
        if (format == null) {
            throw new KeyStoreException("Only secret keys that export their key material are supported");
        }
        if (!"RAW".equals(format)) {
            throw new KeyStoreException("Unsupported secret key material export format: " + format);
        }
        byte[] encoded = secretKey.getEncoded();
        if (encoded == null) {
            throw new KeyStoreException("Key did not export its key material despite supporting RAW format export");
        }
        KeymasterArguments keymasterArguments = new KeymasterArguments();
        try {
            int keymasterSecretKeyAlgorithm = KeyProperties.KeyAlgorithm.toKeymasterSecretKeyAlgorithm(secretKey.getAlgorithm());
            keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, keymasterSecretKeyAlgorithm);
            if (keymasterSecretKeyAlgorithm == 128) {
                int keymasterDigest = KeyProperties.KeyAlgorithm.toKeymasterDigest(secretKey.getAlgorithm());
                if (keymasterDigest == -1) {
                    throw new ProviderException("HMAC key algorithm digest unknown for key algorithm " + secretKey.getAlgorithm());
                }
                iArrAllToKeymaster = new int[]{keymasterDigest};
                if (keyProtection.isDigestsSpecified()) {
                    int[] iArrAllToKeymaster2 = KeyProperties.Digest.allToKeymaster(keyProtection.getDigests());
                    if (iArrAllToKeymaster2.length != 1 || iArrAllToKeymaster2[0] != keymasterDigest) {
                        throw new KeyStoreException("Unsupported digests specification: " + Arrays.asList(keyProtection.getDigests()) + ". Only " + KeyProperties.Digest.fromKeymaster(keymasterDigest) + " supported for HMAC key algorithm " + secretKey.getAlgorithm());
                    }
                }
            } else if (keyProtection.isDigestsSpecified()) {
                iArrAllToKeymaster = KeyProperties.Digest.allToKeymaster(keyProtection.getDigests());
            } else {
                iArrAllToKeymaster = EmptyArray.INT;
            }
            keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, iArrAllToKeymaster);
            int purposes = keyProtection.getPurposes();
            int[] iArrAllToKeymaster3 = KeyProperties.BlockMode.allToKeymaster(keyProtection.getBlockModes());
            int i = purposes & 1;
            if (i != 0 && keyProtection.isRandomizedEncryptionRequired()) {
                for (int i2 : iArrAllToKeymaster3) {
                    if (!KeymasterUtils.isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(i2)) {
                        throw new KeyStoreException("Randomized encryption (IND-CPA) required but may be violated by block mode: " + KeyProperties.BlockMode.fromKeymaster(i2) + ". See KeyProtection documentation.");
                    }
                }
            }
            keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PURPOSE, KeyProperties.Purpose.allToKeymaster(purposes));
            keymasterArguments.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, iArrAllToKeymaster3);
            if (keyProtection.getSignaturePaddings().length > 0) {
                throw new KeyStoreException("Signature paddings not supported for symmetric keys");
            }
            keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PADDING, KeyProperties.EncryptionPadding.allToKeymaster(keyProtection.getEncryptionPaddings()));
            KeymasterUtils.addUserAuthArgs(keymasterArguments, keyProtection);
            KeymasterUtils.addMinMacLengthAuthorizationIfNecessary(keymasterArguments, keymasterSecretKeyAlgorithm, iArrAllToKeymaster3, iArrAllToKeymaster);
            keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, keyProtection.getKeyValidityStart());
            keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, keyProtection.getKeyValidityForOriginationEnd());
            keymasterArguments.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, keyProtection.getKeyValidityForConsumptionEnd());
            if (i != 0 && !keyProtection.isRandomizedEncryptionRequired()) {
                keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_CALLER_NONCE);
            }
            int i3 = keyProtection.isCriticalToDeviceEncryption() ? 8 : 0;
            int i4 = keyProtection.isStrongBoxBacked() ? i3 | 16 : i3;
            Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
            int iImportKey = this.mKeyStore.importKey(Credentials.USER_PRIVATE_KEY + str, keymasterArguments, 3, encoded, this.mUid, i4, new KeyCharacteristics());
            if (iImportKey != 1) {
                throw new KeyStoreException("Failed to import secret key. Keystore error code: " + iImportKey);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new KeyStoreException(e);
        }
    }

    private void setWrappedKeyEntry(String str, WrappedKeyEntry wrappedKeyEntry, KeyStore.ProtectionParameter protectionParameter) throws KeyStoreException {
        if (protectionParameter != null) {
            throw new KeyStoreException("Protection parameters are specified inside wrapped keys");
        }
        byte[] bArr = new byte[32];
        KeymasterArguments keymasterArguments = new KeymasterArguments();
        String[] strArrSplit = wrappedKeyEntry.getTransformation().split("/");
        String str2 = strArrSplit[0];
        if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(str2) || KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(str2)) {
            keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 1);
        }
        if (strArrSplit.length > 1) {
            String str3 = strArrSplit[1];
            if (KeyProperties.BLOCK_MODE_ECB.equalsIgnoreCase(str3)) {
                keymasterArguments.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 1);
            } else if (KeyProperties.BLOCK_MODE_CBC.equalsIgnoreCase(str3)) {
                keymasterArguments.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 2);
            } else if (KeyProperties.BLOCK_MODE_CTR.equalsIgnoreCase(str3)) {
                keymasterArguments.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 3);
            } else if (KeyProperties.BLOCK_MODE_GCM.equalsIgnoreCase(str3)) {
                keymasterArguments.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 32);
            }
        }
        if (strArrSplit.length > 2) {
            String str4 = strArrSplit[2];
            if (!KeyProperties.ENCRYPTION_PADDING_NONE.equalsIgnoreCase(str4)) {
                if (KeyProperties.ENCRYPTION_PADDING_PKCS7.equalsIgnoreCase(str4)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PADDING, 64);
                } else if (KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1.equalsIgnoreCase(str4)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PADDING, 4);
                } else if (KeyProperties.ENCRYPTION_PADDING_RSA_OAEP.equalsIgnoreCase(str4)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_PADDING, 2);
                }
            }
        }
        KeyGenParameterSpec keyGenParameterSpec = (KeyGenParameterSpec) wrappedKeyEntry.getAlgorithmParameterSpec();
        if (keyGenParameterSpec.isDigestsSpecified()) {
            String str5 = keyGenParameterSpec.getDigests()[0];
            if (!KeyProperties.DIGEST_NONE.equalsIgnoreCase(str5)) {
                if (KeyProperties.DIGEST_MD5.equalsIgnoreCase(str5)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, 1);
                } else if (KeyProperties.DIGEST_SHA1.equalsIgnoreCase(str5)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, 2);
                } else if (KeyProperties.DIGEST_SHA224.equalsIgnoreCase(str5)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, 3);
                } else if (KeyProperties.DIGEST_SHA256.equalsIgnoreCase(str5)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, 4);
                } else if (KeyProperties.DIGEST_SHA384.equalsIgnoreCase(str5)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, 5);
                } else if (KeyProperties.DIGEST_SHA512.equalsIgnoreCase(str5)) {
                    keymasterArguments.addEnums(KeymasterDefs.KM_TAG_DIGEST, 6);
                }
            }
        }
        int iImportWrappedKey = this.mKeyStore.importWrappedKey(Credentials.USER_SECRET_KEY + str, wrappedKeyEntry.getWrappedKeyBytes(), Credentials.USER_PRIVATE_KEY + wrappedKeyEntry.getWrappingKeyAlias(), bArr, keymasterArguments, GateKeeper.getSecureUserId(), 0L, this.mUid, new KeyCharacteristics());
        if (iImportWrappedKey == -100) {
            throw new SecureKeyImportUnavailableException("Could not import wrapped key");
        }
        if (iImportWrappedKey != 1) {
            throw new KeyStoreException("Failed to import wrapped key. Keystore error code: " + iImportWrappedKey);
        }
    }

    @Override
    public void engineSetKeyEntry(String str, byte[] bArr, Certificate[] certificateArr) throws KeyStoreException {
        throw new KeyStoreException("Operation not supported because key encoding is unknown");
    }

    @Override
    public void engineSetCertificateEntry(String str, Certificate certificate) throws KeyStoreException {
        if (isKeyEntry(str)) {
            throw new KeyStoreException("Entry exists and is not a trusted certificate");
        }
        if (certificate == null) {
            throw new NullPointerException("cert == null");
        }
        try {
            byte[] encoded = certificate.getEncoded();
            if (!this.mKeyStore.put(Credentials.CA_CERTIFICATE + str, encoded, this.mUid, 0)) {
                throw new KeyStoreException("Couldn't insert certificate; is KeyStore initialized?");
            }
        } catch (CertificateEncodingException e) {
            throw new KeyStoreException(e);
        }
    }

    @Override
    public void engineDeleteEntry(String str) throws KeyStoreException {
        if (!Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid)) {
            throw new KeyStoreException("Failed to delete entry: " + str);
        }
    }

    private Set<String> getUniqueAliases() {
        String[] list = this.mKeyStore.list("", this.mUid);
        if (list == null) {
            return new HashSet();
        }
        HashSet hashSet = new HashSet(list.length);
        for (String str : list) {
            int iIndexOf = str.indexOf(95);
            if (iIndexOf == -1 || str.length() <= iIndexOf) {
                Log.e("AndroidKeyStore", "invalid alias: " + str);
            } else {
                hashSet.add(new String(str.substring(iIndexOf + 1)));
            }
        }
        return hashSet;
    }

    @Override
    public Enumeration<String> engineAliases() {
        return Collections.enumeration(getUniqueAliases());
    }

    @Override
    public boolean engineContainsAlias(String str) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        if (!this.mKeyStore.contains(Credentials.USER_PRIVATE_KEY + str, this.mUid)) {
            if (!this.mKeyStore.contains(Credentials.USER_SECRET_KEY + str, this.mUid)) {
                if (!this.mKeyStore.contains(Credentials.USER_CERTIFICATE + str, this.mUid)) {
                    if (!this.mKeyStore.contains(Credentials.CA_CERTIFICATE + str, this.mUid)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public int engineSize() {
        return getUniqueAliases().size();
    }

    @Override
    public boolean engineIsKeyEntry(String str) {
        return isKeyEntry(str);
    }

    private boolean isKeyEntry(String str) {
        if (!this.mKeyStore.contains(Credentials.USER_PRIVATE_KEY + str, this.mUid)) {
            if (!this.mKeyStore.contains(Credentials.USER_SECRET_KEY + str, this.mUid)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCertificateEntry(String str) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        return this.mKeyStore.contains(Credentials.CA_CERTIFICATE + str, this.mUid);
    }

    @Override
    public boolean engineIsCertificateEntry(String str) {
        return !isKeyEntry(str) && isCertificateEntry(str);
    }

    @Override
    public String engineGetCertificateAlias(Certificate certificate) {
        if (certificate == null || !"X.509".equalsIgnoreCase(certificate.getType())) {
            return null;
        }
        try {
            byte[] encoded = certificate.getEncoded();
            if (encoded == null) {
                return null;
            }
            HashSet hashSet = new HashSet();
            String[] list = this.mKeyStore.list(Credentials.USER_CERTIFICATE, this.mUid);
            if (list != null) {
                for (String str : list) {
                    byte[] bArr = this.mKeyStore.get(Credentials.USER_CERTIFICATE + str, this.mUid);
                    if (bArr != null) {
                        hashSet.add(str);
                        if (Arrays.equals(bArr, encoded)) {
                            return str;
                        }
                    }
                }
            }
            String[] list2 = this.mKeyStore.list(Credentials.CA_CERTIFICATE, this.mUid);
            if (list != null) {
                for (String str2 : list2) {
                    if (!hashSet.contains(str2)) {
                        byte[] bArr2 = this.mKeyStore.get(Credentials.CA_CERTIFICATE + str2, this.mUid);
                        if (bArr2 != null && Arrays.equals(bArr2, encoded)) {
                            return str2;
                        }
                    }
                }
            }
            return null;
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    @Override
    public void engineStore(OutputStream outputStream, char[] cArr) throws NoSuchAlgorithmException, IOException, CertificateException {
        throw new UnsupportedOperationException("Can not serialize AndroidKeyStore to OutputStream");
    }

    @Override
    public void engineLoad(InputStream inputStream, char[] cArr) throws NoSuchAlgorithmException, IOException, CertificateException {
        if (inputStream != null) {
            throw new IllegalArgumentException("InputStream not supported");
        }
        if (cArr != null) {
            throw new IllegalArgumentException("password not supported");
        }
        this.mKeyStore = android.security.KeyStore.getInstance();
        this.mUid = -1;
    }

    @Override
    public void engineLoad(KeyStore.LoadStoreParameter loadStoreParameter) throws NoSuchAlgorithmException, IOException, CertificateException {
        int uid;
        if (loadStoreParameter != null) {
            if (loadStoreParameter instanceof AndroidKeyStoreLoadStoreParameter) {
                uid = ((AndroidKeyStoreLoadStoreParameter) loadStoreParameter).getUid();
            } else {
                throw new IllegalArgumentException("Unsupported param type: " + loadStoreParameter.getClass());
            }
        } else {
            uid = -1;
        }
        this.mKeyStore = android.security.KeyStore.getInstance();
        this.mUid = uid;
    }

    @Override
    public void engineSetEntry(String str, KeyStore.Entry entry, KeyStore.ProtectionParameter protectionParameter) throws KeyStoreException {
        if (entry == null) {
            throw new KeyStoreException("entry == null");
        }
        Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            engineSetCertificateEntry(str, ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate());
            return;
        }
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
            setPrivateKeyEntry(str, privateKeyEntry.getPrivateKey(), privateKeyEntry.getCertificateChain(), protectionParameter);
        } else if (entry instanceof KeyStore.SecretKeyEntry) {
            setSecretKeyEntry(str, ((KeyStore.SecretKeyEntry) entry).getSecretKey(), protectionParameter);
        } else {
            if (entry instanceof WrappedKeyEntry) {
                setWrappedKeyEntry(str, (WrappedKeyEntry) entry, protectionParameter);
                return;
            }
            throw new KeyStoreException("Entry must be a PrivateKeyEntry, SecretKeyEntry or TrustedCertificateEntry; was " + entry);
        }
    }

    static class KeyStoreX509Certificate extends DelegatingX509Certificate {
        private final String mPrivateKeyAlias;
        private final int mPrivateKeyUid;

        KeyStoreX509Certificate(String str, int i, X509Certificate x509Certificate) {
            super(x509Certificate);
            this.mPrivateKeyAlias = str;
            this.mPrivateKeyUid = i;
        }

        @Override
        public PublicKey getPublicKey() {
            PublicKey publicKey = super.getPublicKey();
            return AndroidKeyStoreProvider.getAndroidKeyStorePublicKey(this.mPrivateKeyAlias, this.mPrivateKeyUid, publicKey.getAlgorithm(), publicKey.getEncoded());
        }
    }
}
