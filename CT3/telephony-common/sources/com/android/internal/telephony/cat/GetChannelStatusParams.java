package com.android.internal.telephony.cat;

class GetChannelStatusParams extends CommandParams {
    TextMessage textMsg;

    GetChannelStatusParams(CommandDetails cmdDet, TextMessage textMsg) {
        super(cmdDet);
        this.textMsg = new TextMessage();
        this.textMsg = textMsg;
    }
}
