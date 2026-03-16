package java.nio;

import android.system.ErrnoException;
import java.io.FileDescriptor;
import java.io.IOException;
import libcore.io.Libcore;

final class IoVec {
    private final int bufferCount;
    private final ByteBuffer[] byteBuffers;
    private final int[] byteCounts;
    private final Direction direction;
    private final Object[] ioBuffers;
    private final int offset;
    private final int[] offsets;

    enum Direction {
        READV,
        WRITEV
    }

    IoVec(ByteBuffer[] byteBuffers, int offset, int bufferCount, Direction direction) {
        this.byteBuffers = byteBuffers;
        this.offset = offset;
        this.bufferCount = bufferCount;
        this.direction = direction;
        this.ioBuffers = new Object[bufferCount];
        this.offsets = new int[bufferCount];
        this.byteCounts = new int[bufferCount];
    }

    int init() {
        int totalRemaining = 0;
        for (int i = 0; i < this.bufferCount; i++) {
            ByteBuffer b = this.byteBuffers[this.offset + i];
            if (this.direction == Direction.READV) {
                b.checkWritable();
            }
            int remaining = b.remaining();
            if (b.isDirect()) {
                this.ioBuffers[i] = b;
                this.offsets[i] = b.position();
            } else {
                this.ioBuffers[i] = NioUtils.unsafeArray(b);
                this.offsets[i] = NioUtils.unsafeArrayOffset(b) + b.position();
            }
            this.byteCounts[i] = remaining;
            totalRemaining += remaining;
        }
        return totalRemaining;
    }

    int doTransfer(FileDescriptor fd) throws IOException {
        try {
            if (this.direction != Direction.READV) {
                return Libcore.os.writev(fd, this.ioBuffers, this.offsets, this.byteCounts);
            }
            int result = Libcore.os.readv(fd, this.ioBuffers, this.offsets, this.byteCounts);
            if (result == 0) {
                return -1;
            }
            return result;
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    void didTransfer(int byteCount) {
        for (int i = 0; byteCount > 0 && i < this.bufferCount; i++) {
            ByteBuffer b = this.byteBuffers[this.offset + i];
            if (this.byteCounts[i] < byteCount) {
                b.position(b.limit());
                byteCount -= this.byteCounts[i];
            } else {
                b.position((this.direction == Direction.WRITEV ? b.position() : 0) + byteCount);
                byteCount = 0;
            }
        }
    }
}
