package com.android.org.bouncycastle.asn1;

import java.io.IOException;

public abstract class ASN1Null extends ASN1Primitive {
    @Override
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    ASN1Null() {
    }

    public static ASN1Null getInstance(Object o) {
        if (o instanceof ASN1Null) {
            return (ASN1Null) o;
        }
        if (o != null) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) o));
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to construct NULL from byte[]: " + e.getMessage());
            } catch (ClassCastException e2) {
                throw new IllegalArgumentException("unknown object in getInstance(): " + o.getClass().getName());
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return -1;
    }

    @Override
    boolean asn1Equals(ASN1Primitive o) {
        return o instanceof ASN1Null;
    }

    public String toString() {
        return "NULL";
    }
}
