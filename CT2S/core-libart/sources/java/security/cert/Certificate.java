package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;

public abstract class Certificate implements Serializable {
    private static final long serialVersionUID = -3585440601605666277L;
    private final String type;

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract PublicKey getPublicKey();

    public abstract String toString();

    public abstract void verify(PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException;

    public abstract void verify(PublicKey publicKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException;

    protected Certificate(String type) {
        this.type = type;
    }

    public final String getType() {
        return this.type;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Certificate) {
            try {
                return Arrays.equals(getEncoded(), ((Certificate) other).getEncoded());
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public int hashCode() {
        try {
            byte[] encoded = getEncoded();
            int hash = 0;
            for (int i = 0; i < encoded.length; i++) {
                hash += encoded[i] * i;
            }
            return hash;
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected Object writeReplace() throws ObjectStreamException {
        try {
            return new CertificateRep(getType(), getEncoded());
        } catch (CertificateEncodingException e) {
            throw new NotSerializableException("Could not create serialization object: " + e);
        }
    }

    protected static class CertificateRep implements Serializable {
        private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("type", (Class<?>) String.class), new ObjectStreamField("data", byte[].class, true)};
        private static final long serialVersionUID = -8563758940495660020L;
        private final byte[] data;
        private final String type;

        protected CertificateRep(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        protected Object readResolve() throws ObjectStreamException {
            try {
                CertificateFactory cf = CertificateFactory.getInstance(this.type);
                return cf.generateCertificate(new ByteArrayInputStream(this.data));
            } catch (Throwable t) {
                throw new NotSerializableException("Could not resolve certificate: " + t);
            }
        }
    }
}
