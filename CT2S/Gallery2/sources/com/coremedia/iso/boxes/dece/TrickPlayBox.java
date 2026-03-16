package com.coremedia.iso.boxes.dece;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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

        public Entry(int value) {
            this.value = value;
        }

        public int getPicType() {
            return (this.value >> 6) & 3;
        }

        public int getDependencyLevel() {
            return this.value & 63;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Entry");
            sb.append("{picType=").append(getPicType());
            sb.append(",dependencyLevel=").append(getDependencyLevel());
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    protected long getContentSize() {
        return this.entries.size() + 4;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        while (content.remaining() > 0) {
            this.entries.add(new Entry(IsoTypeReader.readUInt8(content)));
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        for (Entry entry : this.entries) {
            IsoTypeWriter.writeUInt8(byteBuffer, entry.value);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TrickPlayBox");
        sb.append("{entries=").append(this.entries);
        sb.append('}');
        return sb.toString();
    }
}
