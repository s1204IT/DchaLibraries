package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class FreeSpaceBox extends AbstractBox {
    byte[] data;

    @Override
    protected long getContentSize() {
        return this.data.length;
    }

    public FreeSpaceBox() {
        super(SchemaSymbols.ATTVAL_SKIP);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        this.data = new byte[byteBuffer.remaining()];
        byteBuffer.get(this.data);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(this.data);
    }

    public String toString() {
        return "FreeSpaceBox[size=" + this.data.length + ";type=" + getType() + "]";
    }
}
