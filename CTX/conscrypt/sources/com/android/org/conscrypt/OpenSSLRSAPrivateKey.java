package com.android.org.conscrypt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;

class OpenSSLRSAPrivateKey implements RSAPrivateKey, OpenSSLKeyHolder {
    private static final long serialVersionUID = 4872170254439578735L;
    transient boolean fetchedParams;
    transient OpenSSLKey key;
    BigInteger modulus;
    BigInteger privateExponent;

    OpenSSLRSAPrivateKey(OpenSSLKey openSSLKey) {
        this.key = openSSLKey;
    }

    OpenSSLRSAPrivateKey(OpenSSLKey openSSLKey, byte[][] bArr) {
        this(openSSLKey);
        readParams(bArr);
        this.fetchedParams = true;
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    public OpenSSLRSAPrivateKey(RSAPrivateKeySpec rSAPrivateKeySpec) throws InvalidKeySpecException {
        this(init(rSAPrivateKeySpec));
    }

    private static OpenSSLKey init(RSAPrivateKeySpec rSAPrivateKeySpec) throws InvalidKeySpecException {
        BigInteger modulus = rSAPrivateKeySpec.getModulus();
        BigInteger privateExponent = rSAPrivateKeySpec.getPrivateExponent();
        if (modulus == null) {
            throw new InvalidKeySpecException("modulus == null");
        }
        if (privateExponent == null) {
            throw new InvalidKeySpecException("privateExponent == null");
        }
        try {
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(modulus.toByteArray(), null, privateExponent.toByteArray(), null, null, null, null, null));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    static OpenSSLRSAPrivateKey getInstance(OpenSSLKey openSSLKey) {
        byte[][] bArr = NativeCrypto.get_RSA_private_params(openSSLKey.getNativeRef());
        if (bArr[1] != null) {
            return new OpenSSLRSAPrivateCrtKey(openSSLKey, bArr);
        }
        return new OpenSSLRSAPrivateKey(openSSLKey, bArr);
    }

    static OpenSSLKey wrapPlatformKey(RSAPrivateKey rSAPrivateKey) throws InvalidKeyException {
        OpenSSLKey openSSLKeyWrapRsaKey = Platform.wrapRsaKey(rSAPrivateKey);
        if (openSSLKeyWrapRsaKey != null) {
            return openSSLKeyWrapRsaKey;
        }
        return new OpenSSLKey(NativeCrypto.getRSAPrivateKeyWrapper(rSAPrivateKey, rSAPrivateKey.getModulus().toByteArray()), true);
    }

    static OpenSSLKey wrapJCAPrivateKeyForTLSStackOnly(PrivateKey privateKey, PublicKey publicKey) throws InvalidKeyException {
        BigInteger modulus;
        if (privateKey instanceof RSAKey) {
            modulus = ((RSAKey) privateKey).getModulus();
        } else if (publicKey instanceof RSAKey) {
            modulus = ((RSAKey) publicKey).getModulus();
        } else {
            modulus = null;
        }
        if (modulus == null) {
            throw new InvalidKeyException("RSA modulus not available. Private: " + privateKey + ", public: " + publicKey);
        }
        return new OpenSSLKey(NativeCrypto.getRSAPrivateKeyWrapper(privateKey, modulus.toByteArray()), true);
    }

    static OpenSSLKey getInstance(RSAPrivateKey rSAPrivateKey) throws InvalidKeyException {
        if (rSAPrivateKey.getFormat() == null) {
            return wrapPlatformKey(rSAPrivateKey);
        }
        BigInteger modulus = rSAPrivateKey.getModulus();
        BigInteger privateExponent = rSAPrivateKey.getPrivateExponent();
        if (modulus == null) {
            throw new InvalidKeyException("modulus == null");
        }
        if (privateExponent == null) {
            throw new InvalidKeyException("privateExponent == null");
        }
        try {
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(modulus.toByteArray(), null, privateExponent.toByteArray(), null, null, null, null, null));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    final synchronized void ensureReadParams() {
        if (this.fetchedParams) {
            return;
        }
        readParams(NativeCrypto.get_RSA_private_params(this.key.getNativeRef()));
        this.fetchedParams = true;
    }

    void readParams(byte[][] bArr) {
        if (bArr[0] == null) {
            throw new NullPointerException("modulus == null");
        }
        if (bArr[2] == null) {
            throw new NullPointerException("privateExponent == null");
        }
        this.modulus = new BigInteger(bArr[0]);
        if (bArr[2] != null) {
            this.privateExponent = new BigInteger(bArr[2]);
        }
    }

    @Override
    public final BigInteger getPrivateExponent() {
        ensureReadParams();
        return this.privateExponent;
    }

    @Override
    public final BigInteger getModulus() {
        ensureReadParams();
        return this.modulus;
    }

    @Override
    public final byte[] getEncoded() {
        return NativeCrypto.EVP_marshal_private_key(this.key.getNativeRef());
    }

    @Override
    public final String getFormat() {
        return "PKCS#8";
    }

    @Override
    public final String getAlgorithm() {
        return "RSA";
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof OpenSSLRSAPrivateKey) {
            return this.key.equals(((OpenSSLRSAPrivateKey) obj).getOpenSSLKey());
        }
        if (!(obj instanceof RSAPrivateKey)) {
            return false;
        }
        ensureReadParams();
        RSAPrivateKey rSAPrivateKey = (RSAPrivateKey) obj;
        return this.modulus.equals(rSAPrivateKey.getModulus()) && this.privateExponent.equals(rSAPrivateKey.getPrivateExponent());
    }

    public int hashCode() {
        ensureReadParams();
        int iHashCode = 3 + this.modulus.hashCode();
        if (this.privateExponent != null) {
            return (iHashCode * 7) + this.privateExponent.hashCode();
        }
        return iHashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("OpenSSLRSAPrivateKey{");
        ensureReadParams();
        sb.append("modulus=");
        sb.append(this.modulus.toString(16));
        return sb.toString();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(this.modulus.toByteArray(), null, this.privateExponent.toByteArray(), null, null, null, null, null));
        this.fetchedParams = true;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ensureReadParams();
        objectOutputStream.defaultWriteObject();
    }
}
