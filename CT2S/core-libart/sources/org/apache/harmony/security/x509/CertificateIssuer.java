package org.apache.harmony.security.x509;

import java.io.IOException;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.x501.Name;

public final class CertificateIssuer extends ExtensionValue {
    public static final ASN1Type ASN1 = new ASN1Sequence(new ASN1Type[]{GeneralName.ASN1}) {
        @Override
        public Object getDecodedObject(BerInputStream in) {
            return ((Name) ((GeneralName) ((Object[]) in.content)[0]).getName()).getX500Principal();
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            values[0] = object;
        }
    };
    private X500Principal issuer;

    public CertificateIssuer(byte[] encoding) {
        super(encoding);
    }

    public X500Principal getIssuer() throws IOException {
        if (this.issuer == null) {
            this.issuer = (X500Principal) ASN1.decode(getEncoded());
        }
        return this.issuer;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("Certificate Issuer: ");
        if (this.issuer == null) {
            try {
                this.issuer = getIssuer();
            } catch (IOException e) {
                sb.append("Unparseable (incorrect!) extension value:\n");
                super.dumpValue(sb);
            }
        }
        sb.append(this.issuer).append('\n');
    }
}
