package org.apache.harmony.security.x509;

import java.io.IOException;
import java.util.Date;
import org.apache.harmony.security.asn1.ASN1GeneralizedTime;
import org.apache.harmony.security.asn1.ASN1Type;

public final class InvalidityDate extends ExtensionValue {
    public static final ASN1Type ASN1 = ASN1GeneralizedTime.getInstance();
    private final Date date;

    public InvalidityDate(byte[] encoding) throws IOException {
        super(encoding);
        this.date = (Date) ASN1.decode(encoding);
    }

    public InvalidityDate(Date date) {
        this.date = (Date) date.clone();
    }

    public Date getDate() {
        return this.date;
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this.date);
        }
        return this.encoding;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("Invalidity Date: [ ").append(this.date).append(" ]\n");
    }
}
