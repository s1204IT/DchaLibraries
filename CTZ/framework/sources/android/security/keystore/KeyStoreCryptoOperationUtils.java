package android.security.keystore;

import android.security.KeyStore;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import libcore.util.EmptyArray;

abstract class KeyStoreCryptoOperationUtils {
    private static volatile SecureRandom sRng;

    private KeyStoreCryptoOperationUtils() {
    }

    static InvalidKeyException getInvalidKeyExceptionForInit(KeyStore keyStore, AndroidKeyStoreKey androidKeyStoreKey, int i) {
        if (i == 1) {
            return null;
        }
        InvalidKeyException invalidKeyException = keyStore.getInvalidKeyException(androidKeyStoreKey.getAlias(), androidKeyStoreKey.getUid(), i);
        if (i == 15 && (invalidKeyException instanceof UserNotAuthenticatedException)) {
            return null;
        }
        return invalidKeyException;
    }

    public static GeneralSecurityException getExceptionForCipherInit(KeyStore keyStore, AndroidKeyStoreKey androidKeyStoreKey, int i) {
        if (i == 1) {
            return null;
        }
        if (i == -55) {
            return new InvalidAlgorithmParameterException("Caller-provided IV not permitted");
        }
        if (i == -52) {
            return new InvalidAlgorithmParameterException("Invalid IV");
        }
        return getInvalidKeyExceptionForInit(keyStore, androidKeyStoreKey, i);
    }

    static byte[] getRandomBytesToMixIntoKeystoreRng(SecureRandom secureRandom, int i) {
        if (i <= 0) {
            return EmptyArray.BYTE;
        }
        if (secureRandom == null) {
            secureRandom = getRng();
        }
        byte[] bArr = new byte[i];
        secureRandom.nextBytes(bArr);
        return bArr;
    }

    private static SecureRandom getRng() {
        if (sRng == null) {
            sRng = new SecureRandom();
        }
        return sRng;
    }
}
