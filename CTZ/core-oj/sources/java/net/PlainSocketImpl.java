package java.net;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import jdk.net.ExtendedSocketOptions;
import jdk.net.SocketFlow;
import libcore.io.AsynchronousCloseMonitor;
import libcore.io.IoBridge;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import sun.net.ExtendedOptionsImpl;

class PlainSocketImpl extends AbstractPlainSocketImpl {
    PlainSocketImpl() {
        this(new FileDescriptor());
    }

    PlainSocketImpl(FileDescriptor fileDescriptor) {
        this.fd = fileDescriptor;
    }

    @Override
    protected <T> void setOption(SocketOption<T> socketOption, T t) throws IOException {
        if (!socketOption.equals(ExtendedSocketOptions.SO_FLOW_SLA)) {
            super.setOption(socketOption, t);
        } else {
            if (isClosedOrPending()) {
                throw new SocketException("Socket closed");
            }
            ExtendedOptionsImpl.checkSetOptionPermission(socketOption);
            ExtendedOptionsImpl.checkValueType(t, SocketFlow.class);
            ExtendedOptionsImpl.setFlowOption(getFileDescriptor(), (SocketFlow) t);
        }
    }

    @Override
    protected <T> T getOption(SocketOption<T> socketOption) throws IOException {
        if (!socketOption.equals(ExtendedSocketOptions.SO_FLOW_SLA)) {
            return (T) super.getOption(socketOption);
        }
        if (isClosedOrPending()) {
            throw new SocketException("Socket closed");
        }
        ExtendedOptionsImpl.checkGetOptionPermission(socketOption);
        ?? r2 = (T) SocketFlow.create();
        ExtendedOptionsImpl.getFlowOption(getFileDescriptor(), r2);
        return r2;
    }

    @Override
    protected void socketSetOption(int i, Object obj) throws SocketException {
        try {
            socketSetOption0(i, obj);
        } catch (SocketException e) {
            if (this.socket == null || !this.socket.isConnected()) {
                throw e;
            }
        }
    }

    @Override
    void socketCreate(boolean z) throws IOException {
        this.fd.setInt$(IoBridge.socket(OsConstants.AF_INET6, z ? OsConstants.SOCK_STREAM : OsConstants.SOCK_DGRAM, 0).getInt$());
        if (this.serverSocket != null) {
            IoUtils.setBlocking(this.fd, false);
            IoBridge.setSocketOption(this.fd, 4, true);
        }
    }

    @Override
    void socketConnect(InetAddress inetAddress, int i, int i2) throws IOException {
        if (this.fd == null || !this.fd.valid()) {
            throw new SocketException("Socket closed");
        }
        IoBridge.connect(this.fd, inetAddress, i, i2);
        this.address = inetAddress;
        this.port = i;
        if (this.localport == 0 && !isClosedOrPending()) {
            this.localport = IoBridge.getLocalInetSocketAddress(this.fd).getPort();
        }
    }

    @Override
    void socketBind(InetAddress inetAddress, int i) throws IOException {
        if (this.fd == null || !this.fd.valid()) {
            throw new SocketException("Socket closed");
        }
        IoBridge.bind(this.fd, inetAddress, i);
        this.address = inetAddress;
        if (i == 0) {
            this.localport = IoBridge.getLocalInetSocketAddress(this.fd).getPort();
        } else {
            this.localport = i;
        }
    }

    @Override
    void socketListen(int i) throws IOException {
        if (this.fd == null || !this.fd.valid()) {
            throw new SocketException("Socket closed");
        }
        try {
            Libcore.os.listen(this.fd, i);
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }

    @Override
    void socketAccept(SocketImpl socketImpl) throws IOException {
        if (this.fd == null || !this.fd.valid()) {
            throw new SocketException("Socket closed");
        }
        if (this.timeout <= 0) {
            IoBridge.poll(this.fd, OsConstants.POLLIN | OsConstants.POLLERR, -1);
        } else {
            IoBridge.poll(this.fd, OsConstants.POLLIN | OsConstants.POLLERR, this.timeout);
        }
        InetSocketAddress inetSocketAddress = new InetSocketAddress();
        try {
            socketImpl.fd.setInt$(Libcore.os.accept(this.fd, inetSocketAddress).getInt$());
            socketImpl.address = inetSocketAddress.getAddress();
            socketImpl.port = inetSocketAddress.getPort();
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.EAGAIN) {
                throw new SocketTimeoutException(e);
            }
            if (e.errno == OsConstants.EINVAL || e.errno == OsConstants.EBADF) {
                throw new SocketException("Socket closed");
            }
            e.rethrowAsSocketException();
        }
        socketImpl.localport = IoBridge.getLocalInetSocketAddress(socketImpl.fd).getPort();
    }

    @Override
    int socketAvailable() throws IOException {
        return IoBridge.available(this.fd);
    }

    @Override
    void socketClose0(boolean z) throws IOException {
        if (this.fd == null || !this.fd.valid()) {
            throw new SocketException("socket already closed");
        }
        FileDescriptor markerFD = null;
        if (z) {
            markerFD = getMarkerFD();
        }
        if (z && markerFD != null) {
            try {
                Libcore.os.dup2(markerFD, this.fd.getInt$());
                Libcore.os.close(markerFD);
                AsynchronousCloseMonitor.signalBlockedThreads(this.fd);
                return;
            } catch (ErrnoException e) {
                return;
            }
        }
        IoBridge.closeAndSignalBlockedThreads(this.fd);
    }

    private FileDescriptor getMarkerFD() throws SocketException {
        FileDescriptor fileDescriptor = new FileDescriptor();
        FileDescriptor fileDescriptor2 = new FileDescriptor();
        try {
            Libcore.os.socketpair(OsConstants.AF_UNIX, OsConstants.SOCK_STREAM, 0, fileDescriptor, fileDescriptor2);
            Libcore.os.shutdown(fileDescriptor, OsConstants.SHUT_RDWR);
            Libcore.os.close(fileDescriptor2);
            return fileDescriptor;
        } catch (ErrnoException e) {
            return null;
        }
    }

    @Override
    void socketShutdown(int i) throws IOException {
        try {
            Libcore.os.shutdown(this.fd, i);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    void socketSetOption0(int i, Object obj) throws SocketException {
        if (i == 4102) {
            return;
        }
        IoBridge.setSocketOption(this.fd, i, obj);
    }

    @Override
    Object socketGetOption(int i) throws SocketException {
        return IoBridge.getSocketOption(this.fd, i);
    }

    @Override
    void socketSendUrgentData(int i) throws IOException {
        if (this.fd == null || !this.fd.valid()) {
            throw new SocketException("Socket closed");
        }
        try {
            Libcore.os.sendto(this.fd, new byte[]{(byte) i}, 0, 1, OsConstants.MSG_OOB, (InetAddress) null, 0);
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }
}
