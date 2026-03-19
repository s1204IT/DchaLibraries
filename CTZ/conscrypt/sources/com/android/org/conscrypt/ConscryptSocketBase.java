package com.android.org.conscrypt;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;

abstract class ConscryptSocketBase extends AbstractConscryptSocket {
    private final boolean autoClose;
    private final List<HandshakeCompletedListener> listeners;
    private String peerHostname;
    private final PeerInfoProvider peerInfoProvider;
    private final int peerPort;
    private int readTimeoutMilliseconds;
    final Socket socket;

    abstract SSLSession getActiveSession();

    abstract void setApplicationProtocolSelector(ApplicationProtocolSelectorAdapter applicationProtocolSelectorAdapter);

    ConscryptSocketBase() throws IOException {
        this.peerInfoProvider = new PeerInfoProvider() {
            @Override
            String getHostname() {
                return ConscryptSocketBase.this.getHostname();
            }

            @Override
            String getHostnameOrIP() {
                return ConscryptSocketBase.this.getHostnameOrIP();
            }

            @Override
            int getPort() {
                return ConscryptSocketBase.this.getPort();
            }
        };
        this.listeners = new ArrayList(2);
        this.socket = this;
        this.peerHostname = null;
        this.peerPort = -1;
        this.autoClose = false;
    }

    ConscryptSocketBase(String str, int i) throws IOException {
        super(str, i);
        this.peerInfoProvider = new PeerInfoProvider() {
            @Override
            String getHostname() {
                return ConscryptSocketBase.this.getHostname();
            }

            @Override
            String getHostnameOrIP() {
                return ConscryptSocketBase.this.getHostnameOrIP();
            }

            @Override
            int getPort() {
                return ConscryptSocketBase.this.getPort();
            }
        };
        this.listeners = new ArrayList(2);
        this.socket = this;
        this.peerHostname = str;
        this.peerPort = i;
        this.autoClose = false;
    }

    ConscryptSocketBase(InetAddress inetAddress, int i) throws IOException {
        super(inetAddress, i);
        this.peerInfoProvider = new PeerInfoProvider() {
            @Override
            String getHostname() {
                return ConscryptSocketBase.this.getHostname();
            }

            @Override
            String getHostnameOrIP() {
                return ConscryptSocketBase.this.getHostnameOrIP();
            }

            @Override
            int getPort() {
                return ConscryptSocketBase.this.getPort();
            }
        };
        this.listeners = new ArrayList(2);
        this.socket = this;
        this.peerHostname = null;
        this.peerPort = -1;
        this.autoClose = false;
    }

    ConscryptSocketBase(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        super(str, i, inetAddress, i2);
        this.peerInfoProvider = new PeerInfoProvider() {
            @Override
            String getHostname() {
                return ConscryptSocketBase.this.getHostname();
            }

            @Override
            String getHostnameOrIP() {
                return ConscryptSocketBase.this.getHostnameOrIP();
            }

            @Override
            int getPort() {
                return ConscryptSocketBase.this.getPort();
            }
        };
        this.listeners = new ArrayList(2);
        this.socket = this;
        this.peerHostname = str;
        this.peerPort = i;
        this.autoClose = false;
    }

    ConscryptSocketBase(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        super(inetAddress, i, inetAddress2, i2);
        this.peerInfoProvider = new PeerInfoProvider() {
            @Override
            String getHostname() {
                return ConscryptSocketBase.this.getHostname();
            }

            @Override
            String getHostnameOrIP() {
                return ConscryptSocketBase.this.getHostnameOrIP();
            }

            @Override
            int getPort() {
                return ConscryptSocketBase.this.getPort();
            }
        };
        this.listeners = new ArrayList(2);
        this.socket = this;
        this.peerHostname = null;
        this.peerPort = -1;
        this.autoClose = false;
    }

    ConscryptSocketBase(Socket socket, String str, int i, boolean z) throws IOException {
        this.peerInfoProvider = new PeerInfoProvider() {
            @Override
            String getHostname() {
                return ConscryptSocketBase.this.getHostname();
            }

            @Override
            String getHostnameOrIP() {
                return ConscryptSocketBase.this.getHostnameOrIP();
            }

            @Override
            int getPort() {
                return ConscryptSocketBase.this.getPort();
            }
        };
        this.listeners = new ArrayList(2);
        this.socket = (Socket) Preconditions.checkNotNull(socket, "socket");
        this.peerHostname = str;
        this.peerPort = i;
        this.autoClose = z;
    }

