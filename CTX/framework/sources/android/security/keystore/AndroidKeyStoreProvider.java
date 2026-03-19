package android.security.keystore;

import android.os.SystemProperties;
import android.security.KeyStore;
import android.security.keymaster.ExportResult;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;

public class AndroidKeyStoreProvider extends Provider {
    private static final String DESEDE_SYSTEM_PROPERTY = "ro.hardware.keystore_desede";
    private static final String PACKAGE_NAME = "android.security.keystore";
    public static final String PROVIDER_NAME = "AndroidKeyStore";

    public AndroidKeyStoreProvider() {
        super("AndroidKeyStore", 1.0d, "Android KeyStore security provider");
        boolean zEquals = "true".equals(SystemProperties.get(DESEDE_SYSTEM_PROPERTY));
        put("KeyStore.AndroidKeyStore", "android.security.keystore.AndroidKeyStoreSpi");
        put("KeyPairGenerator.EC", "android.security.keystore.AndroidKeyStoreKeyPairGeneratorSpi$EC");
        put("KeyPairGenerator.RSA", "android.security.keystore.AndroidKeyStoreKeyPairGeneratorSpi$RSA");
        putKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_EC);
        putKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_RSA);
        put("KeyGenerator.AES", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$AES");
        put("KeyGenerator.HmacSHA1", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA1");
        put("KeyGenerator.HmacSHA224", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA224");
        put("KeyGenerator.HmacSHA256", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA256");
        put("KeyGenerator.HmacSHA384", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA384");
        put("KeyGenerator.HmacSHA512", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA512");
        if (zEquals) {
            put("KeyGenerator.DESede", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$DESede");
        }
        putSecretKeyFactoryImpl("AES");
        if (zEquals) {
            putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_3DES);
        }
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA1);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA224);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA384);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA512);
    }

    public static void install() {
        Provider[] providers = Security.getProviders();
        int i = 0;
        while (true) {
            if (i < providers.length) {
                if ("BC".equals(providers[i].getName())) {
                    break;
                } else {
                    i++;
                }
            } else {
                i = -1;
                break;
            }
        }
        Security.addProvider(new AndroidKeyStoreProvider());
        AndroidKeyStoreBCWorkaroundProvider androidKeyStoreBCWorkaroundProvider = new AndroidKeyStoreBCWorkaroundProvider();
        if (i != -1) {
            Security.insertProviderAt(androidKeyStoreBCWorkaroundProvider, i + 1);
        } else {
            Security.addProvider(androidKeyStoreBCWorkaroundProvider);
        }
    }

    private void putSecretKeyFactoryImpl(String str) {
        put("SecretKeyFactory." + str, "android.security.keystore.AndroidKeyStoreSecretKeyFactorySpi");
    }

    private void putKeyFactoryImpl(String str) {
        put("KeyFactory." + str, "android.security.keystore.AndroidKeyStoreKeyFactorySpi");
    }

    public static long getKeyStoreOperationHandle(Object obj) {
        Object currentSpi;
        if (obj == null) {
            throw new NullPointerException();
        }
        if (obj instanceof Signature) {
            currentSpi = ((Signature) obj).getCurrentSpi();
        } else if (obj instanceof Mac) {
            currentSpi = ((Mac) obj).getCurrentSpi();
        } else if (obj instanceof Cipher) {
            currentSpi = ((Cipher) obj).getCurrentSpi();
        } else {
            throw new IllegalArgumentException("Unsupported crypto primitive: " + obj + ". Supported: Signature, Mac, Cipher");
        }
        if (currentSpi == null) {
            throw new IllegalStateException("Crypto primitive not initialized");
        }
        if (!(currentSpi instanceof KeyStoreCryptoOperation)) {
            throw new IllegalArgumentException("Crypto primitive not backed by AndroidKeyStore provider: " + obj + ", spi: " + currentSpi);
        }
        return ((KeyStoreCryptoOperation) currentSpi).getOperationHandle();
    }

    public static AndroidKeyStorePublicKey getAndroidKeyStorePublicKey(String str, int i, String str2, byte[] bArr) {
        try {
            PublicKey publicKeyGeneratePublic = KeyFactory.getInstance(str2).generatePublic(new X509EncodedKeySpec(bArr));
            if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(str2)) {
                return new AndroidKeyStoreECPublicKey(str, i, (ECPublicKey) publicKeyGeneratePublic);
            }
            if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(str2)) {
                return new AndroidKeyStoreRSAPublicKey(str, i, (RSAPublicKey) publicKeyGeneratePublic);
            }
            throw new ProviderException("Unsupported Android Keystore public key algorithm: " + str2);
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException("Failed to obtain " + str2 + " KeyFactory", e);
        } catch (InvalidKeySpecException e2) {
            throw new ProviderException("Invalid X.509 encoding of public key", e2);
        }
    }

    private static AndroidKeyStorePrivateKey getAndroidKeyStorePrivateKey(AndroidKeyStorePublicKey androidKeyStorePublicKey) {
        String algorithm = androidKeyStorePublicKey.getAlgorithm();
        if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(algorithm)) {
            return new AndroidKeyStoreECPrivateKey(androidKeyStorePublicKey.getAlias(), androidKeyStorePublicKey.getUid(), ((ECKey) androidKeyStorePublicKey).getParams());
        }
        if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(algorithm)) {
            return new AndroidKeyStoreRSAPrivateKey(androidKeyStorePublicKey.getAlias(), androidKeyStorePublicKey.getUid(), ((RSAKey) androidKeyStorePublicKey).getModulus());
        }
        throw new ProviderException("Unsupported Android Keystore public key algorithm: " + algorithm);
    }

    private static KeyCharacteristics getKeyCharacteristics(KeyStore keyStore, String str, int i) throws UnrecoverableKeyException {
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        int keyCharacteristics2 = keyStore.getKeyCharacteristics(str, null, null, i, keyCharacteristics);
        if (keyCharacteristics2 != 1) {
            throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Failed to obtain information about key").initCause(KeyStore.getKeyStoreException(keyCharacteristics2)));
        }
        return keyCharacteristics;
    }

    private static AndroidKeyStorePublicKey loadAndroidKeyStorePublicKeyFromKeystore(KeyStore keyStore, String str, int i, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        ExportResult exportResultExportKey = keyStore.exportKey(str, 0, null, null, i);
        if (exportResultExportKey.resultCode != 1) {
            throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Failed to obtain X.509 form of public key").initCause(KeyStore.getKeyStoreException(exportResultExportKey.resultCode)));
        }
        byte[] bArr = exportResultExportKey.exportData;
        Integer num = keyCharacteristics.getEnum(KeymasterDefs.KM_TAG_ALGORITHM);
        if (num == null) {
            throw new UnrecoverableKeyException("Key algorithm unknown");
        }
        try {
            return getAndroidKeyStorePublicKey(str, i, KeyProperties.KeyAlgorithm.fromKeymasterAsymmetricKeyAlgorithm(num.intValue()), bArr);
        } catch (IllegalArgumentException e) {
            throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Failed to load private key").initCause(e));
        }
    }

    public static AndroidKeyStorePublicKey loadAndroidKeyStorePublicKeyFromKeystore(KeyStore keyStore, String str, int i) throws UnrecoverableKeyException {
        return loadAndroidKeyStorePublicKeyFromKeystore(keyStore, str, i, getKeyCharacteristics(keyStore, str, i));
    }

    private static KeyPair loadAndroidKeyStoreKeyPairFromKeystore(KeyStore keyStore, String str, int i, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        AndroidKeyStorePublicKey androidKeyStorePublicKeyLoadAndroidKeyStorePublicKeyFromKeystore = loadAndroidKeyStorePublicKeyFromKeystore(keyStore, str, i, keyCharacteristics);
        return new KeyPair(androidKeyStorePublicKeyLoadAndroidKeyStorePublicKeyFromKeystore, getAndroidKeyStorePrivateKey(androidKeyStorePublicKeyLoadAndroidKeyStorePublicKeyFromKeystore));
    }

    public static KeyPair loadAndroidKeyStoreKeyPairFromKeystore(KeyStore keyStore, String str, int i) throws UnrecoverableKeyException {
        return loadAndroidKeyStoreKeyPairFromKeystore(keyStore, str, i, getKeyCharacteristics(keyStore, str, i));
    }

    private static AndroidKeyStorePrivateKey loadAndroidKeyStorePrivateKeyFromKeystore(KeyStore keyStore, String str, int i, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        return (AndroidKeyStorePrivateKey) loadAndroidKeyStoreKeyPairFromKeystore(keyStore, str, i, keyCharacteristics).getPrivate();
    }

    public static AndroidKeyStorePrivateKey loadAndroidKeyStorePrivateKeyFromKeystore(KeyStore keyStore, String str, int i) throws UnrecoverableKeyException {
        return loadAndroidKeyStorePrivateKeyFromKeystore(keyStore, str, i, getKeyCharacteristics(keyStore, str, i));
    }

    private static AndroidKeyStoreSecretKey loadAndroidKeyStoreSecretKeyFromKeystore(String str, int i, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        int iIntValue;
        Integer num = keyCharacteristics.getEnum(KeymasterDefs.KM_TAG_ALGORITHM);
        if (num == null) {
            throw new UnrecoverableKeyException("Key algorithm unknown");
        }
        List<Integer> enums = keyCharacteristics.getEnums(KeymasterDefs.KM_TAG_DIGEST);
        if (enums.isEmpty()) {
            iIntValue = -1;
        } else {
            iIntValue = enums.get(0).intValue();
        }
        try {
            return new AndroidKeyStoreSecretKey(str, i, KeyProperties.KeyAlgorithm.fromKeymasterSecretKeyAlgorithm(num.intValue(), iIntValue));
        } catch (IllegalArgumentException e) {
            throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Unsupported secret key type").initCause(e));
        }
    }

    public static AndroidKeyStoreKey loadAndroidKeyStoreKeyFromKeystore(KeyStore keyStore, String str, int i) throws UnrecoverableKeyException {
        KeyCharacteristics keyCharacteristics = getKeyCharacteristics(keyStore, str, i);
        Integer num = keyCharacteristics.getEnum(KeymasterDefs.KM_TAG_ALGORITHM);
        if (num == null) {
            throw new UnrecoverableKeyException("Key algorithm unknown");
        }
        if (num.intValue() == 128 || num.intValue() == 32 || num.intValue() == 33) {
            return loadAndroidKeyStoreSecretKeyFromKeystore(str, i, keyCharacteristics);
        }
        if (num.intValue() == 1 || num.intValue() == 3) {
            return loadAndroidKeyStorePrivateKeyFromKeystore(keyStore, str, i, keyCharacteristics);
        }
        throw new UnrecoverableKeyException("Key algorithm unknown");
    }

    public static java.security.KeyStore getKeyStoreForUid(int i) throws KeyStoreException, NoSuchProviderException {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore", "AndroidKeyStore");
        try {
            keyStore.load(new AndroidKeyStoreLoadStoreParameter(i));
            return keyStore;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreException("Failed to load AndroidKeyStore KeyStore for UID " + i, e);
        }
    }
}
