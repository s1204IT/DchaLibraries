package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.TextMessage;

class ReceiveDataParams extends CommandParams {
    int channelDataLength;
    int mReceiveDataCid;
    TextMessage textMsg;

    ReceiveDataParams(CommandDetails commandDetails, int i, int i2, TextMessage textMessage) {
        super(commandDetails);
        this.channelDataLength = 0;
        this.textMsg = new TextMessage();
        this.mReceiveDataCid = 0;
        this.channelDataLength = i;
        this.textMsg = textMessage;
        this.mReceiveDataCid = i2;
    }
}
