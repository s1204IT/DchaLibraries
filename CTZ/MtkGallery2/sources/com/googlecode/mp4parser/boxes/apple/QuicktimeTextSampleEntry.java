package com.googlecode.mp4parser.boxes.apple;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import java.nio.ByteBuffer;

public class QuicktimeTextSampleEntry extends SampleEntry {
    int backgroundB;
    int backgroundG;
    int backgroundR;
    long defaultTextBox;
    int displayFlags;
    short fontFace;
    String fontName;
    short fontNumber;
    int foregroundB;
    int foregroundG;
    int foregroundR;
    long reserved1;
    byte reserved2;
    short reserved3;
    int textJustification;

    public QuicktimeTextSampleEntry() {
        super("text");
        this.foregroundR = 65535;
        this.foregroundG = 65535;
        this.foregroundB = 65535;
        this.fontName = "";
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        this.displayFlags = byteBuffer.getInt();
        this.textJustification = byteBuffer.getInt();
        this.backgroundR = IsoTypeReader.readUInt16(byteBuffer);
        this.backgroundG = IsoTypeReader.readUInt16(byteBuffer);
        this.backgroundB = IsoTypeReader.readUInt16(byteBuffer);
        this.defaultTextBox = IsoTypeReader.readUInt64(byteBuffer);
        this.reserved1 = IsoTypeReader.readUInt64(byteBuffer);
        this.fontNumber = byteBuffer.getShort();
        this.fontFace = byteBuffer.getShort();
        this.reserved2 = byteBuffer.get();
        this.reserved3 = byteBuffer.getShort();
        this.foregroundR = IsoTypeReader.readUInt16(byteBuffer);
        this.foregroundG = IsoTypeReader.readUInt16(byteBuffer);
        this.foregroundB = IsoTypeReader.readUInt16(byteBuffer);
        if (byteBuffer.remaining() > 0) {
            byte[] bArr = new byte[IsoTypeReader.readUInt8(byteBuffer)];
            byteBuffer.get(bArr);
            this.fontName = new String(bArr);
            return;
        }
        this.fontName = null;
    }

    @Override
    protected long getContentSize() {
        return 52 + (this.fontName != null ? this.fontName.length() : 0);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        byteBuffer.putInt(this.displayFlags);
        byteBuffer.putInt(this.textJustification);
        IsoTypeWriter.writeUInt16(byteBuffer, this.backgroundR);
        IsoTypeWriter.writeUInt16(byteBuffer, this.backgroundG);
        IsoTypeWriter.writeUInt16(byteBuffer, this.backgroundB);
        IsoTypeWriter.writeUInt64(byteBuffer, this.defaultTextBox);
        IsoTypeWriter.writeUInt64(byteBuffer, this.reserved1);
        byteBuffer.putShort(this.fontNumber);
        byteBuffer.putShort(this.fontFace);
        byteBuffer.put(this.reserved2);
        byteBuffer.putShort(this.reserved3);
        IsoTypeWriter.writeUInt16(byteBuffer, this.foregroundR);
        IsoTypeWriter.writeUInt16(byteBuffer, this.foregroundG);
        IsoTypeWriter.writeUInt16(byteBuffer, this.foregroundB);
        if (this.fontName != null) {
            IsoTypeWriter.writeUInt8(byteBuffer, this.fontName.length());
            byteBuffer.put(this.fontName.getBytes());
        }
    }
}
