package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.TextMessage;

class GetChannelStatusParams extends CommandParams {
    TextMessage textMsg;

    GetChannelStatusParams(CommandDetails commandDetails, TextMessage textMessage) {
        super(commandDetails);
        this.textMsg = new TextMessage();
        this.textMsg = textMessage;
    }
}
