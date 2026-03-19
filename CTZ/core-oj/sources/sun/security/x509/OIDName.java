package sun.security.x509;

import java.io.IOException;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class OIDName implements GeneralNameInterface {
    private ObjectIdentifier oid;

    public OIDName(DerValue derValue) throws IOException {
        this.oid = derValue.getOID();
    }

    public OIDName(ObjectIdentifier objectIdentifier) {
        this.oid = objectIdentifier;
    }

    public OIDName(String str) throws IOException {
        try {
            this.oid = new ObjectIdentifier(str);
        } catch (Exception e) {
            throw new IOException("Unable to create OIDName: " + ((Object) e));
        }
    }

    @Override
    public int getType() {
        return 8;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putOID(this.oid);
    }

    public String toString() {
        return "OIDName: " + this.oid.toString();
    }

    public ObjectIdentifier getOID() {
        return this.oid;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OIDName)) {
            return false;
        }
        return this.oid.equals((Object) ((OIDName) obj).oid);
    }

    public int hashCode() {
        return this.oid.hashCode();
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        if (generalNameInterface == null || generalNameInterface.getType() != 8) {
            return -1;
        }
        if (equals((OIDName) generalNameInterface)) {
            return 0;
        }
        throw new UnsupportedOperationException("Narrowing and widening are not supported for OIDNames");
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("subtreeDepth() not supported for OIDName.");
    }
}
