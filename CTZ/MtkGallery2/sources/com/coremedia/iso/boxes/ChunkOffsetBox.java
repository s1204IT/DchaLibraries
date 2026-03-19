package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractFullBox;

public abstract class ChunkOffsetBox extends AbstractFullBox {
    public abstract long[] getChunkOffsets();

    public ChunkOffsetBox(String str) {
        super(str);
    }

    public String toString() {
        return getClass().getSimpleName() + "[entryCount=" + getChunkOffsets().length + "]";
    }
}
