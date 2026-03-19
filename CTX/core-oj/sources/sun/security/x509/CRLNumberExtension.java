package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Enumeration;
import sun.security.util.Debug;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class CRLNumberExtension extends Extension implements CertAttrSet<String> {
    private static final String LABEL = "CRL Number";
    public static final String NAME = "CRLNumber";
    public static final String NUMBER = "value";
    private BigInteger crlNumber;
    private String extensionLabel;
    private String extensionName;

    private void encodeThis() throws IOException {
        if (this.crlNumber == null) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream derOutputStream = new DerOutputStream();
        derOutputStream.putInteger(this.crlNumber);
        this.extensionValue = derOutputStream.toByteArray();
    }

    public CRLNumberExtension(int i) throws IOException {
        this(PKIXExtensions.CRLNumber_Id, false, BigInteger.valueOf(i), NAME, LABEL);
    }

    public CRLNumberExtension(BigInteger bigInteger) throws IOException {
        this(PKIXExtensions.CRLNumber_Id, false, bigInteger, NAME, LABEL);
    }

    protected CRLNumberExtension(ObjectIdentifier objectIdentifier, boolean z, BigInteger bigInteger, String str, String str2) throws IOException {
        this.crlNumber = null;
        this.extensionId = objectIdentifier;
        this.critical = z;
        this.crlNumber = bigInteger;
        this.extensionName = str;
        this.extensionLabel = str2;
        encodeThis();
    }

    public CRLNumberExtension(Boolean bool, Object obj) throws IOException {
        this(PKIXExtensions.CRLNumber_Id, bool, obj, NAME, LABEL);
    }

    protected CRLNumberExtension(ObjectIdentifier objectIdentifier, Boolean bool, Object obj, String str, String str2) throws IOException {
        this.crlNumber = null;
        this.extensionId = objectIdentifier;
        this.critical = bool.booleanValue();
        this.extensionValue = (byte[]) obj;
        this.crlNumber = new DerValue(this.extensionValue).getBigInteger();
        this.extensionName = str;
        this.extensionLabel = str2;
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (str.equalsIgnoreCase("value")) {
            if (!(obj instanceof BigInteger)) {
                throw new IOException("Attribute must be of type BigInteger.");
            }
            this.crlNumber = (BigInteger) obj;
            encodeThis();
            return;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:" + this.extensionName + ".");
    }

    @Override
    public BigInteger get(String str) throws IOException {
        if (str.equalsIgnoreCase("value")) {
            return this.crlNumber;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:" + this.extensionName + '.');
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase("value")) {
            this.crlNumber = null;
            encodeThis();
        } else {
            throw new IOException("Attribute name not recognized by CertAttrSet:" + this.extensionName + ".");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(this.extensionLabel);
        sb.append(": ");
        sb.append(this.crlNumber == null ? "" : Debug.toHexString(this.crlNumber));
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        new DerOutputStream();
        encode(outputStream, PKIXExtensions.CRLNumber_Id, true);
    }

    protected void encode(OutputStream outputStream, ObjectIdentifier objectIdentifier, boolean z) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = objectIdentifier;
            this.critical = z;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement("value");
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return this.extensionName;
    }
}
