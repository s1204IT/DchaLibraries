package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.FullContainerBox;
import java.nio.ByteBuffer;

public class DataReferenceBox extends FullContainerBox {
    public DataReferenceBox() {
        super("dref");
    }

    @Override
    protected long getContentSize() {
        return super.getContentSize() + 4;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        byteBuffer.get(new byte[4]);
        parseChildBoxes(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, getBoxes().size());
        writeChildBoxes(byteBuffer);
    }
}
