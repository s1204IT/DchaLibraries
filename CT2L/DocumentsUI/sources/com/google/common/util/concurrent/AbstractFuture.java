package com.google.common.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public abstract class AbstractFuture<V> implements ListenableFuture<V> {
    private final Sync<V> sync = new Sync<>();
    private final ExecutionList executionList = new ExecutionList();

    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return this.sync.get(unit.toNanos(timeout));
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
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!this.sync.cancel()) {
            return false;
        }
        this.executionList.execute();
        if (mayInterruptIfRunning) {
            interruptTask();
        }
        return true;
    }

    protected void interruptTask() {
    }

    protected boolean set(V value) {
        boolean result = this.sync.set(value);
        if (result) {
            this.executionList.execute();
        }
        return result;
    }

    static final class Sync<V> extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 0;
        private Throwable exception;
        private V value;

        Sync() {
        }

        @Override
        protected int tryAcquireShared(int ignored) {
            return isDone() ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int finalState) {
            setState(finalState);
            return true;
        }

        V get(long nanos) throws ExecutionException, CancellationException, InterruptedException, TimeoutException {
            if (!tryAcquireSharedNanos(-1, nanos)) {
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
            switch (state) {
                case 2:
                    if (this.exception != null) {
                        throw new ExecutionException(this.exception);
                    }
                    return this.value;
                case 3:
                default:
                    throw new IllegalStateException("Error, synchronizer in invalid state: " + state);
                case 4:
                    throw new CancellationException("Task was cancelled.");
            }
        }

        boolean isDone() {
            return (getState() & 6) != 0;
        }

        boolean isCancelled() {
            return getState() == 4;
        }

        boolean set(V v) {
            return complete(v, null, 2);
        }

        boolean cancel() {
            return complete(null, null, 4);
        }

        private boolean complete(V v, Throwable t, int finalState) {
            boolean doCompletion = compareAndSetState(0, 1);
            if (doCompletion) {
                this.value = v;
                this.exception = t;
                releaseShared(finalState);
            } else if (getState() == 1) {
                acquireShared(-1);
            }
            return doCompletion;
        }
    }
}
