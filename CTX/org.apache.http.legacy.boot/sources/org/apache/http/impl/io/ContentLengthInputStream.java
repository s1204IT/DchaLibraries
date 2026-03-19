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

    public ContentLengthInputStream(SessionInputBuffer sessionInputBuffer, long j) {
        this.in = null;
        if (sessionInputBuffer == null) {
            throw new IllegalArgumentException("Input stream may not be null");
        }
        if (j < 0) {
            throw new IllegalArgumentException("Content length may not be negative");
        }
        this.in = sessionInputBuffer;
        this.contentLength = j;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            try {
                do {
                } while (read(new byte[BUFFER_SIZE]) >= 0);
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
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.closed) {
            throw new IOException("Attempted read from closed stream.");
        }
        if (this.pos >= this.contentLength) {
            return -1;
        }
        if (this.pos + ((long) i2) > this.contentLength) {
            i2 = (int) (this.contentLength - this.pos);
        }
        int i3 = this.in.read(bArr, i, i2);
        this.pos += (long) i3;
        return i3;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public long skip(long j) throws IOException {
        int i;
        if (j <= 0) {
            return 0L;
        }
        byte[] bArr = new byte[BUFFER_SIZE];
        long jMin = Math.min(j, this.contentLength - this.pos);
        long j2 = 0;
        while (jMin > 0 && (i = read(bArr, 0, (int) Math.min(2048L, jMin))) != -1) {
            long j3 = i;
            j2 += j3;
            jMin -= j3;
        }
        this.pos += j2;
        return j2;
    }
}
