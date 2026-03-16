package com.googlecode.mp4parser.boxes.cenc;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.UUIDConverter;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ProtectionSystemSpecificHeaderBox extends AbstractFullBox {
    static final boolean $assertionsDisabled;
    public static byte[] OMA2_SYSTEM_ID;
    public static byte[] PLAYREADY_SYSTEM_ID;
    byte[] content;
    byte[] systemId;

    static {
        $assertionsDisabled = !ProtectionSystemSpecificHeaderBox.class.desiredAssertionStatus();
        OMA2_SYSTEM_ID = UUIDConverter.convert(UUID.fromString("A2B55680-6F43-11E0-9A3F-0002A5D5C51B"));
        PLAYREADY_SYSTEM_ID = UUIDConverter.convert(UUID.fromString("9A04F079-9840-4286-AB92-E65BE0885F95"));
    }

    public ProtectionSystemSpecificHeaderBox() {
        super("pssh");
    }

    @Override
    protected long getContentSize() {
        return this.content.length + 24;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (!$assertionsDisabled && this.systemId.length != 16) {
            throw new AssertionError();
        }
        byteBuffer.put(this.systemId, 0, 16);
        IsoTypeWriter.writeUInt32(byteBuffer, this.content.length);
        byteBuffer.put(this.content);
    }

    @Override
    protected void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.systemId = new byte[16];
        content.get(this.systemId);
        long length = IsoTypeReader.readUInt32(content);
        this.content = new byte[content.remaining()];
        content.get(this.content);
        if (!$assertionsDisabled && length != this.content.length) {
            throw new AssertionError();
        }
    }
}
