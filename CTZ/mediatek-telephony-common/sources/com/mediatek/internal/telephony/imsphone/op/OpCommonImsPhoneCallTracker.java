package com.mediatek.internal.telephony.imsphone.op;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.ims.ImsCallProfile;
import com.android.ims.ImsCall;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;

public interface OpCommonImsPhoneCallTracker {
    void checkIncomingRtt(Intent intent, ImsCall imsCall, ImsPhoneConnection imsPhoneConnection);

    void checkRttCallType(ImsPhone imsPhone, ImsPhoneCall imsPhoneCall, Call.SrvccState srvccState);

    void disposeRtt(ImsPhone imsPhone, ImsPhoneCall imsPhoneCall, Call.SrvccState srvccState);

    void initRtt(ImsPhone imsPhone);

    boolean isAllowMergeRttCallToVoiceOnly();

    boolean isRttCallInvolved(ImsCall imsCall, ImsCall imsCall2);

    void onRttEventReceived(ImsPhoneConnection imsPhoneConnection, int i);

    void onTextCapabilityChanged(ImsPhoneConnection imsPhoneConnection, int i, int i2, int i3, int i4);

    void processRttModifyFailCase(ImsCall imsCall, int i, ImsPhoneConnection imsPhoneConnection);

    void processRttModifySuccessCase(ImsCall imsCall, int i, ImsPhoneConnection imsPhoneConnection);

    void sendRttSrvccOrCsfbEvent(ImsPhoneCall imsPhoneCall);

    void setRttMode(Bundle bundle, ImsCallProfile imsCallProfile);

    void startRttEmcGuardTimer(ImsPhone imsPhone);

    void stopRttEmcGuardTimer();
}
