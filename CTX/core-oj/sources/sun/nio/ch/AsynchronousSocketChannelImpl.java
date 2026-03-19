package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jdk.net.ExtendedSocketOptions;
import sun.net.ExtendedOptionsImpl;
import sun.net.NetHooks;

abstract class AsynchronousSocketChannelImpl extends AsynchronousSocketChannel implements Cancellable, Groupable {
    static final int ST_CONNECTED = 2;
    static final int ST_PENDING = 1;
    static final int ST_UNCONNECTED = 0;
    static final int ST_UNINITIALIZED = -1;
    private final ReadWriteLock closeLock;
    protected final FileDescriptor fd;
    private boolean isReuseAddress;
    protected volatile InetSocketAddress localAddress;
    private volatile boolean open;
    private boolean readKilled;
    private final Object readLock;
    private boolean readShutdown;
    private boolean reading;
    protected volatile InetSocketAddress remoteAddress;
    protected volatile int state;
    protected final Object stateLock;
    private boolean writeKilled;
    private final Object writeLock;
    private boolean writeShutdown;
    private boolean writing;

    abstract void implClose() throws IOException;

    abstract <A> Future<Void> implConnect(SocketAddress socketAddress, A a, CompletionHandler<Void, ? super A> completionHandler);

    abstract <V extends Number, A> Future<V> implRead(boolean z, ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, long j, TimeUnit timeUnit, A a, CompletionHandler<V, ? super A> completionHandler);

    abstract <V extends Number, A> Future<V> implWrite(boolean z, ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, long j, TimeUnit timeUnit, A a, CompletionHandler<V, ? super A> completionHandler);

    @Override
    public NetworkChannel setOption(SocketOption socketOption, Object obj) throws IOException {
        return setOption((SocketOption<Object>) socketOption, obj);
    }

    AsynchronousSocketChannelImpl(AsynchronousChannelGroupImpl asynchronousChannelGroupImpl) throws IOException {
        super(asynchronousChannelGroupImpl.provider());
        this.stateLock = new Object();
        this.localAddress = null;
        this.remoteAddress = null;
        this.state = -1;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.closeLock = new ReentrantReadWriteLock();
        this.open = true;
        this.fd = Net.socket(true);
        this.state = 0;
    }

