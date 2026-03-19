package com.android.internal.telephony.cat;

class CloseChannelParams extends CommandParams {
    boolean mBackToTcpListen;
    int mCloseCid;
    TextMessage textMsg;

    CloseChannelParams(CommandDetails cmdDet, int cid, TextMessage textMsg, boolean backToTcpListen) {
        super(cmdDet);
        this.textMsg = new TextMessage();
        this.mCloseCid = 0;
        this.mBackToTcpListen = false;
        this.textMsg = textMsg;
        this.mCloseCid = cid;
        this.mBackToTcpListen = backToTcpListen;
    }
}
