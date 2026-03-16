package java.io;

import java.util.Arrays;

public class CharArrayReader extends Reader {
    protected char[] buf;
    protected int count;
    protected int markedPos;
    protected int pos;

    public CharArrayReader(char[] buf) {
        this.markedPos = -1;
        this.buf = buf;
        this.count = buf.length;
    }

    public CharArrayReader(char[] buf, int offset, int length) {
        this.markedPos = -1;
        if (offset < 0 || offset > buf.length || length < 0 || offset + length < 0) {
            throw new IllegalArgumentException();
        }
        this.buf = buf;
        this.pos = offset;
        this.markedPos = offset;
        int bufferLength = buf.length;
        this.count = offset + length >= bufferLength ? bufferLength : length;
    }

    @Override
    public void close() {
        synchronized (this.lock) {
            if (isOpen()) {
                this.buf = null;
            }
        }
    }

    private boolean isOpen() {
        return this.buf != null;
    }

    private boolean isClosed() {
        return this.buf == null;
    }

    @Override
    public void mark(int readLimit) throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            this.markedPos = this.pos;
        }
    }

    private void checkNotClosed() throws IOException {
        if (isClosed()) {
            throw new IOException("CharArrayReader is closed");
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        int i;
        synchronized (this.lock) {
            checkNotClosed();
            if (this.pos == this.count) {
                i = -1;
            } else {
                char[] cArr = this.buf;
                int i2 = this.pos;
                this.pos = i2 + 1;
                i = cArr[i2];
            }
        }
        return i;
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        int bytesRead;
        Arrays.checkOffsetAndCount(buffer.length, offset, count);
        synchronized (this.lock) {
            checkNotClosed();
            if (this.pos < this.count) {
                bytesRead = this.pos + count > this.count ? this.count - this.pos : count;
                System.arraycopy(this.buf, this.pos, buffer, offset, bytesRead);
                this.pos += bytesRead;
            } else {
                bytesRead = -1;
            }
        }
        return bytesRead;
    }

    @Override
    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            checkNotClosed();
            z = this.pos != this.count;
        }
        return z;
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            this.pos = this.markedPos != -1 ? this.markedPos : 0;
        }
    }

    @Override
    public long skip(long charCount) throws IOException {
        long skipped = 0;
        synchronized (this.lock) {
            checkNotClosed();
            if (charCount > 0) {
                if (charCount < this.count - this.pos) {
                    this.pos += (int) charCount;
                    skipped = charCount;
                } else {
                    skipped = this.count - this.pos;
                    this.pos = this.count;
                }
            }
        }
        return skipped;
    }
}
