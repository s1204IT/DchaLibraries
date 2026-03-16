package com.googlecode.mp4parser.boxes.piff;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class TfxdBox extends AbstractFullBox {
    public long fragmentAbsoluteDuration;
    public long fragmentAbsoluteTime;

    public TfxdBox() {
        super("uuid");
    }

    @Override
    public byte[] getUserType() {
        return new byte[]{109, 29, -101, 5, 66, -43, 68, -26, -128, -30, 20, 29, -81, -9, 87, -78};
    }

    @Override
    protected long getContentSize() {
        return getVersion() == 1 ? 20L : 12L;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if (getVersion() == 1) {
            this.fragmentAbsoluteTime = IsoTypeReader.readUInt64(content);
            this.fragmentAbsoluteDuration = IsoTypeReader.readUInt64(content);
        } else {
            this.fragmentAbsoluteTime = IsoTypeReader.readUInt32(content);
            this.fragmentAbsoluteDuration = IsoTypeReader.readUInt32(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt64(byteBuffer, this.fragmentAbsoluteTime);
            IsoTypeWriter.writeUInt64(byteBuffer, this.fragmentAbsoluteDuration);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, this.fragmentAbsoluteTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.fragmentAbsoluteDuration);
        }
    }
}
