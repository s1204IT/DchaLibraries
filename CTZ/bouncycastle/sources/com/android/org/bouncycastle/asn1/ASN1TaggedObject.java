package com.android.org.bouncycastle.asn1;

import java.io.IOException;

public abstract class ASN1TaggedObject extends ASN1Primitive implements ASN1TaggedObjectParser {
    boolean empty = false;
    boolean explicit;
    ASN1Encodable obj;
    int tagNo;

    @Override
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static ASN1TaggedObject getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        if (z) {
            return (ASN1TaggedObject) aSN1TaggedObject.getObject();
        }
        throw new IllegalArgumentException("implicitly tagged tagged object");
    }

    public static ASN1TaggedObject getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1TaggedObject)) {
            return (ASN1TaggedObject) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(fromByteArray((byte[]) obj));
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to construct tagged object from byte[]: " + e.getMessage());
            }
        }
        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }

    public ASN1TaggedObject(boolean z, int i, ASN1Encodable aSN1Encodable) {
        this.explicit = true;
        this.obj = null;
        if (aSN1Encodable instanceof ASN1Choice) {
            this.explicit = true;
        } else {
            this.explicit = z;
        }
        this.tagNo = i;
        if (this.explicit) {
            this.obj = aSN1Encodable;
        } else {
            if (aSN1Encodable.toASN1Primitive() instanceof ASN1Set) {
            }
            this.obj = aSN1Encodable;
        }
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1TaggedObject)) {
            return false;
        }
        ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) aSN1Primitive;
        if (this.tagNo == aSN1TaggedObject.tagNo && this.empty == aSN1TaggedObject.empty && this.explicit == aSN1TaggedObject.explicit) {
            return this.obj == null ? aSN1TaggedObject.obj == null : this.obj.toASN1Primitive().equals(aSN1TaggedObject.obj.toASN1Primitive());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int i = this.tagNo;
        if (this.obj != null) {
            return i ^ this.obj.hashCode();
        }
        return i;
    }

    @Override
    public int getTagNo() {
        return this.tagNo;
    }

    public boolean isExplicit() {
        return this.explicit;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public ASN1Primitive getObject() {
        if (this.obj != null) {
            return this.obj.toASN1Primitive();
        }
        return null;
    }

    @Override
    public ASN1Encodable getObjectParser(int i, boolean z) throws IOException {
        if (i != 4) {
            switch (i) {
                case 16:
                    return ASN1Sequence.getInstance(this, z).parser();
                case 17:
                    return ASN1Set.getInstance(this, z).parser();
                default:
                    if (z) {
                        return getObject();
                    }
                    throw new ASN1Exception("implicit tagging not implemented for tag: " + i);
            }
        }
        return ASN1OctetString.getInstance(this, z).parser();
    }

    @Override
    public ASN1Primitive getLoadedObject() {
        return toASN1Primitive();
    }

    @Override
    ASN1Primitive toDERObject() {
        return new DERTaggedObject(this.explicit, this.tagNo, this.obj);
    }

    @Override
    ASN1Primitive toDLObject() {
        return new DLTaggedObject(this.explicit, this.tagNo, this.obj);
    }

    public String toString() {
        return "[" + this.tagNo + "]" + this.obj;
    }
}
