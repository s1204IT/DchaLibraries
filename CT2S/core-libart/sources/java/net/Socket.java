package java.net;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.nio.channels.SocketChannel;
import libcore.io.IoBridge;

public class Socket implements Closeable {
    private static SocketImplFactory factory;
    private final Object connectLock;
    final SocketImpl impl;
    private boolean isBound;
    private boolean isClosed;
    private boolean isConnected;
    volatile boolean isCreated;
    private boolean isInputShutdown;
    private boolean isOutputShutdown;
    private InetAddress localAddress;
    private final Proxy proxy;

    public Socket() {
        this.isCreated = false;
        this.isBound = false;
        this.isConnected = false;
        this.isClosed = false;
        this.isInputShutdown = false;
        this.isOutputShutdown = false;
        this.localAddress = Inet4Address.ANY;
        this.connectLock = new Object();
        this.impl = factory != null ? factory.createSocketImpl() : new PlainSocketImpl();
        this.proxy = null;
    }

    public Socket(Proxy proxy) {
        this.isCreated = false;
        this.isBound = false;
        this.isConnected = false;
        this.isClosed = false;
        this.isInputShutdown = false;
        this.isOutputShutdown = false;
        this.localAddress = Inet4Address.ANY;
        this.connectLock = new Object();
        if (proxy == null || proxy.type() == Proxy.Type.HTTP) {
            throw new IllegalArgumentException("Invalid proxy: " + proxy);
        }
        this.proxy = proxy;
        this.impl = factory != null ? factory.createSocketImpl() : new PlainSocketImpl(proxy);
    }

    private void tryAllAddresses(String dstName, int dstPort, InetAddress localAddress, int localPort, boolean streaming) throws IOException {
        InetAddress[] dstAddresses = InetAddress.getAllByName(dstName);
        for (int i = 0; i < dstAddresses.length - 1; i++) {
            InetAddress dstAddress = dstAddresses[i];
            try {
                checkDestination(dstAddress, dstPort);
                startupSocket(dstAddress, dstPort, localAddress, localPort, streaming);
                return;
            } catch (IOException e) {
            }
        }
        InetAddress dstAddress2 = dstAddresses[dstAddresses.length - 1];
        checkDestination(dstAddress2, dstPort);
        startupSocket(dstAddress2, dstPort, localAddress, localPort, streaming);
    }

    public Socket(String dstName, int dstPort) throws IOException {
        this(dstName, dstPort, (InetAddress) null, 0);
    }

    public Socket(String dstName, int dstPort, InetAddress localAddress, int localPort) throws IOException {
        this();
        tryAllAddresses(dstName, dstPort, localAddress, localPort, true);
    }

    @Deprecated
    public Socket(String hostName, int port, boolean streaming) throws IOException {
        this();
        tryAllAddresses(hostName, port, null, 0, streaming);
    }

    public Socket(InetAddress dstAddress, int dstPort) throws IOException {
        this();
        checkDestination(dstAddress, dstPort);
        startupSocket(dstAddress, dstPort, null, 0, true);
    }

    public Socket(InetAddress dstAddress, int dstPort, InetAddress localAddress, int localPort) throws IOException {
        this();
        checkDestination(dstAddress, dstPort);
        startupSocket(dstAddress, dstPort, localAddress, localPort, true);
    }

    @Deprecated
    public Socket(InetAddress addr, int port, boolean streaming) throws IOException {
        this();
        checkDestination(addr, port);
        startupSocket(addr, port, null, 0, streaming);
    }

    protected Socket(SocketImpl impl) throws SocketException {
        this.isCreated = false;
        this.isBound = false;
        this.isConnected = false;
        this.isClosed = false;
        this.isInputShutdown = false;
        this.isOutputShutdown = false;
        this.localAddress = Inet4Address.ANY;
        this.connectLock = new Object();
        this.impl = impl;
        this.proxy = null;
    }

