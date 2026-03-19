package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cat.ResponseData;
import java.io.ByteArrayOutputStream;

class OpenChannelResponseData extends ResponseData {
    BearerDesc mBearerDesc;
    int mBufferSize;
    ChannelStatus mChannelStatus;

    OpenChannelResponseData(ChannelStatus channelStatus, BearerDesc bearerDesc, int i) {
        this.mChannelStatus = null;
        this.mBearerDesc = null;
        this.mBufferSize = 0;
        if (channelStatus != null) {
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-constructor: channelStatus cid/status : " + channelStatus.mChannelId + "/" + channelStatus.mChannelStatus);
        } else {
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-constructor: channelStatus is null");
        }
        if (bearerDesc != null) {
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-constructor: bearerDesc bearerType " + bearerDesc.bearerType);
        } else {
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-constructor: bearerDesc is null");
        }
        MtkCatLog.d("[BIP]", "OpenChannelResponseData-constructor: buffer size is " + i);
        this.mChannelStatus = channelStatus;
        this.mBearerDesc = bearerDesc;
        this.mBufferSize = i;
    }

    public void format(ByteArrayOutputStream byteArrayOutputStream) {
        if (byteArrayOutputStream == null) {
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: buf is null");
            return;
        }
        if (this.mBearerDesc == null) {
            MtkCatLog.e("[BIP]", "OpenChannelResponseData-format: mBearerDesc is null");
            return;
        }
        if (((GPRSBearerDesc) this.mBearerDesc).bearerType != 2) {
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: bearer type is not gprs");
            return;
        }
        if (this.mBufferSize > 0) {
            if (this.mChannelStatus != null) {
                MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: Write channel status into TR");
                int iValue = ComprehensionTlvTag.CHANNEL_STATUS.value();
                MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: tag: " + iValue);
                byteArrayOutputStream.write(iValue);
                MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: length: 2");
                byteArrayOutputStream.write(2);
                StringBuilder sb = new StringBuilder();
                sb.append("OpenChannelResponseData-format: channel id & isActivated: ");
                sb.append(this.mChannelStatus.mChannelId | (this.mChannelStatus.isActivated ? 128 : 0));
                MtkCatLog.d("[BIP]", sb.toString());
                byteArrayOutputStream.write(this.mChannelStatus.mChannelId | (this.mChannelStatus.isActivated ? 128 : 0));
                MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: channel status: " + this.mChannelStatus.mChannelStatus);
                byteArrayOutputStream.write(this.mChannelStatus.mChannelStatus);
            }
            MtkCatLog.d("[BIP]", "Write bearer description into TR");
            int iValue2 = ComprehensionTlvTag.BEARER_DESCRIPTION.value();
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: tag: " + iValue2);
            byteArrayOutputStream.write(iValue2);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: length: 7");
            byteArrayOutputStream.write(7);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: bearer type: " + ((GPRSBearerDesc) this.mBearerDesc).bearerType);
            byteArrayOutputStream.write(((GPRSBearerDesc) this.mBearerDesc).bearerType);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: precedence: " + ((GPRSBearerDesc) this.mBearerDesc).precedence);
            byteArrayOutputStream.write(((GPRSBearerDesc) this.mBearerDesc).precedence);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: delay: " + ((GPRSBearerDesc) this.mBearerDesc).delay);
            byteArrayOutputStream.write(((GPRSBearerDesc) this.mBearerDesc).delay);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: reliability: " + ((GPRSBearerDesc) this.mBearerDesc).reliability);
            byteArrayOutputStream.write(((GPRSBearerDesc) this.mBearerDesc).reliability);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: peak: " + ((GPRSBearerDesc) this.mBearerDesc).peak);
            byteArrayOutputStream.write(((GPRSBearerDesc) this.mBearerDesc).peak);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: mean: " + ((GPRSBearerDesc) this.mBearerDesc).mean);
            byteArrayOutputStream.write(((GPRSBearerDesc) this.mBearerDesc).mean);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: pdp type: " + ((GPRSBearerDesc) this.mBearerDesc).pdpType);
            byteArrayOutputStream.write(((GPRSBearerDesc) this.mBearerDesc).pdpType);
            MtkCatLog.d("[BIP]", "Write buffer size into TR");
            int iValue3 = ComprehensionTlvTag.BUFFER_SIZE.value();
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: tag: " + iValue3);
            byteArrayOutputStream.write(iValue3);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: length: 2");
            byteArrayOutputStream.write(2);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: length(hi-byte): " + (this.mBufferSize >> 8));
            byteArrayOutputStream.write(this.mBufferSize >> 8);
            MtkCatLog.d("[BIP]", "OpenChannelResponseData-format: length(low-byte): " + (this.mBufferSize & 255));
            byteArrayOutputStream.write(this.mBufferSize & 255);
            return;
        }
        MtkCatLog.d("[BIP]", "Miss ChannelStatus, BearerDesc or BufferSize");
    }
}
