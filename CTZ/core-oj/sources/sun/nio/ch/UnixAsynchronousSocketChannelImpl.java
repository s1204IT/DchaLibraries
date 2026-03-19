package sun.nio.ch;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.channels.ShutdownChannelGroupException;
import java.security.AccessController;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import sun.net.NetHooks;
import sun.nio.ch.Invoker;
import sun.nio.ch.Port;
import sun.security.action.GetPropertyAction;

class UnixAsynchronousSocketChannelImpl extends AsynchronousSocketChannelImpl implements Port.PollableChannel {
    static final boolean $assertionsDisabled = false;
    private static final boolean disableSynchronousRead;
    private static final NativeDispatcher nd = new SocketDispatcher();
    private Object connectAttachment;
    private PendingFuture<Void, Object> connectFuture;
    private CompletionHandler<Void, Object> connectHandler;
    private boolean connectPending;
    private final int fdVal;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private boolean isGatheringWrite;
    private boolean isScatteringRead;
    private SocketAddress pendingRemote;
    private final Port port;
    private Object readAttachment;
    private ByteBuffer readBuffer;
    private ByteBuffer[] readBuffers;
    private PendingFuture<Number, Object> readFuture;
    private CompletionHandler<Number, Object> readHandler;
    private boolean readPending;
    private Runnable readTimeoutTask;
    private Future<?> readTimer;
    private final Object updateLock;
    private Object writeAttachment;
    private ByteBuffer writeBuffer;
    private ByteBuffer[] writeBuffers;
    private PendingFuture<Number, Object> writeFuture;
    private CompletionHandler<Number, Object> writeHandler;
    private boolean writePending;
    private Runnable writeTimeoutTask;
    private Future<?> writeTimer;

    private enum OpType {
        CONNECT,
        READ,
        WRITE
    }

    private static native void checkConnect(int i) throws IOException;

    static {
        String str = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.disableSynchronousRead", "false"));
        disableSynchronousRead = str.length() == 0 ? true : Boolean.valueOf(str).booleanValue();
    }

