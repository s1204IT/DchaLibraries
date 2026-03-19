package java.util.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DeflaterInputStream extends FilterInputStream {
    protected final byte[] buf;
    protected final Deflater def;
    private byte[] rbuf;
    private boolean reachEOF;
    private boolean usesDefaultDeflater;

    private void ensureOpen() throws IOException {
        if (this.in == null) {
            throw new IOException("Stream closed");
        }
    }

    public DeflaterInputStream(InputStream inputStream) {
        this(inputStream, new Deflater());
        this.usesDefaultDeflater = true;
    }

    public DeflaterInputStream(InputStream inputStream, Deflater deflater) {
        this(inputStream, deflater, 512);
    }

    public DeflaterInputStream(InputStream inputStream, Deflater deflater, int i) {
        super(inputStream);
        this.rbuf = new byte[1];
        this.usesDefaultDeflater = false;
        this.reachEOF = false;
        if (inputStream == null) {
            throw new NullPointerException("Null input");
        }
        if (deflater == null) {
            throw new NullPointerException("Null deflater");
        }
        if (i < 1) {
            throw new IllegalArgumentException("Buffer size < 1");
        }
        this.def = deflater;
        this.buf = new byte[i];
    }

    @Override
    public void close() throws IOException {
        if (this.in != null) {
            try {
                if (this.usesDefaultDeflater) {
                    this.def.end();
                }
                this.in.close();
            } finally {
                this.in = null;
            }
        }
    }

    @Override
    public int read() throws IOException {
        if (read(this.rbuf, 0, 1) <= 0) {
            return -1;
        }
        return this.rbuf[0] & Character.DIRECTIONALITY_UNDEFINED;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        ensureOpen();
        if (bArr == null) {
            throw new NullPointerException("Null buffer for read");
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        int i3 = i;
        int i4 = 0;
        while (i2 > 0 && !this.def.finished()) {
            if (this.def.needsInput()) {
                int i5 = this.in.read(this.buf, 0, this.buf.length);
                if (i5 < 0) {
                    this.def.finish();
                } else if (i5 > 0) {
                    this.def.setInput(this.buf, 0, i5);
                }
            }
            int iDeflate = this.def.deflate(bArr, i3, i2);
            i4 += iDeflate;
            i3 += iDeflate;
            i2 -= iDeflate;
        }
        if (this.def.finished()) {
            this.reachEOF = true;
            if (i4 == 0) {
                return -1;
            }
            return i4;
        }
        return i4;
    }

    @Override
    public long skip(long j) throws IOException {
        long j2 = 0;
        if (j < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        if (this.rbuf.length < 512) {
            this.rbuf = new byte[512];
        }
        int iMin = (int) Math.min(j, 2147483647L);
        while (iMin > 0) {
            int i = read(this.rbuf, 0, iMin <= this.rbuf.length ? iMin : this.rbuf.length);
            if (i < 0) {
                break;
            }
            j2 += (long) i;
            iMin -= i;
        }
        return j2;
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
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int i) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
