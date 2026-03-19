package com.googlecode.mp4parser.boxes.basemediaformat;

import com.coremedia.iso.boxes.h264.AvcConfigurationBox;
import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class AvcNalUnitStorageBox extends AbstractBox {
    AvcConfigurationBox.AVCDecoderConfigurationRecord avcDecoderConfigurationRecord;

    public AvcNalUnitStorageBox() {
        super("avcn");
    }

    @Override
    protected long getContentSize() {
        return this.avcDecoderConfigurationRecord.getContentSize();
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        this.avcDecoderConfigurationRecord = new AvcConfigurationBox.AVCDecoderConfigurationRecord(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        this.avcDecoderConfigurationRecord.getContent(byteBuffer);
    }

    public String toString() {
        return "AvcNalUnitStorageBox{SPS=" + this.avcDecoderConfigurationRecord.getSequenceParameterSetsAsStrings() + ",PPS=" + this.avcDecoderConfigurationRecord.getPictureParameterSetsAsStrings() + ",lengthSize=" + (this.avcDecoderConfigurationRecord.lengthSizeMinusOne + 1) + '}';
    }
}
