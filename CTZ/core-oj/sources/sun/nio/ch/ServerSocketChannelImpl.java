package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sun.net.NetHooks;

class ServerSocketChannelImpl extends ServerSocketChannel implements SelChImpl {
    static final boolean $assertionsDisabled = false;
    private static final int ST_INUSE = 0;
    private static final int ST_KILLED = 1;
    private static final int ST_UNINITIALIZED = -1;
    private static NativeDispatcher nd;
    private final FileDescriptor fd;
    private int fdVal;
    private boolean isReuseAddress;
    private InetSocketAddress localAddress;
    private final Object lock;
    ServerSocket socket;
    private int state;
    private final Object stateLock;
    private volatile long thread;

    private native int accept0(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, InetSocketAddress[] inetSocketAddressArr) throws IOException;

    private static native void initIDs();

    static {
        initIDs();
        nd = new SocketDispatcher();
    }

    @Override
    public NetworkChannel setOption(SocketOption socketOption, Object obj) throws IOException {
        return setOption((SocketOption<Object>) socketOption, obj);
    }

    ServerSocketChannelImpl(SelectorProvider selectorProvider) throws IOException {
        super(selectorProvider);
        this.thread = 0L;
        this.lock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.fd = Net.serverSocket(true);
        this.fdVal = IOUtil.fdVal(this.fd);
        this.state = 0;
    }

    ServerSocketChannelImpl(SelectorProvider selectorProvider, FileDescriptor fileDescriptor, boolean z) throws IOException {
        super(selectorProvider);
        this.thread = 0L;
        this.lock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.fd = fileDescriptor;
        this.fdVal = IOUtil.fdVal(fileDescriptor);
        this.state = 0;
        if (z) {
            this.localAddress = Net.localAddress(fileDescriptor);
        }
    }

