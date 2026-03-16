package org.apache.harmony.security.x509;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class GeneralSubtrees {
    public static final ASN1Type ASN1 = new ASN1SequenceOf(GeneralSubtree.ASN1) {
        @Override
        public Object getDecodedObject(BerInputStream in) {
            return new GeneralSubtrees((List) in.content);
        }

        @Override
        public Collection getValues(Object object) {
            GeneralSubtrees gss = (GeneralSubtrees) object;
            return gss.generalSubtrees == null ? new ArrayList() : gss.generalSubtrees;
        }
    };
    private byte[] encoding;
    private List<GeneralSubtree> generalSubtrees;

    public GeneralSubtrees(List<GeneralSubtree> generalSubtrees) {
        this.generalSubtrees = generalSubtrees;
    }

    public List<GeneralSubtree> getSubtrees() {
        return this.generalSubtrees;
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }
}
