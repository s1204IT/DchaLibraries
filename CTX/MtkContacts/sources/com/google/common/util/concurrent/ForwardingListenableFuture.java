package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.concurrent.Executor;

public abstract class ForwardingListenableFuture<V> extends ForwardingFuture<V> implements ListenableFuture<V> {
    @Override
    protected abstract ListenableFuture<V> delegate();

    protected ForwardingListenableFuture() {
    }

    @Override
    public void addListener(Runnable runnable, Executor executor) {
        delegate().addListener(runnable, executor);
    }

    public static abstract class SimpleForwardingListenableFuture<V> extends ForwardingListenableFuture<V> {
        private final ListenableFuture<V> delegate;

        protected SimpleForwardingListenableFuture(ListenableFuture<V> listenableFuture) {
            this.delegate = (ListenableFuture) Preconditions.checkNotNull(listenableFuture);
        }

        @Override
        protected final ListenableFuture<V> delegate() {
            return this.delegate;
        }
    }
}
