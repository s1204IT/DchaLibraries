package com.android.org.conscrypt;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;

public class OpenSSLDHPrivateKey implements DHPrivateKey, OpenSSLKeyHolder {
    private static final long serialVersionUID = -7321023036951606638L;
    private transient byte[] g;
    private transient OpenSSLKey key;
    private transient Object mParamsLock = new Object();
    private transient byte[] p;
    private transient boolean readParams;
    private transient byte[] x;

    OpenSSLDHPrivateKey(OpenSSLKey key) {
        this.key = key;
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    OpenSSLDHPrivateKey(DHPrivateKeySpec dhKeySpec) throws InvalidKeySpecException {
        try {
            this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DH(dhKeySpec.getP().toByteArray(), dhKeySpec.getG().toByteArray(), null, dhKeySpec.getX().toByteArray()));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    private void ensureReadParams() {
        synchronized (this.mParamsLock) {
            if (!this.readParams) {
                byte[][] params = NativeCrypto.get_DH_params(this.key.getPkeyContext());
                this.p = params[0];
                this.g = params[1];
                this.x = params[3];
                this.readParams = true;
            }
        }
    }

    static OpenSSLKey getInstance(DHPrivateKey dhPrivateKey) throws InvalidKeyException {
        try {
            DHParameterSpec dhParams = dhPrivateKey.getParams();
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DH(dhParams.getP().toByteArray(), dhParams.getG().toByteArray(), null, dhPrivateKey.getX().toByteArray()));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public String getAlgorithm() {
        return "DH";
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
    public DHParameterSpec getParams() {
        ensureReadParams();
        return new DHParameterSpec(new BigInteger(this.p), new BigInteger(this.g));
    }

    @Override
    public BigInteger getX() {
        if (this.key.isEngineBased()) {
            throw new UnsupportedOperationException("private key value X cannot be extracted");
        }
        ensureReadParams();
        return new BigInteger(this.x);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ((o instanceof OpenSSLDHPrivateKey) && this.key.equals(((OpenSSLDHPrivateKey) o).getOpenSSLKey())) {
            return true;
        }
        if (!(o instanceof DHPrivateKey)) {
            return false;
        }
        ensureReadParams();
        DHPrivateKey other = (DHPrivateKey) o;
        if (!this.x.equals(other.getX())) {
            return false;
        }
        DHParameterSpec spec = other.getParams();
        return this.g.equals(spec.getG()) && this.p.equals(spec.getP());
    }

    public int hashCode() {
        ensureReadParams();
        int hash = 1;
        if (!this.key.isEngineBased()) {
            hash = this.x.hashCode() + 3;
        }
        return (((hash * 7) + this.p.hashCode()) * 13) + this.g.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("OpenSSLDHPrivateKey{");
        if (this.key.isEngineBased()) {
            sb.append("key=");
            sb.append(this.key);
            sb.append('}');
            return sb.toString();
        }
        ensureReadParams();
        sb.append("X=");
        sb.append(new BigInteger(this.x).toString(16));
        sb.append(',');
        sb.append("P=");
        sb.append(new BigInteger(this.p).toString(16));
        sb.append(',');
        sb.append("G=");
        sb.append(new BigInteger(this.g).toString(16));
        sb.append('}');
        return sb.toString();
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        BigInteger g = (BigInteger) stream.readObject();
        BigInteger p = (BigInteger) stream.readObject();
        BigInteger x = (BigInteger) stream.readObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DH(p.toByteArray(), g.toByteArray(), null, x.toByteArray()));
        this.mParamsLock = new Object();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (getOpenSSLKey().isEngineBased()) {
            throw new NotSerializableException("engine-based keys can not be serialized");
        }
        stream.defaultWriteObject();
        ensureReadParams();
        stream.writeObject(new BigInteger(this.g));
        stream.writeObject(new BigInteger(this.p));
        stream.writeObject(new BigInteger(this.x));
    }
}
