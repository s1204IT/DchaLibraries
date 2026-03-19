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

public class TimeToSampleBox extends AbstractFullBox {
    static final boolean $assertionsDisabled = false;
    List<Entry> entries;

    public TimeToSampleBox() {
        super("stts");
        this.entries = Collections.emptyList();
    }

    @Override
    protected long getContentSize() {
        return 8 + (this.entries.size() * 8);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        int iL2i = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.entries = new ArrayList(iL2i);
        for (int i = 0; i < iL2i; i++) {
            this.entries.add(new Entry(IsoTypeReader.readUInt32(byteBuffer), IsoTypeReader.readUInt32(byteBuffer)));
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.entries.size());
        for (Entry entry : this.entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getCount());
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getDelta());
        }
    }

    public List<Entry> getEntries() {
        return this.entries;
    }

    public void setEntries(List<Entry> list) {
        this.entries = list;
    }

    public String toString() {
        return "TimeToSampleBox[entryCount=" + this.entries.size() + "]";
    }

    public static class Entry {
        long count;
        long delta;

        public Entry(long j, long j2) {
            this.count = j;
            this.delta = j2;
        }

        public long getCount() {
            return this.count;
        }

        public long getDelta() {
            return this.delta;
        }

        public void setCount(long j) {
            this.count = j;
        }

        public String toString() {
            return "Entry{count=" + this.count + ", delta=" + this.delta + '}';
        }
    }

    public static long[] blowupTimeToSamples(List<Entry> list) {
        Iterator<Entry> it = list.iterator();
        long count = 0;
        while (it.hasNext()) {
            count += it.next().getCount();
        }
        long[] jArr = new long[(int) count];
        int i = 0;
        for (Entry entry : list) {
            int i2 = i;
            int i3 = 0;
            while (i3 < entry.getCount()) {
                jArr[i2] = entry.getDelta();
                i3++;
                i2++;
            }
            i = i2;
        }
        return jArr;
    }
}
