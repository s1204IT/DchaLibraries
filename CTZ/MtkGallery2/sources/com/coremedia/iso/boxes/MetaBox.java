package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractContainerBox;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MetaBox extends AbstractContainerBox {
    private int flags;
    private int version;

    public MetaBox() {
        super("meta");
        this.version = 0;
        this.flags = 0;
    }

    @Override
    public long getContentSize() {
        if (isMp4Box()) {
            return 4 + super.getContentSize();
        }
        return super.getContentSize();
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        int iPosition = byteBuffer.position();
        byteBuffer.get(new byte[4]);
        if ("hdlr".equals(IsoTypeReader.read4cc(byteBuffer))) {
            byteBuffer.position(iPosition);
            this.version = -1;
            this.flags = -1;
        } else {
            byteBuffer.position(iPosition);
            this.version = IsoTypeReader.readUInt8(byteBuffer);
            this.flags = IsoTypeReader.readUInt24(byteBuffer);
        }
        while (byteBuffer.remaining() >= 8) {
            try {
                this.boxes.add(this.boxParser.parseBox(new ByteBufferByteChannel(byteBuffer), this));
            } catch (IOException e) {
                throw new RuntimeException("Sebastian needs to fix 7518765283");
            }
        }
        if (byteBuffer.remaining() > 0) {
            throw new RuntimeException("Sebastian needs to fix it 90732r26537");
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        if (isMp4Box()) {
            IsoTypeWriter.writeUInt8(byteBuffer, this.version);
            IsoTypeWriter.writeUInt24(byteBuffer, this.flags);
        }
        writeChildBoxes(byteBuffer);
    }

    public boolean isMp4Box() {
        return (this.version == -1 || this.flags == -1) ? false : true;
    }
}
