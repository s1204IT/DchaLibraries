package com.googlecode.mp4parser.boxes.threegpp26245;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class FontTableBox extends AbstractBox {
    List<FontRecord> entries;

    public FontTableBox() {
        super("ftab");
        this.entries = new LinkedList();
    }

    @Override
    protected long getContentSize() {
        int size = 2;
        for (FontRecord fontRecord : this.entries) {
            size += fontRecord.getSize();
        }
        return size;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        int numberOfRecords = IsoTypeReader.readUInt16(content);
        for (int i = 0; i < numberOfRecords; i++) {
            FontRecord fr = new FontRecord();
            fr.parse(content);
            this.entries.add(fr);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        IsoTypeWriter.writeUInt16(byteBuffer, this.entries.size());
        for (FontRecord record : this.entries) {
            record.getContent(byteBuffer);
        }
    }

    public static class FontRecord {
        int fontId;
        String fontname;

        public void parse(ByteBuffer bb) {
            this.fontId = IsoTypeReader.readUInt16(bb);
            int length = IsoTypeReader.readUInt8(bb);
            this.fontname = IsoTypeReader.readString(bb, length);
        }

        public void getContent(ByteBuffer bb) {
            IsoTypeWriter.writeUInt16(bb, this.fontId);
            IsoTypeWriter.writeUInt8(bb, this.fontname.length());
            bb.put(Utf8.convert(this.fontname));
        }

        public int getSize() {
            return Utf8.utf8StringLengthInBytes(this.fontname) + 3;
        }

        public String toString() {
            return "FontRecord{fontId=" + this.fontId + ", fontname='" + this.fontname + "'}";
        }
    }
}
