package sun.nio.fs;

import dalvik.system.CloseGuard;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class UnixDirectoryStream implements DirectoryStream<Path> {
    private final UnixPath dir;
    private final long dp;
    private final DirectoryStream.Filter<? super Path> filter;
    private volatile boolean isClosed;
    private Iterator<Path> iterator;
    private final ReentrantReadWriteLock streamLock = new ReentrantReadWriteLock(true);
    private final CloseGuard guard = CloseGuard.get();

    UnixDirectoryStream(UnixPath unixPath, long j, DirectoryStream.Filter<? super Path> filter) {
        this.dir = unixPath;
        this.dp = j;
        this.filter = filter;
        this.guard.open("close");
    }

    protected final UnixPath directory() {
        return this.dir;
    }

    protected final Lock readLock() {
        return this.streamLock.readLock();
    }

    protected final Lock writeLock() {
        return this.streamLock.writeLock();
    }

    protected final boolean isOpen() {
        return !this.isClosed;
    }

    protected final boolean closeImpl() throws IOException {
        if (!this.isClosed) {
            this.isClosed = true;
            try {
                UnixNativeDispatcher.closedir(this.dp);
                this.guard.close();
                return true;
            } catch (UnixException e) {
                throw new IOException(e.errorString());
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        writeLock().lock();
        try {
            closeImpl();
        } finally {
            writeLock().unlock();
        }
    }

    protected final Iterator<Path> iterator(DirectoryStream<Path> directoryStream) {
        Iterator<Path> it;
        if (this.isClosed) {
            throw new IllegalStateException("Directory stream is closed");
        }
        synchronized (this) {
            if (this.iterator != null) {
                throw new IllegalStateException("Iterator already obtained");
            }
            this.iterator = new UnixDirectoryIterator(directoryStream);
            it = this.iterator;
        }
        return it;
    }

    @Override
    public Iterator<Path> iterator() {
        return iterator(this);
    }

    private class UnixDirectoryIterator implements Iterator<Path> {
        static final boolean $assertionsDisabled = false;
        private boolean atEof = false;
        private Path nextEntry;
        private final DirectoryStream<Path> stream;

        UnixDirectoryIterator(DirectoryStream<Path> directoryStream) {
            this.stream = directoryStream;
        }

        private boolean isSelfOrParent(byte[] bArr) {
            return bArr[0] == 46 && (bArr.length == 1 || (bArr.length == 2 && bArr[1] == 46));
        }

        private Path readNextEntry() {
            byte[] bArr;
            UnixPath unixPathResolve;
            while (true) {
                UnixDirectoryStream.this.readLock().lock();
                try {
                    try {
                        if (UnixDirectoryStream.this.isOpen()) {
                            bArr = UnixNativeDispatcher.readdir(UnixDirectoryStream.this.dp);
                        } else {
                            bArr = null;
                        }
                        if (bArr == null) {
                            this.atEof = true;
                            return null;
                        }
                        if (!isSelfOrParent(bArr)) {
                            unixPathResolve = UnixDirectoryStream.this.dir.resolve(bArr);
                            try {
                                if (UnixDirectoryStream.this.filter == null || UnixDirectoryStream.this.filter.accept(unixPathResolve)) {
                                    break;
                                }
                            } catch (IOException e) {
                                throw new DirectoryIteratorException(e);
                            }
                        }
                    } catch (UnixException e2) {
                        throw new DirectoryIteratorException(e2.asIOException(UnixDirectoryStream.this.dir));
                    }
                } finally {
                    UnixDirectoryStream.this.readLock().unlock();
                }
            }
            return unixPathResolve;
        }

        @Override
        public synchronized boolean hasNext() {
            if (this.nextEntry == null && !this.atEof) {
                this.nextEntry = readNextEntry();
            }
            return this.nextEntry != null;
        }

        @Override
        public synchronized Path next() {
            Path nextEntry;
            if (this.nextEntry == null && !this.atEof) {
                nextEntry = readNextEntry();
            } else {
                nextEntry = this.nextEntry;
                this.nextEntry = null;
            }
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }
            return nextEntry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        close();
    }
}
