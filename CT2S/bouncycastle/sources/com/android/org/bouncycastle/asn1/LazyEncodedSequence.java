package com.android.org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Enumeration;

class LazyEncodedSequence extends ASN1Sequence {
    private byte[] encoded;

    LazyEncodedSequence(byte[] encoded) throws IOException {
        this.encoded = encoded;
    }

    private void parse() {
        Enumeration en = new LazyConstructionEnumeration(this.encoded);
        while (en.hasMoreElements()) {
            this.seq.addElement(en.nextElement());
        }
        this.encoded = null;
    }

    @Override
    public synchronized ASN1Encodable getObjectAt(int index) {
        if (this.encoded != null) {
            parse();
        }
        return super.getObjectAt(index);
    }

    @Override
    public synchronized Enumeration getObjects() {
        return this.encoded == null ? super.getObjects() : new LazyConstructionEnumeration(this.encoded);
    }

    @Override
    public synchronized int size() {
        if (this.encoded != null) {
            parse();
        }
        return super.size();
    }

    @Override
    ASN1Primitive toDERObject() {
        if (this.encoded != null) {
            parse();
        }
        return super.toDERObject();
    }

    @Override
    ASN1Primitive toDLObject() {
        if (this.encoded != null) {
            parse();
        }
        return super.toDLObject();
    }

    @Override
    int encodedLength() throws IOException {
        return this.encoded != null ? StreamUtil.calculateBodyLength(this.encoded.length) + 1 + this.encoded.length : super.toDLObject().encodedLength();
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        if (this.encoded != null) {
            out.writeEncoded(48, this.encoded);
        } else {
            super.toDLObject().encode(out);
        }
    }
}
