package java.util.zip;

import java.util.Arrays;

public class Adler32 implements Checksum {
    private long adler = 1;

    private native long updateByteImpl(int i, long j);

    private native long updateImpl(byte[] bArr, int i, int i2, long j);

    @Override
    public long getValue() {
        return this.adler;
    }

    @Override
    public void reset() {
        this.adler = 1L;
    }

    @Override
    public void update(int i) {
        this.adler = updateByteImpl(i, this.adler);
    }

    public void update(byte[] buf) {
        update(buf, 0, buf.length);
    }

    @Override
    public void update(byte[] buf, int offset, int byteCount) {
        Arrays.checkOffsetAndCount(buf.length, offset, byteCount);
        this.adler = updateImpl(buf, offset, byteCount, this.adler);
    }
}
