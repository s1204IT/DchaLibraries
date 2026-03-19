package org.tukaani.xz;

import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.simple.SimpleFilter;

class SimpleInputStream extends InputStream {
    static final boolean $assertionsDisabled = false;
    private InputStream in;
    private final SimpleFilter simpleFilter;
    private final byte[] filterBuf = new byte[4096];
    private int pos = 0;
    private int filtered = 0;
    private int unfiltered = 0;
    private boolean endReached = false;
    private IOException exception = null;
    private final byte[] tempBuf = new byte[1];

    static int getMemoryUsage() {
        return 5;
    }

    SimpleInputStream(InputStream inputStream, SimpleFilter simpleFilter) {
        if (inputStream == null) {
            throw new NullPointerException();
        }
        this.in = inputStream;
        this.simpleFilter = simpleFilter;
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
        int i3;
        if (i < 0 || i2 < 0 || (i3 = i + i2) < 0 || i3 > bArr.length) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        if (this.exception != null) {
            throw this.exception;
        }
        int i4 = 0;
        while (true) {
            try {
                int iMin = Math.min(this.filtered, i2);
                System.arraycopy(this.filterBuf, this.pos, bArr, i, iMin);
                this.pos += iMin;
                this.filtered -= iMin;
                i += iMin;
                i2 -= iMin;
                i4 += iMin;
                if (this.pos + this.filtered + this.unfiltered == 4096) {
                    System.arraycopy(this.filterBuf, this.pos, this.filterBuf, 0, this.filtered + this.unfiltered);
                    this.pos = 0;
                }
                if (i2 == 0 || this.endReached) {
                    break;
                }
                int i5 = this.in.read(this.filterBuf, this.pos + this.filtered + this.unfiltered, 4096 - ((this.pos + this.filtered) + this.unfiltered));
                if (i5 == -1) {
                    this.endReached = true;
                    this.filtered = this.unfiltered;
                    this.unfiltered = 0;
                } else {
                    this.unfiltered += i5;
                    this.filtered = this.simpleFilter.code(this.filterBuf, this.pos, this.unfiltered);
                    this.unfiltered -= this.filtered;
                }
            } catch (IOException e) {
                this.exception = e;
                throw e;
            }
        }
        if (i4 > 0) {
            return i4;
        }
        return -1;
    }

    @Override
    public int available() throws IOException {
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        if (this.exception != null) {
            throw this.exception;
        }
        return this.filtered;
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
