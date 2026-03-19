package com.android.org.bouncycastle.asn1.sec;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERTaggedObject;
import com.android.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.util.Enumeration;

public class ECPrivateKeyStructure extends ASN1Object {
    private ASN1Sequence seq;

    public ECPrivateKeyStructure(ASN1Sequence seq) {
        this.seq = seq;
    }

    public ECPrivateKeyStructure(BigInteger key) {
        byte[] bytes = BigIntegers.asUnsignedByteArray(key);
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(1L));
        v.add(new DEROctetString(bytes));
        this.seq = new DERSequence(v);
    }

    public ECPrivateKeyStructure(BigInteger key, ASN1Encodable parameters) {
        this(key, null, parameters);
    }

    public ECPrivateKeyStructure(BigInteger key, DERBitString publicKey, ASN1Encodable parameters) {
        byte[] bytes = BigIntegers.asUnsignedByteArray(key);
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(1L));
        v.add(new DEROctetString(bytes));
        if (parameters != null) {
            v.add(new DERTaggedObject(true, 0, parameters));
        }
        if (publicKey != null) {
            v.add(new DERTaggedObject(true, 1, publicKey));
        }
        this.seq = new DERSequence(v);
    }

    public BigInteger getKey() {
        ASN1OctetString octs = (ASN1OctetString) this.seq.getObjectAt(1);
        return new BigInteger(1, octs.getOctets());
    }

    public DERBitString getPublicKey() {
        return (DERBitString) getObjectInTag(1);
    }

    public ASN1Primitive getParameters() {
        return getObjectInTag(0);
    }

    private ASN1Primitive getObjectInTag(int tagNo) {
        Enumeration e = this.seq.getObjects();
        while (e.hasMoreElements()) {
            ?? r1 = (ASN1Encodable) e.nextElement();
            if ((r1 instanceof ASN1TaggedObject) && r1.getTagNo() == tagNo) {
                return r1.getObject().toASN1Primitive();
            }
        }
        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        return this.seq;
    }
}
