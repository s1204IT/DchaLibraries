package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;

public class StaticChunkOffsetBox extends ChunkOffsetBox {
    private long[] chunkOffsets;

    public StaticChunkOffsetBox() {
        super("stco");
        this.chunkOffsets = new long[0];
    }

    @Override
    public long[] getChunkOffsets() {
        return this.chunkOffsets;
    }

    @Override
    protected long getContentSize() {
        return (this.chunkOffsets.length * 4) + 8;
    }

    public void setChunkOffsets(long[] chunkOffsets) {
        this.chunkOffsets = chunkOffsets;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int entryCount = CastUtils.l2i(IsoTypeReader.readUInt32(content));
        this.chunkOffsets = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            this.chunkOffsets[i] = IsoTypeReader.readUInt32(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.chunkOffsets.length);
        long[] arr$ = this.chunkOffsets;
        for (long chunkOffset : arr$) {
            IsoTypeWriter.writeUInt32(byteBuffer, chunkOffset);
        }
    }
}
