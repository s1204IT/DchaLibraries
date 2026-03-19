package com.coremedia.iso;

import com.googlecode.mp4parser.util.CastUtils;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class ChannelHelper {
    static final boolean $assertionsDisabled = false;

    public static ByteBuffer readFully(ReadableByteChannel readableByteChannel, long j) throws IOException {
        if ((readableByteChannel instanceof FileChannel) && j > 1048576) {
            MappedByteBuffer map = readableByteChannel.map(FileChannel.MapMode.READ_ONLY, readableByteChannel.position(), j);
            readableByteChannel.position(readableByteChannel.position() + j);
            return map;
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(CastUtils.l2i(j));
        readFully(readableByteChannel, byteBufferAllocate, byteBufferAllocate.limit());
        byteBufferAllocate.rewind();
        return byteBufferAllocate;
    }

    public static int readFully(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer, int i) throws IOException {
        int i2;
        int i3 = 0;
        do {
            i2 = readableByteChannel.read(byteBuffer);
            if (-1 == i2) {
                break;
            }
            i3 += i2;
        } while (i3 != i);
        if (i2 == -1) {
            throw new EOFException("End of file. No more boxes.");
        }
        return i3;
    }
}
