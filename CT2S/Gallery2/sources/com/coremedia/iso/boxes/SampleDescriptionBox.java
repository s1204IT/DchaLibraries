package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.FullContainerBox;
import java.nio.ByteBuffer;

public class SampleDescriptionBox extends FullContainerBox {
    public SampleDescriptionBox() {
        super("stsd");
    }

    @Override
    protected long getContentSize() {
        return super.getContentSize() + 4;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        content.get(new byte[4]);
        parseChildBoxes(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.boxes.size());
        writeChildBoxes(byteBuffer);
    }
}
