package java.io;

import java.util.Arrays;

public class StringReader extends Reader {
    private int count;
    private int markpos = -1;
    private int pos;
    private String str;

    public StringReader(String str) {
        this.str = str;
        this.count = str.length();
    }

    @Override
    public void close() {
        this.str = null;
    }

    private boolean isClosed() {
        return this.str == null;
    }

    @Override
    public void mark(int readLimit) throws IOException {
        if (readLimit < 0) {
            throw new IllegalArgumentException("readLimit < 0: " + readLimit);
        }
        synchronized (this.lock) {
            checkNotClosed();
            this.markpos = this.pos;
        }
    }

    private void checkNotClosed() throws IOException {
        if (isClosed()) {
            throw new IOException("StringReader is closed");
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        int iCharAt;
        synchronized (this.lock) {
            checkNotClosed();
            if (this.pos != this.count) {
                String str = this.str;
                int i = this.pos;
                this.pos = i + 1;
                iCharAt = str.charAt(i);
            } else {
                iCharAt = -1;
            }
        }
        return iCharAt;
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        int i;
        synchronized (this.lock) {
            checkNotClosed();
            Arrays.checkOffsetAndCount(buffer.length, offset, count);
            if (count == 0) {
                i = 0;
            } else if (this.pos == this.count) {
                i = -1;
            } else {
                int end = this.pos + count > this.count ? this.count : this.pos + count;
                this.str.getChars(this.pos, end, buffer, offset);
                i = end - this.pos;
                this.pos = end;
            }
        }
        return i;
    }

    @Override
    public boolean ready() throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
        }
        return true;
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            this.pos = this.markpos != -1 ? this.markpos : 0;
        }
    }

    @Override
    public long skip(long charCount) throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            int minSkip = -this.pos;
            int maxSkip = this.count - this.pos;
            if (maxSkip == 0 || charCount > maxSkip) {
                charCount = maxSkip;
            } else if (charCount < minSkip) {
                charCount = minSkip;
            }
            this.pos = (int) (((long) this.pos) + charCount);
        }
        return charCount;
    }
}
