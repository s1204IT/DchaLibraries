package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERIA5String extends ASN1Primitive implements ASN1String {
    private byte[] string;

    public static DERIA5String getInstance(Object obj) {
        if (obj == null || (obj instanceof DERIA5String)) {
            return (DERIA5String) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERIA5String) fromByteArray((byte[]) obj);
            } catch (Exception e) {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static DERIA5String getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        return (explicit || (o instanceof DERIA5String)) ? getInstance(o) : new DERIA5String(((ASN1OctetString) o).getOctets());
    }

    DERIA5String(byte[] string) {
        this.string = string;
    }

    public DERIA5String(String string) {
        this(string, false);
    }

    public DERIA5String(String string, boolean validate) {
        if (string == null) {
            throw new NullPointerException("string cannot be null");
        }
        if (validate && !isIA5String(string)) {
            throw new IllegalArgumentException("string contains illegal characters");
        }
        this.string = Strings.toByteArray(string);
    }

    @Override
    public String getString() {
        return Strings.fromByteArray(this.string);
    }

    public String toString() {
        return getString();
    }

    public byte[] getOctets() {
        return Arrays.clone(this.string);
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
        out.writeEncoded(22, this.string);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    @Override
    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof DERIA5String)) {
            return false;
        }
        DERIA5String s = (DERIA5String) o;
        return Arrays.areEqual(this.string, s.string);
    }

    public static boolean isIA5String(String str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            char ch = str.charAt(i);
            if (ch > 127) {
                return false;
            }
        }
        return true;
    }
}
