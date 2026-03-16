package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import java.math.BigInteger;

public class ECAlgorithms {
    public static ECPoint sumOfTwoMultiplies(ECPoint P, BigInteger a, ECPoint Q, BigInteger b) {
        ECCurve cp = P.getCurve();
        ECPoint Q2 = importPoint(cp, Q);
        if (cp instanceof ECCurve.F2m) {
            ECCurve.F2m f2mCurve = (ECCurve.F2m) cp;
            if (f2mCurve.isKoblitz()) {
                return P.multiply(a).add(Q2.multiply(b));
            }
        }
        return implShamirsTrick(P, a, Q2, b);
    }

    public static ECPoint shamirsTrick(ECPoint P, BigInteger k, ECPoint Q, BigInteger l) {
        ECCurve cp = P.getCurve();
        return implShamirsTrick(P, k, importPoint(cp, Q), l);
    }

    public static ECPoint importPoint(ECCurve c, ECPoint p) {
        ECCurve cp = p.getCurve();
        if (!c.equals(cp)) {
            throw new IllegalArgumentException("Point must be on the same curve");
        }
        return c.importPoint(p);
    }

    static void implMontgomeryTrick(ECFieldElement[] zs, int off, int len) {
        ECFieldElement[] c = new ECFieldElement[len];
        c[0] = zs[off];
        int i = 0;
        while (true) {
            i++;
            if (i >= len) {
                break;
            } else {
                c[i] = c[i - 1].multiply(zs[off + i]);
            }
        }
        int i2 = i - 1;
        ECFieldElement u = c[i2].invert();
        int i3 = i2;
        while (i3 > 0) {
            int i4 = i3 - 1;
            int j = off + i3;
            ECFieldElement tmp = zs[j];
            zs[j] = c[i4].multiply(u);
            u = u.multiply(tmp);
            i3 = i4;
        }
        zs[off] = u;
    }

    static ECPoint implShamirsTrick(ECPoint P, BigInteger k, ECPoint Q, BigInteger l) {
        ECCurve curve = P.getCurve();
        ECPoint infinity = curve.getInfinity();
        ECPoint PaddQ = P.add(Q);
        ECPoint PsubQ = P.subtract(Q);
        ECPoint[] points = {Q, PsubQ, P, PaddQ};
        curve.normalizeAll(points);
        ECPoint[] table = {points[3].negate(), points[2].negate(), points[1].negate(), points[0].negate(), infinity, points[0], points[1], points[2], points[3]};
        byte[] jsf = WNafUtil.generateJSF(k, l);
        ECPoint R = infinity;
        int i = jsf.length;
        while (true) {
            i--;
            if (i >= 0) {
                int jsfi = jsf[i];
                int kDigit = jsfi >> 4;
                int lDigit = (jsfi << 28) >> 28;
                int index = (kDigit * 3) + 4 + lDigit;
                R = R.twicePlus(table[index]);
            } else {
                return R;
            }
        }
    }
}
