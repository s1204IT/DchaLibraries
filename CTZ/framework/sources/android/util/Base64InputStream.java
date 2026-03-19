package android.util;

import android.util.Base64;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Base64InputStream extends FilterInputStream {
    private static final int BUFFER_SIZE = 2048;
    private static byte[] EMPTY = new byte[0];
    private final Base64.Coder coder;
    private boolean eof;
    private byte[] inputBuffer;
    private int outputEnd;
    private int outputStart;

    public Base64InputStream(InputStream inputStream, int i) {
        this(inputStream, i, false);
    }

    public Base64InputStream(InputStream inputStream, int i, boolean z) {
        super(inputStream);
        this.eof = false;
        this.inputBuffer = new byte[2048];
        if (z) {
            this.coder = new Base64.Encoder(i, null);
        } else {
            this.coder = new Base64.Decoder(i, null);
        }
        this.coder.output = new byte[this.coder.maxOutputSize(2048)];
        this.outputStart = 0;
        this.outputEnd = 0;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
        this.inputBuffer = null;
    }

    @Override
    public int available() {
        return this.outputEnd - this.outputStart;
    }

    @Override
    public long skip(long j) throws IOException {
        if (this.outputStart >= this.outputEnd) {
            refill();
        }
        if (this.outputStart >= this.outputEnd) {
            return 0L;
        }
        long jMin = Math.min(j, this.outputEnd - this.outputStart);
        this.outputStart = (int) (((long) this.outputStart) + jMin);
        return jMin;
    }

    @Override
    public int read() throws IOException {
        if (this.outputStart >= this.outputEnd) {
            refill();
        }
        if (this.outputStart >= this.outputEnd) {
            return -1;
        }
        byte[] bArr = this.coder.output;
        int i = this.outputStart;
        this.outputStart = i + 1;
        return bArr[i] & 255;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.outputStart >= this.outputEnd) {
            refill();
        }
        if (this.outputStart >= this.outputEnd) {
            return -1;
        }
        int iMin = Math.min(i2, this.outputEnd - this.outputStart);
        System.arraycopy(this.coder.output, this.outputStart, bArr, i, iMin);
        this.outputStart += iMin;
        return iMin;
    }

    private void refill() throws IOException {
        boolean zProcess;
        if (this.eof) {
            return;
        }
        int i = this.in.read(this.inputBuffer);
        if (i == -1) {
            this.eof = true;
            zProcess = this.coder.process(EMPTY, 0, 0, true);
        } else {
            zProcess = this.coder.process(this.inputBuffer, 0, i, false);
        }
        if (!zProcess) {
            throw new Base64DataException("bad base-64");
        }
        this.outputEnd = this.coder.op;
        this.outputStart = 0;
    }
}
