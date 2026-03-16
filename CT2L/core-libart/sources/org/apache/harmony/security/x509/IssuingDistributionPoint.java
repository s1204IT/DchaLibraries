package org.apache.harmony.security.x509;

import java.io.IOException;
import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Explicit;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class IssuingDistributionPoint extends ExtensionValue {
    public static final ASN1Type ASN1 = new ASN1Sequence(new ASN1Type[]{new ASN1Explicit(0, DistributionPointName.ASN1), new ASN1Implicit(1, ASN1Boolean.getInstance()), new ASN1Implicit(2, ASN1Boolean.getInstance()), new ASN1Implicit(3, ReasonFlags.ASN1), new ASN1Implicit(4, ASN1Boolean.getInstance()), new ASN1Implicit(5, ASN1Boolean.getInstance())}) {
        {
            setOptional(0);
            setOptional(3);
            setDefault(Boolean.FALSE, 1);
            setDefault(Boolean.FALSE, 2);
            setDefault(Boolean.FALSE, 4);
            setDefault(Boolean.FALSE, 5);
        }

        @Override
        protected Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            IssuingDistributionPoint idp = new IssuingDistributionPoint((DistributionPointName) values[0], (ReasonFlags) values[3]);
            idp.encoding = in.getEncoded();
            if (values[1] != null) {
                idp.setOnlyContainsUserCerts(((Boolean) values[1]).booleanValue());
            }
            if (values[2] != null) {
                idp.setOnlyContainsCACerts(((Boolean) values[2]).booleanValue());
            }
            if (values[4] != null) {
                idp.setIndirectCRL(((Boolean) values[4]).booleanValue());
            }
            if (values[5] != null) {
                idp.setOnlyContainsAttributeCerts(((Boolean) values[5]).booleanValue());
            }
            return idp;
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            IssuingDistributionPoint idp = (IssuingDistributionPoint) object;
            values[0] = idp.distributionPoint;
            values[1] = idp.onlyContainsUserCerts ? Boolean.TRUE : null;
            values[2] = idp.onlyContainsCACerts ? Boolean.TRUE : null;
            values[3] = idp.onlySomeReasons;
            values[4] = idp.indirectCRL ? Boolean.TRUE : null;
            values[5] = idp.onlyContainsAttributeCerts ? Boolean.TRUE : null;
        }
    };
    private DistributionPointName distributionPoint;
    private ReasonFlags onlySomeReasons;
    private boolean onlyContainsUserCerts = false;
    private boolean onlyContainsCACerts = false;
    private boolean indirectCRL = false;
    private boolean onlyContainsAttributeCerts = false;

    public IssuingDistributionPoint(DistributionPointName distributionPoint, ReasonFlags onlySomeReasons) {
        this.distributionPoint = distributionPoint;
        this.onlySomeReasons = onlySomeReasons;
    }

    public static IssuingDistributionPoint decode(byte[] encoding) throws IOException {
        IssuingDistributionPoint idp = (IssuingDistributionPoint) ASN1.decode(encoding);
        idp.encoding = encoding;
        return idp;
    }

    public void setOnlyContainsUserCerts(boolean onlyContainsUserCerts) {
        this.onlyContainsUserCerts = onlyContainsUserCerts;
    }

    public void setOnlyContainsCACerts(boolean onlyContainsCACerts) {
        this.onlyContainsCACerts = onlyContainsCACerts;
    }

    public void setIndirectCRL(boolean indirectCRL) {
        this.indirectCRL = indirectCRL;
    }

    public void setOnlyContainsAttributeCerts(boolean onlyContainsAttributeCerts) {
        this.onlyContainsAttributeCerts = onlyContainsAttributeCerts;
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
        sb.append(prefix).append("Issuing Distribution Point: [\n");
        if (this.distributionPoint != null) {
            this.distributionPoint.dumpValue(sb, "  " + prefix);
        }
        sb.append(prefix).append("  onlyContainsUserCerts: ").append(this.onlyContainsUserCerts).append('\n');
        sb.append(prefix).append("  onlyContainsCACerts: ").append(this.onlyContainsCACerts).append('\n');
        if (this.onlySomeReasons != null) {
            this.onlySomeReasons.dumpValue(sb, prefix + "  ");
        }
        sb.append(prefix).append("  indirectCRL: ").append(this.indirectCRL).append('\n');
        sb.append(prefix).append("  onlyContainsAttributeCerts: ").append(this.onlyContainsAttributeCerts).append('\n');
    }
}
