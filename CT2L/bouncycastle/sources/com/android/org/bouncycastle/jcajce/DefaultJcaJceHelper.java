package com.android.org.bouncycastle.jcajce;

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;

public class DefaultJcaJceHelper implements JcaJceHelper {
    @Override
    public Cipher createCipher(String algorithm) throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(algorithm);
    }

    @Override
    public Mac createMac(String algorithm) throws NoSuchAlgorithmException {
        return Mac.getInstance(algorithm);
    }

    @Override
    public KeyAgreement createKeyAgreement(String algorithm) throws NoSuchAlgorithmException {
        return KeyAgreement.getInstance(algorithm);
    }

    @Override
    public AlgorithmParameterGenerator createAlgorithmParameterGenerator(String algorithm) throws NoSuchAlgorithmException {
        return AlgorithmParameterGenerator.getInstance(algorithm);
    }

    @Override
    public AlgorithmParameters createAlgorithmParameters(String algorithm) throws NoSuchAlgorithmException {
        return AlgorithmParameters.getInstance(algorithm);
    }

    @Override
    public KeyGenerator createKeyGenerator(String algorithm) throws NoSuchAlgorithmException {
        return KeyGenerator.getInstance(algorithm);
    }

    @Override
    public KeyFactory createKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        return KeyFactory.getInstance(algorithm);
    }

    @Override
    public SecretKeyFactory createSecretKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        return SecretKeyFactory.getInstance(algorithm);
    }

    @Override
    public KeyPairGenerator createKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance(algorithm);
    }

    @Override
    public MessageDigest createDigest(String algorithm) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm);
    }

    @Override
    public Signature createSignature(String algorithm) throws NoSuchAlgorithmException {
        return Signature.getInstance(algorithm);
    }

    @Override
    public CertificateFactory createCertificateFactory(String algorithm) throws NoSuchAlgorithmException, CertificateException {
        return CertificateFactory.getInstance(algorithm);
    }
}
