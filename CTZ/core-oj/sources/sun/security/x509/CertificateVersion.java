package sun.security.x509;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class CertificateVersion implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.version";
    public static final String NAME = "version";
    public static final int V1 = 0;
    public static final int V2 = 1;
    public static final int V3 = 2;
    public static final String VERSION = "number";
    int version;

    private int getVersion() {
        return this.version;
    }

    private void construct(DerValue derValue) throws IOException {
        if (derValue.isConstructed() && derValue.isContextSpecific()) {
            DerValue derValue2 = derValue.data.getDerValue();
            this.version = derValue2.getInteger();
            if (derValue2.data.available() != 0) {
                throw new IOException("X.509 version, bad format");
            }
        }
    }

    public CertificateVersion() {
        this.version = 0;
        this.version = 0;
    }

    public CertificateVersion(int i) throws IOException {
        this.version = 0;
        if (i == 0 || i == 1 || i == 2) {
            this.version = i;
            return;
        }
        throw new IOException("X.509 Certificate version " + i + " not supported.\n");
    }

    public CertificateVersion(DerInputStream derInputStream) throws IOException {
        this.version = 0;
        this.version = 0;
        construct(derInputStream.getDerValue());
    }

    public CertificateVersion(InputStream inputStream) throws IOException {
        this.version = 0;
        this.version = 0;
        construct(new DerValue(inputStream));
    }

    public CertificateVersion(DerValue derValue) throws IOException {
        this.version = 0;
        this.version = 0;
        construct(derValue);
    }

    @Override
    public String toString() {
        return "Version: V" + (this.version + 1);
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        if (this.version == 0) {
            return;
        }
        DerOutputStream derOutputStream = new DerOutputStream();
        derOutputStream.putInteger(this.version);
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream2.write(DerValue.createTag((byte) -128, true, (byte) 0), derOutputStream);
        outputStream.write(derOutputStream2.toByteArray());
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (!(obj instanceof Integer)) {
            throw new IOException("Attribute must be of type Integer.");
        }
        if (str.equalsIgnoreCase("number")) {
            this.version = ((Integer) obj).intValue();
            return;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet: CertificateVersion.");
    }

    @Override
    public Integer get(String str) throws IOException {
        if (str.equalsIgnoreCase("number")) {
            return new Integer(getVersion());
        }
        throw new IOException("Attribute name not recognized by CertAttrSet: CertificateVersion.");
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase("number")) {
            this.version = 0;
            return;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet: CertificateVersion.");
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement("number");
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return "version";
    }

    public int compare(int i) {
        return this.version - i;
    }
}
