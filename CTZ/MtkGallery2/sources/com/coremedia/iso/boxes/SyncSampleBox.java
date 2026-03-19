package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;

public class SyncSampleBox extends AbstractFullBox {
    private long[] sampleNumber;

    public SyncSampleBox() {
        super("stss");
    }

    public long[] getSampleNumber() {
        return this.sampleNumber;
    }

    @Override
    protected long getContentSize() {
        return (this.sampleNumber.length * 4) + 8;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        int iL2i = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.sampleNumber = new long[iL2i];
        for (int i = 0; i < iL2i; i++) {
            this.sampleNumber[i] = IsoTypeReader.readUInt32(byteBuffer);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.sampleNumber.length);
        for (long j : this.sampleNumber) {
            IsoTypeWriter.writeUInt32(byteBuffer, j);
        }
    }

    public String toString() {
        return "SyncSampleBox[entryCount=" + this.sampleNumber.length + "]";
    }

    public void setSampleNumber(long[] jArr) {
        this.sampleNumber = jArr;
    }
}
