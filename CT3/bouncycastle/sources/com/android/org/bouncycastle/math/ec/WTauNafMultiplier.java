package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

public class WTauNafMultiplier extends AbstractECMultiplier {
    static final String PRECOMP_NAME = "bc_wtnaf";

    @Override
    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger k) {
        if (!(eCPoint instanceof ECPoint.AbstractF2m)) {
            throw new IllegalArgumentException("Only ECPoint.AbstractF2m can be used in WTauNafMultiplier");
        }
        ECCurve.AbstractF2m curve = (ECCurve.AbstractF2m) eCPoint.getCurve();
        int m = curve.getFieldSize();
        byte a = curve.getA().toBigInteger().byteValue();
        byte mu = Tnaf.getMu(a);
        BigInteger[] s = curve.getSi();
        ZTauElement rho = Tnaf.partModReduction(k, m, a, s, mu, (byte) 10);
        return multiplyWTnaf(eCPoint, rho, curve.getPreCompInfo(eCPoint, PRECOMP_NAME), a, mu);
    }

    private ECPoint.AbstractF2m multiplyWTnaf(ECPoint.AbstractF2m p, ZTauElement lambda, PreCompInfo preCompInfo, byte a, byte mu) {
        ZTauElement[] alpha = a == 0 ? Tnaf.alpha0 : Tnaf.alpha1;
        BigInteger tw = Tnaf.getTw(mu, 4);
        byte[] u = Tnaf.tauAdicWNaf(mu, lambda, (byte) 4, BigInteger.valueOf(16L), tw, alpha);
        return multiplyFromWTnaf(p, u, preCompInfo);
    }

    private static ECPoint.AbstractF2m multiplyFromWTnaf(ECPoint.AbstractF2m p, byte[] u, PreCompInfo preCompInfo) {
        ECPoint.AbstractF2m[] pu;
        ECCurve.AbstractF2m curve = (ECCurve.AbstractF2m) p.getCurve();
        byte a = curve.getA().toBigInteger().byteValue();
        if (preCompInfo == 0 || !(preCompInfo instanceof WTauNafPreCompInfo)) {
            pu = Tnaf.getPreComp(p, a);
            WTauNafPreCompInfo pre = new WTauNafPreCompInfo();
            pre.setPreComp(pu);
            curve.setPreCompInfo(p, PRECOMP_NAME, pre);
        } else {
            pu = preCompInfo.getPreComp();
        }
        ECPoint.AbstractF2m[] puNeg = new ECPoint.AbstractF2m[pu.length];
        for (int i = 0; i < pu.length; i++) {
            puNeg[i] = (ECPoint.AbstractF2m) pu[i].negate();
        }
        ECPoint.AbstractF2m q = (ECPoint.AbstractF2m) p.getCurve().getInfinity();
        int tauCount = 0;
        for (int i2 = u.length - 1; i2 >= 0; i2--) {
            tauCount++;
            int ui = u[i2];
            if (ui != 0) {
                ECPoint.AbstractF2m q2 = q.tauPow(tauCount);
                tauCount = 0;
                q = (ECPoint.AbstractF2m) q2.add(ui > 0 ? pu[ui >>> 1] : puNeg[(-ui) >>> 1]);
            }
        }
        if (tauCount > 0) {
            return q.tauPow(tauCount);
        }
        return q;
    }
}
