package com.android.internal.telephony.cat;

class CallCtrlBySimParams extends CommandParams {
    String mDestAddress;
    int mInfoType;
    TextMessage mTextMsg;

    CallCtrlBySimParams(CommandDetails cmdDet, TextMessage textMsg, int infoType, String destAddress) {
        super(cmdDet);
        this.mTextMsg = textMsg;
        this.mInfoType = infoType;
        this.mDestAddress = destAddress;
    }
}
