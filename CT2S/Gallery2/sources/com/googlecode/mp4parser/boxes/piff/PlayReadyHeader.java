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
        int recordCount = IsoTypeReader.readUInt16BE(byteBuffer);
        this.records = PlayReadyRecord.createFor(byteBuffer, recordCount);
    }

    @Override
    public ByteBuffer getData() {
        int size = 6;
        Iterator<PlayReadyRecord> it = this.records.iterator();
        while (it.hasNext()) {
            size = size + 4 + it.next().getValue().rewind().limit();
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        IsoTypeWriter.writeUInt32BE(byteBuffer, size);
        IsoTypeWriter.writeUInt16BE(byteBuffer, this.records.size());
        for (PlayReadyRecord record : this.records) {
            IsoTypeWriter.writeUInt16BE(byteBuffer, record.type);
            IsoTypeWriter.writeUInt16BE(byteBuffer, record.getValue().limit());
            ByteBuffer tmp4debug = record.getValue();
            byteBuffer.put(tmp4debug);
        }
        return byteBuffer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlayReadyHeader");
        sb.append("{length=").append(this.length);
        sb.append(", recordCount=").append(this.records.size());
        sb.append(", records=").append(this.records);
        sb.append('}');
        return sb.toString();
    }

    public static abstract class PlayReadyRecord {
        int type;

        public abstract ByteBuffer getValue();

        public abstract void parse(ByteBuffer byteBuffer);

        public PlayReadyRecord(int type) {
            this.type = type;
        }

        public static List<PlayReadyRecord> createFor(ByteBuffer byteBuffer, int recordCount) {
            PlayReadyRecord record;
            List<PlayReadyRecord> records = new ArrayList<>(recordCount);
            for (int i = 0; i < recordCount; i++) {
                int type = IsoTypeReader.readUInt16BE(byteBuffer);
                int length = IsoTypeReader.readUInt16BE(byteBuffer);
                switch (type) {
                    case 1:
                        record = new RMHeader();
                        break;
                    case 2:
                        record = new DefaulPlayReadyRecord(2);
                        break;
                    case 3:
                        record = new EmeddedLicenseStore();
                        break;
                    default:
                        record = new DefaulPlayReadyRecord(type);
                        break;
                }
                record.parse((ByteBuffer) byteBuffer.slice().limit(length));
                byteBuffer.position(byteBuffer.position() + length);
                records.add(record);
            }
            return records;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PlayReadyRecord");
            sb.append("{type=").append(this.type);
            sb.append(", length=").append(getValue().limit());
            sb.append('}');
            return sb.toString();
        }

        public static class RMHeader extends PlayReadyRecord {
            String header;

            public RMHeader() {
                super(1);
            }

            @Override
            public void parse(ByteBuffer bytes) {
                try {
                    byte[] str = new byte[bytes.slice().limit()];
                    bytes.get(str);
                    this.header = new String(str, "UTF-16LE");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public ByteBuffer getValue() {
                try {
                    byte[] headerBytes = this.header.getBytes("UTF-16LE");
                    return ByteBuffer.wrap(headerBytes);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("RMHeader");
                sb.append("{length=").append(getValue().limit());
                sb.append(", header='").append(this.header).append('\'');
                sb.append('}');
                return sb.toString();
            }
        }

        public static class EmeddedLicenseStore extends PlayReadyRecord {
            ByteBuffer value;

            public EmeddedLicenseStore() {
                super(3);
            }

            @Override
            public void parse(ByteBuffer bytes) {
                this.value = bytes.duplicate();
            }

            @Override
            public ByteBuffer getValue() {
                return this.value;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("EmeddedLicenseStore");
                sb.append("{length=").append(getValue().limit());
                sb.append('}');
                return sb.toString();
            }
        }

        public static class DefaulPlayReadyRecord extends PlayReadyRecord {
            ByteBuffer value;

            public DefaulPlayReadyRecord(int type) {
                super(type);
            }

            @Override
            public void parse(ByteBuffer bytes) {
                this.value = bytes.duplicate();
            }

            @Override
            public ByteBuffer getValue() {
                return this.value;
            }
        }
    }
}
