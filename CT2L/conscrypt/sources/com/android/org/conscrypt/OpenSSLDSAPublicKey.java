package com.android.org.conscrypt;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

public class OpenSSLDSAPublicKey implements DSAPublicKey, OpenSSLKeyHolder {
    private static final long serialVersionUID = 5238609500353792232L;
    private transient OpenSSLKey key;
    private transient OpenSSLDSAParams params;

    OpenSSLDSAPublicKey(OpenSSLKey key) {
        this.key = key;
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    OpenSSLDSAPublicKey(DSAPublicKeySpec dsaKeySpec) throws InvalidKeySpecException {
        try {
            this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DSA(dsaKeySpec.getP().toByteArray(), dsaKeySpec.getQ().toByteArray(), dsaKeySpec.getG().toByteArray(), dsaKeySpec.getY().toByteArray(), null));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    private void ensureReadParams() {
        if (this.params == null) {
            this.params = new OpenSSLDSAParams(this.key);
        }
    }

    static OpenSSLKey getInstance(DSAPublicKey dsaPublicKey) throws InvalidKeyException {
        try {
            DSAParams dsaParams = dsaPublicKey.getParams();
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DSA(dsaParams.getP().toByteArray(), dsaParams.getQ().toByteArray(), dsaParams.getG().toByteArray(), dsaPublicKey.getY().toByteArray(), null));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public DSAParams getParams() {
        ensureReadParams();
        if (this.params.hasParams()) {
            return this.params;
        }
        return null;
    }

    @Override
    public String getAlgorithm() {
        return "DSA";
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        return NativeCrypto.i2d_PUBKEY(this.key.getPkeyContext());
    }

    @Override
    public BigInteger getY() {
        ensureReadParams();
        return this.params.getY();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ((o instanceof OpenSSLDSAPublicKey) && this.key.equals(((OpenSSLDSAPublicKey) o).getOpenSSLKey())) {
            return true;
        }
        if (!(o instanceof DSAPublicKey)) {
            return false;
        }
        ensureReadParams();
        DSAPublicKey other = (DSAPublicKey) o;
        return this.params.getY().equals(other.getY()) && this.params.equals(other.getParams());
    }

    public int hashCode() {
        ensureReadParams();
        return this.params.getY().hashCode() ^ this.params.hashCode();
    }

    public String toString() {
        ensureReadParams();
        return "OpenSSLDSAPublicKey{Y=" + this.params.getY().toString(16) + ",params=" + this.params.toString() + '}';
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        BigInteger g = (BigInteger) stream.readObject();
        BigInteger p = (BigInteger) stream.readObject();
        BigInteger q = (BigInteger) stream.readObject();
        BigInteger y = (BigInteger) stream.readObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DSA(p.toByteArray(), q.toByteArray(), g.toByteArray(), y.toByteArray(), null));
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
        stream.writeObject(this.params.getY());
    }
}
