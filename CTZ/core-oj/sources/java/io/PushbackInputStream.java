package java.io;

public class PushbackInputStream extends FilterInputStream {
    protected byte[] buf;
    protected int pos;

    private void ensureOpen() throws IOException {
        if (this.in == null) {
            throw new IOException("Stream closed");
        }
    }

    public PushbackInputStream(InputStream inputStream, int i) {
        super(inputStream);
        if (i <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.buf = new byte[i];
        this.pos = i;
    }

    public PushbackInputStream(InputStream inputStream) {
        this(inputStream, 1);
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        if (this.pos < this.buf.length) {
            byte[] bArr = this.buf;
            int i = this.pos;
            this.pos = i + 1;
            return bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
        }
        return super.read();
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        ensureOpen();
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        int length = this.buf.length - this.pos;
        if (length > 0) {
            if (i2 < length) {
                length = i2;
            }
            System.arraycopy(this.buf, this.pos, bArr, i, length);
            this.pos += length;
            i += length;
            i2 -= length;
        }
        if (i2 > 0) {
            int i3 = super.read(bArr, i, i2);
            if (i3 == -1) {
                if (length == 0) {
                    return -1;
                }
                return length;
            }
            return length + i3;
        }
        return length;
    }

    public void unread(int i) throws IOException {
        ensureOpen();
        if (this.pos == 0) {
            throw new IOException("Push back buffer is full");
        }
        byte[] bArr = this.buf;
        int i2 = this.pos - 1;
        this.pos = i2;
        bArr[i2] = (byte) i;
    }

    public void unread(byte[] bArr, int i, int i2) throws IOException {
        ensureOpen();
        if (i2 > this.pos) {
            throw new IOException("Push back buffer is full");
        }
        this.pos -= i2;
        System.arraycopy(bArr, i, this.buf, this.pos, i2);
    }

    public void unread(byte[] bArr) throws IOException {
        unread(bArr, 0, bArr.length);
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        int length = this.buf.length - this.pos;
        int iAvailable = super.available();
        if (length > Integer.MAX_VALUE - iAvailable) {
            return Integer.MAX_VALUE;
        }
        return length + iAvailable;
    }

    @Override
    public long skip(long j) throws IOException {
        ensureOpen();
        if (j <= 0) {
            return 0L;
        }
        long length = this.buf.length - this.pos;
        if (length > 0) {
            if (j < length) {
                length = j;
            }
            this.pos = (int) (((long) this.pos) + length);
            j -= length;
        }
        if (j > 0) {
            return length + super.skip(j);
        }
        return length;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int i) {
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.in == null) {
            return;
        }
        this.in.close();
        this.in = null;
        this.buf = null;
    }
}
