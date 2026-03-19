package android.security.keystore;

import android.security.Credentials;
import android.security.GateKeeper;
import android.security.KeyStore;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.ProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactorySpi;
import javax.crypto.spec.SecretKeySpec;

public class AndroidKeyStoreSecretKeyFactorySpi extends SecretKeyFactorySpi {
    private final KeyStore mKeyStore = KeyStore.getInstance();

    @Override
    protected KeySpec engineGetKeySpec(SecretKey secretKey, Class keySpecClass) throws InvalidKeySpecException {
        if (keySpecClass == null) {
            throw new InvalidKeySpecException("keySpecClass == null");
        }
        if (!(secretKey instanceof AndroidKeyStoreSecretKey)) {
            throw new InvalidKeySpecException("Only Android KeyStore secret keys supported: " + (secretKey != 0 ? secretKey.getClass().getName() : "null"));
        }
        if (SecretKeySpec.class.isAssignableFrom(keySpecClass)) {
            throw new InvalidKeySpecException("Key material export of Android KeyStore keys is not supported");
        }
        if (!KeyInfo.class.equals(keySpecClass)) {
            throw new InvalidKeySpecException("Unsupported key spec: " + keySpecClass.getName());
        }
        String keyAliasInKeystore = secretKey.getAlias();
        if (keyAliasInKeystore.startsWith(Credentials.USER_SECRET_KEY)) {
            String entryAlias = keyAliasInKeystore.substring(Credentials.USER_SECRET_KEY.length());
            return getKeyInfo(this.mKeyStore, entryAlias, keyAliasInKeystore, secretKey.getUid());
        }
        throw new InvalidKeySpecException("Invalid key alias: " + keyAliasInKeystore);
    }

