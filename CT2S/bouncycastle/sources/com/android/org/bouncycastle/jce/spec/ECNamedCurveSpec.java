package com.android.org.bouncycastle.jce.spec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import java.math.BigInteger;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

public class ECNamedCurveSpec extends java.security.spec.ECParameterSpec {
    private String name;

    private static EllipticCurve convertCurve(ECCurve curve, byte[] seed) {
        if (curve instanceof ECCurve.Fp) {
            return new EllipticCurve(new ECFieldFp(((ECCurve.Fp) curve).getQ()), curve.getA().toBigInteger(), curve.getB().toBigInteger(), seed);
        }
        ECCurve.F2m curveF2m = (ECCurve.F2m) curve;
        if (curveF2m.isTrinomial()) {
            int[] ks = {curveF2m.getK1()};
            return new EllipticCurve(new ECFieldF2m(curveF2m.getM(), ks), curve.getA().toBigInteger(), curve.getB().toBigInteger(), seed);
        }
        int[] ks2 = {curveF2m.getK3(), curveF2m.getK2(), curveF2m.getK1()};
        return new EllipticCurve(new ECFieldF2m(curveF2m.getM(), ks2), curve.getA().toBigInteger(), curve.getB().toBigInteger(), seed);
    }

    private static ECPoint convertPoint(com.android.org.bouncycastle.math.ec.ECPoint g) {
        com.android.org.bouncycastle.math.ec.ECPoint g2 = g.normalize();
        return new ECPoint(g2.getAffineXCoord().toBigInteger(), g2.getAffineYCoord().toBigInteger());
    }

    public ECNamedCurveSpec(String name, ECCurve curve, com.android.org.bouncycastle.math.ec.ECPoint g, BigInteger n) {
        super(convertCurve(curve, null), convertPoint(g), n, 1);
        this.name = name;
    }

    public ECNamedCurveSpec(String name, EllipticCurve curve, ECPoint g, BigInteger n) {
        super(curve, g, n, 1);
        this.name = name;
    }

    public ECNamedCurveSpec(String name, ECCurve curve, com.android.org.bouncycastle.math.ec.ECPoint g, BigInteger n, BigInteger h) {
        super(convertCurve(curve, null), convertPoint(g), n, h.intValue());
        this.name = name;
    }

    public ECNamedCurveSpec(String name, EllipticCurve curve, ECPoint g, BigInteger n, BigInteger h) {
        super(curve, g, n, h.intValue());
        this.name = name;
    }

    public ECNamedCurveSpec(String name, ECCurve curve, com.android.org.bouncycastle.math.ec.ECPoint g, BigInteger n, BigInteger h, byte[] seed) {
        super(convertCurve(curve, seed), convertPoint(g), n, h.intValue());
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
