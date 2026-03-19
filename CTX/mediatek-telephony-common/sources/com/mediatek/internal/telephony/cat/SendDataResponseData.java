package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cat.ResponseData;
import java.io.ByteArrayOutputStream;

class SendDataResponseData extends ResponseData {
    int mTxBufferSize;

    SendDataResponseData(int i) {
        this.mTxBufferSize = 0;
        this.mTxBufferSize = i;
    }

    public void format(ByteArrayOutputStream byteArrayOutputStream) {
        if (byteArrayOutputStream == null) {
            return;
        }
        byteArrayOutputStream.write(128 | ComprehensionTlvTag.CHANNEL_DATA_LENGTH.value());
        byteArrayOutputStream.write(1);
        if (this.mTxBufferSize >= 255) {
            byteArrayOutputStream.write(255);
        } else {
            byteArrayOutputStream.write(this.mTxBufferSize);
        }
    }
}
