package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class InvalidityDateExtension extends Extension implements CertAttrSet<String> {
    public static final String DATE = "date";
    public static final String NAME = "InvalidityDate";
    private Date date;

    private void encodeThis() throws IOException {
        if (this.date == null) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream derOutputStream = new DerOutputStream();
        derOutputStream.putGeneralizedTime(this.date);
        this.extensionValue = derOutputStream.toByteArray();
    }

    public InvalidityDateExtension(Date date) throws IOException {
        this(false, date);
    }

    public InvalidityDateExtension(boolean z, Date date) throws IOException {
        this.extensionId = PKIXExtensions.InvalidityDate_Id;
        this.critical = z;
        this.date = date;
        encodeThis();
    }

    public InvalidityDateExtension(Boolean bool, Object obj) throws IOException {
        this.extensionId = PKIXExtensions.InvalidityDate_Id;
        this.critical = bool.booleanValue();
        this.extensionValue = (byte[]) obj;
        this.date = new DerValue(this.extensionValue).getGeneralizedTime();
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (!(obj instanceof Date)) {
            throw new IOException("Attribute must be of type Date.");
        }
        if (str.equalsIgnoreCase(DATE)) {
            this.date = (Date) obj;
            encodeThis();
            return;
        }
        throw new IOException("Name not supported by InvalidityDateExtension");
    }

    @Override
    public Date get(String str) throws IOException {
        if (str.equalsIgnoreCase(DATE)) {
            if (this.date == null) {
                return null;
            }
            return new Date(this.date.getTime());
        }
        throw new IOException("Name not supported by InvalidityDateExtension");
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase(DATE)) {
            this.date = null;
            encodeThis();
            return;
        }
        throw new IOException("Name not supported by InvalidityDateExtension");
    }

    @Override
    public String toString() {
        return super.toString() + "    Invalidity Date: " + String.valueOf(this.date);
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.InvalidityDate_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement(DATE);
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static InvalidityDateExtension toImpl(java.security.cert.Extension extension) throws IOException {
        if (extension instanceof InvalidityDateExtension) {
            return (InvalidityDateExtension) extension;
        }
        return new InvalidityDateExtension(Boolean.valueOf(extension.isCritical()), extension.getValue());
    }
}
