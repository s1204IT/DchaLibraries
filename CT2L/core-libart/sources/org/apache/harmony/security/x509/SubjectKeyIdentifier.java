package org.apache.harmony.security.x509;

import java.io.IOException;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.utils.Array;

public final class SubjectKeyIdentifier extends ExtensionValue {
    private final byte[] keyIdentifier;

    public SubjectKeyIdentifier(byte[] keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
    }

    public static SubjectKeyIdentifier decode(byte[] encoding) throws IOException {
        SubjectKeyIdentifier res = new SubjectKeyIdentifier((byte[]) ASN1OctetString.getInstance().decode(encoding));
        res.encoding = encoding;
        return res;
    }

    public byte[] getKeyIdentifier() {
        return this.keyIdentifier;
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1OctetString.getInstance().encode(this.keyIdentifier);
        }
        return this.encoding;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("SubjectKeyIdentifier: [\n");
        sb.append(Array.toString(this.keyIdentifier, prefix));
        sb.append(prefix).append("]\n");
    }
}