    UnixAsynchronousSocketChannelImpl(Port port) throws IOException {
        super(port);
        this.updateLock = new Object();
        this.guard = CloseGuard.get();
        this.readTimeoutTask = new Runnable() {
            @Override
            public void run() {
                synchronized (UnixAsynchronousSocketChannelImpl.this.updateLock) {
                    if (UnixAsynchronousSocketChannelImpl.this.readPending) {
                        UnixAsynchronousSocketChannelImpl.this.readPending = false;
                        CompletionHandler completionHandler = UnixAsynchronousSocketChannelImpl.this.readHandler;
                        Object obj = UnixAsynchronousSocketChannelImpl.this.readAttachment;
                        PendingFuture pendingFuture = UnixAsynchronousSocketChannelImpl.this.readFuture;
                        UnixAsynchronousSocketChannelImpl.this.enableReading(true);
                        InterruptedByTimeoutException interruptedByTimeoutException = new InterruptedByTimeoutException();
                        if (completionHandler == null) {
                            pendingFuture.setFailure(interruptedByTimeoutException);
                        } else {
                            Invoker.invokeIndirectly(UnixAsynchronousSocketChannelImpl.this, (CompletionHandler<Object, ? super Object>) completionHandler, obj, (Object) null, interruptedByTimeoutException);
                        }
                    }
                }
            }
        };
        this.writeTimeoutTask = new Runnable() {
            @Override
            public void run() {
                synchronized (UnixAsynchronousSocketChannelImpl.this.updateLock) {
                    if (UnixAsynchronousSocketChannelImpl.this.writePending) {
                        UnixAsynchronousSocketChannelImpl.this.writePending = false;
                        CompletionHandler completionHandler = UnixAsynchronousSocketChannelImpl.this.writeHandler;
                        Object obj = UnixAsynchronousSocketChannelImpl.this.writeAttachment;
                        PendingFuture pendingFuture = UnixAsynchronousSocketChannelImpl.this.writeFuture;
                        UnixAsynchronousSocketChannelImpl.this.enableWriting(true);
                        InterruptedByTimeoutException interruptedByTimeoutException = new InterruptedByTimeoutException();
                        if (completionHandler != null) {
                            Invoker.invokeIndirectly(UnixAsynchronousSocketChannelImpl.this, (CompletionHandler<Object, ? super Object>) completionHandler, obj, (Object) null, interruptedByTimeoutException);
                        } else {
                            pendingFuture.setFailure(interruptedByTimeoutException);
                        }
                    }
                }
            }
        };
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

    UnixAsynchronousSocketChannelImpl(Port port, FileDescriptor fileDescriptor, InetSocketAddress inetSocketAddress) throws IOException {
        super(port, fileDescriptor, inetSocketAddress);
        this.updateLock = new Object();
        this.guard = CloseGuard.get();
        this.readTimeoutTask = new Runnable() {
            @Override
            public void run() {
                synchronized (UnixAsynchronousSocketChannelImpl.this.updateLock) {
                    if (UnixAsynchronousSocketChannelImpl.this.readPending) {
                        UnixAsynchronousSocketChannelImpl.this.readPending = false;
                        CompletionHandler completionHandler = UnixAsynchronousSocketChannelImpl.this.readHandler;
                        Object obj = UnixAsynchronousSocketChannelImpl.this.readAttachment;
                        PendingFuture pendingFuture = UnixAsynchronousSocketChannelImpl.this.readFuture;
                        UnixAsynchronousSocketChannelImpl.this.enableReading(true);
                        InterruptedByTimeoutException interruptedByTimeoutException = new InterruptedByTimeoutException();
                        if (completionHandler == null) {
                            pendingFuture.setFailure(interruptedByTimeoutException);
                        } else {
                            Invoker.invokeIndirectly(UnixAsynchronousSocketChannelImpl.this, (CompletionHandler<Object, ? super Object>) completionHandler, obj, (Object) null, interruptedByTimeoutException);
                        }
                    }
                }
            }
        };
        this.writeTimeoutTask = new Runnable() {
            @Override
            public void run() {
                synchronized (UnixAsynchronousSocketChannelImpl.this.updateLock) {
                    if (UnixAsynchronousSocketChannelImpl.this.writePending) {
                        UnixAsynchronousSocketChannelImpl.this.writePending = false;
                        CompletionHandler completionHandler = UnixAsynchronousSocketChannelImpl.this.writeHandler;
                        Object obj = UnixAsynchronousSocketChannelImpl.this.writeAttachment;
                        PendingFuture pendingFuture = UnixAsynchronousSocketChannelImpl.this.writeFuture;
                        UnixAsynchronousSocketChannelImpl.this.enableWriting(true);
                        InterruptedByTimeoutException interruptedByTimeoutException = new InterruptedByTimeoutException();
                        if (completionHandler != null) {
                            Invoker.invokeIndirectly(UnixAsynchronousSocketChannelImpl.this, (CompletionHandler<Object, ? super Object>) completionHandler, obj, (Object) null, interruptedByTimeoutException);
                        } else {
                            pendingFuture.setFailure(interruptedByTimeoutException);
                        }
                    }
                }
            }
        };
        this.fdVal = IOUtil.fdVal(fileDescriptor);
        IOUtil.configureBlocking(fileDescriptor, false);
        try {
            port.register(this.fdVal, this);
            this.port = port;
            this.guard.open("close");
        } catch (ShutdownChannelGroupException e) {
            throw new IOException(e);
        }
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return this.port;
    }

    private void updateEvents() {
        int i = this.readPending ? 0 | Net.POLLIN : 0;
        if (this.connectPending || this.writePending) {
            i |= Net.POLLOUT;
        }
        if (i != 0) {
            this.port.startPoll(this.fdVal, i);
        }
    }

    private void lockAndUpdateEvents() {
        synchronized (this.updateLock) {
            updateEvents();
        }
    }

    private void finish(boolean z, boolean z2, boolean z3) {
        boolean z4;
        boolean z5;
        boolean z6;
        synchronized (this.updateLock) {
            z4 = true;
            if (z2) {
                try {
                    if (this.readPending) {
                        this.readPending = false;
                        z5 = true;
                    } else {
                        z5 = false;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (!z3) {
                z6 = false;
                z4 = false;
            } else if (this.writePending) {
                this.writePending = false;
                z6 = false;
            } else if (this.connectPending) {
                this.connectPending = false;
                z6 = true;
                z4 = false;
            }
        }
        if (z5) {
            if (z4) {
                finishWrite(false);
            }
            finishRead(z);
        } else {
            if (z4) {
                finishWrite(z);
            }
            if (z6) {
                finishConnect(z);
            }
        }
    }

    @Override
    public void onEvent(int i, boolean z) {
        boolean z2 = (Net.POLLIN & i) > 0;
        boolean z3 = (Net.POLLOUT & i) > 0;
        if ((i & (Net.POLLERR | Net.POLLHUP)) > 0) {
            z2 = true;
            z3 = true;
        }
        finish(z, z2, z3);
    }

    @Override
    void implClose() throws IOException {
        this.guard.close();
        this.port.unregister(this.fdVal);
        nd.close(this.fd);
        finish(false, true, true);
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
    public void onCancel(PendingFuture<?, ?> pendingFuture) {
        if (pendingFuture.getContext() == OpType.CONNECT) {
            killConnect();
        }
        if (pendingFuture.getContext() == OpType.READ) {
            killReading();
        }
        if (pendingFuture.getContext() == OpType.WRITE) {
            killWriting();
        }
    }

    private void setConnected() throws IOException {
        synchronized (this.stateLock) {
            this.state = 2;
            this.localAddress = Net.localAddress(this.fd);
            this.remoteAddress = (InetSocketAddress) this.pendingRemote;
        }
    }

    private void finishConnect(boolean z) {
        try {
            try {
                begin();
                checkConnect(this.fdVal);
                setConnected();
                end();
                th = null;
            } catch (Throwable th) {
                th = th;
                if (th instanceof ClosedChannelException) {
                    th = new AsynchronousCloseException();
                }
                end();
            }
            if (th != null) {
                try {
                    close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            CompletionHandler<Void, Object> completionHandler = this.connectHandler;
            Object obj = this.connectAttachment;
            PendingFuture<Void, Object> pendingFuture = this.connectFuture;
            if (completionHandler == null) {
                pendingFuture.setResult(null, th);
            } else if (z) {
                Invoker.invokeUnchecked(completionHandler, obj, null, th);
            } else {
                Invoker.invokeIndirectly(this, completionHandler, obj, (Object) null, th);
            }
        } catch (Throwable th3) {
            end();
            throw th3;
        }
    }

    @Override
    <A> Future<Void> implConnect(SocketAddress socketAddress, A a, CompletionHandler<Void, ? super A> completionHandler) {
        boolean z;
        PendingFuture<Void, Object> pendingFuture;
        if (!isOpen()) {
            ClosedChannelException closedChannelException = new ClosedChannelException();
            if (completionHandler == null) {
                return CompletedFuture.withFailure(closedChannelException);
            }
            Invoker.invoke(this, completionHandler, a, null, closedChannelException);
            return null;
        }
        InetSocketAddress inetSocketAddressCheckAddress = Net.checkAddress(socketAddress);
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkConnect(inetSocketAddressCheckAddress.getAddress().getHostAddress(), inetSocketAddressCheckAddress.getPort());
        }
        synchronized (this.stateLock) {
            if (this.state == 2) {
                throw new AlreadyConnectedException();
            }
            if (this.state == 1) {
                throw new ConnectionPendingException();
            }
            this.state = 1;
            this.pendingRemote = socketAddress;
            z = this.localAddress == null;
            try {
            } catch (Throwable th) {
                th = th;
                if (th instanceof ClosedChannelException) {
                    th = new AsynchronousCloseException();
                }
            } finally {
                end();
            }
        }
        begin();
        if (z) {
            NetHooks.beforeTcpConnect(this.fd, inetSocketAddressCheckAddress.getAddress(), inetSocketAddressCheckAddress.getPort());
        }
        if (Net.connect(this.fd, inetSocketAddressCheckAddress.getAddress(), inetSocketAddressCheckAddress.getPort()) == -2) {
            synchronized (this.updateLock) {
                try {
                    if (completionHandler == null) {
                        pendingFuture = new PendingFuture<>(this, OpType.CONNECT);
                        this.connectFuture = pendingFuture;
                    } else {
                        this.connectHandler = completionHandler;
                        this.connectAttachment = a;
                        pendingFuture = null;
                    }
                    this.connectPending = true;
                    updateEvents();
                } finally {
                }
            }
            return pendingFuture;
        }
        setConnected();
        end();
        th = null;
        if (th != null) {
            try {
                close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }
        if (completionHandler == null) {
            return CompletedFuture.withResult(null, th);
        }
        Invoker.invoke(this, completionHandler, a, null, th);
        return null;
    }

    private void finishRead(boolean z) {
        boolean z2 = this.isScatteringRead;
        CompletionHandler<Number, Object> completionHandler = this.readHandler;
        Object obj = this.readAttachment;
        PendingFuture<Number, Object> pendingFuture = this.readFuture;
        Future<?> future = this.readTimer;
        int i = -1;
        try {
            try {
                begin();
                i = z2 ? (int) IOUtil.read(this.fd, this.readBuffers, nd) : IOUtil.read(this.fd, this.readBuffer, -1L, nd);
            } catch (Throwable th) {
                th = th;
                enableReading();
                if (th instanceof ClosedChannelException) {
                    th = new AsynchronousCloseException();
                }
                if (!(th instanceof AsynchronousCloseException)) {
                    lockAndUpdateEvents();
                }
                end();
            }
            if (i == -2) {
                synchronized (this.updateLock) {
                    this.readPending = true;
                }
                return;
            }
            this.readBuffer = null;
            this.readBuffers = null;
            this.readAttachment = null;
            enableReading();
            if (!(numberValueOf instanceof AsynchronousCloseException)) {
                lockAndUpdateEvents();
            }
            end();
            th = null;
            if (future != null) {
                future.cancel(false);
            }
            numberValueOf = th == null ? z2 ? Long.valueOf(i) : Integer.valueOf(i) : null;
            if (completionHandler == null) {
                pendingFuture.setResult(numberValueOf, th);
            } else if (z) {
                Invoker.invokeUnchecked(completionHandler, obj, numberValueOf, th);
            } else {
                Invoker.invokeIndirectly(this, (CompletionHandler<Number, ? super Object>) completionHandler, obj, numberValueOf, th);
            }
        } finally {
            if (!(numberValueOf instanceof AsynchronousCloseException)) {
                lockAndUpdateEvents();
            }
            end();
        }
    }

    @Override
    <V extends Number, A> Future<V> implRead(boolean z, ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, long j, TimeUnit timeUnit, A a, CompletionHandler<V, ? super A> completionHandler) {
        boolean zMayInvokeDirect;
        Invoker.GroupAndInvokeCount groupAndInvokeCount;
        int i;
        PendingFuture<Number, Object> pendingFuture;
        boolean z2 = false;
        if (disableSynchronousRead) {
            zMayInvokeDirect = false;
            groupAndInvokeCount = null;
        } else {
            if (completionHandler == null) {
                zMayInvokeDirect = false;
                groupAndInvokeCount = null;
            } else {
                groupAndInvokeCount = Invoker.getGroupAndInvokeCount();
                zMayInvokeDirect = Invoker.mayInvokeDirect(groupAndInvokeCount, this.port);
                if (zMayInvokeDirect || !this.port.isFixedThreadPool()) {
                }
            }
            z2 = true;
        }
        try {
            try {
                begin();
                i = z2 ? z ? (int) IOUtil.read(this.fd, byteBufferArr, nd) : IOUtil.read(this.fd, byteBuffer, -1L, nd) : -2;
            } catch (Throwable th) {
                th = th;
                i = -2;
            }
            if (i != -2) {
                enableReading();
                end();
                th = null;
                Object objValueOf = th == null ? null : z ? Long.valueOf(i) : Integer.valueOf(i);
                if (completionHandler != null) {
                    return CompletedFuture.withResult(objValueOf, th);
                }
                if (zMayInvokeDirect) {
                    Invoker.invokeDirect(groupAndInvokeCount, completionHandler, a, objValueOf, th);
                } else {
                    Invoker.invokeIndirectly(this, completionHandler, a, objValueOf, th);
                }
                return null;
            }
            try {
                synchronized (this.updateLock) {
                    this.isScatteringRead = z;
                    this.readBuffer = byteBuffer;
                    this.readBuffers = byteBufferArr;
                    if (completionHandler == null) {
                        this.readHandler = null;
                        pendingFuture = new PendingFuture<>(this, OpType.READ);
                        this.readFuture = pendingFuture;
                        this.readAttachment = null;
                    } else {
                        this.readHandler = completionHandler;
                        this.readAttachment = a;
                        this.readFuture = null;
                        pendingFuture = null;
                    }
                    if (j > 0) {
                        this.readTimer = this.port.schedule(this.readTimeoutTask, j, timeUnit);
                    }
                    this.readPending = true;
                    updateEvents();
                }
                end();
                return pendingFuture;
            } catch (Throwable th2) {
                th = th2;
                if (th instanceof ClosedChannelException) {
                    th = new AsynchronousCloseException();
                }
                if (th == null) {
                }
                if (completionHandler != null) {
                }
            }
        } finally {
            enableReading();
            end();
        }
    }

    private void finishWrite(boolean z) {
        boolean z2 = this.isGatheringWrite;
        CompletionHandler<Number, Object> completionHandler = this.writeHandler;
        Object obj = this.writeAttachment;
        PendingFuture<Number, Object> pendingFuture = this.writeFuture;
        Future<?> future = this.writeTimer;
        int iWrite = -1;
        try {
            try {
                begin();
                iWrite = z2 ? (int) IOUtil.write(this.fd, this.writeBuffers, nd) : IOUtil.write(this.fd, this.writeBuffer, -1L, nd);
            } catch (Throwable th) {
                th = th;
                enableWriting();
                if (th instanceof ClosedChannelException) {
                    th = new AsynchronousCloseException();
                }
                if (!(th instanceof AsynchronousCloseException)) {
                    lockAndUpdateEvents();
                }
                end();
            }
            if (iWrite == -2) {
                synchronized (this.updateLock) {
                    this.writePending = true;
                }
                return;
            }
            this.writeBuffer = null;
            this.writeBuffers = null;
            this.writeAttachment = null;
            enableWriting();
            if (!(numberValueOf instanceof AsynchronousCloseException)) {
                lockAndUpdateEvents();
            }
            end();
            th = null;
            if (future != null) {
                future.cancel(false);
            }
            numberValueOf = th == null ? z2 ? Long.valueOf(iWrite) : Integer.valueOf(iWrite) : null;
            if (completionHandler == null) {
                pendingFuture.setResult(numberValueOf, th);
            } else if (z) {
                Invoker.invokeUnchecked(completionHandler, obj, numberValueOf, th);
            } else {
                Invoker.invokeIndirectly(this, (CompletionHandler<Number, ? super Object>) completionHandler, obj, numberValueOf, th);
            }
        } finally {
            if (!(numberValueOf instanceof AsynchronousCloseException)) {
                lockAndUpdateEvents();
            }
            end();
        }
    }

    @Override
    <V extends Number, A> Future<V> implWrite(boolean z, ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, long j, TimeUnit timeUnit, A a, CompletionHandler<V, ? super A> completionHandler) {
        int iWrite;
        PendingFuture<Number, Object> pendingFuture;
        Invoker.GroupAndInvokeCount groupAndInvokeCount = Invoker.getGroupAndInvokeCount();
        boolean zMayInvokeDirect = Invoker.mayInvokeDirect(groupAndInvokeCount, this.port);
        boolean z2 = completionHandler == null || zMayInvokeDirect || !this.port.isFixedThreadPool();
        try {
            try {
                begin();
                iWrite = z2 ? z ? (int) IOUtil.write(this.fd, byteBufferArr, nd) : IOUtil.write(this.fd, byteBuffer, -1L, nd) : -2;
            } catch (Throwable th) {
                th = th;
                iWrite = -2;
            }
            if (iWrite != -2) {
                enableWriting();
                end();
                th = null;
                Object objValueOf = th == null ? null : z ? Long.valueOf(iWrite) : Integer.valueOf(iWrite);
                if (completionHandler != null) {
                    return CompletedFuture.withResult(objValueOf, th);
                }
                if (zMayInvokeDirect) {
                    Invoker.invokeDirect(groupAndInvokeCount, completionHandler, a, objValueOf, th);
                } else {
                    Invoker.invokeIndirectly(this, completionHandler, a, objValueOf, th);
                }
                return null;
            }
            try {
                synchronized (this.updateLock) {
                    this.isGatheringWrite = z;
                    this.writeBuffer = byteBuffer;
                    this.writeBuffers = byteBufferArr;
                    if (completionHandler == null) {
                        this.writeHandler = null;
                        pendingFuture = new PendingFuture<>(this, OpType.WRITE);
                        this.writeFuture = pendingFuture;
                        this.writeAttachment = null;
                    } else {
                        this.writeHandler = completionHandler;
                        this.writeAttachment = a;
                        this.writeFuture = null;
                        pendingFuture = null;
                    }
                    if (j > 0) {
                        this.writeTimer = this.port.schedule(this.writeTimeoutTask, j, timeUnit);
                    }
                    this.writePending = true;
                    updateEvents();
                }
                end();
                return pendingFuture;
            } catch (Throwable th2) {
                th = th2;
                if (th instanceof ClosedChannelException) {
                    th = new AsynchronousCloseException();
                }
                if (th == null) {
                }
                if (completionHandler != null) {
                }
            }
        } finally {
            enableWriting();
            end();
        }
    }
}
