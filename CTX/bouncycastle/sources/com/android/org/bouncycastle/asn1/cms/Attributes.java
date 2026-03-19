package com.android.org.bouncycastle.asn1.cms;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.DLSet;

public class Attributes extends ASN1Object {
    private ASN1Set attributes;

    private Attributes(ASN1Set aSN1Set) {
        this.attributes = aSN1Set;
    }

    public Attributes(ASN1EncodableVector aSN1EncodableVector) {
        this.attributes = new DLSet(aSN1EncodableVector);
    }

    public static Attributes getInstance(Object obj) {
        if (obj instanceof Attributes) {
            return (Attributes) obj;
        }
        if (obj != null) {
            return new Attributes(ASN1Set.getInstance(obj));
        }
        return null;
    }

    public Attribute[] getAttributes() {
        Attribute[] attributeArr = new Attribute[this.attributes.size()];
        for (int i = 0; i != attributeArr.length; i++) {
            attributeArr[i] = Attribute.getInstance(this.attributes.getObjectAt(i));
        }
        return attributeArr;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        return this.attributes;
    }
}
