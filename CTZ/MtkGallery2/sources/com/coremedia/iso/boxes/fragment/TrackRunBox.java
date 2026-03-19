package com.coremedia.iso.boxes.fragment;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import com.mediatek.plugin.preload.SoOperater;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TrackRunBox extends AbstractFullBox {
    private int dataOffset;
    private List<Entry> entries;
    private SampleFlags firstSampleFlags;

    public List<Entry> getEntries() {
        return this.entries;
    }

    public static class Entry {
        private int sampleCompositionTimeOffset;
        private long sampleDuration;
        private SampleFlags sampleFlags;
        private long sampleSize;

        public long getSampleDuration() {
            return this.sampleDuration;
        }

        public long getSampleSize() {
            return this.sampleSize;
        }

        public SampleFlags getSampleFlags() {
            return this.sampleFlags;
        }

        public int getSampleCompositionTimeOffset() {
            return this.sampleCompositionTimeOffset;
        }

        public String toString() {
            return "Entry{sampleDuration=" + this.sampleDuration + ", sampleSize=" + this.sampleSize + ", sampleFlags=" + this.sampleFlags + ", sampleCompositionTimeOffset=" + this.sampleCompositionTimeOffset + '}';
        }
    }

    public TrackRunBox() {
        super("trun");
        this.entries = new ArrayList();
    }

    @Override
    protected long getContentSize() {
        long j;
        int flags = getFlags();
        if ((flags & 1) == 1) {
            j = 12;
        } else {
            j = 8;
        }
        if ((flags & 4) == 4) {
            j += 4;
        }
        long j2 = 0;
        if ((flags & 256) == 256) {
            j2 = 4;
        }
        if ((flags & 512) == 512) {
            j2 += 4;
        }
        if ((flags & SoOperater.STEP) == 1024) {
            j2 += 4;
        }
        if ((flags & 2048) == 2048) {
            j2 += 4;
        }
        return j + (j2 * ((long) this.entries.size()));
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.entries.size());
        int flags = getFlags();
        if ((flags & 1) == 1) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.dataOffset);
        }
        if ((flags & 4) == 4) {
            this.firstSampleFlags.getContent(byteBuffer);
        }
        for (Entry entry : this.entries) {
            if ((flags & 256) == 256) {
                IsoTypeWriter.writeUInt32(byteBuffer, entry.sampleDuration);
            }
            if ((flags & 512) == 512) {
                IsoTypeWriter.writeUInt32(byteBuffer, entry.sampleSize);
            }
            if ((flags & SoOperater.STEP) == 1024) {
                entry.sampleFlags.getContent(byteBuffer);
            }
            if ((flags & 2048) == 2048) {
                byteBuffer.putInt(entry.sampleCompositionTimeOffset);
            }
        }
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        long uInt32 = IsoTypeReader.readUInt32(byteBuffer);
        if ((getFlags() & 1) == 1) {
            this.dataOffset = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        } else {
            this.dataOffset = -1;
        }
        if ((getFlags() & 4) == 4) {
            this.firstSampleFlags = new SampleFlags(byteBuffer);
        }
        for (int i = 0; i < uInt32; i++) {
            Entry entry = new Entry();
            if ((getFlags() & 256) == 256) {
                entry.sampleDuration = IsoTypeReader.readUInt32(byteBuffer);
            }
            if ((getFlags() & 512) == 512) {
                entry.sampleSize = IsoTypeReader.readUInt32(byteBuffer);
            }
            if ((getFlags() & SoOperater.STEP) == 1024) {
                entry.sampleFlags = new SampleFlags(byteBuffer);
            }
            if ((getFlags() & 2048) == 2048) {
                entry.sampleCompositionTimeOffset = byteBuffer.getInt();
            }
            this.entries.add(entry);
        }
    }

    public boolean isDataOffsetPresent() {
        return (getFlags() & 1) == 1;
    }

    public boolean isFirstSampleFlagsPresent() {
        return (getFlags() & 4) == 4;
    }

    public boolean isSampleSizePresent() {
        return (getFlags() & 512) == 512;
    }

    public boolean isSampleDurationPresent() {
        return (getFlags() & 256) == 256;
    }

    public boolean isSampleFlagsPresent() {
        return (getFlags() & SoOperater.STEP) == 1024;
    }

    public boolean isSampleCompositionTimeOffsetPresent() {
        return (getFlags() & 2048) == 2048;
    }

    public int getDataOffset() {
        return this.dataOffset;
    }

    public SampleFlags getFirstSampleFlags() {
        return this.firstSampleFlags;
    }

    public String toString() {
        return "TrackRunBox{sampleCount=" + this.entries.size() + ", dataOffset=" + this.dataOffset + ", dataOffsetPresent=" + isDataOffsetPresent() + ", sampleSizePresent=" + isSampleSizePresent() + ", sampleDurationPresent=" + isSampleDurationPresent() + ", sampleFlagsPresentPresent=" + isSampleFlagsPresent() + ", sampleCompositionTimeOffsetPresent=" + isSampleCompositionTimeOffsetPresent() + ", firstSampleFlags=" + this.firstSampleFlags + '}';
    }
}
