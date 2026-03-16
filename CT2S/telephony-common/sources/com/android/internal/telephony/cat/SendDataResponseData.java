package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

class SendDataResponseData extends ResponseData {
    private int mLength;

    public SendDataResponseData(int len) {
        this.mLength = 0;
        this.mLength = len;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        int tag = ComprehensionTlvTag.CHANNEL_DATA_LENGTH.value();
        buf.write(tag);
        buf.write(1);
        buf.write(this.mLength & 255);
    }
}
