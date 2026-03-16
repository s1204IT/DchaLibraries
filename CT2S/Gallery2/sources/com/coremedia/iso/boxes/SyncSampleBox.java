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
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int entryCount = CastUtils.l2i(IsoTypeReader.readUInt32(content));
        this.sampleNumber = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            this.sampleNumber[i] = IsoTypeReader.readUInt32(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.sampleNumber.length);
        long[] arr$ = this.sampleNumber;
        for (long aSampleNumber : arr$) {
            IsoTypeWriter.writeUInt32(byteBuffer, aSampleNumber);
        }
    }

    public String toString() {
        return "SyncSampleBox[entryCount=" + this.sampleNumber.length + "]";
    }

    public void setSampleNumber(long[] sampleNumber) {
        this.sampleNumber = sampleNumber;
    }
}
