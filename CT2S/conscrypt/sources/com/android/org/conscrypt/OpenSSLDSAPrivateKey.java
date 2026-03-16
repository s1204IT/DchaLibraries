package com.android.org.conscrypt;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;

public class OpenSSLDSAPrivateKey implements DSAPrivateKey, OpenSSLKeyHolder {
    private static final long serialVersionUID = 6524734576187424628L;
    private transient OpenSSLKey key;
    private transient OpenSSLDSAParams params;

    OpenSSLDSAPrivateKey(OpenSSLKey key) {
        this.key = key;
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    OpenSSLDSAPrivateKey(DSAPrivateKeySpec dsaKeySpec) throws InvalidKeySpecException {
        try {
            this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DSA(dsaKeySpec.getP().toByteArray(), dsaKeySpec.getQ().toByteArray(), dsaKeySpec.getG().toByteArray(), null, dsaKeySpec.getX().toByteArray()));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    private void ensureReadParams() {
        if (this.params == null) {
            this.params = new OpenSSLDSAParams(this.key);
        }
    }

    static OpenSSLKey getInstance(DSAPrivateKey dsaPrivateKey) throws InvalidKeyException {
        if (dsaPrivateKey.getFormat() == null) {
            return wrapPlatformKey(dsaPrivateKey);
        }
        try {
            DSAParams dsaParams = dsaPrivateKey.getParams();
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DSA(dsaParams.getP().toByteArray(), dsaParams.getQ().toByteArray(), dsaParams.getG().toByteArray(), null, dsaPrivateKey.getX().toByteArray()));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    public static OpenSSLKey wrapPlatformKey(DSAPrivateKey dsaPrivateKey) {
        return new OpenSSLKey(NativeCrypto.getDSAPrivateKeyWrapper(dsaPrivateKey), true);
    }

    @Override
    public DSAParams getParams() {
        ensureReadParams();
        return this.params;
    }

    @Override
    public String getAlgorithm() {
        return "DSA";
    }

    @Override
    public String getFormat() {
        if (this.key.isEngineBased()) {
            return null;
        }
        return "PKCS#8";
    }

    @Override
    public byte[] getEncoded() {
        if (this.key.isEngineBased()) {
            return null;
        }
        return NativeCrypto.i2d_PKCS8_PRIV_KEY_INFO(this.key.getPkeyContext());
    }

    @Override
    public BigInteger getX() {
        if (this.key.isEngineBased()) {
            throw new UnsupportedOperationException("private key value X cannot be extracted");
        }
        ensureReadParams();
        return this.params.getX();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ((o instanceof OpenSSLDSAPrivateKey) && this.key.equals(((OpenSSLDSAPrivateKey) o).getOpenSSLKey())) {
            return true;
        }
        if (!(o instanceof DSAPrivateKey)) {
            return false;
        }
        ensureReadParams();
        BigInteger x = this.params.getX();
        if (x == null) {
            return false;
        }
        DSAPrivateKey other = (DSAPrivateKey) o;
        return x.equals(other.getX()) && this.params.equals(other.getParams());
    }

    public int hashCode() {
        ensureReadParams();
        int hash = 1;
        BigInteger x = getX();
        if (x != null) {
            hash = x.hashCode() + 3;
        }
        return (hash * 7) + this.params.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("OpenSSLDSAPrivateKey{");
        if (this.key.isEngineBased()) {
            sb.append("key=");
            sb.append(this.key);
            sb.append('}');
            return sb.toString();
        }
        ensureReadParams();
        sb.append("X=");
        sb.append(this.params.getX().toString(16));
        sb.append(',');
        sb.append("params=");
        sb.append(this.params.toString());
        sb.append('}');
        return sb.toString();
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        BigInteger g = (BigInteger) stream.readObject();
        BigInteger p = (BigInteger) stream.readObject();
        BigInteger q = (BigInteger) stream.readObject();
        BigInteger x = (BigInteger) stream.readObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DSA(p.toByteArray(), q.toByteArray(), g.toByteArray(), null, x.toByteArray()));
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (getOpenSSLKey().isEngineBased()) {
            throw new NotSerializableException("engine-based keys can not be serialized");
        }
        stream.defaultWriteObject();
        ensureReadParams();
        stream.writeObject(this.params.getG());
        stream.writeObject(this.params.getP());
        stream.writeObject(this.params.getQ());
        stream.writeObject(this.params.getX());
    }
}
