package com.android.internal.telephony.cat;

class SendDataParams extends CommandParams {
    byte[] channelData;
    int mSendDataCid;
    int mSendMode;
    TextMessage textMsg;

    SendDataParams(CommandDetails cmdDet, byte[] data, int cid, TextMessage textMsg, int sendMode) {
        super(cmdDet);
        this.channelData = null;
        this.textMsg = new TextMessage();
        this.mSendDataCid = 0;
        this.mSendMode = 0;
        this.channelData = data;
        this.textMsg = textMsg;
        this.mSendDataCid = cid;
        this.mSendMode = sendMode;
    }
}
