package com.android.org.conscrypt;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class OpenSSLSignatureRawRSA extends SignatureSpi {
    private byte[] inputBuffer;
    private boolean inputIsTooLong;
    private int inputOffset;
    private OpenSSLKey key;

    @Override
    protected void engineUpdate(byte input) {
        int oldOffset = this.inputOffset;
        this.inputOffset = oldOffset + 1;
        if (this.inputOffset > this.inputBuffer.length) {
            this.inputIsTooLong = true;
        } else {
            this.inputBuffer[oldOffset] = input;
        }
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        int oldOffset = this.inputOffset;
        this.inputOffset += len;
        if (this.inputOffset > this.inputBuffer.length) {
            this.inputIsTooLong = true;
        } else {
            System.arraycopy(input, offset, this.inputBuffer, oldOffset, len);
        }
    }

    @Override
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (privateKey instanceof OpenSSLRSAPrivateKey) {
            OpenSSLRSAPrivateKey rsaPrivateKey = (OpenSSLRSAPrivateKey) privateKey;
            this.key = rsaPrivateKey.getOpenSSLKey();
        } else if (privateKey instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey rsaPrivateKey2 = (RSAPrivateCrtKey) privateKey;
            this.key = OpenSSLRSAPrivateCrtKey.getInstance(rsaPrivateKey2);
        } else if (privateKey instanceof RSAPrivateKey) {
            RSAPrivateKey rsaPrivateKey3 = (RSAPrivateKey) privateKey;
            this.key = OpenSSLRSAPrivateKey.getInstance(rsaPrivateKey3);
        } else {
            throw new InvalidKeyException("Need RSA private key");
        }
        int maxSize = NativeCrypto.RSA_size(this.key.getPkeyContext());
        this.inputBuffer = new byte[maxSize];
        this.inputOffset = 0;
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        if (publicKey instanceof OpenSSLRSAPublicKey) {
            OpenSSLRSAPublicKey rsaPublicKey = (OpenSSLRSAPublicKey) publicKey;
            this.key = rsaPublicKey.getOpenSSLKey();
        } else if (publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey2 = (RSAPublicKey) publicKey;
            this.key = OpenSSLRSAPublicKey.getInstance(rsaPublicKey2);
        } else {
            throw new InvalidKeyException("Need RSA public key");
        }
        int maxSize = NativeCrypto.RSA_size(this.key.getPkeyContext());
        this.inputBuffer = new byte[maxSize];
        this.inputOffset = 0;
    }

    @Override
    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        if (this.key == null) {
            throw new SignatureException("Need RSA private key");
        }
        if (this.inputIsTooLong) {
            throw new SignatureException("input length " + this.inputOffset + " != " + this.inputBuffer.length + " (modulus size)");
        }
        byte[] outputBuffer = new byte[this.inputBuffer.length];
        try {
            try {
                NativeCrypto.RSA_private_encrypt(this.inputOffset, this.inputBuffer, outputBuffer, this.key.getPkeyContext(), 1);
                return outputBuffer;
            } catch (Exception ex) {
                throw new SignatureException(ex);
            }
        } finally {
            this.inputOffset = 0;
        }
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (this.key == null) {
            throw new SignatureException("Need RSA public key");
        }
        if (this.inputIsTooLong) {
            return false;
        }
        byte[] outputBuffer = new byte[this.inputBuffer.length];
        try {
            try {
                try {
                    int resultSize = NativeCrypto.RSA_public_decrypt(sigBytes.length, sigBytes, outputBuffer, this.key.getPkeyContext(), 1);
                    boolean matches = resultSize == this.inputOffset;
                    for (int i = 0; i < resultSize; i++) {
                        if (this.inputBuffer[i] != outputBuffer[i]) {
                            matches = false;
                        }
                    }
                    this.inputOffset = 0;
                    return matches;
                } finally {
                    this.inputOffset = 0;
                }
            } catch (SignatureException e) {
                throw e;
            } catch (Exception e2) {
                return false;
            }
        } catch (Exception ex) {
            throw new SignatureException(ex);
        }
    }
}
