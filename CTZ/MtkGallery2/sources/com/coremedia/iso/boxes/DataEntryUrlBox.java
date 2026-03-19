package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class DataEntryUrlBox extends AbstractFullBox {
    public DataEntryUrlBox() {
        super("url ");
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
    }

    @Override
    protected long getContentSize() {
        return 4L;
    }

    public String toString() {
        return "DataEntryUrlBox[]";
    }
}
