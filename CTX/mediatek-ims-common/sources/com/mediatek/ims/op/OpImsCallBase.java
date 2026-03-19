package com.mediatek.ims.op;

import android.telephony.ims.ImsCallProfile;
import android.util.Log;
import com.android.ims.ImsCall;
import com.mediatek.ims.MtkImsCall;
import com.mediatek.ims.internal.MtkImsCallSession;

public class OpImsCallBase implements OpImsCall {
    private static final String TAG = "OpImsCallBase";

    @Override
    public void callSessionTextCapabilityChanged(ImsCall.Listener listener, MtkImsCall mtkImsCall, int i, int i2, int i3, int i4) {
        printDefaultLog("callSessionTextCapabilityChanged");
    }

    @Override
    public void callSessionRttEventReceived(ImsCall.Listener listener, MtkImsCall mtkImsCall, int i) {
        printDefaultLog("callSessionRttEventReceived");
    }

    void printDefaultLog(String str) {
        Log.d(TAG, str + " call to op base");
    }

    @Override
    public void sendRttDowngradeRequest(ImsCallProfile imsCallProfile, MtkImsCallSession mtkImsCallSession) {
        printDefaultLog("sendRttDowngradeRequest");
    }

    @Override
    public void setRttMode(int i, ImsCallProfile imsCallProfile) {
        printDefaultLog("setRttMode");
    }

    @Override
    public void notifyRttDowngradeEvent(ImsCall.Listener listener, MtkImsCall mtkImsCall) {
        printDefaultLog("notifyRttDowngradeEvent");
    }
}
