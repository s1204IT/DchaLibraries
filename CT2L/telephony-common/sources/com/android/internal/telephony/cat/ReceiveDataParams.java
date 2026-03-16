package com.android.internal.telephony.cat;

class ReceiveDataParams extends CommandParams {
    int channel;
    int datLen;

    ReceiveDataParams(CommandDetails cmdDet, int channel, int datLen) {
        super(cmdDet);
        this.datLen = 0;
        this.channel = 0;
        this.channel = channel;
        this.datLen = datLen;
    }
}
