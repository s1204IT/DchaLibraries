package java.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.cert.CertPath;

public final class CodeSigner implements Serializable {
    private static final long serialVersionUID = 6819288105193937581L;
    private transient int myhash = -1;
    private CertPath signerCertPath;
    private Timestamp timestamp;

    public CodeSigner(CertPath certPath, Timestamp timestamp) {
        if (certPath == null) {
            throw new NullPointerException();
        }
        this.signerCertPath = certPath;
        this.timestamp = timestamp;
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public int hashCode() {
        if (this.myhash == -1) {
            if (this.timestamp == null) {
                this.myhash = this.signerCertPath.hashCode();
            } else {
                this.myhash = this.signerCertPath.hashCode() + this.timestamp.hashCode();
            }
        }
        return this.myhash;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CodeSigner)) {
            return false;
        }
        CodeSigner codeSigner = (CodeSigner) obj;
        if (this == codeSigner) {
            return true;
        }
        Timestamp timestamp = codeSigner.getTimestamp();
        if (this.timestamp == null) {
            if (timestamp != null) {
                return false;
            }
        } else if (timestamp == null || !this.timestamp.equals(timestamp)) {
            return false;
        }
        return this.signerCertPath.equals(codeSigner.getSignerCertPath());
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("(");
        stringBuffer.append("Signer: " + ((Object) this.signerCertPath.getCertificates().get(0)));
        if (this.timestamp != null) {
            stringBuffer.append("timestamp: " + ((Object) this.timestamp));
        }
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.myhash = -1;
    }
}
