package sun.security.x509;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class GeneralSubtrees implements Cloneable {
    private static final int NAME_DIFF_TYPE = -1;
    private static final int NAME_MATCH = 0;
    private static final int NAME_NARROWS = 1;
    private static final int NAME_SAME_TYPE = 3;
    private static final int NAME_WIDENS = 2;
    private final List<GeneralSubtree> trees;

    public GeneralSubtrees() {
        this.trees = new ArrayList();
    }

    private GeneralSubtrees(GeneralSubtrees generalSubtrees) {
        this.trees = new ArrayList(generalSubtrees.trees);
    }

    public GeneralSubtrees(DerValue derValue) throws IOException {
        this();
        if (derValue.tag != 48) {
            throw new IOException("Invalid encoding of GeneralSubtrees.");
        }
        while (derValue.data.available() != 0) {
            add(new GeneralSubtree(derValue.data.getDerValue()));
        }
    }

    public GeneralSubtree get(int i) {
        return this.trees.get(i);
    }

    public void remove(int i) {
        this.trees.remove(i);
    }

    public void add(GeneralSubtree generalSubtree) {
        if (generalSubtree == null) {
            throw new NullPointerException();
        }
        this.trees.add(generalSubtree);
    }

    public boolean contains(GeneralSubtree generalSubtree) {
        if (generalSubtree == null) {
            throw new NullPointerException();
        }
        return this.trees.contains(generalSubtree);
    }

    public int size() {
        return this.trees.size();
    }

    public Iterator<GeneralSubtree> iterator() {
        return this.trees.iterator();
    }

    public List<GeneralSubtree> trees() {
        return this.trees;
    }

    public Object clone() {
        return new GeneralSubtrees(this);
    }

    public String toString() {
        return "   GeneralSubtrees:\n" + this.trees.toString() + "\n";
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        int size = size();
        for (int i = 0; i < size; i++) {
            get(i).encode(derOutputStream2);
        }
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeneralSubtrees)) {
            return false;
        }
        return this.trees.equals(((GeneralSubtrees) obj).trees);
    }

    public int hashCode() {
        return this.trees.hashCode();
    }

    private GeneralNameInterface getGeneralNameInterface(int i) {
        return getGeneralNameInterface(get(i));
    }

    private static GeneralNameInterface getGeneralNameInterface(GeneralSubtree generalSubtree) {
        return generalSubtree.getName().getName();
    }

    private void minimize() {
        boolean z;
        int i = 0;
        while (i < size() - 1) {
            GeneralNameInterface generalNameInterface = getGeneralNameInterface(i);
            int i2 = i + 1;
            while (true) {
                if (i2 < size()) {
                    switch (generalNameInterface.constrains(getGeneralNameInterface(i2))) {
                        case -1:
                        case 3:
                            i2++;
                            break;
                        case 0:
                        case 2:
                            z = true;
                            break;
                        case 1:
                            remove(i2);
                            i2--;
                            i2++;
                            break;
                        default:
                            z = false;
                            break;
                    }
                } else {
                    z = false;
                }
            }
            if (z) {
                remove(i);
                i--;
            }
            i++;
        }
    }

    private GeneralSubtree createWidestSubtree(GeneralNameInterface generalNameInterface) {
        GeneralName generalName;
        try {
            switch (generalNameInterface.getType()) {
                case 0:
                    generalName = new GeneralName(new OtherName(((OtherName) generalNameInterface).getOID(), null));
                    break;
                case 1:
                    generalName = new GeneralName(new RFC822Name(""));
                    break;
                case 2:
                    generalName = new GeneralName(new DNSName(""));
                    break;
                case 3:
                    generalName = new GeneralName(new X400Address((byte[]) null));
                    break;
                case 4:
                    generalName = new GeneralName(new X500Name(""));
                    break;
                case 5:
                    generalName = new GeneralName(new EDIPartyName(""));
                    break;
                case 6:
                    generalName = new GeneralName(new URIName(""));
                    break;
                case 7:
                    generalName = new GeneralName(new IPAddressName((byte[]) null));
                    break;
                case 8:
                    generalName = new GeneralName(new OIDName(new ObjectIdentifier((int[]) null)));
                    break;
                default:
                    throw new IOException("Unsupported GeneralNameInterface type: " + generalNameInterface.getType());
            }
            return new GeneralSubtree(generalName, 0, -1);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error: " + ((Object) e), e);
        }
    }

    public GeneralSubtrees intersect(GeneralSubtrees generalSubtrees) {
        if (generalSubtrees == null) {
            throw new NullPointerException("other GeneralSubtrees must not be null");
        }
        GeneralSubtrees generalSubtrees2 = new GeneralSubtrees();
        if (size() == 0) {
            union(generalSubtrees);
            return null;
        }
        minimize();
        generalSubtrees.minimize();
        GeneralSubtrees generalSubtrees3 = null;
        int i = 0;
        while (i < size()) {
            GeneralNameInterface generalNameInterface = getGeneralNameInterface(i);
            boolean z = false;
            for (int i2 = 0; i2 < generalSubtrees.size(); i2++) {
                GeneralSubtree generalSubtree = generalSubtrees.get(i2);
                switch (generalNameInterface.constrains(getGeneralNameInterface(generalSubtree))) {
                    case 0:
                    case 2:
                        z = false;
                        break;
                    case 1:
                        remove(i);
                        i--;
                        generalSubtrees2.add(generalSubtree);
                        z = false;
                        break;
                    case 3:
                        z = true;
                        break;
                    default:
                        break;
                }
                if (!z) {
                    boolean z2 = false;
                    for (int i3 = 0; i3 < size(); i3++) {
                        GeneralNameInterface generalNameInterface2 = getGeneralNameInterface(i3);
                        if (generalNameInterface2.getType() == generalNameInterface.getType()) {
                            for (int i4 = 0; i4 < generalSubtrees.size(); i4++) {
                                int iConstrains = generalNameInterface2.constrains(generalSubtrees.getGeneralNameInterface(i4));
                                if (iConstrains == 0 || iConstrains == 2 || iConstrains == 1) {
                                    z2 = true;
                                }
                            }
                        }
                    }
                    if (!z2) {
                        if (generalSubtrees3 == null) {
                            generalSubtrees3 = new GeneralSubtrees();
                        }
                        GeneralSubtree generalSubtreeCreateWidestSubtree = createWidestSubtree(generalNameInterface);
                        if (!generalSubtrees3.contains(generalSubtreeCreateWidestSubtree)) {
                            generalSubtrees3.add(generalSubtreeCreateWidestSubtree);
                        }
                    }
                    remove(i);
                    i--;
                }
                i++;
            }
            if (!z) {
            }
            i++;
        }
        if (generalSubtrees2.size() > 0) {
            union(generalSubtrees2);
        }
        for (int i5 = 0; i5 < generalSubtrees.size(); i5++) {
            GeneralSubtree generalSubtree2 = generalSubtrees.get(i5);
            GeneralNameInterface generalNameInterface3 = getGeneralNameInterface(generalSubtree2);
            int i6 = 0;
            boolean z3 = false;
            while (true) {
                if (i6 < size()) {
                    switch (getGeneralNameInterface(i6).constrains(generalNameInterface3)) {
                        case -1:
                            z3 = true;
                            i6++;
                            break;
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            z3 = false;
                            break;
                        default:
                            i6++;
                            break;
                    }
                }
            }
            if (z3) {
                add(generalSubtree2);
            }
        }
        return generalSubtrees3;
    }

    public void union(GeneralSubtrees generalSubtrees) {
        if (generalSubtrees != null) {
            int size = generalSubtrees.size();
            for (int i = 0; i < size; i++) {
                add(generalSubtrees.get(i));
            }
            minimize();
        }
    }

    public void reduce(GeneralSubtrees generalSubtrees) {
        if (generalSubtrees == null) {
            return;
        }
        int size = generalSubtrees.size();
        for (int i = 0; i < size; i++) {
            GeneralNameInterface generalNameInterface = generalSubtrees.getGeneralNameInterface(i);
            int i2 = 0;
            while (i2 < size()) {
                switch (generalNameInterface.constrains(getGeneralNameInterface(i2))) {
                    case 0:
                        remove(i2);
                        i2--;
                        break;
                    case 1:
                        remove(i2);
                        i2--;
                        break;
                }
                i2++;
            }
        }
    }
}
