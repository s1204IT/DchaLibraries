package sun.nio.ch;

import android.system.ErrnoException;
import android.system.OsConstants;
import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DirectByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.Libcore;
import sun.misc.Cleaner;
import sun.security.action.GetPropertyAction;

public class FileChannelImpl extends FileChannel {
    static final boolean $assertionsDisabled = false;
    private static final long MAPPED_TRANSFER_SIZE = 8388608;
    private static final int MAP_PV = 2;
    private static final int MAP_RO = 0;
    private static final int MAP_RW = 1;
    private static final int TRANSFER_SIZE = 8192;
    private static boolean isSharedFileLockTable;
    private static volatile boolean propertyChecked;
    private final boolean append;

    @ReachabilitySensitive
    public final FileDescriptor fd;
    private volatile FileLockTable fileLockTable;
    private final FileDispatcher nd;
    private final Object parent;
    private final String path;
    private final boolean readable;
    private final boolean writable;
    private static volatile boolean transferSupported = true;
    private static volatile boolean pipeSupported = true;
    private static volatile boolean fileSupported = true;
    private static final long allocationGranularity = initIDs();
    private final NativeThreadSet threads = new NativeThreadSet(2);
    private final Object positionLock = new Object();

    @ReachabilitySensitive
    private final CloseGuard guard = CloseGuard.get();

    private static native long initIDs();

    private native long map0(int i, long j, long j2) throws IOException;

    private native long position0(FileDescriptor fileDescriptor, long j);

    private native long transferTo0(FileDescriptor fileDescriptor, long j, long j2, FileDescriptor fileDescriptor2);

    private static native int unmap0(long j, long j2);

    private FileChannelImpl(FileDescriptor fileDescriptor, String str, boolean z, boolean z2, boolean z3, Object obj) {
        this.fd = fileDescriptor;
        this.readable = z;
        this.writable = z2;
        this.append = z3;
        this.parent = obj;
        this.path = str;
        this.nd = new FileDispatcherImpl(z3);
        if (fileDescriptor != null && fileDescriptor.valid()) {
            this.guard.open("close");
        }
    }

    public static FileChannel open(FileDescriptor fileDescriptor, String str, boolean z, boolean z2, Object obj) {
        return new FileChannelImpl(fileDescriptor, str, z, z2, $assertionsDisabled, obj);
    }

    public static FileChannel open(FileDescriptor fileDescriptor, String str, boolean z, boolean z2, boolean z3, Object obj) {
        return new FileChannelImpl(fileDescriptor, str, z, z2, z3, obj);
    }

