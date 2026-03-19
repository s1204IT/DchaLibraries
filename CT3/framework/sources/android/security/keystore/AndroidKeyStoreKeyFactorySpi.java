package android.security.keystore;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.storage.VolumeInfo;
import android.security.Credentials;
import android.security.KeyStore;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class AndroidKeyStoreKeyFactorySpi extends KeyFactorySpi {
    private final KeyStore mKeyStore = KeyStore.getInstance();

    @Override
    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpecClass) throws InvalidKeySpecException {
        if (key == 0) {
            throw new InvalidKeySpecException("key == null");
        }
        if (!(key instanceof AndroidKeyStorePrivateKey) && !(key instanceof AndroidKeyStorePublicKey)) {
            throw new InvalidKeySpecException("Unsupported key type: " + key.getClass().getName() + ". This KeyFactory supports only Android Keystore asymmetric keys");
        }
        if (keySpecClass == null) {
            throw new InvalidKeySpecException("keySpecClass == null");
        }
        if (KeyInfo.class.equals(keySpecClass)) {
            if (!(key instanceof AndroidKeyStorePrivateKey)) {
                throw new InvalidKeySpecException("Unsupported key type: " + key.getClass().getName() + ". KeyInfo can be obtained only for Android Keystore private keys");
            }
            String keyAliasInKeystore = key.getAlias();
            if (keyAliasInKeystore.startsWith(Credentials.USER_PRIVATE_KEY)) {
                String entryAlias = keyAliasInKeystore.substring(Credentials.USER_PRIVATE_KEY.length());
                T result = AndroidKeyStoreSecretKeyFactorySpi.getKeyInfo(this.mKeyStore, entryAlias, keyAliasInKeystore, key.getUid());
                return result;
            }
            throw new InvalidKeySpecException("Invalid key alias: " + keyAliasInKeystore);
        }
        if (X509EncodedKeySpec.class.equals(keySpecClass)) {
            if (!(key instanceof AndroidKeyStorePublicKey)) {
                throw new InvalidKeySpecException("Unsupported key type: " + key.getClass().getName() + ". X509EncodedKeySpec can be obtained only for Android Keystore public keys");
            }
            T result2 = new X509EncodedKeySpec(key.getEncoded());
            return result2;
        }
        if (PKCS8EncodedKeySpec.class.equals(keySpecClass)) {
            if (key instanceof AndroidKeyStorePrivateKey) {
                throw new InvalidKeySpecException("Key material export of Android Keystore private keys is not supported");
            }
            throw new InvalidKeySpecException("Cannot export key material of public key in PKCS#8 format. Only X.509 format (X509EncodedKeySpec) supported for public keys.");
        }
        if (RSAPublicKeySpec.class.equals(keySpecClass)) {
            if (key instanceof AndroidKeyStoreRSAPublicKey) {
                T result3 = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
                return result3;
            }
            throw new InvalidKeySpecException("Obtaining RSAPublicKeySpec not supported for " + key.getAlgorithm() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + (key instanceof AndroidKeyStorePrivateKey ? VolumeInfo.ID_PRIVATE_INTERNAL : "public") + " key");
        }
        if (ECPublicKeySpec.class.equals(keySpecClass)) {
            if (key instanceof AndroidKeyStoreECPublicKey) {
                T result4 = new ECPublicKeySpec(key.getW(), key.getParams());
                return result4;
            }
            throw new InvalidKeySpecException("Obtaining ECPublicKeySpec not supported for " + key.getAlgorithm() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + (key instanceof AndroidKeyStorePrivateKey ? VolumeInfo.ID_PRIVATE_INTERNAL : "public") + " key");
        }
        throw new InvalidKeySpecException("Unsupported key spec: " + keySpecClass.getName());
    }

    @Override
    protected PrivateKey engineGeneratePrivate(KeySpec spec) throws InvalidKeySpecException {
        throw new InvalidKeySpecException("To generate a key pair in Android Keystore, use KeyPairGenerator initialized with " + KeyGenParameterSpec.class.getName());
    }

    @Override
    protected PublicKey engineGeneratePublic(KeySpec spec) throws InvalidKeySpecException {
        throw new InvalidKeySpecException("To generate a key pair in Android Keystore, use KeyPairGenerator initialized with " + KeyGenParameterSpec.class.getName());
    }

    @Override
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (!(key instanceof AndroidKeyStorePrivateKey) && !(key instanceof AndroidKeyStorePublicKey)) {
            throw new InvalidKeyException("To import a key into Android Keystore, use KeyStore.setEntry");
        }
        return key;
    }
}
