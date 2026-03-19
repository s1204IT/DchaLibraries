package com.android.org.bouncycastle.jce;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.org.bouncycastle.crypto.ec.CustomNamedCurves;
import com.android.org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import java.util.Enumeration;

public class ECNamedCurveTable {
    public static ECNamedCurveParameterSpec getParameterSpec(String str) {
        X9ECParameters byName = CustomNamedCurves.getByName(str);
        if (byName == null) {
            try {
                byName = CustomNamedCurves.getByOID(new ASN1ObjectIdentifier(str));
            } catch (IllegalArgumentException e) {
            }
            if (byName == null && (byName = com.android.org.bouncycastle.asn1.x9.ECNamedCurveTable.getByName(str)) == null) {
                try {
                    byName = com.android.org.bouncycastle.asn1.x9.ECNamedCurveTable.getByOID(new ASN1ObjectIdentifier(str));
                } catch (IllegalArgumentException e2) {
                }
            }
        }
        if (byName == null) {
            return null;
        }
        return new ECNamedCurveParameterSpec(str, byName.getCurve(), byName.getG(), byName.getN(), byName.getH(), byName.getSeed());
    }

    public static Enumeration getNames() {
        return com.android.org.bouncycastle.asn1.x9.ECNamedCurveTable.getNames();
    }
}