    private void checkDestination(InetAddress destAddr, int dstPort) {
        if (dstPort < 0 || dstPort > 65535) {
            throw new IllegalArgumentException("Port out of range: " + dstPort);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        this.isClosed = true;
        this.isConnected = false;
        this.localAddress = Inet4Address.ANY;
        this.impl.close();
    }

    public void onClose() {
        this.isClosed = true;
        this.isConnected = false;
        this.localAddress = Inet4Address.ANY;
        this.impl.onClose();
    }

    public InetAddress getInetAddress() {
        if (isConnected()) {
            return this.impl.getInetAddress();
        }
        return null;
    }

    public InputStream getInputStream() throws IOException {
        checkOpenAndCreate(false);
        if (isInputShutdown()) {
            throw new SocketException("Socket input is shutdown");
        }
        return this.impl.getInputStream();
    }

    public boolean getKeepAlive() throws SocketException {
        checkOpenAndCreate(true);
        return ((Boolean) this.impl.getOption(8)).booleanValue();
    }

    public InetAddress getLocalAddress() {
        return this.localAddress;
    }

    public int getLocalPort() {
        if (isBound()) {
            return this.impl.getLocalPort();
        }
        return -1;
    }

    public OutputStream getOutputStream() throws IOException {
        checkOpenAndCreate(false);
        if (isOutputShutdown()) {
            throw new SocketException("Socket output is shutdown");
        }
        return this.impl.getOutputStream();
    }

    public int getPort() {
        if (isConnected()) {
            return this.impl.getPort();
        }
        return 0;
    }

    public int getSoLinger() throws SocketException {
        checkOpenAndCreate(true);
        Object value = this.impl.getOption(128);
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        return -1;
    }

    public synchronized int getReceiveBufferSize() throws SocketException {
        checkOpenAndCreate(true);
        return ((Integer) this.impl.getOption(SocketOptions.SO_RCVBUF)).intValue();
    }

    public synchronized int getSendBufferSize() throws SocketException {
        checkOpenAndCreate(true);
        return ((Integer) this.impl.getOption(SocketOptions.SO_SNDBUF)).intValue();
    }

    public synchronized int getSoTimeout() throws SocketException {
        checkOpenAndCreate(true);
        return ((Integer) this.impl.getOption(SocketOptions.SO_TIMEOUT)).intValue();
    }

    public boolean getTcpNoDelay() throws SocketException {
        checkOpenAndCreate(true);
        return ((Boolean) this.impl.getOption(1)).booleanValue();
    }

    public void setKeepAlive(boolean keepAlive) throws SocketException {
        if (this.impl != null) {
            checkOpenAndCreate(true);
            this.impl.setOption(8, Boolean.valueOf(keepAlive));
        }
    }

    public static synchronized void setSocketImplFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new SocketException("Factory already set");
        }
        factory = fac;
    }

    public synchronized void setSendBufferSize(int size) throws SocketException {
        checkOpenAndCreate(true);
        if (size < 1) {
            throw new IllegalArgumentException("size < 1");
        }
        this.impl.setOption(SocketOptions.SO_SNDBUF, Integer.valueOf(size));
    }

    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        checkOpenAndCreate(true);
        if (size < 1) {
            throw new IllegalArgumentException("size < 1");
        }
        this.impl.setOption(SocketOptions.SO_RCVBUF, Integer.valueOf(size));
    }

    public void setSoLinger(boolean on, int timeout) throws SocketException {
        checkOpenAndCreate(true);
        if (on && timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        if (on) {
            this.impl.setOption(128, Integer.valueOf(timeout));
        } else {
            this.impl.setOption(128, Boolean.FALSE);
        }
    }

    public synchronized void setSoTimeout(int timeout) throws SocketException {
        checkOpenAndCreate(true);
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        this.impl.setOption(SocketOptions.SO_TIMEOUT, Integer.valueOf(timeout));
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        checkOpenAndCreate(true);
        this.impl.setOption(1, Boolean.valueOf(on));
    }

    private void startupSocket(InetAddress dstAddress, int dstPort, InetAddress localAddress, int localPort, boolean streaming) throws IOException {
        if (localPort < 0 || localPort > 65535) {
            throw new IllegalArgumentException("Local port out of range: " + localPort);
        }
        InetAddress addr = localAddress == null ? Inet4Address.ANY : localAddress;
        synchronized (this) {
            this.impl.create(streaming);
            this.isCreated = true;
            if (streaming) {
                try {
                    if (!usingSocks()) {
                        this.impl.bind(addr, localPort);
                    }
                    this.isBound = true;
                    cacheLocalAddress();
                    this.impl.connect(dstAddress, dstPort);
                    this.isConnected = true;
                    cacheLocalAddress();
                } catch (IOException e) {
                    this.impl.close();
                    throw e;
                }
            }
        }
    }

    private boolean usingSocks() {
        return this.proxy != null && this.proxy.type() == Proxy.Type.SOCKS;
    }

    public String toString() {
        return !isConnected() ? "Socket[unconnected]" : this.impl.toString();
    }

    public void shutdownInput() throws IOException {
        if (isInputShutdown()) {
            throw new SocketException("Socket input is shutdown");
        }
        checkOpenAndCreate(false);
        this.impl.shutdownInput();
        this.isInputShutdown = true;
    }

    public void shutdownOutput() throws IOException {
        if (isOutputShutdown()) {
            throw new SocketException("Socket output is shutdown");
        }
        checkOpenAndCreate(false);
        this.impl.shutdownOutput();
        this.isOutputShutdown = true;
    }

    private void checkOpenAndCreate(boolean create) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!create) {
            if (!isConnected()) {
                throw new SocketException("Socket is not connected");
            }
            return;
        }
        if (!this.isCreated) {
            synchronized (this) {
                if (!this.isCreated) {
                    try {
                        this.impl.create(true);
                        this.isCreated = true;
                    } catch (SocketException e) {
                        throw e;
                    } catch (IOException e2) {
                        throw new SocketException(e2.toString());
                    }
                }
            }
        }
    }

    public SocketAddress getLocalSocketAddress() {
        if (isBound()) {
            return new InetSocketAddress(getLocalAddress(), getLocalPort());
        }
        return null;
    }

    public SocketAddress getRemoteSocketAddress() {
        if (isConnected()) {
            return new InetSocketAddress(getInetAddress(), getPort());
        }
        return null;
    }

    public boolean isBound() {
        return this.isBound;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public void bind(SocketAddress localAddr) throws IOException {
        InetAddress addr;
        int port;
        checkOpenAndCreate(true);
        if (isBound()) {
            throw new BindException("Socket is already bound");
        }
        if (localAddr == null) {
            port = 0;
            addr = Inet4Address.ANY;
        } else {
            if (!(localAddr instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("Local address not an InetSocketAddress: " + localAddr.getClass());
            }
            InetSocketAddress inetAddr = (InetSocketAddress) localAddr;
            addr = inetAddr.getAddress();
            if (addr == null) {
                throw new UnknownHostException("Host is unresolved: " + inetAddr.getHostName());
            }
            port = inetAddr.getPort();
        }
        synchronized (this) {
            try {
                this.impl.bind(addr, port);
                this.isBound = true;
                cacheLocalAddress();
            } catch (IOException e) {
                this.impl.close();
                throw e;
            }
        }
    }

    public void onBind(InetAddress localAddress, int localPort) {
        this.isBound = true;
        this.localAddress = localAddress;
        this.impl.onBind(localAddress, localPort);
    }

    public void connect(SocketAddress remoteAddr) throws IOException {
        connect(remoteAddr, 0);
    }

    public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
        checkOpenAndCreate(true);
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        if (isConnected()) {
            throw new SocketException("Already connected");
        }
        if (remoteAddr == null) {
            throw new IllegalArgumentException("remoteAddr == null");
        }
        if (!(remoteAddr instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Remote address not an InetSocketAddress: " + remoteAddr.getClass());
        }
        InetSocketAddress inetAddr = (InetSocketAddress) remoteAddr;
        InetAddress addr = inetAddr.getAddress();
        if (addr == null) {
            throw new UnknownHostException("Host is unresolved: " + inetAddr.getHostName());
        }
        int port = inetAddr.getPort();
        checkDestination(addr, port);
        synchronized (this.connectLock) {
            try {
                if (!isBound()) {
                    if (!usingSocks()) {
                        this.impl.bind(Inet4Address.ANY, 0);
                    }
                    this.isBound = true;
                }
                this.impl.connect(remoteAddr, timeout);
                this.isConnected = true;
                cacheLocalAddress();
            } catch (IOException e) {
                this.impl.close();
                throw e;
            }
        }
    }

    public void onConnect(InetAddress remoteAddress, int remotePort) {
        this.isConnected = true;
        this.impl.onConnect(remoteAddress, remotePort);
    }

    public boolean isInputShutdown() {
        return this.isInputShutdown;
    }

    public boolean isOutputShutdown() {
        return this.isOutputShutdown;
    }

    public void setReuseAddress(boolean reuse) throws SocketException {
        checkOpenAndCreate(true);
        this.impl.setOption(4, Boolean.valueOf(reuse));
    }

    public boolean getReuseAddress() throws SocketException {
        checkOpenAndCreate(true);
        return ((Boolean) this.impl.getOption(4)).booleanValue();
    }

    public void setOOBInline(boolean oobinline) throws SocketException {
        checkOpenAndCreate(true);
        this.impl.setOption(SocketOptions.SO_OOBINLINE, Boolean.valueOf(oobinline));
    }

    public boolean getOOBInline() throws SocketException {
        checkOpenAndCreate(true);
        return ((Boolean) this.impl.getOption(SocketOptions.SO_OOBINLINE)).booleanValue();
    }

    public void setTrafficClass(int value) throws SocketException {
        checkOpenAndCreate(true);
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Value doesn't fit in an unsigned byte: " + value);
        }
        this.impl.setOption(3, Integer.valueOf(value));
    }

    public int getTrafficClass() throws SocketException {
        checkOpenAndCreate(true);
        return ((Integer) this.impl.getOption(3)).intValue();
    }

    public void sendUrgentData(int value) throws IOException {
        this.impl.sendUrgentData(value);
    }

    void accepted() throws SocketException {
        this.isConnected = true;
        this.isBound = true;
        this.isCreated = true;
        cacheLocalAddress();
    }

    private void cacheLocalAddress() throws SocketException {
        this.localAddress = IoBridge.getSocketLocalAddress(this.impl.fd);
    }

    public SocketChannel getChannel() {
        return null;
    }

    public FileDescriptor getFileDescriptor$() {
        return this.impl.fd;
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }
}
