package com.googlecode.mp4parser.boxes.threegpp26245;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;
import java.util.Iterator;
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
        Iterator<FontRecord> it = this.entries.iterator();
        int size = 2;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        int uInt16 = IsoTypeReader.readUInt16(byteBuffer);
        for (int i = 0; i < uInt16; i++) {
            FontRecord fontRecord = new FontRecord();
            fontRecord.parse(byteBuffer);
            this.entries.add(fontRecord);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        IsoTypeWriter.writeUInt16(byteBuffer, this.entries.size());
        Iterator<FontRecord> it = this.entries.iterator();
        while (it.hasNext()) {
            it.next().getContent(byteBuffer);
        }
    }

    public static class FontRecord {
        int fontId;
        String fontname;

        public void parse(ByteBuffer byteBuffer) {
            this.fontId = IsoTypeReader.readUInt16(byteBuffer);
            this.fontname = IsoTypeReader.readString(byteBuffer, IsoTypeReader.readUInt8(byteBuffer));
        }

        public void getContent(ByteBuffer byteBuffer) {
            IsoTypeWriter.writeUInt16(byteBuffer, this.fontId);
            IsoTypeWriter.writeUInt8(byteBuffer, this.fontname.length());
            byteBuffer.put(Utf8.convert(this.fontname));
        }

        public int getSize() {
            return Utf8.utf8StringLengthInBytes(this.fontname) + 3;
        }

        public String toString() {
            return "FontRecord{fontId=" + this.fontId + ", fontname='" + this.fontname + "'}";
        }
    }
}
