package com.android.internal.telephony.cat;

class SendDataParams extends CommandParams {
    int channel;
    byte[] data;

    SendDataParams(CommandDetails cmdDet, int channel, byte[] data) {
        super(cmdDet);
        this.data = null;
        this.channel = 0;
        this.channel = channel;
        this.data = data;
    }
}
