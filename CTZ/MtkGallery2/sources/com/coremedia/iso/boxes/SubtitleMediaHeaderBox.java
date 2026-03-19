package com.coremedia.iso.boxes;

import java.nio.ByteBuffer;

public class SubtitleMediaHeaderBox extends AbstractMediaHeaderBox {
    public SubtitleMediaHeaderBox() {
        super("sthd");
    }

    @Override
    protected long getContentSize() {
        return 4L;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
    }

    public String toString() {
        return "SubtitleMediaHeaderBox";
    }
}
