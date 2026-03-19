package com.android.server.locksettings.recoverablekeystore;

import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.AEADBadTagException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class KeySyncUtils {
    private static final int KEY_CLAIMANT_LENGTH_BYTES = 16;
    private static final String PUBLIC_KEY_FACTORY_ALGORITHM = "EC";
    private static final String RECOVERY_KEY_ALGORITHM = "AES";
    private static final int RECOVERY_KEY_SIZE_BITS = 256;
    private static final byte[] THM_ENCRYPTED_RECOVERY_KEY_HEADER = "V1 THM_encrypted_recovery_key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LOCALLY_ENCRYPTED_RECOVERY_KEY_HEADER = "V1 locally_encrypted_recovery_key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ENCRYPTED_APPLICATION_KEY_HEADER = "V1 encrypted_application_key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] RECOVERY_CLAIM_HEADER = "V1 KF_claim".getBytes(StandardCharsets.UTF_8);
    private static final byte[] RECOVERY_RESPONSE_HEADER = "V1 reencrypted_recovery_key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] THM_KF_HASH_PREFIX = "THM_KF_hash".getBytes(StandardCharsets.UTF_8);

    public static byte[] thmEncryptRecoveryKey(PublicKey publicKey, byte[] bArr, byte[] bArr2, SecretKey secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        return SecureBox.encrypt(publicKey, calculateThmKfHash(bArr), concat(THM_ENCRYPTED_RECOVERY_KEY_HEADER, bArr2), locallyEncryptRecoveryKey(bArr, secretKey));
    }

    public static byte[] calculateThmKfHash(byte[] bArr) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(THM_KF_HASH_PREFIX);
        messageDigest.update(bArr);
        return messageDigest.digest();
    }

    @VisibleForTesting
    static byte[] locallyEncryptRecoveryKey(byte[] bArr, SecretKey secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        return SecureBox.encrypt(null, bArr, LOCALLY_ENCRYPTED_RECOVERY_KEY_HEADER, secretKey.getEncoded());
    }

    public static SecretKey generateRecoveryKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(RECOVERY_KEY_ALGORITHM);
        keyGenerator.init(256, new SecureRandom());
        return keyGenerator.generateKey();
    }

    public static Map<String, byte[]> encryptKeysWithRecoveryKey(SecretKey secretKey, Map<String, SecretKey> map) throws NoSuchAlgorithmException, InvalidKeyException {
        HashMap map2 = new HashMap();
        for (String str : map.keySet()) {
            map2.put(str, SecureBox.encrypt(null, secretKey.getEncoded(), ENCRYPTED_APPLICATION_KEY_HEADER, map.get(str).getEncoded()));
        }
        return map2;
    }

    public static byte[] generateKeyClaimant() {
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        return bArr;
    }

    public static byte[] encryptRecoveryClaim(PublicKey publicKey, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) throws NoSuchAlgorithmException, InvalidKeyException {
        return SecureBox.encrypt(publicKey, null, concat(RECOVERY_CLAIM_HEADER, bArr, bArr2), concat(bArr3, bArr4));
    }

    public static byte[] decryptRecoveryClaimResponse(byte[] bArr, byte[] bArr2, byte[] bArr3) throws NoSuchAlgorithmException, InvalidKeyException, AEADBadTagException {
        return SecureBox.decrypt(null, bArr, concat(RECOVERY_RESPONSE_HEADER, bArr2), bArr3);
    }

    public static byte[] decryptRecoveryKey(byte[] bArr, byte[] bArr2) throws NoSuchAlgorithmException, InvalidKeyException, AEADBadTagException {
        return SecureBox.decrypt(null, bArr, LOCALLY_ENCRYPTED_RECOVERY_KEY_HEADER, bArr2);
    }

    public static byte[] decryptApplicationKey(byte[] bArr, byte[] bArr2) throws NoSuchAlgorithmException, InvalidKeyException, AEADBadTagException {
        return SecureBox.decrypt(null, bArr, ENCRYPTED_APPLICATION_KEY_HEADER, bArr2);
    }

    public static PublicKey deserializePublicKey(byte[] bArr) throws InvalidKeySpecException {
        try {
            return KeyFactory.getInstance(PUBLIC_KEY_FACTORY_ALGORITHM).generatePublic(new X509EncodedKeySpec(bArr));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] packVaultParams(PublicKey publicKey, long j, int i, byte[] bArr) {
        return ByteBuffer.allocate(77 + bArr.length).order(ByteOrder.LITTLE_ENDIAN).put(SecureBox.encodePublicKey(publicKey)).putLong(j).putInt(i).put(bArr).array();
    }

    @VisibleForTesting
    static byte[] concat(byte[]... bArr) {
        int length = 0;
        for (byte[] bArr2 : bArr) {
            length += bArr2.length;
        }
        byte[] bArr3 = new byte[length];
        int length2 = 0;
        for (byte[] bArr4 : bArr) {
            System.arraycopy(bArr4, 0, bArr3, length2, bArr4.length);
            length2 += bArr4.length;
        }
        return bArr3;
    }

    private KeySyncUtils() {
    }
}
