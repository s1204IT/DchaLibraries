package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.TextMessage;

class CloseChannelParams extends CommandParams {
    boolean mBackToTcpListen;
    int mCloseCid;
    TextMessage textMsg;

    CloseChannelParams(CommandDetails commandDetails, int i, TextMessage textMessage, boolean z) {
        super(commandDetails);
        this.textMsg = new TextMessage();
        this.mCloseCid = 0;
        this.mBackToTcpListen = false;
        this.textMsg = textMessage;
        this.mCloseCid = i;
        this.mBackToTcpListen = z;
    }
}
