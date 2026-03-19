package com.android.org.bouncycastle.asn1;

import java.io.IOException;

public abstract class ASN1Null extends ASN1Primitive {
    @Override
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static ASN1Null getInstance(Object obj) {
        if (obj instanceof ASN1Null) {
            return (ASN1Null) obj;
        }
        if (obj != null) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to construct NULL from byte[]: " + e.getMessage());
            } catch (ClassCastException e2) {
                throw new IllegalArgumentException("unknown object in getInstance(): " + obj.getClass().getName());
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return -1;
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1Null)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "NULL";
    }
}
