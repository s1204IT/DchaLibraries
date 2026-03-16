package com.android.org.bouncycastle.asn1.x9;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.nist.NISTNamedCurves;
import com.android.org.bouncycastle.asn1.sec.SECNamedCurves;
import java.util.Enumeration;
import java.util.Vector;

public class ECNamedCurveTable {
    public static X9ECParameters getByName(String name) {
        X9ECParameters ecP = X962NamedCurves.getByName(name);
        if (ecP == null) {
            ecP = SECNamedCurves.getByName(name);
        }
        if (ecP == null) {
            return NISTNamedCurves.getByName(name);
        }
        return ecP;
    }

    public static X9ECParameters getByOID(ASN1ObjectIdentifier oid) {
        X9ECParameters ecP = X962NamedCurves.getByOID(oid);
        if (ecP == null) {
            return SECNamedCurves.getByOID(oid);
        }
        return ecP;
    }

    public static Enumeration getNames() {
        Vector v = new Vector();
        addEnumeration(v, X962NamedCurves.getNames());
        addEnumeration(v, SECNamedCurves.getNames());
        addEnumeration(v, NISTNamedCurves.getNames());
        return v.elements();
    }

    private static void addEnumeration(Vector v, Enumeration e) {
        while (e.hasMoreElements()) {
            v.addElement(e.nextElement());
        }
    }
}
