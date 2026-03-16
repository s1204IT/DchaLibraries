package org.apache.harmony.security.x509;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class GeneralNames {
    public static final ASN1Type ASN1 = new ASN1SequenceOf(GeneralName.ASN1) {
        @Override
        public Object getDecodedObject(BerInputStream in) {
            return new GeneralNames((List) in.content, in.getEncoded());
        }

        @Override
        public Collection getValues(Object object) {
            GeneralNames gns = (GeneralNames) object;
            return gns.generalNames;
        }
    };
    private byte[] encoding;
    private List<GeneralName> generalNames;

    public GeneralNames() {
        this.generalNames = new ArrayList();
    }

    public GeneralNames(List<GeneralName> generalNames) {
        this.generalNames = generalNames;
    }

    private GeneralNames(List<GeneralName> generalNames, byte[] encoding) {
        this.generalNames = generalNames;
        this.encoding = encoding;
    }

    public List<GeneralName> getNames() {
        if (this.generalNames == null || this.generalNames.size() == 0) {
            return null;
        }
        return new ArrayList(this.generalNames);
    }

    public Collection<List<?>> getPairsList() {
        Collection<List<?>> result = new ArrayList<>();
        if (this.generalNames != null) {
            for (GeneralName generalName : this.generalNames) {
                try {
                    result.add(generalName.getAsList());
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return result;
    }

    public void addName(GeneralName name) {
        this.encoding = null;
        if (this.generalNames == null) {
            this.generalNames = new ArrayList();
        }
        this.generalNames.add(name);
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }

    public void dumpValue(StringBuilder sb, String prefix) {
        if (this.generalNames != null) {
            for (GeneralName generalName : this.generalNames) {
                sb.append(prefix);
                sb.append(generalName);
                sb.append('\n');
            }
        }
    }
}
