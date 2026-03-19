package sun.security.x509;

import java.io.IOException;
import java.util.Enumeration;

public class OCSPNoCheckExtension extends Extension implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.extensions.OCSPNoCheck";
    public static final String NAME = "OCSPNoCheck";

    public OCSPNoCheckExtension() throws IOException {
        this.extensionId = PKIXExtensions.OCSPNoCheck_Id;
        this.critical = false;
        this.extensionValue = new byte[0];
    }

    public OCSPNoCheckExtension(Boolean bool, Object obj) throws IOException {
        this.extensionId = PKIXExtensions.OCSPNoCheck_Id;
        this.critical = bool.booleanValue();
        this.extensionValue = new byte[0];
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        throw new IOException("No attribute is allowed by CertAttrSet:OCSPNoCheckExtension.");
    }

    @Override
    public Object get(String str) throws IOException {
        throw new IOException("No attribute is allowed by CertAttrSet:OCSPNoCheckExtension.");
    }

    @Override
    public void delete(String str) throws IOException {
        throw new IOException("No attribute is allowed by CertAttrSet:OCSPNoCheckExtension.");
    }

    @Override
    public Enumeration<String> getElements() {
        return new AttributeNameEnumeration().elements();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
