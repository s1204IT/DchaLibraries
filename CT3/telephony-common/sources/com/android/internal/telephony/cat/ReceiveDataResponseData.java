package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

class ReceiveDataResponseData extends ResponseData {
    byte[] mData;
    int mRemainingCount;

    ReceiveDataResponseData(byte[] data, int remaining) {
        this.mData = null;
        this.mRemainingCount = 0;
        this.mData = data;
        this.mRemainingCount = remaining;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (buf == null) {
            return;
        }
        int tag = ComprehensionTlvTag.CHANNEL_DATA.value() | 128;
        buf.write(tag);
        if (this.mData != null) {
            if (this.mData.length >= 128) {
                buf.write(129);
            }
            buf.write(this.mData.length);
            buf.write(this.mData, 0, this.mData.length);
        } else {
            buf.write(0);
        }
        int tag2 = ComprehensionTlvTag.CHANNEL_DATA_LENGTH.value() | 128;
        buf.write(tag2);
        buf.write(1);
        CatLog.d("[BIP]", "ReceiveDataResponseData: length: " + this.mRemainingCount);
        if (this.mRemainingCount >= 255) {
            buf.write(255);
        } else {
            buf.write(this.mRemainingCount);
        }
    }
}
