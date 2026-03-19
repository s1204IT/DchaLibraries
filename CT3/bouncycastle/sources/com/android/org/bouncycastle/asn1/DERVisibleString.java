package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERVisibleString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static com.android.org.bouncycastle.asn1.DERVisibleString getInstance(java.lang.Object r4) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.asn1.DERVisibleString.getInstance(java.lang.Object):com.android.org.bouncycastle.asn1.DERVisibleString");
    }

    public static DERVisibleString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERVisibleString)) {
            return getInstance(o);
        }
        return new DERVisibleString(ASN1OctetString.getInstance(o).getOctets());
    }

    DERVisibleString(byte[] string) {
        this.string = string;
    }

    public DERVisibleString(String string) {
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
        out.writeEncoded(26, this.string);
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERVisibleString)) {
            return false;
        }
        return Arrays.areEqual(this.string, aSN1Primitive.string);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.string);
    }
}
