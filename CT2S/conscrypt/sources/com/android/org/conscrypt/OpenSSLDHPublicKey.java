package com.android.org.conscrypt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class OpenSSLDHPublicKey implements DHPublicKey, OpenSSLKeyHolder {
    private static final long serialVersionUID = 6123717708079837723L;
    private transient byte[] g;
    private transient OpenSSLKey key;
    private final transient Object mParamsLock = new Object();
    private transient byte[] p;
    private transient boolean readParams;
    private transient byte[] y;

    OpenSSLDHPublicKey(OpenSSLKey key) {
        this.key = key;
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    OpenSSLDHPublicKey(DHPublicKeySpec dsaKeySpec) throws InvalidKeySpecException {
        try {
            this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DH(dsaKeySpec.getP().toByteArray(), dsaKeySpec.getG().toByteArray(), dsaKeySpec.getY().toByteArray(), null));
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
                this.y = params[2];
                this.readParams = true;
            }
        }
    }

    static OpenSSLKey getInstance(DHPublicKey DHPublicKey) throws InvalidKeyException {
        try {
            DHParameterSpec dhParams = DHPublicKey.getParams();
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DH(dhParams.getP().toByteArray(), dhParams.getG().toByteArray(), DHPublicKey.getY().toByteArray(), null));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public DHParameterSpec getParams() {
        ensureReadParams();
        return new DHParameterSpec(new BigInteger(this.p), new BigInteger(this.g));
    }

    @Override
    public String getAlgorithm() {
        return "DH";
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
        return new BigInteger(this.y);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ((o instanceof OpenSSLDHPublicKey) && this.key.equals(((OpenSSLDHPublicKey) o).getOpenSSLKey())) {
            return true;
        }
        if (!(o instanceof DHPublicKey)) {
            return false;
        }
        ensureReadParams();
        DHPublicKey other = (DHPublicKey) o;
        if (!this.y.equals(other.getY())) {
            return false;
        }
        DHParameterSpec spec = other.getParams();
        return this.g.equals(spec.getG()) && this.p.equals(spec.getP());
    }

    public int hashCode() {
        ensureReadParams();
        int hash = this.y.hashCode() + 3;
        return (((hash * 7) + this.p.hashCode()) * 13) + this.g.hashCode();
    }

    public String toString() {
        ensureReadParams();
        return "OpenSSLDHPublicKey{Y=" + new BigInteger(this.y).toString(16) + ",P=" + new BigInteger(this.p).toString(16) + ",G=" + new BigInteger(this.g).toString(16) + '}';
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        BigInteger g = (BigInteger) stream.readObject();
        BigInteger p = (BigInteger) stream.readObject();
        BigInteger y = (BigInteger) stream.readObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_DH(p.toByteArray(), g.toByteArray(), y.toByteArray(), null));
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        ensureReadParams();
        stream.writeObject(new BigInteger(this.g));
        stream.writeObject(new BigInteger(this.p));
        stream.writeObject(new BigInteger(this.y));
    }
}
