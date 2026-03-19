package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Iterable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

public abstract class ASN1Sequence extends ASN1Primitive implements Iterable<ASN1Encodable> {
    protected Vector seq = new Vector();

    @Override
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static com.android.org.bouncycastle.asn1.ASN1Sequence getInstance(java.lang.Object r5) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.asn1.ASN1Sequence.getInstance(java.lang.Object):com.android.org.bouncycastle.asn1.ASN1Sequence");
    }

    public static ASN1Sequence getInstance(ASN1TaggedObject obj, boolean explicit) {
        if (explicit) {
            if (!obj.isExplicit()) {
                throw new IllegalArgumentException("object implicit - explicit expected.");
            }
            return getInstance(obj.getObject().toASN1Primitive());
        }
        if (obj.isExplicit()) {
            if (obj instanceof BERTaggedObject) {
                return new BERSequence(obj.getObject());
            }
            return new DLSequence(obj.getObject());
        }
        if (obj.getObject() instanceof ASN1Sequence) {
            return (ASN1Sequence) obj.getObject();
        }
        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }

    protected ASN1Sequence() {
    }

    protected ASN1Sequence(ASN1Encodable obj) {
        this.seq.addElement(obj);
    }

    protected ASN1Sequence(ASN1EncodableVector v) {
        for (int i = 0; i != v.size(); i++) {
            this.seq.addElement(v.get(i));
        }
    }

    protected ASN1Sequence(ASN1Encodable[] array) {
        for (int i = 0; i != array.length; i++) {
            this.seq.addElement(array[i]);
        }
    }

    public ASN1Encodable[] toArray() {
        ASN1Encodable[] values = new ASN1Encodable[size()];
        for (int i = 0; i != size(); i++) {
            values[i] = getObjectAt(i);
        }
        return values;
    }

    public Enumeration getObjects() {
        return this.seq.elements();
    }

    public ASN1SequenceParser parser() {
        return new ASN1SequenceParser() {
            private int index;
            private final int max;

            {
                this.max = ASN1Sequence.this.size();
            }

            @Override
            public ASN1Encodable readObject() throws IOException {
                if (this.index == this.max) {
                    return null;
                }
                ASN1Sequence aSN1Sequence = ASN1Sequence.this;
                int i = this.index;
                this.index = i + 1;
                ?? objectAt = aSN1Sequence.getObjectAt(i);
                return objectAt instanceof ASN1Sequence ? objectAt.parser() : objectAt instanceof ASN1Set ? objectAt.parser() : objectAt;
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

    public ASN1Encodable getObjectAt(int index) {
        return (ASN1Encodable) this.seq.elementAt(index);
    }

    public int size() {
        return this.seq.size();
    }

    @Override
    public int hashCode() {
        Enumeration e = getObjects();
        int hashCode = size();
        while (e.hasMoreElements()) {
            Object o = getNext(e);
            hashCode = (hashCode * 17) ^ o.hashCode();
        }
        return hashCode;
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1Sequence) || size() != aSN1Primitive.size()) {
            return false;
        }
        Enumeration s1 = getObjects();
        Enumeration s2 = aSN1Primitive.getObjects();
        while (s1.hasMoreElements()) {
            ASN1Encodable obj1 = getNext(s1);
            ASN1Encodable obj2 = getNext(s2);
            ASN1Primitive o1 = obj1.toASN1Primitive();
            ASN1Primitive o2 = obj2.toASN1Primitive();
            if (o1 != o2 && !o1.equals(o2)) {
                return false;
            }
        }
        return true;
    }

    private ASN1Encodable getNext(Enumeration e) {
        ASN1Encodable encObj = (ASN1Encodable) e.nextElement();
        return encObj;
    }

    @Override
    ASN1Primitive toDERObject() {
        ASN1Sequence derSeq = new DERSequence();
        derSeq.seq = this.seq;
        return derSeq;
    }

    @Override
    ASN1Primitive toDLObject() {
        ASN1Sequence dlSeq = new DLSequence();
        dlSeq.seq = this.seq;
        return dlSeq;
    }

    @Override
    boolean isConstructed() {
        return true;
    }

    public String toString() {
        return this.seq.toString();
    }

    @Override
    public Iterator<ASN1Encodable> iterator() {
        return new Arrays.Iterator(toArray());
    }
}
