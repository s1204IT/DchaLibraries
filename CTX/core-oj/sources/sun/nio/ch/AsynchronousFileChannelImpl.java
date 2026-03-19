package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class AsynchronousFileChannelImpl extends AsynchronousFileChannel {
    protected final ReadWriteLock closeLock = new ReentrantReadWriteLock();
    protected volatile boolean closed;
    protected final ExecutorService executor;
    protected final FileDescriptor fdObj;
    private volatile FileLockTable fileLockTable;
    protected final boolean reading;
    protected final boolean writing;

    abstract <A> Future<FileLock> implLock(long j, long j2, boolean z, A a, CompletionHandler<FileLock, ? super A> completionHandler);

    abstract <A> Future<Integer> implRead(ByteBuffer byteBuffer, long j, A a, CompletionHandler<Integer, ? super A> completionHandler);

    protected abstract void implRelease(FileLockImpl fileLockImpl) throws IOException;

    abstract <A> Future<Integer> implWrite(ByteBuffer byteBuffer, long j, A a, CompletionHandler<Integer, ? super A> completionHandler);

    protected AsynchronousFileChannelImpl(FileDescriptor fileDescriptor, boolean z, boolean z2, ExecutorService executorService) {
        this.fdObj = fileDescriptor;
        this.reading = z;
        this.writing = z2;
        this.executor = executorService;
    }

    final ExecutorService executor() {
        return this.executor;
    }

    @Override
    public final boolean isOpen() {
        return !this.closed;
    }

    protected final void begin() throws IOException {
        this.closeLock.readLock().lock();
        if (this.closed) {
            throw new ClosedChannelException();
        }
    }

    protected final void end() {
        this.closeLock.readLock().unlock();
    }

    protected final void end(boolean z) throws IOException {
        end();
        if (!z && !isOpen()) {
            throw new AsynchronousCloseException();
        }
    }

    @Override
    public final Future<FileLock> lock(long j, long j2, boolean z) {
        return implLock(j, j2, z, null, null);
    }

    @Override
    public final <A> void lock(long j, long j2, boolean z, A a, CompletionHandler<FileLock, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        implLock(j, j2, z, a, completionHandler);
    }

    final void ensureFileLockTableInitialized() throws IOException {
        if (this.fileLockTable == null) {
            synchronized (this) {
                if (this.fileLockTable == null) {
                    this.fileLockTable = FileLockTable.newSharedFileLockTable(this, this.fdObj);
                }
            }
        }
    }

    final void invalidateAllLocks() throws IOException {
        if (this.fileLockTable != null) {
            for (FileLock fileLock : this.fileLockTable.removeAll()) {
                synchronized (fileLock) {
                    if (fileLock.isValid()) {
                        FileLockImpl fileLockImpl = (FileLockImpl) fileLock;
                        implRelease(fileLockImpl);
                        fileLockImpl.invalidate();
                    }
                }
            }
        }
    }

    protected final FileLockImpl addToFileLockTable(long j, long j2, boolean z) {
        try {
            this.closeLock.readLock().lock();
            if (!this.closed) {
                try {
                    ensureFileLockTableInitialized();
                    FileLockImpl fileLockImpl = new FileLockImpl(this, j, j2, z);
                    this.fileLockTable.add(fileLockImpl);
                    return fileLockImpl;
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
            return null;
        } finally {
            end();
        }
    }

    protected final void removeFromFileLockTable(FileLockImpl fileLockImpl) {
        this.fileLockTable.remove(fileLockImpl);
    }

    final void release(FileLockImpl fileLockImpl) throws IOException {
        try {
            begin();
            implRelease(fileLockImpl);
            removeFromFileLockTable(fileLockImpl);
        } finally {
            end();
        }
    }

    @Override
    public final Future<Integer> read(ByteBuffer byteBuffer, long j) {
        return implRead(byteBuffer, j, null, null);
    }

    @Override
    public final <A> void read(ByteBuffer byteBuffer, long j, A a, CompletionHandler<Integer, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        implRead(byteBuffer, j, a, completionHandler);
    }

    @Override
    public final Future<Integer> write(ByteBuffer byteBuffer, long j) {
        return implWrite(byteBuffer, j, null, null);
    }

    @Override
    public final <A> void write(ByteBuffer byteBuffer, long j, A a, CompletionHandler<Integer, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        implWrite(byteBuffer, j, a, completionHandler);
    }
}
