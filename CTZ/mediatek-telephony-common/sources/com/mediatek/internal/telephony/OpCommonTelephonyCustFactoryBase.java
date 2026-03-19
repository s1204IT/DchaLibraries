package com.mediatek.internal.telephony;

import com.mediatek.internal.telephony.imsphone.op.OpCommonImsPhoneCallTracker;
import com.mediatek.internal.telephony.imsphone.op.OpCommonImsPhoneCallTrackerBase;
import com.mediatek.internal.telephony.imsphone.op.OpImsPhoneConnection;
import com.mediatek.internal.telephony.imsphone.op.OpImsPhoneConnectionBase;
import com.mediatek.internal.telephony.imsphone.op.OpImsRttTextHandler;
import com.mediatek.internal.telephony.imsphone.op.OpImsRttTextHandlerBase;

public class OpCommonTelephonyCustFactoryBase {
    public OpCommonImsPhoneCallTracker makeOpCommonImsPhoneCallTracker() {
        return new OpCommonImsPhoneCallTrackerBase();
    }

    public OpImsPhoneConnection makeOpImsPhoneConnection() {
        return new OpImsPhoneConnectionBase();
    }

    public OpImsRttTextHandler makeOpImsRttTextHandler() {
        return new OpImsRttTextHandlerBase();
    }
}
