package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SimpleAsynchronousFileChannelImpl extends AsynchronousFileChannelImpl {
    private static final FileDispatcher nd = new FileDispatcherImpl();
    private final NativeThreadSet threads;

    private static class DefaultExecutorHolder {
        static final ExecutorService defaultExecutor = ThreadPool.createDefault().executor();

        private DefaultExecutorHolder() {
        }
    }

    SimpleAsynchronousFileChannelImpl(FileDescriptor fileDescriptor, boolean z, boolean z2, ExecutorService executorService) {
        super(fileDescriptor, z, z2, executorService);
        this.threads = new NativeThreadSet(2);
    }

    public static AsynchronousFileChannel open(FileDescriptor fileDescriptor, boolean z, boolean z2, ThreadPool threadPool) {
        return new SimpleAsynchronousFileChannelImpl(fileDescriptor, z, z2, threadPool == null ? DefaultExecutorHolder.defaultExecutor : threadPool.executor());
    }

    @Override
    public void close() throws IOException {
        synchronized (this.fdObj) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            invalidateAllLocks();
            this.threads.signalAndWait();
            this.closeLock.writeLock().lock();
            this.closeLock.writeLock().unlock();
            nd.close(this.fdObj);
        }
    }

    @Override
    public long size() throws IOException {
        Throwable th;
        long j;
        long size;
        int iAdd = this.threads.add();
        try {
            try {
                begin();
                j = 0;
                while (true) {
                    try {
                        size = nd.size(this.fdObj);
                        if (size != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            j = size;
                        } catch (Throwable th2) {
                            th = th2;
                            j = size;
                            end(j >= 0);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                end(size >= 0);
                return size;
            } finally {
                this.threads.remove(iAdd);
            }
        } catch (Throwable th4) {
            th = th4;
            j = 0;
        }
    }

    @Override
    public AsynchronousFileChannel truncate(long j) throws IOException {
        long size;
        if (j < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        if (!this.writing) {
            throw new NonWritableChannelException();
        }
        int iAdd = this.threads.add();
        try {
            try {
                begin();
                long j2 = 0;
                while (true) {
                    try {
                        size = nd.size(this.fdObj);
                        if (size != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            j2 = size;
                        } catch (Throwable th) {
                            th = th;
                            end(size > 0);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        size = j2;
                    }
                }
                if (j < size && isOpen()) {
                    do {
                        size = nd.truncate(this.fdObj, j);
                        if (size != -3) {
                            break;
                        }
                    } while (isOpen());
                }
                end(size > 0);
                return this;
            } catch (Throwable th3) {
                th = th3;
                size = 0;
            }
        } finally {
            this.threads.remove(iAdd);
        }
    }

    @Override
    public void force(boolean z) throws IOException {
        int i;
        int iForce;
        int iAdd = this.threads.add();
        try {
            try {
                begin();
                i = 0;
                while (true) {
                    try {
                        iForce = nd.force(this.fdObj, z);
                        if (iForce != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            } else {
                                i = iForce;
                            }
                        } catch (Throwable th) {
                            th = th;
                            i = iForce;
                            end(i >= 0);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                end(iForce >= 0);
            } catch (Throwable th3) {
                th = th3;
                i = 0;
            }
        } finally {
            this.threads.remove(iAdd);
        }
    }

    @Override
    <A> Future<FileLock> implLock(final long j, final long j2, final boolean z, final A a, final CompletionHandler<FileLock, ? super A> completionHandler) {
        if (z && !this.reading) {
            throw new NonReadableChannelException();
        }
        if (!z && !this.writing) {
            throw new NonWritableChannelException();
        }
        final FileLockImpl fileLockImplAddToFileLockTable = addToFileLockTable(j, j2, z);
        PendingFuture pendingFuture = null;
        if (fileLockImplAddToFileLockTable != null) {
            if (completionHandler == null) {
                pendingFuture = new PendingFuture(this);
            }
            final PendingFuture pendingFuture2 = pendingFuture;
            try {
                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int iLock;
                        int iAdd = SimpleAsynchronousFileChannelImpl.this.threads.add();
                        try {
                            try {
                                SimpleAsynchronousFileChannelImpl.this.begin();
                                do {
                                    iLock = SimpleAsynchronousFileChannelImpl.nd.lock(SimpleAsynchronousFileChannelImpl.this.fdObj, true, j, j2, z);
                                    if (iLock != 2) {
                                        break;
                                    }
                                } while (SimpleAsynchronousFileChannelImpl.this.isOpen());
                                if (iLock != 0 || !SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                    throw new AsynchronousCloseException();
                                }
                                e = null;
                            } catch (IOException e) {
                                e = e;
                                SimpleAsynchronousFileChannelImpl.this.removeFromFileLockTable(fileLockImplAddToFileLockTable);
                                if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                    e = new AsynchronousCloseException();
                                }
                            } finally {
                                SimpleAsynchronousFileChannelImpl.this.end();
                            }
                            SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                            if (completionHandler == null) {
                                pendingFuture2.setResult(fileLockImplAddToFileLockTable, e);
                            } else {
                                Invoker.invokeUnchecked(completionHandler, a, fileLockImplAddToFileLockTable, e);
                            }
                        } catch (Throwable th) {
                            SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                            throw th;
                        }
                    }
                });
                return pendingFuture2;
            } catch (Throwable th) {
                removeFromFileLockTable(fileLockImplAddToFileLockTable);
                throw th;
            }
        }
        ClosedChannelException closedChannelException = new ClosedChannelException();
        if (completionHandler != null) {
            Invoker.invokeIndirectly(completionHandler, a, (Object) null, closedChannelException, this.executor);
            return null;
        }
        return CompletedFuture.withFailure(closedChannelException);
    }

    @Override
    public FileLock tryLock(long j, long j2, boolean z) throws IOException {
        int iLock;
        if (z && !this.reading) {
            throw new NonReadableChannelException();
        }
        if (!z && !this.writing) {
            throw new NonWritableChannelException();
        }
        FileLockImpl fileLockImplAddToFileLockTable = addToFileLockTable(j, j2, z);
        if (fileLockImplAddToFileLockTable == null) {
            throw new ClosedChannelException();
        }
        int iAdd = this.threads.add();
        try {
            begin();
            do {
                iLock = nd.lock(this.fdObj, false, j, j2, z);
                if (iLock != 2) {
                    break;
                }
            } while (isOpen());
            if (iLock != 0 || !isOpen()) {
                if (iLock != -1) {
                    if (iLock == 2) {
                        throw new AsynchronousCloseException();
                    }
                    throw new AssertionError();
                }
                return null;
            }
            end();
            this.threads.remove(iAdd);
            return fileLockImplAddToFileLockTable;
        } finally {
            removeFromFileLockTable(fileLockImplAddToFileLockTable);
            end();
            this.threads.remove(iAdd);
        }
    }

    @Override
    protected void implRelease(FileLockImpl fileLockImpl) throws IOException {
        nd.release(this.fdObj, fileLockImpl.position(), fileLockImpl.size());
    }

    @Override
    <A> Future<Integer> implRead(final ByteBuffer byteBuffer, final long j, final A a, final CompletionHandler<Integer, ? super A> completionHandler) {
        ClosedChannelException closedChannelException;
        if (j < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        if (!this.reading) {
            throw new NonReadableChannelException();
        }
        if (byteBuffer.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        if (!isOpen() || byteBuffer.remaining() == 0) {
            if (!isOpen()) {
                closedChannelException = new ClosedChannelException();
            } else {
                closedChannelException = null;
            }
            if (completionHandler == null) {
                return CompletedFuture.withResult(0, closedChannelException);
            }
            Invoker.invokeIndirectly((CompletionHandler<int, ? super A>) completionHandler, (Object) a, 0, (Throwable) closedChannelException, (Executor) this.executor);
            return null;
        }
        PendingFuture pendingFuture = completionHandler == null ? new PendingFuture(this) : null;
        final PendingFuture pendingFuture2 = pendingFuture;
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                IOException iOException;
                int i;
                int iAdd = SimpleAsynchronousFileChannelImpl.this.threads.add();
                int i2 = 0;
                try {
                    try {
                        SimpleAsynchronousFileChannelImpl.this.begin();
                        while (true) {
                            i = IOUtil.read(SimpleAsynchronousFileChannelImpl.this.fdObj, byteBuffer, j, SimpleAsynchronousFileChannelImpl.nd);
                            if (i != -3) {
                                break;
                            }
                            try {
                                if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                    break;
                                } else {
                                    i2 = i;
                                }
                            } catch (IOException e) {
                                e = e;
                                i2 = i;
                                if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                    e = new AsynchronousCloseException();
                                }
                                SimpleAsynchronousFileChannelImpl.this.end();
                                SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                                iOException = e;
                            }
                        }
                        if (i < 0 && !SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                            throw new AsynchronousCloseException();
                        }
                        SimpleAsynchronousFileChannelImpl.this.end();
                        SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                        iOException = null;
                        i2 = i;
                    } catch (IOException e2) {
                        e = e2;
                    }
                    if (completionHandler == null) {
                        pendingFuture2.setResult(Integer.valueOf(i2), iOException);
                    } else {
                        Invoker.invokeUnchecked(completionHandler, a, Integer.valueOf(i2), iOException);
                    }
                } catch (Throwable th) {
                    SimpleAsynchronousFileChannelImpl.this.end();
                    SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                    throw th;
                }
            }
        });
        return pendingFuture;
    }

    @Override
    <A> Future<Integer> implWrite(final ByteBuffer byteBuffer, final long j, final A a, final CompletionHandler<Integer, ? super A> completionHandler) {
        ClosedChannelException closedChannelException;
        if (j < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        if (!this.writing) {
            throw new NonWritableChannelException();
        }
        if (!isOpen() || byteBuffer.remaining() == 0) {
            if (!isOpen()) {
                closedChannelException = new ClosedChannelException();
            } else {
                closedChannelException = null;
            }
            if (completionHandler == null) {
                return CompletedFuture.withResult(0, closedChannelException);
            }
            Invoker.invokeIndirectly((CompletionHandler<int, ? super A>) completionHandler, (Object) a, 0, (Throwable) closedChannelException, (Executor) this.executor);
            return null;
        }
        PendingFuture pendingFuture = completionHandler == null ? new PendingFuture(this) : null;
        final PendingFuture pendingFuture2 = pendingFuture;
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                IOException iOException;
                int iWrite;
                int iAdd = SimpleAsynchronousFileChannelImpl.this.threads.add();
                int i = 0;
                try {
                    try {
                        SimpleAsynchronousFileChannelImpl.this.begin();
                        while (true) {
                            iWrite = IOUtil.write(SimpleAsynchronousFileChannelImpl.this.fdObj, byteBuffer, j, SimpleAsynchronousFileChannelImpl.nd);
                            if (iWrite != -3) {
                                break;
                            }
                            try {
                                if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                    break;
                                } else {
                                    i = iWrite;
                                }
                            } catch (IOException e) {
                                e = e;
                                i = iWrite;
                                if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                    e = new AsynchronousCloseException();
                                }
                                SimpleAsynchronousFileChannelImpl.this.end();
                                SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                                iOException = e;
                            }
                        }
                        if (iWrite < 0 && !SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                            throw new AsynchronousCloseException();
                        }
                        SimpleAsynchronousFileChannelImpl.this.end();
                        SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                        iOException = null;
                        i = iWrite;
                    } catch (IOException e2) {
                        e = e2;
                    }
                    if (completionHandler == null) {
                        pendingFuture2.setResult(Integer.valueOf(i), iOException);
                    } else {
                        Invoker.invokeUnchecked(completionHandler, a, Integer.valueOf(i), iOException);
                    }
                } catch (Throwable th) {
                    SimpleAsynchronousFileChannelImpl.this.end();
                    SimpleAsynchronousFileChannelImpl.this.threads.remove(iAdd);
                    throw th;
                }
            }
        });
        return pendingFuture;
    }
}
