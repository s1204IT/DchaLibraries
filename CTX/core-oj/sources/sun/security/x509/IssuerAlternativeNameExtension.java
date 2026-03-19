package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class IssuerAlternativeNameExtension extends Extension implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.extensions.IssuerAlternativeName";
    public static final String ISSUER_NAME = "issuer_name";
    public static final String NAME = "IssuerAlternativeName";
    GeneralNames names;

    private void encodeThis() throws IOException {
        if (this.names == null || this.names.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream derOutputStream = new DerOutputStream();
        this.names.encode(derOutputStream);
        this.extensionValue = derOutputStream.toByteArray();
    }

    public IssuerAlternativeNameExtension(GeneralNames generalNames) throws IOException {
        this.names = null;
        this.names = generalNames;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = false;
        encodeThis();
    }

    public IssuerAlternativeNameExtension(Boolean bool, GeneralNames generalNames) throws IOException {
        this.names = null;
        this.names = generalNames;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = bool.booleanValue();
        encodeThis();
    }

    public IssuerAlternativeNameExtension() {
        this.names = null;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = false;
        this.names = new GeneralNames();
    }

    public IssuerAlternativeNameExtension(Boolean bool, Object obj) throws IOException {
        this.names = null;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = bool.booleanValue();
        this.extensionValue = (byte[]) obj;
        DerValue derValue = new DerValue(this.extensionValue);
        if (derValue.data == null) {
            this.names = new GeneralNames();
        } else {
            this.names = new GeneralNames(derValue);
        }
    }

    @Override
    public String toString() {
        String str = super.toString() + "IssuerAlternativeName [\n";
        if (this.names == null) {
            str = str + "  null\n";
        } else {
            Iterator<GeneralName> it = this.names.names().iterator();
            while (it.hasNext()) {
                str = str + "  " + ((Object) it.next()) + "\n";
            }
        }
        return str + "]\n";
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (str.equalsIgnoreCase(ISSUER_NAME)) {
            if (!(obj instanceof GeneralNames)) {
                throw new IOException("Attribute value should be of type GeneralNames.");
            }
            this.names = (GeneralNames) obj;
            encodeThis();
            return;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:IssuerAlternativeName.");
    }

    @Override
    public GeneralNames get(String str) throws IOException {
        if (str.equalsIgnoreCase(ISSUER_NAME)) {
            return this.names;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:IssuerAlternativeName.");
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase(ISSUER_NAME)) {
            this.names = null;
            encodeThis();
            return;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:IssuerAlternativeName.");
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement(ISSUER_NAME);
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
