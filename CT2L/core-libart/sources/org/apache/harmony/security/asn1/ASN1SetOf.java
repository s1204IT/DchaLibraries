package org.apache.harmony.security.asn1;

import java.io.IOException;

public class ASN1SetOf extends ASN1ValueCollection {
    public ASN1SetOf(ASN1Type type) {
        super(17, type);
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        in.readSetOf(this);
        if (in.isVerify) {
            return null;
        }
        return getDecodedObject(in);
    }

    @Override
    public final void encodeContent(BerOutputStream out) {
        out.encodeSetOf(this);
    }

    @Override
    public final void setEncodingContent(BerOutputStream out) {
        out.getSetOfLength(this);
    }
}
