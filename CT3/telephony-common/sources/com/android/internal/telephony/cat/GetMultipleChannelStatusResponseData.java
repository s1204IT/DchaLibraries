package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

class GetMultipleChannelStatusResponseData extends ResponseData {
    ArrayList mArrList;

    GetMultipleChannelStatusResponseData(ArrayList arrList) {
        this.mArrList = null;
        this.mArrList = arrList;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (buf == null) {
            return;
        }
        int tag = ComprehensionTlvTag.CHANNEL_STATUS.value() | 128;
        CatLog.d("[BIP]", "ChannelStatusResp: size: " + this.mArrList.size());
        if (this.mArrList.size() <= 0) {
            CatLog.d("[BIP]", "ChannelStatusResp: no channel status.");
            buf.write(tag);
            buf.write(2);
            buf.write(0);
            buf.write(0);
            return;
        }
        for (ChannelStatus chStatus : this.mArrList) {
            buf.write(tag);
            buf.write(2);
            buf.write((chStatus.mChannelId & 7) | chStatus.mChannelStatus);
            buf.write(chStatus.mChannelStatusInfo);
            CatLog.d("[BIP]", "ChannelStatusResp: cid:" + chStatus.mChannelId + ",status:" + chStatus.mChannelStatus + ",info:" + chStatus.mChannelStatusInfo);
        }
    }
}
