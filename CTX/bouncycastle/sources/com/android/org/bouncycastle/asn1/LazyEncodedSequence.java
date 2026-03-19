package com.android.org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Enumeration;

class LazyEncodedSequence extends ASN1Sequence {
    private byte[] encoded;

    LazyEncodedSequence(byte[] bArr) throws IOException {
        this.encoded = bArr;
    }

    private void parse() {
        LazyConstructionEnumeration lazyConstructionEnumeration = new LazyConstructionEnumeration(this.encoded);
        while (lazyConstructionEnumeration.hasMoreElements()) {
            this.seq.addElement(lazyConstructionEnumeration.nextElement());
        }
        this.encoded = null;
    }

    @Override
    public synchronized ASN1Encodable getObjectAt(int i) {
        if (this.encoded != null) {
            parse();
        }
        return super.getObjectAt(i);
    }

    @Override
    public synchronized Enumeration getObjects() {
        if (this.encoded == null) {
            return super.getObjects();
        }
        return new LazyConstructionEnumeration(this.encoded);
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
        if (this.encoded != null) {
            return 1 + StreamUtil.calculateBodyLength(this.encoded.length) + this.encoded.length;
        }
        return super.toDLObject().encodedLength();
    }

    @Override
    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        if (this.encoded != null) {
            aSN1OutputStream.writeEncoded(48, this.encoded);
        } else {
            super.toDLObject().encode(aSN1OutputStream);
        }
    }
}
