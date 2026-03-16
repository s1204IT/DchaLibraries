package java.nio;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructFlock;
import android.util.MutableLong;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.IoVec;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import libcore.io.Libcore;

final class FileChannelImpl extends FileChannel {
    private static final Comparator<FileLock> LOCK_COMPARATOR = new Comparator<FileLock>() {
        @Override
        public int compare(FileLock lock1, FileLock lock2) {
            long position1 = lock1.position();
            long position2 = lock2.position();
            if (position1 > position2) {
                return 1;
            }
            return position1 < position2 ? -1 : 0;
        }
    };
    private final FileDescriptor fd;
    private final Closeable ioObject;
    private final SortedSet<FileLock> locks = new TreeSet(LOCK_COMPARATOR);
    private final int mode;

    public FileChannelImpl(Closeable ioObject, FileDescriptor fd, int mode) {
        this.fd = fd;
        this.ioObject = ioObject;
        this.mode = mode;
    }

    private void checkOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    private void checkReadable() {
        if ((this.mode & OsConstants.O_ACCMODE) == OsConstants.O_WRONLY) {
            throw new NonReadableChannelException();
        }
    }

    private void checkWritable() {
        if ((this.mode & OsConstants.O_ACCMODE) == OsConstants.O_RDONLY) {
            throw new NonWritableChannelException();
        }
    }

    @Override
    protected void implCloseChannel() throws IOException {
        this.ioObject.close();
    }

    private FileLock basicLock(long position, long size, boolean shared, boolean wait) throws IOException {
        int accessMode = this.mode & OsConstants.O_ACCMODE;
        if (accessMode == OsConstants.O_RDONLY) {
            if (!shared) {
                throw new NonWritableChannelException();
            }
        } else if (accessMode == OsConstants.O_WRONLY && shared) {
            throw new NonReadableChannelException();
        }
        if (position < 0 || size < 0) {
            throw new IllegalArgumentException("position=" + position + " size=" + size);
        }
        FileLock pendingLock = new FileLockImpl(this, position, size, shared);
        addLock(pendingLock);
        StructFlock flock = new StructFlock();
        flock.l_type = (short) (shared ? OsConstants.F_RDLCK : OsConstants.F_WRLCK);
        flock.l_whence = (short) OsConstants.SEEK_SET;
        flock.l_start = position;
        flock.l_len = translateLockLength(size);
        boolean success = false;
        try {
            try {
                success = Libcore.os.fcntlFlock(this.fd, wait ? OsConstants.F_SETLKW64 : OsConstants.F_SETLK64, flock) != -1;
                if (success) {
                    return pendingLock;
                }
                return null;
            } catch (ErrnoException errnoException) {
                throw errnoException.rethrowAsIOException();
            }
        } finally {
            if (!success) {
                removeLock(pendingLock);
            }
        }
    }

    private static long translateLockLength(long byteCount) {
        if (byteCount == Long.MAX_VALUE) {
            return 0L;
        }
        return byteCount;
    }

    private static final class FileLockImpl extends FileLock {
        private boolean isReleased;

        public FileLockImpl(FileChannel channel, long position, long size, boolean shared) {
            super(channel, position, size, shared);
            this.isReleased = false;
        }

        @Override
        public boolean isValid() {
            return !this.isReleased && channel().isOpen();
        }

        @Override
        public void release() throws IOException {
            if (!channel().isOpen()) {
                throw new ClosedChannelException();
            }
            if (!this.isReleased) {
                ((FileChannelImpl) channel()).release(this);
                this.isReleased = true;
            }
        }
    }

    @Override
    public final FileLock lock(long position, long size, boolean shared) throws IOException {
        checkOpen();
        try {
            begin();
            FileLock resultLock = basicLock(position, size, shared, true);
            try {
                end(true);
                return resultLock;
            } catch (ClosedByInterruptException e) {
                throw new FileLockInterruptionException();
            }
        } catch (Throwable th) {
            try {
                end(false);
                throw th;
            } catch (ClosedByInterruptException e2) {
                throw new FileLockInterruptionException();
            }
        }
    }

    @Override
    public final FileLock tryLock(long position, long size, boolean shared) throws IOException {
        checkOpen();
        return basicLock(position, size, shared, false);
    }

