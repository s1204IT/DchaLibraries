package java.io;

import java.util.Arrays;

public class ByteArrayInputStream extends InputStream {
    protected byte[] buf;
    protected int count;
    protected int mark;
    protected int pos;

    public ByteArrayInputStream(byte[] buf) {
        this.mark = 0;
        this.buf = buf;
        this.count = buf.length;
    }

    public ByteArrayInputStream(byte[] buf, int offset, int length) {
        this.buf = buf;
        this.pos = offset;
        this.mark = offset;
        this.count = offset + length > buf.length ? buf.length : offset + length;
    }

    @Override
    public synchronized int available() {
        return this.count - this.pos;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.mark = this.pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized int read() {
        int i;
        if (this.pos < this.count) {
            byte[] bArr = this.buf;
            int i2 = this.pos;
            this.pos = i2 + 1;
            i = bArr[i2] & Character.DIRECTIONALITY_UNDEFINED;
        } else {
            i = -1;
        }
        return i;
    }

    @Override
    public synchronized int read(byte[] buffer, int byteOffset, int byteCount) {
        int copylen;
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        if (this.pos >= this.count) {
            copylen = -1;
        } else if (byteCount == 0) {
            copylen = 0;
        } else {
            copylen = this.count - this.pos < byteCount ? this.count - this.pos : byteCount;
            System.arraycopy(this.buf, this.pos, buffer, byteOffset, copylen);
            this.pos += copylen;
        }
        return copylen;
    }

    @Override
    public synchronized void reset() {
        this.pos = this.mark;
    }

    @Override
    public synchronized long skip(long byteCount) {
        long j = 0;
        synchronized (this) {
            if (byteCount > 0) {
                int temp = this.pos;
                this.pos = ((long) (this.count - this.pos)) < byteCount ? this.count : (int) (((long) this.pos) + byteCount);
                j = this.pos - temp;
            }
        }
        return j;
    }
}
