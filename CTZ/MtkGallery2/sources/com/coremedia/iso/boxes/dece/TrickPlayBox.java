package com.coremedia.iso.boxes.dece;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrickPlayBox extends AbstractFullBox {
    private List<Entry> entries;

    public TrickPlayBox() {
        super("trik");
        this.entries = new ArrayList();
    }

    public static class Entry {
        private int value;

        public Entry() {
        }

        public Entry(int i) {
            this.value = i;
        }

        public int getPicType() {
            return (this.value >> 6) & 3;
        }

        public int getDependencyLevel() {
            return this.value & 63;
        }

        public String toString() {
            return "Entry{picType=" + getPicType() + ",dependencyLevel=" + getDependencyLevel() + '}';
        }
    }

    @Override
    protected long getContentSize() {
        return 4 + this.entries.size();
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        while (byteBuffer.remaining() > 0) {
            this.entries.add(new Entry(IsoTypeReader.readUInt8(byteBuffer)));
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        Iterator<Entry> it = this.entries.iterator();
        while (it.hasNext()) {
            IsoTypeWriter.writeUInt8(byteBuffer, it.next().value);
        }
    }

    public String toString() {
        return "TrickPlayBox{entries=" + this.entries + '}';
    }
}
