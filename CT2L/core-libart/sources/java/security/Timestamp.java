package java.security;

import java.io.Serializable;
import java.security.cert.CertPath;
import java.util.Date;

public final class Timestamp implements Serializable {
    private static final long serialVersionUID = -5502683707821851294L;
    private transient int hash;
    private CertPath signerCertPath;
    private Date timestamp;

    public Timestamp(Date timestamp, CertPath signerCertPath) {
        if (timestamp == null) {
            throw new NullPointerException("timestamp == null");
        }
        if (signerCertPath == null) {
            throw new NullPointerException("signerCertPath == null");
        }
        this.timestamp = new Date(timestamp.getTime());
        this.signerCertPath = signerCertPath;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Timestamp)) {
            return false;
        }
        Timestamp that = (Timestamp) obj;
        return this.timestamp.equals(that.timestamp) && this.signerCertPath.equals(that.signerCertPath);
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    public Date getTimestamp() {
        return (Date) this.timestamp.clone();
    }

    public int hashCode() {
        if (this.hash == 0) {
            this.hash = this.timestamp.hashCode() ^ this.signerCertPath.hashCode();
        }
        return this.hash;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(256);
        buf.append("Timestamp [").append(this.timestamp).append(" certPath=");
        buf.append(this.signerCertPath.getCertificates().get(0)).append("]");
        return buf.toString();
    }
}
