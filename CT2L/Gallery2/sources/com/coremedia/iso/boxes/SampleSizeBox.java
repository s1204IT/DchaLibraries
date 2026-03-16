package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;

public class SampleSizeBox extends AbstractFullBox {
    int sampleCount;
    private long sampleSize;
    private long[] sampleSizes;

    public SampleSizeBox() {
        super("stsz");
        this.sampleSizes = new long[0];
    }

    public long getSampleSize() {
        return this.sampleSize;
    }

    public long getSampleCount() {
        return this.sampleSize > 0 ? this.sampleCount : this.sampleSizes.length;
    }

    public long[] getSampleSizes() {
        return this.sampleSizes;
    }

    public void setSampleSizes(long[] sampleSizes) {
        this.sampleSizes = sampleSizes;
    }

    @Override
    protected long getContentSize() {
        return (this.sampleSize == 0 ? this.sampleSizes.length * 4 : 0) + 12;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.sampleSize = IsoTypeReader.readUInt32(content);
        this.sampleCount = CastUtils.l2i(IsoTypeReader.readUInt32(content));
        if (this.sampleSize == 0) {
            this.sampleSizes = new long[this.sampleCount];
            for (int i = 0; i < this.sampleCount; i++) {
                this.sampleSizes[i] = IsoTypeReader.readUInt32(content);
            }
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.sampleSize);
        if (this.sampleSize == 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.sampleSizes.length);
            long[] arr$ = this.sampleSizes;
            for (long sampleSize1 : arr$) {
                IsoTypeWriter.writeUInt32(byteBuffer, sampleSize1);
            }
            return;
        }
        IsoTypeWriter.writeUInt32(byteBuffer, this.sampleCount);
    }

    public String toString() {
        return "SampleSizeBox[sampleSize=" + getSampleSize() + ";sampleCount=" + getSampleCount() + "]";
    }
}
