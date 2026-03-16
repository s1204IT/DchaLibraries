package org.apache.harmony.security.x509;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class CRLDistributionPoints extends ExtensionValue {
    public static final ASN1Type ASN1 = new ASN1SequenceOf(DistributionPoint.ASN1) {
        @Override
        public Object getDecodedObject(BerInputStream in) {
            return new CRLDistributionPoints((List) in.content, in.getEncoded());
        }

        @Override
        public Collection<?> getValues(Object object) {
            CRLDistributionPoints dps = (CRLDistributionPoints) object;
            return dps.distributionPoints;
        }
    };
    private List<DistributionPoint> distributionPoints;
    private byte[] encoding;

    private CRLDistributionPoints(List<DistributionPoint> distributionPoints, byte[] encoding) {
        if (distributionPoints == null || distributionPoints.size() == 0) {
            throw new IllegalArgumentException("distributionPoints are empty");
        }
        this.distributionPoints = distributionPoints;
        this.encoding = encoding;
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }

    public static CRLDistributionPoints decode(byte[] encoding) throws IOException {
        return (CRLDistributionPoints) ASN1.decode(encoding);
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("CRL Distribution Points: [\n");
        int number = 0;
        for (DistributionPoint distributionPoint : this.distributionPoints) {
            number++;
            sb.append(prefix).append("  [").append(number).append("]\n");
            distributionPoint.dumpValue(sb, prefix + "  ");
        }
        sb.append(prefix).append("]\n");
    }
}
