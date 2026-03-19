package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class EditListBox extends AbstractFullBox {
    private List<Entry> entries;

    public EditListBox() {
        super("elst");
        this.entries = new LinkedList();
    }

    @Override
    protected long getContentSize() {
        if (getVersion() == 1) {
            return 8 + ((long) (this.entries.size() * 20));
        }
        return 8 + ((long) (this.entries.size() * 12));
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        int iL2i = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.entries = new LinkedList();
        for (int i = 0; i < iL2i; i++) {
            this.entries.add(new Entry(this, byteBuffer));
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.entries.size());
        Iterator<Entry> it = this.entries.iterator();
        while (it.hasNext()) {
            it.next().getContent(byteBuffer);
        }
    }

    public String toString() {
        return "EditListBox{entries=" + this.entries + '}';
    }

    public static class Entry {
        EditListBox editListBox;
        private double mediaRate;
        private long mediaTime;
        private long segmentDuration;

        public Entry(EditListBox editListBox, ByteBuffer byteBuffer) {
            if (editListBox.getVersion() == 1) {
                this.segmentDuration = IsoTypeReader.readUInt64(byteBuffer);
                this.mediaTime = IsoTypeReader.readUInt64(byteBuffer);
                this.mediaRate = IsoTypeReader.readFixedPoint1616(byteBuffer);
            } else {
                this.segmentDuration = IsoTypeReader.readUInt32(byteBuffer);
                this.mediaTime = IsoTypeReader.readUInt32(byteBuffer);
                this.mediaRate = IsoTypeReader.readFixedPoint1616(byteBuffer);
            }
            this.editListBox = editListBox;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Entry entry = (Entry) obj;
            if (this.mediaTime == entry.mediaTime && this.segmentDuration == entry.segmentDuration) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((int) (this.segmentDuration ^ (this.segmentDuration >>> 32)))) + ((int) (this.mediaTime ^ (this.mediaTime >>> 32)));
        }

        public void getContent(ByteBuffer byteBuffer) {
            if (this.editListBox.getVersion() == 1) {
                IsoTypeWriter.writeUInt64(byteBuffer, this.segmentDuration);
                IsoTypeWriter.writeUInt64(byteBuffer, this.mediaTime);
            } else {
                IsoTypeWriter.writeUInt32(byteBuffer, CastUtils.l2i(this.segmentDuration));
                byteBuffer.putInt(CastUtils.l2i(this.mediaTime));
            }
            IsoTypeWriter.writeFixedPont1616(byteBuffer, this.mediaRate);
        }

        public String toString() {
            return "Entry{segmentDuration=" + this.segmentDuration + ", mediaTime=" + this.mediaTime + ", mediaRate=" + this.mediaRate + '}';
        }
    }
}
