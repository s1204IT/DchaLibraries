package com.android.internal.util;

import java.io.IOException;
import java.io.InputStream;
import libcore.io.Streams;

public class SizedInputStream extends InputStream {
    private long mLength;
    private final InputStream mWrapped;

    public SizedInputStream(InputStream inputStream, long j) {
        this.mWrapped = inputStream;
        this.mLength = j;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.mWrapped.close();
    }

    @Override
    public int read() throws IOException {
        return Streams.readSingleByte(this);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.mLength <= 0) {
            return -1;
        }
        if (i2 > this.mLength) {
            i2 = (int) this.mLength;
        }
        int i3 = this.mWrapped.read(bArr, i, i2);
        if (i3 != -1) {
            this.mLength -= (long) i3;
        } else if (this.mLength > 0) {
            throw new IOException("Unexpected EOF; expected " + this.mLength + " more bytes");
        }
        return i3;
    }
}
