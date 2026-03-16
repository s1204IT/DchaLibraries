package org.apache.harmony.security.asn1;

import java.io.IOException;

public final class ASN1Boolean extends ASN1Primitive {
    private static final ASN1Boolean ASN1 = new ASN1Boolean();

    public ASN1Boolean() {
        super(1);
    }

    public static ASN1Boolean getInstance() {
        return ASN1;
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        in.readBoolean();
        if (in.isVerify) {
            return null;
        }
        return getDecodedObject(in);
    }

    @Override
    public Object getDecodedObject(BerInputStream in) throws IOException {
        return in.buffer[in.contentOffset] == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    public void encodeContent(BerOutputStream out) {
        out.encodeBoolean();
    }

    @Override
    public void setEncodingContent(BerOutputStream out) {
        out.length = 1;
    }
}
