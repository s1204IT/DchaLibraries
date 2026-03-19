package com.android.org.conscrypt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

public class OpenSSLRSAPublicKey implements RSAPublicKey, OpenSSLKeyHolder {
    private static final long serialVersionUID = 123125005824688292L;
    private transient boolean fetchedParams;
    private transient OpenSSLKey key;
    private BigInteger modulus;
    private BigInteger publicExponent;

    OpenSSLRSAPublicKey(OpenSSLKey openSSLKey) {
        this.key = openSSLKey;
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    OpenSSLRSAPublicKey(RSAPublicKeySpec rSAPublicKeySpec) throws InvalidKeySpecException {
        try {
            this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(rSAPublicKeySpec.getModulus().toByteArray(), rSAPublicKeySpec.getPublicExponent().toByteArray(), null, null, null, null, null, null));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    static OpenSSLKey getInstance(RSAPublicKey rSAPublicKey) throws InvalidKeyException {
        try {
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(rSAPublicKey.getModulus().toByteArray(), rSAPublicKey.getPublicExponent().toByteArray(), null, null, null, null, null, null));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public String getAlgorithm() {
        return "RSA";
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        return NativeCrypto.EVP_marshal_public_key(this.key.getNativeRef());
    }

    private void ensureReadParams() {
        if (this.fetchedParams) {
            return;
        }
        byte[][] bArr = NativeCrypto.get_RSA_public_params(this.key.getNativeRef());
        this.modulus = new BigInteger(bArr[0]);
        this.publicExponent = new BigInteger(bArr[1]);
        this.fetchedParams = true;
    }

    @Override
    public BigInteger getModulus() {
        ensureReadParams();
        return this.modulus;
    }

    @Override
    public BigInteger getPublicExponent() {
        ensureReadParams();
        return this.publicExponent;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj instanceof OpenSSLRSAPublicKey) && this.key.equals(((OpenSSLRSAPublicKey) obj).getOpenSSLKey())) {
            return true;
        }
        if (!(obj instanceof RSAPublicKey)) {
            return false;
        }
        ensureReadParams();
        RSAPublicKey rSAPublicKey = (RSAPublicKey) obj;
        return this.modulus.equals(rSAPublicKey.getModulus()) && this.publicExponent.equals(rSAPublicKey.getPublicExponent());
    }

    public int hashCode() {
        ensureReadParams();
        return this.modulus.hashCode() ^ this.publicExponent.hashCode();
    }

    public String toString() {
        ensureReadParams();
        return "OpenSSLRSAPublicKey{modulus=" + this.modulus.toString(16) + ",publicExponent=" + this.publicExponent.toString(16) + '}';
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(this.modulus.toByteArray(), this.publicExponent.toByteArray(), null, null, null, null, null, null));
        this.fetchedParams = true;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ensureReadParams();
        objectOutputStream.defaultWriteObject();
    }
}
