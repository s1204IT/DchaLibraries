package com.mediatek.internal.telephony.imsphone.op;

import android.telephony.Rlog;
import com.android.ims.ImsCall;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.imsphone.ImsRttTextHandler;
import com.mediatek.ims.MtkImsCall;
import java.util.Set;

public class OpImsPhoneConnectionBase implements OpImsPhoneConnection {
    private static final String TAG = "OpImsPhoneConnectionBase";

    @Override
    public void updateTextCapability(Set<Connection.Listener> set, int i, int i2, int i3, int i4) {
        printDefaultLog("updateTextCapability");
    }

    void printDefaultLog(String str) {
        Rlog.d(TAG, str + " call to op base");
    }

    @Override
    public void stopRttTextProcessing(ImsRttTextHandler imsRttTextHandler, ImsPhoneConnection imsPhoneConnection) {
        printDefaultLog("stopRttTextProcessing");
    }

    @Override
    public void sendRttDowngradeRequest(MtkImsCall mtkImsCall, ImsRttTextHandler imsRttTextHandler, ImsPhoneConnection imsPhoneConnection) {
        printDefaultLog("sendRttDowngradeRequest");
    }

    @Override
    public void setRttIncomingCall(boolean z) {
        printDefaultLog("setRttIncomingCall");
    }

    @Override
    public boolean isIncomingRtt() {
        return false;
    }

    @Override
    public void setIncomingRttDuringEmcGuard(boolean z) {
        printDefaultLog("setIncomingRttDuringEmcGuard");
    }

    @Override
    public boolean isIncomingRttDuringEmcGuard() {
        printDefaultLog("isIncomingRttDuringEmcGuard");
        return false;
    }

    @Override
    public void setImsCall(ImsCall imsCall) {
    }

    @Override
    public boolean onSendRttModifyRequest(ImsPhoneConnection imsPhoneConnection) {
        return false;
    }
}
