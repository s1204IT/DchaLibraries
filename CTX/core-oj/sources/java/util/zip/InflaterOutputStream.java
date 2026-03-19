package java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class InflaterOutputStream extends FilterOutputStream {
    protected final byte[] buf;
    private boolean closed;
    protected final Inflater inf;
    private boolean usesDefaultInflater;
    private final byte[] wbuf;

    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
    }

    public InflaterOutputStream(OutputStream outputStream) {
        this(outputStream, new Inflater());
        this.usesDefaultInflater = true;
    }

    public InflaterOutputStream(OutputStream outputStream, Inflater inflater) {
        this(outputStream, inflater, 512);
    }

    public InflaterOutputStream(OutputStream outputStream, Inflater inflater, int i) {
        super(outputStream);
        this.wbuf = new byte[1];
        this.usesDefaultInflater = false;
        this.closed = false;
        if (outputStream == null) {
            throw new NullPointerException("Null output");
        }
        if (inflater == null) {
            throw new NullPointerException("Null inflater");
        }
        if (i <= 0) {
            throw new IllegalArgumentException("Buffer size < 1");
        }
        this.inf = inflater;
        this.buf = new byte[i];
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            try {
                finish();
            } finally {
                this.out.close();
                this.closed = true;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        int iInflate;
        ensureOpen();
        if (!this.inf.finished()) {
            while (!this.inf.finished() && !this.inf.needsInput() && (iInflate = this.inf.inflate(this.buf, 0, this.buf.length)) >= 1) {
                try {
                    this.out.write(this.buf, 0, iInflate);
                } catch (DataFormatException e) {
                    String message = e.getMessage();
                    if (message == null) {
                        message = "Invalid ZLIB data format";
                    }
                    throw new ZipException(message);
                }
            }
            super.flush();
        }
    }

    public void finish() throws IOException {
        ensureOpen();
        flush();
        if (this.usesDefaultInflater) {
            this.inf.end();
        }
    }

    @Override
    public void write(int i) throws IOException {
        this.wbuf[0] = (byte) i;
        write(this.wbuf, 0, 1);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        int iInflate;
        ensureOpen();
        if (bArr == null) {
            throw new NullPointerException("Null buffer for read");
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return;
        }
        do {
            try {
                if (this.inf.needsInput()) {
                    if (i2 >= 1) {
                        int i3 = 512;
                        if (i2 < 512) {
                            i3 = i2;
                        }
                        this.inf.setInput(bArr, i, i3);
                        i += i3;
                        i2 -= i3;
                    } else {
                        return;
                    }
                }
                do {
                    iInflate = this.inf.inflate(this.buf, 0, this.buf.length);
                    if (iInflate > 0) {
                        this.out.write(this.buf, 0, iInflate);
                    }
                } while (iInflate > 0);
                if (this.inf.finished()) {
                    return;
                }
            } catch (DataFormatException e) {
                String message = e.getMessage();
                if (message == null) {
                    message = "Invalid ZLIB data format";
                }
                throw new ZipException(message);
            }
        } while (!this.inf.needsDictionary());
        throw new ZipException("ZLIB dictionary missing");
    }
}
