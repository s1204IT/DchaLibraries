package com.mediatek.ims.internal.op;

import android.telephony.Rlog;
import android.telephony.ims.ImsCallSession;
import com.mediatek.ims.internal.MtkImsCallSession;

public class OpImsCallSessionBase {
    private static final String TAG = "OpImsCallSessionBase";

    public void callSessionTextCapabilityChanged(ImsCallSession.Listener listener, MtkImsCallSession mtkImsCallSession, int i, int i2, int i3, int i4) {
        printDefaultLog("callSessionTextCapabilityChanged");
    }

    public void callSessionRttEventReceived(ImsCallSession.Listener listener, MtkImsCallSession mtkImsCallSession, int i) {
        printDefaultLog("callSessionRttEventReceived");
    }

    void printDefaultLog(String str) {
        Rlog.d(TAG, str + " call to op base");
    }
}
