package java.util.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import libcore.io.Streams;

public class DeflaterInputStream extends FilterInputStream {
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private boolean available;
    protected final byte[] buf;
    private boolean closed;
    protected final Deflater def;

    public DeflaterInputStream(InputStream in) {
        this(in, new Deflater(), 1024);
    }

    public DeflaterInputStream(InputStream in, Deflater deflater) {
        this(in, deflater, 1024);
    }

    public DeflaterInputStream(InputStream in, Deflater deflater, int bufferSize) {
        super(in);
        this.closed = false;
        this.available = true;
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        if (deflater == null) {
            throw new NullPointerException("deflater == null");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize <= 0: " + bufferSize);
        }
        this.def = deflater;
        this.buf = new byte[bufferSize];
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.def.end();
        this.in.close();
    }

    @Override
    public int read() throws IOException {
        return Streams.readSingleByte(this);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        checkClosed();
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        if (byteCount == 0) {
            return 0;
        }
        if (!this.available) {
            return -1;
        }
        int count = 0;
        while (count < byteCount && !this.def.finished()) {
            if (this.def.needsInput()) {
                int bytesRead = this.in.read(this.buf);
                if (bytesRead == -1) {
                    this.def.finish();
                } else {
                    this.def.setInput(this.buf, 0, bytesRead);
                }
            }
            int bytesDeflated = this.def.deflate(buffer, byteOffset + count, byteCount - count);
            if (bytesDeflated == -1) {
                break;
            }
            count += bytesDeflated;
        }
        if (count == 0) {
            count = -1;
            this.available = false;
        }
        if (this.def.finished()) {
            this.available = false;
            return count;
        }
        return count;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return Streams.skipByReading(this, Math.min(2147483647L, byteCount));
    }

    @Override
    public int available() throws IOException {
        checkClosed();
        return this.available ? 1 : 0;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int limit) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException();
    }

    private void checkClosed() throws IOException {
        if (this.closed) {
            throw new IOException("Stream is closed");
        }
    }
}
