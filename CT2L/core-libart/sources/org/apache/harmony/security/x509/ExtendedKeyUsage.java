package org.apache.harmony.security.x509;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.harmony.security.asn1.ASN1Oid;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.ObjectIdentifier;

public final class ExtendedKeyUsage extends ExtensionValue {
    public static final ASN1Type ASN1 = new ASN1SequenceOf(new ASN1Oid() {
        @Override
        public Object getDecodedObject(BerInputStream in) throws IOException {
            int[] oid = (int[]) super.getDecodedObject(in);
            return ObjectIdentifier.toString(oid);
        }
    });
    private List<String> keys;

    public ExtendedKeyUsage(byte[] encoding) {
        super(encoding);
    }

    public List<String> getExtendedKeyUsage() throws IOException {
        if (this.keys == null) {
            this.keys = (List) ASN1.decode(getEncoded());
        }
        return this.keys;
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this.keys);
        }
        return this.encoding;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("Extended Key Usage: ");
        if (this.keys == null) {
            try {
                this.keys = getExtendedKeyUsage();
            } catch (IOException e) {
                super.dumpValue(sb);
                return;
            }
        }
        sb.append('[');
        Iterator<?> it = this.keys.iterator();
        while (it.hasNext()) {
            sb.append(" \"").append((Object) it.next()).append('\"');
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(" ]\n");
    }
}
