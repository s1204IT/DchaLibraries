package sun.nio.ch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;

public class ChannelInputStream extends InputStream {
    protected final ReadableByteChannel ch;
    private ByteBuffer bb = null;
    private byte[] bs = null;
    private byte[] b1 = null;

    public static int read(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer) throws IOException {
        int i;
        if (readableByteChannel instanceof SelectableChannel) {
            SelectableChannel selectableChannel = (SelectableChannel) readableByteChannel;
            synchronized (selectableChannel.blockingLock()) {
                if (!selectableChannel.isBlocking()) {
                    throw new IllegalBlockingModeException();
                }
                i = readableByteChannel.read(byteBuffer);
            }
            return i;
        }
        return readableByteChannel.read(byteBuffer);
    }

    public ChannelInputStream(ReadableByteChannel readableByteChannel) {
        this.ch = readableByteChannel;
    }

    @Override
    public synchronized int read() throws IOException {
        if (this.b1 == null) {
            this.b1 = new byte[1];
        }
        if (read(this.b1) == 1) {
            return this.b1[0] & Character.DIRECTIONALITY_UNDEFINED;
        }
        return -1;
    }

    @Override
    public synchronized int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        ByteBuffer byteBufferWrap;
        if (i >= 0) {
            if (i <= bArr.length && i2 >= 0 && (i3 = i + i2) <= bArr.length && i3 >= 0) {
                if (i2 == 0) {
                    return 0;
                }
                if (this.bs == bArr) {
                    byteBufferWrap = this.bb;
                } else {
                    byteBufferWrap = ByteBuffer.wrap(bArr);
                }
                byteBufferWrap.limit(Math.min(i3, byteBufferWrap.capacity()));
                byteBufferWrap.position(i);
                this.bb = byteBufferWrap;
                this.bs = bArr;
                return read(byteBufferWrap);
            }
        }
        throw new IndexOutOfBoundsException();
    }

    protected int read(ByteBuffer byteBuffer) throws IOException {
        return read(this.ch, byteBuffer);
    }

    @Override
    public int available() throws IOException {
        if (this.ch instanceof SeekableByteChannel) {
            SeekableByteChannel seekableByteChannel = (SeekableByteChannel) this.ch;
            long jMax = Math.max(0L, seekableByteChannel.size() - seekableByteChannel.position());
            return jMax > 2147483647L ? Integer.MAX_VALUE : (int) jMax;
        }
        return 0;
    }

    @Override
    public void close() throws IOException {
        this.ch.close();
    }
}
