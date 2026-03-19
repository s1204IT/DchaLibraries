package com.mediatek.internal.telephony.imsphone.op;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.ims.ImsCallProfile;
import android.util.Log;
import com.android.ims.ImsCall;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;

public class OpCommonImsPhoneCallTrackerBase implements OpCommonImsPhoneCallTracker {
    private static final String TAG = "OpImsPhoneCallTrackerBase";

    @Override
    public void onTextCapabilityChanged(ImsPhoneConnection imsPhoneConnection, int i, int i2, int i3, int i4) {
        printDefaultLog("onTextCapabilityChanged");
    }

    @Override
    public void onRttEventReceived(ImsPhoneConnection imsPhoneConnection, int i) {
        printDefaultLog("onRttEventReceived");
    }

    void printDefaultLog(String str) {
        Log.d(TAG, str + " call to op base");
    }

    @Override
    public void initRtt(ImsPhone imsPhone) {
        printDefaultLog("initRtt");
    }

    @Override
    public void disposeRtt(ImsPhone imsPhone, ImsPhoneCall imsPhoneCall, Call.SrvccState srvccState) {
        printDefaultLog("disposeRtt");
    }

    @Override
    public void stopRttEmcGuardTimer() {
        printDefaultLog("stopRttEmcGuardTimer");
    }

    @Override
    public void checkRttCallType(ImsPhone imsPhone, ImsPhoneCall imsPhoneCall, Call.SrvccState srvccState) {
        printDefaultLog("checkRttCallType");
    }

    @Override
    public void setRttMode(Bundle bundle, ImsCallProfile imsCallProfile) {
        printDefaultLog("setRttMode");
    }

    @Override
    public void sendRttSrvccOrCsfbEvent(ImsPhoneCall imsPhoneCall) {
        printDefaultLog("sendRttSrvccOrCsfbEvent");
    }

    @Override
    public void checkIncomingRtt(Intent intent, ImsCall imsCall, ImsPhoneConnection imsPhoneConnection) {
        printDefaultLog("checkIncomingRtt");
    }

    @Override
    public void processRttModifyFailCase(ImsCall imsCall, int i, ImsPhoneConnection imsPhoneConnection) {
        printDefaultLog("processRttModifyFailCase");
    }

    @Override
    public void processRttModifySuccessCase(ImsCall imsCall, int i, ImsPhoneConnection imsPhoneConnection) {
        printDefaultLog("processRttModifySuccessCase");
    }

    @Override
    public void startRttEmcGuardTimer(ImsPhone imsPhone) {
        printDefaultLog("startRttEmcGuardTimer");
    }

    @Override
    public boolean isRttCallInvolved(ImsCall imsCall, ImsCall imsCall2) {
        printDefaultLog("isRttCallInvolved");
        return false;
    }

    @Override
    public boolean isAllowMergeRttCallToVoiceOnly() {
        printDefaultLog("isAllowMergeRttCallToVoiceOnly");
        return true;
    }
}
