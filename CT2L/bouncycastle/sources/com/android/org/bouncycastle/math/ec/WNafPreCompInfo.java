package com.android.org.bouncycastle.math.ec;

public class WNafPreCompInfo implements PreCompInfo {
    private ECPoint[] preComp = null;
    private ECPoint[] preCompNeg = null;
    private ECPoint twiceP = null;

    protected ECPoint[] getPreComp() {
        return this.preComp;
    }

    protected ECPoint[] getPreCompNeg() {
        return this.preCompNeg;
    }

    protected void setPreComp(ECPoint[] preComp) {
        this.preComp = preComp;
    }

    protected void setPreCompNeg(ECPoint[] preCompNeg) {
        this.preCompNeg = preCompNeg;
    }

    protected ECPoint getTwiceP() {
        return this.twiceP;
    }

    protected void setTwiceP(ECPoint twiceP) {
        this.twiceP = twiceP;
    }
}
