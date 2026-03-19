package com.googlecode.mp4parser.boxes.piff;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayReadyHeader extends ProtectionSpecificHeader {
    private long length;
    private List<PlayReadyRecord> records;

    @Override
    public void parse(ByteBuffer byteBuffer) {
        this.length = IsoTypeReader.readUInt32BE(byteBuffer);
        this.records = PlayReadyRecord.createFor(byteBuffer, IsoTypeReader.readUInt16BE(byteBuffer));
    }

    @Override
    public ByteBuffer getData() {
        Iterator<PlayReadyRecord> it = this.records.iterator();
        int iLimit = 6;
        while (it.hasNext()) {
            iLimit = iLimit + 4 + it.next().getValue().rewind().limit();
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(iLimit);
        IsoTypeWriter.writeUInt32BE(byteBufferAllocate, iLimit);
        IsoTypeWriter.writeUInt16BE(byteBufferAllocate, this.records.size());
        for (PlayReadyRecord playReadyRecord : this.records) {
            IsoTypeWriter.writeUInt16BE(byteBufferAllocate, playReadyRecord.type);
            IsoTypeWriter.writeUInt16BE(byteBufferAllocate, playReadyRecord.getValue().limit());
            byteBufferAllocate.put(playReadyRecord.getValue());
        }
        return byteBufferAllocate;
    }

    @Override
    public String toString() {
        return "PlayReadyHeader{length=" + this.length + ", recordCount=" + this.records.size() + ", records=" + this.records + '}';
    }

    public static abstract class PlayReadyRecord {
        int type;

        public abstract ByteBuffer getValue();

        public abstract void parse(ByteBuffer byteBuffer);

        public PlayReadyRecord(int i) {
            this.type = i;
        }

        public static List<PlayReadyRecord> createFor(ByteBuffer byteBuffer, int i) {
            PlayReadyRecord rMHeader;
            ArrayList arrayList = new ArrayList(i);
            for (int i2 = 0; i2 < i; i2++) {
                int uInt16BE = IsoTypeReader.readUInt16BE(byteBuffer);
                int uInt16BE2 = IsoTypeReader.readUInt16BE(byteBuffer);
                switch (uInt16BE) {
                    case 1:
                        rMHeader = new RMHeader();
                        break;
                    case 2:
                        rMHeader = new DefaulPlayReadyRecord(2);
                        break;
                    case 3:
                        rMHeader = new EmeddedLicenseStore();
                        break;
                    default:
                        rMHeader = new DefaulPlayReadyRecord(uInt16BE);
                        break;
                }
                rMHeader.parse((ByteBuffer) byteBuffer.slice().limit(uInt16BE2));
                byteBuffer.position(byteBuffer.position() + uInt16BE2);
                arrayList.add(rMHeader);
            }
            return arrayList;
        }

        public String toString() {
            return "PlayReadyRecord{type=" + this.type + ", length=" + getValue().limit() + '}';
        }

        public static class RMHeader extends PlayReadyRecord {
            String header;

            public RMHeader() {
                super(1);
            }

            @Override
            public void parse(ByteBuffer byteBuffer) {
                try {
                    byte[] bArr = new byte[byteBuffer.slice().limit()];
                    byteBuffer.get(bArr);
                    this.header = new String(bArr, "UTF-16LE");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public ByteBuffer getValue() {
                try {
                    return ByteBuffer.wrap(this.header.getBytes("UTF-16LE"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String toString() {
                return "RMHeader{length=" + getValue().limit() + ", header='" + this.header + "'}";
            }
        }

        public static class EmeddedLicenseStore extends PlayReadyRecord {
            ByteBuffer value;

            public EmeddedLicenseStore() {
                super(3);
            }

            @Override
            public void parse(ByteBuffer byteBuffer) {
                this.value = byteBuffer.duplicate();
            }

            @Override
            public ByteBuffer getValue() {
                return this.value;
            }

            @Override
            public String toString() {
                return "EmeddedLicenseStore{length=" + getValue().limit() + '}';
            }
        }

        public static class DefaulPlayReadyRecord extends PlayReadyRecord {
            ByteBuffer value;

            public DefaulPlayReadyRecord(int i) {
                super(i);
            }

            @Override
            public void parse(ByteBuffer byteBuffer) {
                this.value = byteBuffer.duplicate();
            }

            @Override
            public ByteBuffer getValue() {
                return this.value;
            }
        }
    }
}
