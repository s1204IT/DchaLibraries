package java.net;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import sun.net.ConnectionResetException;
import sun.net.NetHooks;
import sun.net.ResourceManager;

abstract class AbstractPlainSocketImpl extends SocketImpl {
    public static final int SHUT_RD = 0;
    public static final int SHUT_WR = 1;
    private int resetState;
    protected boolean stream;
    int timeout;
    private boolean shut_rd = false;
    private boolean shut_wr = false;
    private SocketInputStream socketInputStream = null;
    private SocketOutputStream socketOutputStream = null;
    protected int fdUseCount = 0;

    @ReachabilitySensitive
    protected final Object fdLock = new Object();
    protected boolean closePending = false;
    private int CONNECTION_NOT_RESET = 0;
    private int CONNECTION_RESET_PENDING = 1;
    private int CONNECTION_RESET = 2;
    private final Object resetLock = new Object();

    @ReachabilitySensitive
    private final CloseGuard guard = CloseGuard.get();

    abstract void socketAccept(SocketImpl socketImpl) throws IOException;

    abstract int socketAvailable() throws IOException;

    abstract void socketBind(InetAddress inetAddress, int i) throws IOException;

    abstract void socketClose0(boolean z) throws IOException;

    abstract void socketConnect(InetAddress inetAddress, int i, int i2) throws IOException;

    abstract void socketCreate(boolean z) throws IOException;

    abstract Object socketGetOption(int i) throws SocketException;

    abstract void socketListen(int i) throws IOException;

    abstract void socketSendUrgentData(int i) throws IOException;

    abstract void socketSetOption(int i, Object obj) throws SocketException;

    abstract void socketShutdown(int i) throws IOException;

    AbstractPlainSocketImpl() {
    }

    @Override
    protected synchronized void create(boolean z) throws IOException {
        this.stream = z;
        if (!z) {
            ResourceManager.beforeUdpCreate();
            try {
                socketCreate(false);
            } catch (IOException e) {
                ResourceManager.afterUdpClose();
                throw e;
            }
        } else {
            socketCreate(true);
        }
        if (this.socket != null) {
            this.socket.setCreated();
        }
        if (this.serverSocket != null) {
            this.serverSocket.setCreated();
        }
        if (this.fd != null && this.fd.valid()) {
            this.guard.open("close");
        }
    }

    @Override
    protected void connect(String str, int i) throws IOException {
        try {
            InetAddress byName = InetAddress.getByName(str);
            this.port = i;
            this.address = byName;
            connectToAddress(byName, i, this.timeout);
        } catch (Throwable th) {
            try {
                close();
            } catch (IOException e) {
            }
            throw th;
        }
    }

    @Override
    protected void connect(InetAddress inetAddress, int i) throws IOException {
        this.port = i;
        this.address = inetAddress;
        try {
            connectToAddress(inetAddress, i, this.timeout);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    @Override
    protected void connect(SocketAddress socketAddress, int i) throws IOException {
        if (socketAddress != null) {
            try {
                if (socketAddress instanceof InetSocketAddress) {
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                    if (inetSocketAddress.isUnresolved()) {
                        throw new UnknownHostException(inetSocketAddress.getHostName());
                    }
                    this.port = inetSocketAddress.getPort();
                    this.address = inetSocketAddress.getAddress();
                    connectToAddress(this.address, this.port, i);
                    return;
                }
            } catch (Throwable th) {
                try {
                    close();
                } catch (IOException e) {
                }
                throw th;
            }
        }
        throw new IllegalArgumentException("unsupported address type");
    }

    private void connectToAddress(InetAddress inetAddress, int i, int i2) throws IOException {
        if (inetAddress.isAnyLocalAddress()) {
            doConnect(InetAddress.getLocalHost(), i, i2);
        } else {
            doConnect(inetAddress, i, i2);
        }
    }

    @Override
    public void setOption(int i, Object obj) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        if (i == 4102) {
            this.timeout = ((Integer) obj).intValue();
        }
        socketSetOption(i, obj);
    }

    @Override
    public Object getOption(int i) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        if (i == 4102) {
            return new Integer(this.timeout);
        }
        return socketGetOption(i);
    }

