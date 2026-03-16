package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DERUniversalString extends ASN1Primitive implements ASN1String {
    private static final char[] table = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private byte[] string;

    public static DERUniversalString getInstance(Object obj) {
        if (obj == null || (obj instanceof DERUniversalString)) {
            return (DERUniversalString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERUniversalString) fromByteArray((byte[]) obj);
            } catch (Exception e) {
                throw new IllegalArgumentException("encoding error getInstance: " + e.toString());
            }
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static DERUniversalString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        return (explicit || (o instanceof DERUniversalString)) ? getInstance(o) : new DERUniversalString(((ASN1OctetString) o).getOctets());
    }

    public DERUniversalString(byte[] string) {
        this.string = string;
    }

    @Override
    public String getString() {
        StringBuffer buf = new StringBuffer("#");
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ASN1OutputStream aOut = new ASN1OutputStream(bOut);
        try {
            aOut.writeObject(this);
            byte[] string = bOut.toByteArray();
            for (int i = 0; i != string.length; i++) {
                buf.append(table[(string[i] >>> 4) & 15]);
                buf.append(table[string[i] & 15]);
            }
            return buf.toString();
        } catch (IOException e) {
            throw new RuntimeException("internal error encoding BitString");
        }
    }

    public String toString() {
        return getString();
    }

    public byte[] getOctets() {
        return this.string;
    }

    @Override
    boolean isConstructed() {
        return false;
    }

    @Override
    int encodedLength() {
        return StreamUtil.calculateBodyLength(this.string.length) + 1 + this.string.length;
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(28, getOctets());
    }

    @Override
    boolean asn1Equals(ASN1Primitive o) {
        if (o instanceof DERUniversalString) {
            return Arrays.areEqual(this.string, ((DERUniversalString) o).string);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.string);
    }
}
