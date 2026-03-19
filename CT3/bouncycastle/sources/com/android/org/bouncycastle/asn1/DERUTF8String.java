package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERUTF8String extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static com.android.org.bouncycastle.asn1.DERUTF8String getInstance(java.lang.Object r4) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.asn1.DERUTF8String.getInstance(java.lang.Object):com.android.org.bouncycastle.asn1.DERUTF8String");
    }

    public static DERUTF8String getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERUTF8String)) {
            return getInstance(o);
        }
        return new DERUTF8String(ASN1OctetString.getInstance(o).getOctets());
    }

    DERUTF8String(byte[] string) {
        this.string = string;
    }

    public DERUTF8String(String string) {
        this.string = Strings.toUTF8ByteArray(string);
    }

    @Override
    public String getString() {
        return Strings.fromUTF8ByteArray(this.string);
    }

    public String toString() {
        return getString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERUTF8String)) {
            return false;
        }
        return Arrays.areEqual(this.string, aSN1Primitive.string);
    }

    @Override
    boolean isConstructed() {
        return false;
    }

    @Override
    int encodedLength() throws IOException {
        return StreamUtil.calculateBodyLength(this.string.length) + 1 + this.string.length;
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(12, this.string);
    }
}
