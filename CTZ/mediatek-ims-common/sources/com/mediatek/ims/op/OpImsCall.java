package com.mediatek.ims.op;

import android.telephony.ims.ImsCallProfile;
import com.android.ims.ImsCall;
import com.mediatek.ims.MtkImsCall;
import com.mediatek.ims.internal.MtkImsCallSession;

public interface OpImsCall {
    void callSessionRttEventReceived(ImsCall.Listener listener, MtkImsCall mtkImsCall, int i);

    void callSessionTextCapabilityChanged(ImsCall.Listener listener, MtkImsCall mtkImsCall, int i, int i2, int i3, int i4);

    void notifyRttDowngradeEvent(ImsCall.Listener listener, MtkImsCall mtkImsCall);

    void sendRttDowngradeRequest(ImsCallProfile imsCallProfile, MtkImsCallSession mtkImsCallSession);

    void setRttMode(int i, ImsCallProfile imsCallProfile);
}
