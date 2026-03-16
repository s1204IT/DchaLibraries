package java.util.zip;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipFile;
import libcore.io.Streams;

public class InflaterInputStream extends FilterInputStream {
    static final int BUF_SIZE = 512;
    protected byte[] buf;
    boolean closed;
    boolean eof;
    protected Inflater inf;
    protected int len;
    int nativeEndBufSize;

    public InflaterInputStream(InputStream is) {
        this(is, new Inflater(), 512);
    }

    public InflaterInputStream(InputStream is, Inflater inflater) {
        this(is, inflater, 512);
    }

    public InflaterInputStream(InputStream is, Inflater inflater, int bufferSize) {
        super(is);
        this.nativeEndBufSize = 0;
        if (is == null) {
            throw new NullPointerException("is == null");
        }
        if (inflater == null) {
            throw new NullPointerException("inflater == null");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize <= 0: " + bufferSize);
        }
        this.inf = inflater;
        if (is instanceof ZipFile.RAFStream) {
            this.nativeEndBufSize = bufferSize;
        } else {
            this.buf = new byte[bufferSize];
        }
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
        if (this.eof) {
            return -1;
        }
        do {
            if (this.inf.needsInput()) {
                fill();
            }
            try {
                int result = this.inf.inflate(buffer, byteOffset, byteCount);
                this.eof = this.inf.finished();
                if (result <= 0) {
                    if (this.eof) {
                        return -1;
                    }
                    if (this.inf.needsDictionary()) {
                        this.eof = true;
                        return -1;
                    }
                } else {
                    return result;
                }
            } catch (DataFormatException e) {
                this.eof = true;
                if (this.len == -1) {
                    throw new EOFException();
                }
                throw ((IOException) new IOException().initCause(e));
            }
        } while (this.len != -1);
        this.eof = true;
        throw new EOFException();
    }

    protected void fill() throws IOException {
        checkClosed();
        if (this.nativeEndBufSize > 0) {
            ZipFile.RAFStream is = (ZipFile.RAFStream) this.in;
            this.len = is.fill(this.inf, this.nativeEndBufSize);
            return;
        }
        int i = this.in.read(this.buf);
        this.len = i;
        if (i > 0) {
            this.inf.setInput(this.buf, 0, this.len);
        }
    }

    @Override
    public long skip(long byteCount) throws IOException {
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount < 0");
        }
        return Streams.skipByReading(this, byteCount);
    }

    @Override
    public int available() throws IOException {
        checkClosed();
        return this.eof ? 0 : 1;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            this.inf.end();
            this.closed = true;
            this.eof = true;
            super.close();
        }
    }

    @Override
    public void mark(int readlimit) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void checkClosed() throws IOException {
        if (this.closed) {
            throw new IOException("Stream is closed");
        }
    }
}
