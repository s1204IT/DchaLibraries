package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class SampleToGroupBox extends AbstractFullBox {
    List<Entry> entries;
    private String groupingType;
    private String groupingTypeParameter;

    public SampleToGroupBox() {
        super("sbgp");
        this.entries = new LinkedList();
    }

    @Override
    protected long getContentSize() {
        return getVersion() == 1 ? (this.entries.size() * 8) + 16 : (this.entries.size() * 8) + 12;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(this.groupingType.getBytes());
        if (getVersion() == 1) {
            byteBuffer.put(this.groupingTypeParameter.getBytes());
        }
        IsoTypeWriter.writeUInt32(byteBuffer, this.entries.size());
        for (Entry entry : this.entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getSampleCount());
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getGroupDescriptionIndex());
        }
    }

    @Override
    protected void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.groupingType = IsoTypeReader.read4cc(content);
        if (getVersion() == 1) {
            this.groupingTypeParameter = IsoTypeReader.read4cc(content);
        }
        long entryCount = IsoTypeReader.readUInt32(content);
        while (true) {
            long entryCount2 = entryCount;
            entryCount = entryCount2 - 1;
            if (entryCount2 > 0) {
                this.entries.add(new Entry(CastUtils.l2i(IsoTypeReader.readUInt32(content)), CastUtils.l2i(IsoTypeReader.readUInt32(content))));
            } else {
                return;
            }
        }
    }

    public static class Entry {
        private int groupDescriptionIndex;
        private long sampleCount;

        public Entry(long sampleCount, int groupDescriptionIndex) {
            this.sampleCount = sampleCount;
            this.groupDescriptionIndex = groupDescriptionIndex;
        }

        public long getSampleCount() {
            return this.sampleCount;
        }

        public int getGroupDescriptionIndex() {
            return this.groupDescriptionIndex;
        }

        public String toString() {
            return "Entry{sampleCount=" + this.sampleCount + ", groupDescriptionIndex=" + this.groupDescriptionIndex + '}';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry entry = (Entry) o;
            return this.groupDescriptionIndex == entry.groupDescriptionIndex && this.sampleCount == entry.sampleCount;
        }

        public int hashCode() {
            int result = (int) (this.sampleCount ^ (this.sampleCount >>> 32));
            return (result * 31) + this.groupDescriptionIndex;
        }
    }
}