    @Override
    public ServerSocket socket() {
        ServerSocket serverSocket;
        synchronized (this.stateLock) {
            if (this.socket == null) {
                this.socket = ServerSocketAdaptor.create(this);
            }
            serverSocket = this.socket;
        }
        return serverSocket;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        InetSocketAddress revealedLocalAddress;
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            revealedLocalAddress = this.localAddress == null ? this.localAddress : Net.getRevealedLocalAddress(Net.asInetSocketAddress(this.localAddress));
        }
        return revealedLocalAddress;
    }

    @Override
    public <T> ServerSocketChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
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
            } else {
                Net.setSocketOption(this.fd, Net.UNSPEC, socketOption, t);
            }
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
            return (T) Net.getSocketOption(this.fd, Net.UNSPEC, socketOption);
        }
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private DefaultOptionsHolder() {
        }

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet hashSet = new HashSet(2);
            hashSet.add(StandardSocketOptions.SO_RCVBUF);
            hashSet.add(StandardSocketOptions.SO_REUSEADDR);
            hashSet.add(StandardSocketOptions.IP_TOS);
            return Collections.unmodifiableSet(hashSet);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    public boolean isBound() {
        boolean z;
        synchronized (this.stateLock) {
            z = this.localAddress != null ? true : $assertionsDisabled;
        }
        return z;
    }

    public InetSocketAddress localAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.localAddress;
        }
        return inetSocketAddress;
    }

    @Override
    public ServerSocketChannel bind(SocketAddress socketAddress, int i) throws IOException {
        synchronized (this.lock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (isBound()) {
                throw new AlreadyBoundException();
            }
            InetSocketAddress inetSocketAddress = socketAddress == null ? new InetSocketAddress(0) : Net.checkAddress(socketAddress);
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkListen(inetSocketAddress.getPort());
            }
            NetHooks.beforeTcpBind(this.fd, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
            Net.bind(this.fd, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
            FileDescriptor fileDescriptor = this.fd;
            if (i < 1) {
                i = 50;
            }
            Net.listen(fileDescriptor, i);
            synchronized (this.stateLock) {
                this.localAddress = Net.localAddress(this.fd);
            }
        }
        return this;
    }

    @Override
    public SocketChannel accept() throws IOException {
        int i;
        int iAccept;
        synchronized (this.lock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isBound()) {
                throw new NotYetBoundException();
            }
            FileDescriptor fileDescriptor = new FileDescriptor();
            boolean z = true;
            InetSocketAddress[] inetSocketAddressArr = new InetSocketAddress[1];
            try {
                begin();
                if (isOpen()) {
                    this.thread = NativeThread.current();
                    i = 0;
                    while (true) {
                        try {
                            iAccept = accept(this.fd, fileDescriptor, inetSocketAddressArr);
                            if (iAccept != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                i = iAccept;
                            } catch (Throwable th) {
                                th = th;
                                i = iAccept;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    this.thread = 0L;
                    end(iAccept > 0);
                    if (iAccept < 1) {
                        return null;
                    }
                    IOUtil.configureBlocking(fileDescriptor, true);
                    InetSocketAddress inetSocketAddress = inetSocketAddressArr[0];
                    SocketChannelImpl socketChannelImpl = new SocketChannelImpl(provider(), fileDescriptor, inetSocketAddress);
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        try {
                            securityManager.checkAccept(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
                        } catch (SecurityException e) {
                            socketChannelImpl.close();
                            throw e;
                        }
                    }
                    return socketChannelImpl;
                }
                this.thread = 0L;
                end($assertionsDisabled);
                return null;
            } catch (Throwable th3) {
                th = th3;
                i = 0;
            }
            this.thread = 0L;
            if (i <= 0) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    protected void implConfigureBlocking(boolean z) throws IOException {
        IOUtil.configureBlocking(this.fd, z);
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            if (this.state != 1) {
                nd.preClose(this.fd);
            }
            long j = this.thread;
            if (j != 0) {
                NativeThread.signal(j);
            }
            if (!isRegistered()) {
                kill();
            }
        }
    }

    @Override
    public void kill() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 1) {
                return;
            }
            if (this.state == -1) {
                this.state = 1;
            } else {
                nd.close(this.fd);
                this.state = 1;
            }
        }
    }

    public boolean translateReadyOps(int i, int i2, SelectionKeyImpl selectionKeyImpl) {
        int iNioInterestOps = selectionKeyImpl.nioInterestOps();
        int iNioReadyOps = selectionKeyImpl.nioReadyOps();
        if ((Net.POLLNVAL & i) != 0) {
            return $assertionsDisabled;
        }
        if (((Net.POLLERR | Net.POLLHUP) & i) != 0) {
            selectionKeyImpl.nioReadyOps(iNioInterestOps);
            if (((~iNioReadyOps) & iNioInterestOps) != 0) {
                return true;
            }
            return $assertionsDisabled;
        }
        if ((i & Net.POLLIN) != 0 && (iNioInterestOps & 16) != 0) {
            i2 |= 16;
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
        synchronized (this.lock) {
            boolean z = $assertionsDisabled;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        return 0;
                    }
                    this.thread = NativeThread.current();
                    int iPoll = Net.poll(this.fd, i, j);
                    this.thread = 0L;
                    if (iPoll > 0) {
                        z = true;
                    }
                    end(z);
                    return iPoll;
                }
            } finally {
                this.thread = 0L;
                end($assertionsDisabled);
            }
        }
    }

    @Override
    public void translateAndSetInterestOps(int i, SelectionKeyImpl selectionKeyImpl) {
        selectionKeyImpl.selector.putEventOps(selectionKeyImpl, (i & 16) != 0 ? 0 | Net.POLLIN : 0);
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
        stringBuffer.append(getClass().getName());
        stringBuffer.append('[');
        if (!isOpen()) {
            stringBuffer.append("closed");
        } else {
            synchronized (this.stateLock) {
                InetSocketAddress inetSocketAddressLocalAddress = localAddress();
                if (inetSocketAddressLocalAddress == null) {
                    stringBuffer.append("unbound");
                } else {
                    stringBuffer.append(Net.getRevealedLocalAddressAsString(inetSocketAddressLocalAddress));
                }
            }
        }
        stringBuffer.append(']');
        return stringBuffer.toString();
    }

    private int accept(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, InetSocketAddress[] inetSocketAddressArr) throws IOException {
        return accept0(fileDescriptor, fileDescriptor2, inetSocketAddressArr);
    }
}
