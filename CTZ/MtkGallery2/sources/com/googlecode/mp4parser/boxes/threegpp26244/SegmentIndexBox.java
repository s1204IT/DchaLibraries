package com.googlecode.mp4parser.boxes.threegpp26244;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitWriterBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SegmentIndexBox extends AbstractFullBox {
    long earliestPresentationTime;
    List<Entry> entries;
    long firstOffset;
    long referenceId;
    int reserved;
    long timeScale;

    public SegmentIndexBox() {
        super("sidx");
        this.entries = new ArrayList();
    }

    @Override
    protected long getContentSize() {
        return 12 + (getVersion() == 0 ? 8L : 16L) + 2 + 2 + ((long) (this.entries.size() * 12));
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, this.referenceId);
        IsoTypeWriter.writeUInt32(byteBuffer, this.timeScale);
        if (getVersion() == 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.earliestPresentationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.firstOffset);
        } else {
            IsoTypeWriter.writeUInt64(byteBuffer, this.earliestPresentationTime);
            IsoTypeWriter.writeUInt64(byteBuffer, this.firstOffset);
        }
        IsoTypeWriter.writeUInt16(byteBuffer, this.reserved);
        IsoTypeWriter.writeUInt16(byteBuffer, this.entries.size());
        for (Entry entry : this.entries) {
            BitWriterBuffer bitWriterBuffer = new BitWriterBuffer(byteBuffer);
            bitWriterBuffer.writeBits(entry.getReferenceType(), 1);
            bitWriterBuffer.writeBits(entry.getReferencedSize(), 31);
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getSubsegmentDuration());
            BitWriterBuffer bitWriterBuffer2 = new BitWriterBuffer(byteBuffer);
            bitWriterBuffer2.writeBits(entry.getStartsWithSap(), 1);
            bitWriterBuffer2.writeBits(entry.getSapType(), 3);
            bitWriterBuffer2.writeBits(entry.getSapDeltaTime(), 28);
        }
    }

    @Override
    protected void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.referenceId = IsoTypeReader.readUInt32(byteBuffer);
        this.timeScale = IsoTypeReader.readUInt32(byteBuffer);
        if (getVersion() == 0) {
            this.earliestPresentationTime = IsoTypeReader.readUInt32(byteBuffer);
            this.firstOffset = IsoTypeReader.readUInt32(byteBuffer);
        } else {
            this.earliestPresentationTime = IsoTypeReader.readUInt64(byteBuffer);
            this.firstOffset = IsoTypeReader.readUInt64(byteBuffer);
        }
        this.reserved = IsoTypeReader.readUInt16(byteBuffer);
        int uInt16 = IsoTypeReader.readUInt16(byteBuffer);
        for (int i = 0; i < uInt16; i++) {
            BitReaderBuffer bitReaderBuffer = new BitReaderBuffer(byteBuffer);
            Entry entry = new Entry();
            entry.setReferenceType((byte) bitReaderBuffer.readBits(1));
            entry.setReferencedSize(bitReaderBuffer.readBits(31));
            entry.setSubsegmentDuration(IsoTypeReader.readUInt32(byteBuffer));
            BitReaderBuffer bitReaderBuffer2 = new BitReaderBuffer(byteBuffer);
            entry.setStartsWithSap((byte) bitReaderBuffer2.readBits(1));
            entry.setSapType((byte) bitReaderBuffer2.readBits(3));
            entry.setSapDeltaTime(bitReaderBuffer2.readBits(28));
            this.entries.add(entry);
        }
    }

    public static class Entry {
        byte referenceType;
        int referencedSize;
        int sapDeltaTime;
        byte sapType;
        byte startsWithSap;
        long subsegmentDuration;

        public byte getReferenceType() {
            return this.referenceType;
        }

        public void setReferenceType(byte b) {
            this.referenceType = b;
        }

        public int getReferencedSize() {
            return this.referencedSize;
        }

        public void setReferencedSize(int i) {
            this.referencedSize = i;
        }

        public long getSubsegmentDuration() {
            return this.subsegmentDuration;
        }

        public void setSubsegmentDuration(long j) {
            this.subsegmentDuration = j;
        }

        public byte getStartsWithSap() {
            return this.startsWithSap;
        }

        public void setStartsWithSap(byte b) {
            this.startsWithSap = b;
        }

        public byte getSapType() {
            return this.sapType;
        }

        public void setSapType(byte b) {
            this.sapType = b;
        }

        public int getSapDeltaTime() {
            return this.sapDeltaTime;
        }

        public void setSapDeltaTime(int i) {
            this.sapDeltaTime = i;
        }

        public String toString() {
            return "Entry{referenceType=" + ((int) this.referenceType) + ", referencedSize=" + this.referencedSize + ", subsegmentDuration=" + this.subsegmentDuration + ", startsWithSap=" + ((int) this.startsWithSap) + ", sapType=" + ((int) this.sapType) + ", sapDeltaTime=" + this.sapDeltaTime + '}';
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Entry entry = (Entry) obj;
            if (this.referenceType == entry.referenceType && this.referencedSize == entry.referencedSize && this.sapDeltaTime == entry.sapDeltaTime && this.sapType == entry.sapType && this.startsWithSap == entry.startsWithSap && this.subsegmentDuration == entry.subsegmentDuration) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((((((((this.referenceType * 31) + this.referencedSize) * 31) + ((int) (this.subsegmentDuration ^ (this.subsegmentDuration >>> 32)))) * 31) + this.startsWithSap) * 31) + this.sapType)) + this.sapDeltaTime;
        }
    }
}
