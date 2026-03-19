package sun.security.x509;

import java.io.IOException;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public final class AccessDescription {
    private GeneralName accessLocation;
    private ObjectIdentifier accessMethod;
    private int myhash = -1;
    public static final ObjectIdentifier Ad_OCSP_Id = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 1});
    public static final ObjectIdentifier Ad_CAISSUERS_Id = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 2});
    public static final ObjectIdentifier Ad_TIMESTAMPING_Id = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 3});
    public static final ObjectIdentifier Ad_CAREPOSITORY_Id = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 5});

    public AccessDescription(ObjectIdentifier objectIdentifier, GeneralName generalName) {
        this.accessMethod = objectIdentifier;
        this.accessLocation = generalName;
    }

    public AccessDescription(DerValue derValue) throws IOException {
        DerInputStream data = derValue.getData();
        this.accessMethod = data.getOID();
        this.accessLocation = new GeneralName(data.getDerValue());
    }

    public ObjectIdentifier getAccessMethod() {
        return this.accessMethod;
    }

    public GeneralName getAccessLocation() {
        return this.accessLocation;
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream2.putOID(this.accessMethod);
        this.accessLocation.encode(derOutputStream2);
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    public int hashCode() {
        if (this.myhash == -1) {
            this.myhash = this.accessMethod.hashCode() + this.accessLocation.hashCode();
        }
        return this.myhash;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AccessDescription)) {
            return false;
        }
        AccessDescription accessDescription = (AccessDescription) obj;
        if (this == accessDescription) {
            return true;
        }
        if (!this.accessMethod.equals((Object) accessDescription.getAccessMethod()) || !this.accessLocation.equals(accessDescription.getAccessLocation())) {
            return false;
        }
        return true;
    }

    public String toString() {
        String string;
        if (this.accessMethod.equals((Object) Ad_CAISSUERS_Id)) {
            string = "caIssuers";
        } else if (this.accessMethod.equals((Object) Ad_CAREPOSITORY_Id)) {
            string = "caRepository";
        } else if (this.accessMethod.equals((Object) Ad_TIMESTAMPING_Id)) {
            string = "timeStamping";
        } else if (this.accessMethod.equals((Object) Ad_OCSP_Id)) {
            string = "ocsp";
        } else {
            string = this.accessMethod.toString();
        }
        return "\n   accessMethod: " + string + "\n   accessLocation: " + this.accessLocation.toString() + "\n";
    }
}
