package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class UnknownBox extends AbstractBox {
    ByteBuffer data;

    @Override
    protected long getContentSize() {
        return this.data.limit();
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        this.data = content;
        content.position(content.position() + content.remaining());
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        this.data.rewind();
        byteBuffer.put(this.data);
    }
}
