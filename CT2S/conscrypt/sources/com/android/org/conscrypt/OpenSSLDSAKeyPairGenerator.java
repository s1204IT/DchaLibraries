package com.android.org.conscrypt;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;

public class OpenSSLDSAKeyPairGenerator extends KeyPairGeneratorSpi {
    private byte[] g;
    private byte[] p;
    private byte[] q;
    private int primeBits = 1024;
    private SecureRandom random = null;

    @Override
    public KeyPair generateKeyPair() {
        byte[] seed;
        if (this.random == null) {
            seed = null;
        } else {
            seed = new byte[20];
            this.random.nextBytes(seed);
        }
        OpenSSLKey key = new OpenSSLKey(NativeCrypto.DSA_generate_key(this.primeBits, seed, this.g, this.p, this.q));
        OpenSSLDSAPrivateKey privKey = new OpenSSLDSAPrivateKey(key);
        OpenSSLDSAPublicKey pubKey = new OpenSSLDSAPublicKey(key);
        return new KeyPair(pubKey, privKey);
    }

    @Override
    public void initialize(int keysize, SecureRandom random) {
        this.primeBits = keysize;
        this.random = random;
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        this.random = random;
        if (params instanceof DSAParameterSpec) {
            DSAParameterSpec dsaParams = (DSAParameterSpec) params;
            BigInteger gInt = dsaParams.getG();
            if (gInt != null) {
                this.g = gInt.toByteArray();
            }
            BigInteger pInt = dsaParams.getP();
            if (pInt != null) {
                this.p = pInt.toByteArray();
            }
            BigInteger qInt = dsaParams.getQ();
            if (qInt != null) {
                this.q = qInt.toByteArray();
                return;
            }
            return;
        }
        if (params != null) {
            throw new InvalidAlgorithmParameterException("Params must be DSAParameterSpec");
        }
    }
}
