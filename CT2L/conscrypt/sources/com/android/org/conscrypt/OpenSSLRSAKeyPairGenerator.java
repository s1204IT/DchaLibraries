package com.android.org.conscrypt;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

public class OpenSSLRSAKeyPairGenerator extends KeyPairGeneratorSpi {
    private byte[] publicExponent = {1, 0, 1};
    private int modulusBits = 2048;

    @Override
    public KeyPair generateKeyPair() {
        OpenSSLKey key = new OpenSSLKey(NativeCrypto.RSA_generate_key_ex(this.modulusBits, this.publicExponent));
        PrivateKey privKey = OpenSSLRSAPrivateKey.getInstance(key);
        PublicKey pubKey = new OpenSSLRSAPublicKey(key);
        return new KeyPair(pubKey, privKey);
    }

    @Override
    public void initialize(int keysize, SecureRandom random) {
        this.modulusBits = keysize;
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        if (!(params instanceof RSAKeyGenParameterSpec)) {
            throw new InvalidAlgorithmParameterException("Only RSAKeyGenParameterSpec supported");
        }
        RSAKeyGenParameterSpec spec = (RSAKeyGenParameterSpec) params;
        BigInteger publicExponent = spec.getPublicExponent();
        if (publicExponent != null) {
            this.publicExponent = publicExponent.toByteArray();
        }
        this.modulusBits = spec.getKeysize();
    }
}
