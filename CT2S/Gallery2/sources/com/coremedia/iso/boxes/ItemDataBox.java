package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class ItemDataBox extends AbstractBox {
    ByteBuffer data;

    public ItemDataBox() {
        super("idat");
        this.data = ByteBuffer.allocate(0);
    }

    @Override
    protected long getContentSize() {
        return this.data.limit();
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        this.data = content.slice();
        content.position(content.position() + content.remaining());
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(this.data);
    }
}