    AsynchronousSocketChannelImpl(AsynchronousChannelGroupImpl asynchronousChannelGroupImpl, FileDescriptor fileDescriptor, InetSocketAddress inetSocketAddress) throws IOException {
        super(asynchronousChannelGroupImpl.provider());
        this.stateLock = new Object();
        this.localAddress = null;
        this.remoteAddress = null;
        this.state = -1;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.closeLock = new ReentrantReadWriteLock();
        this.open = true;
        this.fd = fileDescriptor;
        this.state = 2;
        this.localAddress = Net.localAddress(fileDescriptor);
        this.remoteAddress = inetSocketAddress;
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

    final void enableReading(boolean z) {
        synchronized (this.readLock) {
            this.reading = false;
            if (z) {
                this.readKilled = true;
            }
        }
    }

    final void enableReading() {
        enableReading(false);
    }

    final void enableWriting(boolean z) {
        synchronized (this.writeLock) {
            this.writing = false;
            if (z) {
                this.writeKilled = true;
            }
        }
    }

    final void enableWriting() {
        enableWriting(false);
    }

    final void killReading() {
        synchronized (this.readLock) {
            this.readKilled = true;
        }
    }

    final void killWriting() {
        synchronized (this.writeLock) {
            this.writeKilled = true;
        }
    }

    final void killConnect() {
        killReading();
        killWriting();
    }

    @Override
    public final Future<Void> connect(SocketAddress socketAddress) {
        return implConnect(socketAddress, null, null);
    }

    @Override
    public final <A> void connect(SocketAddress socketAddress, A a, CompletionHandler<Void, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        implConnect(socketAddress, a, completionHandler);
    }

    private <V extends Number, A> Future<V> read(boolean z, ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, long j, TimeUnit timeUnit, A a, CompletionHandler<V, ? super A> completionHandler) {
        Object objValueOf;
        if (!isOpen()) {
            ClosedChannelException closedChannelException = new ClosedChannelException();
            if (completionHandler == null) {
                return CompletedFuture.withFailure(closedChannelException);
            }
            Invoker.invoke(this, completionHandler, a, null, closedChannelException);
            return null;
        }
        if (this.remoteAddress == null) {
            throw new NotYetConnectedException();
        }
        boolean z2 = true;
        boolean z3 = z || byteBuffer.hasRemaining();
        synchronized (this.readLock) {
            if (this.readKilled) {
                throw new IllegalStateException("Reading not allowed due to timeout or cancellation");
            }
            if (this.reading) {
                throw new ReadPendingException();
            }
            if (!this.readShutdown) {
                if (z3) {
                    this.reading = true;
                }
                z2 = false;
            }
        }
        if (z2 || !z3) {
            if (z) {
                objValueOf = Long.valueOf(z2 ? -1L : 0L);
            } else {
                objValueOf = Integer.valueOf(z2 ? -1 : 0);
            }
            if (completionHandler == null) {
                return CompletedFuture.withResult(objValueOf);
            }
            Invoker.invoke(this, completionHandler, a, objValueOf, null);
            return null;
        }
        return implRead(z, byteBuffer, byteBufferArr, j, timeUnit, a, completionHandler);
    }

    @Override
    public final Future<Integer> read(ByteBuffer byteBuffer) {
        if (byteBuffer.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        return read(false, byteBuffer, (ByteBuffer[]) null, 0L, TimeUnit.MILLISECONDS, (Object) null, (CompletionHandler<V, ? super Object>) null);
    }

    @Override
    public final <A> void read(ByteBuffer byteBuffer, long j, TimeUnit timeUnit, A a, CompletionHandler<Integer, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        if (byteBuffer.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        read(false, byteBuffer, (ByteBuffer[]) null, j, timeUnit, (Object) a, (CompletionHandler) completionHandler);
    }

    @Override
    public final <A> void read(ByteBuffer[] byteBufferArr, int i, int i2, long j, TimeUnit timeUnit, A a, CompletionHandler<Long, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        ByteBuffer[] byteBufferArrSubsequence = Util.subsequence(byteBufferArr, i, i2);
        for (ByteBuffer byteBuffer : byteBufferArrSubsequence) {
            if (byteBuffer.isReadOnly()) {
                throw new IllegalArgumentException("Read-only buffer");
            }
        }
        read(true, (ByteBuffer) null, byteBufferArrSubsequence, j, timeUnit, (Object) a, (CompletionHandler) completionHandler);
    }

    private <V extends Number, A> Future<V> write(boolean z, ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, long j, TimeUnit timeUnit, A a, CompletionHandler<V, ? super A> completionHandler) {
        boolean z2 = true;
        boolean z3 = z || byteBuffer.hasRemaining();
        if (isOpen()) {
            if (this.remoteAddress == null) {
                throw new NotYetConnectedException();
            }
            synchronized (this.writeLock) {
                if (this.writeKilled) {
                    throw new IllegalStateException("Writing not allowed due to timeout or cancellation");
                }
                if (this.writing) {
                    throw new WritePendingException();
                }
                if (!this.writeShutdown) {
                    if (z3) {
                        this.writing = true;
                    }
                    z2 = false;
                }
            }
        }
        if (z2) {
            ClosedChannelException closedChannelException = new ClosedChannelException();
            if (completionHandler == null) {
                return CompletedFuture.withFailure(closedChannelException);
            }
            Invoker.invoke(this, completionHandler, a, null, closedChannelException);
            return null;
        }
        if (!z3) {
            Object obj = z ? 0L : 0;
            if (completionHandler == null) {
                return CompletedFuture.withResult(obj);
            }
            Invoker.invoke(this, completionHandler, a, obj, null);
            return null;
        }
        return implWrite(z, byteBuffer, byteBufferArr, j, timeUnit, a, completionHandler);
    }

    @Override
    public final Future<Integer> write(ByteBuffer byteBuffer) {
        return write(false, byteBuffer, (ByteBuffer[]) null, 0L, TimeUnit.MILLISECONDS, (Object) null, (CompletionHandler<V, ? super Object>) null);
    }

    @Override
    public final <A> void write(ByteBuffer byteBuffer, long j, TimeUnit timeUnit, A a, CompletionHandler<Integer, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        write(false, byteBuffer, (ByteBuffer[]) null, j, timeUnit, (Object) a, (CompletionHandler) completionHandler);
    }

    @Override
    public final <A> void write(ByteBuffer[] byteBufferArr, int i, int i2, long j, TimeUnit timeUnit, A a, CompletionHandler<Long, ? super A> completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("'handler' is null");
        }
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        write(true, (ByteBuffer) null, Util.subsequence(byteBufferArr, i, i2), j, timeUnit, (Object) a, (CompletionHandler) completionHandler);
    }

    @Override
    public final AsynchronousSocketChannel bind(SocketAddress socketAddress) throws IOException {
        try {
            begin();
            synchronized (this.stateLock) {
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
    public final <T> AsynchronousSocketChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
        if (socketOption == null) {
            throw new NullPointerException();
        }
        if (!supportedOptions().contains(socketOption)) {
            throw new UnsupportedOperationException("'" + ((Object) socketOption) + "' not supported");
        }
        try {
            begin();
            if (this.writeShutdown) {
                throw new IOException("Connection has been shutdown for writing");
            }
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
            HashSet hashSet = new HashSet(5);
            hashSet.add(StandardSocketOptions.SO_SNDBUF);
            hashSet.add(StandardSocketOptions.SO_RCVBUF);
            hashSet.add(StandardSocketOptions.SO_KEEPALIVE);
            hashSet.add(StandardSocketOptions.SO_REUSEADDR);
            hashSet.add(StandardSocketOptions.TCP_NODELAY);
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

    @Override
    public final SocketAddress getRemoteAddress() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        return this.remoteAddress;
    }

    @Override
    public final AsynchronousSocketChannel shutdownInput() throws IOException {
        try {
            begin();
            if (this.remoteAddress == null) {
                throw new NotYetConnectedException();
            }
            synchronized (this.readLock) {
                if (!this.readShutdown) {
                    Net.shutdown(this.fd, 0);
                    this.readShutdown = true;
                }
            }
            return this;
        } finally {
            end();
        }
    }

    @Override
    public final AsynchronousSocketChannel shutdownOutput() throws IOException {
        try {
            begin();
            if (this.remoteAddress == null) {
                throw new NotYetConnectedException();
            }
            synchronized (this.writeLock) {
                if (!this.writeShutdown) {
                    Net.shutdown(this.fd, 1);
                    this.writeShutdown = true;
                }
            }
            return this;
        } finally {
            end();
        }
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append('[');
        synchronized (this.stateLock) {
            if (!isOpen()) {
                sb.append("closed");
            } else {
                switch (this.state) {
                    case 0:
                        sb.append("unconnected");
                        break;
                    case 1:
                        sb.append("connection-pending");
                        break;
                    case 2:
                        sb.append("connected");
                        if (this.readShutdown) {
                            sb.append(" ishut");
                        }
                        if (this.writeShutdown) {
                            sb.append(" oshut");
                        }
                        break;
                }
                if (this.localAddress != null) {
                    sb.append(" local=");
                    sb.append(Net.getRevealedLocalAddressAsString(this.localAddress));
                }
                if (this.remoteAddress != null) {
                    sb.append(" remote=");
                    sb.append(this.remoteAddress.toString());
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
