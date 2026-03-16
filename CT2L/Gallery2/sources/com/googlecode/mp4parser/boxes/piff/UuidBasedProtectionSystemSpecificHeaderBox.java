package com.googlecode.mp4parser.boxes.piff;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import com.googlecode.mp4parser.util.UUIDConverter;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidBasedProtectionSystemSpecificHeaderBox extends AbstractFullBox {
    public static byte[] USER_TYPE = {-48, -118, 79, 24, 16, -13, 74, -126, -74, -56, 50, -40, -85, -95, -125, -45};
    ProtectionSpecificHeader protectionSpecificHeader;
    UUID systemId;

    public UuidBasedProtectionSystemSpecificHeaderBox() {
        super("uuid", USER_TYPE);
    }

    @Override
    protected long getContentSize() {
        return this.protectionSpecificHeader.getData().limit() + 24;
    }

    @Override
    public byte[] getUserType() {
        return USER_TYPE;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt64(byteBuffer, this.systemId.getMostSignificantBits());
        IsoTypeWriter.writeUInt64(byteBuffer, this.systemId.getLeastSignificantBits());
        ByteBuffer data = this.protectionSpecificHeader.getData();
        data.rewind();
        IsoTypeWriter.writeUInt32(byteBuffer, data.limit());
        byteBuffer.put(data);
    }

    @Override
    protected void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        byte[] systemIdBytes = new byte[16];
        content.get(systemIdBytes);
        this.systemId = UUIDConverter.convert(systemIdBytes);
        CastUtils.l2i(IsoTypeReader.readUInt32(content));
        this.protectionSpecificHeader = ProtectionSpecificHeader.createFor(this.systemId, content);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UuidBasedProtectionSystemSpecificHeaderBox");
        sb.append("{systemId=").append(this.systemId.toString());
        sb.append(", dataSize=").append(this.protectionSpecificHeader.getData().limit());
        sb.append('}');
        return sb.toString();
    }
}
