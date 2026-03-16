package com.coremedia.iso;

import com.googlecode.mp4parser.util.CastUtils;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class ChannelHelper {
    static final boolean $assertionsDisabled;

    static {
        $assertionsDisabled = !ChannelHelper.class.desiredAssertionStatus();
    }

    public static ByteBuffer readFully(ReadableByteChannel channel, long size) throws IOException {
        if ((channel instanceof FileChannel) && size > 1048576) {
            ByteBuffer bb = ((FileChannel) channel).map(FileChannel.MapMode.READ_ONLY, ((FileChannel) channel).position(), size);
            ((FileChannel) channel).position(((FileChannel) channel).position() + size);
            return bb;
        }
        ByteBuffer buf = ByteBuffer.allocate(CastUtils.l2i(size));
        readFully(channel, buf, buf.limit());
        buf.rewind();
        if ($assertionsDisabled || buf.limit() == size) {
            return buf;
        }
        throw new AssertionError();
    }

    public static int readFully(ReadableByteChannel channel, ByteBuffer buf, int length) throws IOException {
        int n;
        int count = 0;
        do {
            n = channel.read(buf);
            if (-1 == n) {
                break;
            }
            count += n;
        } while (count != length);
        if (n == -1) {
            throw new EOFException("End of file. No more boxes.");
        }
        return count;
    }
}
