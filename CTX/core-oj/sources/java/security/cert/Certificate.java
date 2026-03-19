package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import sun.security.x509.X509CertImpl;

public abstract class Certificate implements Serializable {
    private static final long serialVersionUID = -3585440601605666277L;
    private int hash = -1;
    private final String type;

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract PublicKey getPublicKey();

    public abstract String toString();

    public abstract void verify(PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException;

    public abstract void verify(PublicKey publicKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException;

    protected Certificate(String str) {
        this.type = str;
    }

    public final String getType() {
        return this.type;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Certificate)) {
            return false;
        }
        try {
            return Arrays.equals(X509CertImpl.getEncodedInternal(this), X509CertImpl.getEncodedInternal((Certificate) obj));
        } catch (CertificateException e) {
            return false;
        }
    }

    public int hashCode() {
        int iHashCode = this.hash;
        if (iHashCode == -1) {
            try {
                iHashCode = Arrays.hashCode(X509CertImpl.getEncodedInternal(this));
            } catch (CertificateException e) {
                iHashCode = 0;
            }
            this.hash = iHashCode;
        }
        return iHashCode;
    }

    public void verify(PublicKey publicKey, Provider provider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
        throw new UnsupportedOperationException();
    }

    protected static class CertificateRep implements Serializable {
        private static final long serialVersionUID = -8563758940495660020L;
        private byte[] data;
        private String type;

        protected CertificateRep(String str, byte[] bArr) {
            this.type = str;
            this.data = bArr;
        }

        protected Object readResolve() throws ObjectStreamException {
            try {
                return CertificateFactory.getInstance(this.type).generateCertificate(new ByteArrayInputStream(this.data));
            } catch (CertificateException e) {
                throw new NotSerializableException("java.security.cert.Certificate: " + this.type + ": " + e.getMessage());
            }
        }
    }

    protected Object writeReplace() throws ObjectStreamException {
        try {
            return new CertificateRep(this.type, getEncoded());
        } catch (CertificateException e) {
            throw new NotSerializableException("java.security.cert.Certificate: " + this.type + ": " + e.getMessage());
        }
    }
}
