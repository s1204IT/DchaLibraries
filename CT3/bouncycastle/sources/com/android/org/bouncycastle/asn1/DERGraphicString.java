package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERGraphicString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static com.android.org.bouncycastle.asn1.DERGraphicString getInstance(java.lang.Object r4) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.asn1.DERGraphicString.getInstance(java.lang.Object):com.android.org.bouncycastle.asn1.DERGraphicString");
    }

    public static DERGraphicString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERGraphicString)) {
            return getInstance(o);
        }
        return new DERGraphicString(((ASN1OctetString) o).getOctets());
    }

    public DERGraphicString(byte[] string) {
        this.string = Arrays.clone(string);
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
        out.writeEncoded(25, this.string);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERGraphicString)) {
            return false;
        }
        return Arrays.areEqual(this.string, aSN1Primitive.string);
    }

    @Override
    public String getString() {
        return Strings.fromByteArray(this.string);
    }
}
