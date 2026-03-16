package org.apache.harmony.security.x509;

import java.io.IOException;
import java.math.BigInteger;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Type;

public final class CRLNumber extends ExtensionValue {
    public static final ASN1Type ASN1 = ASN1Integer.getInstance();
    private final BigInteger number;

    public CRLNumber(byte[] encoding) throws IOException {
        super(encoding);
        this.number = new BigInteger((byte[]) ASN1.decode(encoding));
    }

    public BigInteger getNumber() {
        return this.number;
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this.number.toByteArray());
        }
        return this.encoding;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("CRL Number: [ ").append(this.number).append(" ]\n");
    }
}
