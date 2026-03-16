package com.android.org.bouncycastle.jcajce.provider.asymmetric.dh;

import com.android.org.bouncycastle.crypto.params.DESParameters;
import com.android.org.bouncycastle.util.Integers;
import com.android.org.bouncycastle.util.Strings;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyAgreementSpi extends javax.crypto.KeyAgreementSpi {
    private static final Hashtable algorithms = new Hashtable();
    private BigInteger g;
    private BigInteger p;
    private BigInteger result;
    private BigInteger x;

    static {
        Integer i64 = Integers.valueOf(64);
        Integer i192 = Integers.valueOf(192);
        Integer i128 = Integers.valueOf(128);
        Integer i256 = Integers.valueOf(256);
        algorithms.put("DES", i64);
        algorithms.put("DESEDE", i192);
        algorithms.put("BLOWFISH", i128);
        algorithms.put("AES", i256);
    }

    private byte[] bigIntToBytes(BigInteger r) {
        int expectedLength = (this.p.bitLength() + 7) / 8;
        byte[] tmp = r.toByteArray();
        if (tmp.length == expectedLength) {
            return tmp;
        }
        if (tmp[0] == 0 && tmp.length == expectedLength + 1) {
            byte[] rv = new byte[tmp.length - 1];
            System.arraycopy(tmp, 1, rv, 0, rv.length);
            return rv;
        }
        byte[] rv2 = new byte[expectedLength];
        System.arraycopy(tmp, 0, rv2, rv2.length - tmp.length, tmp.length);
        return rv2;
    }

    @Override
    protected Key engineDoPhase(Key key, boolean lastPhase) throws IllegalStateException, InvalidKeyException {
        if (this.x == null) {
            throw new IllegalStateException("Diffie-Hellman not initialised.");
        }
        if (!(key instanceof DHPublicKey)) {
            throw new InvalidKeyException("DHKeyAgreement doPhase requires DHPublicKey");
        }
        DHPublicKey pubKey = (DHPublicKey) key;
        if (!pubKey.getParams().getG().equals(this.g) || !pubKey.getParams().getP().equals(this.p)) {
            throw new InvalidKeyException("DHPublicKey not for this KeyAgreement!");
        }
        if (lastPhase) {
            this.result = ((DHPublicKey) key).getY().modPow(this.x, this.p);
            return null;
        }
        this.result = ((DHPublicKey) key).getY().modPow(this.x, this.p);
        return new BCDHPublicKey(this.result, pubKey.getParams());
    }

    @Override
    protected byte[] engineGenerateSecret() throws IllegalStateException {
        if (this.x == null) {
            throw new IllegalStateException("Diffie-Hellman not initialised.");
        }
        return bigIntToBytes(this.result);
    }

    @Override
    protected int engineGenerateSecret(byte[] sharedSecret, int offset) throws IllegalStateException, ShortBufferException {
        if (this.x == null) {
            throw new IllegalStateException("Diffie-Hellman not initialised.");
        }
        byte[] secret = bigIntToBytes(this.result);
        if (sharedSecret.length - offset < secret.length) {
            throw new ShortBufferException("DHKeyAgreement - buffer too short");
        }
        System.arraycopy(secret, 0, sharedSecret, offset, secret.length);
        return secret.length;
    }

    @Override
    protected SecretKey engineGenerateSecret(String algorithm) {
        if (this.x == null) {
            throw new IllegalStateException("Diffie-Hellman not initialised.");
        }
        String algKey = Strings.toUpperCase(algorithm);
        byte[] res = bigIntToBytes(this.result);
        if (!algorithms.containsKey(algKey)) {
            return new SecretKeySpec(res, algorithm);
        }
        Integer length = (Integer) algorithms.get(algKey);
        byte[] key = new byte[length.intValue() / 8];
        System.arraycopy(res, 0, key, 0, key.length);
        if (algKey.startsWith("DES")) {
            DESParameters.setOddParity(key);
        }
        return new SecretKeySpec(key, algorithm);
    }

    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (!(key instanceof DHPrivateKey)) {
            throw new InvalidKeyException("DHKeyAgreement requires DHPrivateKey for initialisation");
        }
        DHPrivateKey privKey = (DHPrivateKey) key;
        if (params != null) {
            if (!(params instanceof DHParameterSpec)) {
                throw new InvalidAlgorithmParameterException("DHKeyAgreement only accepts DHParameterSpec");
            }
            DHParameterSpec p = (DHParameterSpec) params;
            this.p = p.getP();
            this.g = p.getG();
        } else {
            this.p = privKey.getParams().getP();
            this.g = privKey.getParams().getG();
        }
        BigInteger x = privKey.getX();
        this.result = x;
        this.x = x;
    }

    @Override
    protected void engineInit(Key key, SecureRandom random) throws InvalidKeyException {
        if (!(key instanceof DHPrivateKey)) {
            throw new InvalidKeyException("DHKeyAgreement requires DHPrivateKey");
        }
        DHPrivateKey privKey = (DHPrivateKey) key;
        this.p = privKey.getParams().getP();
        this.g = privKey.getParams().getG();
        BigInteger x = privKey.getX();
        this.result = x;
        this.x = x;
    }
}
