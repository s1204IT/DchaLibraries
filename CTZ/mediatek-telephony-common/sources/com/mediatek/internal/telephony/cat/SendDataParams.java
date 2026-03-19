package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.TextMessage;

class SendDataParams extends CommandParams {
    byte[] channelData;
    int mSendDataCid;
    int mSendMode;
    TextMessage textMsg;

    SendDataParams(CommandDetails commandDetails, byte[] bArr, int i, TextMessage textMessage, int i2) {
        super(commandDetails);
        this.channelData = null;
        this.textMsg = new TextMessage();
        this.mSendDataCid = 0;
        this.mSendMode = 0;
        this.channelData = bArr;
        this.textMsg = textMessage;
        this.mSendDataCid = i;
        this.mSendMode = i2;
    }
}
