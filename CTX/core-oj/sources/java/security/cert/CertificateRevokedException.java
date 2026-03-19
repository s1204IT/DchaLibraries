package java.security.cert;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.InvalidityDateExtension;

public class CertificateRevokedException extends CertificateException {
    private static final long serialVersionUID = 7839996631571608627L;
    private final X500Principal authority;
    private transient Map<String, Extension> extensions;
    private final CRLReason reason;
    private Date revocationDate;

    public CertificateRevokedException(Date date, CRLReason cRLReason, X500Principal x500Principal, Map<String, Extension> map) {
        if (date == null || cRLReason == null || x500Principal == null || map == null) {
            throw new NullPointerException();
        }
        this.revocationDate = new Date(date.getTime());
        this.reason = cRLReason;
        this.authority = x500Principal;
        this.extensions = Collections.checkedMap(new HashMap(), String.class, Extension.class);
        this.extensions.putAll(map);
    }

    public Date getRevocationDate() {
        return (Date) this.revocationDate.clone();
    }

    public CRLReason getRevocationReason() {
        return this.reason;
    }

    public X500Principal getAuthorityName() {
        return this.authority;
    }

    public Date getInvalidityDate() {
        Extension extension = getExtensions().get("2.5.29.24");
        if (extension == null) {
            return null;
        }
        try {
            return new Date(InvalidityDateExtension.toImpl(extension).get("DATE").getTime());
        } catch (IOException e) {
            return null;
        }
    }

    public Map<String, Extension> getExtensions() {
        return Collections.unmodifiableMap(this.extensions);
    }

    @Override
    public String getMessage() {
        return "Certificate has been revoked, reason: " + ((Object) this.reason) + ", revocation date: " + ((Object) this.revocationDate) + ", authority: " + ((Object) this.authority) + ", extension OIDs: " + ((Object) this.extensions.keySet());
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.extensions.size());
        Iterator<Map.Entry<String, Extension>> it = this.extensions.entrySet().iterator();
        while (it.hasNext()) {
            Extension value = it.next().getValue();
            objectOutputStream.writeObject(value.getId());
            objectOutputStream.writeBoolean(value.isCritical());
            byte[] value2 = value.getValue();
            objectOutputStream.writeInt(value2.length);
            objectOutputStream.write(value2);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.revocationDate = new Date(this.revocationDate.getTime());
        int i = objectInputStream.readInt();
        if (i == 0) {
            this.extensions = Collections.emptyMap();
        } else {
            this.extensions = new HashMap(i);
        }
        for (int i2 = 0; i2 < i; i2++) {
            String str = (String) objectInputStream.readObject();
            boolean z = objectInputStream.readBoolean();
            byte[] bArr = new byte[objectInputStream.readInt()];
            objectInputStream.readFully(bArr);
            this.extensions.put(str, sun.security.x509.Extension.newExtension(new ObjectIdentifier(str), z, bArr));
        }
    }
}
