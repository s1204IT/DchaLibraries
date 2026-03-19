package com.android.server.locksettings;

import android.security.keystore.KeyProtection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SyntheticPasswordCrypto {
    private static final int AES_KEY_LENGTH = 32;
    private static final byte[] APPLICATION_ID_PERSONALIZATION = "application-id".getBytes();
    private static final int PROFILE_KEY_IV_SIZE = 12;
    private static final int USER_AUTHENTICATION_VALIDITY = 15;

    private static byte[] decrypt(SecretKey secretKey, byte[] bArr) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        if (bArr == null) {
            return null;
        }
        byte[] bArrCopyOfRange = Arrays.copyOfRange(bArr, 0, 12);
        byte[] bArrCopyOfRange2 = Arrays.copyOfRange(bArr, 12, bArr.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, secretKey, new GCMParameterSpec(128, bArrCopyOfRange));
        return cipher.doFinal(bArrCopyOfRange2);
    }

    private static byte[] encrypt(SecretKey secretKey, byte[] bArr) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        if (bArr == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(1, secretKey);
        byte[] bArrDoFinal = cipher.doFinal(bArr);
        byte[] iv = cipher.getIV();
        if (iv.length != 12) {
            throw new RuntimeException("Invalid iv length: " + iv.length);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(iv);
        byteArrayOutputStream.write(bArrDoFinal);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] encrypt(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        try {
            return encrypt(new SecretKeySpec(Arrays.copyOf(personalisedHash(bArr2, bArr), 32), "AES"), bArr3);
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        try {
            return decrypt(new SecretKeySpec(Arrays.copyOf(personalisedHash(bArr2, bArr), 32), "AES"), bArr3);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decryptBlobV1(String str, byte[] bArr, byte[] bArr2) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return decrypt((SecretKey) keyStore.getKey(str, null), decrypt(bArr2, APPLICATION_ID_PERSONALIZATION, bArr));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to decrypt blob", e);
        }
    }

    public static byte[] decryptBlob(String str, byte[] bArr, byte[] bArr2) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return decrypt(bArr2, APPLICATION_ID_PERSONALIZATION, decrypt((SecretKey) keyStore.getKey(str, null), bArr));
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to decrypt blob", e);
        }
    }

    public static byte[] createBlob(String str, byte[] bArr, byte[] bArr2, long j) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            SecretKey secretKeyGenerateKey = keyGenerator.generateKey();
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyProtection.Builder criticalToDeviceEncryption = new KeyProtection.Builder(2).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setCriticalToDeviceEncryption(true);
            if (j != 0) {
                criticalToDeviceEncryption.setUserAuthenticationRequired(true).setBoundToSpecificSecureUserId(j).setUserAuthenticationValidityDurationSeconds(15);
            }
            keyStore.setEntry(str, new KeyStore.SecretKeyEntry(secretKeyGenerateKey), criticalToDeviceEncryption.build());
            return encrypt(secretKeyGenerateKey, encrypt(bArr2, APPLICATION_ID_PERSONALIZATION, bArr));
        } catch (IOException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to encrypt blob", e);
        }
    }

    public static void destroyBlobKey(String str) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry(str);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
    }

    protected static byte[] personalisedHash(byte[] bArr, byte[]... bArr2) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            if (bArr.length > 128) {
                throw new RuntimeException("Personalisation too long");
            }
            messageDigest.update(Arrays.copyOf(bArr, 128));
            for (byte[] bArr3 : bArr2) {
                messageDigest.update(bArr3);
            }
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException for SHA-512", e);
        }
    }
}
