package java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class InflaterOutputStream extends FilterOutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    protected final byte[] buf;
    private boolean closed;
    protected final Inflater inf;

    public InflaterOutputStream(OutputStream out) {
        this(out, new Inflater());
    }

    public InflaterOutputStream(OutputStream out, Inflater inf) {
        this(out, inf, 1024);
    }

    public InflaterOutputStream(OutputStream out, Inflater inf, int bufferSize) {
        super(out);
        this.closed = false;
        if (out == null) {
            throw new NullPointerException("out == null");
        }
        if (inf == null) {
            throw new NullPointerException("inf == null");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize <= 0: " + bufferSize);
        }
        this.inf = inf;
        this.buf = new byte[bufferSize];
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            finish();
            this.inf.end();
            this.out.close();
            this.closed = true;
        }
    }

    @Override
    public void flush() throws IOException {
        finish();
        this.out.flush();
    }

    public void finish() throws IOException {
        checkClosed();
        write();
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] bytes, int offset, int byteCount) throws IOException {
        checkClosed();
        Arrays.checkOffsetAndCount(bytes.length, offset, byteCount);
        this.inf.setInput(bytes, offset, byteCount);
        write();
    }

    private void write() throws IOException {
        while (true) {
            try {
                int inflated = this.inf.inflate(this.buf);
                if (inflated > 0) {
                    this.out.write(this.buf, 0, inflated);
                } else {
                    return;
                }
            } catch (DataFormatException e) {
                throw new ZipException();
            }
        }
    }

    private void checkClosed() throws IOException {
        if (this.closed) {
            throw new IOException();
        }
    }
}
