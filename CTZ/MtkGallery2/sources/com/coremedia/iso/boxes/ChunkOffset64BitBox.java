package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;

public class ChunkOffset64BitBox extends ChunkOffsetBox {
    private long[] chunkOffsets;

    public ChunkOffset64BitBox() {
        super("co64");
    }

    @Override
    public long[] getChunkOffsets() {
        return this.chunkOffsets;
    }

    @Override
    protected long getContentSize() {
        return 8 + (this.chunkOffsets.length * 8);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        int iL2i = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.chunkOffsets = new long[iL2i];
        for (int i = 0; i < iL2i; i++) {
            this.chunkOffsets[i] = IsoTypeReader.readUInt64(byteBuffer);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.chunkOffsets.length);
        for (long j : this.chunkOffsets) {
            IsoTypeWriter.writeUInt64(byteBuffer, j);
        }
    }
}
