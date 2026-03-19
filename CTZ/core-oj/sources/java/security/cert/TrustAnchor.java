package java.security.cert;

import java.io.IOException;
import java.security.PublicKey;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.NameConstraintsExtension;

public class TrustAnchor {
    private final String caName;
    private final X500Principal caPrincipal;
    private NameConstraintsExtension nc;
    private byte[] ncBytes;
    private final PublicKey pubKey;
    private final X509Certificate trustedCert;

    public TrustAnchor(X509Certificate x509Certificate, byte[] bArr) {
        if (x509Certificate == null) {
            throw new NullPointerException("the trustedCert parameter must be non-null");
        }
        this.trustedCert = x509Certificate;
        this.pubKey = null;
        this.caName = null;
        this.caPrincipal = null;
        setNameConstraints(bArr);
    }

    public TrustAnchor(X500Principal x500Principal, PublicKey publicKey, byte[] bArr) {
        if (x500Principal == null || publicKey == null) {
            throw new NullPointerException();
        }
        this.trustedCert = null;
        this.caPrincipal = x500Principal;
        this.caName = x500Principal.getName();
        this.pubKey = publicKey;
        setNameConstraints(bArr);
    }

    public TrustAnchor(String str, PublicKey publicKey, byte[] bArr) {
        if (publicKey == null) {
            throw new NullPointerException("the pubKey parameter must be non-null");
        }
        if (str == null) {
            throw new NullPointerException("the caName parameter must be non-null");
        }
        if (str.length() == 0) {
            throw new IllegalArgumentException("the caName parameter must be a non-empty String");
        }
        this.caPrincipal = new X500Principal(str);
        this.pubKey = publicKey;
        this.caName = str;
        this.trustedCert = null;
        setNameConstraints(bArr);
    }

    public final X509Certificate getTrustedCert() {
        return this.trustedCert;
    }

    public final X500Principal getCA() {
        return this.caPrincipal;
    }

    public final String getCAName() {
        return this.caName;
    }

    public final PublicKey getCAPublicKey() {
        return this.pubKey;
    }

    private void setNameConstraints(byte[] bArr) {
        if (bArr == null) {
            this.ncBytes = null;
            this.nc = null;
            return;
        }
        this.ncBytes = (byte[]) bArr.clone();
        try {
            this.nc = new NameConstraintsExtension(Boolean.FALSE, bArr);
        } catch (IOException e) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException(e.getMessage());
            illegalArgumentException.initCause(e);
            throw illegalArgumentException;
        }
    }

    public final byte[] getNameConstraints() {
        if (this.ncBytes == null) {
            return null;
        }
        return (byte[]) this.ncBytes.clone();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("[\n");
        if (this.pubKey != null) {
            stringBuffer.append("  Trusted CA Public Key: " + this.pubKey.toString() + "\n");
            stringBuffer.append("  Trusted CA Issuer Name: " + String.valueOf(this.caName) + "\n");
        } else {
            stringBuffer.append("  Trusted CA cert: " + this.trustedCert.toString() + "\n");
        }
        if (this.nc != null) {
            stringBuffer.append("  Name Constraints: " + this.nc.toString() + "\n");
        }
        return stringBuffer.toString();
    }
}
