package org.apache.harmony.security.x509;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class NameConstraints extends ExtensionValue {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{new ASN1Implicit(0, GeneralSubtrees.ASN1), new ASN1Implicit(1, GeneralSubtrees.ASN1)}) {
        {
            setOptional(0);
            setOptional(1);
        }

        @Override
        protected Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            return new NameConstraints((GeneralSubtrees) values[0], (GeneralSubtrees) values[1], in.getEncoded());
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            NameConstraints nc = (NameConstraints) object;
            values[0] = nc.permittedSubtrees;
            values[1] = nc.excludedSubtrees;
        }
    };
    private byte[] encoding;
    private final GeneralSubtrees excludedSubtrees;
    private ArrayList<GeneralName>[] excluded_names;
    private final GeneralSubtrees permittedSubtrees;
    private ArrayList<GeneralName>[] permitted_names;

    public NameConstraints(GeneralSubtrees permittedSubtrees, GeneralSubtrees excludedSubtrees) {
        List<GeneralSubtree> es;
        List<GeneralSubtree> ps;
        if (permittedSubtrees != null && ((ps = permittedSubtrees.getSubtrees()) == null || ps.isEmpty())) {
            throw new IllegalArgumentException("permittedSubtrees are empty");
        }
        if (excludedSubtrees != null && ((es = excludedSubtrees.getSubtrees()) == null || es.isEmpty())) {
            throw new IllegalArgumentException("excludedSubtrees are empty");
        }
        this.permittedSubtrees = permittedSubtrees;
        this.excludedSubtrees = excludedSubtrees;
    }

    private NameConstraints(GeneralSubtrees permittedSubtrees, GeneralSubtrees excludedSubtrees, byte[] encoding) {
        this(permittedSubtrees, excludedSubtrees);
        this.encoding = encoding;
    }

    public static NameConstraints decode(byte[] encoding) throws IOException {
        return (NameConstraints) ASN1.decode(encoding);
    }

    @Override
    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }

    private void prepareNames() {
        this.permitted_names = new ArrayList[9];
        if (this.permittedSubtrees != null) {
            for (GeneralSubtree generalSubtree : this.permittedSubtrees.getSubtrees()) {
                GeneralName name = generalSubtree.getBase();
                int tag = name.getTag();
                if (this.permitted_names[tag] == null) {
                    this.permitted_names[tag] = new ArrayList<>();
                }
                this.permitted_names[tag].add(name);
            }
        }
        this.excluded_names = new ArrayList[9];
        if (this.excludedSubtrees != null) {
            for (GeneralSubtree generalSubtree2 : this.excludedSubtrees.getSubtrees()) {
                GeneralName name2 = generalSubtree2.getBase();
                int tag2 = name2.getTag();
                if (this.excluded_names[tag2] == null) {
                    this.excluded_names[tag2] = new ArrayList<>();
                }
                this.excluded_names[tag2].add(name2);
            }
        }
    }

    private byte[] getExtensionValue(X509Certificate cert, String OID) {
        try {
            byte[] bytes = cert.getExtensionValue(OID);
            if (bytes == null) {
                return null;
            }
            return (byte[]) ASN1OctetString.getInstance().decode(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean isAcceptable(X509Certificate cert) {
        if (this.permitted_names == null) {
            prepareNames();
        }
        byte[] bytes = getExtensionValue(cert, "2.5.29.17");
        try {
            List<GeneralName> names = bytes == null ? new ArrayList<>(1) : ((GeneralNames) GeneralNames.ASN1.decode(bytes)).getNames();
            if (this.excluded_names[4] != null || this.permitted_names[4] != null) {
                try {
                    names.add(new GeneralName(4, cert.getSubjectX500Principal().getName()));
                } catch (IOException e) {
                }
            }
            return isAcceptable(names);
        } catch (IOException e2) {
            e2.printStackTrace();
            return false;
        }
    }

    public boolean isAcceptable(List<GeneralName> names) {
        if (this.permitted_names == null) {
            prepareNames();
        }
        boolean[] types_presented = new boolean[9];
        boolean[] permitted_found = new boolean[9];
        for (GeneralName name : names) {
            int type = name.getTag();
            if (this.excluded_names[type] != null) {
                for (int i = 0; i < this.excluded_names[type].size(); i++) {
                    if (this.excluded_names[type].get(i).isAcceptable(name)) {
                        return false;
                    }
                }
            }
            if (this.permitted_names[type] != null && !permitted_found[type]) {
                types_presented[type] = true;
                for (int i2 = 0; i2 < this.permitted_names[type].size(); i2++) {
                    if (this.permitted_names[type].get(i2).isAcceptable(name)) {
                        permitted_found[type] = true;
                    }
                }
            }
        }
        for (int type2 = 0; type2 < 9; type2++) {
            if (types_presented[type2] && !permitted_found[type2]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("Name Constraints: [\n");
        if (this.permittedSubtrees != null) {
            sb.append(prefix).append("  Permitted: [\n");
            for (GeneralSubtree generalSubtree : this.permittedSubtrees.getSubtrees()) {
                generalSubtree.dumpValue(sb, prefix + "    ");
            }
            sb.append(prefix).append("  ]\n");
        }
        if (this.excludedSubtrees != null) {
            sb.append(prefix).append("  Excluded: [\n");
            for (GeneralSubtree generalSubtree2 : this.excludedSubtrees.getSubtrees()) {
                generalSubtree2.dumpValue(sb, prefix + "    ");
            }
            sb.append(prefix).append("  ]\n");
        }
        sb.append('\n').append(prefix).append("]\n");
    }
}
