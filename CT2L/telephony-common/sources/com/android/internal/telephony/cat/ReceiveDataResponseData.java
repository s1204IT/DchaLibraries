package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

class ReceiveDataResponseData extends ResponseData {
    private byte[] mData;
    private int mLength;

    public ReceiveDataResponseData(byte[] data, int len) {
        this.mData = null;
        this.mLength = 0;
        this.mData = data;
        this.mLength = len;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        int tag = ComprehensionTlvTag.CHANNEL_DATA.value() | 128;
        if (this.mData != null) {
            buf.write(tag);
            if (this.mData.length > 127) {
                buf.write(129);
            }
            buf.write(this.mData.length & 255);
            byte[] arr$ = this.mData;
            for (byte b : arr$) {
                buf.write(b);
            }
        }
        int tag2 = ComprehensionTlvTag.CHANNEL_DATA_LENGTH.value() | 128;
        buf.write(tag2);
        buf.write(1);
        buf.write(this.mLength & 255);
    }
}
