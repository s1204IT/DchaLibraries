package com.google.common.util.concurrent;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;

public abstract class AbstractListeningExecutorService extends AbstractExecutorService implements ListeningExecutorService {
    @Override
    protected final <T> ListenableFutureTask<T> newTaskFor(Runnable runnable, T t) {
        return ListenableFutureTask.create(runnable, t);
    }

    @Override
    protected final <T> ListenableFutureTask<T> newTaskFor(Callable<T> callable) {
        return ListenableFutureTask.create(callable);
    }

    @Override
    public ListenableFuture<?> submit(Runnable runnable) {
        return (ListenableFuture) super.submit(runnable);
    }

    @Override
    public <T> ListenableFuture<T> submit(Runnable runnable, T t) {
        return (ListenableFuture) super.submit(runnable, (Object) t);
    }

    @Override
    public <T> ListenableFuture<T> submit(Callable<T> callable) {
        return (ListenableFuture) super.submit((Callable) callable);
    }
}
