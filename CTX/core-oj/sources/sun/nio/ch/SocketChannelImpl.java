package sun.nio.ch;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jdk.net.ExtendedSocketOptions;
import sun.net.ExtendedOptionsImpl;
import sun.net.NetHooks;

class SocketChannelImpl extends SocketChannel implements SelChImpl {
    static final boolean $assertionsDisabled = false;
    private static final int ST_CONNECTED = 2;
    private static final int ST_KILLED = 4;
    private static final int ST_KILLPENDING = 3;
    private static final int ST_PENDING = 1;
    private static final int ST_UNCONNECTED = 0;
    private static final int ST_UNINITIALIZED = -1;
    private static NativeDispatcher nd = new SocketDispatcher();
    private final FileDescriptor fd;
    private final int fdVal;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private boolean isInputOpen;
    private boolean isOutputOpen;
    private boolean isReuseAddress;
    private InetSocketAddress localAddress;
    private final Object readLock;
    private volatile long readerThread;
    private boolean readyToConnect;
    private InetSocketAddress remoteAddress;
    private Socket socket;
    private int state;
    private final Object stateLock;
    private final Object writeLock;
    private volatile long writerThread;

    private static native int checkConnect(FileDescriptor fileDescriptor, boolean z, boolean z2) throws IOException;

    private static native int sendOutOfBandData(FileDescriptor fileDescriptor, byte b) throws IOException;

    @Override
    public NetworkChannel setOption(SocketOption socketOption, Object obj) throws IOException {
        return setOption((SocketOption<Object>) socketOption, obj);
    }

    SocketChannelImpl(SelectorProvider selectorProvider) throws IOException {
        super(selectorProvider);
        this.readerThread = 0L;
        this.writerThread = 0L;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.isInputOpen = true;
        this.isOutputOpen = true;
        this.readyToConnect = $assertionsDisabled;
        this.guard = CloseGuard.get();
        this.fd = Net.socket(true);
        this.fdVal = IOUtil.fdVal(this.fd);
        this.state = 0;
        if (this.fd != null && this.fd.valid()) {
            this.guard.open("close");
        }
    }

    SocketChannelImpl(SelectorProvider selectorProvider, FileDescriptor fileDescriptor, boolean z) throws IOException {
        super(selectorProvider);
        this.readerThread = 0L;
        this.writerThread = 0L;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.isInputOpen = true;
        this.isOutputOpen = true;
        this.readyToConnect = $assertionsDisabled;
        this.guard = CloseGuard.get();
        this.fd = fileDescriptor;
        this.fdVal = IOUtil.fdVal(fileDescriptor);
        this.state = 0;
        if (fileDescriptor != null && fileDescriptor.valid()) {
            this.guard.open("close");
        }
        if (z) {
            this.localAddress = Net.localAddress(fileDescriptor);
        }
    }

    SocketChannelImpl(SelectorProvider selectorProvider, FileDescriptor fileDescriptor, InetSocketAddress inetSocketAddress) throws IOException {
        super(selectorProvider);
        this.readerThread = 0L;
        this.writerThread = 0L;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.isInputOpen = true;
        this.isOutputOpen = true;
        this.readyToConnect = $assertionsDisabled;
        this.guard = CloseGuard.get();
        this.fd = fileDescriptor;
        this.fdVal = IOUtil.fdVal(fileDescriptor);
        this.state = 2;
        this.localAddress = Net.localAddress(fileDescriptor);
        this.remoteAddress = inetSocketAddress;
        if (fileDescriptor != null && fileDescriptor.valid()) {
            this.guard.open("close");
        }
    }

