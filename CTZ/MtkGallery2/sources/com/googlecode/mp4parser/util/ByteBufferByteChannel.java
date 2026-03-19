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
    public int read(ByteBuffer byteBuffer) throws IOException {
        byte[] bArrArray = byteBuffer.array();
        int iRemaining = byteBuffer.remaining();
        if (this.byteBuffer.remaining() >= iRemaining) {
            this.byteBuffer.get(bArrArray, byteBuffer.position(), iRemaining);
            return iRemaining;
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
    public int write(ByteBuffer byteBuffer) throws IOException {
        int iRemaining = byteBuffer.remaining();
        this.byteBuffer.put(byteBuffer);
        return iRemaining;
    }
}
