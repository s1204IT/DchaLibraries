package android.net;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;

public class LocalServerSocket implements Closeable {
    private static final int LISTEN_BACKLOG = 50;
    private final LocalSocketImpl impl;
    private final LocalSocketAddress localAddress;

    public LocalServerSocket(String str) throws IOException {
        this.impl = new LocalSocketImpl();
        this.impl.create(2);
        this.localAddress = new LocalSocketAddress(str);
        this.impl.bind(this.localAddress);
        this.impl.listen(50);
    }

    public LocalServerSocket(FileDescriptor fileDescriptor) throws IOException {
        this.impl = new LocalSocketImpl(fileDescriptor);
        this.impl.listen(50);
        this.localAddress = this.impl.getSockAddress();
    }

    public LocalSocketAddress getLocalSocketAddress() {
        return this.localAddress;
    }

    public LocalSocket accept() throws IOException {
        LocalSocketImpl localSocketImpl = new LocalSocketImpl();
        this.impl.accept(localSocketImpl);
        return LocalSocket.createLocalSocketForAccept(localSocketImpl);
    }

    public FileDescriptor getFileDescriptor() {
        return this.impl.getFileDescriptor();
    }

    @Override
    public void close() throws IOException {
        this.impl.close();
    }
}
