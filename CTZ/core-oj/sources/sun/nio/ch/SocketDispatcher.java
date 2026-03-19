package sun.nio.ch;

import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.IOException;

class SocketDispatcher extends NativeDispatcher {
    SocketDispatcher() {
    }

    @Override
    int read(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onNetwork();
        return FileDispatcherImpl.read0(fileDescriptor, j, i);
    }

    @Override
    long readv(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onNetwork();
        return FileDispatcherImpl.readv0(fileDescriptor, j, i);
    }

    @Override
    int write(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onNetwork();
        return FileDispatcherImpl.write0(fileDescriptor, j, i);
    }

    @Override
    long writev(FileDescriptor fileDescriptor, long j, int i) throws IOException {
        BlockGuard.getThreadPolicy().onNetwork();
        return FileDispatcherImpl.writev0(fileDescriptor, j, i);
    }

    @Override
    void close(FileDescriptor fileDescriptor) throws IOException {
        FileDispatcherImpl.close0(fileDescriptor);
    }

    @Override
    void preClose(FileDescriptor fileDescriptor) throws IOException {
        FileDispatcherImpl.preClose0(fileDescriptor);
    }
}
