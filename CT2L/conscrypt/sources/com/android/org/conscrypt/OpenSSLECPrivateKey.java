package com.android.org.conscrypt;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public final class OpenSSLECPrivateKey implements ECPrivateKey, OpenSSLKeyHolder {
    private static final String ALGORITHM = "EC";
    private static final long serialVersionUID = -4036633595001083922L;
    protected transient OpenSSLECGroupContext group;
    protected transient OpenSSLKey key;

    public OpenSSLECPrivateKey(OpenSSLECGroupContext group, OpenSSLKey key) {
        this.group = group;
        this.key = key;
    }

    public OpenSSLECPrivateKey(OpenSSLKey key) {
        long origGroup = NativeCrypto.EC_KEY_get0_group(key.getPkeyContext());
        this.group = new OpenSSLECGroupContext(NativeCrypto.EC_GROUP_dup(origGroup));
        this.key = key;
    }

    public OpenSSLECPrivateKey(ECPrivateKeySpec ecKeySpec) throws InvalidKeySpecException {
        try {
            this.group = OpenSSLECGroupContext.getInstance(ecKeySpec.getParams());
            BigInteger privKey = ecKeySpec.getS();
            this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_EC_KEY(this.group.getContext(), 0L, privKey.toByteArray()));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    public static OpenSSLKey wrapPlatformKey(ECPrivateKey ecPrivateKey) throws InvalidKeyException {
        try {
            OpenSSLECGroupContext group = OpenSSLECGroupContext.getInstance(ecPrivateKey.getParams());
            return wrapPlatformKey(ecPrivateKey, group);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("Unknown group parameters", e);
        }
    }

    private static OpenSSLKey wrapPlatformKey(ECPrivateKey ecPrivateKey, OpenSSLECGroupContext group) throws InvalidKeyException {
        return new OpenSSLKey(NativeCrypto.getECPrivateKeyWrapper(ecPrivateKey, group.getContext()), true);
    }

    public static OpenSSLKey getInstance(ECPrivateKey ecPrivateKey) throws InvalidKeyException {
        try {
            OpenSSLECGroupContext group = OpenSSLECGroupContext.getInstance(ecPrivateKey.getParams());
            if (ecPrivateKey.getFormat() == null) {
                return wrapPlatformKey(ecPrivateKey, group);
            }
            BigInteger privKey = ecPrivateKey.getS();
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_EC_KEY(group.getContext(), 0L, privKey.toByteArray()));
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
    public ECParameterSpec getParams() {
        return this.group.getECParameterSpec();
    }

    @Override
    public BigInteger getS() {
        if (this.key.isEngineBased()) {
            throw new UnsupportedOperationException("private key value S cannot be extracted");
        }
        return getPrivateKey();
    }

    private BigInteger getPrivateKey() {
        return new BigInteger(NativeCrypto.EC_KEY_get_private_key(this.key.getPkeyContext()));
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
        if (!(o instanceof ECPrivateKey)) {
            return false;
        }
        ECPrivateKey other = (ECPrivateKey) o;
        if (!getPrivateKey().equals(other.getS())) {
            return false;
        }
        ECParameterSpec spec = getParams();
        ECParameterSpec otherSpec = other.getParams();
        return spec.getCurve().equals(otherSpec.getCurve()) && spec.getGenerator().equals(otherSpec.getGenerator()) && spec.getOrder().equals(otherSpec.getOrder()) && spec.getCofactor() == otherSpec.getCofactor();
    }

    public int hashCode() {
        return Arrays.hashCode(NativeCrypto.i2d_PKCS8_PRIV_KEY_INFO(this.key.getPkeyContext()));
    }

    public String toString() {
        return NativeCrypto.EVP_PKEY_print_private(this.key.getPkeyContext());
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        byte[] encoded = (byte[]) stream.readObject();
        this.key = new OpenSSLKey(NativeCrypto.d2i_PKCS8_PRIV_KEY_INFO(encoded));
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
