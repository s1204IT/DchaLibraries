package com.googlecode.mp4parser.util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public class ByteBufferByteChannel implements ByteChannel {
    ByteBuffer byteBuffer;

    public ByteBufferByteChannel(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        byte[] b = dst.array();
        int r = dst.remaining();
        if (this.byteBuffer.remaining() >= r) {
            this.byteBuffer.get(b, dst.position(), r);
            return r;
        }
        throw new EOFException("Reading beyond end of stream");
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int r = src.remaining();
        this.byteBuffer.put(src);
        return r;
    }
}
