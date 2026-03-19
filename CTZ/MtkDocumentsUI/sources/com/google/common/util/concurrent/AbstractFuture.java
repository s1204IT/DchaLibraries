package com.google.common.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public abstract class AbstractFuture<V> implements ListenableFuture<V> {
    private final Sync<V> sync = new Sync<>();
    private final ExecutionList executionList = new ExecutionList();

    protected AbstractFuture() {
    }

    @Override
    public V get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        return this.sync.get(timeUnit.toNanos(j));
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        return this.sync.get();
    }

    @Override
    public boolean isDone() {
        return this.sync.isDone();
    }

    @Override
    public boolean isCancelled() {
        return this.sync.isCancelled();
    }

    @Override
    public boolean cancel(boolean z) {
        if (!this.sync.cancel(z)) {
            return false;
        }
        this.executionList.execute();
        if (z) {
            interruptTask();
            return true;
        }
        return true;
    }

    protected void interruptTask() {
    }

    protected boolean set(V v) {
        boolean z = this.sync.set(v);
        if (z) {
            this.executionList.execute();
        }
        return z;
    }

    static final class Sync<V> extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 0;
        private Throwable exception;
        private V value;

        Sync() {
        }

        @Override
        protected int tryAcquireShared(int i) {
            if (isDone()) {
                return 1;
            }
            return -1;
        }

        @Override
        protected boolean tryReleaseShared(int i) {
            setState(i);
            return true;
        }

        V get(long j) throws ExecutionException, CancellationException, InterruptedException, TimeoutException {
            if (!tryAcquireSharedNanos(-1, j)) {
                throw new TimeoutException("Timeout waiting for task.");
            }
            return getValue();
        }

        V get() throws ExecutionException, CancellationException, InterruptedException {
            acquireSharedInterruptibly(-1);
            return getValue();
        }

        private V getValue() throws ExecutionException, CancellationException {
            int state = getState();
            if (state == 2) {
                if (this.exception != null) {
                    throw new ExecutionException(this.exception);
                }
                return this.value;
            }
            if (state == 4 || state == 8) {
                throw AbstractFuture.cancellationExceptionWithCause("Task was cancelled.", this.exception);
            }
            throw new IllegalStateException("Error, synchronizer in invalid state: " + state);
        }

        boolean isDone() {
            return (getState() & 14) != 0;
        }

        boolean isCancelled() {
            return (getState() & 12) != 0;
        }

        boolean set(V v) {
            return complete(v, null, 2);
        }

        boolean cancel(boolean z) {
            return complete(null, null, z ? 8 : 4);
        }

        private boolean complete(V v, Throwable th, int i) {
            boolean zCompareAndSetState = compareAndSetState(0, 1);
            if (!zCompareAndSetState) {
                if (getState() == 1) {
                    acquireShared(-1);
                }
            } else {
                this.value = v;
                if ((i & 12) != 0) {
                    th = new CancellationException("Future.cancel() was called.");
                }
                this.exception = th;
                releaseShared(i);
            }
            return zCompareAndSetState;
        }
    }

    static final CancellationException cancellationExceptionWithCause(String str, Throwable th) {
        CancellationException cancellationException = new CancellationException(str);
        cancellationException.initCause(th);
        return cancellationException;
    }
}
