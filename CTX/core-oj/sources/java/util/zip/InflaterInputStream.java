package java.util.zip;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InflaterInputStream extends FilterInputStream {
    private byte[] b;
    protected byte[] buf;
    protected boolean closed;
    protected Inflater inf;
    protected int len;
    private boolean reachEOF;
    private byte[] singleByteBuf;

    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
    }

    public InflaterInputStream(InputStream inputStream, Inflater inflater, int i) {
        super(inputStream);
        this.closed = false;
        this.reachEOF = false;
        this.singleByteBuf = new byte[1];
        this.b = new byte[512];
        if (inputStream == null || inflater == null) {
            throw new NullPointerException();
        }
        if (i <= 0) {
            throw new IllegalArgumentException("buffer size <= 0");
        }
        this.inf = inflater;
        this.buf = new byte[i];
    }

    public InflaterInputStream(InputStream inputStream, Inflater inflater) {
        this(inputStream, inflater, 512);
    }

    public InflaterInputStream(InputStream inputStream) {
        this(inputStream, new Inflater());
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        if (read(this.singleByteBuf, 0, 1) == -1) {
            return -1;
        }
        return Byte.toUnsignedInt(this.singleByteBuf[0]);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        ensureOpen();
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        while (true) {
            try {
                int iInflate = this.inf.inflate(bArr, i, i2);
                if (iInflate == 0) {
                    if (this.inf.finished() || this.inf.needsDictionary()) {
                        break;
                    }
                    if (this.inf.needsInput()) {
                        fill();
                    }
                } else {
                    if (this.inf.finished()) {
                        this.reachEOF = true;
                    }
                    return iInflate;
                }
            } catch (DataFormatException e) {
                String message = e.getMessage();
                if (message == null) {
                    message = "Invalid ZLIB data format";
                }
                throw new ZipException(message);
            }
        }
        this.reachEOF = true;
        return -1;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        if (this.reachEOF) {
            return 0;
        }
        return 1;
    }

    @Override
    public long skip(long j) throws IOException {
        if (j < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        int iMin = (int) Math.min(j, 2147483647L);
        int i = 0;
        while (true) {
            if (i >= iMin) {
                break;
            }
            int length = iMin - i;
            if (length > this.b.length) {
                length = this.b.length;
            }
            int i2 = read(this.b, 0, length);
            if (i2 == -1) {
                this.reachEOF = true;
                break;
            }
            i += i2;
        }
        return i;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            this.inf.end();
            this.in.close();
            this.closed = true;
        }
    }

    protected void fill() throws IOException {
        ensureOpen();
        this.len = this.in.read(this.buf, 0, this.buf.length);
        if (this.len == -1) {
            throw new EOFException("Unexpected end of ZLIB input stream");
        }
        this.inf.setInput(this.buf, 0, this.len);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int i) {
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
