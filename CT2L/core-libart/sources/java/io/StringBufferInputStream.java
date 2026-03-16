package java.io;

import java.util.Arrays;

@Deprecated
public class StringBufferInputStream extends InputStream {
    protected String buffer;
    protected int count;
    protected int pos;

    public StringBufferInputStream(String str) {
        if (str == null) {
            throw new NullPointerException("str == null");
        }
        this.buffer = str;
        this.count = str.length();
    }

    @Override
    public synchronized int available() {
        return this.count - this.pos;
    }

    @Override
    public synchronized int read() {
        int iCharAt;
        if (this.pos < this.count) {
            String str = this.buffer;
            int i = this.pos;
            this.pos = i + 1;
            iCharAt = str.charAt(i) & 255;
        } else {
            iCharAt = -1;
        }
        return iCharAt;
    }

    @Override
    public synchronized int read(byte[] buffer, int byteOffset, int byteCount) {
        int copylen;
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        if (byteCount == 0) {
            copylen = 0;
        } else {
            copylen = this.count - this.pos < byteCount ? this.count - this.pos : byteCount;
            for (int i = 0; i < copylen; i++) {
                buffer[byteOffset + i] = (byte) this.buffer.charAt(this.pos + i);
            }
            this.pos += copylen;
        }
        return copylen;
    }

    @Override
    public synchronized void reset() {
        this.pos = 0;
    }

    @Override
    public synchronized long skip(long charCount) {
        int numskipped;
        long j = 0;
        synchronized (this) {
            if (charCount > 0) {
                if (this.count - this.pos < charCount) {
                    numskipped = this.count - this.pos;
                    this.pos = this.count;
                } else {
                    numskipped = (int) charCount;
                    this.pos = (int) (((long) this.pos) + charCount);
                }
                j = numskipped;
            }
        }
        return j;
    }
}