    public void release(FileLock lock) throws IOException {
        checkOpen();
        StructFlock flock = new StructFlock();
        flock.l_type = (short) OsConstants.F_UNLCK;
        flock.l_whence = (short) OsConstants.SEEK_SET;
        flock.l_start = lock.position();
        flock.l_len = translateLockLength(lock.size());
        try {
            Libcore.os.fcntlFlock(this.fd, OsConstants.F_SETLKW64, flock);
            removeLock(lock);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    @Override
    public void force(boolean metadata) throws IOException {
        checkOpen();
        if ((this.mode & OsConstants.O_ACCMODE) != OsConstants.O_RDONLY) {
            try {
                if (metadata) {
                    Libcore.os.fsync(this.fd);
                } else {
                    Libcore.os.fdatasync(this.fd);
                }
            } catch (ErrnoException errnoException) {
                throw errnoException.rethrowAsIOException();
            }
        }
    }

    @Override
    public final MappedByteBuffer map(FileChannel.MapMode mapMode, long position, long size) throws IOException {
        checkOpen();
        if (mapMode == null) {
            throw new NullPointerException("mapMode == null");
        }
        if (position < 0 || size < 0 || size > 2147483647L) {
            throw new IllegalArgumentException("position=" + position + " size=" + size);
        }
        int accessMode = this.mode & OsConstants.O_ACCMODE;
        if (accessMode == OsConstants.O_RDONLY) {
            if (mapMode != FileChannel.MapMode.READ_ONLY) {
                throw new NonWritableChannelException();
            }
        } else if (accessMode == OsConstants.O_WRONLY) {
            throw new NonReadableChannelException();
        }
        if (position + size > size()) {
            try {
                Libcore.os.ftruncate(this.fd, position + size);
            } catch (ErrnoException ftruncateException) {
                try {
                    if (OsConstants.S_ISREG(Libcore.os.fstat(this.fd).st_mode) || ftruncateException.errno != OsConstants.EINVAL) {
                        throw ftruncateException.rethrowAsIOException();
                    }
                } catch (ErrnoException fstatException) {
                    throw fstatException.rethrowAsIOException();
                }
            }
        }
        long alignment = position - (position % Libcore.os.sysconf(OsConstants._SC_PAGE_SIZE));
        int offset = (int) (position - alignment);
        MemoryBlock block = MemoryBlock.mmap(this.fd, alignment, size + ((long) offset), mapMode);
        return new DirectByteBuffer(block, (int) size, offset, mapMode == FileChannel.MapMode.READ_ONLY, mapMode);
    }

    @Override
    public long position() throws IOException {
        checkOpen();
        try {
            return Libcore.os.lseek(this.fd, 0L, OsConstants.SEEK_CUR);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        checkOpen();
        if (newPosition < 0) {
            throw new IllegalArgumentException("position: " + newPosition);
        }
        try {
            Libcore.os.lseek(this.fd, newPosition, OsConstants.SEEK_SET);
            return this;
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    @Override
    public int read(ByteBuffer buffer, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("position: " + position);
        }
        return readImpl(buffer, position);
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        return readImpl(buffer, -1L);
    }

    private int readImpl(ByteBuffer buffer, long position) throws IOException {
        buffer.checkWritable();
        checkOpen();
        checkReadable();
        if (!buffer.hasRemaining()) {
            return 0;
        }
        int bytesRead = 0;
        try {
            begin();
            try {
                if (position == -1) {
                    bytesRead = Libcore.os.read(this.fd, buffer);
                } else {
                    bytesRead = Libcore.os.pread(this.fd, buffer, position);
                }
                if (bytesRead == 0) {
                    bytesRead = -1;
                }
            } catch (ErrnoException errnoException) {
                if (errnoException.errno == OsConstants.EAGAIN) {
                    bytesRead = 0;
                } else {
                    throw errnoException.rethrowAsIOException();
                }
            }
            end(1 != 0 && bytesRead >= 0);
            return bytesRead;
        } catch (Throwable th) {
            end(0 != 0 && bytesRead >= 0);
            throw th;
        }
    }

    private int transferIoVec(IoVec ioVec) throws IOException {
        if (ioVec.init() == 0) {
            return 0;
        }
        try {
            begin();
            int bytesTransferred = ioVec.doTransfer(this.fd);
            end(true);
            ioVec.didTransfer(bytesTransferred);
            return bytesTransferred;
        } catch (Throwable th) {
            end(false);
            throw th;
        }
    }

    @Override
    public long read(ByteBuffer[] buffers, int offset, int length) throws IOException {
        Arrays.checkOffsetAndCount(buffers.length, offset, length);
        checkOpen();
        checkReadable();
        return transferIoVec(new IoVec(buffers, offset, length, IoVec.Direction.READV));
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        try {
            return Libcore.os.fstat(this.fd).st_size;
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        checkOpen();
        if (!src.isOpen()) {
            throw new ClosedChannelException();
        }
        checkWritable();
        if (position < 0 || count < 0 || count > 2147483647L) {
            throw new IllegalArgumentException("position=" + position + " count=" + count);
        }
        if (position > size()) {
            return 0L;
        }
        if (src instanceof FileChannel) {
            FileChannel fileSrc = (FileChannel) src;
            long size = fileSrc.size();
            long filePosition = fileSrc.position();
            long count2 = Math.min(count, size - filePosition);
            ByteBuffer buffer = fileSrc.map(FileChannel.MapMode.READ_ONLY, filePosition, count2);
            try {
                fileSrc.position(filePosition + count2);
                return write(buffer, position);
            } finally {
                NioUtils.freeDirectBuffer(buffer);
            }
        }
        ByteBuffer buffer2 = ByteBuffer.allocate((int) count);
        src.read(buffer2);
        buffer2.flip();
        return write(buffer2, position);
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        checkOpen();
        if (!target.isOpen()) {
            throw new ClosedChannelException();
        }
        checkReadable();
        if (target instanceof FileChannelImpl) {
            ((FileChannelImpl) target).checkWritable();
        }
        if (position < 0 || count < 0) {
            throw new IllegalArgumentException("position=" + position + " count=" + count);
        }
        if (count == 0 || position >= size()) {
            return 0L;
        }
        long count2 = Math.min(count, size() - position);
        boolean completed = false;
        if (target instanceof SocketChannelImpl) {
            FileDescriptor outFd = ((SocketChannelImpl) target).getFD();
            try {
                begin();
                MutableLong offset = new MutableLong(position);
                completed = true;
                return Libcore.os.sendfile(outFd, this.fd, offset, count2);
            } catch (ErrnoException errnoException) {
                if (errnoException.errno != OsConstants.ENOSYS && errnoException.errno != OsConstants.EINVAL) {
                    throw errnoException.rethrowAsIOException();
                }
            } finally {
                end(completed);
            }
        }
        ByteBuffer buffer = null;
        try {
            buffer = map(FileChannel.MapMode.READ_ONLY, position, count2);
            return target.write(buffer);
        } finally {
            NioUtils.freeDirectBuffer(buffer);
        }
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        checkOpen();
        if (size < 0) {
            throw new IllegalArgumentException("size < 0: " + size);
        }
        checkWritable();
        if (size < size()) {
            try {
                Libcore.os.ftruncate(this.fd, size);
            } catch (ErrnoException errnoException) {
                throw errnoException.rethrowAsIOException();
            }
        }
        if (position() > size) {
            position(size);
        }
        return this;
    }

    @Override
    public int write(ByteBuffer buffer, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("position < 0: " + position);
        }
        return writeImpl(buffer, position);
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        return writeImpl(buffer, -1L);
    }

    private int writeImpl(ByteBuffer buffer, long position) throws IOException {
        int bytesWritten;
        checkOpen();
        checkWritable();
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        if (!buffer.hasRemaining()) {
            return 0;
        }
        boolean completed = false;
        try {
            begin();
            try {
                if (position == -1) {
                    bytesWritten = Libcore.os.write(this.fd, buffer);
                } else {
                    bytesWritten = Libcore.os.pwrite(this.fd, buffer, position);
                }
                completed = true;
                return bytesWritten;
            } catch (ErrnoException errnoException) {
                throw errnoException.rethrowAsIOException();
            }
        } finally {
            end(completed);
        }
    }

    @Override
    public long write(ByteBuffer[] buffers, int offset, int length) throws IOException {
        Arrays.checkOffsetAndCount(buffers.length, offset, length);
        checkOpen();
        checkWritable();
        return transferIoVec(new IoVec(buffers, offset, length, IoVec.Direction.WRITEV));
    }

    static int calculateTotalRemaining(ByteBuffer[] buffers, int offset, int length, boolean copyingIn) {
        int count = 0;
        for (int i = offset; i < offset + length; i++) {
            count += buffers[i].remaining();
            if (copyingIn) {
                buffers[i].checkWritable();
            }
        }
        return count;
    }

    public FileDescriptor getFD() {
        return this.fd;
    }

    private synchronized void addLock(FileLock lock) throws OverlappingFileLockException {
        long lockEnd = lock.position() + lock.size();
        for (FileLock existingLock : this.locks) {
            if (existingLock.position() > lockEnd) {
                break;
            } else if (existingLock.overlaps(lock.position(), lock.size())) {
                throw new OverlappingFileLockException();
            }
        }
        this.locks.add(lock);
    }

    private synchronized void removeLock(FileLock lock) {
        this.locks.remove(lock);
    }
}
