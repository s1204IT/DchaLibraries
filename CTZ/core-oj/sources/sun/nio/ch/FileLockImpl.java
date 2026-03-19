package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileLockImpl extends FileLock {
    static final boolean $assertionsDisabled = false;
    private volatile boolean valid;

    FileLockImpl(FileChannel fileChannel, long j, long j2, boolean z) {
        super(fileChannel, j, j2, z);
        this.valid = true;
    }

    FileLockImpl(AsynchronousFileChannel asynchronousFileChannel, long j, long j2, boolean z) {
        super(asynchronousFileChannel, j, j2, z);
        this.valid = true;
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    void invalidate() {
        this.valid = false;
    }

    @Override
    public synchronized void release() throws IOException {
        Channel channelAcquiredBy = acquiredBy();
        if (!channelAcquiredBy.isOpen()) {
            throw new ClosedChannelException();
        }
        if (this.valid) {
            if (channelAcquiredBy instanceof FileChannelImpl) {
                ((FileChannelImpl) channelAcquiredBy).release(this);
            } else if (channelAcquiredBy instanceof AsynchronousFileChannelImpl) {
                ((AsynchronousFileChannelImpl) channelAcquiredBy).release(this);
            } else {
                throw new AssertionError();
            }
            this.valid = false;
        }
    }
}