    @Override
    public final void connect(SocketAddress socketAddress) throws IOException {
        connect(socketAddress, 0);
    }

    @Override
    public final void connect(SocketAddress socketAddress, int i) throws IOException {
        if (this.peerHostname == null && (socketAddress instanceof InetSocketAddress)) {
            this.peerHostname = Platform.getHostStringFromInetSocketAddress((InetSocketAddress) socketAddress);
        }
        if (isDelegating()) {
            this.socket.connect(socketAddress, i);
        } else {
            super.connect(socketAddress, i);
        }
    }

    @Override
    public void bind(SocketAddress socketAddress) throws IOException {
        if (isDelegating()) {
            this.socket.bind(socketAddress);
        } else {
            super.bind(socketAddress);
        }
    }

    @Override
    public void close() throws IOException {
        if (isDelegating()) {
            if (this.autoClose && !this.socket.isClosed()) {
                this.socket.close();
                return;
            }
            return;
        }
        if (!super.isClosed()) {
            super.close();
        }
    }

    @Override
    public InetAddress getInetAddress() {
        if (isDelegating()) {
            return this.socket.getInetAddress();
        }
        return super.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        if (isDelegating()) {
            return this.socket.getLocalAddress();
        }
        return super.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        if (isDelegating()) {
            return this.socket.getLocalPort();
        }
        return super.getLocalPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        if (isDelegating()) {
            return this.socket.getRemoteSocketAddress();
        }
        return super.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        if (isDelegating()) {
            return this.socket.getLocalSocketAddress();
        }
        return super.getLocalSocketAddress();
    }

    @Override
    public final int getPort() {
        if (isDelegating()) {
            return this.socket.getPort();
        }
        if (this.peerPort != -1) {
            return this.peerPort;
        }
        return super.getPort();
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        Preconditions.checkArgument(handshakeCompletedListener != null, "Provided listener is null");
        this.listeners.add(handshakeCompletedListener);
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        Preconditions.checkArgument(handshakeCompletedListener != null, "Provided listener is null");
        if (!this.listeners.remove(handshakeCompletedListener)) {
            throw new IllegalArgumentException("Provided listener is not registered");
        }
    }

    @Override
    public FileDescriptor getFileDescriptor$() {
        if (isDelegating()) {
            return Platform.getFileDescriptor(this.socket);
        }
        return Platform.getFileDescriptorFromSSLSocket(this);
    }

    @Override
    public final void setSoTimeout(int i) throws SocketException {
        if (isDelegating()) {
            this.socket.setSoTimeout(i);
        } else {
            super.setSoTimeout(i);
            this.readTimeoutMilliseconds = i;
        }
    }

    @Override
    public final int getSoTimeout() throws SocketException {
        if (isDelegating()) {
            return this.socket.getSoTimeout();
        }
        return this.readTimeoutMilliseconds;
    }

    @Override
    public final void sendUrgentData(int i) throws IOException {
        throw new SocketException("Method sendUrgentData() is not supported.");
    }

    @Override
    public final void setOOBInline(boolean z) throws SocketException {
        throw new SocketException("Method setOOBInline() is not supported.");
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return false;
    }

