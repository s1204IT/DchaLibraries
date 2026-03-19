package java.io;

public class ByteArrayInputStream extends InputStream {
    protected byte[] buf;
    protected int count;
    protected int mark;
    protected int pos;

    public ByteArrayInputStream(byte[] bArr) {
        this.mark = 0;
        this.buf = bArr;
        this.pos = 0;
        this.count = bArr.length;
    }

    public ByteArrayInputStream(byte[] bArr, int i, int i2) {
        this.mark = 0;
        this.buf = bArr;
        this.pos = i;
        this.count = Math.min(i2 + i, bArr.length);
        this.mark = i;
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
    public synchronized int read(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (this.pos >= this.count) {
            return -1;
        }
        int i3 = this.count - this.pos;
        if (i2 > i3) {
            i2 = i3;
        }
        if (i2 <= 0) {
            return 0;
        }
        System.arraycopy(this.buf, this.pos, bArr, i, i2);
        this.pos += i2;
        return i2;
    }

    @Override
    public synchronized long skip(long j) {
        long j2;
        j2 = this.count - this.pos;
        if (j < j2) {
            j2 = 0;
            if (j >= 0) {
                j2 = j;
            }
        }
        this.pos = (int) (((long) this.pos) + j2);
        return j2;
    }

    @Override
    public synchronized int available() {
        return this.count - this.pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int i) {
        this.mark = this.pos;
    }

    @Override
    public synchronized void reset() {
        this.pos = this.mark;
    }

    @Override
    public void close() throws IOException {
    }
}
