package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ASN1OctetString extends ASN1Primitive implements ASN1OctetStringParser {
    byte[] string;

    @Override
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static ASN1OctetString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof ASN1OctetString)) {
            return getInstance(o);
        }
        return BEROctetString.fromSequence(ASN1Sequence.getInstance(o));
    }

    public static com.android.org.bouncycastle.asn1.ASN1OctetString getInstance(java.lang.Object r5) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.asn1.ASN1OctetString.getInstance(java.lang.Object):com.android.org.bouncycastle.asn1.ASN1OctetString");
    }

    public ASN1OctetString(byte[] string) {
        if (string == null) {
            throw new NullPointerException("string cannot be null");
        }
        this.string = string;
    }

    @Override
    public InputStream getOctetStream() {
        return new ByteArrayInputStream(this.string);
    }

    public ASN1OctetStringParser parser() {
        return this;
    }

    public byte[] getOctets() {
        return this.string;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getOctets());
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1OctetString)) {
            return false;
        }
        return Arrays.areEqual(this.string, aSN1Primitive.string);
    }

    @Override
    public ASN1Primitive getLoadedObject() {
        return toASN1Primitive();
    }

    @Override
    ASN1Primitive toDERObject() {
        return new DEROctetString(this.string);
    }

    @Override
    ASN1Primitive toDLObject() {
        return new DEROctetString(this.string);
    }

    public String toString() {
        return "#" + new String(Hex.encode(this.string));
    }
}
