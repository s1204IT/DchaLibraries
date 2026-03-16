package com.android.org.conscrypt;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class OpenSSLDSAKeyFactory extends KeyFactorySpi {
    @Override
    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (keySpec instanceof DSAPublicKeySpec) {
            return new OpenSSLDSAPublicKey((DSAPublicKeySpec) keySpec);
        }
        if (keySpec instanceof X509EncodedKeySpec) {
            return OpenSSLKey.getPublicKey((X509EncodedKeySpec) keySpec, NativeCrypto.EVP_PKEY_DSA);
        }
        throw new InvalidKeySpecException("Must use DSAPublicKeySpec or X509EncodedKeySpec; was " + keySpec.getClass().getName());
    }

    @Override
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (keySpec instanceof DSAPrivateKeySpec) {
            return new OpenSSLDSAPrivateKey((DSAPrivateKeySpec) keySpec);
        }
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            return OpenSSLKey.getPrivateKey((PKCS8EncodedKeySpec) keySpec, NativeCrypto.EVP_PKEY_DSA);
        }
        throw new InvalidKeySpecException("Must use DSAPrivateKeySpec or PKCS8EncodedKeySpec; was " + keySpec.getClass().getName());
    }

    @Override
    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec) throws InvalidKeySpecException {
        if (key == null) {
            throw new InvalidKeySpecException("key == null");
        }
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (!"DSA".equals(key.getAlgorithm())) {
            throw new InvalidKeySpecException("Key must be a DSA key");
        }
        if ((key instanceof DSAPublicKey) && DSAPublicKeySpec.class.isAssignableFrom(keySpec)) {
            DSAPublicKey dsaKey = (DSAPublicKey) key;
            DSAParams params = dsaKey.getParams();
            return new DSAPublicKeySpec(dsaKey.getY(), params.getP(), params.getQ(), params.getG());
        }
        if ((key instanceof PublicKey) && DSAPublicKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded = key.getEncoded();
            if (!"X.509".equals(key.getFormat()) || encoded == null) {
                throw new InvalidKeySpecException("Not a valid X.509 encoding");
            }
            DSAPublicKey dsaKey2 = (DSAPublicKey) engineGeneratePublic(new X509EncodedKeySpec(encoded));
            DSAParams params2 = dsaKey2.getParams();
            return new DSAPublicKeySpec(dsaKey2.getY(), params2.getP(), params2.getQ(), params2.getG());
        }
        if ((key instanceof DSAPrivateKey) && DSAPrivateKeySpec.class.isAssignableFrom(keySpec)) {
            DSAPrivateKey dsaKey3 = (DSAPrivateKey) key;
            DSAParams params3 = dsaKey3.getParams();
            return new DSAPrivateKeySpec(dsaKey3.getX(), params3.getP(), params3.getQ(), params3.getG());
        }
        if ((key instanceof PrivateKey) && DSAPrivateKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded2 = key.getEncoded();
            if (!"PKCS#8".equals(key.getFormat()) || encoded2 == null) {
                throw new InvalidKeySpecException("Not a valid PKCS#8 encoding");
            }
            DSAPrivateKey dsaKey4 = (DSAPrivateKey) engineGeneratePrivate(new PKCS8EncodedKeySpec(encoded2));
            DSAParams params4 = dsaKey4.getParams();
            return new DSAPrivateKeySpec(dsaKey4.getX(), params4.getP(), params4.getQ(), params4.getG());
        }
        if ((key instanceof PrivateKey) && PKCS8EncodedKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded3 = key.getEncoded();
            if (!"PKCS#8".equals(key.getFormat())) {
                throw new InvalidKeySpecException("Encoding type must be PKCS#8; was " + key.getFormat());
            }
            if (encoded3 == null) {
                throw new InvalidKeySpecException("Key is not encodable");
            }
            return new PKCS8EncodedKeySpec(encoded3);
        }
        if ((key instanceof PublicKey) && X509EncodedKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded4 = key.getEncoded();
            if (!"X.509".equals(key.getFormat())) {
                throw new InvalidKeySpecException("Encoding type must be X.509; was " + key.getFormat());
            }
            if (encoded4 == null) {
                throw new InvalidKeySpecException("Key is not encodable");
            }
            return new X509EncodedKeySpec(encoded4);
        }
        throw new InvalidKeySpecException("Unsupported key type and key spec combination; key=" + key.getClass().getName() + ", keySpec=" + keySpec.getName());
    }

    @Override
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (!(key instanceof OpenSSLDSAPublicKey) && !(key instanceof OpenSSLDSAPrivateKey)) {
            if (key instanceof DSAPublicKey) {
                DSAPublicKey dsaKey = (DSAPublicKey) key;
                BigInteger y = dsaKey.getY();
                DSAParams params = dsaKey.getParams();
                BigInteger p = params.getP();
                BigInteger q = params.getQ();
                BigInteger g = params.getG();
                try {
                    return engineGeneratePublic(new DSAPublicKeySpec(y, p, q, g));
                } catch (InvalidKeySpecException e) {
                    throw new InvalidKeyException(e);
                }
            }
            if (key instanceof DSAPrivateKey) {
                DSAPrivateKey dsaKey2 = (DSAPrivateKey) key;
                BigInteger x = dsaKey2.getX();
                DSAParams params2 = dsaKey2.getParams();
                BigInteger p2 = params2.getP();
                BigInteger q2 = params2.getQ();
                BigInteger g2 = params2.getG();
                try {
                    return engineGeneratePrivate(new DSAPrivateKeySpec(x, p2, q2, g2));
                } catch (InvalidKeySpecException e2) {
                    throw new InvalidKeyException(e2);
                }
            }
            if ((key instanceof PrivateKey) && "PKCS#8".equals(key.getFormat())) {
                byte[] encoded = key.getEncoded();
                if (encoded == null) {
                    throw new InvalidKeyException("Key does not support encoding");
                }
                try {
                    return engineGeneratePrivate(new PKCS8EncodedKeySpec(encoded));
                } catch (InvalidKeySpecException e3) {
                    throw new InvalidKeyException(e3);
                }
            }
            if ((key instanceof PublicKey) && "X.509".equals(key.getFormat())) {
                byte[] encoded2 = key.getEncoded();
                if (encoded2 == null) {
                    throw new InvalidKeyException("Key does not support encoding");
                }
                try {
                    return engineGeneratePublic(new X509EncodedKeySpec(encoded2));
                } catch (InvalidKeySpecException e4) {
                    throw new InvalidKeyException(e4);
                }
            }
            throw new InvalidKeyException("Key must be DSA public or private key; was " + key.getClass().getName());
        }
        return key;
    }
}
