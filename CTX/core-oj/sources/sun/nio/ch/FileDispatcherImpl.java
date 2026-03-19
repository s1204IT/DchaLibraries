package sun.nio.ch;

import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SelectableChannel;

class FileDispatcherImpl extends FileDispatcher {
    static native void close0(FileDescriptor fileDescriptor) throws IOException;

    static native void closeIntFD(int i) throws IOException;

    static native int force0(FileDescriptor fileDescriptor, boolean z) throws IOException;

    static native int lock0(FileDescriptor fileDescriptor, boolean z, long j, long j2, boolean z2) throws IOException;

    static native void preClose0(FileDescriptor fileDescriptor) throws IOException;

    static native int pread0(FileDescriptor fileDescriptor, long j, int i, long j2) throws IOException;

    static native int pwrite0(FileDescriptor fileDescriptor, long j, int i, long j2) throws IOException;

    static native int read0(FileDescriptor fileDescriptor, long j, int i) throws IOException;

    static native long readv0(FileDescriptor fileDescriptor, long j, int i) throws IOException;

    static native void release0(FileDescriptor fileDescriptor, long j, long j2) throws IOException;

    static native long size0(FileDescriptor fileDescriptor) throws IOException;

    static native int truncate0(FileDescriptor fileDescriptor, long j) throws IOException;

    static native int write0(FileDescriptor fileDescriptor, long j, int i) throws IOException;

    static native long writev0(FileDescriptor fileDescriptor, long j, int i) throws IOException;

    FileDispatcherImpl(boolean z) {
    }

    FileDispatcherImpl() {
    }

    @Override
    int read(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return read0(fileDescriptor, j, i);
    }

    @Override
    int pread(FileDescriptor fileDescriptor, long j, int i, long j2) throws IOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return pread0(fileDescriptor, j, i, j2);
    }

    @Override
    long readv(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return readv0(fileDescriptor, j, i);
    }

    @Override
    int write(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return write0(fileDescriptor, j, i);
    }

    @Override
    int pwrite(FileDescriptor fileDescriptor, long j, int i, long j2) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return pwrite0(fileDescriptor, j, i, j2);
    }

    @Override
    long writev(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return writev0(fileDescriptor, j, i);
    }

    @Override
    int force(FileDescriptor fileDescriptor, boolean z) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return force0(fileDescriptor, z);
    }

    @Override
    int truncate(FileDescriptor fileDescriptor, long j) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return truncate0(fileDescriptor, j);
    }

    @Override
    long size(FileDescriptor fileDescriptor) throws IOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return size0(fileDescriptor);
    }

    @Override
    int lock(FileDescriptor fileDescriptor, boolean z, long j, long j2, boolean z2) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return lock0(fileDescriptor, z, j, j2, z2);
    }

    @Override
    void release(FileDescriptor fileDescriptor, long j, long j2) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        release0(fileDescriptor, j, j2);
    }

    @Override
    void close(FileDescriptor fileDescriptor) throws IOException {
        close0(fileDescriptor);
    }

    @Override
    void preClose(FileDescriptor fileDescriptor) throws IOException {
        preClose0(fileDescriptor);
    }

    @Override
    FileDescriptor duplicateForMapping(FileDescriptor fileDescriptor) {
        return new FileDescriptor();
    }

    @Override
    boolean canTransferToDirectly(SelectableChannel selectableChannel) {
        return true;
    }

    @Override
    boolean transferToDirectlyNeedsPositionLock() {
        return false;
    }
}
