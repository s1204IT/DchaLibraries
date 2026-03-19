package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class TextSampleEntry extends SampleEntry {
    private int[] backgroundColorRgba;
    private BoxRecord boxRecord;
    private long displayFlags;
    private int horizontalJustification;
    private StyleRecord styleRecord;
    private int verticalJustification;

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        this.displayFlags = IsoTypeReader.readUInt32(byteBuffer);
        this.horizontalJustification = IsoTypeReader.readUInt8(byteBuffer);
        this.verticalJustification = IsoTypeReader.readUInt8(byteBuffer);
        this.backgroundColorRgba = new int[4];
        this.backgroundColorRgba[0] = IsoTypeReader.readUInt8(byteBuffer);
        this.backgroundColorRgba[1] = IsoTypeReader.readUInt8(byteBuffer);
        this.backgroundColorRgba[2] = IsoTypeReader.readUInt8(byteBuffer);
        this.backgroundColorRgba[3] = IsoTypeReader.readUInt8(byteBuffer);
        this.boxRecord = new BoxRecord();
        this.boxRecord.parse(byteBuffer);
        this.styleRecord = new StyleRecord();
        this.styleRecord.parse(byteBuffer);
        _parseChildBoxes(byteBuffer);
    }

    @Override
    protected long getContentSize() {
        long size = 18 + ((long) this.boxRecord.getSize()) + ((long) this.styleRecord.getSize());
        Iterator<Box> it = this.boxes.iterator();
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
    }

    public String toString() {
        return "TextSampleEntry";
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.displayFlags);
        IsoTypeWriter.writeUInt8(byteBuffer, this.horizontalJustification);
        IsoTypeWriter.writeUInt8(byteBuffer, this.verticalJustification);
        IsoTypeWriter.writeUInt8(byteBuffer, this.backgroundColorRgba[0]);
        IsoTypeWriter.writeUInt8(byteBuffer, this.backgroundColorRgba[1]);
        IsoTypeWriter.writeUInt8(byteBuffer, this.backgroundColorRgba[2]);
        IsoTypeWriter.writeUInt8(byteBuffer, this.backgroundColorRgba[3]);
        this.boxRecord.getContent(byteBuffer);
        this.styleRecord.getContent(byteBuffer);
        _writeChildBoxes(byteBuffer);
    }

    public static class BoxRecord {
        int bottom;
        int left;
        int right;
        int top;

        public void parse(ByteBuffer byteBuffer) {
            this.top = IsoTypeReader.readUInt16(byteBuffer);
            this.left = IsoTypeReader.readUInt16(byteBuffer);
            this.bottom = IsoTypeReader.readUInt16(byteBuffer);
            this.right = IsoTypeReader.readUInt16(byteBuffer);
        }

        public void getContent(ByteBuffer byteBuffer) {
            IsoTypeWriter.writeUInt16(byteBuffer, this.top);
            IsoTypeWriter.writeUInt16(byteBuffer, this.left);
            IsoTypeWriter.writeUInt16(byteBuffer, this.bottom);
            IsoTypeWriter.writeUInt16(byteBuffer, this.right);
        }

        public int getSize() {
            return 8;
        }
    }

    public static class StyleRecord {
        int endChar;
        int faceStyleFlags;
        int fontId;
        int fontSize;
        int startChar;
        int[] textColor = {255, 255, 255, 255};

        public void parse(ByteBuffer byteBuffer) {
            this.startChar = IsoTypeReader.readUInt16(byteBuffer);
            this.endChar = IsoTypeReader.readUInt16(byteBuffer);
            this.fontId = IsoTypeReader.readUInt16(byteBuffer);
            this.faceStyleFlags = IsoTypeReader.readUInt8(byteBuffer);
            this.fontSize = IsoTypeReader.readUInt8(byteBuffer);
            this.textColor = new int[4];
            this.textColor[0] = IsoTypeReader.readUInt8(byteBuffer);
            this.textColor[1] = IsoTypeReader.readUInt8(byteBuffer);
            this.textColor[2] = IsoTypeReader.readUInt8(byteBuffer);
            this.textColor[3] = IsoTypeReader.readUInt8(byteBuffer);
        }

        public void getContent(ByteBuffer byteBuffer) {
            IsoTypeWriter.writeUInt16(byteBuffer, this.startChar);
            IsoTypeWriter.writeUInt16(byteBuffer, this.endChar);
            IsoTypeWriter.writeUInt16(byteBuffer, this.fontId);
            IsoTypeWriter.writeUInt8(byteBuffer, this.faceStyleFlags);
            IsoTypeWriter.writeUInt8(byteBuffer, this.fontSize);
            IsoTypeWriter.writeUInt8(byteBuffer, this.textColor[0]);
            IsoTypeWriter.writeUInt8(byteBuffer, this.textColor[1]);
            IsoTypeWriter.writeUInt8(byteBuffer, this.textColor[2]);
            IsoTypeWriter.writeUInt8(byteBuffer, this.textColor[3]);
        }

        public int getSize() {
            return 12;
        }
    }
}
