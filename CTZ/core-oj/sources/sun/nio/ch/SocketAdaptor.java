package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class SocketAdaptor extends Socket {
    private final SocketChannelImpl sc;
    private InputStream socketInputStream;
    private volatile int timeout;

    private SocketAdaptor(SocketChannelImpl socketChannelImpl) throws SocketException {
        super(new FileDescriptorHolderSocketImpl(socketChannelImpl.getFD()));
        this.timeout = 0;
        this.socketInputStream = null;
        this.sc = socketChannelImpl;
    }

    public static Socket create(SocketChannelImpl socketChannelImpl) {
        try {
            return new SocketAdaptor(socketChannelImpl);
        } catch (SocketException e) {
            throw new InternalError("Should not reach here");
        }
    }

    @Override
    public SocketChannel getChannel() {
        return this.sc;
    }

    @Override
    public void connect(SocketAddress socketAddress) throws IOException {
        connect(socketAddress, 0);
    }

    @Override
    public void connect(SocketAddress socketAddress, int i) throws IOException {
        if (socketAddress == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }
        if (i < 0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
        synchronized (this.sc.blockingLock()) {
            if (!this.sc.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            try {
            } catch (Exception e) {
                Net.translateException(e, true);
            }
            if (i == 0) {
                try {
                    this.sc.connect(socketAddress);
                } catch (Exception e2) {
                    Net.translateException(e2);
                }
                return;
            }
            this.sc.configureBlocking(false);
            try {
                if (this.sc.connect(socketAddress)) {
                    return;
                }
                long jCurrentTimeMillis = i;
                while (this.sc.isOpen()) {
                    long jCurrentTimeMillis2 = System.currentTimeMillis();
                    if (this.sc.poll(Net.POLLCONN, jCurrentTimeMillis) <= 0 || !this.sc.finishConnect()) {
                        jCurrentTimeMillis -= System.currentTimeMillis() - jCurrentTimeMillis2;
                        if (jCurrentTimeMillis <= 0) {
                            try {
                                this.sc.close();
                            } catch (IOException e3) {
                            }
                            throw new SocketTimeoutException();
                        }
                    } else {
                        if (this.sc.isOpen()) {
                            this.sc.configureBlocking(true);
                        }
                        return;
                    }
                }
                throw new ClosedChannelException();
            } finally {
                if (this.sc.isOpen()) {
                    this.sc.configureBlocking(true);
                }
            }
        }
    }

    @Override
    public void bind(SocketAddress socketAddress) throws IOException {
        try {
            this.sc.bind(socketAddress);
        } catch (Exception e) {
            Net.translateException(e);
        }
    }

    @Override
    public InetAddress getInetAddress() {
        SocketAddress socketAddressRemoteAddress;
        if (isConnected() && (socketAddressRemoteAddress = this.sc.remoteAddress()) != null) {
            return ((InetSocketAddress) socketAddressRemoteAddress).getAddress();
        }
        return null;
    }

    @Override
    public InetAddress getLocalAddress() {
        InetSocketAddress inetSocketAddressLocalAddress;
        if (this.sc.isOpen() && (inetSocketAddressLocalAddress = this.sc.localAddress()) != null) {
            return Net.getRevealedLocalAddress(inetSocketAddressLocalAddress).getAddress();
        }
        return new InetSocketAddress(0).getAddress();
    }

    @Override
    public int getPort() {
        SocketAddress socketAddressRemoteAddress;
        if (isConnected() && (socketAddressRemoteAddress = this.sc.remoteAddress()) != null) {
            return ((InetSocketAddress) socketAddressRemoteAddress).getPort();
        }
        return 0;
    }

    @Override
    public int getLocalPort() {
        InetSocketAddress inetSocketAddressLocalAddress = this.sc.localAddress();
        if (inetSocketAddressLocalAddress == null) {
            return -1;
        }
        return inetSocketAddressLocalAddress.getPort();
    }

    private class SocketInputStream extends ChannelInputStream {
        private SocketInputStream() {
            super(SocketAdaptor.this.sc);
        }

        @Override
        protected int read(ByteBuffer byteBuffer) throws IOException {
            int i;
            synchronized (SocketAdaptor.this.sc.blockingLock()) {
                if (SocketAdaptor.this.sc.isBlocking()) {
                    if (SocketAdaptor.this.timeout == 0) {
                        return SocketAdaptor.this.sc.read(byteBuffer);
                    }
                    SocketAdaptor.this.sc.configureBlocking(false);
                    try {
                        int i2 = SocketAdaptor.this.sc.read(byteBuffer);
                        if (i2 == 0) {
                            long jCurrentTimeMillis = SocketAdaptor.this.timeout;
                            while (SocketAdaptor.this.sc.isOpen()) {
                                long jCurrentTimeMillis2 = System.currentTimeMillis();
                                if (SocketAdaptor.this.sc.poll(Net.POLLIN, jCurrentTimeMillis) > 0 && (i = SocketAdaptor.this.sc.read(byteBuffer)) != 0) {
                                    if (SocketAdaptor.this.sc.isOpen()) {
                                        SocketAdaptor.this.sc.configureBlocking(true);
                                    }
                                    return i;
                                }
                                jCurrentTimeMillis -= System.currentTimeMillis() - jCurrentTimeMillis2;
                                if (jCurrentTimeMillis <= 0) {
                                    throw new SocketTimeoutException();
                                }
                            }
                            throw new ClosedChannelException();
                        }
                        return i2;
                    } finally {
                        if (SocketAdaptor.this.sc.isOpen()) {
                            SocketAdaptor.this.sc.configureBlocking(true);
                        }
                    }
                }
                throw new IllegalBlockingModeException();
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!this.sc.isOpen()) {
            throw new SocketException("Socket is closed");
        }
        if (!this.sc.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (!this.sc.isInputOpen()) {
            throw new SocketException("Socket input is shutdown");
        }
        if (this.socketInputStream == null) {
            try {
                this.socketInputStream = (InputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                    @Override
                    public InputStream run() throws IOException {
                        return new SocketInputStream();
                    }
                });
            } catch (PrivilegedActionException e) {
                throw ((IOException) e.getException());
            }
        }
        return this.socketInputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!this.sc.isOpen()) {
            throw new SocketException("Socket is closed");
        }
        if (!this.sc.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (!this.sc.isOutputOpen()) {
            throw new SocketException("Socket output is shutdown");
        }
        try {
            return (OutputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                @Override
                public OutputStream run() throws IOException {
                    return Channels.newOutputStream(SocketAdaptor.this.sc);
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((IOException) e.getException());
        }
    }

    private void setBooleanOption(SocketOption<Boolean> socketOption, boolean z) throws SocketException {
        try {
            this.sc.setOption(socketOption, Boolean.valueOf(z));
        } catch (IOException e) {
            Net.translateToSocketException(e);
        }
    }

    private void setIntOption(SocketOption<Integer> socketOption, int i) throws SocketException {
        try {
            this.sc.setOption(socketOption, Integer.valueOf(i));
        } catch (IOException e) {
            Net.translateToSocketException(e);
        }
    }

    private boolean getBooleanOption(SocketOption<Boolean> socketOption) throws SocketException {
        try {
            return ((Boolean) this.sc.getOption(socketOption)).booleanValue();
        } catch (IOException e) {
            Net.translateToSocketException(e);
            return false;
        }
    }

    private int getIntOption(SocketOption<Integer> socketOption) throws SocketException {
        try {
            return ((Integer) this.sc.getOption(socketOption)).intValue();
        } catch (IOException e) {
            Net.translateToSocketException(e);
            return -1;
        }
    }

    @Override
    public void setTcpNoDelay(boolean z) throws SocketException {
        setBooleanOption(StandardSocketOptions.TCP_NODELAY, z);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return getBooleanOption(StandardSocketOptions.TCP_NODELAY);
    }

    @Override
    public void setSoLinger(boolean z, int i) throws SocketException {
        if (!z) {
            i = -1;
        }
        setIntOption(StandardSocketOptions.SO_LINGER, i);
    }

    @Override
    public int getSoLinger() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_LINGER);
    }

    @Override
    public void sendUrgentData(int i) throws IOException {
        if (this.sc.sendOutOfBandData((byte) i) == 0) {
            throw new IOException("Socket buffer full");
        }
    }

    @Override
    public void setOOBInline(boolean z) throws SocketException {
        setBooleanOption(ExtendedSocketOption.SO_OOBINLINE, z);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return getBooleanOption(ExtendedSocketOption.SO_OOBINLINE);
    }

    @Override
    public void setSoTimeout(int i) throws SocketException {
        if (i < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.timeout = i;
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return this.timeout;
    }

    @Override
    public void setSendBufferSize(int i) throws SocketException {
        if (i <= 0) {
            throw new IllegalArgumentException("Invalid send size");
        }
        setIntOption(StandardSocketOptions.SO_SNDBUF, i);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_SNDBUF);
    }

    @Override
    public void setReceiveBufferSize(int i) throws SocketException {
        if (i <= 0) {
            throw new IllegalArgumentException("Invalid receive size");
        }
        setIntOption(StandardSocketOptions.SO_RCVBUF, i);
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_RCVBUF);
    }

    @Override
    public void setKeepAlive(boolean z) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_KEEPALIVE, z);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_KEEPALIVE);
    }

    @Override
    public void setTrafficClass(int i) throws SocketException {
        setIntOption(StandardSocketOptions.IP_TOS, i);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return getIntOption(StandardSocketOptions.IP_TOS);
    }

    @Override
    public void setReuseAddress(boolean z) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_REUSEADDR, z);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
    }

    @Override
    public void close() throws IOException {
        this.sc.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        try {
            this.sc.shutdownInput();
        } catch (Exception e) {
            Net.translateException(e);
        }
    }

    @Override
    public void shutdownOutput() throws IOException {
        try {
            this.sc.shutdownOutput();
        } catch (Exception e) {
            Net.translateException(e);
        }
    }

    @Override
    public String toString() {
        if (this.sc.isConnected()) {
            return "Socket[addr=" + ((Object) getInetAddress()) + ",port=" + getPort() + ",localport=" + getLocalPort() + "]";
        }
        return "Socket[unconnected]";
    }

    @Override
    public boolean isConnected() {
        return this.sc.isConnected();
    }

    @Override
    public boolean isBound() {
        return this.sc.localAddress() != null;
    }

    @Override
    public boolean isClosed() {
        return !this.sc.isOpen();
    }

    @Override
    public boolean isInputShutdown() {
        return !this.sc.isInputOpen();
    }

    @Override
    public boolean isOutputShutdown() {
        return !this.sc.isOutputOpen();
    }

    @Override
    public FileDescriptor getFileDescriptor$() {
        return this.sc.getFD();
    }
}
