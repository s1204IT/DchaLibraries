package org.apache.harmony.security.x509;

import java.io.IOException;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.asn1.ASN1Choice;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.x501.Name;

public final class DistributionPointName {
    public static final ASN1Choice ASN1 = new ASN1Choice(new ASN1Type[]{new ASN1Implicit(0, GeneralNames.ASN1), new ASN1Implicit(1, Name.ASN1_RDN)}) {
        @Override
        public int getIndex(Object object) {
            DistributionPointName dpn = (DistributionPointName) object;
            return dpn.fullName == null ? 1 : 0;
        }

        @Override
        protected Object getDecodedObject(BerInputStream in) throws IOException {
            if (in.choiceIndex == 0) {
                DistributionPointName result = new DistributionPointName((GeneralNames) in.content);
                return result;
            }
            DistributionPointName result2 = new DistributionPointName((Name) in.content);
            return result2;
        }

        @Override
        public Object getObjectToEncode(Object object) {
            DistributionPointName dpn = (DistributionPointName) object;
            return dpn.fullName == null ? dpn.nameRelativeToCRLIssuer : dpn.fullName;
        }
    };
    private final GeneralNames fullName;
    private final Name nameRelativeToCRLIssuer;

    public DistributionPointName(GeneralNames fullName) {
        this.fullName = fullName;
        this.nameRelativeToCRLIssuer = null;
    }

    public DistributionPointName(Name nameRelativeToCRLIssuer) {
        this.fullName = null;
        this.nameRelativeToCRLIssuer = nameRelativeToCRLIssuer;
    }

    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix);
        sb.append("Distribution Point Name: [\n");
        if (this.fullName != null) {
            this.fullName.dumpValue(sb, prefix + "  ");
        } else {
            sb.append(prefix);
            sb.append("  ");
            sb.append(this.nameRelativeToCRLIssuer.getName(X500Principal.RFC2253));
        }
        sb.append(prefix);
        sb.append("]\n");
    }
}
