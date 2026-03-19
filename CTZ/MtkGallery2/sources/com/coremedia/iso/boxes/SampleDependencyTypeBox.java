package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SampleDependencyTypeBox extends AbstractFullBox {
    private List<Entry> entries;

    public static class Entry {
        private int value;

        public Entry(int i) {
            this.value = i;
        }

        public int getReserved() {
            return (this.value >> 6) & 3;
        }

        public int getSampleDependsOn() {
            return (this.value >> 4) & 3;
        }

        public int getSampleIsDependentOn() {
            return (this.value >> 2) & 3;
        }

        public int getSampleHasRedundancy() {
            return this.value & 3;
        }

        public String toString() {
            return "Entry{reserved=" + getReserved() + ", sampleDependsOn=" + getSampleDependsOn() + ", sampleIsDependentOn=" + getSampleIsDependentOn() + ", sampleHasRedundancy=" + getSampleHasRedundancy() + '}';
        }
    }

    public SampleDependencyTypeBox() {
        super("sdtp");
        this.entries = new ArrayList();
    }

    @Override
    protected long getContentSize() {
        return 4 + this.entries.size();
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        Iterator<Entry> it = this.entries.iterator();
        while (it.hasNext()) {
            IsoTypeWriter.writeUInt8(byteBuffer, it.next().value);
        }
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        while (byteBuffer.remaining() > 0) {
            this.entries.add(new Entry(IsoTypeReader.readUInt8(byteBuffer)));
        }
    }

    public List<Entry> getEntries() {
        return this.entries;
    }

    public void setEntries(List<Entry> list) {
        this.entries = list;
    }

    public String toString() {
        return "SampleDependencyTypeBox{entries=" + this.entries + '}';
    }
}
