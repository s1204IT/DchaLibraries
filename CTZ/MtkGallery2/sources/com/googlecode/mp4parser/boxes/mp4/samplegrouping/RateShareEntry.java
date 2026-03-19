package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class RateShareEntry extends GroupEntry {
    private short discardPriority;
    private List<Entry> entries = new LinkedList();
    private int maximumBitrate;
    private int minimumBitrate;
    private short operationPointCut;
    private short targetRateShare;

    @Override
    public void parse(ByteBuffer byteBuffer) {
        this.operationPointCut = byteBuffer.getShort();
        if (this.operationPointCut == 1) {
            this.targetRateShare = byteBuffer.getShort();
        } else {
            int i = this.operationPointCut;
            while (true) {
                int i2 = i - 1;
                if (i <= 0) {
                    break;
                }
                this.entries.add(new Entry(CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer)), byteBuffer.getShort()));
                i = i2;
            }
        }
        this.maximumBitrate = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.minimumBitrate = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.discardPriority = (short) IsoTypeReader.readUInt8(byteBuffer);
    }

    @Override
    public ByteBuffer get() {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(this.operationPointCut == 1 ? 13 : (this.operationPointCut * 6) + 11);
        byteBufferAllocate.putShort(this.operationPointCut);
        if (this.operationPointCut == 1) {
            byteBufferAllocate.putShort(this.targetRateShare);
        } else {
            for (Entry entry : this.entries) {
                byteBufferAllocate.putInt(entry.getAvailableBitrate());
                byteBufferAllocate.putShort(entry.getTargetRateShare());
            }
        }
        byteBufferAllocate.putInt(this.maximumBitrate);
        byteBufferAllocate.putInt(this.minimumBitrate);
        IsoTypeWriter.writeUInt8(byteBufferAllocate, this.discardPriority);
        byteBufferAllocate.rewind();
        return byteBufferAllocate;
    }

    public static class Entry {
        int availableBitrate;
        short targetRateShare;

        public Entry(int i, short s) {
            this.availableBitrate = i;
            this.targetRateShare = s;
        }

        public String toString() {
            return "{availableBitrate=" + this.availableBitrate + ", targetRateShare=" + ((int) this.targetRateShare) + '}';
        }

        public int getAvailableBitrate() {
            return this.availableBitrate;
        }

        public short getTargetRateShare() {
            return this.targetRateShare;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Entry entry = (Entry) obj;
            if (this.availableBitrate == entry.availableBitrate && this.targetRateShare == entry.targetRateShare) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.availableBitrate) + this.targetRateShare;
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RateShareEntry rateShareEntry = (RateShareEntry) obj;
        if (this.discardPriority != rateShareEntry.discardPriority || this.maximumBitrate != rateShareEntry.maximumBitrate || this.minimumBitrate != rateShareEntry.minimumBitrate || this.operationPointCut != rateShareEntry.operationPointCut || this.targetRateShare != rateShareEntry.targetRateShare) {
            return false;
        }
        if (this.entries == null ? rateShareEntry.entries == null : this.entries.equals(rateShareEntry.entries)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((((((((this.operationPointCut * 31) + this.targetRateShare) * 31) + (this.entries != null ? this.entries.hashCode() : 0)) * 31) + this.maximumBitrate) * 31) + this.minimumBitrate)) + this.discardPriority;
    }
}
