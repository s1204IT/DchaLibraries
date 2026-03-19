package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import javax.security.auth.x500.X500Principal;
import sun.security.provider.X509Factory;
import sun.security.util.Cache;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.X509CertImpl;

public class X509CertificatePair {
    private static final byte TAG_FORWARD = 0;
    private static final byte TAG_REVERSE = 1;
    private static final Cache<Object, X509CertificatePair> cache = Cache.newSoftMemoryCache(750);
    private byte[] encoded;
    private X509Certificate forward;
    private X509Certificate reverse;

    public X509CertificatePair() {
    }

    public X509CertificatePair(X509Certificate x509Certificate, X509Certificate x509Certificate2) throws CertificateException {
        if (x509Certificate == null && x509Certificate2 == null) {
            throw new CertificateException("at least one of certificate pair must be non-null");
        }
        this.forward = x509Certificate;
        this.reverse = x509Certificate2;
        checkPair();
    }

    private X509CertificatePair(byte[] bArr) throws CertificateException {
        try {
            parse(new DerValue(bArr));
            this.encoded = bArr;
            checkPair();
        } catch (IOException e) {
            throw new CertificateException(e.toString());
        }
    }

    public static synchronized void clearCache() {
        cache.clear();
    }

    public static synchronized X509CertificatePair generateCertificatePair(byte[] bArr) throws CertificateException {
        X509CertificatePair x509CertificatePair = cache.get(new Cache.EqualByteArray(bArr));
        if (x509CertificatePair != null) {
            return x509CertificatePair;
        }
        X509CertificatePair x509CertificatePair2 = new X509CertificatePair(bArr);
        cache.put(new Cache.EqualByteArray(x509CertificatePair2.encoded), x509CertificatePair2);
        return x509CertificatePair2;
    }

    public void setForward(X509Certificate x509Certificate) throws CertificateException {
        checkPair();
        this.forward = x509Certificate;
    }

    public void setReverse(X509Certificate x509Certificate) throws CertificateException {
        checkPair();
        this.reverse = x509Certificate;
    }

    public X509Certificate getForward() {
        return this.forward;
    }

    public X509Certificate getReverse() {
        return this.reverse;
    }

    public byte[] getEncoded() throws CertificateEncodingException {
        try {
            if (this.encoded == null) {
                DerOutputStream derOutputStream = new DerOutputStream();
                emit(derOutputStream);
                this.encoded = derOutputStream.toByteArray();
            }
            return this.encoded;
        } catch (IOException e) {
            throw new CertificateEncodingException(e.toString());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X.509 Certificate Pair: [\n");
        if (this.forward != null) {
            sb.append("  Forward: ");
            sb.append((Object) this.forward);
            sb.append("\n");
        }
        if (this.reverse != null) {
            sb.append("  Reverse: ");
            sb.append((Object) this.reverse);
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private void parse(DerValue derValue) throws IOException, CertificateException {
        if (derValue.tag != 48) {
            throw new IOException("Sequence tag missing for X509CertificatePair");
        }
        while (derValue.data != null && derValue.data.available() != 0) {
            DerValue derValue2 = derValue.data.getDerValue();
            switch ((byte) (derValue2.tag & 31)) {
                case 0:
                    if (derValue2.isContextSpecific() && derValue2.isConstructed()) {
                        if (this.forward != null) {
                            throw new IOException("Duplicate forward certificate in X509CertificatePair");
                        }
                        this.forward = X509Factory.intern(new X509CertImpl(derValue2.data.getDerValue().toByteArray()));
                    }
                    break;
                case 1:
                    if (derValue2.isContextSpecific() && derValue2.isConstructed()) {
                        if (this.reverse != null) {
                            throw new IOException("Duplicate reverse certificate in X509CertificatePair");
                        }
                        this.reverse = X509Factory.intern(new X509CertImpl(derValue2.data.getDerValue().toByteArray()));
                    }
                    break;
                default:
                    throw new IOException("Invalid encoding of X509CertificatePair");
            }
        }
        if (this.forward == null && this.reverse == null) {
            throw new CertificateException("at least one of certificate pair must be non-null");
        }
    }

    private void emit(DerOutputStream derOutputStream) throws IOException, CertificateEncodingException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        if (this.forward != null) {
            DerOutputStream derOutputStream3 = new DerOutputStream();
            derOutputStream3.putDerValue(new DerValue(this.forward.getEncoded()));
            derOutputStream2.write(DerValue.createTag((byte) -128, true, (byte) 0), derOutputStream3);
        }
        if (this.reverse != null) {
            DerOutputStream derOutputStream4 = new DerOutputStream();
            derOutputStream4.putDerValue(new DerValue(this.reverse.getEncoded()));
            derOutputStream2.write(DerValue.createTag((byte) -128, true, (byte) 1), derOutputStream4);
        }
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    private void checkPair() throws CertificateException {
        if (this.forward == null || this.reverse == null) {
            return;
        }
        X500Principal subjectX500Principal = this.forward.getSubjectX500Principal();
        X500Principal issuerX500Principal = this.forward.getIssuerX500Principal();
        X500Principal subjectX500Principal2 = this.reverse.getSubjectX500Principal();
        X500Principal issuerX500Principal2 = this.reverse.getIssuerX500Principal();
        if (!issuerX500Principal.equals(subjectX500Principal2) || !issuerX500Principal2.equals(subjectX500Principal)) {
            throw new CertificateException("subject and issuer names in forward and reverse certificates do not match");
        }
        try {
            PublicKey publicKey = this.reverse.getPublicKey();
            if (!(publicKey instanceof DSAPublicKey) || ((DSAPublicKey) publicKey).getParams() != null) {
                this.forward.verify(publicKey);
            }
            PublicKey publicKey2 = this.forward.getPublicKey();
            if (!(publicKey2 instanceof DSAPublicKey) || ((DSAPublicKey) publicKey2).getParams() != null) {
                this.reverse.verify(publicKey2);
            }
        } catch (GeneralSecurityException e) {
            throw new CertificateException("invalid signature: " + e.getMessage());
        }
    }
}
