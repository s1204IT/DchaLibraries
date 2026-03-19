package com.android.org.conscrypt;

import com.android.org.conscrypt.OpenSSLCipher;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

public class OpenSSLCipherChaCha20 extends OpenSSLCipher {
    static final boolean $assertionsDisabled = false;
    private static final int BLOCK_SIZE_BYTES = 64;
    private static final int NONCE_SIZE_BYTES = 12;
    private int currentBlockConsumedBytes = 0;
    private int blockCounter = 0;

    @Override
    void engineInitInternal(byte[] bArr, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (algorithmParameterSpec instanceof IvParameterSpec) {
            IvParameterSpec ivParameterSpec = (IvParameterSpec) algorithmParameterSpec;
            if (ivParameterSpec.getIV().length != 12) {
                throw new InvalidAlgorithmParameterException("IV must be 12 bytes long");
            }
            this.iv = ivParameterSpec.getIV();
            return;
        }
        if (!isEncrypting()) {
            throw new InvalidAlgorithmParameterException("IV must be specified when decrypting");
        }
        this.iv = new byte[12];
        if (secureRandom != null) {
            secureRandom.nextBytes(this.iv);
        } else {
            NativeCrypto.RAND_bytes(this.iv);
        }
    }

    @Override
    int updateInternal(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4) throws ShortBufferException {
        byte[] bArr3;
        byte[] bArr4;
        int i5;
        int i6;
        int i7 = i3;
        if (this.currentBlockConsumedBytes > 0) {
            int iMin = Math.min(64 - this.currentBlockConsumedBytes, i2);
            byte[] bArr5 = new byte[BLOCK_SIZE_BYTES];
            byte[] bArr6 = new byte[BLOCK_SIZE_BYTES];
            bArr3 = bArr;
            System.arraycopy(bArr3, i, bArr5, this.currentBlockConsumedBytes, iMin);
            NativeCrypto.chacha20_encrypt_decrypt(bArr5, 0, bArr6, 0, BLOCK_SIZE_BYTES, this.encodedKey, this.iv, this.blockCounter);
            bArr4 = bArr2;
            System.arraycopy(bArr6, this.currentBlockConsumedBytes, bArr4, i7, iMin);
            this.currentBlockConsumedBytes += iMin;
            if (this.currentBlockConsumedBytes < BLOCK_SIZE_BYTES) {
                return iMin;
            }
            this.currentBlockConsumedBytes = 0;
            int i8 = i + iMin;
            i7 += iMin;
            i6 = i2 - iMin;
            this.blockCounter++;
            i5 = i8;
        } else {
            bArr3 = bArr;
            bArr4 = bArr2;
            i5 = i;
            i6 = i2;
        }
        NativeCrypto.chacha20_encrypt_decrypt(bArr3, i5, bArr4, i7, i6, this.encodedKey, this.iv, this.blockCounter);
        this.currentBlockConsumedBytes = i6 % BLOCK_SIZE_BYTES;
        this.blockCounter += i6 / BLOCK_SIZE_BYTES;
        return i2;
    }

    @Override
    int doFinalInternal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        reset();
        return 0;
    }

    private void reset() {
        this.blockCounter = 0;
        this.currentBlockConsumedBytes = 0;
    }

    @Override
    String getBaseCipherName() {
        return "ChaCha20";
    }

    @Override
    void checkSupportedKeySize(int i) throws InvalidKeyException {
        if (i != 32) {
            throw new InvalidKeyException("Unsupported key size: " + i + " bytes (must be 32)");
        }
    }

    @Override
    void checkSupportedMode(OpenSSLCipher.Mode mode) throws NoSuchAlgorithmException {
        if (mode != OpenSSLCipher.Mode.NONE) {
            throw new NoSuchAlgorithmException("Mode must be NONE");
        }
    }

    @Override
    void checkSupportedPadding(OpenSSLCipher.Padding padding) throws NoSuchPaddingException {
        if (padding != OpenSSLCipher.Padding.NOPADDING) {
            throw new NoSuchPaddingException("Must be NoPadding");
        }
    }

    @Override
    int getCipherBlockSize() {
        return 0;
    }

    @Override
    int getOutputSizeForFinal(int i) {
        return i;
    }

    @Override
    int getOutputSizeForUpdate(int i) {
        return i;
    }
}
