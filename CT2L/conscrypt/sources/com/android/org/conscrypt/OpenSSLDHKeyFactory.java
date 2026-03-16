package com.android.org.conscrypt;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;

public class OpenSSLDHKeyFactory extends KeyFactorySpi {
    @Override
    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (keySpec instanceof DHPublicKeySpec) {
            return new OpenSSLDHPublicKey((DHPublicKeySpec) keySpec);
        }
        if (keySpec instanceof X509EncodedKeySpec) {
            return OpenSSLKey.getPublicKey((X509EncodedKeySpec) keySpec, 28);
        }
        throw new InvalidKeySpecException("Must use DHPublicKeySpec or X509EncodedKeySpec; was " + keySpec.getClass().getName());
    }

    @Override
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (keySpec instanceof DHPrivateKeySpec) {
            return new OpenSSLDHPrivateKey((DHPrivateKeySpec) keySpec);
        }
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            return OpenSSLKey.getPrivateKey((PKCS8EncodedKeySpec) keySpec, 28);
        }
        throw new InvalidKeySpecException("Must use DHPrivateKeySpec or PKCS8EncodedKeySpec; was " + keySpec.getClass().getName());
    }

    @Override
    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec) throws InvalidKeySpecException {
        if (key == null) {
            throw new InvalidKeySpecException("key == null");
        }
        if (keySpec == null) {
            throw new InvalidKeySpecException("keySpec == null");
        }
        if (!"DH".equals(key.getAlgorithm())) {
            throw new InvalidKeySpecException("Key must be a DH key");
        }
        if ((key instanceof DHPublicKey) && DHPublicKeySpec.class.isAssignableFrom(keySpec)) {
            DHPublicKey dhKey = (DHPublicKey) key;
            DHParameterSpec params = dhKey.getParams();
            return new DHPublicKeySpec(dhKey.getY(), params.getP(), params.getG());
        }
        if ((key instanceof PublicKey) && DHPublicKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded = key.getEncoded();
            if (!"X.509".equals(key.getFormat()) || encoded == null) {
                throw new InvalidKeySpecException("Not a valid X.509 encoding");
            }
            DHPublicKey dhKey2 = (DHPublicKey) engineGeneratePublic(new X509EncodedKeySpec(encoded));
            DHParameterSpec params2 = dhKey2.getParams();
            return new DHPublicKeySpec(dhKey2.getY(), params2.getP(), params2.getG());
        }
        if ((key instanceof DHPrivateKey) && DHPrivateKeySpec.class.isAssignableFrom(keySpec)) {
            DHPrivateKey dhKey3 = (DHPrivateKey) key;
            DHParameterSpec params3 = dhKey3.getParams();
            return new DHPrivateKeySpec(dhKey3.getX(), params3.getP(), params3.getG());
        }
        if ((key instanceof PrivateKey) && DHPrivateKeySpec.class.isAssignableFrom(keySpec)) {
            byte[] encoded2 = key.getEncoded();
            if (!"PKCS#8".equals(key.getFormat()) || encoded2 == null) {
                throw new InvalidKeySpecException("Not a valid PKCS#8 encoding");
            }
            DHPrivateKey dhKey4 = (DHPrivateKey) engineGeneratePrivate(new PKCS8EncodedKeySpec(encoded2));
            DHParameterSpec params4 = dhKey4.getParams();
            return new DHPrivateKeySpec(dhKey4.getX(), params4.getP(), params4.getG());
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
        if (!(key instanceof OpenSSLDHPublicKey) && !(key instanceof OpenSSLDHPrivateKey)) {
            if (key instanceof DHPublicKey) {
                DHPublicKey dhKey = (DHPublicKey) key;
                BigInteger y = dhKey.getY();
                DHParameterSpec params = dhKey.getParams();
                BigInteger p = params.getP();
                BigInteger g = params.getG();
                try {
                    return engineGeneratePublic(new DHPublicKeySpec(y, p, g));
                } catch (InvalidKeySpecException e) {
                    throw new InvalidKeyException(e);
                }
            }
            if (key instanceof DHPrivateKey) {
                DHPrivateKey dhKey2 = (DHPrivateKey) key;
                BigInteger x = dhKey2.getX();
                DHParameterSpec params2 = dhKey2.getParams();
                BigInteger p2 = params2.getP();
                BigInteger g2 = params2.getG();
                try {
                    return engineGeneratePrivate(new DHPrivateKeySpec(x, p2, g2));
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
            throw new InvalidKeyException("Key must be DH public or private key; was " + key.getClass().getName());
        }
        return key;
    }
}
