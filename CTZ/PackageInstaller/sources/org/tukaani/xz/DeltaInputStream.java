package org.tukaani.xz;

import java.io.IOException;
import java.io.InputStream;

public class DeltaInputStream extends InputStream {
    private final org.tukaani.xz.delta.DeltaDecoder delta;
    private InputStream in;
    private IOException exception = null;
    private final byte[] tempBuf = new byte[1];

    public DeltaInputStream(InputStream inputStream, int i) {
        if (inputStream == null) {
            throw new NullPointerException();
        }
        this.in = inputStream;
        this.delta = new org.tukaani.xz.delta.DeltaDecoder(i);
    }

    @Override
    public int read() throws IOException {
        if (read(this.tempBuf, 0, 1) == -1) {
            return -1;
        }
        return this.tempBuf[0] & 255;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (i2 == 0) {
            return 0;
        }
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        if (this.exception != null) {
            throw this.exception;
        }
        try {
            int i3 = this.in.read(bArr, i, i2);
            if (i3 == -1) {
                return -1;
            }
            this.delta.decode(bArr, i, i3);
            return i3;
        } catch (IOException e) {
            this.exception = e;
            throw e;
        }
    }

    @Override
    public int available() throws IOException {
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        if (this.exception != null) {
            throw this.exception;
        }
        return this.in.available();
    }

    @Override
    public void close() throws IOException {
        if (this.in != null) {
            try {
                this.in.close();
            } finally {
                this.in = null;
            }
        }
    }
}
