package com.coremedia.iso.boxes.fragment;

import android.support.v4.app.NotificationCompat;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
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
        long size = 8;
        int flags = getFlags();
        if ((flags & 1) == 1) {
            size = 8 + 4;
        }
        if ((flags & 4) == 4) {
            size += 4;
        }
        long entrySize = 0;
        if ((flags & NotificationCompat.FLAG_LOCAL_ONLY) == 256) {
            entrySize = 0 + 4;
        }
        if ((flags & NotificationCompat.FLAG_GROUP_SUMMARY) == 512) {
            entrySize += 4;
        }
        if ((flags & 1024) == 1024) {
            entrySize += 4;
        }
        if ((flags & 2048) == 2048) {
            entrySize += 4;
        }
        return size + (((long) this.entries.size()) * entrySize);
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
            if ((flags & NotificationCompat.FLAG_LOCAL_ONLY) == 256) {
                IsoTypeWriter.writeUInt32(byteBuffer, entry.sampleDuration);
            }
            if ((flags & NotificationCompat.FLAG_GROUP_SUMMARY) == 512) {
                IsoTypeWriter.writeUInt32(byteBuffer, entry.sampleSize);
            }
            if ((flags & 1024) == 1024) {
                entry.sampleFlags.getContent(byteBuffer);
            }
            if ((flags & 2048) == 2048) {
                byteBuffer.putInt(entry.sampleCompositionTimeOffset);
            }
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        long sampleCount = IsoTypeReader.readUInt32(content);
        if ((getFlags() & 1) == 1) {
            this.dataOffset = CastUtils.l2i(IsoTypeReader.readUInt32(content));
        } else {
            this.dataOffset = -1;
        }
        if ((getFlags() & 4) == 4) {
            this.firstSampleFlags = new SampleFlags(content);
        }
        for (int i = 0; i < sampleCount; i++) {
            Entry entry = new Entry();
            if ((getFlags() & NotificationCompat.FLAG_LOCAL_ONLY) == 256) {
                entry.sampleDuration = IsoTypeReader.readUInt32(content);
            }
            if ((getFlags() & NotificationCompat.FLAG_GROUP_SUMMARY) == 512) {
                entry.sampleSize = IsoTypeReader.readUInt32(content);
            }
            if ((getFlags() & 1024) == 1024) {
                entry.sampleFlags = new SampleFlags(content);
            }
            if ((getFlags() & 2048) == 2048) {
                entry.sampleCompositionTimeOffset = content.getInt();
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
        return (getFlags() & NotificationCompat.FLAG_GROUP_SUMMARY) == 512;
    }

    public boolean isSampleDurationPresent() {
        return (getFlags() & NotificationCompat.FLAG_LOCAL_ONLY) == 256;
    }

    public boolean isSampleFlagsPresent() {
        return (getFlags() & 1024) == 1024;
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
        StringBuilder sb = new StringBuilder();
        sb.append("TrackRunBox");
        sb.append("{sampleCount=").append(this.entries.size());
        sb.append(", dataOffset=").append(this.dataOffset);
        sb.append(", dataOffsetPresent=").append(isDataOffsetPresent());
        sb.append(", sampleSizePresent=").append(isSampleSizePresent());
        sb.append(", sampleDurationPresent=").append(isSampleDurationPresent());
        sb.append(", sampleFlagsPresentPresent=").append(isSampleFlagsPresent());
        sb.append(", sampleCompositionTimeOffsetPresent=").append(isSampleCompositionTimeOffsetPresent());
        sb.append(", firstSampleFlags=").append(this.firstSampleFlags);
        sb.append('}');
        return sb.toString();
    }
}
