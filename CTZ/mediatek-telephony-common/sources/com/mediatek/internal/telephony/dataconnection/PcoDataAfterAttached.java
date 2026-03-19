package com.mediatek.internal.telephony.dataconnection;

import android.telephony.PcoData;

public class PcoDataAfterAttached extends PcoData {
    public final String apnName;

    public PcoDataAfterAttached(int i, String str, String str2, int i2, byte[] bArr) {
        super(i, str2, i2, bArr);
        this.apnName = str;
    }

    public String toString() {
        return "PcoDataAfterAttached(" + this.cid + ", " + this.apnName + ", " + this.bearerProto + ", " + this.pcoId + ", contents[" + this.contents.length + "])";
    }
}
