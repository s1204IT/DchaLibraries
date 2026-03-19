package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cat.ResponseData;
import java.io.ByteArrayOutputStream;

class ReceiveDataResponseData extends ResponseData {
    byte[] mData;
    int mRemainingCount;

    ReceiveDataResponseData(byte[] bArr, int i) {
        this.mData = null;
        this.mRemainingCount = 0;
        this.mData = bArr;
        this.mRemainingCount = i;
    }

    public void format(ByteArrayOutputStream byteArrayOutputStream) {
        if (byteArrayOutputStream == null) {
            return;
        }
        byteArrayOutputStream.write(ComprehensionTlvTag.CHANNEL_DATA.value() | 128);
        if (this.mData != null) {
            if (this.mData.length >= 128) {
                byteArrayOutputStream.write(129);
            }
            byteArrayOutputStream.write(this.mData.length);
            byteArrayOutputStream.write(this.mData, 0, this.mData.length);
        } else {
            byteArrayOutputStream.write(0);
        }
        byteArrayOutputStream.write(ComprehensionTlvTag.CHANNEL_DATA_LENGTH.value() | 128);
        byteArrayOutputStream.write(1);
        MtkCatLog.d("[BIP]", "ReceiveDataResponseData: length: " + this.mRemainingCount);
        if (this.mRemainingCount >= 255) {
            byteArrayOutputStream.write(255);
        } else {
            byteArrayOutputStream.write(this.mRemainingCount);
        }
    }
}
