package com.android.org.conscrypt;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class OpenSSLRSAKeyFactory extends KeyFactorySpi {
    @Override
    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (keySpec instanceof RSAPublicKeySpec) {
            return new OpenSSLRSAPublicKey((RSAPublicKeySpec) keySpec);
        }
        if (keySpec instanceof X509EncodedKeySpec) {
            return OpenSSLKey.getPublicKey((X509EncodedKeySpec) keySpec, 6);
        }
        throw new InvalidKeySpecException("Must use RSAPublicKeySpec or X509EncodedKeySpec; was " + keySpec.getClass().getName());
    }

    @Override
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (keySpec instanceof RSAPrivateCrtKeySpec) {
            return new OpenSSLRSAPrivateCrtKey((RSAPrivateCrtKeySpec) keySpec);
        }
        if (keySpec instanceof RSAPrivateKeySpec) {
            return new OpenSSLRSAPrivateKey((RSAPrivateKeySpec) keySpec);
        }
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            return OpenSSLKey.getPrivateKey((PKCS8EncodedKeySpec) keySpec, 6);
        }
        throw new InvalidKeySpecException("Must use RSAPublicKeySpec or PKCS8EncodedKeySpec; was " + keySpec.getClass().getName());
    }

    @Override
    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec) throws InvalidKeySpecException {
        if (key == null) {
            throw new InvalidKeySpecException("key == null");
        }
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (!"RSA".equals(key.getAlgorithm())) {
            throw new InvalidKeySpecException("Key must be a RSA key");
        }
        if ((key instanceof RSAPublicKey) && RSAPublicKeySpec.class.isAssignableFrom(keySpec)) {
            RSAPublicKey rsaKey = (RSAPublicKey) key;
            return new RSAPublicKeySpec(rsaKey.getModulus(), rsaKey.getPublicExponent());
        }
        if ((key instanceof PublicKey) && RSAPublicKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded = key.getEncoded();
            if (!"X.509".equals(key.getFormat()) || encoded == null) {
                throw new InvalidKeySpecException("Not a valid X.509 encoding");
            }
            RSAPublicKey rsaKey2 = (RSAPublicKey) engineGeneratePublic(new X509EncodedKeySpec(encoded));
            return new RSAPublicKeySpec(rsaKey2.getModulus(), rsaKey2.getPublicExponent());
        }
        if ((key instanceof RSAPrivateCrtKey) && RSAPrivateCrtKeySpec.class.isAssignableFrom(keySpec)) {
            RSAPrivateCrtKey rsaKey3 = (RSAPrivateCrtKey) key;
            return new RSAPrivateCrtKeySpec(rsaKey3.getModulus(), rsaKey3.getPublicExponent(), rsaKey3.getPrivateExponent(), rsaKey3.getPrimeP(), rsaKey3.getPrimeQ(), rsaKey3.getPrimeExponentP(), rsaKey3.getPrimeExponentQ(), rsaKey3.getCrtCoefficient());
        }
        if ((key instanceof RSAPrivateCrtKey) && RSAPrivateKeySpec.class.isAssignableFrom(keySpec)) {
            RSAPrivateCrtKey rsaKey4 = (RSAPrivateCrtKey) key;
            return new RSAPrivateKeySpec(rsaKey4.getModulus(), rsaKey4.getPrivateExponent());
        }
        if ((key instanceof RSAPrivateKey) && RSAPrivateKeySpec.class.isAssignableFrom(keySpec)) {
            RSAPrivateKey rsaKey5 = (RSAPrivateKey) key;
            return new RSAPrivateKeySpec(rsaKey5.getModulus(), rsaKey5.getPrivateExponent());
        }
        if ((key instanceof PrivateKey) && RSAPrivateCrtKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded2 = key.getEncoded();
            if (!"PKCS#8".equals(key.getFormat()) || encoded2 == null) {
                throw new InvalidKeySpecException("Not a valid PKCS#8 encoding");
            }
            RSAPrivateKey privKey = (RSAPrivateKey) engineGeneratePrivate(new PKCS8EncodedKeySpec(encoded2));
            if (!(privKey instanceof RSAPrivateCrtKey)) {
                throw new InvalidKeySpecException("Encoded key is not an RSAPrivateCrtKey");
            }
            RSAPrivateCrtKey rsaKey6 = (RSAPrivateCrtKey) privKey;
            return new RSAPrivateCrtKeySpec(rsaKey6.getModulus(), rsaKey6.getPublicExponent(), rsaKey6.getPrivateExponent(), rsaKey6.getPrimeP(), rsaKey6.getPrimeQ(), rsaKey6.getPrimeExponentP(), rsaKey6.getPrimeExponentQ(), rsaKey6.getCrtCoefficient());
        }
        if ((key instanceof PrivateKey) && RSAPrivateKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded3 = key.getEncoded();
            if (!"PKCS#8".equals(key.getFormat()) || encoded3 == null) {
                throw new InvalidKeySpecException("Not a valid PKCS#8 encoding");
            }
            RSAPrivateKey rsaKey7 = (RSAPrivateKey) engineGeneratePrivate(new PKCS8EncodedKeySpec(encoded3));
            return new RSAPrivateKeySpec(rsaKey7.getModulus(), rsaKey7.getPrivateExponent());
        }
        if ((key instanceof PrivateKey) && PKCS8EncodedKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded4 = key.getEncoded();
            if (!"PKCS#8".equals(key.getFormat())) {
                throw new InvalidKeySpecException("Encoding type must be PKCS#8; was " + key.getFormat());
            }
            if (encoded4 == null) {
                throw new InvalidKeySpecException("Key is not encodable");
            }
            return new PKCS8EncodedKeySpec(encoded4);
        }
        if (!(key instanceof PublicKey) || !X509EncodedKeySpec.class.isAssignableFrom(keySpec)) {
            throw new InvalidKeySpecException("Unsupported key type and key spec combination; key=" + key.getClass().getName() + ", keySpec=" + keySpec.getName());
        }
        byte[] encoded5 = key.getEncoded();
        if (!"X.509".equals(key.getFormat())) {
            throw new InvalidKeySpecException("Encoding type must be X.509; was " + key.getFormat());
        }
        if (encoded5 == null) {
            throw new InvalidKeySpecException("Key is not encodable");
        }
        return new X509EncodedKeySpec(encoded5);
    }

    @Override
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if ((key instanceof OpenSSLRSAPublicKey) || (key instanceof OpenSSLRSAPrivateKey)) {
            return key;
        }
        if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) key;
            try {
                return engineGeneratePublic(new RSAPublicKeySpec(rsaKey.getModulus(), rsaKey.getPublicExponent()));
            } catch (InvalidKeySpecException e) {
                throw new InvalidKeyException(e);
            }
        }
        if (key instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey rsaKey2 = (RSAPrivateCrtKey) key;
            BigInteger modulus = rsaKey2.getModulus();
            BigInteger publicExponent = rsaKey2.getPublicExponent();
            BigInteger privateExponent = rsaKey2.getPrivateExponent();
            BigInteger primeP = rsaKey2.getPrimeP();
            BigInteger primeQ = rsaKey2.getPrimeQ();
            BigInteger primeExponentP = rsaKey2.getPrimeExponentP();
            BigInteger primeExponentQ = rsaKey2.getPrimeExponentQ();
            BigInteger crtCoefficient = rsaKey2.getCrtCoefficient();
            try {
                return engineGeneratePrivate(new RSAPrivateCrtKeySpec(modulus, publicExponent, privateExponent, primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient));
            } catch (InvalidKeySpecException e2) {
                throw new InvalidKeyException(e2);
            }
        }
        if (key instanceof RSAPrivateKey) {
            RSAPrivateKey rsaKey3 = (RSAPrivateKey) key;
            BigInteger modulus2 = rsaKey3.getModulus();
            BigInteger privateExponent2 = rsaKey3.getPrivateExponent();
            try {
                return engineGeneratePrivate(new RSAPrivateKeySpec(modulus2, privateExponent2));
            } catch (InvalidKeySpecException e3) {
                throw new InvalidKeyException(e3);
            }
        }
        if ((key instanceof PrivateKey) && "PKCS#8".equals(key.getFormat())) {
            byte[] encoded = key.getEncoded();
            if (encoded == null) {
                throw new InvalidKeyException("Key does not support encoding");
            }
            try {
                return engineGeneratePrivate(new PKCS8EncodedKeySpec(encoded));
            } catch (InvalidKeySpecException e4) {
                throw new InvalidKeyException(e4);
            }
        }
        if ((key instanceof PublicKey) && "X.509".equals(key.getFormat())) {
            byte[] encoded2 = key.getEncoded();
            if (encoded2 == null) {
                throw new InvalidKeyException("Key does not support encoding");
            }
            try {
                return engineGeneratePublic(new X509EncodedKeySpec(encoded2));
            } catch (InvalidKeySpecException e5) {
                throw new InvalidKeyException(e5);
            }
        }
        throw new InvalidKeyException("Key must be an RSA public or private key; was " + key.getClass().getName());
    }
}
