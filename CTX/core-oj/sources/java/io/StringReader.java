package java.io;

public class StringReader extends Reader {
    private int length;
    private String str;
    private int next = 0;
    private int mark = 0;

    public StringReader(String str) {
        this.str = str;
        this.length = str.length();
    }

    private void ensureOpen() throws IOException {
        if (this.str == null) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (this.next >= this.length) {
                return -1;
            }
            String str = this.str;
            int i = this.next;
            this.next = i + 1;
            return str.charAt(i);
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
            if (this.next >= this.length) {
                return -1;
            }
            int iMin = Math.min(this.length - this.next, i2);
            this.str.getChars(this.next, this.next + iMin, cArr, i);
            this.next += iMin;
            return iMin;
        }
    }

    @Override
    public long skip(long j) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (this.next >= this.length) {
                return 0L;
            }
            long jMax = Math.max(-this.next, Math.min(this.length - this.next, j));
            this.next = (int) (((long) this.next) + jMax);
            return jMax;
        }
    }

    @Override
    public boolean ready() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
        }
        return true;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int i) throws IOException {
        if (i < 0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        synchronized (this.lock) {
            ensureOpen();
            this.mark = this.next;
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            this.next = this.mark;
        }
    }

    @Override
    public void close() {
        this.str = null;
    }
}
