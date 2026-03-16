package org.apache.harmony.security.x509;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class CertificatePolicies extends ExtensionValue {
    public static final ASN1Type ASN1 = new ASN1SequenceOf(PolicyInformation.ASN1) {
        @Override
        public Object getDecodedObject(BerInputStream in) {
            return new CertificatePolicies((List) in.content, in.getEncoded());
        }

        @Override
        public Collection getValues(Object object) {
            CertificatePolicies cps = (CertificatePolicies) object;
            return cps.policyInformations;
        }
    };
    private byte[] encoding;
    private List<PolicyInformation> policyInformations;

    public CertificatePolicies() {
    }

    public static CertificatePolicies decode(byte[] encoding) throws IOException {
        CertificatePolicies cps = (CertificatePolicies) ASN1.decode(encoding);
        cps.encoding = encoding;
        return cps;
    }

    private CertificatePolicies(List<PolicyInformation> policyInformations, byte[] encoding) {
        this.policyInformations = policyInformations;
        this.encoding = encoding;
    }

    public List<PolicyInformation> getPolicyInformations() {
        return new ArrayList(this.policyInformations);
    }

    public CertificatePolicies addPolicyInformation(PolicyInformation policyInformation) {
        this.encoding = null;
        if (this.policyInformations == null) {
            this.policyInformations = new ArrayList();
        }
        this.policyInformations.add(policyInformation);
        return this;
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("CertificatePolicies [\n");
        for (PolicyInformation policyInformation : this.policyInformations) {
            sb.append(prefix);
            sb.append("  ");
            policyInformation.dumpValue(sb);
            sb.append('\n');
        }
        sb.append(prefix).append("]\n");
    }
}
