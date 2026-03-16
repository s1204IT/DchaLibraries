package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

class OpenChannelResponseData extends ResponseData {
    private BearerDescription bearer;
    private int bufSize;
    private Integer channelStatus;

    public OpenChannelResponseData(int bufSize, Integer channelStatus, BearerDescription bearer) {
        this.bufSize = bufSize;
        this.channelStatus = channelStatus;
        this.bearer = bearer;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (this.channelStatus != null) {
            int tag = ComprehensionTlvTag.CHANNEL_STATUS.value();
            buf.write(tag);
            buf.write(2);
            buf.write((this.channelStatus.intValue() >> 8) & 255);
            buf.write(this.channelStatus.intValue() & 255);
        }
        if (this.bearer != null) {
            int tag2 = ComprehensionTlvTag.BEARER_DESC.value();
            buf.write(tag2);
            if (this.bearer.parameters != null) {
                if (this.bearer.type.value() != 11 || this.bearer.parameters.length <= 1) {
                    int len = 1 + this.bearer.parameters.length;
                    buf.write(len);
                    buf.write(this.bearer.type.value() & 255);
                    buf.write(this.bearer.parameters, 0, this.bearer.parameters.length);
                } else {
                    int len2 = 1 + 2;
                    buf.write(len2);
                    buf.write(this.bearer.type.value() & 255);
                    buf.write(this.bearer.parameters, 0, 1);
                    buf.write(this.bearer.parameters, this.bearer.parameters.length - 1, 1);
                }
            } else {
                buf.write(1);
                buf.write(this.bearer.type.value() & 255);
            }
        }
        int tag3 = ComprehensionTlvTag.BUFFER_SIZE.value();
        buf.write(tag3);
        buf.write(2);
        buf.write((this.bufSize >> 8) & 255);
        buf.write(this.bufSize & 255);
    }
}
