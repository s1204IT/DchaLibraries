package com.mediatek.internal.telephony;

import com.mediatek.ims.internal.op.OpImsCallSessionBase;

public class MtkOpCommonTelephonyCustFactoryBase {
    public OpImsCallSessionBase makeOpImsCallSession() {
        return new OpImsCallSessionBase();
    }
}
