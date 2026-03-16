package org.apache.harmony.security.asn1;

import java.io.IOException;
import java.util.Arrays;

public final class ASN1Enumerated extends ASN1Primitive {
    private static final ASN1Enumerated ASN1 = new ASN1Enumerated();

    public ASN1Enumerated() {
        super(10);
    }

    public static ASN1Enumerated getInstance() {
        return ASN1;
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        in.readEnumerated();
        if (in.isVerify) {
            return null;
        }
        return getDecodedObject(in);
    }

    @Override
    public Object getDecodedObject(BerInputStream in) throws IOException {
        return Arrays.copyOfRange(in.buffer, in.contentOffset, in.contentOffset + in.length);
    }

    @Override
    public void encodeContent(BerOutputStream out) {
        out.encodeInteger();
    }

    @Override
    public void setEncodingContent(BerOutputStream out) {
        out.length = ((byte[]) out.content).length;
    }
}
