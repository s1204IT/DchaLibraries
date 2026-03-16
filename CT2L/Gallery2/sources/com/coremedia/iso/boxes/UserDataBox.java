package com.coremedia.iso.boxes;

import com.coremedia.iso.BoxParser;
import com.googlecode.mp4parser.AbstractContainerBox;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class UserDataBox extends AbstractContainerBox {
    @Override
    protected long getContentSize() {
        return super.getContentSize();
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        super.parse(readableByteChannel, header, contentSize, boxParser);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        super._parseDetails(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        super.getContent(byteBuffer);
    }

    public UserDataBox() {
        super("udta");
    }
}
