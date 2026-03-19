package android.security.keystore;

import android.net.ProxyInfo;
import android.security.Credentials;
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
    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (isPrivateKeyEntry(alias)) {
            String privateKeyAlias = Credentials.USER_PRIVATE_KEY + alias;
            return AndroidKeyStoreProvider.loadAndroidKeyStorePrivateKeyFromKeystore(this.mKeyStore, privateKeyAlias, this.mUid);
        }
        if (isSecretKeyEntry(alias)) {
            String secretKeyAlias = Credentials.USER_SECRET_KEY + alias;
            return AndroidKeyStoreProvider.loadAndroidKeyStoreSecretKeyFromKeystore(this.mKeyStore, secretKeyAlias, this.mUid);
        }
        return null;
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {
        Certificate[] caList;
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        X509Certificate leaf = (X509Certificate) engineGetCertificate(alias);
        if (leaf == null) {
            return null;
        }
        byte[] caBytes = this.mKeyStore.get(Credentials.CA_CERTIFICATE + alias, this.mUid);
        if (caBytes != null) {
            Collection<X509Certificate> caChain = toCertificates(caBytes);
            caList = new Certificate[caChain.size() + 1];
            Iterator<X509Certificate> it = caChain.iterator();
            int i = 1;
            while (it.hasNext()) {
                caList[i] = it.next();
                i++;
            }
        } else {
            caList = new Certificate[1];
        }
        caList[0] = leaf;
        return caList;
    }

    @Override
    public Certificate engineGetCertificate(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        byte[] encodedCert = this.mKeyStore.get(Credentials.USER_CERTIFICATE + alias, this.mUid);
        if (encodedCert != null) {
            return getCertificateForPrivateKeyEntry(alias, encodedCert);
        }
        byte[] encodedCert2 = this.mKeyStore.get(Credentials.CA_CERTIFICATE + alias, this.mUid);
        if (encodedCert2 != null) {
            return getCertificateForTrustedCertificateEntry(encodedCert2);
        }
        return null;
    }

    private Certificate getCertificateForTrustedCertificateEntry(byte[] encodedCert) {
        return toCertificate(encodedCert);
    }

    private Certificate getCertificateForPrivateKeyEntry(String alias, byte[] encodedCert) {
        X509Certificate cert = toCertificate(encodedCert);
        if (cert == null) {
            return null;
        }
        String privateKeyAlias = Credentials.USER_PRIVATE_KEY + alias;
        if (this.mKeyStore.contains(privateKeyAlias, this.mUid)) {
            return wrapIntoKeyStoreCertificate(privateKeyAlias, this.mUid, cert);
        }
        return cert;
    }

    private static KeyStoreX509Certificate wrapIntoKeyStoreCertificate(String privateKeyAlias, int uid, X509Certificate certificate) {
        if (certificate != null) {
            return new KeyStoreX509Certificate(privateKeyAlias, uid, certificate);
        }
        return null;
    }

    private static X509Certificate toCertificate(byte[] bytes) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) {
            Log.w("AndroidKeyStore", "Couldn't parse certificate in keystore", e);
            return null;
        }
    }

    private static Collection<X509Certificate> toCertificates(byte[] bytes) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return certFactory.generateCertificates(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) {
            Log.w("AndroidKeyStore", "Couldn't parse certificates in keystore", e);
            return new ArrayList();
        }
    }

    private Date getModificationDate(String alias) {
        long epochMillis = this.mKeyStore.getmtime(alias, this.mUid);
        if (epochMillis == -1) {
            return null;
        }
        return new Date(epochMillis);
    }

    @Override
    public Date engineGetCreationDate(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        Date d = getModificationDate(Credentials.USER_PRIVATE_KEY + alias);
        if (d != null) {
            return d;
        }
        Date d2 = getModificationDate(Credentials.USER_SECRET_KEY + alias);
        if (d2 != null) {
            return d2;
        }
        Date d3 = getModificationDate(Credentials.USER_CERTIFICATE + alias);
        if (d3 != null) {
            return d3;
        }
        return getModificationDate(Credentials.CA_CERTIFICATE + alias);
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
        if (password != null && password.length > 0) {
            throw new KeyStoreException("entries cannot be protected with passwords");
        }
        if (key instanceof PrivateKey) {
            setPrivateKeyEntry(alias, (PrivateKey) key, chain, null);
        } else {
            if (key instanceof SecretKey) {
                setSecretKeyEntry(alias, (SecretKey) key, null);
                return;
            }
            throw new KeyStoreException("Only PrivateKey and SecretKey are supported");
        }
    }

    private static KeyProtection getLegacyKeyProtectionParameter(PrivateKey key) throws KeyStoreException {
        KeyProtection.Builder specBuilder;
        String keyAlgorithm = key.getAlgorithm();
        if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(keyAlgorithm)) {
            specBuilder = new KeyProtection.Builder(12);
            specBuilder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, "SHA-256", KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
        } else if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(keyAlgorithm)) {
            specBuilder = new KeyProtection.Builder(15);
            specBuilder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, "SHA-256", KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
            specBuilder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);
            specBuilder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS);
            specBuilder.setRandomizedEncryptionRequired(false);
        } else {
            throw new KeyStoreException("Unsupported key algorithm: " + keyAlgorithm);
        }
        specBuilder.setUserAuthenticationRequired(false);
        return specBuilder.build();
    }

    private void setPrivateKeyEntry(String alias, PrivateKey privateKey, Certificate[] chain, KeyStore.ProtectionParameter param) throws KeyStoreException {
        KeyProtection spec;
        byte[] chainBytes;
        boolean shouldReplacePrivateKey;
        byte[] pkcs8EncodedPrivateKeyBytes;
        KeymasterArguments importArgs;
        int flags = 0;
        if (param == null) {
            spec = getLegacyKeyProtectionParameter(privateKey);
        } else if (param instanceof KeyStoreParameter) {
            spec = getLegacyKeyProtectionParameter(privateKey);
            KeyStoreParameter legacySpec = (KeyStoreParameter) param;
            if (legacySpec.isEncryptionRequired()) {
                flags = 1;
            }
        } else {
            if (!(param instanceof KeyProtection)) {
                throw new KeyStoreException("Unsupported protection parameter class:" + param.getClass().getName() + ". Supported: " + KeyProtection.class.getName() + ", " + KeyStoreParameter.class.getName());
            }
            spec = (KeyProtection) param;
        }
        if (chain == null || chain.length == 0) {
            throw new KeyStoreException("Must supply at least one Certificate with PrivateKey");
        }
        X509Certificate[] x509chain = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            if (!"X.509".equals(chain[i].getType())) {
                throw new KeyStoreException("Certificates must be in X.509 format: invalid cert #" + i);
            }
            if (!(chain[i] instanceof X509Certificate)) {
                throw new KeyStoreException("Certificates must be in X.509 format: invalid cert #" + i);
            }
            x509chain[i] = (X509Certificate) chain[i];
        }
        try {
            byte[] userCertBytes = x509chain[0].getEncoded();
            if (chain.length > 1) {
                byte[][] certsBytes = new byte[x509chain.length - 1][];
                int totalCertLength = 0;
                for (int i2 = 0; i2 < certsBytes.length; i2++) {
                    try {
                        certsBytes[i2] = x509chain[i2 + 1].getEncoded();
                        totalCertLength += certsBytes[i2].length;
                    } catch (CertificateEncodingException e) {
                        throw new KeyStoreException("Failed to encode certificate #" + i2, e);
                    }
                }
                chainBytes = new byte[totalCertLength];
                int outputOffset = 0;
                for (int i3 = 0; i3 < certsBytes.length; i3++) {
                    int certLength = certsBytes[i3].length;
                    System.arraycopy(certsBytes[i3], 0, chainBytes, outputOffset, certLength);
                    outputOffset += certLength;
                    certsBytes[i3] = null;
                }
            } else {
                chainBytes = null;
            }
            String alias2 = privateKey instanceof AndroidKeyStorePrivateKey ? ((AndroidKeyStoreKey) privateKey).getAlias() : null;
            if (alias2 == null || !alias2.startsWith(Credentials.USER_PRIVATE_KEY)) {
                shouldReplacePrivateKey = true;
                String keyFormat = privateKey.getFormat();
                if (keyFormat == null || !"PKCS#8".equals(keyFormat)) {
                    throw new KeyStoreException("Unsupported private key export format: " + keyFormat + ". Only private keys which export their key material in PKCS#8 format are supported.");
                }
                pkcs8EncodedPrivateKeyBytes = privateKey.getEncoded();
                if (pkcs8EncodedPrivateKeyBytes == null) {
                    throw new KeyStoreException("Private key did not export any key material");
                }
                importArgs = new KeymasterArguments();
                try {
                    importArgs.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, KeyProperties.KeyAlgorithm.toKeymasterAsymmetricKeyAlgorithm(privateKey.getAlgorithm()));
                    int purposes = spec.getPurposes();
                    importArgs.addEnums(KeymasterDefs.KM_TAG_PURPOSE, KeyProperties.Purpose.allToKeymaster(purposes));
                    if (spec.isDigestsSpecified()) {
                        importArgs.addEnums(KeymasterDefs.KM_TAG_DIGEST, KeyProperties.Digest.allToKeymaster(spec.getDigests()));
                    }
                    importArgs.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, KeyProperties.BlockMode.allToKeymaster(spec.getBlockModes()));
                    int[] keymasterEncryptionPaddings = KeyProperties.EncryptionPadding.allToKeymaster(spec.getEncryptionPaddings());
                    if ((purposes & 1) != 0 && spec.isRandomizedEncryptionRequired()) {
                        for (int keymasterPadding : keymasterEncryptionPaddings) {
                            if (!KeymasterUtils.isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(keymasterPadding)) {
                                throw new KeyStoreException("Randomized encryption (IND-CPA) required but is violated by encryption padding mode: " + KeyProperties.EncryptionPadding.fromKeymaster(keymasterPadding) + ". See KeyProtection documentation.");
                            }
                        }
                    }
                    importArgs.addEnums(KeymasterDefs.KM_TAG_PADDING, keymasterEncryptionPaddings);
                    importArgs.addEnums(KeymasterDefs.KM_TAG_PADDING, KeyProperties.SignaturePadding.allToKeymaster(spec.getSignaturePaddings()));
                    KeymasterUtils.addUserAuthArgs(importArgs, false, spec.getUserAuthenticationValidityDurationSeconds(), spec.isUserAuthenticationValidWhileOnBody(), spec.isInvalidatedByBiometricEnrollment());
                    importArgs.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, spec.getKeyValidityStart());
                    importArgs.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, spec.getKeyValidityForOriginationEnd());
                    importArgs.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, spec.getKeyValidityForConsumptionEnd());
                } catch (IllegalArgumentException | IllegalStateException e2) {
                    throw new KeyStoreException(e2);
                }
            } else {
                String keySubalias = alias2.substring(Credentials.USER_PRIVATE_KEY.length());
                if (!alias.equals(keySubalias)) {
                    throw new KeyStoreException("Can only replace keys with same alias: " + alias + " != " + keySubalias);
                }
                shouldReplacePrivateKey = false;
                importArgs = null;
                pkcs8EncodedPrivateKeyBytes = null;
            }
            boolean success = false;
            try {
                if (shouldReplacePrivateKey) {
                    Credentials.deleteAllTypesForAlias(this.mKeyStore, alias, this.mUid);
                    KeyCharacteristics resultingKeyCharacteristics = new KeyCharacteristics();
                    int errorCode = this.mKeyStore.importKey(Credentials.USER_PRIVATE_KEY + alias, importArgs, 1, pkcs8EncodedPrivateKeyBytes, this.mUid, flags, resultingKeyCharacteristics);
                    if (errorCode != 1) {
                        throw new KeyStoreException("Failed to store private key", android.security.KeyStore.getKeyStoreException(errorCode));
                    }
                } else {
                    Credentials.deleteCertificateTypesForAlias(this.mKeyStore, alias, this.mUid);
                    Credentials.deleteSecretKeyTypeForAlias(this.mKeyStore, alias, this.mUid);
                }
                int errorCode2 = this.mKeyStore.insert(Credentials.USER_CERTIFICATE + alias, userCertBytes, this.mUid, flags);
                if (errorCode2 != 1) {
                    throw new KeyStoreException("Failed to store certificate #0", android.security.KeyStore.getKeyStoreException(errorCode2));
                }
                int errorCode3 = this.mKeyStore.insert(Credentials.CA_CERTIFICATE + alias, chainBytes, this.mUid, flags);
                if (errorCode3 != 1) {
                    throw new KeyStoreException("Failed to store certificate chain", android.security.KeyStore.getKeyStoreException(errorCode3));
                }
                success = true;
                if (success) {
                    return;
                }
            } finally {
                if (!success) {
                    if (shouldReplacePrivateKey) {
                        Credentials.deleteAllTypesForAlias(this.mKeyStore, alias, this.mUid);
                    } else {
                        Credentials.deleteCertificateTypesForAlias(this.mKeyStore, alias, this.mUid);
                        Credentials.deleteSecretKeyTypeForAlias(this.mKeyStore, alias, this.mUid);
                    }
                }
            }
        } catch (CertificateEncodingException e3) {
            throw new KeyStoreException("Failed to encode certificate #0", e3);
        }
    }

    private void setSecretKeyEntry(String entryAlias, SecretKey key, KeyStore.ProtectionParameter param) throws KeyStoreException {
        int[] keymasterDigests;
        if (param != null && !(param instanceof KeyProtection)) {
            throw new KeyStoreException("Unsupported protection parameter class: " + param.getClass().getName() + ". Supported: " + KeyProtection.class.getName());
        }
        KeyProtection params = (KeyProtection) param;
        if (key instanceof AndroidKeyStoreSecretKey) {
            String keyAliasInKeystore = ((AndroidKeyStoreSecretKey) key).getAlias();
            if (keyAliasInKeystore == null) {
                throw new KeyStoreException("KeyStore-backed secret key does not have an alias");
            }
            if (!keyAliasInKeystore.startsWith(Credentials.USER_SECRET_KEY)) {
                throw new KeyStoreException("KeyStore-backed secret key has invalid alias: " + keyAliasInKeystore);
            }
            String keyEntryAlias = keyAliasInKeystore.substring(Credentials.USER_SECRET_KEY.length());
            if (!entryAlias.equals(keyEntryAlias)) {
                throw new KeyStoreException("Can only replace KeyStore-backed keys with same alias: " + entryAlias + " != " + keyEntryAlias);
            }
            if (params != null) {
                throw new KeyStoreException("Modifying KeyStore-backed key using protection parameters not supported");
            }
            return;
        }
        if (params == null) {
            throw new KeyStoreException("Protection parameters must be specified when importing a symmetric key");
        }
        String keyExportFormat = key.getFormat();
        if (keyExportFormat == null) {
            throw new KeyStoreException("Only secret keys that export their key material are supported");
        }
        if (!"RAW".equals(keyExportFormat)) {
            throw new KeyStoreException("Unsupported secret key material export format: " + keyExportFormat);
        }
        byte[] keyMaterial = key.getEncoded();
        if (keyMaterial == null) {
            throw new KeyStoreException("Key did not export its key material despite supporting RAW format export");
        }
        KeymasterArguments args = new KeymasterArguments();
        try {
            int keymasterAlgorithm = KeyProperties.KeyAlgorithm.toKeymasterSecretKeyAlgorithm(key.getAlgorithm());
            args.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, keymasterAlgorithm);
            if (keymasterAlgorithm == 128) {
                int keymasterImpliedDigest = KeyProperties.KeyAlgorithm.toKeymasterDigest(key.getAlgorithm());
                if (keymasterImpliedDigest == -1) {
                    throw new ProviderException("HMAC key algorithm digest unknown for key algorithm " + key.getAlgorithm());
                }
                keymasterDigests = new int[]{keymasterImpliedDigest};
                if (params.isDigestsSpecified()) {
                    int[] keymasterDigestsFromParams = KeyProperties.Digest.allToKeymaster(params.getDigests());
                    if (keymasterDigestsFromParams.length != 1 || keymasterDigestsFromParams[0] != keymasterImpliedDigest) {
                        throw new KeyStoreException("Unsupported digests specification: " + Arrays.asList(params.getDigests()) + ". Only " + KeyProperties.Digest.fromKeymaster(keymasterImpliedDigest) + " supported for HMAC key algorithm " + key.getAlgorithm());
                    }
                }
            } else {
                keymasterDigests = params.isDigestsSpecified() ? KeyProperties.Digest.allToKeymaster(params.getDigests()) : EmptyArray.INT;
            }
            args.addEnums(KeymasterDefs.KM_TAG_DIGEST, keymasterDigests);
            int purposes = params.getPurposes();
            int[] keymasterBlockModes = KeyProperties.BlockMode.allToKeymaster(params.getBlockModes());
            if ((purposes & 1) != 0 && params.isRandomizedEncryptionRequired()) {
                for (int keymasterBlockMode : keymasterBlockModes) {
                    if (!KeymasterUtils.isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(keymasterBlockMode)) {
                        throw new KeyStoreException("Randomized encryption (IND-CPA) required but may be violated by block mode: " + KeyProperties.BlockMode.fromKeymaster(keymasterBlockMode) + ". See KeyProtection documentation.");
                    }
                }
            }
            args.addEnums(KeymasterDefs.KM_TAG_PURPOSE, KeyProperties.Purpose.allToKeymaster(purposes));
            args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, keymasterBlockModes);
            if (params.getSignaturePaddings().length > 0) {
                throw new KeyStoreException("Signature paddings not supported for symmetric keys");
            }
            int[] keymasterPaddings = KeyProperties.EncryptionPadding.allToKeymaster(params.getEncryptionPaddings());
            args.addEnums(KeymasterDefs.KM_TAG_PADDING, keymasterPaddings);
            KeymasterUtils.addUserAuthArgs(args, false, params.getUserAuthenticationValidityDurationSeconds(), params.isUserAuthenticationValidWhileOnBody(), params.isInvalidatedByBiometricEnrollment());
            KeymasterUtils.addMinMacLengthAuthorizationIfNecessary(args, keymasterAlgorithm, keymasterBlockModes, keymasterDigests);
            args.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, params.getKeyValidityStart());
            args.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, params.getKeyValidityForOriginationEnd());
            args.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, params.getKeyValidityForConsumptionEnd());
            if ((purposes & 1) != 0 && !params.isRandomizedEncryptionRequired()) {
                args.addBoolean(KeymasterDefs.KM_TAG_CALLER_NONCE);
            }
            Credentials.deleteAllTypesForAlias(this.mKeyStore, entryAlias, this.mUid);
            int errorCode = this.mKeyStore.importKey(Credentials.USER_SECRET_KEY + entryAlias, args, 3, keyMaterial, this.mUid, 0, new KeyCharacteristics());
            if (errorCode != 1) {
                throw new KeyStoreException("Failed to import secret key. Keystore error code: " + errorCode);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new KeyStoreException(e);
        }
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] userKey, Certificate[] chain) throws KeyStoreException {
        throw new KeyStoreException("Operation not supported because key encoding is unknown");
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        if (isKeyEntry(alias)) {
            throw new KeyStoreException("Entry exists and is not a trusted certificate");
        }
        if (cert == null) {
            throw new NullPointerException("cert == null");
        }
        try {
            byte[] encoded = cert.getEncoded();
            if (this.mKeyStore.put(Credentials.CA_CERTIFICATE + alias, encoded, this.mUid, 0)) {
            } else {
                throw new KeyStoreException("Couldn't insert certificate; is KeyStore initialized?");
            }
        } catch (CertificateEncodingException e) {
            throw new KeyStoreException(e);
        }
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException {
        if (Credentials.deleteAllTypesForAlias(this.mKeyStore, alias, this.mUid)) {
        } else {
            throw new KeyStoreException("Failed to delete entry: " + alias);
        }
    }

    private Set<String> getUniqueAliases() {
        String[] rawAliases = this.mKeyStore.list(ProxyInfo.LOCAL_EXCL_LIST, this.mUid);
        if (rawAliases == null) {
            return new HashSet();
        }
        Set<String> aliases = new HashSet<>(rawAliases.length);
        for (String alias : rawAliases) {
            int idx = alias.indexOf(95);
            if (idx == -1 || alias.length() <= idx) {
                Log.e("AndroidKeyStore", "invalid alias: " + alias);
            } else {
                aliases.add(new String(alias.substring(idx + 1)));
            }
        }
        return aliases;
    }

    @Override
    public Enumeration<String> engineAliases() {
        return Collections.enumeration(getUniqueAliases());
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        if (this.mKeyStore.contains(Credentials.USER_PRIVATE_KEY + alias, this.mUid) || this.mKeyStore.contains(Credentials.USER_SECRET_KEY + alias, this.mUid) || this.mKeyStore.contains(Credentials.USER_CERTIFICATE + alias, this.mUid)) {
            return true;
        }
        return this.mKeyStore.contains(Credentials.CA_CERTIFICATE + alias, this.mUid);
    }

    @Override
    public int engineSize() {
        return getUniqueAliases().size();
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
        return isKeyEntry(alias);
    }

    private boolean isKeyEntry(String alias) {
        if (isPrivateKeyEntry(alias)) {
            return true;
        }
        return isSecretKeyEntry(alias);
    }

    private boolean isPrivateKeyEntry(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        return this.mKeyStore.contains(Credentials.USER_PRIVATE_KEY + alias, this.mUid);
    }

    private boolean isSecretKeyEntry(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        return this.mKeyStore.contains(Credentials.USER_SECRET_KEY + alias, this.mUid);
    }

    private boolean isCertificateEntry(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        return this.mKeyStore.contains(Credentials.CA_CERTIFICATE + alias, this.mUid);
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
        if (isKeyEntry(alias)) {
            return false;
        }
        return isCertificateEntry(alias);
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
        byte[] certBytes;
        if (cert == null || !"X.509".equalsIgnoreCase(cert.getType())) {
            return null;
        }
        try {
            byte[] targetCertBytes = cert.getEncoded();
            if (targetCertBytes == null) {
                return null;
            }
            Set<String> nonCaEntries = new HashSet<>();
            String[] certAliases = this.mKeyStore.list(Credentials.USER_CERTIFICATE, this.mUid);
            if (certAliases != null) {
                for (String alias : certAliases) {
                    byte[] certBytes2 = this.mKeyStore.get(Credentials.USER_CERTIFICATE + alias, this.mUid);
                    if (certBytes2 != null) {
                        nonCaEntries.add(alias);
                        if (Arrays.equals(certBytes2, targetCertBytes)) {
                            return alias;
                        }
                    }
                }
            }
            String[] caAliases = this.mKeyStore.list(Credentials.CA_CERTIFICATE, this.mUid);
            if (certAliases != null) {
                for (String alias2 : caAliases) {
                    if (!nonCaEntries.contains(alias2) && (certBytes = this.mKeyStore.get(Credentials.CA_CERTIFICATE + alias2, this.mUid)) != null && Arrays.equals(certBytes, targetCertBytes)) {
                        return alias2;
                    }
                }
            }
            return null;
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) throws NoSuchAlgorithmException, IOException, CertificateException {
        throw new UnsupportedOperationException("Can not serialize AndroidKeyStore to OutputStream");
    }

    @Override
    public void engineLoad(InputStream stream, char[] password) throws NoSuchAlgorithmException, IOException, CertificateException {
        if (stream != null) {
            throw new IllegalArgumentException("InputStream not supported");
        }
        if (password != null) {
            throw new IllegalArgumentException("password not supported");
        }
        this.mKeyStore = android.security.KeyStore.getInstance();
        this.mUid = -1;
    }

    @Override
    public void engineLoad(KeyStore.LoadStoreParameter param) throws NoSuchAlgorithmException, IOException, CertificateException {
        int uid = -1;
        if (param != null) {
            if (param instanceof AndroidKeyStoreLoadStoreParameter) {
                uid = ((AndroidKeyStoreLoadStoreParameter) param).getUid();
            } else {
                throw new IllegalArgumentException("Unsupported param type: " + param.getClass());
            }
        }
        this.mKeyStore = android.security.KeyStore.getInstance();
        this.mUid = uid;
    }

    @Override
    public void engineSetEntry(String alias, KeyStore.Entry entry, KeyStore.ProtectionParameter param) throws KeyStoreException {
        if (entry == null) {
            throw new KeyStoreException("entry == null");
        }
        Credentials.deleteAllTypesForAlias(this.mKeyStore, alias, this.mUid);
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            KeyStore.TrustedCertificateEntry trE = (KeyStore.TrustedCertificateEntry) entry;
            engineSetCertificateEntry(alias, trE.getTrustedCertificate());
        } else if (entry instanceof KeyStore.PrivateKeyEntry) {
            KeyStore.PrivateKeyEntry prE = (KeyStore.PrivateKeyEntry) entry;
            setPrivateKeyEntry(alias, prE.getPrivateKey(), prE.getCertificateChain(), param);
        } else {
            if (entry instanceof KeyStore.SecretKeyEntry) {
                KeyStore.SecretKeyEntry secE = (KeyStore.SecretKeyEntry) entry;
                setSecretKeyEntry(alias, secE.getSecretKey(), param);
                return;
            }
            throw new KeyStoreException("Entry must be a PrivateKeyEntry, SecretKeyEntry or TrustedCertificateEntry; was " + entry);
        }
    }

    static class KeyStoreX509Certificate extends DelegatingX509Certificate {
        private final String mPrivateKeyAlias;
        private final int mPrivateKeyUid;

        KeyStoreX509Certificate(String privateKeyAlias, int privateKeyUid, X509Certificate delegate) {
            super(delegate);
            this.mPrivateKeyAlias = privateKeyAlias;
            this.mPrivateKeyUid = privateKeyUid;
        }

        @Override
        public PublicKey getPublicKey() {
            PublicKey original = super.getPublicKey();
            return AndroidKeyStoreProvider.getAndroidKeyStorePublicKey(this.mPrivateKeyAlias, this.mPrivateKeyUid, original.getAlgorithm(), original.getEncoded());
        }
    }
}
