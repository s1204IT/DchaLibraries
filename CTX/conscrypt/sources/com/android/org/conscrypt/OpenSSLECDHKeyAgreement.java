package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
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
    public Key engineDoPhase(Key key, boolean z) throws InvalidKeyException {
        byte[] bArr;
        if (this.mOpenSslPrivateKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (!z) {
            throw new IllegalStateException("ECDH only has one phase");
        }
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (!(key instanceof PublicKey)) {
            throw new InvalidKeyException("Not a public key: " + key.getClass());
        }
        OpenSSLKey openSSLKeyFromPublicKey = OpenSSLKey.fromPublicKey((PublicKey) key);
        byte[] bArr2 = new byte[this.mExpectedResultLength];
        int iECDH_compute_key = NativeCrypto.ECDH_compute_key(bArr2, 0, openSSLKeyFromPublicKey.getNativeRef(), this.mOpenSslPrivateKey.getNativeRef());
        if (iECDH_compute_key == -1) {
            throw new RuntimeException("Engine returned " + iECDH_compute_key);
        }
        if (iECDH_compute_key != this.mExpectedResultLength) {
            if (iECDH_compute_key < this.mExpectedResultLength) {
                bArr = new byte[iECDH_compute_key];
                System.arraycopy(bArr2, 0, this.mResult, 0, this.mResult.length);
            } else {
                throw new RuntimeException("Engine produced a longer than expected result. Expected: " + this.mExpectedResultLength + ", actual: " + iECDH_compute_key);
            }
        } else {
            bArr = bArr2;
        }
        this.mResult = bArr;
        return null;
    }

    @Override
    protected int engineGenerateSecret(byte[] bArr, int i) throws ShortBufferException {
        checkCompleted();
        int length = bArr.length - i;
        if (this.mResult.length > length) {
            throw new ShortBufferException("Needed: " + this.mResult.length + ", available: " + length);
        }
        System.arraycopy(this.mResult, 0, bArr, i, this.mResult.length);
        return this.mResult.length;
    }

    @Override
    protected byte[] engineGenerateSecret() {
        checkCompleted();
        return this.mResult;
    }

    @Override
    protected SecretKey engineGenerateSecret(String str) {
        checkCompleted();
        return new SecretKeySpec(engineGenerateSecret(), str);
    }

    @Override
    protected void engineInit(Key key, SecureRandom secureRandom) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (!(key instanceof PrivateKey)) {
            throw new InvalidKeyException("Not a private key: " + key.getClass());
        }
        OpenSSLKey openSSLKeyFromPrivateKey = OpenSSLKey.fromPrivateKey((PrivateKey) key);
        this.mExpectedResultLength = (NativeCrypto.EC_GROUP_get_degree(new NativeRef.EC_GROUP(NativeCrypto.EC_KEY_get1_group(openSSLKeyFromPrivateKey.getNativeRef()))) + 7) / 8;
        this.mOpenSslPrivateKey = openSSLKeyFromPrivateKey;
    }

    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (algorithmParameterSpec != null) {
            throw new InvalidAlgorithmParameterException("No algorithm parameters supported");
        }
        engineInit(key, secureRandom);
    }

    private void checkCompleted() {
        if (this.mResult == null) {
            throw new IllegalStateException("Key agreement not completed");
        }
    }
}
