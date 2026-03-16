package java.security;

import java.io.Serializable;
import java.security.cert.CertPath;

public final class CodeSigner implements Serializable {
    private static final long serialVersionUID = 6819288105193937581L;
    private transient int hash;
    private CertPath signerCertPath;
    private Timestamp timestamp;

    public CodeSigner(CertPath signerCertPath, Timestamp timestamp) {
        if (signerCertPath == null) {
            throw new NullPointerException("signerCertPath == null");
        }
        this.signerCertPath = signerCertPath;
        this.timestamp = timestamp;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CodeSigner)) {
            return false;
        }
        CodeSigner that = (CodeSigner) obj;
        if (this.signerCertPath.equals(that.signerCertPath)) {
            return this.timestamp == null ? that.timestamp == null : this.timestamp.equals(that.timestamp);
        }
        return false;
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public int hashCode() {
        if (this.hash == 0) {
            this.hash = (this.timestamp == null ? 0 : this.timestamp.hashCode()) ^ this.signerCertPath.hashCode();
        }
        return this.hash;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(256);
        buf.append("CodeSigner [").append(this.signerCertPath.getCertificates().get(0));
        if (this.timestamp != null) {
            buf.append("; ").append(this.timestamp);
        }
        buf.append("]");
        return buf.toString();
    }
}
