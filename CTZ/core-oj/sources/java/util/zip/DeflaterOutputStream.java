package java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DeflaterOutputStream extends FilterOutputStream {
    protected byte[] buf;
    private boolean closed;
    protected Deflater def;
    private final boolean syncFlush;
    boolean usesDefaultDeflater;

    public DeflaterOutputStream(OutputStream outputStream, Deflater deflater, int i, boolean z) {
        super(outputStream);
        this.closed = false;
        this.usesDefaultDeflater = false;
        if (outputStream == null || deflater == null) {
            throw new NullPointerException();
        }
        if (i <= 0) {
            throw new IllegalArgumentException("buffer size <= 0");
        }
        this.def = deflater;
        this.buf = new byte[i];
        this.syncFlush = z;
    }

    public DeflaterOutputStream(OutputStream outputStream, Deflater deflater, int i) {
        this(outputStream, deflater, i, false);
    }

    public DeflaterOutputStream(OutputStream outputStream, Deflater deflater, boolean z) {
        this(outputStream, deflater, 512, z);
    }

    public DeflaterOutputStream(OutputStream outputStream, Deflater deflater) {
        this(outputStream, deflater, 512, false);
    }

    public DeflaterOutputStream(OutputStream outputStream, boolean z) {
        this(outputStream, new Deflater(), 512, z);
        this.usesDefaultDeflater = true;
    }

    public DeflaterOutputStream(OutputStream outputStream) {
        this(outputStream, false);
        this.usesDefaultDeflater = true;
    }

    @Override
    public void write(int i) throws IOException {
        write(new byte[]{(byte) (i & 255)}, 0, 1);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (this.def.finished()) {
            throw new IOException("write beyond end of stream");
        }
        int i3 = i + i2;
        if ((i | i2 | i3 | (bArr.length - i3)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 != 0 && !this.def.finished()) {
            this.def.setInput(bArr, i, i2);
            while (!this.def.needsInput()) {
                deflate();
            }
        }
    }

    public void finish() throws IOException {
        if (!this.def.finished()) {
            this.def.finish();
            while (!this.def.finished()) {
                deflate();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            finish();
            if (this.usesDefaultDeflater) {
                this.def.end();
            }
            this.out.close();
            this.closed = true;
        }
    }

    protected void deflate() throws IOException {
        while (true) {
            int iDeflate = this.def.deflate(this.buf, 0, this.buf.length);
            if (iDeflate > 0) {
                this.out.write(this.buf, 0, iDeflate);
            } else {
                return;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        int iDeflate;
        if (this.syncFlush && !this.def.finished()) {
            do {
                iDeflate = this.def.deflate(this.buf, 0, this.buf.length, 2);
                if (iDeflate <= 0) {
                    break;
                } else {
                    this.out.write(this.buf, 0, iDeflate);
                }
            } while (iDeflate >= this.buf.length);
        }
        this.out.flush();
    }
}
