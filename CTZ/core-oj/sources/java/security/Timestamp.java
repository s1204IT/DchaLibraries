package java.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.cert.CertPath;
import java.util.Date;
import java.util.List;

public final class Timestamp implements Serializable {
    private static final long serialVersionUID = -5502683707821851294L;
    private transient int myhash = -1;
    private CertPath signerCertPath;
    private Date timestamp;

    public Timestamp(Date date, CertPath certPath) {
        if (date == null || certPath == null) {
            throw new NullPointerException();
        }
        this.timestamp = new Date(date.getTime());
        this.signerCertPath = certPath;
    }

    public Date getTimestamp() {
        return new Date(this.timestamp.getTime());
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    public int hashCode() {
        if (this.myhash == -1) {
            this.myhash = this.timestamp.hashCode() + this.signerCertPath.hashCode();
        }
        return this.myhash;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Timestamp)) {
            return false;
        }
        Timestamp timestamp = (Timestamp) obj;
        if (this == timestamp) {
            return true;
        }
        if (!this.timestamp.equals(timestamp.getTimestamp()) || !this.signerCertPath.equals(timestamp.getSignerCertPath())) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("(");
        stringBuffer.append("timestamp: " + ((Object) this.timestamp));
        List<? extends java.security.cert.Certificate> certificates = this.signerCertPath.getCertificates();
        if (!certificates.isEmpty()) {
            stringBuffer.append("TSA: " + ((Object) certificates.get(0)));
        } else {
            stringBuffer.append("TSA: <empty>");
        }
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.myhash = -1;
        this.timestamp = new Date(this.timestamp.getTime());
    }
}
