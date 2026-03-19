package com.android.org.bouncycastle.asn1;

import java.io.IOException;

public class DERNull extends ASN1Null {
    public static final DERNull INSTANCE = new DERNull();
    private static final byte[] zeroBytes = new byte[0];

    protected DERNull() {
    }

    @Override
    boolean isConstructed() {
        return false;
    }

    @Override
    int encodedLength() {
        return 2;
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(5, zeroBytes);
    }
}
