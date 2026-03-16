package org.apache.harmony.security.asn1;

import java.io.IOException;

public final class ASN1Explicit extends ASN1Constructed {
    public final ASN1Type type;

    public ASN1Explicit(int tagNumber, ASN1Type type) {
        this(128, tagNumber, type);
    }

    public ASN1Explicit(int tagClass, int tagNumber, ASN1Type type) {
        super(tagClass, tagNumber);
        this.type = type;
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        if (this.constrId != in.tag) {
            throw new ASN1Exception("ASN.1 explicitly tagged type is expected at [" + in.tagOffset + "]. Expected tag: " + Integer.toHexString(this.constrId) + ", but encountered tag " + Integer.toHexString(in.tag));
        }
        in.next();
        in.content = this.type.decode(in);
        if (in.isVerify) {
            return null;
        }
        return getDecodedObject(in);
    }

    @Override
    public void encodeContent(BerOutputStream out) {
        out.encodeExplicit(this);
    }

    @Override
    public void setEncodingContent(BerOutputStream out) {
        out.getExplicitLength(this);
    }

    @Override
    public String toString() {
        return super.toString() + " for type " + this.type;
    }
}
