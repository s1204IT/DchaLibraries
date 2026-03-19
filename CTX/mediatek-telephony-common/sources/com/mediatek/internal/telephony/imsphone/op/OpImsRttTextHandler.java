package com.mediatek.internal.telephony.imsphone.op;

import android.os.Message;
import com.android.internal.telephony.imsphone.ImsRttTextHandler;

public interface OpImsRttTextHandler {
    void appendToNetworkBuffer(Message message, ImsRttTextHandler imsRttTextHandler);

    void attemptSendToNetwork(Message message, ImsRttTextHandler imsRttTextHandler);
}
