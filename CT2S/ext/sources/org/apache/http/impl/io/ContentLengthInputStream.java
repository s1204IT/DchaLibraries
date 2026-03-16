package org.apache.http.impl.io;

import java.io.IOException;
import java.io.InputStream;
import org.apache.http.io.SessionInputBuffer;

@Deprecated
public class ContentLengthInputStream extends InputStream {
    private static final int BUFFER_SIZE = 2048;
    private long contentLength;
    private SessionInputBuffer in;
    private long pos = 0;
    private boolean closed = false;

    public ContentLengthInputStream(SessionInputBuffer in, long contentLength) {
        this.in = null;
        if (in == null) {
            throw new IllegalArgumentException("Input stream may not be null");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content length may not be negative");
        }
        this.in = in;
        this.contentLength = contentLength;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            try {
                byte[] buffer = new byte[2048];
                do {
                } while (read(buffer) >= 0);
            } finally {
                this.closed = true;
            }
        }
    }

    @Override
    public int read() throws IOException {
        if (this.closed) {
            throw new IOException("Attempted read from closed stream.");
        }
        if (this.pos >= this.contentLength) {
            return -1;
        }
        this.pos++;
        return this.in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.closed) {
            throw new IOException("Attempted read from closed stream.");
        }
        if (this.pos >= this.contentLength) {
            return -1;
        }
        if (this.pos + ((long) len) > this.contentLength) {
            len = (int) (this.contentLength - this.pos);
        }
        int count = this.in.read(b, off, len);
        this.pos += (long) count;
        return count;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        int l;
        if (n <= 0) {
            return 0L;
        }
        byte[] buffer = new byte[2048];
        long remaining = Math.min(n, this.contentLength - this.pos);
        long count = 0;
        while (remaining > 0 && (l = read(buffer, 0, (int) Math.min(2048L, remaining))) != -1) {
            count += (long) l;
            remaining -= (long) l;
        }
        this.pos += count;
        return count;
    }
}
