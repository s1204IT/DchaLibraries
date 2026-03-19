package sun.security.x509;

import java.io.IOException;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class EDIPartyName implements GeneralNameInterface {
    private static final byte TAG_ASSIGNER = 0;
    private static final byte TAG_PARTYNAME = 1;
    private String assigner;
    private int myhash;
    private String party;

    public EDIPartyName(String str, String str2) {
        this.assigner = null;
        this.party = null;
        this.myhash = -1;
        this.assigner = str;
        this.party = str2;
    }

    public EDIPartyName(String str) {
        this.assigner = null;
        this.party = null;
        this.myhash = -1;
        this.party = str;
    }

    public EDIPartyName(DerValue derValue) throws IOException {
        this.assigner = null;
        this.party = null;
        this.myhash = -1;
        DerValue[] sequence = new DerInputStream(derValue.toByteArray()).getSequence(2);
        int length = sequence.length;
        if (length < 1 || length > 2) {
            throw new IOException("Invalid encoding of EDIPartyName");
        }
        for (int i = 0; i < length; i++) {
            DerValue derValue2 = sequence[i];
            if (derValue2.isContextSpecific((byte) 0) && !derValue2.isConstructed()) {
                if (this.assigner != null) {
                    throw new IOException("Duplicate nameAssigner found in EDIPartyName");
                }
                derValue2 = derValue2.data.getDerValue();
                this.assigner = derValue2.getAsString();
            }
            if (derValue2.isContextSpecific((byte) 1) && !derValue2.isConstructed()) {
                if (this.party != null) {
                    throw new IOException("Duplicate partyName found in EDIPartyName");
                }
                this.party = derValue2.data.getDerValue().getAsString();
            }
        }
    }

    @Override
    public int getType() {
        return 5;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        DerOutputStream derOutputStream3 = new DerOutputStream();
        if (this.assigner != null) {
            DerOutputStream derOutputStream4 = new DerOutputStream();
            derOutputStream4.putPrintableString(this.assigner);
            derOutputStream2.write(DerValue.createTag((byte) -128, false, (byte) 0), derOutputStream4);
        }
        if (this.party == null) {
            throw new IOException("Cannot have null partyName");
        }
        derOutputStream3.putPrintableString(this.party);
        derOutputStream2.write(DerValue.createTag((byte) -128, false, (byte) 1), derOutputStream3);
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    public String getAssignerName() {
        return this.assigner;
    }

    public String getPartyName() {
        return this.party;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EDIPartyName)) {
            return false;
        }
        EDIPartyName eDIPartyName = (EDIPartyName) obj;
        String str = eDIPartyName.assigner;
        if (this.assigner == null) {
            if (str != null) {
                return false;
            }
        } else if (!this.assigner.equals(str)) {
            return false;
        }
        String str2 = eDIPartyName.party;
        return this.party == null ? str2 == null : this.party.equals(str2);
    }

    public int hashCode() {
        if (this.myhash == -1) {
            this.myhash = (this.party == null ? 1 : this.party.hashCode()) + 37;
            if (this.assigner != null) {
                this.myhash = (37 * this.myhash) + this.assigner.hashCode();
            }
        }
        return this.myhash;
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("EDIPartyName: ");
        if (this.assigner == null) {
            str = "";
        } else {
            str = "  nameAssigner = " + this.assigner + ",";
        }
        sb.append(str);
        sb.append("  partyName = ");
        sb.append(this.party);
        return sb.toString();
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        if (generalNameInterface != null && generalNameInterface.getType() == 5) {
            throw new UnsupportedOperationException("Narrowing, widening, and matching of names not supported for EDIPartyName");
        }
        return -1;
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("subtreeDepth() not supported for EDIPartyName");
    }
}
