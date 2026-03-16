package org.apache.harmony.security.x509;

import java.io.IOException;
import org.apache.harmony.security.asn1.ASN1Explicit;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class DistributionPoint {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{new ASN1Explicit(0, DistributionPointName.ASN1), new ASN1Implicit(1, ReasonFlags.ASN1), new ASN1Implicit(2, GeneralNames.ASN1)}) {
        {
            setOptional(0);
            setOptional(1);
            setOptional(2);
        }

        @Override
        protected Object getDecodedObject(BerInputStream in) throws IOException {
            Object[] values = (Object[]) in.content;
            return new DistributionPoint((DistributionPointName) values[0], (ReasonFlags) values[1], (GeneralNames) values[2]);
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            DistributionPoint dp = (DistributionPoint) object;
            values[0] = dp.distributionPoint;
            values[1] = dp.reasons;
            values[2] = dp.cRLIssuer;
        }
    };
    private final GeneralNames cRLIssuer;
    private final DistributionPointName distributionPoint;
    private final ReasonFlags reasons;

    public DistributionPoint(DistributionPointName distributionPoint, ReasonFlags reasons, GeneralNames cRLIssuer) {
        if (reasons != null && distributionPoint == null && cRLIssuer == null) {
            throw new IllegalArgumentException("DistributionPoint MUST NOT consist of only the reasons field");
        }
        this.distributionPoint = distributionPoint;
        this.reasons = reasons;
        this.cRLIssuer = cRLIssuer;
    }

    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix);
        sb.append("Distribution Point: [\n");
        if (this.distributionPoint != null) {
            this.distributionPoint.dumpValue(sb, prefix + "  ");
        }
        if (this.reasons != null) {
            this.reasons.dumpValue(sb, prefix + "  ");
        }
        if (this.cRLIssuer != null) {
            sb.append(prefix);
            sb.append("  CRL Issuer: [\n");
            this.cRLIssuer.dumpValue(sb, prefix + "    ");
            sb.append(prefix);
            sb.append("  ]\n");
        }
        sb.append(prefix);
        sb.append("]\n");
    }
}
