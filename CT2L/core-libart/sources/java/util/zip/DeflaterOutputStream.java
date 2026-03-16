package java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import libcore.io.Streams;

public class DeflaterOutputStream extends FilterOutputStream {
    static final int BUF_SIZE = 512;
    protected byte[] buf;
    protected Deflater def;
    boolean done;
    private final boolean syncFlush;

    public DeflaterOutputStream(OutputStream os) {
        this(os, new Deflater(), 512, false);
    }

    public DeflaterOutputStream(OutputStream os, Deflater def) {
        this(os, def, 512, false);
    }

    public DeflaterOutputStream(OutputStream os, Deflater def, int bufferSize) {
        this(os, def, bufferSize, false);
    }

    public DeflaterOutputStream(OutputStream os, boolean syncFlush) {
        this(os, new Deflater(), 512, syncFlush);
    }

    public DeflaterOutputStream(OutputStream os, Deflater def, boolean syncFlush) {
        this(os, def, 512, syncFlush);
    }

    public DeflaterOutputStream(OutputStream os, Deflater def, int bufferSize, boolean syncFlush) {
        super(os);
        this.done = false;
        if (os == null) {
            throw new NullPointerException("os == null");
        }
        if (def == null) {
            throw new NullPointerException("def == null");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize <= 0: " + bufferSize);
        }
        this.def = def;
        this.syncFlush = syncFlush;
        this.buf = new byte[bufferSize];
    }

    protected void deflate() throws IOException {
        while (true) {
            int byteCount = this.def.deflate(this.buf);
            if (byteCount != 0) {
                this.out.write(this.buf, 0, byteCount);
            } else {
                return;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!this.def.finished()) {
            finish();
        }
        this.def.end();
        this.out.close();
    }

    public void finish() throws IOException {
        if (!this.done) {
            this.def.finish();
            while (!this.def.finished()) {
                int byteCount = this.def.deflate(this.buf);
                this.out.write(this.buf, 0, byteCount);
            }
            this.done = true;
        }
    }

    @Override
    public void write(int i) throws IOException {
        Streams.writeSingleByte(this, i);
    }

    @Override
    public void write(byte[] buffer, int offset, int byteCount) throws IOException {
        if (this.done) {
            throw new IOException("attempt to write after finish");
        }
        Arrays.checkOffsetAndCount(buffer.length, offset, byteCount);
        if (!this.def.needsInput()) {
            throw new IOException();
        }
        this.def.setInput(buffer, offset, byteCount);
        deflate();
    }

    @Override
    public void flush() throws IOException {
        if (this.syncFlush && !this.done) {
            while (true) {
                int byteCount = this.def.deflate(this.buf, 0, this.buf.length, 2);
                if (byteCount == 0) {
                    break;
                } else {
                    this.out.write(this.buf, 0, byteCount);
                }
            }
        }
        this.out.flush();
    }
}