    @Override
    public SocketChannel getChannel() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (isDelegating()) {
            return this.socket.getInputStream();
        }
        return super.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (isDelegating()) {
            return this.socket.getOutputStream();
        }
        return super.getOutputStream();
    }

    @Override
    public void setTcpNoDelay(boolean z) throws SocketException {
        if (isDelegating()) {
            this.socket.setTcpNoDelay(z);
        } else {
            super.setTcpNoDelay(z);
        }
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        if (isDelegating()) {
            return this.socket.getTcpNoDelay();
        }
        return super.getTcpNoDelay();
    }

    @Override
    public void setSoLinger(boolean z, int i) throws SocketException {
        if (isDelegating()) {
            this.socket.setSoLinger(z, i);
        } else {
            super.setSoLinger(z, i);
        }
    }

    @Override
    public int getSoLinger() throws SocketException {
        if (isDelegating()) {
            return this.socket.getSoLinger();
        }
        return super.getSoLinger();
    }

    @Override
    public void setSendBufferSize(int i) throws SocketException {
        if (isDelegating()) {
            this.socket.setSendBufferSize(i);
        } else {
            super.setSendBufferSize(i);
        }
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        if (isDelegating()) {
            return this.socket.getSendBufferSize();
        }
        return super.getSendBufferSize();
    }

    @Override
    public void setReceiveBufferSize(int i) throws SocketException {
        if (isDelegating()) {
            this.socket.setReceiveBufferSize(i);
        } else {
            super.setReceiveBufferSize(i);
        }
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        if (isDelegating()) {
            return this.socket.getReceiveBufferSize();
        }
        return super.getReceiveBufferSize();
    }

    @Override
    public void setKeepAlive(boolean z) throws SocketException {
        if (isDelegating()) {
            this.socket.setKeepAlive(z);
        } else {
            super.setKeepAlive(z);
        }
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        if (isDelegating()) {
            return this.socket.getKeepAlive();
        }
        return super.getKeepAlive();
    }

    @Override
    public void setTrafficClass(int i) throws SocketException {
        if (isDelegating()) {
            this.socket.setTrafficClass(i);
        } else {
            super.setTrafficClass(i);
        }
    }

    @Override
    public int getTrafficClass() throws SocketException {
        if (isDelegating()) {
            return this.socket.getTrafficClass();
        }
        return super.getTrafficClass();
    }

    @Override
    public void setReuseAddress(boolean z) throws SocketException {
        if (isDelegating()) {
            this.socket.setReuseAddress(z);
        } else {
            super.setReuseAddress(z);
        }
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        if (isDelegating()) {
            return this.socket.getReuseAddress();
        }
        return super.getReuseAddress();
    }

    @Override
    public void shutdownInput() throws IOException {
        if (isDelegating()) {
            this.socket.shutdownInput();
        } else {
            super.shutdownInput();
        }
    }

    @Override
    public void shutdownOutput() throws IOException {
        if (isDelegating()) {
            this.socket.shutdownOutput();
        } else {
            super.shutdownOutput();
        }
    }

    @Override
    public boolean isConnected() {
        if (isDelegating()) {
            return this.socket.isConnected();
        }
        return super.isConnected();
    }

    @Override
    public boolean isBound() {
        if (isDelegating()) {
            return this.socket.isBound();
        }
        return super.isBound();
    }

    @Override
    public boolean isClosed() {
        if (isDelegating()) {
            return this.socket.isClosed();
        }
        return super.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        if (isDelegating()) {
            return this.socket.isInputShutdown();
        }
        return super.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        if (isDelegating()) {
            return this.socket.isOutputShutdown();
        }
        return super.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(int i, int i2, int i3) {
        if (isDelegating()) {
            this.socket.setPerformancePreferences(i, i2, i3);
        } else {
            super.setPerformancePreferences(i, i2, i3);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SSL socket over ");
        if (isDelegating()) {
            sb.append(this.socket.toString());
        } else {
            sb.append(super.toString());
        }
        return sb.toString();
    }

    @Override
    String getHostname() {
        return this.peerHostname;
    }

    @Override
    void setHostname(String str) {
        this.peerHostname = str;
    }

    @Override
    String getHostnameOrIP() {
        if (this.peerHostname != null) {
            return this.peerHostname;
        }
        InetAddress inetAddress = getInetAddress();
        if (inetAddress != null) {
            return Platform.getOriginalHostNameFromInetAddress(inetAddress);
        }
        return null;
    }

    @Override
    void setSoWriteTimeout(int i) throws SocketException {
        throw new SocketException("Method setSoWriteTimeout() is not supported.");
    }

    @Override
    int getSoWriteTimeout() throws SocketException {
        return 0;
    }

    @Override
    void setHandshakeTimeout(int i) throws SocketException {
        throw new SocketException("Method setHandshakeTimeout() is not supported.");
    }

    final void checkOpen() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
    }

    @Override
    final PeerInfoProvider peerInfoProvider() {
        return this.peerInfoProvider;
    }

    final void notifyHandshakeCompletedListeners() {
        if (this.listeners != null && !this.listeners.isEmpty()) {
            HandshakeCompletedEvent handshakeCompletedEvent = new HandshakeCompletedEvent(this, getActiveSession());
            Iterator<HandshakeCompletedListener> it = this.listeners.iterator();
            while (it.hasNext()) {
                try {
                    it.next().handshakeCompleted(handshakeCompletedEvent);
                } catch (RuntimeException e) {
                    Thread threadCurrentThread = Thread.currentThread();
                    threadCurrentThread.getUncaughtExceptionHandler().uncaughtException(threadCurrentThread, e);
                }
            }
        }
    }

    private boolean isDelegating() {
        return (this.socket == null || this.socket == this) ? false : true;
    }
}
