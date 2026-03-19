package java.io;

import java.nio.CharBuffer;

public abstract class Reader implements Readable, Closeable {
    private static final int maxSkipBufferSize = 8192;
    protected Object lock;
    private char[] skipBuffer;

    public abstract void close() throws IOException;

    public abstract int read(char[] cArr, int i, int i2) throws IOException;

    protected Reader() {
        this.skipBuffer = null;
        this.lock = this;
    }

    protected Reader(Object obj) {
        this.skipBuffer = null;
        if (obj == null) {
            throw new NullPointerException();
        }
        this.lock = obj;
    }

    @Override
    public int read(CharBuffer charBuffer) throws IOException {
        int iRemaining = charBuffer.remaining();
        char[] cArr = new char[iRemaining];
        int i = read(cArr, 0, iRemaining);
        if (i > 0) {
            charBuffer.put(cArr, 0, i);
        }
        return i;
    }

    public int read() throws IOException {
        char[] cArr = new char[1];
        if (read(cArr, 0, 1) == -1) {
            return -1;
        }
        return cArr[0];
    }

    public int read(char[] cArr) throws IOException {
        return read(cArr, 0, cArr.length);
    }

    public long skip(long j) throws IOException {
        long j2;
        int i;
        if (j < 0) {
            throw new IllegalArgumentException("skip value is negative");
        }
        int iMin = (int) Math.min(j, 8192L);
        synchronized (this.lock) {
            if (this.skipBuffer == null || this.skipBuffer.length < iMin) {
                this.skipBuffer = new char[iMin];
            }
            long j3 = j;
            while (j3 > 0 && (i = read(this.skipBuffer, 0, (int) Math.min(j3, iMin))) != -1) {
                j3 -= (long) i;
            }
            j2 = j - j3;
        }
        return j2;
    }

    public boolean ready() throws IOException {
        return false;
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(int i) throws IOException {
        throw new IOException("mark() not supported");
    }

    public void reset() throws IOException {
        throw new IOException("reset() not supported");
    }
}
