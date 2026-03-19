package sun.security.x509;

import java.io.IOException;
import java.util.Arrays;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class OtherName implements GeneralNameInterface {
    private static final byte TAG_VALUE = 0;
    private GeneralNameInterface gni;
    private int myhash = -1;
    private String name;
    private byte[] nameValue;
    private ObjectIdentifier oid;

    public OtherName(ObjectIdentifier objectIdentifier, byte[] bArr) throws IOException {
        this.nameValue = null;
        this.gni = null;
        if (objectIdentifier == null || bArr == null) {
            throw new NullPointerException("parameters may not be null");
        }
        this.oid = objectIdentifier;
        this.nameValue = bArr;
        this.gni = getGNI(objectIdentifier, bArr);
        if (this.gni != null) {
            this.name = this.gni.toString();
            return;
        }
        this.name = "Unrecognized ObjectIdentifier: " + objectIdentifier.toString();
    }

    public OtherName(DerValue derValue) throws IOException {
        this.nameValue = null;
        this.gni = null;
        DerInputStream derInputStream = derValue.toDerInputStream();
        this.oid = derInputStream.getOID();
        this.nameValue = derInputStream.getDerValue().toByteArray();
        this.gni = getGNI(this.oid, this.nameValue);
        if (this.gni != null) {
            this.name = this.gni.toString();
            return;
        }
        this.name = "Unrecognized ObjectIdentifier: " + this.oid.toString();
    }

    public ObjectIdentifier getOID() {
        return this.oid;
    }

    public byte[] getNameValue() {
        return (byte[]) this.nameValue.clone();
    }

    private GeneralNameInterface getGNI(ObjectIdentifier objectIdentifier, byte[] bArr) throws IOException {
        try {
            Class<?> cls = OIDMap.getClass(objectIdentifier);
            if (cls == null) {
                return null;
            }
            return (GeneralNameInterface) cls.getConstructor(Object.class).newInstance(bArr);
        } catch (Exception e) {
            throw new IOException("Instantiation error: " + ((Object) e), e);
        }
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        if (this.gni != null) {
            this.gni.encode(derOutputStream);
            return;
        }
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream2.putOID(this.oid);
        derOutputStream2.write(DerValue.createTag((byte) -128, true, (byte) 0), this.nameValue);
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OtherName)) {
            return false;
        }
        OtherName otherName = (OtherName) obj;
        if (!otherName.oid.equals((Object) this.oid)) {
            return false;
        }
        try {
            GeneralNameInterface gni = getGNI(otherName.oid, otherName.nameValue);
            if (gni == null) {
                return Arrays.equals(this.nameValue, otherName.nameValue);
            }
            try {
                return gni.constrains(this) == 0;
            } catch (UnsupportedOperationException e) {
                return false;
            }
        } catch (IOException e2) {
            return false;
        }
    }

    public int hashCode() {
        if (this.myhash == -1) {
            this.myhash = this.oid.hashCode() + 37;
            for (int i = 0; i < this.nameValue.length; i++) {
                this.myhash = (this.myhash * 37) + this.nameValue[i];
            }
        }
        return this.myhash;
    }

    public String toString() {
        return "Other-Name: " + this.name;
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) {
        if (generalNameInterface != null && generalNameInterface.getType() == 0) {
            throw new UnsupportedOperationException("Narrowing, widening, and matching are not supported for OtherName.");
        }
        return -1;
    }

    @Override
    public int subtreeDepth() {
        throw new UnsupportedOperationException("subtreeDepth() not supported for generic OtherName");
    }
}
