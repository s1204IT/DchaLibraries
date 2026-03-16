package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CompositionTimeToSample extends AbstractFullBox {
    static final boolean $assertionsDisabled;
    List<Entry> entries;

    static {
        $assertionsDisabled = !CompositionTimeToSample.class.desiredAssertionStatus();
    }

    public CompositionTimeToSample() {
        super("ctts");
        this.entries = Collections.emptyList();
    }

    @Override
    protected long getContentSize() {
        return (this.entries.size() * 8) + 8;
    }

    public List<Entry> getEntries() {
        return this.entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int numberOfEntries = CastUtils.l2i(IsoTypeReader.readUInt32(content));
        this.entries = new ArrayList(numberOfEntries);
        for (int i = 0; i < numberOfEntries; i++) {
            Entry e = new Entry(CastUtils.l2i(IsoTypeReader.readUInt32(content)), content.getInt());
            this.entries.add(e);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.entries.size());
        for (Entry entry : this.entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getCount());
            byteBuffer.putInt(entry.getOffset());
        }
    }

    public static class Entry {
        int count;
        int offset;

        public Entry(int count, int offset) {
            this.count = count;
            this.offset = offset;
        }

        public int getCount() {
            return this.count;
        }

        public int getOffset() {
            return this.offset;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String toString() {
            return "Entry{count=" + this.count + ", offset=" + this.offset + '}';
        }
    }

    public static int[] blowupCompositionTimes(List<Entry> entries) {
        long numOfSamples = 0;
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            numOfSamples += (long) it.next().getCount();
        }
        if (!$assertionsDisabled && numOfSamples > 2147483647L) {
            throw new AssertionError();
        }
        int[] decodingTime = new int[(int) numOfSamples];
        int current = 0;
        for (Entry entry : entries) {
            int i = 0;
            while (i < entry.getCount()) {
                decodingTime[current] = entry.getOffset();
                i++;
                current++;
            }
        }
        return decodingTime;
    }
}
