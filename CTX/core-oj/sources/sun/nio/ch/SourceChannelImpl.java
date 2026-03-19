package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.spi.SelectorProvider;

class SourceChannelImpl extends Pipe.SourceChannel implements SelChImpl {
    static final boolean $assertionsDisabled = false;
    private static final int ST_INUSE = 0;
    private static final int ST_KILLED = 1;
    private static final int ST_UNINITIALIZED = -1;
    private static final NativeDispatcher nd = new FileDispatcherImpl();
    FileDescriptor fd;
    int fdVal;
    private final Object lock;
    private volatile int state;
    private final Object stateLock;
    private volatile long thread;

    @Override
    public FileDescriptor getFD() {
        return this.fd;
    }

    @Override
    public int getFDVal() {
        return this.fdVal;
    }

    SourceChannelImpl(SelectorProvider selectorProvider, FileDescriptor fileDescriptor) {
        super(selectorProvider);
        this.thread = 0L;
        this.lock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.fd = fileDescriptor;
        this.fdVal = IOUtil.fdVal(fileDescriptor);
        this.state = 0;
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

    @Override
    protected void implConfigureBlocking(boolean z) throws IOException {
        IOUtil.configureBlocking(this.fd, z);
    }

    public boolean translateReadyOps(int i, int i2, SelectionKeyImpl selectionKeyImpl) {
        int iNioInterestOps = selectionKeyImpl.nioInterestOps();
        int iNioReadyOps = selectionKeyImpl.nioReadyOps();
        if ((Net.POLLNVAL & i) != 0) {
            throw new Error("POLLNVAL detected");
        }
        if (((Net.POLLERR | Net.POLLHUP) & i) != 0) {
            selectionKeyImpl.nioReadyOps(iNioInterestOps);
            if (((~iNioReadyOps) & iNioInterestOps) != 0) {
                return true;
            }
            return $assertionsDisabled;
        }
        if ((i & Net.POLLIN) != 0 && (iNioInterestOps & 1) != 0) {
            i2 |= 1;
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

    @Override
    public void translateAndSetInterestOps(int i, SelectionKeyImpl selectionKeyImpl) {
        if (i == 1) {
            i = Net.POLLIN;
        }
        selectionKeyImpl.selector.putEventOps(selectionKeyImpl, i);
    }

    private void ensureOpen() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        int i;
        int i2;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        ensureOpen();
        synchronized (this.lock) {
            boolean z = true;
            try {
                begin();
                if (!isOpen()) {
                    this.thread = 0L;
                    end($assertionsDisabled);
                    return 0;
                }
                this.thread = NativeThread.current();
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
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                int iNormalize = IOStatus.normalize(i2);
                this.thread = 0L;
                if (i2 <= 0 && i2 != -2) {
                    z = false;
                }
                end(z);
                return iNormalize;
            } catch (Throwable th3) {
                th = th3;
                i = 0;
            }
            this.thread = 0L;
            if (i <= 0 && i != -2) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    public long read(ByteBuffer[] byteBufferArr, int i, int i2) throws IOException {
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        return read(Util.subsequence(byteBufferArr, i, i2));
    }

    @Override
    public long read(ByteBuffer[] byteBufferArr) throws IOException {
        long j;
        long j2;
        if (byteBufferArr == null) {
            throw new NullPointerException();
        }
        ensureOpen();
        synchronized (this.lock) {
            boolean z = true;
            try {
                begin();
                if (!isOpen()) {
                    this.thread = 0L;
                    end($assertionsDisabled);
                    return 0L;
                }
                this.thread = NativeThread.current();
                j = 0;
                while (true) {
                    try {
                        j2 = IOUtil.read(this.fd, byteBufferArr, nd);
                        if (j2 != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            j = j2;
                        } catch (Throwable th) {
                            th = th;
                            j = j2;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                long jNormalize = IOStatus.normalize(j2);
                this.thread = 0L;
                if (j2 <= 0 && j2 != -2) {
                    z = false;
                }
                end(z);
                return jNormalize;
            } catch (Throwable th3) {
                th = th3;
                j = 0;
            }
            this.thread = 0L;
            if (j <= 0 && j != -2) {
                z = false;
            }
            end(z);
            throw th;
        }
    }
}
