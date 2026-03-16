package java.io;

import java.util.Arrays;

public class PushbackInputStream extends FilterInputStream {
    protected byte[] buf;
    protected int pos;

    public PushbackInputStream(InputStream in) {
        super(in);
        this.buf = in == null ? null : new byte[1];
        this.pos = 1;
    }

    public PushbackInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.buf = in == null ? null : new byte[size];
        this.pos = size;
    }

    @Override
    public int available() throws IOException {
        if (this.buf == null) {
            throw new IOException();
        }
        return (this.buf.length - this.pos) + this.in.available();
    }

    @Override
    public void close() throws IOException {
        if (this.in != null) {
            this.in.close();
            this.in = null;
            this.buf = null;
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        if (this.buf == null) {
            throw new IOException();
        }
        if (this.pos >= this.buf.length) {
            return this.in.read();
        }
        byte[] bArr = this.buf;
        int i = this.pos;
        this.pos = i + 1;
        return bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (this.buf == null) {
            throw streamClosed();
        }
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        int copiedBytes = 0;
        int copyLength = 0;
        int newOffset = byteOffset;
        if (this.pos < this.buf.length) {
            copyLength = this.buf.length - this.pos >= byteCount ? byteCount : this.buf.length - this.pos;
            System.arraycopy(this.buf, this.pos, buffer, newOffset, copyLength);
            newOffset += copyLength;
            copiedBytes = 0 + copyLength;
            this.pos += copyLength;
        }
        if (copyLength != byteCount) {
            int inCopied = this.in.read(buffer, newOffset, byteCount - copiedBytes);
            if (inCopied > 0) {
                return inCopied + copiedBytes;
            }
            return copiedBytes == 0 ? inCopied : copiedBytes;
        }
        return byteCount;
    }

    private IOException streamClosed() throws IOException {
        throw new IOException("PushbackInputStream is closed");
    }

    @Override
    public long skip(long byteCount) throws IOException {
        if (this.in == null) {
            throw streamClosed();
        }
        if (byteCount <= 0) {
            return 0L;
        }
        int numSkipped = 0;
        if (this.pos < this.buf.length) {
            numSkipped = (int) ((byteCount < ((long) (this.buf.length - this.pos)) ? byteCount : this.buf.length - this.pos) + ((long) 0));
            this.pos += numSkipped;
        }
        if (numSkipped < byteCount) {
            numSkipped = (int) (((long) numSkipped) + this.in.skip(byteCount - ((long) numSkipped)));
        }
        return numSkipped;
    }

    public void unread(byte[] buffer) throws IOException {
        unread(buffer, 0, buffer.length);
    }

    public void unread(byte[] buffer, int offset, int length) throws IOException {
        if (length > this.pos) {
            throw new IOException("Pushback buffer full");
        }
        Arrays.checkOffsetAndCount(buffer.length, offset, length);
        if (this.buf == null) {
            throw streamClosed();
        }
        System.arraycopy(buffer, offset, this.buf, this.pos - length, length);
        this.pos -= length;
    }

    public void unread(int oneByte) throws IOException {
        if (this.buf == null) {
            throw new IOException();
        }
        if (this.pos == 0) {
            throw new IOException("Pushback buffer full");
        }
        byte[] bArr = this.buf;
        int i = this.pos - 1;
        this.pos = i;
        bArr[i] = (byte) oneByte;
    }

    @Override
    public void mark(int readlimit) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException();
    }
}
