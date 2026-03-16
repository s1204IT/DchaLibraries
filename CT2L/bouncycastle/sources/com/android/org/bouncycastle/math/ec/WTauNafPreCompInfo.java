package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.math.ec.ECPoint;

class WTauNafPreCompInfo implements PreCompInfo {
    private ECPoint.F2m[] preComp;

    WTauNafPreCompInfo(ECPoint.F2m[] preComp) {
        this.preComp = null;
        this.preComp = preComp;
    }

    protected ECPoint.F2m[] getPreComp() {
        return this.preComp;
    }
}
