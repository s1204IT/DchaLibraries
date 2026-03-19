package android.security.keystore;

import android.security.KeyStore;
import android.security.keymaster.KeymasterDefs;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import libcore.util.EmptyArray;

abstract class KeyStoreCryptoOperationUtils {
    private static volatile SecureRandom sRng;

    private KeyStoreCryptoOperationUtils() {
    }

    static InvalidKeyException getInvalidKeyExceptionForInit(KeyStore keyStore, AndroidKeyStoreKey key, int beginOpResultCode) {
        if (beginOpResultCode == 1) {
            return null;
        }
        InvalidKeyException e = keyStore.getInvalidKeyException(key.getAlias(), key.getUid(), beginOpResultCode);
        switch (beginOpResultCode) {
            case 15:
                if (e instanceof UserNotAuthenticatedException) {
                    return null;
                }
            default:
                return e;
        }
    }

    public static GeneralSecurityException getExceptionForCipherInit(KeyStore keyStore, AndroidKeyStoreKey key, int beginOpResultCode) {
        if (beginOpResultCode == 1) {
            return null;
        }
        switch (beginOpResultCode) {
            case KeymasterDefs.KM_ERROR_CALLER_NONCE_PROHIBITED:
                return new InvalidAlgorithmParameterException("Caller-provided IV not permitted");
            case KeymasterDefs.KM_ERROR_KEY_RATE_LIMIT_EXCEEDED:
            case KeymasterDefs.KM_ERROR_MISSING_MAC_LENGTH:
            default:
                return getInvalidKeyExceptionForInit(keyStore, key, beginOpResultCode);
            case KeymasterDefs.KM_ERROR_INVALID_NONCE:
                return new InvalidAlgorithmParameterException("Invalid IV");
        }
    }

    static byte[] getRandomBytesToMixIntoKeystoreRng(SecureRandom rng, int sizeBytes) {
        if (sizeBytes <= 0) {
            return EmptyArray.BYTE;
        }
        if (rng == null) {
            rng = getRng();
        }
        byte[] result = new byte[sizeBytes];
        rng.nextBytes(result);
        return result;
    }

    private static SecureRandom getRng() {
        if (sRng == null) {
            sRng = new SecureRandom();
        }
        return sRng;
    }
}
