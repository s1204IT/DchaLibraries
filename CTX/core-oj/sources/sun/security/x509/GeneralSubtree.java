package sun.security.x509;

import java.io.IOException;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class GeneralSubtree {
    private static final int MIN_DEFAULT = 0;
    private static final byte TAG_MAX = 1;
    private static final byte TAG_MIN = 0;
    private int maximum;
    private int minimum;
    private int myhash = -1;
    private GeneralName name;

    public GeneralSubtree(GeneralName generalName, int i, int i2) {
        this.minimum = 0;
        this.maximum = -1;
        this.name = generalName;
        this.minimum = i;
        this.maximum = i2;
    }

    public GeneralSubtree(DerValue derValue) throws IOException {
        this.minimum = 0;
        this.maximum = -1;
        if (derValue.tag != 48) {
            throw new IOException("Invalid encoding for GeneralSubtree.");
        }
        this.name = new GeneralName(derValue.data.getDerValue(), true);
        while (derValue.data.available() != 0) {
            DerValue derValue2 = derValue.data.getDerValue();
            if (derValue2.isContextSpecific((byte) 0) && !derValue2.isConstructed()) {
                derValue2.resetTag((byte) 2);
                this.minimum = derValue2.getInteger();
            } else if (derValue2.isContextSpecific((byte) 1) && !derValue2.isConstructed()) {
                derValue2.resetTag((byte) 2);
                this.maximum = derValue2.getInteger();
            } else {
                throw new IOException("Invalid encoding of GeneralSubtree.");
            }
        }
    }

    public GeneralName getName() {
        return this.name;
    }

    public int getMinimum() {
        return this.minimum;
    }

    public int getMaximum() {
        return this.maximum;
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("\n   GeneralSubtree: [\n    GeneralName: ");
        sb.append(this.name == null ? "" : this.name.toString());
        sb.append("\n    Minimum: ");
        sb.append(this.minimum);
        String string = sb.toString();
        if (this.maximum == -1) {
            str = string + "\t    Maximum: undefined";
        } else {
            str = string + "\t    Maximum: " + this.maximum;
        }
        return str + "    ]\n";
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof GeneralSubtree)) {
            return false;
        }
        GeneralSubtree generalSubtree = (GeneralSubtree) obj;
        if (this.name == null) {
            if (generalSubtree.name != null) {
                return false;
            }
        } else if (!this.name.equals(generalSubtree.name)) {
            return false;
        }
        return this.minimum == generalSubtree.minimum && this.maximum == generalSubtree.maximum;
    }

    public int hashCode() {
        if (this.myhash == -1) {
            this.myhash = 17;
            if (this.name != null) {
                this.myhash = (this.myhash * 37) + this.name.hashCode();
            }
            if (this.minimum != 0) {
                this.myhash = (this.myhash * 37) + this.minimum;
            }
            if (this.maximum != -1) {
                this.myhash = (37 * this.myhash) + this.maximum;
            }
        }
        return this.myhash;
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        this.name.encode(derOutputStream2);
        if (this.minimum != 0) {
            DerOutputStream derOutputStream3 = new DerOutputStream();
            derOutputStream3.putInteger(this.minimum);
            derOutputStream2.writeImplicit(DerValue.createTag((byte) -128, false, (byte) 0), derOutputStream3);
        }
        if (this.maximum != -1) {
            DerOutputStream derOutputStream4 = new DerOutputStream();
            derOutputStream4.putInteger(this.maximum);
            derOutputStream2.writeImplicit(DerValue.createTag((byte) -128, false, (byte) 1), derOutputStream4);
        }
        derOutputStream.write((byte) 48, derOutputStream2);
    }
}
