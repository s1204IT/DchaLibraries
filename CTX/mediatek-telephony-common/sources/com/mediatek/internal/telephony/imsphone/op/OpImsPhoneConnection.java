package com.mediatek.internal.telephony.imsphone.op;

import com.android.ims.ImsCall;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.imsphone.ImsRttTextHandler;
import com.mediatek.ims.MtkImsCall;
import java.util.Set;

public interface OpImsPhoneConnection {
    boolean isIncomingRtt();

    boolean isIncomingRttDuringEmcGuard();

    boolean onSendRttModifyRequest(ImsPhoneConnection imsPhoneConnection);

    void sendRttDowngradeRequest(MtkImsCall mtkImsCall, ImsRttTextHandler imsRttTextHandler, ImsPhoneConnection imsPhoneConnection);

    void setImsCall(ImsCall imsCall);

    void setIncomingRttDuringEmcGuard(boolean z);

    void setRttIncomingCall(boolean z);

    void stopRttTextProcessing(ImsRttTextHandler imsRttTextHandler, ImsPhoneConnection imsPhoneConnection);

    void updateTextCapability(Set<Connection.Listener> set, int i, int i2, int i3, int i4);
}
