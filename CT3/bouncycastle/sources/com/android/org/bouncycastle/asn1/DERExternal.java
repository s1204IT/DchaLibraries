package com.android.org.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DERExternal extends ASN1Primitive {
    private ASN1Primitive dataValueDescriptor;
    private ASN1ObjectIdentifier directReference;
    private int encoding;
    private ASN1Primitive externalContent;
    private ASN1Integer indirectReference;

    public DERExternal(ASN1EncodableVector aSN1EncodableVector) {
        int i = 0;
        ?? objFromVector = getObjFromVector(aSN1EncodableVector, 0);
        boolean z = objFromVector instanceof ASN1ObjectIdentifier;
        ?? objFromVector2 = objFromVector;
        if (z) {
            this.directReference = objFromVector;
            i = 1;
            objFromVector2 = getObjFromVector(aSN1EncodableVector, 1);
        }
        boolean z2 = objFromVector2 instanceof ASN1Integer;
        ?? objFromVector3 = objFromVector2;
        if (z2) {
            this.indirectReference = objFromVector2;
            i++;
            objFromVector3 = getObjFromVector(aSN1EncodableVector, i);
        }
        boolean z3 = objFromVector3 instanceof ASN1TaggedObject;
        ?? objFromVector4 = objFromVector3;
        if (!z3) {
            this.dataValueDescriptor = objFromVector3;
            i++;
            objFromVector4 = getObjFromVector(aSN1EncodableVector, i);
        }
        if (aSN1EncodableVector.size() != i + 1) {
            throw new IllegalArgumentException("input vector too large");
        }
        if (!(objFromVector4 instanceof ASN1TaggedObject)) {
            throw new IllegalArgumentException("No tagged object found in vector. Structure doesn't seem to be of type External");
        }
        ?? r1 = objFromVector4;
        setEncoding(r1.getTagNo());
        this.externalContent = r1.getObject();
    }

    private ASN1Primitive getObjFromVector(ASN1EncodableVector v, int index) {
        if (v.size() <= index) {
            throw new IllegalArgumentException("too few objects in input vector");
        }
        return v.get(index).toASN1Primitive();
    }

    public DERExternal(ASN1ObjectIdentifier directReference, ASN1Integer indirectReference, ASN1Primitive dataValueDescriptor, DERTaggedObject externalData) {
        this(directReference, indirectReference, dataValueDescriptor, externalData.getTagNo(), externalData.toASN1Primitive());
    }

    public DERExternal(ASN1ObjectIdentifier directReference, ASN1Integer indirectReference, ASN1Primitive dataValueDescriptor, int encoding, ASN1Primitive externalData) {
        setDirectReference(directReference);
        setIndirectReference(indirectReference);
        setDataValueDescriptor(dataValueDescriptor);
        setEncoding(encoding);
        setExternalContent(externalData.toASN1Primitive());
    }

    @Override
    public int hashCode() {
        int ret = 0;
        if (this.directReference != null) {
            ret = this.directReference.hashCode();
        }
        if (this.indirectReference != null) {
            ret ^= this.indirectReference.hashCode();
        }
        if (this.dataValueDescriptor != null) {
            ret ^= this.dataValueDescriptor.hashCode();
        }
        return ret ^ this.externalContent.hashCode();
    }

    @Override
    boolean isConstructed() {
        return true;
    }

    @Override
    int encodedLength() throws IOException {
        return getEncoded().length;
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (this.directReference != null) {
            baos.write(this.directReference.getEncoded(ASN1Encoding.DER));
        }
        if (this.indirectReference != null) {
            baos.write(this.indirectReference.getEncoded(ASN1Encoding.DER));
        }
        if (this.dataValueDescriptor != null) {
            baos.write(this.dataValueDescriptor.getEncoded(ASN1Encoding.DER));
        }
        DERTaggedObject obj = new DERTaggedObject(true, this.encoding, this.externalContent);
        baos.write(obj.getEncoded(ASN1Encoding.DER));
        out.writeEncoded(32, 8, baos.toByteArray());
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERExternal)) {
            return false;
        }
        if (this == aSN1Primitive) {
            return true;
        }
        if (this.directReference != null && (aSN1Primitive.directReference == null || !aSN1Primitive.directReference.equals(this.directReference))) {
            return false;
        }
        if (this.indirectReference != null && (aSN1Primitive.indirectReference == null || !aSN1Primitive.indirectReference.equals(this.indirectReference))) {
            return false;
        }
        if (this.dataValueDescriptor == null || (aSN1Primitive.dataValueDescriptor != null && aSN1Primitive.dataValueDescriptor.equals(this.dataValueDescriptor))) {
            return this.externalContent.equals(aSN1Primitive.externalContent);
        }
        return false;
    }

    public ASN1Primitive getDataValueDescriptor() {
        return this.dataValueDescriptor;
    }

    public ASN1ObjectIdentifier getDirectReference() {
        return this.directReference;
    }

    public int getEncoding() {
        return this.encoding;
    }

    public ASN1Primitive getExternalContent() {
        return this.externalContent;
    }

    public ASN1Integer getIndirectReference() {
        return this.indirectReference;
    }

    private void setDataValueDescriptor(ASN1Primitive dataValueDescriptor) {
        this.dataValueDescriptor = dataValueDescriptor;
    }

    private void setDirectReference(ASN1ObjectIdentifier directReferemce) {
        this.directReference = directReferemce;
    }

    private void setEncoding(int encoding) {
        if (encoding < 0 || encoding > 2) {
            throw new IllegalArgumentException("invalid encoding value: " + encoding);
        }
        this.encoding = encoding;
    }

    private void setExternalContent(ASN1Primitive externalContent) {
        this.externalContent = externalContent;
    }

    private void setIndirectReference(ASN1Integer indirectReference) {
        this.indirectReference = indirectReference;
    }
}
