package java.util.zip;

import java.util.Arrays;

public class CRC32 implements Checksum {
    private long crc = 0;
    long tbytes = 0;

    private native long updateByteImpl(byte b, long j);

    private native long updateImpl(byte[] bArr, int i, int i2, long j);

    @Override
    public long getValue() {
        return this.crc;
    }

    @Override
    public void reset() {
        this.crc = 0L;
        this.tbytes = 0L;
    }

    @Override
    public void update(int val) {
        this.crc = updateByteImpl((byte) val, this.crc);
    }

    public void update(byte[] buf) {
        update(buf, 0, buf.length);
    }

    @Override
    public void update(byte[] buf, int offset, int byteCount) {
        Arrays.checkOffsetAndCount(buf.length, offset, byteCount);
        this.tbytes += (long) byteCount;
        this.crc = updateImpl(buf, offset, byteCount, this.crc);
    }
}
