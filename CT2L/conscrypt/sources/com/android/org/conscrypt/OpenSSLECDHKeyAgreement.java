package com.android.org.conscrypt;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.KeyAgreementSpi;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public final class OpenSSLECDHKeyAgreement extends KeyAgreementSpi {
    private int mExpectedResultLength;
    private OpenSSLKey mOpenSslPrivateKey;
    private byte[] mResult;

    @Override
    public Key engineDoPhase(Key key, boolean lastPhase) throws InvalidKeyException {
        byte[] result;
        if (this.mOpenSslPrivateKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (!lastPhase) {
            throw new IllegalStateException("ECDH only has one phase");
        }
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (!(key instanceof PublicKey)) {
            throw new InvalidKeyException("Not a public key: " + key.getClass());
        }
        OpenSSLKey openSslPublicKey = OpenSSLKey.fromPublicKey((PublicKey) key);
        byte[] buffer = new byte[this.mExpectedResultLength];
        int actualResultLength = NativeCrypto.ECDH_compute_key(buffer, 0, openSslPublicKey.getPkeyContext(), this.mOpenSslPrivateKey.getPkeyContext());
        if (actualResultLength == -1) {
            throw new RuntimeException("Engine returned " + actualResultLength);
        }
        if (actualResultLength == this.mExpectedResultLength) {
            result = buffer;
        } else if (actualResultLength < this.mExpectedResultLength) {
            result = new byte[actualResultLength];
            System.arraycopy(buffer, 0, this.mResult, 0, this.mResult.length);
        } else {
            throw new RuntimeException("Engine produced a longer than expected result. Expected: " + this.mExpectedResultLength + ", actual: " + actualResultLength);
        }
        this.mResult = result;
        return null;
    }

    @Override
    protected int engineGenerateSecret(byte[] sharedSecret, int offset) throws ShortBufferException {
        checkCompleted();
        int available = sharedSecret.length - offset;
        if (this.mResult.length > available) {
            throw new ShortBufferException("Needed: " + this.mResult.length + ", available: " + available);
        }
        System.arraycopy(this.mResult, 0, sharedSecret, offset, this.mResult.length);
        return this.mResult.length;
    }

    @Override
    protected byte[] engineGenerateSecret() {
        checkCompleted();
        return this.mResult;
    }

    @Override
    protected SecretKey engineGenerateSecret(String algorithm) {
        checkCompleted();
        return new SecretKeySpec(engineGenerateSecret(), algorithm);
    }

    @Override
    protected void engineInit(Key key, SecureRandom random) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (!(key instanceof PrivateKey)) {
            throw new InvalidKeyException("Not a private key: " + key.getClass());
        }
        OpenSSLKey openSslKey = OpenSSLKey.fromPrivateKey((PrivateKey) key);
        int fieldSizeBits = NativeCrypto.EC_GROUP_get_degree(NativeCrypto.EC_KEY_get0_group(openSslKey.getPkeyContext()));
        this.mExpectedResultLength = (fieldSizeBits + 7) / 8;
        this.mOpenSslPrivateKey = openSslKey;
    }

    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("No algorithm parameters supported");
        }
        engineInit(key, random);
    }

    private void checkCompleted() {
        if (this.mResult == null) {
            throw new IllegalStateException("Key agreement not completed");
        }
    }
}