    static KeyInfo getKeyInfo(KeyStore keyStore, String entryAlias, String keyAliasInKeystore, int keyUid) {
        boolean insideSecureHardware;
        int origin;
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        int errorCode = keyStore.getKeyCharacteristics(keyAliasInKeystore, null, null, keyUid, keyCharacteristics);
        if (errorCode != 1) {
            throw new ProviderException("Failed to obtain information about key. Keystore error: " + errorCode);
        }
        try {
            if (keyCharacteristics.hwEnforced.containsTag(KeymasterDefs.KM_TAG_ORIGIN)) {
                insideSecureHardware = true;
                origin = KeyProperties.Origin.fromKeymaster(keyCharacteristics.hwEnforced.getEnum(KeymasterDefs.KM_TAG_ORIGIN, -1));
            } else if (keyCharacteristics.swEnforced.containsTag(KeymasterDefs.KM_TAG_ORIGIN)) {
                insideSecureHardware = false;
                origin = KeyProperties.Origin.fromKeymaster(keyCharacteristics.swEnforced.getEnum(KeymasterDefs.KM_TAG_ORIGIN, -1));
            } else {
                throw new ProviderException("Key origin not available");
            }
            long keySizeUnsigned = keyCharacteristics.getUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, -1L);
            if (keySizeUnsigned == -1) {
                throw new ProviderException("Key size not available");
            }
            if (keySizeUnsigned > 2147483647L) {
                throw new ProviderException("Key too large: " + keySizeUnsigned + " bits");
            }
            int keySize = (int) keySizeUnsigned;
            int purposes = KeyProperties.Purpose.allFromKeymaster(keyCharacteristics.getEnums(KeymasterDefs.KM_TAG_PURPOSE));
            List<String> encryptionPaddingsList = new ArrayList<>();
            List<String> signaturePaddingsList = new ArrayList<>();
            Iterator keymasterPadding$iterator = keyCharacteristics.getEnums(KeymasterDefs.KM_TAG_PADDING).iterator();
            while (keymasterPadding$iterator.hasNext()) {
                int keymasterPadding = ((Integer) keymasterPadding$iterator.next()).intValue();
                try {
                    String jcaPadding = KeyProperties.EncryptionPadding.fromKeymaster(keymasterPadding);
                    encryptionPaddingsList.add(jcaPadding);
                } catch (IllegalArgumentException e) {
                    try {
                        String padding = KeyProperties.SignaturePadding.fromKeymaster(keymasterPadding);
                        signaturePaddingsList.add(padding);
                    } catch (IllegalArgumentException e2) {
                        throw new ProviderException("Unsupported encryption padding: " + keymasterPadding);
                    }
                }
            }
            String[] encryptionPaddings = (String[]) encryptionPaddingsList.toArray(new String[encryptionPaddingsList.size()]);
            String[] signaturePaddings = (String[]) signaturePaddingsList.toArray(new String[signaturePaddingsList.size()]);
            String[] digests = KeyProperties.Digest.allFromKeymaster(keyCharacteristics.getEnums(KeymasterDefs.KM_TAG_DIGEST));
            String[] blockModes = KeyProperties.BlockMode.allFromKeymaster(keyCharacteristics.getEnums(KeymasterDefs.KM_TAG_BLOCK_MODE));
            int keymasterSwEnforcedUserAuthenticators = keyCharacteristics.swEnforced.getEnum(KeymasterDefs.KM_TAG_USER_AUTH_TYPE, 0);
            int keymasterHwEnforcedUserAuthenticators = keyCharacteristics.hwEnforced.getEnum(KeymasterDefs.KM_TAG_USER_AUTH_TYPE, 0);
            List<BigInteger> keymasterSecureUserIds = keyCharacteristics.getUnsignedLongs(KeymasterDefs.KM_TAG_USER_SECURE_ID);
            Date keyValidityStart = keyCharacteristics.getDate(KeymasterDefs.KM_TAG_ACTIVE_DATETIME);
            Date keyValidityForOriginationEnd = keyCharacteristics.getDate(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME);
            Date keyValidityForConsumptionEnd = keyCharacteristics.getDate(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME);
            boolean userAuthenticationRequired = !keyCharacteristics.getBoolean(KeymasterDefs.KM_TAG_NO_AUTH_REQUIRED);
            long userAuthenticationValidityDurationSeconds = keyCharacteristics.getUnsignedInt(KeymasterDefs.KM_TAG_AUTH_TIMEOUT, -1L);
            if (userAuthenticationValidityDurationSeconds > 2147483647L) {
                throw new ProviderException("User authentication timeout validity too long: " + userAuthenticationValidityDurationSeconds + " seconds");
            }
            boolean userAuthenticationRequirementEnforcedBySecureHardware = userAuthenticationRequired && keymasterHwEnforcedUserAuthenticators != 0 && keymasterSwEnforcedUserAuthenticators == 0;
            boolean userAuthenticationValidWhileOnBody = keyCharacteristics.hwEnforced.getBoolean(KeymasterDefs.KM_TAG_ALLOW_WHILE_ON_BODY);
            boolean invalidatedByBiometricEnrollment = false;
            if (keymasterSwEnforcedUserAuthenticators == 2 || keymasterHwEnforcedUserAuthenticators == 2) {
                invalidatedByBiometricEnrollment = (keymasterSecureUserIds == null || keymasterSecureUserIds.isEmpty() || keymasterSecureUserIds.contains(getGateKeeperSecureUserId())) ? false : true;
            }
            return new KeyInfo(entryAlias, insideSecureHardware, origin, keySize, keyValidityStart, keyValidityForOriginationEnd, keyValidityForConsumptionEnd, purposes, encryptionPaddings, signaturePaddings, digests, blockModes, userAuthenticationRequired, (int) userAuthenticationValidityDurationSeconds, userAuthenticationRequirementEnforcedBySecureHardware, userAuthenticationValidWhileOnBody, invalidatedByBiometricEnrollment);
        } catch (IllegalArgumentException e3) {
            throw new ProviderException("Unsupported key characteristic", e3);
        }
    }

    private static BigInteger getGateKeeperSecureUserId() throws ProviderException {
        try {
            return BigInteger.valueOf(GateKeeper.getSecureUserId());
        } catch (IllegalStateException e) {
            throw new ProviderException("Failed to get GateKeeper secure user ID", e);
        }
    }

    @Override
    protected SecretKey engineGenerateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        throw new InvalidKeySpecException("To generate secret key in Android Keystore, use KeyGenerator initialized with " + KeyGenParameterSpec.class.getName());
    }

    @Override
    protected SecretKey engineTranslateKey(SecretKey key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (!(key instanceof AndroidKeyStoreSecretKey)) {
            throw new InvalidKeyException("To import a secret key into Android Keystore, use KeyStore.setEntry");
        }
        return key;
    }
}
