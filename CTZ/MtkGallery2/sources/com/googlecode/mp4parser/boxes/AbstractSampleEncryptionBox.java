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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractSampleEncryptionBox extends AbstractFullBox {
    int algorithmId;
    List<Entry> entries;
    int ivSize;
    byte[] kid;

    protected AbstractSampleEncryptionBox(String str) {
        super(str);
        this.algorithmId = -1;
        this.ivSize = -1;
        this.kid = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        this.entries = new LinkedList();
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        int i;
        parseVersionAndFlags(byteBuffer);
        if ((getFlags() & 1) > 0) {
            this.algorithmId = IsoTypeReader.readUInt24(byteBuffer);
            this.ivSize = IsoTypeReader.readUInt8(byteBuffer);
            i = this.ivSize;
            this.kid = new byte[16];
            byteBuffer.get(this.kid);
        } else {
            int defaultIvSize = -1;
            for (Box box : Path.getPaths(this, "/moov[0]/trak/tkhd")) {
                if (((TrackHeaderBox) box).getTrackId() == ((TrackFragmentHeaderBox) getParent().getBoxes(TrackFragmentHeaderBox.class).get(0)).getTrackId()) {
                    AbstractTrackEncryptionBox abstractTrackEncryptionBox = (AbstractTrackEncryptionBox) Path.getPath(box, "../mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schi[0]/tenc[0]");
                    if (abstractTrackEncryptionBox == null) {
                        abstractTrackEncryptionBox = (AbstractTrackEncryptionBox) Path.getPath(box, "../mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schi[0]/uuid[0]");
                    }
                    defaultIvSize = abstractTrackEncryptionBox.getDefaultIvSize();
                }
            }
            i = defaultIvSize;
        }
        long uInt32 = IsoTypeReader.readUInt32(byteBuffer);
        while (true) {
            long j = uInt32 - 1;
            if (uInt32 > 0) {
                Entry entry = new Entry();
                entry.iv = new byte[i < 0 ? 8 : i];
                byteBuffer.get(entry.iv);
                if ((getFlags() & 2) > 0) {
                    int uInt16 = IsoTypeReader.readUInt16(byteBuffer);
                    entry.pairs = new LinkedList();
                    while (true) {
                        int i2 = uInt16 - 1;
                        if (uInt16 > 0) {
                            entry.pairs.add(entry.createPair(IsoTypeReader.readUInt16(byteBuffer), IsoTypeReader.readUInt32(byteBuffer)));
                            uInt16 = i2;
                        }
                    }
                }
                this.entries.add(entry);
                uInt32 = j;
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
                byte[] bArr = new byte[this.ivSize];
                System.arraycopy(entry.iv, 0, bArr, this.ivSize - entry.iv.length, entry.iv.length);
                byteBuffer.put(bArr);
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
        long length;
        if (isOverrideTrackEncryptionBoxParameters()) {
            length = 8 + ((long) this.kid.length);
        } else {
            length = 4;
        }
        long size = length + 4;
        Iterator<Entry> it = this.entries.iterator();
        while (it.hasNext()) {
            size += (long) it.next().getSize();
        }
        return size;
    }

    @Override
    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        super.getBox(writableByteChannel);
    }

    public class Entry {
        public byte[] iv;
        public List<Pair> pairs = new LinkedList();

        public Entry() {
        }

        public int getSize() {
            int length;
            if (AbstractSampleEncryptionBox.this.isOverrideTrackEncryptionBoxParameters()) {
                length = AbstractSampleEncryptionBox.this.ivSize;
            } else {
                length = this.iv.length;
            }
            if (AbstractSampleEncryptionBox.this.isSubSampleEncryption()) {
                length += 2;
                for (Pair pair : this.pairs) {
                    length += 6;
                }
            }
            return length;
        }

        public Pair createPair(int i, long j) {
            return new Pair(i, j);
        }

        public class Pair {
            public int clear;
            public long encrypted;

            public Pair(int i, long j) {
                this.clear = i;
                this.encrypted = j;
            }

            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null || getClass() != obj.getClass()) {
                    return false;
                }
                Pair pair = (Pair) obj;
                if (this.clear == pair.clear && this.encrypted == pair.encrypted) {
                    return true;
                }
                return false;
            }

            public int hashCode() {
                return (31 * this.clear) + ((int) (this.encrypted ^ (this.encrypted >>> 32)));
            }

            public String toString() {
                return "clr:" + this.clear + " enc:" + this.encrypted;
            }
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Entry entry = (Entry) obj;
            if (!new BigInteger(this.iv).equals(new BigInteger(entry.iv))) {
                return false;
            }
            if (this.pairs == null ? entry.pairs == null : this.pairs.equals(entry.pairs)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * (this.iv != null ? Arrays.hashCode(this.iv) : 0)) + (this.pairs != null ? this.pairs.hashCode() : 0);
        }

        public String toString() {
            return "Entry{iv=" + Hex.encodeHex(this.iv) + ", pairs=" + this.pairs + '}';
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractSampleEncryptionBox abstractSampleEncryptionBox = (AbstractSampleEncryptionBox) obj;
        if (this.algorithmId != abstractSampleEncryptionBox.algorithmId || this.ivSize != abstractSampleEncryptionBox.ivSize) {
            return false;
        }
        if (this.entries == null ? abstractSampleEncryptionBox.entries != null : !this.entries.equals(abstractSampleEncryptionBox.entries)) {
            return false;
        }
        if (Arrays.equals(this.kid, abstractSampleEncryptionBox.kid)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((((this.algorithmId * 31) + this.ivSize) * 31) + (this.kid != null ? Arrays.hashCode(this.kid) : 0))) + (this.entries != null ? this.entries.hashCode() : 0);
    }
}
