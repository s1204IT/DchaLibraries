package com.android.org.conscrypt;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.DHParameterSpec;

public class OpenSSLDHKeyPairGenerator extends KeyPairGeneratorSpi {
    private static final BigInteger DEFAULT_GENERATOR = BigInteger.valueOf(2);
    private BigInteger prime;
    private int primeBits = 1024;
    private BigInteger generator = DEFAULT_GENERATOR;

    @Override
    public KeyPair generateKeyPair() {
        OpenSSLKey key;
        if (this.prime != null) {
            key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DH(this.prime.toByteArray(), this.generator.toByteArray(), null, null));
        } else {
            key = new OpenSSLKey(NativeCrypto.DH_generate_parameters_ex(this.primeBits, this.generator.longValue()));
        }
        NativeCrypto.DH_generate_key(key.getPkeyContext());
        OpenSSLDHPrivateKey privKey = new OpenSSLDHPrivateKey(key);
        OpenSSLDHPublicKey pubKey = new OpenSSLDHPublicKey(key);
        return new KeyPair(pubKey, privKey);
    }

    @Override
    public void initialize(int keysize, SecureRandom random) {
        this.prime = null;
        this.primeBits = keysize;
        this.generator = DEFAULT_GENERATOR;
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        this.prime = null;
        this.primeBits = 1024;
        this.generator = DEFAULT_GENERATOR;
        if (params instanceof DHParameterSpec) {
            DHParameterSpec dhParams = (DHParameterSpec) params;
            this.prime = dhParams.getP();
            BigInteger gen = dhParams.getG();
            if (gen != null) {
                this.generator = gen;
                return;
            }
            return;
        }
        if (params != null) {
            throw new InvalidAlgorithmParameterException("Params must be DHParameterSpec");
        }
    }
}
