package com.google.common.util.concurrent;

import java.util.concurrent.Callable;

public abstract class ForwardingListeningExecutorService extends ForwardingExecutorService implements ListeningExecutorService {
    @Override
    protected abstract ListeningExecutorService delegate();

    protected ForwardingListeningExecutorService() {
    }

    @Override
    public <T> ListenableFuture<T> submit(Callable<T> callable) {
        return delegate().submit((Callable) callable);
    }

    @Override
    public ListenableFuture<?> submit(Runnable runnable) {
        return delegate().submit(runnable);
    }

    @Override
    public <T> ListenableFuture<T> submit(Runnable runnable, T t) {
        return delegate().submit(runnable, (Object) t);
    }
}
