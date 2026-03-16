package java.security.cert;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.x509.InvalidityDate;

public class CertificateRevokedException extends CertificateException {
    private static final long serialVersionUID = 7839996631571608627L;
    private final X500Principal authority;
    private transient Map<String, Extension> extensions;
    private final CRLReason reason;
    private final Date revocationDate;

    public CertificateRevokedException(Date revocationDate, CRLReason reason, X500Principal authority, Map<String, Extension> extensions) {
        this.revocationDate = revocationDate;
        this.reason = reason;
        this.authority = authority;
        this.extensions = extensions;
    }

    public X500Principal getAuthorityName() {
        return this.authority;
    }

    public Map<String, Extension> getExtensions() {
        return Collections.unmodifiableMap(this.extensions);
    }

    public Date getInvalidityDate() {
        Extension invalidityDateExtension;
        if (this.extensions == null || (invalidityDateExtension = this.extensions.get("2.5.29.24")) == null) {
            return null;
        }
        try {
            InvalidityDate invalidityDate = new InvalidityDate(invalidityDateExtension.getValue());
            return invalidityDate.getDate();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getMessage() {
        StringBuffer sb = new StringBuffer("Certificate was revoked");
        if (this.revocationDate != null) {
            sb.append(" on ").append(this.revocationDate.toString());
        }
        if (this.reason != null) {
            sb.append(" due to ").append(this.reason);
        }
        return sb.toString();
    }

    public Date getRevocationDate() {
        return (Date) this.revocationDate.clone();
    }

    public CRLReason getRevocationReason() {
        return this.reason;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int size = stream.readInt();
        this.extensions = new HashMap(size);
        for (int i = 0; i < size; i++) {
            String oid = (String) stream.readObject();
            boolean critical = stream.readBoolean();
            int valueLen = stream.readInt();
            byte[] value = new byte[valueLen];
            stream.read(value);
            this.extensions.put(oid, new org.apache.harmony.security.x509.Extension(oid, critical, value));
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.extensions.size());
        for (Extension e : this.extensions.values()) {
            stream.writeObject(e.getId());
            stream.writeBoolean(e.isCritical());
            byte[] value = e.getValue();
            stream.writeInt(value.length);
            stream.write(value);
        }
    }
}
