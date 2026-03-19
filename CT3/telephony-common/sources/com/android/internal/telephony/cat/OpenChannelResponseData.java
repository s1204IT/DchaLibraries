package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

class OpenChannelResponseData extends ResponseData {
    BearerDesc mBearerDesc;
    int mBufferSize;
    ChannelStatus mChannelStatus;

    OpenChannelResponseData(ChannelStatus channelStatus, BearerDesc bearerDesc, int bufferSize) {
        this.mChannelStatus = null;
        this.mBearerDesc = null;
        this.mBufferSize = 0;
        if (channelStatus != null) {
            CatLog.d("[BIP]", "OpenChannelResponseData-constructor: channelStatus cid/status : " + channelStatus.mChannelId + "/" + channelStatus.mChannelStatus);
        } else {
            CatLog.d("[BIP]", "OpenChannelResponseData-constructor: channelStatus is null");
        }
        if (bearerDesc != null) {
            CatLog.d("[BIP]", "OpenChannelResponseData-constructor: bearerDesc bearerType " + bearerDesc.bearerType);
        } else {
            CatLog.d("[BIP]", "OpenChannelResponseData-constructor: bearerDesc is null");
        }
        CatLog.d("[BIP]", "OpenChannelResponseData-constructor: buffer size is " + bufferSize);
        this.mChannelStatus = channelStatus;
        this.mBearerDesc = bearerDesc;
        this.mBufferSize = bufferSize;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (buf == null) {
            CatLog.d("[BIP]", "OpenChannelResponseData-format: buf is null");
            return;
        }
        if (this.mBearerDesc == null) {
            CatLog.e("[BIP]", "OpenChannelResponseData-format: mBearerDesc is null");
            return;
        }
        if (((GPRSBearerDesc) this.mBearerDesc).bearerType != 2) {
            CatLog.d("[BIP]", "OpenChannelResponseData-format: bearer type is not gprs");
            return;
        }
        if (this.mBufferSize > 0) {
            if (this.mChannelStatus != null) {
                CatLog.d("[BIP]", "OpenChannelResponseData-format: Write channel status into TR");
                int tag = ComprehensionTlvTag.CHANNEL_STATUS.value();
                CatLog.d("[BIP]", "OpenChannelResponseData-format: tag: " + tag);
                buf.write(tag);
                CatLog.d("[BIP]", "OpenChannelResponseData-format: length: 2");
                buf.write(2);
                CatLog.d("[BIP]", "OpenChannelResponseData-format: channel id & isActivated: " + ((this.mChannelStatus.isActivated ? 128 : 0) | this.mChannelStatus.mChannelId));
                buf.write(this.mChannelStatus.mChannelId | (this.mChannelStatus.isActivated ? 128 : 0));
                CatLog.d("[BIP]", "OpenChannelResponseData-format: channel status: " + this.mChannelStatus.mChannelStatus);
                buf.write(this.mChannelStatus.mChannelStatus);
            }
            CatLog.d("[BIP]", "Write bearer description into TR");
            int tag2 = ComprehensionTlvTag.BEARER_DESCRIPTION.value();
            CatLog.d("[BIP]", "OpenChannelResponseData-format: tag: " + tag2);
            buf.write(tag2);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: length: 7");
            buf.write(7);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: bearer type: " + ((GPRSBearerDesc) this.mBearerDesc).bearerType);
            buf.write(((GPRSBearerDesc) this.mBearerDesc).bearerType);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: precedence: " + ((GPRSBearerDesc) this.mBearerDesc).precedence);
            buf.write(((GPRSBearerDesc) this.mBearerDesc).precedence);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: delay: " + ((GPRSBearerDesc) this.mBearerDesc).delay);
            buf.write(((GPRSBearerDesc) this.mBearerDesc).delay);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: reliability: " + ((GPRSBearerDesc) this.mBearerDesc).reliability);
            buf.write(((GPRSBearerDesc) this.mBearerDesc).reliability);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: peak: " + ((GPRSBearerDesc) this.mBearerDesc).peak);
            buf.write(((GPRSBearerDesc) this.mBearerDesc).peak);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: mean: " + ((GPRSBearerDesc) this.mBearerDesc).mean);
            buf.write(((GPRSBearerDesc) this.mBearerDesc).mean);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: pdp type: " + ((GPRSBearerDesc) this.mBearerDesc).pdpType);
            buf.write(((GPRSBearerDesc) this.mBearerDesc).pdpType);
            CatLog.d("[BIP]", "Write buffer size into TR");
            int tag3 = ComprehensionTlvTag.BUFFER_SIZE.value();
            CatLog.d("[BIP]", "OpenChannelResponseData-format: tag: " + tag3);
            buf.write(tag3);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: length: 2");
            buf.write(2);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: length(hi-byte): " + (this.mBufferSize >> 8));
            buf.write(this.mBufferSize >> 8);
            CatLog.d("[BIP]", "OpenChannelResponseData-format: length(low-byte): " + (this.mBufferSize & 255));
            buf.write(this.mBufferSize & 255);
            return;
        }
        CatLog.d("[BIP]", "Miss ChannelStatus, BearerDesc or BufferSize");
    }
}
