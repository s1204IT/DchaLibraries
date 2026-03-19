package sun.nio.ch;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AcceptPendingException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetBoundException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.nio.ch.Port;

class UnixAsynchronousServerSocketChannelImpl extends AsynchronousServerSocketChannelImpl implements Port.PollableChannel {
    private static final NativeDispatcher nd = new SocketDispatcher();
    private AccessControlContext acceptAcc;
    private Object acceptAttachment;
    private PendingFuture<AsynchronousSocketChannel, Object> acceptFuture;
    private CompletionHandler<AsynchronousSocketChannel, Object> acceptHandler;
    private boolean acceptPending;
    private final AtomicBoolean accepting;
    private final int fdVal;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private final Port port;
    private final Object updateLock;

    private native int accept0(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, InetSocketAddress[] inetSocketAddressArr) throws IOException;

    private static native void initIDs();

    static {
        initIDs();
    }

    private void enableAccept() {
        this.accepting.set(false);
    }

    UnixAsynchronousServerSocketChannelImpl(Port port) throws IOException {
        super(port);
        this.accepting = new AtomicBoolean();
        this.updateLock = new Object();
        this.guard = CloseGuard.get();
        try {
            IOUtil.configureBlocking(this.fd, false);
            this.port = port;
            this.fdVal = IOUtil.fdVal(this.fd);
            port.register(this.fdVal, this);
            this.guard.open("close");
        } catch (IOException e) {
            nd.close(this.fd);
            throw e;
        }
    }

