package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SubSampleInformationBox extends AbstractFullBox {
    private List<SampleEntry> entries;
    private long entryCount;

    public SubSampleInformationBox() {
        super("subs");
        this.entries = new ArrayList();
    }

    @Override
    protected long getContentSize() {
        long entries = 8 + (6 * this.entryCount);
        int subsampleEntries = 0;
        for (SampleEntry sampleEntry : this.entries) {
            subsampleEntries += ((getVersion() == 1 ? 4 : 2) + 1 + 1 + 4) * sampleEntry.getSubsampleCount();
        }
        return ((long) subsampleEntries) + entries;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.entryCount = IsoTypeReader.readUInt32(content);
        for (int i = 0; i < this.entryCount; i++) {
            SampleEntry sampleEntry = new SampleEntry();
            sampleEntry.setSampleDelta(IsoTypeReader.readUInt32(content));
            int subsampleCount = IsoTypeReader.readUInt16(content);
            for (int j = 0; j < subsampleCount; j++) {
                SampleEntry.SubsampleEntry subsampleEntry = new SampleEntry.SubsampleEntry();
                subsampleEntry.setSubsampleSize(getVersion() == 1 ? IsoTypeReader.readUInt32(content) : IsoTypeReader.readUInt16(content));
                subsampleEntry.setSubsamplePriority(IsoTypeReader.readUInt8(content));
                subsampleEntry.setDiscardable(IsoTypeReader.readUInt8(content));
                subsampleEntry.setReserved(IsoTypeReader.readUInt32(content));
                sampleEntry.addSubsampleEntry(subsampleEntry);
            }
            this.entries.add(sampleEntry);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.entries.size());
        for (SampleEntry sampleEntry : this.entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleEntry.getSampleDelta());
            IsoTypeWriter.writeUInt16(byteBuffer, sampleEntry.getSubsampleCount());
            List<SampleEntry.SubsampleEntry> subsampleEntries = sampleEntry.getSubsampleEntries();
            for (SampleEntry.SubsampleEntry subsampleEntry : subsampleEntries) {
                if (getVersion() == 1) {
                    IsoTypeWriter.writeUInt32(byteBuffer, subsampleEntry.getSubsampleSize());
                } else {
                    IsoTypeWriter.writeUInt16(byteBuffer, CastUtils.l2i(subsampleEntry.getSubsampleSize()));
                }
                IsoTypeWriter.writeUInt8(byteBuffer, subsampleEntry.getSubsamplePriority());
                IsoTypeWriter.writeUInt8(byteBuffer, subsampleEntry.getDiscardable());
                IsoTypeWriter.writeUInt32(byteBuffer, subsampleEntry.getReserved());
            }
        }
    }

    public String toString() {
        return "SubSampleInformationBox{entryCount=" + this.entryCount + ", entries=" + this.entries + '}';
    }

    public static class SampleEntry {
        private long sampleDelta;
        private int subsampleCount;
        private List<SubsampleEntry> subsampleEntries = new ArrayList();

        public long getSampleDelta() {
            return this.sampleDelta;
        }

        public void setSampleDelta(long sampleDelta) {
            this.sampleDelta = sampleDelta;
        }

        public int getSubsampleCount() {
            return this.subsampleCount;
        }

        public List<SubsampleEntry> getSubsampleEntries() {
            return this.subsampleEntries;
        }

        public void addSubsampleEntry(SubsampleEntry subsampleEntry) {
            this.subsampleEntries.add(subsampleEntry);
            this.subsampleCount++;
        }

        public static class SubsampleEntry {
            private int discardable;
            private long reserved;
            private int subsamplePriority;
            private long subsampleSize;

            public long getSubsampleSize() {
                return this.subsampleSize;
            }

            public void setSubsampleSize(long subsampleSize) {
                this.subsampleSize = subsampleSize;
            }

            public int getSubsamplePriority() {
                return this.subsamplePriority;
            }

            public void setSubsamplePriority(int subsamplePriority) {
                this.subsamplePriority = subsamplePriority;
            }

            public int getDiscardable() {
                return this.discardable;
            }

            public void setDiscardable(int discardable) {
                this.discardable = discardable;
            }

            public long getReserved() {
                return this.reserved;
            }

            public void setReserved(long reserved) {
                this.reserved = reserved;
            }

            public String toString() {
                return "SubsampleEntry{subsampleSize=" + this.subsampleSize + ", subsamplePriority=" + this.subsamplePriority + ", discardable=" + this.discardable + ", reserved=" + this.reserved + '}';
            }
        }

        public String toString() {
            return "SampleEntry{sampleDelta=" + this.sampleDelta + ", subsampleCount=" + this.subsampleCount + ", subsampleEntries=" + this.subsampleEntries + '}';
        }
    }
}
