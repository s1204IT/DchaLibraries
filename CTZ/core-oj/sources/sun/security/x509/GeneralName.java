package sun.security.x509;

import java.io.IOException;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class GeneralName {
    private GeneralNameInterface name;

    public GeneralName(GeneralNameInterface generalNameInterface) {
        this.name = null;
        if (generalNameInterface == null) {
            throw new NullPointerException("GeneralName must not be null");
        }
        this.name = generalNameInterface;
    }

    public GeneralName(DerValue derValue) throws IOException {
        this(derValue, false);
    }

    public GeneralName(DerValue derValue, boolean z) throws IOException {
        this.name = null;
        short s = (byte) (derValue.tag & 31);
        switch (s) {
            case 0:
                if (derValue.isContextSpecific() && derValue.isConstructed()) {
                    derValue.resetTag((byte) 48);
                    this.name = new OtherName(derValue);
                    return;
                }
                throw new IOException("Invalid encoding of Other-Name");
            case 1:
                if (derValue.isContextSpecific() && !derValue.isConstructed()) {
                    derValue.resetTag((byte) 22);
                    this.name = new RFC822Name(derValue);
                    return;
                }
                throw new IOException("Invalid encoding of RFC822 name");
            case 2:
                if (derValue.isContextSpecific() && !derValue.isConstructed()) {
                    derValue.resetTag((byte) 22);
                    this.name = new DNSName(derValue);
                    return;
                }
                throw new IOException("Invalid encoding of DNS name");
            case 3:
            default:
                throw new IOException("Unrecognized GeneralName tag, (" + ((int) s) + ")");
            case 4:
                if (derValue.isContextSpecific() && derValue.isConstructed()) {
                    this.name = new X500Name(derValue.getData());
                    return;
                }
                throw new IOException("Invalid encoding of Directory name");
            case 5:
                if (derValue.isContextSpecific() && derValue.isConstructed()) {
                    derValue.resetTag((byte) 48);
                    this.name = new EDIPartyName(derValue);
                    return;
                }
                throw new IOException("Invalid encoding of EDI name");
            case 6:
                if (derValue.isContextSpecific() && !derValue.isConstructed()) {
                    derValue.resetTag((byte) 22);
                    this.name = z ? URIName.nameConstraint(derValue) : new URIName(derValue);
                    return;
                }
                throw new IOException("Invalid encoding of URI");
            case 7:
                if (derValue.isContextSpecific() && !derValue.isConstructed()) {
                    derValue.resetTag((byte) 4);
                    this.name = new IPAddressName(derValue);
                    return;
                }
                throw new IOException("Invalid encoding of IP address");
            case 8:
                if (derValue.isContextSpecific() && !derValue.isConstructed()) {
                    derValue.resetTag((byte) 6);
                    this.name = new OIDName(derValue);
                    return;
                }
                throw new IOException("Invalid encoding of OID name");
        }
    }

    public int getType() {
        return this.name.getType();
    }

    public GeneralNameInterface getName() {
        return this.name;
    }

    public String toString() {
        return this.name.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeneralName)) {
            return false;
        }
        try {
            return this.name.constrains(((GeneralName) obj).name) == 0;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        this.name.encode(derOutputStream2);
        int type = this.name.getType();
        if (type == 0 || type == 3 || type == 5) {
            derOutputStream.writeImplicit(DerValue.createTag((byte) -128, true, (byte) type), derOutputStream2);
        } else if (type == 4) {
            derOutputStream.write(DerValue.createTag((byte) -128, true, (byte) type), derOutputStream2);
        } else {
            derOutputStream.writeImplicit(DerValue.createTag((byte) -128, false, (byte) type), derOutputStream2);
        }
    }
}
