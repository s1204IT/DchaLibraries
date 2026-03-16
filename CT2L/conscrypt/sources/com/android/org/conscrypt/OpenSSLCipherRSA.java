package com.android.org.conscrypt;

import com.android.org.conscrypt.util.EmptyArray;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public abstract class OpenSSLCipherRSA extends CipherSpi {
    private byte[] buffer;
    private int bufferOffset;
    private boolean encrypting;
    private boolean inputTooLarge;
    private OpenSSLKey key;
    private int padding;
    private boolean usingPrivateKey;

    protected OpenSSLCipherRSA(int padding) {
        this.padding = 1;
        this.padding = padding;
    }

    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        String modeUpper = mode.toUpperCase(Locale.ROOT);
        if ("NONE".equals(modeUpper) || "ECB".equals(modeUpper)) {
        } else {
            throw new NoSuchAlgorithmException("mode not supported: " + mode);
        }
    }

    @Override
    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        String paddingUpper = padding.toUpperCase(Locale.ROOT);
        if ("PKCS1PADDING".equals(paddingUpper)) {
            this.padding = 1;
        } else {
            if ("NOPADDING".equals(paddingUpper)) {
                this.padding = 3;
                return;
            }
            throw new NoSuchPaddingException("padding not supported: " + padding);
        }
    }

    @Override
    protected int engineGetBlockSize() {
        return this.encrypting ? paddedBlockSizeBytes() : keySizeBytes();
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return this.encrypting ? keySizeBytes() : paddedBlockSizeBytes();
    }

    private int paddedBlockSizeBytes() {
        int paddedBlockSizeBytes = keySizeBytes();
        if (this.padding == 1) {
            return (paddedBlockSizeBytes - 1) - 10;
        }
        return paddedBlockSizeBytes;
    }

    private int keySizeBytes() {
        if (this.key == null) {
            throw new IllegalStateException("cipher is not initialized");
        }
        return NativeCrypto.RSA_size(this.key.getPkeyContext());
    }

    @Override
    protected byte[] engineGetIV() {
        return null;
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    private void engineInitInternal(int opmode, Key key) throws InvalidKeyException {
        if (opmode == 1 || opmode == 3) {
            this.encrypting = true;
        } else if (opmode == 2 || opmode == 4) {
            this.encrypting = false;
        } else {
            throw new InvalidParameterException("Unsupported opmode " + opmode);
        }
        if (key instanceof OpenSSLRSAPrivateKey) {
            OpenSSLRSAPrivateKey rsaPrivateKey = (OpenSSLRSAPrivateKey) key;
            this.usingPrivateKey = true;
            this.key = rsaPrivateKey.getOpenSSLKey();
        } else if (key instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey rsaPrivateKey2 = (RSAPrivateCrtKey) key;
            this.usingPrivateKey = true;
            this.key = OpenSSLRSAPrivateCrtKey.getInstance(rsaPrivateKey2);
        } else if (key instanceof RSAPrivateKey) {
            RSAPrivateKey rsaPrivateKey3 = (RSAPrivateKey) key;
            this.usingPrivateKey = true;
            this.key = OpenSSLRSAPrivateKey.getInstance(rsaPrivateKey3);
        } else if (key instanceof OpenSSLRSAPublicKey) {
            OpenSSLRSAPublicKey rsaPublicKey = (OpenSSLRSAPublicKey) key;
            this.usingPrivateKey = false;
            this.key = rsaPublicKey.getOpenSSLKey();
        } else if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey2 = (RSAPublicKey) key;
            this.usingPrivateKey = false;
            this.key = OpenSSLRSAPublicKey.getInstance(rsaPublicKey2);
        } else {
            throw new InvalidKeyException("Need RSA private or public key");
        }
        this.buffer = new byte[NativeCrypto.RSA_size(this.key.getPkeyContext())];
        this.inputTooLarge = false;
    }

    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        engineInitInternal(opmode, key);
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("unknown param type: " + params.getClass().getName());
        }
        engineInitInternal(opmode, key);
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("unknown param type: " + params.getClass().getName());
        }
        engineInitInternal(opmode, key);
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        if (this.bufferOffset + inputLen > this.buffer.length) {
            this.inputTooLarge = true;
            return EmptyArray.BYTE;
        }
        System.arraycopy(input, inputOffset, this.buffer, this.bufferOffset, inputLen);
        this.bufferOffset += inputLen;
        return EmptyArray.BYTE;
    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        engineUpdate(input, inputOffset, inputLen);
        return 0;
    }

    @Override
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws BadPaddingException, IllegalBlockSizeException {
        byte[] tmpBuf;
        int resultSize;
        if (input != null) {
            engineUpdate(input, inputOffset, inputLen);
        }
        if (this.inputTooLarge) {
            throw new IllegalBlockSizeException("input must be under " + this.buffer.length + " bytes");
        }
        if (this.bufferOffset != this.buffer.length) {
            if (this.padding == 3) {
                tmpBuf = new byte[this.buffer.length];
                System.arraycopy(this.buffer, 0, tmpBuf, this.buffer.length - this.bufferOffset, this.bufferOffset);
            } else {
                tmpBuf = Arrays.copyOf(this.buffer, this.bufferOffset);
            }
        } else {
            tmpBuf = this.buffer;
        }
        byte[] output = new byte[this.buffer.length];
        if (this.encrypting) {
            if (this.usingPrivateKey) {
                resultSize = NativeCrypto.RSA_private_encrypt(tmpBuf.length, tmpBuf, output, this.key.getPkeyContext(), this.padding);
            } else {
                resultSize = NativeCrypto.RSA_public_encrypt(tmpBuf.length, tmpBuf, output, this.key.getPkeyContext(), this.padding);
            }
        } else {
            try {
                if (this.usingPrivateKey) {
                    resultSize = NativeCrypto.RSA_private_decrypt(tmpBuf.length, tmpBuf, output, this.key.getPkeyContext(), this.padding);
                } else {
                    resultSize = NativeCrypto.RSA_public_decrypt(tmpBuf.length, tmpBuf, output, this.key.getPkeyContext(), this.padding);
                }
            } catch (SignatureException e) {
                IllegalBlockSizeException newE = new IllegalBlockSizeException();
                newE.initCause(e);
                throw newE;
            }
        }
        if (!this.encrypting && resultSize != output.length) {
            output = Arrays.copyOf(output, resultSize);
        }
        this.bufferOffset = 0;
        return output;
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] b = engineDoFinal(input, inputOffset, inputLen);
        int lastOffset = outputOffset + b.length;
        if (lastOffset > output.length) {
            throw new ShortBufferException("output buffer is too small " + output.length + " < " + lastOffset);
        }
        System.arraycopy(b, 0, output, outputOffset, b.length);
        return b.length;
    }

    @Override
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        try {
            byte[] encoded = key.getEncoded();
            return engineDoFinal(encoded, 0, encoded.length);
        } catch (BadPaddingException e) {
            IllegalBlockSizeException newE = new IllegalBlockSizeException();
            newE.initCause(e);
            throw newE;
        }
    }

    @Override
    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            byte[] encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            if (wrappedKeyType == 1) {
                KeyFactory keyFactory = KeyFactory.getInstance(wrappedKeyAlgorithm);
                return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
            }
            if (wrappedKeyType == 2) {
                KeyFactory keyFactory2 = KeyFactory.getInstance(wrappedKeyAlgorithm);
                return keyFactory2.generatePrivate(new PKCS8EncodedKeySpec(encoded));
            }
            if (wrappedKeyType == 3) {
                return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
            }
            throw new UnsupportedOperationException("wrappedKeyType == " + wrappedKeyType);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException(e);
        } catch (BadPaddingException e2) {
            throw new InvalidKeyException(e2);
        } catch (IllegalBlockSizeException e3) {
            throw new InvalidKeyException(e3);
        }
    }

    public static class PKCS1 extends OpenSSLCipherRSA {
        public PKCS1() {
            super(1);
        }
    }

    public static class Raw extends OpenSSLCipherRSA {
        public Raw() {
            super(3);
        }
    }
}
