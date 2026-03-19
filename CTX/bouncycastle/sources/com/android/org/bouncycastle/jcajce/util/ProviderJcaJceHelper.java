package com.android.org.bouncycastle.jcajce.util;

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;

public class ProviderJcaJceHelper implements JcaJceHelper {
    protected final Provider provider;

    public ProviderJcaJceHelper(Provider provider) {
        this.provider = provider;
    }

    @Override
    public Cipher createCipher(String str) throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(str, this.provider);
    }

    @Override
    public Mac createMac(String str) throws NoSuchAlgorithmException {
        return Mac.getInstance(str, this.provider);
    }

    @Override
    public KeyAgreement createKeyAgreement(String str) throws NoSuchAlgorithmException {
        return KeyAgreement.getInstance(str, this.provider);
    }

    @Override
    public AlgorithmParameterGenerator createAlgorithmParameterGenerator(String str) throws NoSuchAlgorithmException {
        return AlgorithmParameterGenerator.getInstance(str, this.provider);
    }

    @Override
    public AlgorithmParameters createAlgorithmParameters(String str) throws NoSuchAlgorithmException {
        return AlgorithmParameters.getInstance(str, this.provider);
    }

    @Override
    public KeyGenerator createKeyGenerator(String str) throws NoSuchAlgorithmException {
        return KeyGenerator.getInstance(str, this.provider);
    }

    @Override
    public KeyFactory createKeyFactory(String str) throws NoSuchAlgorithmException {
        return KeyFactory.getInstance(str, this.provider);
    }

    @Override
    public SecretKeyFactory createSecretKeyFactory(String str) throws NoSuchAlgorithmException {
        return SecretKeyFactory.getInstance(str, this.provider);
    }

    @Override
    public KeyPairGenerator createKeyPairGenerator(String str) throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance(str, this.provider);
    }

    @Override
    public MessageDigest createDigest(String str) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(str, this.provider);
    }

    @Override
    public Signature createSignature(String str) throws NoSuchAlgorithmException {
        return Signature.getInstance(str, this.provider);
    }

    @Override
    public CertificateFactory createCertificateFactory(String str) throws CertificateException {
        return CertificateFactory.getInstance(str, this.provider);
    }

    @Override
    public SecureRandom createSecureRandom(String str) throws NoSuchAlgorithmException {
        return SecureRandom.getInstance(str, this.provider);
    }
}
