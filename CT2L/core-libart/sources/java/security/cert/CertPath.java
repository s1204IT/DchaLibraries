package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public abstract class CertPath implements Serializable {
    private static final long serialVersionUID = 6068470306649138683L;
    private final String type;

    public abstract List<? extends Certificate> getCertificates();

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract byte[] getEncoded(String str) throws CertificateEncodingException;

    public abstract Iterator<String> getEncodings();

    protected CertPath(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof CertPath) {
            CertPath o = (CertPath) other;
            if (getType().equals(o.getType()) && getCertificates().equals(o.getCertificates())) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        int hash = getType().hashCode();
        return (hash * 31) + getCertificates().hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getType());
        sb.append(" Cert Path, len=");
        sb.append(getCertificates().size());
        sb.append(": [\n");
        int n = 1;
        Iterator<? extends Certificate> i = getCertificates().iterator();
        while (i.hasNext()) {
            sb.append("---------------certificate ");
            sb.append(n);
            sb.append("---------------\n");
            sb.append(i.next().toString());
            n++;
        }
        sb.append("\n]");
        return sb.toString();
    }

    protected Object writeReplace() throws ObjectStreamException {
        try {
            return new CertPathRep(getType(), getEncoded());
        } catch (CertificateEncodingException e) {
            throw new NotSerializableException("Could not create serialization object: " + e);
        }
    }

    protected static class CertPathRep implements Serializable {
        private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("type", (Class<?>) String.class), new ObjectStreamField("data", byte[].class, true)};
        private static final long serialVersionUID = 3015633072427920915L;
        private final byte[] data;
        private final String type;

        protected CertPathRep(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        protected Object readResolve() throws ObjectStreamException {
            try {
                CertificateFactory cf = CertificateFactory.getInstance(this.type);
                return cf.generateCertPath(new ByteArrayInputStream(this.data));
            } catch (Throwable t) {
                throw new NotSerializableException("Could not resolve cert path: " + t);
            }
        }
    }
}
