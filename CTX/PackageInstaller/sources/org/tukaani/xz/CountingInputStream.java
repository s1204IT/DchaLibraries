package org.tukaani.xz;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class CountingInputStream extends FilterInputStream {
    private long size;

    public CountingInputStream(InputStream inputStream) {
        super(inputStream);
        this.size = 0L;
    }

    @Override
    public int read() throws IOException {
        int i = this.in.read();
        if (i != -1 && this.size >= 0) {
            this.size++;
        }
        return i;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3 = this.in.read(bArr, i, i2);
        if (i3 > 0 && this.size >= 0) {
            this.size += (long) i3;
        }
        return i3;
    }

    public long getSize() {
        return this.size;
    }
}
