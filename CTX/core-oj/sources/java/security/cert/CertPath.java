package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public abstract class CertPath implements Serializable {
    private static final long serialVersionUID = 6068470306649138683L;
    private String type;

    public abstract List<? extends Certificate> getCertificates();

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract byte[] getEncoded(String str) throws CertificateEncodingException;

    public abstract Iterator<String> getEncodings();

    protected CertPath(String str) {
        this.type = str;
    }

    public String getType() {
        return this.type;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CertPath)) {
            return false;
        }
        CertPath certPath = (CertPath) obj;
        if (certPath.getType().equals(this.type)) {
            return getCertificates().equals(certPath.getCertificates());
        }
        return false;
    }

    public int hashCode() {
        return (31 * this.type.hashCode()) + getCertificates().hashCode();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<? extends Certificate> it = getCertificates().iterator();
        stringBuffer.append("\n" + this.type + " Cert Path: length = " + getCertificates().size() + ".\n");
        stringBuffer.append("[\n");
        int i = 1;
        while (it.hasNext()) {
            stringBuffer.append("=========================================================Certificate " + i + " start.\n");
            stringBuffer.append(it.next().toString());
            stringBuffer.append("\n=========================================================Certificate " + i + " end.\n\n\n");
            i++;
        }
        stringBuffer.append("\n]");
        return stringBuffer.toString();
    }

    protected Object writeReplace() throws ObjectStreamException {
        try {
            return new CertPathRep(this.type, getEncoded());
        } catch (CertificateException e) {
            NotSerializableException notSerializableException = new NotSerializableException("java.security.cert.CertPath: " + this.type);
            notSerializableException.initCause(e);
            throw notSerializableException;
        }
    }

    protected static class CertPathRep implements Serializable {
        private static final long serialVersionUID = 3015633072427920915L;
        private byte[] data;
        private String type;

        protected CertPathRep(String str, byte[] bArr) {
            this.type = str;
            this.data = bArr;
        }

        protected Object readResolve() throws ObjectStreamException {
            try {
                return CertificateFactory.getInstance(this.type).generateCertPath(new ByteArrayInputStream(this.data));
            } catch (CertificateException e) {
                NotSerializableException notSerializableException = new NotSerializableException("java.security.cert.CertPath: " + this.type);
                notSerializableException.initCause(e);
                throw notSerializableException;
            }
        }
    }
}
