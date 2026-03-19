package java.io;

public class CharArrayReader extends Reader {
    protected char[] buf;
    protected int count;
    protected int markedPos;
    protected int pos;

    public CharArrayReader(char[] cArr) {
        this.markedPos = 0;
        this.buf = cArr;
        this.pos = 0;
        this.count = cArr.length;
    }

    public CharArrayReader(char[] cArr, int i, int i2) {
        int i3;
        this.markedPos = 0;
        if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i2 + i) < 0) {
            throw new IllegalArgumentException();
        }
        this.buf = cArr;
        this.pos = i;
        this.count = Math.min(i3, cArr.length);
        this.markedPos = i;
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
            if (this.pos >= this.count) {
                return -1;
            }
            char[] cArr = this.buf;
            int i = this.pos;
            this.pos = i + 1;
            return cArr[i];
        }
    }

    @Override
    public int read(char[] cArr, int i, int i2) throws IOException {
        int i3;
        synchronized (this.lock) {
            ensureOpen();
            if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) > cArr.length || i3 < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (i2 == 0) {
                return 0;
            }
            if (this.pos >= this.count) {
                return -1;
            }
            int i4 = this.count - this.pos;
            if (i2 > i4) {
                i2 = i4;
            }
            if (i2 <= 0) {
                return 0;
            }
            System.arraycopy((Object) this.buf, this.pos, (Object) cArr, i, i2);
            this.pos += i2;
            return i2;
        }
    }

    @Override
    public long skip(long j) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            long j2 = this.count - this.pos;
            if (j > j2) {
                j = j2;
            }
            if (j < 0) {
                return 0L;
            }
            this.pos = (int) (((long) this.pos) + j);
            return j;
        }
    }

    @Override
    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            ensureOpen();
            z = this.count - this.pos > 0;
        }
        return z;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int i) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            this.markedPos = this.pos;
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            this.pos = this.markedPos;
        }
    }

    @Override
    public void close() {
        this.buf = null;
    }
}
