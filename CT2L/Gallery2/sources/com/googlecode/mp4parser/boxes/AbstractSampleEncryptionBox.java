package com.googlecode.mp4parser.boxes;

import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentHeaderBox;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.Path;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractSampleEncryptionBox extends AbstractFullBox {
    int algorithmId;
    List<Entry> entries;
    int ivSize;
    byte[] kid;

    protected AbstractSampleEncryptionBox(String type) {
        super(type);
        this.algorithmId = -1;
        this.ivSize = -1;
        this.kid = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        this.entries = new LinkedList();
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int useThisIvSize = -1;
        if ((getFlags() & 1) > 0) {
            this.algorithmId = IsoTypeReader.readUInt24(content);
            this.ivSize = IsoTypeReader.readUInt8(content);
            useThisIvSize = this.ivSize;
            this.kid = new byte[16];
            content.get(this.kid);
        } else {
            List<Box> tkhds = Path.getPaths(this, "/moov[0]/trak/tkhd");
            for (Box tkhd : tkhds) {
                if (((TrackHeaderBox) tkhd).getTrackId() == ((TrackFragmentHeaderBox) getParent().getBoxes(TrackFragmentHeaderBox.class).get(0)).getTrackId()) {
                    AbstractTrackEncryptionBox tenc = (AbstractTrackEncryptionBox) Path.getPath(tkhd, "../mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schi[0]/tenc[0]");
                    if (tenc == null) {
                        tenc = (AbstractTrackEncryptionBox) Path.getPath(tkhd, "../mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schi[0]/uuid[0]");
                    }
                    useThisIvSize = tenc.getDefaultIvSize();
                }
            }
        }
        long numOfEntries = IsoTypeReader.readUInt32(content);
        while (true) {
            long numOfEntries2 = numOfEntries;
            numOfEntries = numOfEntries2 - 1;
            if (numOfEntries2 > 0) {
                Entry e = new Entry();
                e.iv = new byte[useThisIvSize < 0 ? 8 : useThisIvSize];
                content.get(e.iv);
                if ((getFlags() & 2) > 0) {
                    int numOfPairs = IsoTypeReader.readUInt16(content);
                    e.pairs = new LinkedList();
                    while (true) {
                        int numOfPairs2 = numOfPairs;
                        numOfPairs = numOfPairs2 - 1;
                        if (numOfPairs2 > 0) {
                            e.pairs.add(e.createPair(IsoTypeReader.readUInt16(content), IsoTypeReader.readUInt32(content)));
                        }
                    }
                }
                this.entries.add(e);
            } else {
                return;
            }
        }
    }

    public boolean isSubSampleEncryption() {
        return (getFlags() & 2) > 0;
    }

    public boolean isOverrideTrackEncryptionBoxParameters() {
        return (getFlags() & 1) > 0;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (isOverrideTrackEncryptionBoxParameters()) {
            IsoTypeWriter.writeUInt24(byteBuffer, this.algorithmId);
            IsoTypeWriter.writeUInt8(byteBuffer, this.ivSize);
            byteBuffer.put(this.kid);
        }
        IsoTypeWriter.writeUInt32(byteBuffer, this.entries.size());
        for (Entry entry : this.entries) {
            if (isOverrideTrackEncryptionBoxParameters()) {
                byte[] ivFull = new byte[this.ivSize];
                System.arraycopy(entry.iv, 0, ivFull, this.ivSize - entry.iv.length, entry.iv.length);
                byteBuffer.put(ivFull);
            } else {
                byteBuffer.put(entry.iv);
            }
            if (isSubSampleEncryption()) {
                IsoTypeWriter.writeUInt16(byteBuffer, entry.pairs.size());
                for (Entry.Pair pair : entry.pairs) {
                    IsoTypeWriter.writeUInt16(byteBuffer, pair.clear);
                    IsoTypeWriter.writeUInt32(byteBuffer, pair.encrypted);
                }
            }
        }
    }

    @Override
    protected long getContentSize() {
        long contentSize = 4;
        if (isOverrideTrackEncryptionBoxParameters()) {
            long contentSize2 = 4 + 4;
            contentSize = contentSize2 + ((long) this.kid.length);
        }
        long contentSize3 = contentSize + 4;
        for (Entry entry : this.entries) {
            contentSize3 += (long) entry.getSize();
        }
        return contentSize3;
    }

    @Override
    public void getBox(WritableByteChannel os) throws IOException {
        super.getBox(os);
    }

    public class Entry {
        public byte[] iv;
        public List<Pair> pairs = new LinkedList();

        public Entry() {
        }

        public int getSize() {
            int size;
            if (AbstractSampleEncryptionBox.this.isOverrideTrackEncryptionBoxParameters()) {
                size = AbstractSampleEncryptionBox.this.ivSize;
            } else {
                size = this.iv.length;
            }
            if (AbstractSampleEncryptionBox.this.isSubSampleEncryption()) {
                size += 2;
                for (Pair pair : this.pairs) {
                    size += 6;
                }
            }
            return size;
        }

        public Pair createPair(int clear, long encrypted) {
            return new Pair(clear, encrypted);
        }

        public class Pair {
            public int clear;
            public long encrypted;

            public Pair(int clear, long encrypted) {
                this.clear = clear;
                this.encrypted = encrypted;
            }

            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Pair pair = (Pair) o;
                return this.clear == pair.clear && this.encrypted == pair.encrypted;
            }

            public int hashCode() {
                int result = this.clear;
                return (result * 31) + ((int) (this.encrypted ^ (this.encrypted >>> 32)));
            }

            public String toString() {
                return "clr:" + this.clear + " enc:" + this.encrypted;
            }
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry entry = (Entry) o;
            if (!new BigInteger(this.iv).equals(new BigInteger(entry.iv))) {
                return false;
            }
            if (this.pairs != null) {
                if (this.pairs.equals(entry.pairs)) {
                    return true;
                }
            } else if (entry.pairs == null) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = this.iv != null ? Arrays.hashCode(this.iv) : 0;
            return (result * 31) + (this.pairs != null ? this.pairs.hashCode() : 0);
        }

        public String toString() {
            return "Entry{iv=" + Hex.encodeHex(this.iv) + ", pairs=" + this.pairs + '}';
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractSampleEncryptionBox that = (AbstractSampleEncryptionBox) o;
        if (this.algorithmId == that.algorithmId && this.ivSize == that.ivSize) {
            if (this.entries == null ? that.entries != null : !this.entries.equals(that.entries)) {
                return false;
            }
            return Arrays.equals(this.kid, that.kid);
        }
        return false;
    }

    public int hashCode() {
        int result = this.algorithmId;
        return (((((result * 31) + this.ivSize) * 31) + (this.kid != null ? Arrays.hashCode(this.kid) : 0)) * 31) + (this.entries != null ? this.entries.hashCode() : 0);
    }
}
