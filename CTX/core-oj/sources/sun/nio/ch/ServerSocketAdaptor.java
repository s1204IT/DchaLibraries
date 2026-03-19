package sun.nio.ch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerSocketAdaptor extends ServerSocket {
    static final boolean $assertionsDisabled = false;
    private final ServerSocketChannelImpl ssc;
    private volatile int timeout = 0;

    public static ServerSocket create(ServerSocketChannelImpl serverSocketChannelImpl) {
        try {
            return new ServerSocketAdaptor(serverSocketChannelImpl);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private ServerSocketAdaptor(ServerSocketChannelImpl serverSocketChannelImpl) throws IOException {
        this.ssc = serverSocketChannelImpl;
    }

    @Override
    public void bind(SocketAddress socketAddress) throws IOException {
        bind(socketAddress, 50);
    }

    @Override
    public void bind(SocketAddress socketAddress, int i) throws IOException {
        if (socketAddress == null) {
            socketAddress = new InetSocketAddress(0);
        }
        try {
            this.ssc.bind(socketAddress, i);
        } catch (Exception e) {
            Net.translateException(e);
        }
    }

    @Override
    public InetAddress getInetAddress() {
        if (!this.ssc.isBound()) {
            return null;
        }
        return Net.getRevealedLocalAddress(this.ssc.localAddress()).getAddress();
    }

    @Override
    public int getLocalPort() {
        if (!this.ssc.isBound()) {
            return -1;
        }
        return Net.asInetSocketAddress(this.ssc.localAddress()).getPort();
    }

    @Override
    public Socket accept() throws IOException {
        SocketChannel socketChannelAccept;
        synchronized (this.ssc.blockingLock()) {
            if (!this.ssc.isBound()) {
                throw new IllegalBlockingModeException();
            }
            try {
                if (this.timeout == 0) {
                    SocketChannel socketChannelAccept2 = this.ssc.accept();
                    if (socketChannelAccept2 == null && !this.ssc.isBlocking()) {
                        throw new IllegalBlockingModeException();
                    }
                    return socketChannelAccept2.socket();
                }
                this.ssc.configureBlocking(false);
                try {
                    SocketChannel socketChannelAccept3 = this.ssc.accept();
                    if (socketChannelAccept3 != null) {
                        return socketChannelAccept3.socket();
                    }
                    long jCurrentTimeMillis = this.timeout;
                    while (this.ssc.isOpen()) {
                        long jCurrentTimeMillis2 = System.currentTimeMillis();
                        if (this.ssc.poll(Net.POLLIN, jCurrentTimeMillis) > 0 && (socketChannelAccept = this.ssc.accept()) != null) {
                            Socket socket = socketChannelAccept.socket();
                            if (this.ssc.isOpen()) {
                                this.ssc.configureBlocking(true);
                            }
                            return socket;
                        }
                        jCurrentTimeMillis -= System.currentTimeMillis() - jCurrentTimeMillis2;
                        if (jCurrentTimeMillis <= 0) {
                            throw new SocketTimeoutException();
                        }
                    }
                    throw new ClosedChannelException();
                } finally {
                    if (this.ssc.isOpen()) {
                        this.ssc.configureBlocking(true);
                    }
                }
            } catch (Exception e) {
                Net.translateException(e);
                return null;
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.ssc.close();
    }

    @Override
    public ServerSocketChannel getChannel() {
        return this.ssc;
    }

    @Override
    public boolean isBound() {
        return this.ssc.isBound();
    }

    @Override
    public boolean isClosed() {
        return !this.ssc.isOpen();
    }

    @Override
    public void setSoTimeout(int i) throws SocketException {
        this.timeout = i;
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return this.timeout;
    }

    @Override
    public void setReuseAddress(boolean z) throws SocketException {
        try {
            this.ssc.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.valueOf(z));
        } catch (IOException e) {
            Net.translateToSocketException(e);
        }
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        try {
            return ((Boolean) this.ssc.getOption(StandardSocketOptions.SO_REUSEADDR)).booleanValue();
        } catch (IOException e) {
            Net.translateToSocketException(e);
            return false;
        }
    }

    @Override
    public String toString() {
        if (!isBound()) {
            return "ServerSocket[unbound]";
        }
        return "ServerSocket[addr=" + ((Object) getInetAddress()) + ",localport=" + getLocalPort() + "]";
    }

    @Override
    public void setReceiveBufferSize(int i) throws SocketException {
        if (i <= 0) {
            throw new IllegalArgumentException("size cannot be 0 or negative");
        }
        try {
            this.ssc.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(i));
        } catch (IOException e) {
            Net.translateToSocketException(e);
        }
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        try {
            return ((Integer) this.ssc.getOption(StandardSocketOptions.SO_RCVBUF)).intValue();
        } catch (IOException e) {
            Net.translateToSocketException(e);
            return -1;
        }
    }
}
