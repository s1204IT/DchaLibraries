package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class Extension implements java.security.cert.Extension {
    private static final int hashMagic = 31;
    protected boolean critical;
    protected ObjectIdentifier extensionId;
    protected byte[] extensionValue;

    public Extension() {
        this.extensionId = null;
        this.critical = false;
        this.extensionValue = null;
    }

    public Extension(DerValue derValue) throws IOException {
        this.extensionId = null;
        this.critical = false;
        this.extensionValue = null;
        DerInputStream derInputStream = derValue.toDerInputStream();
        this.extensionId = derInputStream.getOID();
        DerValue derValue2 = derInputStream.getDerValue();
        if (derValue2.tag == 1) {
            this.critical = derValue2.getBoolean();
            this.extensionValue = derInputStream.getDerValue().getOctetString();
        } else {
            this.critical = false;
            this.extensionValue = derValue2.getOctetString();
        }
    }

    public Extension(ObjectIdentifier objectIdentifier, boolean z, byte[] bArr) throws IOException {
        this.extensionId = null;
        this.critical = false;
        this.extensionValue = null;
        this.extensionId = objectIdentifier;
        this.critical = z;
        this.extensionValue = new DerValue(bArr).getOctetString();
    }

    public Extension(Extension extension) {
        this.extensionId = null;
        this.critical = false;
        this.extensionValue = null;
        this.extensionId = extension.extensionId;
        this.critical = extension.critical;
        this.extensionValue = extension.extensionValue;
    }

    public static Extension newExtension(ObjectIdentifier objectIdentifier, boolean z, byte[] bArr) throws IOException {
        Extension extension = new Extension();
        extension.extensionId = objectIdentifier;
        extension.critical = z;
        extension.extensionValue = bArr;
        return extension;
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new NullPointerException();
        }
        DerOutputStream derOutputStream = new DerOutputStream();
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream.putOID(this.extensionId);
        if (this.critical) {
            derOutputStream.putBoolean(this.critical);
        }
        derOutputStream.putOctetString(this.extensionValue);
        derOutputStream2.write((byte) 48, derOutputStream);
        outputStream.write(derOutputStream2.toByteArray());
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        if (this.extensionId == null) {
            throw new IOException("Null OID to encode for the extension!");
        }
        if (this.extensionValue == null) {
            throw new IOException("No value to encode for the extension!");
        }
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream2.putOID(this.extensionId);
        if (this.critical) {
            derOutputStream2.putBoolean(this.critical);
        }
        derOutputStream2.putOctetString(this.extensionValue);
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    @Override
    public boolean isCritical() {
        return this.critical;
    }

    public ObjectIdentifier getExtensionId() {
        return this.extensionId;
    }

    @Override
    public byte[] getValue() {
        return (byte[]) this.extensionValue.clone();
    }

    public byte[] getExtensionValue() {
        return this.extensionValue;
    }

    @Override
    public String getId() {
        return this.extensionId.toString();
    }

    public String toString() {
        String str = "ObjectId: " + this.extensionId.toString();
        if (this.critical) {
            return str + " Criticality=true\n";
        }
        return str + " Criticality=false\n";
    }

    public int hashCode() {
        int i = 0;
        if (this.extensionValue != null) {
            byte[] bArr = this.extensionValue;
            int length = bArr.length;
            while (length > 0) {
                int i2 = length - 1;
                i += length * bArr[i2];
                length = i2;
            }
        }
        return (((i * 31) + this.extensionId.hashCode()) * 31) + (this.critical ? 1231 : 1237);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Extension)) {
            return false;
        }
        Extension extension = (Extension) obj;
        if (this.critical == extension.critical && this.extensionId.equals((Object) extension.extensionId)) {
            return Arrays.equals(this.extensionValue, extension.extensionValue);
        }
        return false;
    }
}
