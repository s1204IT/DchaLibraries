package sun.security.x509;

import java.io.IOException;
import java.util.Enumeration;
import sun.security.util.BitArray;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class ReasonFlags {
    private boolean[] bitString;
    public static final String UNUSED = "unused";
    public static final String KEY_COMPROMISE = "key_compromise";
    public static final String CA_COMPROMISE = "ca_compromise";
    public static final String AFFILIATION_CHANGED = "affiliation_changed";
    public static final String SUPERSEDED = "superseded";
    public static final String CESSATION_OF_OPERATION = "cessation_of_operation";
    public static final String CERTIFICATE_HOLD = "certificate_hold";
    public static final String PRIVILEGE_WITHDRAWN = "privilege_withdrawn";
    public static final String AA_COMPROMISE = "aa_compromise";
    private static final String[] NAMES = {UNUSED, KEY_COMPROMISE, CA_COMPROMISE, AFFILIATION_CHANGED, SUPERSEDED, CESSATION_OF_OPERATION, CERTIFICATE_HOLD, PRIVILEGE_WITHDRAWN, AA_COMPROMISE};

    private static int name2Index(String str) throws IOException {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(str)) {
                return i;
            }
        }
        throw new IOException("Name not recognized by ReasonFlags");
    }

    private boolean isSet(int i) {
        return i < this.bitString.length && this.bitString[i];
    }

    private void set(int i, boolean z) {
        if (i >= this.bitString.length) {
            boolean[] zArr = new boolean[i + 1];
            System.arraycopy((Object) this.bitString, 0, (Object) zArr, 0, this.bitString.length);
            this.bitString = zArr;
        }
        this.bitString[i] = z;
    }

    public ReasonFlags(byte[] bArr) {
        this.bitString = new BitArray(bArr.length * 8, bArr).toBooleanArray();
    }

    public ReasonFlags(boolean[] zArr) {
        this.bitString = zArr;
    }

    public ReasonFlags(BitArray bitArray) {
        this.bitString = bitArray.toBooleanArray();
    }

    public ReasonFlags(DerInputStream derInputStream) throws IOException {
        this.bitString = derInputStream.getDerValue().getUnalignedBitString(true).toBooleanArray();
    }

    public ReasonFlags(DerValue derValue) throws IOException {
        this.bitString = derValue.getUnalignedBitString(true).toBooleanArray();
    }

    public boolean[] getFlags() {
        return this.bitString;
    }

    public void set(String str, Object obj) throws IOException {
        if (!(obj instanceof Boolean)) {
            throw new IOException("Attribute must be of type Boolean.");
        }
        set(name2Index(str), ((Boolean) obj).booleanValue());
    }

    public Object get(String str) throws IOException {
        return Boolean.valueOf(isSet(name2Index(str)));
    }

    public void delete(String str) throws IOException {
        set(str, Boolean.FALSE);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Reason Flags [\n");
        if (isSet(0)) {
            sb.append("  Unused\n");
        }
        if (isSet(1)) {
            sb.append("  Key Compromise\n");
        }
        if (isSet(2)) {
            sb.append("  CA Compromise\n");
        }
        if (isSet(3)) {
            sb.append("  Affiliation_Changed\n");
        }
        if (isSet(4)) {
            sb.append("  Superseded\n");
        }
        if (isSet(5)) {
            sb.append("  Cessation Of Operation\n");
        }
        if (isSet(6)) {
            sb.append("  Certificate Hold\n");
        }
        if (isSet(7)) {
            sb.append("  Privilege Withdrawn\n");
        }
        if (isSet(8)) {
            sb.append("  AA Compromise\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putTruncatedUnalignedBitString(new BitArray(this.bitString));
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        for (int i = 0; i < NAMES.length; i++) {
            attributeNameEnumeration.addElement(NAMES[i]);
        }
        return attributeNameEnumeration.elements();
    }
}