    private void ensureOpen() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    @Override
    protected void implCloseChannel() throws IOException {
        this.guard.close();
        if (this.fileLockTable != null) {
            for (FileLock fileLock : this.fileLockTable.removeAll()) {
                synchronized (fileLock) {
                    if (fileLock.isValid()) {
                        this.nd.release(this.fd, fileLock.position(), fileLock.size());
                        ((FileLockImpl) fileLock).invalidate();
                    }
                }
            }
        }
        this.threads.signalAndWait();
        if (this.parent != null) {
            ((Closeable) this.parent).close();
        } else {
            this.nd.close(this.fd);
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        int iAdd;
        int i;
        int i2;
        ensureOpen();
        if (!this.readable) {
            throw new NonReadableChannelException();
        }
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
                try {
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return 0;
                    }
                    i = 0;
                    while (true) {
                        try {
                            i2 = IOUtil.read(this.fd, byteBuffer, -1L, this.nd);
                            if (i2 != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                i = i2;
                            } catch (Throwable th) {
                                th = th;
                                i = i2;
                                this.threads.remove(iAdd);
                                if (i > 0) {
                                }
                                end(z);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    int iNormalize = IOStatus.normalize(i2);
                    this.threads.remove(iAdd);
                    if (i2 <= 0) {
                        z = false;
                    }
                    end(z);
                    return iNormalize;
                } catch (Throwable th3) {
                    th = th3;
                    i = 0;
                    this.threads.remove(iAdd);
                    if (i > 0) {
                        z = false;
                    }
                    end(z);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                iAdd = -1;
            }
        }
    }

    @Override
    public long read(ByteBuffer[] byteBufferArr, int i, int i2) throws IOException {
        int iAdd;
        long j;
        long j2;
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        if (!this.readable) {
            throw new NonReadableChannelException();
        }
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
                try {
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return 0L;
                    }
                    j = 0;
                    while (true) {
                        try {
                            j2 = IOUtil.read(this.fd, byteBufferArr, i, i2, this.nd);
                            if (j2 != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                j = j2;
                            } catch (Throwable th) {
                                th = th;
                                j = j2;
                                this.threads.remove(iAdd);
                                if (j > 0) {
                                }
                                end(z);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    long jNormalize = IOStatus.normalize(j2);
                    this.threads.remove(iAdd);
                    if (j2 <= 0) {
                        z = false;
                    }
                    end(z);
                    return jNormalize;
                } catch (Throwable th3) {
                    th = th3;
                    j = 0;
                    this.threads.remove(iAdd);
                    if (j > 0) {
                        z = false;
                    }
                    end(z);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                iAdd = -1;
            }
        }
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        int iAdd;
        int i;
        int iWrite;
        ensureOpen();
        if (!this.writable) {
            throw new NonWritableChannelException();
        }
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
                try {
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return 0;
                    }
                    i = 0;
                    while (true) {
                        try {
                            iWrite = IOUtil.write(this.fd, byteBuffer, -1L, this.nd);
                            if (iWrite != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                i = iWrite;
                            } catch (Throwable th) {
                                th = th;
                                i = iWrite;
                                this.threads.remove(iAdd);
                                if (i > 0) {
                                }
                                end(z);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    int iNormalize = IOStatus.normalize(iWrite);
                    this.threads.remove(iAdd);
                    if (iWrite <= 0) {
                        z = false;
                    }
                    end(z);
                    return iNormalize;
                } catch (Throwable th3) {
                    th = th3;
                    i = 0;
                    this.threads.remove(iAdd);
                    if (i > 0) {
                        z = false;
                    }
                    end(z);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                iAdd = -1;
            }
        }
    }

    @Override
    public long write(ByteBuffer[] byteBufferArr, int i, int i2) throws IOException {
        int iAdd;
        long j;
        long jWrite;
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        if (!this.writable) {
            throw new NonWritableChannelException();
        }
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
                try {
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return 0L;
                    }
                    j = 0;
                    while (true) {
                        try {
                            jWrite = IOUtil.write(this.fd, byteBufferArr, i, i2, this.nd);
                            if (jWrite != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                j = jWrite;
                            } catch (Throwable th) {
                                th = th;
                                j = jWrite;
                                this.threads.remove(iAdd);
                                if (j > 0) {
                                }
                                end(z);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    long jNormalize = IOStatus.normalize(jWrite);
                    this.threads.remove(iAdd);
                    if (jWrite <= 0) {
                        z = false;
                    }
                    end(z);
                    return jNormalize;
                } catch (Throwable th3) {
                    th = th3;
                    j = 0;
                    this.threads.remove(iAdd);
                    if (j > 0) {
                        z = false;
                    }
                    end(z);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                iAdd = -1;
            }
        }
    }

    @Override
    public long position() throws IOException {
        long size;
        int iAdd;
        Throwable th;
        ensureOpen();
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
                try {
                    if (isOpen()) {
                        if (this.append) {
                            BlockGuard.getThreadPolicy().onWriteToDisk();
                        }
                        size = -1;
                        do {
                            try {
                                size = this.append ? this.nd.size(this.fd) : position0(this.fd, -1L);
                                if (size != -3) {
                                    break;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } while (isOpen());
                        long jNormalize = IOStatus.normalize(size);
                        this.threads.remove(iAdd);
                        if (size <= -1) {
                            z = false;
                        }
                        end(z);
                        return jNormalize;
                    }
                    this.threads.remove(iAdd);
                    end($assertionsDisabled);
                    return 0L;
                } catch (Throwable th3) {
                    th = th3;
                    size = -1;
                }
            } catch (Throwable th4) {
                size = -1;
                iAdd = -1;
                th = th4;
            }
            this.threads.remove(iAdd);
            if (size <= -1) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    public FileChannel position(long j) throws IOException {
        int iAdd;
        long j2;
        long jPosition0;
        ensureOpen();
        if (j < 0) {
            throw new IllegalArgumentException();
        }
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
            } catch (Throwable th) {
                th = th;
                iAdd = -1;
            }
            try {
                if (!isOpen()) {
                    this.threads.remove(iAdd);
                    end($assertionsDisabled);
                    return null;
                }
                BlockGuard.getThreadPolicy().onReadFromDisk();
                j2 = -1;
                while (true) {
                    try {
                        jPosition0 = position0(this.fd, j);
                        if (jPosition0 != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            j2 = jPosition0;
                        } catch (Throwable th2) {
                            th = th2;
                            j2 = jPosition0;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                this.threads.remove(iAdd);
                if (jPosition0 <= -1) {
                    z = false;
                }
                end(z);
                return this;
            } catch (Throwable th4) {
                th = th4;
                j2 = -1;
            }
            this.threads.remove(iAdd);
            if (j2 <= -1) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    public long size() throws IOException {
        long j;
        int iAdd;
        Throwable th;
        long size;
        ensureOpen();
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
                try {
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return -1L;
                    }
                    j = -1;
                    while (true) {
                        try {
                            size = this.nd.size(this.fd);
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
                                this.threads.remove(iAdd);
                                if (j <= -1) {
                                    z = false;
                                }
                                end(z);
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    long jNormalize = IOStatus.normalize(size);
                    this.threads.remove(iAdd);
                    if (size <= -1) {
                        z = false;
                    }
                    end(z);
                    return jNormalize;
                } catch (Throwable th4) {
                    th = th4;
                    j = -1;
                }
            } catch (Throwable th5) {
                j = -1;
                iAdd = -1;
                th = th5;
            }
        }
    }

    @Override
    public FileChannel truncate(long j) throws IOException {
        int iAdd;
        int iPosition0;
        long size;
        long jPosition0;
        ensureOpen();
        if (j < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        if (!this.writable) {
            throw new NonWritableChannelException();
        }
        synchronized (this.positionLock) {
            boolean z = true;
            try {
                begin();
                iAdd = this.threads.add();
                try {
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return null;
                    }
                    do {
                        size = this.nd.size(this.fd);
                        if (size != -3) {
                            break;
                        }
                    } while (isOpen());
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return null;
                    }
                    do {
                        jPosition0 = position0(this.fd, -1L);
                        if (jPosition0 != -3) {
                            break;
                        }
                    } while (isOpen());
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end($assertionsDisabled);
                        return null;
                    }
                    if (j < size) {
                        int i = -1;
                        while (true) {
                            try {
                                iPosition0 = this.nd.truncate(this.fd, j);
                                if (iPosition0 != -3) {
                                    break;
                                }
                                try {
                                    if (!isOpen()) {
                                        break;
                                    }
                                    i = iPosition0;
                                } catch (Throwable th) {
                                    th = th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                iPosition0 = i;
                            }
                        }
                        if (!isOpen()) {
                            this.threads.remove(iAdd);
                            if (iPosition0 <= -1) {
                                z = false;
                            }
                            end(z);
                            return null;
                        }
                    }
                    if (jPosition0 <= j) {
                        j = jPosition0;
                    }
                    do {
                        iPosition0 = (int) position0(this.fd, j);
                        if (iPosition0 != -3) {
                            break;
                        }
                    } while (isOpen());
                    this.threads.remove(iAdd);
                    if (iPosition0 <= -1) {
                        z = false;
                    }
                    end(z);
                    return this;
                } catch (Throwable th3) {
                    th = th3;
                    iPosition0 = -1;
                }
            } catch (Throwable th4) {
                th = th4;
                iAdd = -1;
                iPosition0 = -1;
            }
            this.threads.remove(iAdd);
            if (iPosition0 <= -1) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    public void force(boolean z) throws Throwable {
        int iAdd;
        int i;
        int iForce;
        ensureOpen();
        try {
            begin();
            iAdd = this.threads.add();
            try {
                if (isOpen()) {
                    i = -1;
                    while (true) {
                        try {
                            iForce = this.nd.force(this.fd, z);
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
                                this.threads.remove(iAdd);
                                end(i > -1);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    this.threads.remove(iAdd);
                    end(iForce > -1);
                    return;
                }
                this.threads.remove(iAdd);
                end($assertionsDisabled);
            } catch (Throwable th3) {
                th = th3;
                i = -1;
            }
        } catch (Throwable th4) {
            th = th4;
            iAdd = -1;
            i = -1;
        }
    }

    private long transferToDirectlyInternal(long j, int i, WritableByteChannel writableByteChannel, FileDescriptor fileDescriptor) throws Throwable {
        int iAdd;
        long jTransferTo0;
        try {
            begin();
            iAdd = this.threads.add();
            try {
                if (!isOpen()) {
                    this.threads.remove(iAdd);
                    end($assertionsDisabled);
                    return -1L;
                }
                BlockGuard.getThreadPolicy().onWriteToDisk();
                long j2 = -1;
                while (true) {
                    try {
                        jTransferTo0 = transferTo0(this.fd, j, i, fileDescriptor);
                        if (jTransferTo0 != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            j2 = jTransferTo0;
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        jTransferTo0 = j2;
                    }
                }
                if (jTransferTo0 == -6) {
                    if (writableByteChannel instanceof SinkChannelImpl) {
                        pipeSupported = $assertionsDisabled;
                    }
                    if (writableByteChannel instanceof FileChannelImpl) {
                        fileSupported = $assertionsDisabled;
                    }
                    this.threads.remove(iAdd);
                    end(jTransferTo0 > -1);
                    return -6L;
                }
                if (jTransferTo0 == -4) {
                    transferSupported = $assertionsDisabled;
                    this.threads.remove(iAdd);
                    end(jTransferTo0 > -1);
                    return -4L;
                }
                long jNormalize = IOStatus.normalize(jTransferTo0);
                this.threads.remove(iAdd);
                end(jTransferTo0 > -1);
                return jNormalize;
            } catch (Throwable th3) {
                th = th3;
                jTransferTo0 = -1;
            }
        } catch (Throwable th4) {
            th = th4;
            iAdd = -1;
        }
        this.threads.remove(iAdd);
        end(jTransferTo0 > -1);
        throw th;
    }

    private long transferToDirectly(long j, int i, WritableByteChannel writableByteChannel) throws IOException {
        long jTransferToDirectlyInternal;
        if (!transferSupported) {
            return -4L;
        }
        FileDescriptor fd = null;
        if (writableByteChannel instanceof FileChannelImpl) {
            if (!fileSupported) {
                return -6L;
            }
            fd = ((FileChannelImpl) writableByteChannel).fd;
        } else if (writableByteChannel instanceof SelChImpl) {
            if ((writableByteChannel instanceof SinkChannelImpl) && !pipeSupported) {
                return -6L;
            }
            if (!this.nd.canTransferToDirectly((SelectableChannel) writableByteChannel)) {
                return -6L;
            }
            fd = ((SelChImpl) writableByteChannel).getFD();
        }
        FileDescriptor fileDescriptor = fd;
        if (fileDescriptor == null || IOUtil.fdVal(this.fd) == IOUtil.fdVal(fileDescriptor)) {
            return -4L;
        }
        if (this.nd.transferToDirectlyNeedsPositionLock()) {
            synchronized (this.positionLock) {
                long jPosition = position();
                try {
                    jTransferToDirectlyInternal = transferToDirectlyInternal(j, i, writableByteChannel, fileDescriptor);
                } finally {
                    position(jPosition);
                }
            }
            return jTransferToDirectlyInternal;
        }
        return transferToDirectlyInternal(j, i, writableByteChannel, fileDescriptor);
    }

    private long transferToTrustedChannel(long j, long j2, WritableByteChannel writableByteChannel) throws Throwable {
        boolean z = writableByteChannel instanceof SelChImpl;
        if (!(writableByteChannel instanceof FileChannelImpl) && !z) {
            return -4L;
        }
        long j3 = j;
        long j4 = j2;
        while (true) {
            if (j4 <= 0) {
                break;
            }
            try {
                MappedByteBuffer map = map(FileChannel.MapMode.READ_ONLY, j3, Math.min(j4, MAPPED_TRANSFER_SIZE));
                try {
                    long jWrite = writableByteChannel.write(map);
                    j4 -= jWrite;
                    if (!z) {
                        j3 += jWrite;
                        unmap(map);
                    } else {
                        unmap(map);
                        break;
                    }
                } catch (Throwable th) {
                    unmap(map);
                    throw th;
                }
            } catch (ClosedByInterruptException e) {
                try {
                    close();
                } catch (Throwable th2) {
                    e.addSuppressed(th2);
                }
                throw e;
            } catch (IOException e2) {
                if (j4 == j2) {
                    throw e2;
                }
            }
        }
        return j2 - j4;
    }

    private long transferToArbitraryChannel(long j, int i, WritableByteChannel writableByteChannel) throws IOException {
        long j2;
        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer(Math.min(i, 8192));
        try {
            try {
                Util.erase(temporaryDirectBuffer);
                long j3 = j;
                j2 = 0;
                while (true) {
                    long j4 = i;
                    if (j2 >= j4) {
                        break;
                    }
                    try {
                        temporaryDirectBuffer.limit(Math.min((int) (j4 - j2), 8192));
                        int i2 = read(temporaryDirectBuffer, j3);
                        if (i2 <= 0) {
                            break;
                        }
                        temporaryDirectBuffer.flip();
                        int iWrite = writableByteChannel.write(temporaryDirectBuffer);
                        long j5 = iWrite;
                        j2 += j5;
                        if (iWrite != i2) {
                            break;
                        }
                        j3 += j5;
                        temporaryDirectBuffer.clear();
                    } catch (IOException e) {
                        e = e;
                        if (j2 > 0) {
                            return j2;
                        }
                        throw e;
                    }
                }
                return j2;
            } finally {
                Util.releaseTemporaryDirectBuffer(temporaryDirectBuffer);
            }
        } catch (IOException e2) {
            e = e2;
            j2 = 0;
        }
    }

    @Override
    public long transferTo(long j, long j2, WritableByteChannel writableByteChannel) throws Throwable {
        ensureOpen();
        if (!writableByteChannel.isOpen()) {
            throw new ClosedChannelException();
        }
        if (!this.readable) {
            throw new NonReadableChannelException();
        }
        if ((writableByteChannel instanceof FileChannelImpl) && !((FileChannelImpl) writableByteChannel).writable) {
            throw new NonWritableChannelException();
        }
        if (j < 0 || j2 < 0) {
            throw new IllegalArgumentException();
        }
        long size = size();
        if (j > size) {
            return 0L;
        }
        int iMin = (int) Math.min(j2, 2147483647L);
        long j3 = size - j;
        if (j3 < iMin) {
            iMin = (int) j3;
        }
        long jTransferToDirectly = transferToDirectly(j, iMin, writableByteChannel);
        if (jTransferToDirectly >= 0) {
            return jTransferToDirectly;
        }
        long jTransferToTrustedChannel = transferToTrustedChannel(j, iMin, writableByteChannel);
        if (jTransferToTrustedChannel >= 0) {
            return jTransferToTrustedChannel;
        }
        return transferToArbitraryChannel(j, iMin, writableByteChannel);
    }

    private long transferFromFileChannel(FileChannelImpl fileChannelImpl, long j, long j2) throws Throwable {
        Object obj;
        FileChannelImpl fileChannelImpl2 = fileChannelImpl;
        if (!fileChannelImpl2.readable) {
            throw new NonReadableChannelException();
        }
        Object obj2 = fileChannelImpl2.positionLock;
        synchronized (obj2) {
            try {
                long jPosition = fileChannelImpl.position();
                long jMin = Math.min(j2, fileChannelImpl.size() - jPosition);
                long j3 = j;
                long j4 = jPosition;
                long j5 = jMin;
                while (j5 > 0) {
                    FileChannelImpl fileChannelImpl3 = fileChannelImpl2;
                    obj = obj2;
                    long j6 = j3;
                    try {
                        MappedByteBuffer map = fileChannelImpl3.map(FileChannel.MapMode.READ_ONLY, j4, Math.min(j5, MAPPED_TRANSFER_SIZE));
                        try {
                            try {
                                long jWrite = write(map, j6);
                                j4 += jWrite;
                                j3 = j6 + jWrite;
                                j5 -= jWrite;
                                unmap(map);
                                obj2 = obj;
                                fileChannelImpl2 = fileChannelImpl;
                            } catch (IOException e) {
                                if (j5 == jMin) {
                                    throw e;
                                }
                                unmap(map);
                                long j7 = jMin - j5;
                                fileChannelImpl.position(jPosition + j7);
                                return j7;
                            }
                        } finally {
                            unmap(map);
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                obj = obj2;
                long j72 = jMin - j5;
                fileChannelImpl.position(jPosition + j72);
                return j72;
            } catch (Throwable th2) {
                th = th2;
                obj = obj2;
            }
        }
    }

    private long transferFromArbitraryChannel(ReadableByteChannel readableByteChannel, long j, long j2) throws IOException {
        long j3;
        long j4 = 8192;
        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer((int) Math.min(j2, 8192L));
        try {
            try {
                Util.erase(temporaryDirectBuffer);
                long j5 = j;
                j3 = 0;
                while (j3 < j2) {
                    try {
                        temporaryDirectBuffer.limit((int) Math.min(j2 - j3, j4));
                        int i = readableByteChannel.read(temporaryDirectBuffer);
                        if (i <= 0) {
                            break;
                        }
                        temporaryDirectBuffer.flip();
                        int iWrite = write(temporaryDirectBuffer, j5);
                        long j6 = iWrite;
                        j3 += j6;
                        if (iWrite != i) {
                            break;
                        }
                        j5 += j6;
                        temporaryDirectBuffer.clear();
                        j4 = 8192;
                    } catch (IOException e) {
                        e = e;
                        if (j3 > 0) {
                            return j3;
                        }
                        throw e;
                    }
                }
                return j3;
            } finally {
                Util.releaseTemporaryDirectBuffer(temporaryDirectBuffer);
            }
        } catch (IOException e2) {
            e = e2;
            j3 = 0;
        }
    }

    @Override
    public long transferFrom(ReadableByteChannel readableByteChannel, long j, long j2) throws IOException {
        ensureOpen();
        if (!readableByteChannel.isOpen()) {
            throw new ClosedChannelException();
        }
        if (!this.writable) {
            throw new NonWritableChannelException();
        }
        if (j < 0 || j2 < 0) {
            throw new IllegalArgumentException();
        }
        if (j > size()) {
            return 0L;
        }
        if (readableByteChannel instanceof FileChannelImpl) {
            return transferFromFileChannel((FileChannelImpl) readableByteChannel, j, j2);
        }
        return transferFromArbitraryChannel(readableByteChannel, j, j2);
    }

    @Override
    public int read(ByteBuffer byteBuffer, long j) throws IOException {
        int internal;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        if (j < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        if (!this.readable) {
            throw new NonReadableChannelException();
        }
        ensureOpen();
        if (this.nd.needsPositionLock()) {
            synchronized (this.positionLock) {
                internal = readInternal(byteBuffer, j);
            }
            return internal;
        }
        return readInternal(byteBuffer, j);
    }

    private int readInternal(ByteBuffer byteBuffer, long j) throws Throwable {
        int iAdd;
        int i;
        int i2;
        try {
            begin();
            iAdd = this.threads.add();
            try {
                if (!isOpen()) {
                    this.threads.remove(iAdd);
                    end($assertionsDisabled);
                    return -1;
                }
                i = 0;
                while (true) {
                    try {
                        i2 = IOUtil.read(this.fd, byteBuffer, j, this.nd);
                        if (i2 != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            i = i2;
                        } catch (Throwable th) {
                            th = th;
                            i = i2;
                            this.threads.remove(iAdd);
                            end(i > 0);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                int iNormalize = IOStatus.normalize(i2);
                this.threads.remove(iAdd);
                end(i2 > 0);
                return iNormalize;
            } catch (Throwable th3) {
                th = th3;
                i = 0;
                this.threads.remove(iAdd);
                end(i > 0);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            iAdd = -1;
        }
    }

    @Override
    public int write(ByteBuffer byteBuffer, long j) throws IOException {
        int iWriteInternal;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        if (j < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        if (!this.writable) {
            throw new NonWritableChannelException();
        }
        ensureOpen();
        if (this.nd.needsPositionLock()) {
            synchronized (this.positionLock) {
                iWriteInternal = writeInternal(byteBuffer, j);
            }
            return iWriteInternal;
        }
        return writeInternal(byteBuffer, j);
    }

    private int writeInternal(ByteBuffer byteBuffer, long j) throws Throwable {
        int iAdd;
        int i;
        int iWrite;
        try {
            begin();
            iAdd = this.threads.add();
            try {
                if (!isOpen()) {
                    this.threads.remove(iAdd);
                    end($assertionsDisabled);
                    return -1;
                }
                i = 0;
                while (true) {
                    try {
                        iWrite = IOUtil.write(this.fd, byteBuffer, j, this.nd);
                        if (iWrite != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            i = iWrite;
                        } catch (Throwable th) {
                            th = th;
                            i = iWrite;
                            this.threads.remove(iAdd);
                            end(i > 0);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                int iNormalize = IOStatus.normalize(iWrite);
                this.threads.remove(iAdd);
                end(iWrite > 0);
                return iNormalize;
            } catch (Throwable th3) {
                th = th3;
                i = 0;
                this.threads.remove(iAdd);
                end(i > 0);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            iAdd = -1;
        }
    }

    private static class Unmapper implements Runnable {
        static final boolean $assertionsDisabled = false;
        static volatile int count;
        private static final NativeDispatcher nd = new FileDispatcherImpl();
        static volatile long totalCapacity;
        static volatile long totalSize;
        private volatile long address;
        private final int cap;
        private final FileDescriptor fd;
        private final long size;

        private Unmapper(long j, long j2, int i, FileDescriptor fileDescriptor) {
            this.address = j;
            this.size = j2;
            this.cap = i;
            this.fd = fileDescriptor;
            synchronized (Unmapper.class) {
                count++;
                totalSize += j2;
                totalCapacity += (long) i;
            }
        }

        @Override
        public void run() {
            if (this.address != 0) {
                FileChannelImpl.unmap0(this.address, this.size);
                this.address = 0L;
                if (this.fd.valid()) {
                    try {
                        nd.close(this.fd);
                    } catch (IOException e) {
                    }
                }
                synchronized (Unmapper.class) {
                    count--;
                    totalSize -= this.size;
                    totalCapacity -= (long) this.cap;
                }
            }
        }
    }

    private static void unmap(MappedByteBuffer mappedByteBuffer) {
        Cleaner cleaner = ((DirectBuffer) mappedByteBuffer).cleaner();
        if (cleaner != null) {
            cleaner.clean();
        }
    }

    @Override
    public MappedByteBuffer map(FileChannel.MapMode mapMode, long j, long j2) throws Throwable {
        ?? r14;
        ?? r142;
        long size;
        long j3;
        long jMap0;
        ensureOpen();
        if (mapMode == null) {
            throw new NullPointerException("Mode is null");
        }
        if (j < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        if (j2 < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        long j4 = j + j2;
        if (j4 < 0) {
            throw new IllegalArgumentException("Position + size overflow");
        }
        if (j2 > 2147483647L) {
            throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        }
        int i = mapMode == FileChannel.MapMode.READ_ONLY ? 0 : mapMode == FileChannel.MapMode.READ_WRITE ? 1 : mapMode == FileChannel.MapMode.PRIVATE ? 2 : -1;
        if (mapMode != FileChannel.MapMode.READ_ONLY && !this.writable) {
            throw new NonWritableChannelException();
        }
        if (!this.readable) {
            throw new NonReadableChannelException();
        }
        long j5 = -1;
        try {
            begin();
            int iAdd = this.threads.add();
            try {
                if (!isOpen()) {
                    this.threads.remove(iAdd);
                    end(IOStatus.checkAll(-1L));
                    return null;
                }
                do {
                    ?? r0 = this.nd;
                    r142 = this.fd;
                    size = r0.size(r142);
                    if (size != -3) {
                        break;
                    }
                } while (isOpen());
                if (!isOpen()) {
                    this.threads.remove(iAdd);
                    end(IOStatus.checkAll(-1L));
                    return null;
                }
                r14 = r142;
                if (size < j4) {
                    do {
                        try {
                            ?? r02 = this.nd;
                            r142 = this.fd;
                            int iTruncate = r02.truncate(r142, j4);
                            r142 = -3;
                            r142 = -3;
                            r142 = -3;
                            if (iTruncate != -3) {
                                break;
                            }
                        } catch (IOException e) {
                            try {
                                if (OsConstants.S_ISREG(Libcore.os.fstat(this.fd).st_mode)) {
                                    throw e;
                                }
                            } catch (ErrnoException e2) {
                                e2.rethrowAsIOException();
                            }
                        }
                    } while (isOpen());
                    r14 = r142;
                    if (!isOpen()) {
                        this.threads.remove(iAdd);
                        end(IOStatus.checkAll(-1L));
                        return null;
                    }
                }
                if (j2 == 0) {
                    try {
                        DirectByteBuffer directByteBuffer = new DirectByteBuffer(0, 0L, new FileDescriptor(), null, (!this.writable || i == 0) ? true : $assertionsDisabled);
                        this.threads.remove(iAdd);
                        end(IOStatus.checkAll(0L));
                        return directByteBuffer;
                    } catch (Throwable th) {
                        th = th;
                        j5 = 0;
                    }
                } else {
                    long j6 = (int) (j % allocationGranularity);
                    long j7 = j - j6;
                    long j8 = j2 + j6;
                    try {
                        try {
                            BlockGuard.getThreadPolicy().onReadFromDisk();
                            j3 = j8;
                            r14 = iAdd;
                            try {
                                jMap0 = map0(i, j7, j3);
                                r14 = r14;
                            } catch (OutOfMemoryError e3) {
                                System.gc();
                                try {
                                    Thread.sleep(100L);
                                } catch (InterruptedException e4) {
                                    Thread.currentThread().interrupt();
                                }
                                try {
                                    jMap0 = map0(i, j7, j3);
                                    r14 = r14;
                                } catch (OutOfMemoryError e5) {
                                    throw new IOException("Map failed", e5);
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } catch (OutOfMemoryError e6) {
                        j3 = j8;
                        r14 = iAdd;
                    }
                    long j9 = jMap0;
                    try {
                        try {
                            FileDescriptor fileDescriptorDuplicateForMapping = this.nd.duplicateForMapping(this.fd);
                            int i2 = (int) j2;
                            DirectByteBuffer directByteBuffer2 = new DirectByteBuffer(i2, j9 + j6, fileDescriptorDuplicateForMapping, new Unmapper(j9, j3, i2, fileDescriptorDuplicateForMapping), (!this.writable || i == 0) ? true : $assertionsDisabled);
                            this.threads.remove(r14);
                            end(IOStatus.checkAll(j9));
                            return directByteBuffer2;
                        } catch (IOException e7) {
                            unmap0(j9, j3);
                            throw e7;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        j5 = j9;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
            }
            r14 = iAdd;
        } catch (Throwable th5) {
            th = th5;
            r14 = -1;
        }
        this.threads.remove(r14);
        end(IOStatus.checkAll(j5));
        throw th;
    }

    private static boolean isSharedFileLockTable() {
        if (!propertyChecked) {
            synchronized (FileChannelImpl.class) {
                if (!propertyChecked) {
                    String str = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.disableSystemWideOverlappingFileLockCheck"));
                    isSharedFileLockTable = (str == null || str.equals("false")) ? true : $assertionsDisabled;
                    propertyChecked = true;
                }
            }
        }
        return isSharedFileLockTable;
    }

    private FileLockTable fileLockTable() throws IOException {
        if (this.fileLockTable == null) {
            synchronized (this) {
                if (this.fileLockTable == null) {
                    if (isSharedFileLockTable()) {
                        int iAdd = this.threads.add();
                        try {
                            ensureOpen();
                            this.fileLockTable = FileLockTable.newSharedFileLockTable(this, this.fd);
                            this.threads.remove(iAdd);
                        } catch (Throwable th) {
                            this.threads.remove(iAdd);
                            throw th;
                        }
                    } else {
                        this.fileLockTable = new SimpleFileLockTable();
                    }
                }
            }
        }
        return this.fileLockTable;
    }

    @Override
    public FileLock lock(long j, long j2, boolean z) throws Throwable {
        boolean z2;
        FileLockImpl fileLockImpl;
        FileLockTable fileLockTable;
        int i;
        FileLockImpl fileLockImpl2;
        int iLock;
        FileLockImpl fileLockImpl3;
        ensureOpen();
        if (z && !this.readable) {
            throw new NonReadableChannelException();
        }
        if (!z && !this.writable) {
            throw new NonWritableChannelException();
        }
        FileLockImpl fileLockImpl4 = new FileLockImpl(this, j, j2, z);
        FileLockTable fileLockTable2 = fileLockTable();
        fileLockTable2.add(fileLockImpl4);
        try {
            begin();
            int iAdd = this.threads.add();
            try {
                if (!isOpen()) {
                    fileLockTable2.remove(fileLockImpl4);
                    this.threads.remove(iAdd);
                    try {
                        end($assertionsDisabled);
                        return null;
                    } catch (ClosedByInterruptException e) {
                        throw new FileLockInterruptionException();
                    }
                }
                while (true) {
                    fileLockImpl2 = fileLockImpl4;
                    try {
                        iLock = this.nd.lock(this.fd, true, j, j2, z);
                        if (iLock != 2 || !isOpen()) {
                            break;
                        }
                        fileLockImpl4 = fileLockImpl2;
                    } catch (Throwable th) {
                        th = th;
                        fileLockImpl = fileLockImpl2;
                        z2 = false;
                        i = iAdd;
                        fileLockTable = fileLockTable2;
                    }
                }
                boolean z3 = true;
                if (!isOpen()) {
                    fileLockImpl3 = fileLockImpl2;
                    i = iAdd;
                    fileLockTable = fileLockTable2;
                    z3 = false;
                } else if (iLock == 1) {
                    fileLockImpl = fileLockImpl2;
                    z2 = false;
                    i = iAdd;
                    fileLockTable = fileLockTable2;
                    try {
                        FileLockImpl fileLockImpl5 = new FileLockImpl(this, j, j2, $assertionsDisabled);
                        fileLockTable.replace(fileLockImpl, fileLockImpl5);
                        fileLockImpl3 = fileLockImpl5;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } else {
                    fileLockImpl3 = fileLockImpl2;
                    i = iAdd;
                    fileLockTable = fileLockTable2;
                }
                if (!z3) {
                    fileLockTable.remove(fileLockImpl3);
                }
                this.threads.remove(i);
                try {
                    end(z3);
                    return fileLockImpl3;
                } catch (ClosedByInterruptException e2) {
                    throw new FileLockInterruptionException();
                }
            } catch (Throwable th3) {
                th = th3;
                z2 = false;
                i = iAdd;
                fileLockImpl = fileLockImpl4;
            }
            fileLockTable = fileLockTable2;
        } catch (Throwable th4) {
            th = th4;
            z2 = false;
            fileLockImpl = fileLockImpl4;
            fileLockTable = fileLockTable2;
            i = -1;
        }
        fileLockTable.remove(fileLockImpl);
        this.threads.remove(i);
        try {
            end(z2);
            throw th;
        } catch (ClosedByInterruptException e3) {
            throw new FileLockInterruptionException();
        }
    }

    @Override
    public FileLock tryLock(long j, long j2, boolean z) throws Throwable {
        FileLockTable fileLockTable;
        FileLockImpl fileLockImpl;
        ensureOpen();
        if (z && !this.readable) {
            throw new NonReadableChannelException();
        }
        if (!z && !this.writable) {
            throw new NonWritableChannelException();
        }
        FileLockImpl fileLockImpl2 = new FileLockImpl(this, j, j2, z);
        FileLockTable fileLockTable2 = fileLockTable();
        fileLockTable2.add(fileLockImpl2);
        int iAdd = this.threads.add();
        try {
            try {
                try {
                    ensureOpen();
                } catch (Throwable th) {
                    th = th;
                    this.threads.remove(iAdd);
                    throw th;
                }
            } catch (IOException e) {
                e = e;
                fileLockTable = fileLockTable2;
                fileLockImpl = fileLockImpl2;
            }
            try {
                int iLock = this.nd.lock(this.fd, $assertionsDisabled, j, j2, z);
                if (iLock == -1) {
                    fileLockTable2.remove(fileLockImpl2);
                    this.threads.remove(iAdd);
                    return null;
                }
                if (iLock != 1) {
                    this.threads.remove(iAdd);
                    return fileLockImpl2;
                }
                FileLockImpl fileLockImpl3 = new FileLockImpl(this, j, j2, $assertionsDisabled);
                fileLockTable2.replace(fileLockImpl2, fileLockImpl3);
                this.threads.remove(iAdd);
                return fileLockImpl3;
            } catch (IOException e2) {
                e = e2;
                fileLockImpl = fileLockImpl2;
                fileLockTable = fileLockTable2;
                fileLockTable.remove(fileLockImpl);
                throw e;
            }
        } catch (Throwable th2) {
            th = th2;
            this.threads.remove(iAdd);
            throw th;
        }
    }

    void release(FileLockImpl fileLockImpl) throws IOException {
        int iAdd = this.threads.add();
        try {
            ensureOpen();
            this.nd.release(this.fd, fileLockImpl.position(), fileLockImpl.size());
            this.threads.remove(iAdd);
            this.fileLockTable.remove(fileLockImpl);
        } catch (Throwable th) {
            this.threads.remove(iAdd);
            throw th;
        }
    }

    private static class SimpleFileLockTable extends FileLockTable {
        static final boolean $assertionsDisabled = false;
        private final List<FileLock> lockList = new ArrayList(2);

        private void checkList(long j, long j2) throws OverlappingFileLockException {
            Iterator<FileLock> it = this.lockList.iterator();
            while (it.hasNext()) {
                if (it.next().overlaps(j, j2)) {
                    throw new OverlappingFileLockException();
                }
            }
        }

        @Override
        public void add(FileLock fileLock) throws OverlappingFileLockException {
            synchronized (this.lockList) {
                checkList(fileLock.position(), fileLock.size());
                this.lockList.add(fileLock);
            }
        }

        @Override
        public void remove(FileLock fileLock) {
            synchronized (this.lockList) {
                this.lockList.remove(fileLock);
            }
        }

        @Override
        public List<FileLock> removeAll() {
            ArrayList arrayList;
            synchronized (this.lockList) {
                arrayList = new ArrayList(this.lockList);
                this.lockList.clear();
            }
            return arrayList;
        }

        @Override
        public void replace(FileLock fileLock, FileLock fileLock2) {
            synchronized (this.lockList) {
                this.lockList.remove(fileLock);
                this.lockList.add(fileLock2);
            }
        }
    }
}