    @Override
    void implClose() throws IOException {
        this.guard.close();
        this.port.unregister(this.fdVal);
        nd.close(this.fd);
        synchronized (this.updateLock) {
            if (this.acceptPending) {
                this.acceptPending = false;
                CompletionHandler<AsynchronousSocketChannel, Object> completionHandler = this.acceptHandler;
                Object obj = this.acceptAttachment;
                PendingFuture<AsynchronousSocketChannel, Object> pendingFuture = this.acceptFuture;
                AsynchronousCloseException asynchronousCloseException = new AsynchronousCloseException();
                asynchronousCloseException.setStackTrace(new StackTraceElement[0]);
                if (completionHandler == null) {
                    pendingFuture.setFailure(asynchronousCloseException);
                } else {
                    Invoker.invokeIndirectly(this, completionHandler, obj, (Object) null, asynchronousCloseException);
                }
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return this.port;
    }

    @Override
    public void onEvent(int i, boolean z) throws IOException {
        synchronized (this.updateLock) {
            if (this.acceptPending) {
                this.acceptPending = false;
                FileDescriptor fileDescriptor = new FileDescriptor();
                InetSocketAddress[] inetSocketAddressArr = new InetSocketAddress[1];
                AsynchronousSocketChannel asynchronousSocketChannelFinishAccept = null;
                try {
                    begin();
                } catch (Throwable th) {
                    th = th;
                    if (th instanceof ClosedChannelException) {
                        th = new AsynchronousCloseException();
                    }
                } finally {
                    end();
                }
                if (accept(this.fd, fileDescriptor, inetSocketAddressArr) == -2) {
                    synchronized (this.updateLock) {
                        this.acceptPending = true;
                    }
                    this.port.startPoll(this.fdVal, Net.POLLIN);
                    return;
                }
                end();
                th = null;
                if (th == null) {
                    try {
                        asynchronousSocketChannelFinishAccept = finishAccept(fileDescriptor, inetSocketAddressArr[0], this.acceptAcc);
                    } catch (Throwable th2) {
                        th = ((th2 instanceof IOException) || (th2 instanceof SecurityException)) ? th2 : new IOException(th2);
                    }
                }
                CompletionHandler<AsynchronousSocketChannel, Object> completionHandler = this.acceptHandler;
                Object obj = this.acceptAttachment;
                PendingFuture<AsynchronousSocketChannel, Object> pendingFuture = this.acceptFuture;
                enableAccept();
                if (completionHandler != null) {
                    Invoker.invoke(this, completionHandler, obj, asynchronousSocketChannelFinishAccept, th);
                    return;
                }
                pendingFuture.setResult(asynchronousSocketChannelFinishAccept, th);
                if (asynchronousSocketChannelFinishAccept == null || !pendingFuture.isCancelled()) {
                    return;
                }
                try {
                    asynchronousSocketChannelFinishAccept.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private AsynchronousSocketChannel finishAccept(FileDescriptor fileDescriptor, final InetSocketAddress inetSocketAddress, AccessControlContext accessControlContext) throws IOException, SecurityException {
        try {
            UnixAsynchronousSocketChannelImpl unixAsynchronousSocketChannelImpl = new UnixAsynchronousSocketChannelImpl(this.port, fileDescriptor, inetSocketAddress);
            try {
                if (accessControlContext != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            SecurityManager securityManager = System.getSecurityManager();
                            if (securityManager != null) {
                                securityManager.checkAccept(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
                                return null;
                            }
                            return null;
                        }
                    }, accessControlContext);
                } else {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        securityManager.checkAccept(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
                    }
                }
                return unixAsynchronousSocketChannelImpl;
            } catch (SecurityException e) {
                try {
                    unixAsynchronousSocketChannelImpl.close();
                } catch (Throwable th) {
                    e.addSuppressed(th);
                }
                throw e;
            }
        } catch (IOException e2) {
            nd.close(fileDescriptor);
            throw e2;
        }
    }

    @Override
    Future<AsynchronousSocketChannel> implAccept(Object obj, CompletionHandler<AsynchronousSocketChannel, Object> completionHandler) {
        AsynchronousSocketChannel asynchronousSocketChannelFinishAccept;
        PendingFuture<AsynchronousSocketChannel, Object> pendingFuture;
        if (!isOpen()) {
            ClosedChannelException closedChannelException = new ClosedChannelException();
            if (completionHandler == null) {
                return CompletedFuture.withFailure(closedChannelException);
            }
            Invoker.invoke(this, completionHandler, obj, null, closedChannelException);
            return null;
        }
        if (this.localAddress == null) {
            throw new NotYetBoundException();
        }
        if (isAcceptKilled()) {
            throw new RuntimeException("Accept not allowed due cancellation");
        }
        if (!this.accepting.compareAndSet(false, true)) {
            throw new AcceptPendingException();
        }
        FileDescriptor fileDescriptor = new FileDescriptor();
        InetSocketAddress[] inetSocketAddressArr = new InetSocketAddress[1];
        try {
            begin();
        } catch (Throwable th) {
            th = th;
            if (th instanceof ClosedChannelException) {
                th = new AsynchronousCloseException();
            }
        } finally {
            end();
        }
        if (accept(this.fd, fileDescriptor, inetSocketAddressArr) != -2) {
            end();
            th = null;
            if (th == null) {
                try {
                    asynchronousSocketChannelFinishAccept = finishAccept(fileDescriptor, inetSocketAddressArr[0], null);
                } catch (Throwable th2) {
                    th = th2;
                    asynchronousSocketChannelFinishAccept = null;
                }
            } else {
                asynchronousSocketChannelFinishAccept = null;
            }
            enableAccept();
            if (completionHandler == null) {
                return CompletedFuture.withResult(asynchronousSocketChannelFinishAccept, th);
            }
            Invoker.invokeIndirectly(this, (CompletionHandler<AsynchronousSocketChannel, ? super Object>) completionHandler, obj, asynchronousSocketChannelFinishAccept, th);
            return null;
        }
        synchronized (this.updateLock) {
            try {
                if (completionHandler == null) {
                    this.acceptHandler = null;
                    pendingFuture = new PendingFuture<>(this);
                    this.acceptFuture = pendingFuture;
                } else {
                    this.acceptHandler = completionHandler;
                    this.acceptAttachment = obj;
                    pendingFuture = null;
                }
                this.acceptAcc = System.getSecurityManager() == null ? null : AccessController.getContext();
                this.acceptPending = true;
            } catch (Throwable th3) {
                throw th3;
            }
        }
        this.port.startPoll(this.fdVal, Net.POLLIN);
        return pendingFuture;
    }

    private int accept(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, InetSocketAddress[] inetSocketAddressArr) throws IOException {
        return accept0(fileDescriptor, fileDescriptor2, inetSocketAddressArr);
    }
}
