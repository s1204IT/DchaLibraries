package com.android.internal.telephony.cat;

class CloseChannelParams extends CommandParams {
    int channel;

    CloseChannelParams(CommandDetails cmdDet, int channel) {
        super(cmdDet);
        this.channel = 0;
        this.channel = channel;
    }
}
