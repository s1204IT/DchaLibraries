package org.apache.harmony.security.x509;

import java.io.IOException;
import java.math.BigInteger;
import org.apache.harmony.security.asn1.ASN1Integer;

public final class InhibitAnyPolicy extends ExtensionValue {
    private final int skipCerts;

    public InhibitAnyPolicy(byte[] encoding) throws IOException {
        super(encoding);
        this.skipCerts = new BigInteger((byte[]) ASN1Integer.getInstance().decode(encoding)).intValue();
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1Integer.getInstance().encode(ASN1Integer.fromIntValue(this.skipCerts));
        }
        return this.encoding;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("Inhibit Any-Policy: ").append(this.skipCerts).append('\n');
    }
}
