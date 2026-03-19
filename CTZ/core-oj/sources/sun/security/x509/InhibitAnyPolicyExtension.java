package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import sun.security.util.Debug;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class InhibitAnyPolicyExtension extends Extension implements CertAttrSet<String> {
    public static ObjectIdentifier AnyPolicy_Id = null;
    public static final String IDENT = "x509.info.extensions.InhibitAnyPolicy";
    public static final String NAME = "InhibitAnyPolicy";
    public static final String SKIP_CERTS = "skip_certs";
    private static final Debug debug = Debug.getInstance("certpath");
    private int skipCerts;

    static {
        try {
            AnyPolicy_Id = new ObjectIdentifier("2.5.29.32.0");
        } catch (IOException e) {
        }
    }

    private void encodeThis() throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        derOutputStream.putInteger(this.skipCerts);
        this.extensionValue = derOutputStream.toByteArray();
    }

    public InhibitAnyPolicyExtension(int i) throws IOException {
        this.skipCerts = Integer.MAX_VALUE;
        if (i < -1) {
            throw new IOException("Invalid value for skipCerts");
        }
        if (i == -1) {
            this.skipCerts = Integer.MAX_VALUE;
        } else {
            this.skipCerts = i;
        }
        this.extensionId = PKIXExtensions.InhibitAnyPolicy_Id;
        this.critical = true;
        encodeThis();
    }

    public InhibitAnyPolicyExtension(Boolean bool, Object obj) throws IOException {
        this.skipCerts = Integer.MAX_VALUE;
        this.extensionId = PKIXExtensions.InhibitAnyPolicy_Id;
        if (!bool.booleanValue()) {
            throw new IOException("Criticality cannot be false for InhibitAnyPolicy");
        }
        this.critical = bool.booleanValue();
        this.extensionValue = (byte[]) obj;
        DerValue derValue = new DerValue(this.extensionValue);
        if (derValue.tag != 2) {
            throw new IOException("Invalid encoding of InhibitAnyPolicy: data not integer");
        }
        if (derValue.data == null) {
            throw new IOException("Invalid encoding of InhibitAnyPolicy: null data");
        }
        int integer = derValue.getInteger();
        if (integer < -1) {
            throw new IOException("Invalid value for skipCerts");
        }
        if (integer == -1) {
            this.skipCerts = Integer.MAX_VALUE;
        } else {
            this.skipCerts = integer;
        }
    }

    @Override
    public String toString() {
        return super.toString() + "InhibitAnyPolicy: " + this.skipCerts + "\n";
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.InhibitAnyPolicy_Id;
            this.critical = true;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (str.equalsIgnoreCase(SKIP_CERTS)) {
            if (!(obj instanceof Integer)) {
                throw new IOException("Attribute value should be of type Integer.");
            }
            int iIntValue = ((Integer) obj).intValue();
            if (iIntValue < -1) {
                throw new IOException("Invalid value for skipCerts");
            }
            if (iIntValue == -1) {
                this.skipCerts = Integer.MAX_VALUE;
            } else {
                this.skipCerts = iIntValue;
            }
            encodeThis();
            return;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:InhibitAnyPolicy.");
    }

    @Override
    public Integer get(String str) throws IOException {
        if (str.equalsIgnoreCase(SKIP_CERTS)) {
            return new Integer(this.skipCerts);
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:InhibitAnyPolicy.");
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase(SKIP_CERTS)) {
            throw new IOException("Attribute skip_certs may not be deleted.");
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:InhibitAnyPolicy.");
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement(SKIP_CERTS);
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
