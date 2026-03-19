package java.io;

public class PushbackReader extends FilterReader {
    private char[] buf;
    private int pos;

    public PushbackReader(Reader reader, int i) {
        super(reader);
        if (i <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.buf = new char[i];
        this.pos = i;
    }

    public PushbackReader(Reader reader) {
        this(reader, 1);
    }

    private void ensureOpen() throws IOException {
        if (this.buf == null) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (this.pos < this.buf.length) {
                char[] cArr = this.buf;
                int i = this.pos;
                this.pos = i + 1;
                return cArr[i];
            }
            return super.read();
        }
    }

    @Override
    public int read(char[] cArr, int i, int i2) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            try {
                if (i2 <= 0) {
                    if (i2 < 0) {
                        throw new IndexOutOfBoundsException();
                    }
                    if (i < 0 || i > cArr.length) {
                        throw new IndexOutOfBoundsException();
                    }
                    return 0;
                }
                int length = this.buf.length - this.pos;
                if (length > 0) {
                    if (i2 < length) {
                        length = i2;
                    }
                    System.arraycopy((Object) this.buf, this.pos, (Object) cArr, i, length);
                    this.pos += length;
                    i += length;
                    i2 -= length;
                }
                if (i2 <= 0) {
                    return length;
                }
                int i3 = super.read(cArr, i, i2);
                if (i3 == -1) {
                    return length != 0 ? length : -1;
                }
                return length + i3;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    public void unread(int i) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (this.pos == 0) {
                throw new IOException("Pushback buffer overflow");
            }
            char[] cArr = this.buf;
            int i2 = this.pos - 1;
            this.pos = i2;
            cArr[i2] = (char) i;
        }
    }

    public void unread(char[] cArr, int i, int i2) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (i2 > this.pos) {
                throw new IOException("Pushback buffer overflow");
            }
            this.pos -= i2;
            System.arraycopy((Object) cArr, i, (Object) this.buf, this.pos, i2);
        }
    }

    public void unread(char[] cArr) throws IOException {
        unread(cArr, 0, cArr.length);
    }

    @Override
    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            ensureOpen();
            z = this.pos < this.buf.length || super.ready();
        }
        return z;
    }

    @Override
    public void mark(int i) throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.buf = null;
    }

    @Override
    public long skip(long j) throws IOException {
        if (j < 0) {
            throw new IllegalArgumentException("skip value is negative");
        }
        synchronized (this.lock) {
            ensureOpen();
            int length = this.buf.length - this.pos;
            if (length > 0) {
                long j2 = length;
                if (j <= j2) {
                    this.pos = (int) (((long) this.pos) + j);
                    return j;
                }
                this.pos = this.buf.length;
                j -= j2;
            }
            return ((long) length) + super.skip(j);
        }
    }
}
