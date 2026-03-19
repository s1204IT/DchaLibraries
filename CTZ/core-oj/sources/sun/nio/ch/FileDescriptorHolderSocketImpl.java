package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

class FileDescriptorHolderSocketImpl extends SocketImpl {
    public FileDescriptorHolderSocketImpl(FileDescriptor fileDescriptor) {
        this.fd = fileDescriptor;
    }

    @Override
    public void setOption(int i, Object obj) throws SocketException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getOption(int i) throws SocketException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void create(boolean z) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void connect(String str, int i) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void connect(InetAddress inetAddress, int i) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void connect(SocketAddress socketAddress, int i) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void bind(InetAddress inetAddress, int i) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void listen(int i) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void accept(SocketImpl socketImpl) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int available() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void sendUrgentData(int i) throws IOException {
        throw new UnsupportedOperationException();
    }
}
