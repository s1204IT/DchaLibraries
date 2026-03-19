package com.mediatek.internal.telephony.imsphone.op;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.imsphone.ImsRttTextHandler;

public class OpImsRttTextHandlerBase implements OpImsRttTextHandler {
    private static final String TAG = "OpImsRttTextHandlerBase";

    void printDefaultLog(String str) {
        Rlog.d(TAG, str + " call to op base");
    }

    @Override
    public void appendToNetworkBuffer(Message message, ImsRttTextHandler imsRttTextHandler) {
    }

    @Override
    public void attemptSendToNetwork(Message message, ImsRttTextHandler imsRttTextHandler) {
    }
}
