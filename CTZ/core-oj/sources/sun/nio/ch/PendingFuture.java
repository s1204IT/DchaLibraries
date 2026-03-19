package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class PendingFuture<V, A> implements Future<V> {
    private static final CancellationException CANCELLED = new CancellationException();
    private final A attachment;
    private final AsynchronousChannel channel;
    private volatile Object context;
    private volatile Throwable exc;
    private final CompletionHandler<V, ? super A> handler;
    private volatile boolean haveResult;
    private CountDownLatch latch;
    private volatile V result;
    private Future<?> timeoutTask;

    PendingFuture(AsynchronousChannel asynchronousChannel, CompletionHandler<V, ? super A> completionHandler, A a, Object obj) {
        this.channel = asynchronousChannel;
        this.handler = completionHandler;
        this.attachment = a;
        this.context = obj;
    }

    PendingFuture(AsynchronousChannel asynchronousChannel, CompletionHandler<V, ? super A> completionHandler, A a) {
        this.channel = asynchronousChannel;
        this.handler = completionHandler;
        this.attachment = a;
    }

    PendingFuture(AsynchronousChannel asynchronousChannel) {
        this(asynchronousChannel, null, null);
    }

    PendingFuture(AsynchronousChannel asynchronousChannel, Object obj) {
        this(asynchronousChannel, null, null, obj);
    }

    AsynchronousChannel channel() {
        return this.channel;
    }

    CompletionHandler<V, ? super A> handler() {
        return this.handler;
    }

    A attachment() {
        return this.attachment;
    }

    void setContext(Object obj) {
        this.context = obj;
    }

    Object getContext() {
        return this.context;
    }

    void setTimeoutTask(Future<?> future) {
        synchronized (this) {
            if (this.haveResult) {
                future.cancel(false);
            } else {
                this.timeoutTask = future;
            }
        }
    }

    private boolean prepareForWait() {
        synchronized (this) {
            if (this.haveResult) {
                return false;
            }
            if (this.latch == null) {
                this.latch = new CountDownLatch(1);
            }
            return true;
        }
    }

    void setResult(V v) {
        synchronized (this) {
            if (this.haveResult) {
                return;
            }
            this.result = v;
            this.haveResult = true;
            if (this.timeoutTask != null) {
                this.timeoutTask.cancel(false);
            }
            if (this.latch != null) {
                this.latch.countDown();
            }
        }
    }

    void setFailure(Throwable th) {
        if (!(th instanceof IOException) && !(th instanceof SecurityException)) {
            th = new IOException(th);
        }
        synchronized (this) {
            if (this.haveResult) {
                return;
            }
            this.exc = th;
            this.haveResult = true;
            if (this.timeoutTask != null) {
                this.timeoutTask.cancel(false);
            }
            if (this.latch != null) {
                this.latch.countDown();
            }
        }
    }

    void setResult(V v, Throwable th) {
        if (th == null) {
            setResult(v);
        } else {
            setFailure(th);
        }
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        if (!this.haveResult && prepareForWait()) {
            this.latch.await();
        }
        if (this.exc != null) {
            if (this.exc == CANCELLED) {
                throw new CancellationException();
            }
            throw new ExecutionException(this.exc);
        }
        return this.result;
    }

    @Override
    public V get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        if (!this.haveResult && prepareForWait() && !this.latch.await(j, timeUnit)) {
            throw new TimeoutException();
        }
        if (this.exc != null) {
            if (this.exc == CANCELLED) {
                throw new CancellationException();
            }
            throw new ExecutionException(this.exc);
        }
        return this.result;
    }

    Throwable exception() {
        if (this.exc != CANCELLED) {
            return this.exc;
        }
        return null;
    }

    V value() {
        return this.result;
    }

    @Override
    public boolean isCancelled() {
        return this.exc == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return this.haveResult;
    }

    @Override
    public boolean cancel(boolean z) {
        synchronized (this) {
            if (this.haveResult) {
                return false;
            }
            if (channel() instanceof Cancellable) {
                ((Cancellable) channel()).onCancel(this);
            }
            this.exc = CANCELLED;
            this.haveResult = true;
            if (this.timeoutTask != null) {
                this.timeoutTask.cancel(false);
            }
            if (z) {
                try {
                    channel().close();
                } catch (IOException e) {
                }
            }
            if (this.latch != null) {
                this.latch.countDown();
            }
            return true;
        }
    }
}
