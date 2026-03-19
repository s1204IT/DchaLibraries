package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class SampleGroupDescriptionBox extends AbstractFullBox {
    private int defaultLength;
    private int descriptionLength;
    private List<GroupEntry> groupEntries;
    private String groupingType;

    public SampleGroupDescriptionBox() {
        super("sgpd");
        this.groupEntries = new LinkedList();
    }

    @Override
    protected long getContentSize() {
        long j;
        if (getVersion() == 1) {
            j = 12;
        } else {
            j = 8;
        }
        long size = j + 4;
        for (GroupEntry groupEntry : this.groupEntries) {
            if (getVersion() == 1 && this.defaultLength == 0) {
                size += 4;
            }
            size += (long) groupEntry.size();
        }
        return size;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(this.groupingType.getBytes());
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.defaultLength);
        }
        IsoTypeWriter.writeUInt32(byteBuffer, this.groupEntries.size());
        for (GroupEntry groupEntry : this.groupEntries) {
            if (getVersion() == 1 && this.defaultLength == 0) {
                IsoTypeWriter.writeUInt32(byteBuffer, groupEntry.get().limit());
            }
            byteBuffer.put(groupEntry.get());
        }
    }

    @Override
    protected void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        if (getVersion() != 1) {
            throw new RuntimeException("SampleGroupDescriptionBox are only supported in version 1");
        }
        this.groupingType = IsoTypeReader.read4cc(byteBuffer);
        if (getVersion() == 1) {
            this.defaultLength = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        }
        long uInt32 = IsoTypeReader.readUInt32(byteBuffer);
        while (true) {
            long j = uInt32 - 1;
            if (uInt32 > 0) {
                int i = this.defaultLength;
                if (getVersion() == 1) {
                    if (this.defaultLength == 0) {
                        this.descriptionLength = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
                        i = this.descriptionLength;
                    }
                    int iPosition = byteBuffer.position() + i;
                    ByteBuffer byteBufferSlice = byteBuffer.slice();
                    byteBufferSlice.limit(i);
                    this.groupEntries.add(parseGroupEntry(byteBufferSlice, this.groupingType));
                    byteBuffer.position(iPosition);
                    uInt32 = j;
                } else {
                    throw new RuntimeException("This should be implemented");
                }
            } else {
                return;
            }
        }
    }

    private GroupEntry parseGroupEntry(ByteBuffer byteBuffer, String str) {
        GroupEntry unknownEntry;
        if ("roll".equals(str)) {
            unknownEntry = new RollRecoveryEntry();
        } else if ("rash".equals(str)) {
            unknownEntry = new RateShareEntry();
        } else if ("seig".equals(str)) {
            unknownEntry = new CencSampleEncryptionInformationGroupEntry();
        } else if ("rap ".equals(str)) {
            unknownEntry = new VisualRandomAccessEntry();
        } else if ("tele".equals(str)) {
            unknownEntry = new TemporalLevelEntry();
        } else {
            unknownEntry = new UnknownEntry();
        }
        unknownEntry.parse(byteBuffer);
        return unknownEntry;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SampleGroupDescriptionBox sampleGroupDescriptionBox = (SampleGroupDescriptionBox) obj;
        if (this.defaultLength != sampleGroupDescriptionBox.defaultLength) {
            return false;
        }
        if (this.groupEntries == null ? sampleGroupDescriptionBox.groupEntries != null : !this.groupEntries.equals(sampleGroupDescriptionBox.groupEntries)) {
            return false;
        }
        if (this.groupingType == null ? sampleGroupDescriptionBox.groupingType == null : this.groupingType.equals(sampleGroupDescriptionBox.groupingType)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((this.groupingType != null ? this.groupingType.hashCode() : 0) * 31) + this.defaultLength)) + (this.groupEntries != null ? this.groupEntries.hashCode() : 0);
    }

    public String toString() {
        return "SampleGroupDescriptionBox{groupingType='" + this.groupingType + "', defaultLength=" + this.defaultLength + ", groupEntries=" + this.groupEntries + '}';
    }
}
