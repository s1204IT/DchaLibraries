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
        if (this.sampleSize > 0) {
            return this.sampleCount;
        }
        return this.sampleSizes.length;
    }

    public long[] getSampleSizes() {
        return this.sampleSizes;
    }

    public void setSampleSizes(long[] jArr) {
        this.sampleSizes = jArr;
    }

    @Override
    protected long getContentSize() {
        return 12 + (this.sampleSize == 0 ? this.sampleSizes.length * 4 : 0);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.sampleSize = IsoTypeReader.readUInt32(byteBuffer);
        this.sampleCount = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        if (this.sampleSize == 0) {
            this.sampleSizes = new long[this.sampleCount];
            for (int i = 0; i < this.sampleCount; i++) {
                this.sampleSizes[i] = IsoTypeReader.readUInt32(byteBuffer);
            }
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.sampleSize);
        if (this.sampleSize == 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.sampleSizes.length);
            for (long j : this.sampleSizes) {
                IsoTypeWriter.writeUInt32(byteBuffer, j);
            }
            return;
        }
        IsoTypeWriter.writeUInt32(byteBuffer, this.sampleCount);
    }

    public String toString() {
        return "SampleSizeBox[sampleSize=" + getSampleSize() + ";sampleCount=" + getSampleCount() + "]";
    }
}
