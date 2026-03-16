package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class FreeSpaceBox extends AbstractBox {
    byte[] data;

    @Override
    protected long getContentSize() {
        return this.data.length;
    }

    public FreeSpaceBox() {
        super("skip");
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        this.data = new byte[content.remaining()];
        content.get(this.data);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(this.data);
    }

    public String toString() {
        return "FreeSpaceBox[size=" + this.data.length + ";type=" + getType() + "]";
    }
}
