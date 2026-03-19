package sun.security.x509;

import java.io.IOException;
import java.util.Objects;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class DistributionPointName {
    private static final byte TAG_FULL_NAME = 0;
    private static final byte TAG_RELATIVE_NAME = 1;
    private GeneralNames fullName;
    private volatile int hashCode;
    private RDN relativeName;

    public DistributionPointName(GeneralNames generalNames) {
        this.fullName = null;
        this.relativeName = null;
        if (generalNames == null) {
            throw new IllegalArgumentException("fullName must not be null");
        }
        this.fullName = generalNames;
    }

    public DistributionPointName(RDN rdn) {
        this.fullName = null;
        this.relativeName = null;
        if (rdn == null) {
            throw new IllegalArgumentException("relativeName must not be null");
        }
        this.relativeName = rdn;
    }

    public DistributionPointName(DerValue derValue) throws IOException {
        this.fullName = null;
        this.relativeName = null;
        if (derValue.isContextSpecific((byte) 0) && derValue.isConstructed()) {
            derValue.resetTag((byte) 48);
            this.fullName = new GeneralNames(derValue);
        } else {
            if (derValue.isContextSpecific((byte) 1) && derValue.isConstructed()) {
                derValue.resetTag((byte) 49);
                this.relativeName = new RDN(derValue);
                return;
            }
            throw new IOException("Invalid encoding for DistributionPointName");
        }
    }

    public GeneralNames getFullName() {
        return this.fullName;
    }

    public RDN getRelativeName() {
        return this.relativeName;
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        if (this.fullName != null) {
            this.fullName.encode(derOutputStream2);
            derOutputStream.writeImplicit(DerValue.createTag((byte) -128, true, (byte) 0), derOutputStream2);
        } else {
            this.relativeName.encode(derOutputStream2);
            derOutputStream.writeImplicit(DerValue.createTag((byte) -128, true, (byte) 1), derOutputStream2);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DistributionPointName)) {
            return false;
        }
        DistributionPointName distributionPointName = (DistributionPointName) obj;
        return Objects.equals(this.fullName, distributionPointName.fullName) && Objects.equals(this.relativeName, distributionPointName.relativeName);
    }

    public int hashCode() {
        int iHashCode = this.hashCode;
        if (iHashCode == 0) {
            iHashCode = this.fullName != null ? 1 + this.fullName.hashCode() : 1 + this.relativeName.hashCode();
            this.hashCode = iHashCode;
        }
        return iHashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.fullName != null) {
            sb.append("DistributionPointName:\n     " + ((Object) this.fullName) + "\n");
        } else {
            sb.append("DistributionPointName:\n     " + ((Object) this.relativeName) + "\n");
        }
        return sb.toString();
    }
}
