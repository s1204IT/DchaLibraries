package com.android.camera.exif;

import java.io.InputStream;
import java.nio.ByteBuffer;

class ByteBufferInputStream extends InputStream {
    private ByteBuffer mBuf;

    public ByteBufferInputStream(ByteBuffer buf) {
        this.mBuf = buf;
    }

    @Override
    public int read() {
        if (this.mBuf.hasRemaining()) {
            return this.mBuf.get() & 255;
        }
        return -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) {
        if (!this.mBuf.hasRemaining()) {
            return -1;
        }
        int len2 = Math.min(len, this.mBuf.remaining());
        this.mBuf.get(bytes, off, len2);
        return len2;
    }
}
