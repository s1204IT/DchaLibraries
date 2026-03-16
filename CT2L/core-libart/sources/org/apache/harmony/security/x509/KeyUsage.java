package org.apache.harmony.security.x509;

import java.io.IOException;
import org.apache.harmony.security.asn1.ASN1BitString;
import org.apache.harmony.security.asn1.ASN1Type;

public final class KeyUsage extends ExtensionValue {
    private final boolean[] keyUsage;
    private static final String[] USAGES = {"digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment", "keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"};
    private static final ASN1Type ASN1 = new ASN1BitString.ASN1NamedBitList(9);

    public KeyUsage(byte[] encoding) throws IOException {
        super(encoding);
        this.keyUsage = (boolean[]) ASN1.decode(encoding);
    }

    public boolean[] getKeyUsage() {
        return this.keyUsage;
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this.keyUsage);
        }
        return this.encoding;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("KeyUsage [\n");
        for (int i = 0; i < this.keyUsage.length; i++) {
            if (this.keyUsage[i]) {
                sb.append(prefix).append("  ").append(USAGES[i]).append('\n');
            }
        }
        sb.append(prefix).append("]\n");
    }
}
