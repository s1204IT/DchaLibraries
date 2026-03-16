package com.android.org.conscrypt;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public final class OpenSSLECPublicKey implements ECPublicKey, OpenSSLKeyHolder {
    private static final String ALGORITHM = "EC";
    private static final long serialVersionUID = 3215842926808298020L;
    protected transient OpenSSLECGroupContext group;
    protected transient OpenSSLKey key;

    public OpenSSLECPublicKey(OpenSSLECGroupContext group, OpenSSLKey key) {
        this.group = group;
        this.key = key;
    }

    public OpenSSLECPublicKey(OpenSSLKey key) {
        long origGroup = NativeCrypto.EC_KEY_get0_group(key.getPkeyContext());
        this.group = new OpenSSLECGroupContext(NativeCrypto.EC_GROUP_dup(origGroup));
        this.key = key;
    }

    public OpenSSLECPublicKey(ECPublicKeySpec ecKeySpec) throws InvalidKeySpecException {
        try {
            this.group = OpenSSLECGroupContext.getInstance(ecKeySpec.getParams());
            OpenSSLECPointContext pubKey = OpenSSLECPointContext.getInstance(NativeCrypto.get_EC_GROUP_type(this.group.getContext()), this.group, ecKeySpec.getW());
            this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_EC_KEY(this.group.getContext(), pubKey.getContext(), null));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    public static OpenSSLKey getInstance(ECPublicKey ecPublicKey) throws InvalidKeyException {
        try {
            OpenSSLECGroupContext group = OpenSSLECGroupContext.getInstance(ecPublicKey.getParams());
            OpenSSLECPointContext pubKey = OpenSSLECPointContext.getInstance(NativeCrypto.get_EC_GROUP_type(group.getContext()), group, ecPublicKey.getW());
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_EC_KEY(group.getContext(), pubKey.getContext(), null));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public String getAlgorithm() {
        return ALGORITHM;
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
    public ECParameterSpec getParams() {
        return this.group.getECParameterSpec();
    }

    private ECPoint getPublicKey() {
        OpenSSLECPointContext pubKey = new OpenSSLECPointContext(this.group, NativeCrypto.EC_KEY_get_public_key(this.key.getPkeyContext()));
        return pubKey.getECPoint();
    }

    @Override
    public ECPoint getW() {
        return getPublicKey();
    }

    @Override
    public OpenSSLKey getOpenSSLKey() {
        return this.key;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof OpenSSLECPrivateKey) {
            return this.key.equals(((OpenSSLECPrivateKey) o).key);
        }
        if (!(o instanceof ECPublicKey)) {
            return false;
        }
        ECPublicKey other = (ECPublicKey) o;
        if (!getPublicKey().equals(other.getW())) {
            return false;
        }
        ECParameterSpec spec = getParams();
        ECParameterSpec otherSpec = other.getParams();
        return spec.getCurve().equals(otherSpec.getCurve()) && spec.getGenerator().equals(otherSpec.getGenerator()) && spec.getOrder().equals(otherSpec.getOrder()) && spec.getCofactor() == otherSpec.getCofactor();
    }

    public int hashCode() {
        return Arrays.hashCode(NativeCrypto.i2d_PUBKEY(this.key.getPkeyContext()));
    }

    public String toString() {
        return NativeCrypto.EVP_PKEY_print_public(this.key.getPkeyContext());
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        byte[] encoded = (byte[]) stream.readObject();
        this.key = new OpenSSLKey(NativeCrypto.d2i_PUBKEY(encoded));
        long origGroup = NativeCrypto.EC_KEY_get0_group(this.key.getPkeyContext());
        this.group = new OpenSSLECGroupContext(NativeCrypto.EC_GROUP_dup(origGroup));
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (this.key.isEngineBased()) {
            throw new NotSerializableException("engine-based keys can not be serialized");
        }
        stream.defaultWriteObject();
        stream.writeObject(getEncoded());
    }
}
