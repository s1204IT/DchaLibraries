package com.android.org.bouncycastle.asn1.x509;

import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import java.math.BigInteger;

public class CRLNumber extends ASN1Object {
    private BigInteger number;

    public CRLNumber(BigInteger number) {
        this.number = number;
    }

    public BigInteger getCRLNumber() {
        return this.number;
    }

    public String toString() {
        return "CRLNumber: " + getCRLNumber();
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        return new ASN1Integer(this.number);
    }

    public static CRLNumber getInstance(Object obj) {
        if (obj instanceof CRLNumber) {
            return obj;
        }
        if (obj != 0) {
            return new CRLNumber(ASN1Integer.getInstance(obj).getValue());
        }
        return null;
    }
}
