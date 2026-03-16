package org.apache.harmony.security.x509;

import java.util.Date;
import org.apache.harmony.security.asn1.ASN1Choice;
import org.apache.harmony.security.asn1.ASN1GeneralizedTime;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.ASN1UTCTime;

public final class Time {
    public static final ASN1Choice ASN1 = new ASN1Choice(new ASN1Type[]{ASN1GeneralizedTime.getInstance(), ASN1UTCTime.getInstance()}) {
        @Override
        public int getIndex(Object object) {
            return ((Date) object).getTime() < Time.JAN_01_2050 ? 1 : 0;
        }

        @Override
        public Object getObjectToEncode(Object object) {
            return object;
        }
    };
    private static final long JAN_01_2050 = 2524608000000L;
}