    synchronized void doConnect(InetAddress inetAddress, int i, int i2) throws IOException {
        synchronized (this.fdLock) {
            if (!this.closePending && (this.socket == null || !this.socket.isBound())) {
                NetHooks.beforeTcpConnect(this.fd, inetAddress, i);
            }
        }
        try {
            acquireFD();
            try {
                BlockGuard.getThreadPolicy().onNetwork();
                socketConnect(inetAddress, i, i2);
                synchronized (this.fdLock) {
                    if (this.closePending) {
                        throw new SocketException("Socket closed");
                    }
                }
                if (this.socket != null) {
                    this.socket.setBound();
                    this.socket.setConnected();
                }
            } finally {
                releaseFD();
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    @Override
    protected synchronized void bind(InetAddress inetAddress, int i) throws IOException {
        synchronized (this.fdLock) {
            if (!this.closePending && (this.socket == null || !this.socket.isBound())) {
                NetHooks.beforeTcpBind(this.fd, inetAddress, i);
            }
        }
        socketBind(inetAddress, i);
        if (this.socket != null) {
            this.socket.setBound();
        }
        if (this.serverSocket != null) {
            this.serverSocket.setBound();
        }
    }

    @Override
    protected synchronized void listen(int i) throws IOException {
        socketListen(i);
    }

    @Override
    protected void accept(SocketImpl socketImpl) throws IOException {
        acquireFD();
        try {
            BlockGuard.getThreadPolicy().onNetwork();
            socketAccept(socketImpl);
        } finally {
            releaseFD();
        }
    }

    @Override
    protected synchronized InputStream getInputStream() throws IOException {
        synchronized (this.fdLock) {
            if (isClosedOrPending()) {
                throw new IOException("Socket Closed");
            }
            if (this.shut_rd) {
                throw new IOException("Socket input is shutdown");
            }
            if (this.socketInputStream == null) {
                this.socketInputStream = new SocketInputStream(this);
            }
        }
        return this.socketInputStream;
    }

    void setInputStream(SocketInputStream socketInputStream) {
        this.socketInputStream = socketInputStream;
    }

    @Override
    protected synchronized OutputStream getOutputStream() throws IOException {
        synchronized (this.fdLock) {
            if (isClosedOrPending()) {
                throw new IOException("Socket Closed");
            }
            if (this.shut_wr) {
                throw new IOException("Socket output is shutdown");
            }
            if (this.socketOutputStream == null) {
                this.socketOutputStream = new SocketOutputStream(this);
            }
        }
        return this.socketOutputStream;
    }

    void setFileDescriptor(FileDescriptor fileDescriptor) {
        this.fd = fileDescriptor;
    }

    void setAddress(InetAddress inetAddress) {
        this.address = inetAddress;
    }

    void setPort(int i) {
        this.port = i;
    }

    void setLocalPort(int i) {
        this.localport = i;
    }

    @Override
    protected synchronized int available() throws IOException {
        int iSocketAvailable;
        if (isClosedOrPending()) {
            throw new IOException("Stream closed.");
        }
        if (isConnectionReset() || this.shut_rd) {
            return 0;
        }
        try {
            iSocketAvailable = socketAvailable();
            if (iSocketAvailable == 0) {
                try {
                    if (isConnectionResetPending()) {
                        setConnectionReset();
                    }
                } catch (ConnectionResetException e) {
                    setConnectionResetPending();
                    try {
                        int iSocketAvailable2 = socketAvailable();
                        if (iSocketAvailable2 == 0) {
                            try {
                                setConnectionReset();
                                iSocketAvailable = iSocketAvailable2;
                            } catch (ConnectionResetException e2) {
                                iSocketAvailable = iSocketAvailable2;
                            }
                        } else {
                            iSocketAvailable = iSocketAvailable2;
                        }
                    } catch (ConnectionResetException e3) {
                    }
                }
            }
        } catch (ConnectionResetException e4) {
            iSocketAvailable = 0;
        }
        return iSocketAvailable;
    }

    @Override
    protected void close() throws IOException {
        synchronized (this.fdLock) {
            if (this.fd != null && this.fd.valid()) {
                if (!this.stream) {
                    ResourceManager.afterUdpClose();
                }
                if (!this.closePending) {
                    this.closePending = true;
                    this.guard.close();
                    if (this.fdUseCount != 0) {
                        this.fdUseCount--;
                        socketPreClose();
                    } else {
                        try {
                            socketPreClose();
                        } finally {
                            socketClose();
                        }
                    }
                }
            }
        }
    }

    @Override
    void reset() throws IOException {
        if (this.fd != null && this.fd.valid()) {
            socketClose();
            this.guard.close();
        }
        super.reset();
    }

    @Override
    protected void shutdownInput() throws IOException {
        if (this.fd != null && this.fd.valid()) {
            socketShutdown(0);
            if (this.socketInputStream != null) {
                this.socketInputStream.setEOF(true);
            }
            this.shut_rd = true;
        }
    }

    @Override
    protected void shutdownOutput() throws IOException {
        if (this.fd != null && this.fd.valid()) {
            socketShutdown(1);
            this.shut_wr = true;
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        return true;
    }

    @Override
    protected void sendUrgentData(int i) throws IOException {
        if (this.fd == null || !this.fd.valid()) {
            throw new IOException("Socket Closed");
        }
        socketSendUrgentData(i);
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        close();
    }

    FileDescriptor acquireFD() {
        FileDescriptor fileDescriptor;
        synchronized (this.fdLock) {
            this.fdUseCount++;
            fileDescriptor = this.fd;
        }
        return fileDescriptor;
    }

    void releaseFD() {
        synchronized (this.fdLock) {
            this.fdUseCount--;
            if (this.fdUseCount == -1 && this.fd != null) {
                try {
                    socketClose();
                } catch (IOException e) {
                }
            }
        }
    }

    public boolean isConnectionReset() {
        boolean z;
        synchronized (this.resetLock) {
            z = this.resetState == this.CONNECTION_RESET;
        }
        return z;
    }

    public boolean isConnectionResetPending() {
        boolean z;
        synchronized (this.resetLock) {
            z = this.resetState == this.CONNECTION_RESET_PENDING;
        }
        return z;
    }

    public void setConnectionReset() {
        synchronized (this.resetLock) {
            this.resetState = this.CONNECTION_RESET;
        }
    }

    public void setConnectionResetPending() {
        synchronized (this.resetLock) {
            if (this.resetState == this.CONNECTION_NOT_RESET) {
                this.resetState = this.CONNECTION_RESET_PENDING;
            }
        }
    }

    public boolean isClosedOrPending() {
        synchronized (this.fdLock) {
            if (!this.closePending && this.fd != null && this.fd.valid()) {
                return false;
            }
            return true;
        }
    }

    public int getTimeout() {
        return this.timeout;
    }

    private void socketPreClose() throws IOException {
        socketClose0(true);
    }

    protected void socketClose() throws IOException {
        socketClose0(false);
    }
}
