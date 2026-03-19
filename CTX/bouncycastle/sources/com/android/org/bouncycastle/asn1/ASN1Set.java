package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Iterable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

public abstract class ASN1Set extends ASN1Primitive implements Iterable<ASN1Encodable> {
    private Vector set = new Vector();
    private boolean isSorted = false;

    @Override
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static ASN1Set getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1Set)) {
            return (ASN1Set) obj;
        }
        if (obj instanceof ASN1SetParser) {
            return getInstance(((ASN1SetParser) obj).toASN1Primitive());
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to construct set from byte[]: " + e.getMessage());
            }
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Primitive aSN1Primitive = ((ASN1Encodable) obj).toASN1Primitive();
            if (aSN1Primitive instanceof ASN1Set) {
                return (ASN1Set) aSN1Primitive;
            }
        }
        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }

    public static ASN1Set getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        if (z) {
            if (!aSN1TaggedObject.isExplicit()) {
                throw new IllegalArgumentException("object implicit - explicit expected.");
            }
            return (ASN1Set) aSN1TaggedObject.getObject();
        }
        if (aSN1TaggedObject.isExplicit()) {
            if (aSN1TaggedObject instanceof BERTaggedObject) {
                return new BERSet(aSN1TaggedObject.getObject());
            }
            return new DLSet(aSN1TaggedObject.getObject());
        }
        if (aSN1TaggedObject.getObject() instanceof ASN1Set) {
            return (ASN1Set) aSN1TaggedObject.getObject();
        }
        if (aSN1TaggedObject.getObject() instanceof ASN1Sequence) {
            ASN1Sequence aSN1Sequence = (ASN1Sequence) aSN1TaggedObject.getObject();
            if (aSN1TaggedObject instanceof BERTaggedObject) {
                return new BERSet(aSN1Sequence.toArray());
            }
            return new DLSet(aSN1Sequence.toArray());
        }
        throw new IllegalArgumentException("unknown object in getInstance: " + aSN1TaggedObject.getClass().getName());
    }

    protected ASN1Set() {
    }

    protected ASN1Set(ASN1Encodable aSN1Encodable) {
        this.set.addElement(aSN1Encodable);
    }

    protected ASN1Set(ASN1EncodableVector aSN1EncodableVector, boolean z) {
        for (int i = 0; i != aSN1EncodableVector.size(); i++) {
            this.set.addElement(aSN1EncodableVector.get(i));
        }
        if (z) {
            sort();
        }
    }

    protected ASN1Set(ASN1Encodable[] aSN1EncodableArr, boolean z) {
        for (int i = 0; i != aSN1EncodableArr.length; i++) {
            this.set.addElement(aSN1EncodableArr[i]);
        }
        if (z) {
            sort();
        }
    }

    public Enumeration getObjects() {
        return this.set.elements();
    }

    public ASN1Encodable getObjectAt(int i) {
        return (ASN1Encodable) this.set.elementAt(i);
    }

    public int size() {
        return this.set.size();
    }

    public ASN1Encodable[] toArray() {
        ASN1Encodable[] aSN1EncodableArr = new ASN1Encodable[size()];
        for (int i = 0; i != size(); i++) {
            aSN1EncodableArr[i] = getObjectAt(i);
        }
        return aSN1EncodableArr;
    }

    public ASN1SetParser parser() {
        return new ASN1SetParser() {
            private int index;
            private final int max;

            {
                this.max = ASN1Set.this.size();
            }

            @Override
            public ASN1Encodable readObject() throws IOException {
                if (this.index == this.max) {
                    return null;
                }
                ASN1Set aSN1Set = ASN1Set.this;
                int i = this.index;
                this.index = i + 1;
                ASN1Encodable objectAt = aSN1Set.getObjectAt(i);
                if (objectAt instanceof ASN1Sequence) {
                    return ((ASN1Sequence) objectAt).parser();
                }
                if (objectAt instanceof ASN1Set) {
                    return ((ASN1Set) objectAt).parser();
                }
                return objectAt;
            }

            @Override
            public ASN1Primitive getLoadedObject() {
                return this;
            }

            @Override
            public ASN1Primitive toASN1Primitive() {
                return this;
            }
        };
    }

    @Override
    public int hashCode() {
        Enumeration objects = getObjects();
        int size = size();
        while (objects.hasMoreElements()) {
            size = (size * 17) ^ getNext(objects).hashCode();
        }
        return size;
    }

    @Override
    ASN1Primitive toDERObject() {
        if (this.isSorted) {
            DERSet dERSet = new DERSet();
            dERSet.set = this.set;
            return dERSet;
        }
        Vector vector = new Vector();
        for (int i = 0; i != this.set.size(); i++) {
            vector.addElement(this.set.elementAt(i));
        }
        DERSet dERSet2 = new DERSet();
        dERSet2.set = vector;
        dERSet2.sort();
        return dERSet2;
    }

    @Override
    ASN1Primitive toDLObject() {
        DLSet dLSet = new DLSet();
        dLSet.set = this.set;
        return dLSet;
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1Set)) {
            return false;
        }
        ASN1Set aSN1Set = (ASN1Set) aSN1Primitive;
        if (size() != aSN1Set.size()) {
            return false;
        }
        Enumeration objects = getObjects();
        Enumeration objects2 = aSN1Set.getObjects();
        while (objects.hasMoreElements()) {
            ASN1Encodable next = getNext(objects);
            ASN1Encodable next2 = getNext(objects2);
            ASN1Primitive aSN1Primitive2 = next.toASN1Primitive();
            ASN1Primitive aSN1Primitive3 = next2.toASN1Primitive();
            if (aSN1Primitive2 != aSN1Primitive3 && !aSN1Primitive2.equals(aSN1Primitive3)) {
                return false;
            }
        }
        return true;
    }

    private ASN1Encodable getNext(Enumeration enumeration) {
        ASN1Encodable aSN1Encodable = (ASN1Encodable) enumeration.nextElement();
        if (aSN1Encodable == null) {
            return DERNull.INSTANCE;
        }
        return aSN1Encodable;
    }

    private boolean lessThanOrEqual(byte[] bArr, byte[] bArr2) {
        int iMin = Math.min(bArr.length, bArr2.length);
        for (int i = 0; i != iMin; i++) {
            if (bArr[i] != bArr2[i]) {
                return (bArr[i] & 255) < (bArr2[i] & 255);
            }
        }
        return iMin == bArr.length;
    }

    private byte[] getDEREncoded(ASN1Encodable aSN1Encodable) {
        try {
            return aSN1Encodable.toASN1Primitive().getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot encode object added to SET");
        }
    }

    protected void sort() {
        if (!r9.isSorted) {
            r9.isSorted = true;
            if (r9.set.size() > 1) {
                r2 = r9.set.size() - 1;
                r1 = true;
                while (r1) {
                    r3 = 0;
                    r1 = getDEREncoded((com.android.org.bouncycastle.asn1.ASN1Encodable) r9.set.elementAt(0));
                    r4 = 0;
                    r5 = false;
                    while (r3 != r2) {
                        r7 = r3 + 1;
                        r6 = getDEREncoded((com.android.org.bouncycastle.asn1.ASN1Encodable) r9.set.elementAt(r7));
                        if (!lessThanOrEqual(r1, r6)) {
                            r4 = r9.set.elementAt(r3);
                            r9.set.setElementAt(r9.set.elementAt(r7), r3);
                            r9.set.setElementAt(r4, r7);
                            r5 = true;
                            r4 = r3;
                        } else {
                            r1 = r6;
                        }
                        r3 = r7;
                    }
                    r2 = r4;
                    r1 = r5;
                }
            }
        }
    }

    @Override
    boolean isConstructed() {
        return true;
    }

    public String toString() {
        return this.set.toString();
    }

    @Override
    public Iterator<ASN1Encodable> iterator() {
        return new Arrays.Iterator(toArray());
    }
}
