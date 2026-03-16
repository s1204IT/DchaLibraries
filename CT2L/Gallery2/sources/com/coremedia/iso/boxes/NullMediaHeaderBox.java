package com.coremedia.iso.boxes;

import java.nio.ByteBuffer;

public class NullMediaHeaderBox extends AbstractMediaHeaderBox {
    public NullMediaHeaderBox() {
        super("nmhd");
    }

    @Override
    protected long getContentSize() {
        return 4L;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
    }
}
