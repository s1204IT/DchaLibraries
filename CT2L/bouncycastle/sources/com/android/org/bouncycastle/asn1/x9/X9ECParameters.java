package com.android.org.bouncycastle.asn1.x9;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

public class X9ECParameters extends ASN1Object implements X9ObjectIdentifiers {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private ECCurve curve;
    private X9FieldID fieldID;
    private ECPoint g;
    private BigInteger h;
    private BigInteger n;
    private byte[] seed;

    private X9ECParameters(ASN1Sequence seq) {
        if (!(seq.getObjectAt(0) instanceof ASN1Integer) || !((ASN1Integer) seq.getObjectAt(0)).getValue().equals(ONE)) {
            throw new IllegalArgumentException("bad version in X9ECParameters");
        }
        X9Curve x9c = new X9Curve(X9FieldID.getInstance(seq.getObjectAt(1)), ASN1Sequence.getInstance(seq.getObjectAt(2)));
        this.curve = x9c.getCurve();
        ASN1Encodable p = seq.getObjectAt(3);
        if (p instanceof X9ECPoint) {
            this.g = ((X9ECPoint) p).getPoint();
        } else {
            this.g = new X9ECPoint(this.curve, (ASN1OctetString) p).getPoint();
        }
        this.n = ((ASN1Integer) seq.getObjectAt(4)).getValue();
        this.seed = x9c.getSeed();
        if (seq.size() == 6) {
            this.h = ((ASN1Integer) seq.getObjectAt(5)).getValue();
        }
    }

    public static X9ECParameters getInstance(Object obj) {
        if (obj instanceof X9ECParameters) {
            return (X9ECParameters) obj;
        }
        if (obj != null) {
            return new X9ECParameters(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public X9ECParameters(ECCurve curve, ECPoint g, BigInteger n) {
        this(curve, g, n, ONE, null);
    }

    public X9ECParameters(ECCurve curve, ECPoint g, BigInteger n, BigInteger h) {
        this(curve, g, n, h, null);
    }

    public X9ECParameters(ECCurve curve, ECPoint g, BigInteger n, BigInteger h, byte[] seed) {
        this.curve = curve;
        this.g = g.normalize();
        this.n = n;
        this.h = h;
        this.seed = seed;
        if (curve instanceof ECCurve.Fp) {
            this.fieldID = new X9FieldID(((ECCurve.Fp) curve).getQ());
        } else if (curve instanceof ECCurve.F2m) {
            ECCurve.F2m curveF2m = (ECCurve.F2m) curve;
            this.fieldID = new X9FieldID(curveF2m.getM(), curveF2m.getK1(), curveF2m.getK2(), curveF2m.getK3());
        }
    }

    public ECCurve getCurve() {
        return this.curve;
    }

    public ECPoint getG() {
        return this.g;
    }

    public BigInteger getN() {
        return this.n;
    }

    public BigInteger getH() {
        return this.h == null ? ONE : this.h;
    }

    public byte[] getSeed() {
        return this.seed;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(1L));
        v.add(this.fieldID);
        v.add(new X9Curve(this.curve, this.seed));
        v.add(new X9ECPoint(this.g));
        v.add(new ASN1Integer(this.n));
        if (this.h != null) {
            v.add(new ASN1Integer(this.h));
        }
        return new DERSequence(v);
    }
}
