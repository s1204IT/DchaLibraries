package org.apache.harmony.security.x509.tsp;

import java.math.BigInteger;
import java.util.List;
import org.apache.harmony.security.asn1.ASN1BitString;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.BitString;

public class PKIStatusInfo {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{ASN1Integer.getInstance(), new ASN1SequenceOf(ASN1StringType.UTF8STRING), ASN1BitString.getInstance()}) {
        {
            setOptional(1);
            setOptional(2);
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            PKIStatusInfo psi = (PKIStatusInfo) object;
            values[0] = BigInteger.valueOf(psi.status.getStatus()).toByteArray();
            values[1] = psi.statusString;
            if (psi.failInfo != null) {
                boolean[] failInfoBoolArray = new boolean[PKIFailureInfo.getMaxValue()];
                failInfoBoolArray[psi.failInfo.getValue()] = true;
                values[2] = new BitString(failInfoBoolArray);
                return;
            }
            values[2] = null;
        }

        @Override
        protected Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            int failInfoValue = -1;
            if (values[2] != null) {
                boolean[] failInfoBoolArray = ((BitString) values[2]).toBooleanArray();
                int i = 0;
                while (true) {
                    if (i >= failInfoBoolArray.length) {
                        break;
                    }
                    if (!failInfoBoolArray[i]) {
                        i++;
                    } else {
                        failInfoValue = i;
                        break;
                    }
                }
            }
            return new PKIStatusInfo(PKIStatus.getInstance(ASN1Integer.toIntValue(values[0])), (List) values[1], PKIFailureInfo.getInstance(failInfoValue));
        }
    };
    private final PKIFailureInfo failInfo;
    private final PKIStatus status;
    private final List statusString;

    public PKIStatusInfo(PKIStatus pKIStatus, List statusString, PKIFailureInfo failInfo) {
        this.status = pKIStatus;
        this.statusString = statusString;
        this.failInfo = failInfo;
    }

    public String toString() {
        return "-- PKIStatusInfo:\nPKIStatus : " + this.status + "\nstatusString:  " + this.statusString + "\nfailInfo:  " + this.failInfo + "\n-- PKIStatusInfo End\n";
    }

    public PKIFailureInfo getFailInfo() {
        return this.failInfo;
    }

    public PKIStatus getStatus() {
        return this.status;
    }

    public List getStatusString() {
        return this.statusString;
    }
}
