package com.android.org.conscrypt;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.util.Arrays;
import javax.crypto.SecretKey;

public class OpenSSLSecretKey implements SecretKey, OpenSSLKeyHolder {
    private static final long serialVersionUID = 1831053062911514589L;
    private final String algorithm;
    private final byte[] encoded;
    private transient OpenSSLKey key;
    private final int type;

    public OpenSSLSecretKey(String algorithm, byte[] encoded) {
        this.algorithm = algorithm;
        this.encoded = encoded;
        this.type = NativeCrypto.EVP_PKEY_HMAC;
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_mac_key(this.type, encoded));
    }

    public OpenSSLSecretKey(String algorithm, OpenSSLKey key) {
        this.algorithm = algorithm;
        this.key = key;
        this.type = NativeCrypto.EVP_PKEY_type(key.getPkeyContext());
        this.encoded = null;
    }

    public static OpenSSLKey getInstance(SecretKey key) throws InvalidKeyException {
        try {
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_mac_key(NativeCrypto.EVP_PKEY_HMAC, key.getEncoded()));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String getFormat() {
        if (this.key.isEngineBased()) {
            return null;
        }
        return "RAW";
    }

    @Override
    public byte[] getEncoded() {
        if (this.key.isEngineBased()) {
            return null;
        }
        return this.encoded;
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SecretKey)) {
            return false;
        }
        SecretKey other = (SecretKey) o;
        if (!this.algorithm.equals(other.getAlgorithm())) {
            return false;
        }
        if (o instanceof OpenSSLSecretKey) {
            OpenSSLSecretKey otherOpenSSL = (OpenSSLSecretKey) o;
            return this.key.equals(otherOpenSSL.getOpenSSLKey());
        }
        if (this.key.isEngineBased() || !getFormat().equals(other.getFormat())) {
            return false;
        }
        return Arrays.equals(this.encoded, other.getEncoded());
    }

    public int hashCode() {
        return this.key.hashCode();
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_mac_key(this.type, this.encoded));
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (getOpenSSLKey().isEngineBased()) {
            throw new NotSerializableException("engine-based keys can not be serialized");
        }
        stream.defaultWriteObject();
    }
}
