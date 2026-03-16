package org.apache.harmony.security.x509.tsp;

import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.pkcs7.ContentInfo;

public class TimeStampResp {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{PKIStatusInfo.ASN1, ContentInfo.ASN1}) {
        {
            setOptional(1);
        }

        @Override
        protected Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            return new TimeStampResp((PKIStatusInfo) values[0], (ContentInfo) values[1]);
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            TimeStampResp resp = (TimeStampResp) object;
            values[0] = resp.status;
            values[1] = resp.timeStampToken;
        }
    };
    private final PKIStatusInfo status;
    private final ContentInfo timeStampToken;

    public TimeStampResp(PKIStatusInfo status, ContentInfo timeStampToken) {
        this.status = status;
        this.timeStampToken = timeStampToken;
    }

    public String toString() {
        return "-- TimeStampResp:\nstatus:  " + this.status + "\ntimeStampToken:  " + this.timeStampToken + "\n-- TimeStampResp End\n";
    }

    public PKIStatusInfo getStatus() {
        return this.status;
    }

    public ContentInfo getTimeStampToken() {
        return this.timeStampToken;
    }
}
