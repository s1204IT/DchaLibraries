package com.android.server.locksettings.recoverablekeystore;

import com.android.internal.annotations.VisibleForTesting;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecureBox {
    private static final String CIPHER_ALG = "AES";
    private static final String EC_ALG = "EC";
    private static final int EC_COORDINATE_LEN_BYTES = 32;
    private static final String EC_P256_COMMON_NAME = "secp256r1";
    private static final String EC_P256_OPENSSL_NAME = "prime256v1";
    private static final int EC_PUBLIC_KEY_LEN_BYTES = 65;
    private static final byte EC_PUBLIC_KEY_PREFIX = 4;
    private static final String ENC_ALG = "AES/GCM/NoPadding";
    private static final int GCM_KEY_LEN_BYTES = 16;
    private static final int GCM_NONCE_LEN_BYTES = 12;
    private static final int GCM_TAG_LEN_BYTES = 16;
    private static final String KA_ALG = "ECDH";
    private static final String MAC_ALG = "HmacSHA256";
    private static final byte[] VERSION = {2, 0};
    private static final byte[] HKDF_SALT = concat("SECUREBOX".getBytes(StandardCharsets.UTF_8), VERSION);
    private static final byte[] HKDF_INFO_WITH_PUBLIC_KEY = "P256 HKDF-SHA-256 AES-128-GCM".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_INFO_WITHOUT_PUBLIC_KEY = "SHARED HKDF-SHA-256 AES-128-GCM".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CONSTANT_01 = {1};
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final BigInteger BIG_INT_02 = BigInteger.valueOf(2);
    private static final BigInteger EC_PARAM_P = new BigInteger("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16);
    private static final BigInteger EC_PARAM_A = EC_PARAM_P.subtract(new BigInteger("3"));
    private static final BigInteger EC_PARAM_B = new BigInteger("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16);

    @VisibleForTesting
    static final ECParameterSpec EC_PARAM_SPEC = new ECParameterSpec(new EllipticCurve(new ECFieldFp(EC_PARAM_P), EC_PARAM_A, EC_PARAM_B), new ECPoint(new BigInteger("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", 16), new BigInteger("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", 16)), new BigInteger("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16), 1);

    private enum AesGcmOperation {
        ENCRYPT,
        DECRYPT
    }

    private SecureBox() {
    }

    public static KeyPair genKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(EC_ALG);
        try {
            keyPairGenerator.initialize(new ECGenParameterSpec(EC_P256_OPENSSL_NAME));
            return keyPairGenerator.generateKeyPair();
        } catch (InvalidAlgorithmParameterException e) {
            try {
                keyPairGenerator.initialize(new ECGenParameterSpec(EC_P256_COMMON_NAME));
                return keyPairGenerator.generateKeyPair();
            } catch (InvalidAlgorithmParameterException e2) {
                throw new NoSuchAlgorithmException("Unable to find the NIST P-256 curve", e2);
            }
        }
    }

    public static byte[] encrypt(PublicKey publicKey, byte[] bArr, byte[] bArr2, byte[] bArr3) throws NoSuchAlgorithmException, InvalidKeyException {
        KeyPair keyPairGenKeyPair;
        byte[] bArrDhComputeSecret;
        byte[] bArr4;
        byte[] bArrEmptyByteArrayIfNull = emptyByteArrayIfNull(bArr);
        if (publicKey == null && bArrEmptyByteArrayIfNull.length == 0) {
            throw new IllegalArgumentException("Both the public key and shared secret are empty");
        }
        byte[] bArrEmptyByteArrayIfNull2 = emptyByteArrayIfNull(bArr2);
        byte[] bArrEmptyByteArrayIfNull3 = emptyByteArrayIfNull(bArr3);
        if (publicKey == null) {
            byte[] bArr5 = EMPTY_BYTE_ARRAY;
            bArr4 = HKDF_INFO_WITHOUT_PUBLIC_KEY;
            keyPairGenKeyPair = null;
            bArrDhComputeSecret = bArr5;
        } else {
            keyPairGenKeyPair = genKeyPair();
            bArrDhComputeSecret = dhComputeSecret(keyPairGenKeyPair.getPrivate(), publicKey);
            bArr4 = HKDF_INFO_WITH_PUBLIC_KEY;
        }
        byte[] bArrGenRandomNonce = genRandomNonce();
        byte[] bArrAesGcmEncrypt = aesGcmEncrypt(hkdfDeriveKey(concat(bArrDhComputeSecret, bArrEmptyByteArrayIfNull), HKDF_SALT, bArr4), bArrGenRandomNonce, bArrEmptyByteArrayIfNull3, bArrEmptyByteArrayIfNull2);
        return keyPairGenKeyPair == null ? concat(VERSION, bArrGenRandomNonce, bArrAesGcmEncrypt) : concat(VERSION, encodePublicKey(keyPairGenKeyPair.getPublic()), bArrGenRandomNonce, bArrAesGcmEncrypt);
    }

    public static byte[] decrypt(PrivateKey privateKey, byte[] bArr, byte[] bArr2, byte[] bArr3) throws NoSuchAlgorithmException, InvalidKeyException, AEADBadTagException {
        byte[] bArrDhComputeSecret;
        byte[] bArr4;
        byte[] bArrEmptyByteArrayIfNull = emptyByteArrayIfNull(bArr);
        if (privateKey == null && bArrEmptyByteArrayIfNull.length == 0) {
            throw new IllegalArgumentException("Both the private key and shared secret are empty");
        }
        byte[] bArrEmptyByteArrayIfNull2 = emptyByteArrayIfNull(bArr2);
        if (bArr3 == null) {
            throw new NullPointerException("Encrypted payload must not be null.");
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr3);
        if (!Arrays.equals(readEncryptedPayload(byteBufferWrap, VERSION.length), VERSION)) {
            throw new AEADBadTagException("The payload was not encrypted by SecureBox v2");
        }
        if (privateKey == null) {
            bArrDhComputeSecret = EMPTY_BYTE_ARRAY;
            bArr4 = HKDF_INFO_WITHOUT_PUBLIC_KEY;
        } else {
            bArrDhComputeSecret = dhComputeSecret(privateKey, decodePublicKey(readEncryptedPayload(byteBufferWrap, 65)));
            bArr4 = HKDF_INFO_WITH_PUBLIC_KEY;
        }
        return aesGcmDecrypt(hkdfDeriveKey(concat(bArrDhComputeSecret, bArrEmptyByteArrayIfNull), HKDF_SALT, bArr4), readEncryptedPayload(byteBufferWrap, 12), readEncryptedPayload(byteBufferWrap, byteBufferWrap.remaining()), bArrEmptyByteArrayIfNull2);
    }

    private static byte[] readEncryptedPayload(ByteBuffer byteBuffer, int i) throws AEADBadTagException {
        byte[] bArr = new byte[i];
        try {
            byteBuffer.get(bArr);
            return bArr;
        } catch (BufferUnderflowException e) {
            throw new AEADBadTagException("The encrypted payload is too short");
        }
    }

    private static byte[] dhComputeSecret(PrivateKey privateKey, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(KA_ALG);
        try {
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            return keyAgreement.generateSecret();
        } catch (RuntimeException e) {
            throw new InvalidKeyException(e);
        }
    }

    private static SecretKey hkdfDeriveKey(byte[] bArr, byte[] bArr2, byte[] bArr3) throws NoSuchAlgorithmException {
        Mac mac = Mac.getInstance(MAC_ALG);
        try {
            mac.init(new SecretKeySpec(bArr2, MAC_ALG));
            try {
                mac.init(new SecretKeySpec(mac.doFinal(bArr), MAC_ALG));
                mac.update(bArr3);
                return new SecretKeySpec(Arrays.copyOf(mac.doFinal(CONSTANT_01), 16), CIPHER_ALG);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        } catch (InvalidKeyException e2) {
            throw new RuntimeException(e2);
        }
    }

    private static byte[] aesGcmEncrypt(SecretKey secretKey, byte[] bArr, byte[] bArr2, byte[] bArr3) throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            return aesGcmInternal(AesGcmOperation.ENCRYPT, secretKey, bArr, bArr2, bArr3);
        } catch (AEADBadTagException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] aesGcmDecrypt(SecretKey secretKey, byte[] bArr, byte[] bArr2, byte[] bArr3) throws NoSuchAlgorithmException, InvalidKeyException, AEADBadTagException {
        return aesGcmInternal(AesGcmOperation.DECRYPT, secretKey, bArr, bArr2, bArr3);
    }

    private static byte[] aesGcmInternal(AesGcmOperation aesGcmOperation, SecretKey secretKey, byte[] bArr, byte[] bArr2, byte[] bArr3) throws NoSuchAlgorithmException, InvalidKeyException, AEADBadTagException {
        try {
            Cipher cipher = Cipher.getInstance(ENC_ALG);
            GCMParameterSpec gCMParameterSpec = new GCMParameterSpec(128, bArr);
            try {
                if (aesGcmOperation == AesGcmOperation.DECRYPT) {
                    cipher.init(2, secretKey, gCMParameterSpec);
                } else {
                    cipher.init(1, secretKey, gCMParameterSpec);
                }
                try {
                    cipher.updateAAD(bArr3);
                    return cipher.doFinal(bArr2);
                } catch (AEADBadTagException e) {
                    throw e;
                } catch (BadPaddingException | IllegalBlockSizeException e2) {
                    throw new RuntimeException(e2);
                }
            } catch (InvalidAlgorithmParameterException e3) {
                throw new RuntimeException(e3);
            }
        } catch (NoSuchPaddingException e4) {
            throw new RuntimeException(e4);
        }
    }

    static byte[] encodePublicKey(PublicKey publicKey) {
        ECPoint w = ((ECPublicKey) publicKey).getW();
        byte[] byteArray = w.getAffineX().toByteArray();
        byte[] byteArray2 = w.getAffineY().toByteArray();
        byte[] bArr = new byte[65];
        System.arraycopy(byteArray2, 0, bArr, 65 - byteArray2.length, byteArray2.length);
        System.arraycopy(byteArray, 0, bArr, 33 - byteArray.length, byteArray.length);
        bArr[0] = 4;
        return bArr;
    }

    @VisibleForTesting
    static PublicKey decodePublicKey(byte[] bArr) throws NoSuchAlgorithmException, InvalidKeyException {
        BigInteger bigInteger = new BigInteger(1, Arrays.copyOfRange(bArr, 1, 33));
        BigInteger bigInteger2 = new BigInteger(1, Arrays.copyOfRange(bArr, 33, 65));
        validateEcPoint(bigInteger, bigInteger2);
        try {
            return KeyFactory.getInstance(EC_ALG).generatePublic(new ECPublicKeySpec(new ECPoint(bigInteger, bigInteger2), EC_PARAM_SPEC));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private static void validateEcPoint(BigInteger bigInteger, BigInteger bigInteger2) throws InvalidKeyException {
        if (bigInteger.compareTo(EC_PARAM_P) >= 0 || bigInteger2.compareTo(EC_PARAM_P) >= 0 || bigInteger.signum() == -1 || bigInteger2.signum() == -1) {
            throw new InvalidKeyException("Point lies outside of the expected curve");
        }
        if (!bigInteger2.modPow(BIG_INT_02, EC_PARAM_P).equals(bigInteger.modPow(BIG_INT_02, EC_PARAM_P).add(EC_PARAM_A).mod(EC_PARAM_P).multiply(bigInteger).add(EC_PARAM_B).mod(EC_PARAM_P))) {
            throw new InvalidKeyException("Point lies outside of the expected curve");
        }
    }

    private static byte[] genRandomNonce() throws NoSuchAlgorithmException {
        byte[] bArr = new byte[12];
        new SecureRandom().nextBytes(bArr);
        return bArr;
    }

    @VisibleForTesting
    static byte[] concat(byte[]... bArr) {
        int length = 0;
        for (int i = 0; i < bArr.length; i++) {
            if (bArr[i] == null) {
                bArr[i] = EMPTY_BYTE_ARRAY;
            }
            length += bArr[i].length;
        }
        byte[] bArr2 = new byte[length];
        int length2 = 0;
        for (byte[] bArr3 : bArr) {
            System.arraycopy(bArr3, 0, bArr2, length2, bArr3.length);
            length2 += bArr3.length;
        }
        return bArr2;
    }

    private static byte[] emptyByteArrayIfNull(byte[] bArr) {
        return bArr == null ? EMPTY_BYTE_ARRAY : bArr;
    }
}