    @Override
    public Socket socket() {
        Socket socket;
        synchronized (this.stateLock) {
            if (this.socket == null) {
                this.socket = SocketAdaptor.create(this);
            }
            socket = this.socket;
        }
        return socket;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        InetSocketAddress revealedLocalAddress;
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            revealedLocalAddress = Net.getRevealedLocalAddress(this.localAddress);
        }
        return revealedLocalAddress;
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            inetSocketAddress = this.remoteAddress;
        }
        return inetSocketAddress;
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
        if (socketOption == null) {
            throw new NullPointerException();
        }
        if (!supportedOptions().contains(socketOption)) {
            throw new UnsupportedOperationException("'" + ((Object) socketOption) + "' not supported");
        }
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (socketOption == StandardSocketOptions.IP_TOS) {
                Net.setSocketOption(this.fd, Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET, socketOption, t);
                return this;
            }
            if (socketOption == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                this.isReuseAddress = ((Boolean) t).booleanValue();
                return this;
            }
            Net.setSocketOption(this.fd, Net.UNSPEC, socketOption, t);
            return this;
        }
    }

    @Override
    public <T> T getOption(SocketOption<T> socketOption) throws IOException {
        if (socketOption == null) {
            throw new NullPointerException();
        }
        if (!supportedOptions().contains(socketOption)) {
            throw new UnsupportedOperationException("'" + ((Object) socketOption) + "' not supported");
        }
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (socketOption == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                return (T) Boolean.valueOf(this.isReuseAddress);
            }
            if (socketOption == StandardSocketOptions.IP_TOS) {
                return (T) Net.getSocketOption(this.fd, Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET, socketOption);
            }
            return (T) Net.getSocketOption(this.fd, Net.UNSPEC, socketOption);
        }
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private DefaultOptionsHolder() {
        }

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet hashSet = new HashSet(8);
            hashSet.add(StandardSocketOptions.SO_SNDBUF);
            hashSet.add(StandardSocketOptions.SO_RCVBUF);
            hashSet.add(StandardSocketOptions.SO_KEEPALIVE);
            hashSet.add(StandardSocketOptions.SO_REUSEADDR);
            hashSet.add(StandardSocketOptions.SO_LINGER);
            hashSet.add(StandardSocketOptions.TCP_NODELAY);
            hashSet.add(StandardSocketOptions.IP_TOS);
            hashSet.add(ExtendedSocketOption.SO_OOBINLINE);
            if (ExtendedOptionsImpl.flowSupported()) {
                hashSet.add(ExtendedSocketOptions.SO_FLOW_SLA);
            }
            return Collections.unmodifiableSet(hashSet);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    private boolean ensureReadOpen() throws ClosedChannelException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
            if (!this.isInputOpen) {
                return $assertionsDisabled;
            }
            return true;
        }
    }

    private void ensureWriteOpen() throws ClosedChannelException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!this.isOutputOpen) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
        }
    }

    private void readerCleanup() throws IOException {
        synchronized (this.stateLock) {
            this.readerThread = 0L;
            if (this.state == 3) {
                kill();
            }
        }
    }

    private void writerCleanup() throws IOException {
        synchronized (this.stateLock) {
            this.writerThread = 0L;
            if (this.state == 3) {
                kill();
            }
        }
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        int i;
        int i2;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        synchronized (this.readLock) {
            if (!ensureReadOpen()) {
                return -1;
            }
            boolean z = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        readerCleanup();
                        end($assertionsDisabled);
                        synchronized (this.stateLock) {
                            return !this.isInputOpen ? -1 : 0;
                        }
                    }
                    this.readerThread = NativeThread.current();
                    i = 0;
                    while (true) {
                        try {
                            i2 = IOUtil.read(this.fd, byteBuffer, -1L, nd);
                            if (i2 != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                i = i2;
                            } catch (Throwable th) {
                                th = th;
                                i = i2;
                                readerCleanup();
                                if (i <= 0 && i != -2) {
                                    z = false;
                                }
                                end(z);
                                synchronized (this.stateLock) {
                                    if (i <= 0) {
                                        if (!this.isInputOpen) {
                                            return -1;
                                        }
                                    }
                                    throw th;
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    int iNormalize = IOStatus.normalize(i2);
                    readerCleanup();
                    if (i2 <= 0 && i2 != -2) {
                        z = false;
                    }
                    end(z);
                    synchronized (this.stateLock) {
                        if (i2 <= 0) {
                            try {
                                if (!this.isInputOpen) {
                                    return -1;
                                }
                            } catch (Throwable th3) {
                                throw th3;
                            }
                        }
                        return iNormalize;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                i = 0;
            }
        }
    }

    @Override
    public long read(ByteBuffer[] byteBufferArr, int i, int i2) throws IOException {
        long j;
        long j2;
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.readLock) {
            if (!ensureReadOpen()) {
                return -1L;
            }
            boolean z = $assertionsDisabled;
            try {
                begin();
            } catch (Throwable th) {
                th = th;
                j = 0;
            }
            synchronized (this.stateLock) {
                if (isOpen()) {
                    this.readerThread = NativeThread.current();
                    j = 0;
                    while (true) {
                        try {
                            j2 = IOUtil.read(this.fd, byteBufferArr, i, i2, nd);
                            if (j2 != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                j = j2;
                            } catch (Throwable th2) {
                                th = th2;
                                j = j2;
                                readerCleanup();
                                if (j > 0 || j == -2) {
                                    z = true;
                                }
                                end(z);
                                synchronized (this.stateLock) {
                                    if (j <= 0) {
                                        try {
                                            if (!this.isInputOpen) {
                                            }
                                        } finally {
                                        }
                                    }
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    long jNormalize = IOStatus.normalize(j2);
                    readerCleanup();
                    if (j2 > 0 || j2 == -2) {
                        z = true;
                    }
                    end(z);
                    synchronized (this.stateLock) {
                        if (j2 <= 0) {
                            try {
                                if (!this.isInputOpen) {
                                    return -1L;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                throw th;
                            }
                        }
                        return jNormalize;
                    }
                }
                readerCleanup();
                end($assertionsDisabled);
                synchronized (this.stateLock) {
                    try {
                        return !this.isInputOpen ? -1L : 0L;
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
                throw th;
            }
        }
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        int i;
        int iWrite;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        synchronized (this.writeLock) {
            ensureWriteOpen();
            boolean z = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        writerCleanup();
                        end($assertionsDisabled);
                        synchronized (this.stateLock) {
                            if (!this.isOutputOpen) {
                                throw new AsynchronousCloseException();
                            }
                        }
                        return 0;
                    }
                    this.writerThread = NativeThread.current();
                    i = 0;
                    while (true) {
                        try {
                            iWrite = IOUtil.write(this.fd, byteBuffer, -1L, nd);
                            if (iWrite != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                i = iWrite;
                            } catch (Throwable th) {
                                th = th;
                                i = iWrite;
                                writerCleanup();
                                if (i <= 0 && i != -2) {
                                    z = false;
                                }
                                end(z);
                                synchronized (this.stateLock) {
                                    if (i <= 0) {
                                        try {
                                            if (!this.isOutputOpen) {
                                                throw new AsynchronousCloseException();
                                            }
                                        } finally {
                                        }
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    int iNormalize = IOStatus.normalize(iWrite);
                    writerCleanup();
                    if (iWrite <= 0 && iWrite != -2) {
                        z = false;
                    }
                    end(z);
                    synchronized (this.stateLock) {
                        if (iWrite <= 0) {
                            try {
                                if (!this.isOutputOpen) {
                                    throw new AsynchronousCloseException();
                                }
                            } catch (Throwable th3) {
                                throw th3;
                            }
                        }
                    }
                    return iNormalize;
                }
            } catch (Throwable th4) {
                th = th4;
                i = 0;
            }
        }
    }

    @Override
    public long write(ByteBuffer[] byteBufferArr, int i, int i2) throws IOException {
        long j;
        long jWrite;
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.writeLock) {
            ensureWriteOpen();
            boolean z = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        writerCleanup();
                        end($assertionsDisabled);
                        synchronized (this.stateLock) {
                            if (!this.isOutputOpen) {
                                throw new AsynchronousCloseException();
                            }
                        }
                        return 0L;
                    }
                    this.writerThread = NativeThread.current();
                    j = 0;
                    while (true) {
                        try {
                            jWrite = IOUtil.write(this.fd, byteBufferArr, i, i2, nd);
                            if (jWrite != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                j = jWrite;
                            } catch (Throwable th) {
                                th = th;
                                j = jWrite;
                                writerCleanup();
                                if (j <= 0 && j != -2) {
                                    z = false;
                                }
                                end(z);
                                synchronized (this.stateLock) {
                                    if (j <= 0) {
                                        try {
                                            if (!this.isOutputOpen) {
                                                throw new AsynchronousCloseException();
                                            }
                                        } finally {
                                        }
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    long jNormalize = IOStatus.normalize(jWrite);
                    writerCleanup();
                    if (jWrite <= 0 && jWrite != -2) {
                        z = false;
                    }
                    end(z);
                    synchronized (this.stateLock) {
                        if (jWrite <= 0) {
                            try {
                                if (!this.isOutputOpen) {
                                    throw new AsynchronousCloseException();
                                }
                            } catch (Throwable th3) {
                                throw th3;
                            }
                        }
                    }
                    return jNormalize;
                }
            } catch (Throwable th4) {
                th = th4;
                j = 0;
            }
        }
    }

    int sendOutOfBandData(byte b) throws IOException {
        int i;
        int iSendOutOfBandData;
        synchronized (this.writeLock) {
            ensureWriteOpen();
            boolean z = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        writerCleanup();
                        end($assertionsDisabled);
                        synchronized (this.stateLock) {
                            if (!this.isOutputOpen) {
                                throw new AsynchronousCloseException();
                            }
                        }
                        return 0;
                    }
                    this.writerThread = NativeThread.current();
                    i = 0;
                    while (true) {
                        try {
                            iSendOutOfBandData = sendOutOfBandData(this.fd, b);
                            if (iSendOutOfBandData != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                i = iSendOutOfBandData;
                            } catch (Throwable th) {
                                th = th;
                                i = iSendOutOfBandData;
                                writerCleanup();
                                if (i <= 0 && i != -2) {
                                    z = false;
                                }
                                end(z);
                                synchronized (this.stateLock) {
                                    if (i <= 0) {
                                        try {
                                            if (!this.isOutputOpen) {
                                                throw new AsynchronousCloseException();
                                            }
                                        } finally {
                                        }
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    int iNormalize = IOStatus.normalize(iSendOutOfBandData);
                    writerCleanup();
                    if (iSendOutOfBandData <= 0 && iSendOutOfBandData != -2) {
                        z = false;
                    }
                    end(z);
                    synchronized (this.stateLock) {
                        if (iSendOutOfBandData <= 0) {
                            try {
                                if (!this.isOutputOpen) {
                                    throw new AsynchronousCloseException();
                                }
                            } catch (Throwable th3) {
                                throw th3;
                            }
                        }
                    }
                    return iNormalize;
                }
            } catch (Throwable th4) {
                th = th4;
                i = 0;
            }
        }
    }

    @Override
    protected void implConfigureBlocking(boolean z) throws IOException {
        IOUtil.configureBlocking(this.fd, z);
    }

    public InetSocketAddress localAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.localAddress;
        }
        return inetSocketAddress;
    }

    public SocketAddress remoteAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.remoteAddress;
        }
        return inetSocketAddress;
    }

    @Override
    public SocketChannel bind(SocketAddress socketAddress) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        throw new ClosedChannelException();
                    }
                    if (this.state == 1) {
                        throw new ConnectionPendingException();
                    }
                    if (this.localAddress != null) {
                        throw new AlreadyBoundException();
                    }
                    InetSocketAddress inetSocketAddress = socketAddress == null ? new InetSocketAddress(0) : Net.checkAddress(socketAddress);
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        securityManager.checkListen(inetSocketAddress.getPort());
                    }
                    NetHooks.beforeTcpBind(this.fd, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                    Net.bind(this.fd, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                    this.localAddress = Net.localAddress(this.fd);
                }
            }
        }
        return this;
    }

    @Override
    public boolean isConnected() {
        boolean z;
        synchronized (this.stateLock) {
            z = this.state == 2 ? true : $assertionsDisabled;
        }
        return z;
    }

    @Override
    public boolean isConnectionPending() {
        boolean z;
        synchronized (this.stateLock) {
            z = true;
            if (this.state != 1) {
                z = $assertionsDisabled;
            }
        }
        return z;
    }

    void ensureOpenAndUnconnected() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.state == 2) {
                throw new AlreadyConnectedException();
            }
            if (this.state == 1) {
                throw new ConnectionPendingException();
            }
        }
    }

    @Override
    public boolean connect(SocketAddress socketAddress) throws IOException {
        int i;
        int iConnect;
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                ensureOpenAndUnconnected();
                InetSocketAddress inetSocketAddressCheckAddress = Net.checkAddress(socketAddress);
                SecurityManager securityManager = System.getSecurityManager();
                if (securityManager != null) {
                    securityManager.checkConnect(inetSocketAddressCheckAddress.getAddress().getHostAddress(), inetSocketAddressCheckAddress.getPort());
                }
                synchronized (blockingLock()) {
                    boolean z = true;
                    try {
                        try {
                            begin();
                            synchronized (this.stateLock) {
                                if (!isOpen()) {
                                    readerCleanup();
                                    end($assertionsDisabled);
                                    return $assertionsDisabled;
                                }
                                if (this.localAddress == null) {
                                    NetHooks.beforeTcpConnect(this.fd, inetSocketAddressCheckAddress.getAddress(), inetSocketAddressCheckAddress.getPort());
                                }
                                this.readerThread = NativeThread.current();
                                i = 0;
                                while (true) {
                                    try {
                                        InetAddress address = inetSocketAddressCheckAddress.getAddress();
                                        if (address.isAnyLocalAddress()) {
                                            address = InetAddress.getLocalHost();
                                        }
                                        iConnect = Net.connect(this.fd, address, inetSocketAddressCheckAddress.getPort());
                                        if (iConnect != -3) {
                                            break;
                                        }
                                        try {
                                            if (!isOpen()) {
                                                break;
                                            }
                                            i = iConnect;
                                        } catch (Throwable th) {
                                            th = th;
                                            i = iConnect;
                                            readerCleanup();
                                            if (i <= 0 && i != -2) {
                                                z = false;
                                            }
                                            end(z);
                                            throw th;
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                                readerCleanup();
                                end(iConnect > 0 || iConnect == -2);
                                synchronized (this.stateLock) {
                                    this.remoteAddress = inetSocketAddressCheckAddress;
                                    if (iConnect > 0) {
                                        this.state = 2;
                                        if (isOpen()) {
                                            this.localAddress = Net.localAddress(this.fd);
                                        }
                                        return true;
                                    }
                                    if (!isBlocking()) {
                                        this.state = 1;
                                        if (isOpen()) {
                                            this.localAddress = Net.localAddress(this.fd);
                                        }
                                    }
                                    return $assertionsDisabled;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            i = 0;
                        }
                    } catch (IOException e) {
                        close();
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public boolean finishConnect() throws java.io.IOException {
        r0 = r14.readLock;
        synchronized (r0) {
            ;
            r1 = r14.writeLock;
            synchronized (r1) {
                ;
                r2 = r14.stateLock;
                synchronized (r2) {
                    ;
                    if (isOpen()) {
                        r5 = true;
                        if (r14.state == 2) {
                            return true;
                        } else {
                            if (r14.state == 1) {
                                begin();
                                r9 = blockingLock();
                                synchronized (r9) {
                                    ;
                                    r10 = r14.stateLock;
                                    synchronized (r10) {
                                        ;
                                        if (!isOpen()) {
                                        } else {
                                            r14.readerThread = sun.nio.ch.NativeThread.current();
                                            dalvik.system.BlockGuard.getThreadPolicy().onNetwork();
                                            if (!isBlocking()) {
                                                do {
                                                    r12 = checkConnect(r14.fd, sun.nio.ch.SocketChannelImpl.$assertionsDisabled, r14.readyToConnect);
                                                    if (r12 == -3) {
                                                    }
                                                } while (isOpen());
                                            } else {
                                                while (true) {
                                                    r12 = checkConnect(r14.fd, true, r14.readyToConnect);
                                                    if (r12 != 0 && (r12 != -3 || !isOpen())) {
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                while (true) {
                                }
                                throw r4;
                            } else {
                                throw new java.nio.channels.NoConnectionPendingException();
                            }
                        }
                    } else {
                        throw new java.nio.channels.ClosedChannelException();
                    }
                }
            }
        }
    }

    @Override
    public SocketChannel shutdownInput() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
            if (this.isInputOpen) {
                Net.shutdown(this.fd, 0);
                if (this.readerThread != 0) {
                    NativeThread.signal(this.readerThread);
                }
                this.isInputOpen = $assertionsDisabled;
            }
        }
        return this;
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
            if (this.isOutputOpen) {
                Net.shutdown(this.fd, 1);
                if (this.writerThread != 0) {
                    NativeThread.signal(this.writerThread);
                }
                this.isOutputOpen = $assertionsDisabled;
            }
        }
        return this;
    }

    public boolean isInputOpen() {
        boolean z;
        synchronized (this.stateLock) {
            z = this.isInputOpen;
        }
        return z;
    }

    public boolean isOutputOpen() {
        boolean z;
        synchronized (this.stateLock) {
            z = this.isOutputOpen;
        }
        return z;
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            this.isInputOpen = $assertionsDisabled;
            this.isOutputOpen = $assertionsDisabled;
            if (this.state != 4) {
                this.guard.close();
                nd.preClose(this.fd);
            }
            if (this.readerThread != 0) {
                NativeThread.signal(this.readerThread);
            }
            if (this.writerThread != 0) {
                NativeThread.signal(this.writerThread);
            }
            if (!isRegistered()) {
                kill();
            }
        }
    }

    @Override
    public void kill() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 4) {
                return;
            }
            if (this.state == -1) {
                this.state = 4;
                return;
            }
            if (this.readerThread == 0 && this.writerThread == 0) {
                nd.close(this.fd);
                this.state = 4;
            } else {
                this.state = 3;
            }
        }
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        close();
    }

    public boolean translateReadyOps(int i, int i2, SelectionKeyImpl selectionKeyImpl) {
        int iNioInterestOps = selectionKeyImpl.nioInterestOps();
        int iNioReadyOps = selectionKeyImpl.nioReadyOps();
        if ((Net.POLLNVAL & i) != 0) {
            return $assertionsDisabled;
        }
        if (((Net.POLLERR | Net.POLLHUP) & i) != 0) {
            selectionKeyImpl.nioReadyOps(iNioInterestOps);
            this.readyToConnect = true;
            if (((~iNioReadyOps) & iNioInterestOps) != 0) {
                return true;
            }
            return $assertionsDisabled;
        }
        if ((Net.POLLIN & i) != 0 && (iNioInterestOps & 1) != 0 && this.state == 2) {
            i2 |= 1;
        }
        if ((Net.POLLCONN & i) != 0 && (iNioInterestOps & 8) != 0 && (this.state == 0 || this.state == 1)) {
            i2 |= 8;
            this.readyToConnect = true;
        }
        if ((i & Net.POLLOUT) != 0 && (iNioInterestOps & 4) != 0 && this.state == 2) {
            i2 |= 4;
        }
        selectionKeyImpl.nioReadyOps(i2);
        if (((~iNioReadyOps) & i2) != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    @Override
    public boolean translateAndUpdateReadyOps(int i, SelectionKeyImpl selectionKeyImpl) {
        return translateReadyOps(i, selectionKeyImpl.nioReadyOps(), selectionKeyImpl);
    }

    @Override
    public boolean translateAndSetReadyOps(int i, SelectionKeyImpl selectionKeyImpl) {
        return translateReadyOps(i, 0, selectionKeyImpl);
    }

    int poll(int i, long j) throws IOException {
        synchronized (this.readLock) {
            boolean z = $assertionsDisabled;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        return 0;
                    }
                    this.readerThread = NativeThread.current();
                    int iPoll = Net.poll(this.fd, i, j);
                    readerCleanup();
                    if (iPoll > 0) {
                        z = true;
                    }
                    end(z);
                    return iPoll;
                }
            } finally {
                readerCleanup();
                end($assertionsDisabled);
            }
        }
    }

    @Override
    public void translateAndSetInterestOps(int i, SelectionKeyImpl selectionKeyImpl) {
        int i2 = (i & 1) != 0 ? 0 | Net.POLLIN : 0;
        if ((i & 4) != 0) {
            i2 |= Net.POLLOUT;
        }
        if ((i & 8) != 0) {
            i2 |= Net.POLLCONN;
        }
        selectionKeyImpl.selector.putEventOps(selectionKeyImpl, i2);
    }

    @Override
    public FileDescriptor getFD() {
        return this.fd;
    }

    @Override
    public int getFDVal() {
        return this.fdVal;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(getClass().getSuperclass().getName());
        stringBuffer.append('[');
        if (!isOpen()) {
            stringBuffer.append("closed");
        } else {
            synchronized (this.stateLock) {
                switch (this.state) {
                    case 0:
                        stringBuffer.append("unconnected");
                        break;
                    case 1:
                        stringBuffer.append("connection-pending");
                        break;
                    case 2:
                        stringBuffer.append("connected");
                        if (!this.isInputOpen) {
                            stringBuffer.append(" ishut");
                        }
                        if (!this.isOutputOpen) {
                            stringBuffer.append(" oshut");
                        }
                        break;
                }
                InetSocketAddress inetSocketAddressLocalAddress = localAddress();
                if (inetSocketAddressLocalAddress != null) {
                    stringBuffer.append(" local=");
                    stringBuffer.append(Net.getRevealedLocalAddressAsString(inetSocketAddressLocalAddress));
                }
                if (remoteAddress() != null) {
                    stringBuffer.append(" remote=");
                    stringBuffer.append(remoteAddress().toString());
                }
            }
        }
        stringBuffer.append(']');
        return stringBuffer.toString();
    }
}
