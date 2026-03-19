package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NetworkChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import sun.net.NetHooks;

abstract class AsynchronousServerSocketChannelImpl extends AsynchronousServerSocketChannel implements Cancellable, Groupable {
    private volatile boolean acceptKilled;
    private ReadWriteLock closeLock;
    protected final FileDescriptor fd;
    private boolean isReuseAddress;
    protected volatile InetSocketAddress localAddress;
    private volatile boolean open;
    private final Object stateLock;

    abstract Future<AsynchronousSocketChannel> implAccept(Object obj, CompletionHandler<AsynchronousSocketChannel, Object> completionHandler);

    abstract void implClose() throws IOException;

    @Override
    public NetworkChannel setOption(SocketOption socketOption, Object obj) throws IOException {
        return setOption((SocketOption<Object>) socketOption, obj);
    }

    AsynchronousServerSocketChannelImpl(AsynchronousChannelGroupImpl asynchronousChannelGroupImpl) {
        super(asynchronousChannelGroupImpl.provider());
        this.localAddress = null;
        this.stateLock = new Object();
        this.closeLock = new ReentrantReadWriteLock();
        this.open = true;
        this.fd = Net.serverSocket(true);
    }

    @Override
    public final boolean isOpen() {
        return this.open;
    }

    final void begin() throws IOException {
        this.closeLock.readLock().lock();
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    final void end() {
        this.closeLock.readLock().unlock();
    }

    @Override
    public final void close() throws IOException {
        this.closeLock.writeLock().lock();
        try {
            if (!this.open) {
                return;
            }
            this.open = false;
            this.closeLock.writeLock().unlock();
            implClose();
        } finally {
            this.closeLock.writeLock().unlock();
        }
    }

    @Override
    public final Future<AsynchronousSocketChannel> accept() {
        return implAccept(null, null);
    }

    @Override
    public final <A> void accept(A a, CompletionHandler<AsynchronousSocketChannel, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        implAccept(a, completionHandler);
    }

    final boolean isAcceptKilled() {
        return this.acceptKilled;
    }

    @Override
    public final void onCancel(PendingFuture<?, ?> pendingFuture) {
        this.acceptKilled = true;
    }

    @Override
    public final AsynchronousServerSocketChannel bind(SocketAddress socketAddress, int i) throws IOException {
        InetSocketAddress inetSocketAddress = socketAddress == null ? new InetSocketAddress(0) : Net.checkAddress(socketAddress);
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkListen(inetSocketAddress.getPort());
        }
        try {
            begin();
            synchronized (this.stateLock) {
                if (this.localAddress != null) {
                    throw new AlreadyBoundException();
                }
                NetHooks.beforeTcpBind(this.fd, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                Net.bind(this.fd, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                FileDescriptor fileDescriptor = this.fd;
                if (i < 1) {
                    i = 50;
                }
                Net.listen(fileDescriptor, i);
                this.localAddress = Net.localAddress(this.fd);
            }
            return this;
        } finally {
            end();
        }
    }

    @Override
    public final SocketAddress getLocalAddress() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        return Net.getRevealedLocalAddress(this.localAddress);
    }

    @Override
    public final <T> AsynchronousServerSocketChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
        if (socketOption == null) {
            throw new NullPointerException();
        }
        if (!supportedOptions().contains(socketOption)) {
            throw new UnsupportedOperationException("'" + ((Object) socketOption) + "' not supported");
        }
        try {
            begin();
            if (socketOption == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                this.isReuseAddress = ((Boolean) t).booleanValue();
            } else {
                Net.setSocketOption(this.fd, Net.UNSPEC, socketOption, t);
            }
            return this;
        } finally {
            end();
        }
    }

    @Override
    public final <T> T getOption(SocketOption<T> socketOption) throws IOException {
        if (socketOption == null) {
            throw new NullPointerException();
        }
        if (!supportedOptions().contains(socketOption)) {
            throw new UnsupportedOperationException("'" + ((Object) socketOption) + "' not supported");
        }
        try {
            begin();
            if (socketOption == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                return (T) Boolean.valueOf(this.isReuseAddress);
            }
            return (T) Net.getSocketOption(this.fd, Net.UNSPEC, socketOption);
        } finally {
            end();
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
            return Collections.unmodifiableSet(hashSet);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append('[');
        if (!isOpen()) {
            sb.append("closed");
        } else if (this.localAddress == null) {
            sb.append("unbound");
        } else {
            sb.append(Net.getRevealedLocalAddressAsString(this.localAddress));
        }
        sb.append(']');
        return sb.toString();
    }
}
