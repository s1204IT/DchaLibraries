package org.apache.harmony.security.asn1;

public abstract class ASN1Constructed extends ASN1Type {
    protected ASN1Constructed(int tagNumber) {
        super(0, tagNumber);
    }

    protected ASN1Constructed(int tagClass, int tagNumber) {
        super(tagClass, tagNumber);
    }

    @Override
    public final boolean checkTag(int identifier) {
        return this.constrId == identifier;
    }

    @Override
    public void encodeASN(BerOutputStream out) {
        out.encodeTag(this.constrId);
        encodeContent(out);
    }
}
