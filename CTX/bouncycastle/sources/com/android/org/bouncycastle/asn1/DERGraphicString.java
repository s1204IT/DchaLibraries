package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERGraphicString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static DERGraphicString getInstance(Object obj) {
        if (obj == null || (obj instanceof DERGraphicString)) {
            return (DERGraphicString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERGraphicString) fromByteArray((byte[]) obj);
            } catch (Exception e) {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static DERGraphicString getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        if (z || (object instanceof DERGraphicString)) {
            return getInstance(object);
        }
        return new DERGraphicString(((ASN1OctetString) object).getOctets());
    }

    public DERGraphicString(byte[] bArr) {
        this.string = Arrays.clone(bArr);
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
        return 1 + StreamUtil.calculateBodyLength(this.string.length) + this.string.length;
    }

    @Override
    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(25, this.string);
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
        return Arrays.areEqual(this.string, ((DERGraphicString) aSN1Primitive).string);
    }

    @Override
    public String getString() {
        return Strings.fromByteArray(this.string);
    }
}
