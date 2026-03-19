package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class CertificatePoliciesExtension extends Extension implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.extensions.CertificatePolicies";
    public static final String NAME = "CertificatePolicies";
    public static final String POLICIES = "policies";
    private List<PolicyInformation> certPolicies;

    private void encodeThis() throws IOException {
        if (this.certPolicies == null || this.certPolicies.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream derOutputStream = new DerOutputStream();
        DerOutputStream derOutputStream2 = new DerOutputStream();
        Iterator<PolicyInformation> it = this.certPolicies.iterator();
        while (it.hasNext()) {
            it.next().encode(derOutputStream2);
        }
        derOutputStream.write((byte) 48, derOutputStream2);
        this.extensionValue = derOutputStream.toByteArray();
    }

    public CertificatePoliciesExtension(List<PolicyInformation> list) throws IOException {
        this(Boolean.FALSE, list);
    }

    public CertificatePoliciesExtension(Boolean bool, List<PolicyInformation> list) throws IOException {
        this.certPolicies = list;
        this.extensionId = PKIXExtensions.CertificatePolicies_Id;
        this.critical = bool.booleanValue();
        encodeThis();
    }

    public CertificatePoliciesExtension(Boolean bool, Object obj) throws IOException {
        this.extensionId = PKIXExtensions.CertificatePolicies_Id;
        this.critical = bool.booleanValue();
        this.extensionValue = (byte[]) obj;
        DerValue derValue = new DerValue(this.extensionValue);
        if (derValue.tag != 48) {
            throw new IOException("Invalid encoding for CertificatePoliciesExtension.");
        }
        this.certPolicies = new ArrayList();
        while (derValue.data.available() != 0) {
            this.certPolicies.add(new PolicyInformation(derValue.data.getDerValue()));
        }
    }

    @Override
    public String toString() {
        if (this.certPolicies == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("CertificatePolicies [\n");
        Iterator<PolicyInformation> it = this.certPolicies.iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
        }
        sb.append("]\n");
        return sb.toString();
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.CertificatePolicies_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (str.equalsIgnoreCase(POLICIES)) {
            if (!(obj instanceof List)) {
                throw new IOException("Attribute value should be of type List.");
            }
            this.certPolicies = (List) obj;
            encodeThis();
            return;
        }
        throw new IOException("Attribute name [" + str + "] not recognized by CertAttrSet:CertificatePoliciesExtension.");
    }

    @Override
    public List<PolicyInformation> get(String str) throws IOException {
        if (str.equalsIgnoreCase(POLICIES)) {
            return this.certPolicies;
        }
        throw new IOException("Attribute name [" + str + "] not recognized by CertAttrSet:CertificatePoliciesExtension.");
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase(POLICIES)) {
            this.certPolicies = null;
            encodeThis();
        } else {
            throw new IOException("Attribute name [" + str + "] not recognized by CertAttrSet:CertificatePoliciesExtension.");
        }
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement(POLICIES);
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
