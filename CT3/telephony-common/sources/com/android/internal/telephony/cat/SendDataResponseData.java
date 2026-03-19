package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

class SendDataResponseData extends ResponseData {
    int mTxBufferSize;

    SendDataResponseData(int size) {
        this.mTxBufferSize = 0;
        this.mTxBufferSize = size;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (buf == null) {
            return;
        }
        int tag = ComprehensionTlvTag.CHANNEL_DATA_LENGTH.value() | 128;
        buf.write(tag);
        buf.write(1);
        if (this.mTxBufferSize >= 255) {
            buf.write(255);
        } else {
            buf.write(this.mTxBufferSize);
        }
    }
}
