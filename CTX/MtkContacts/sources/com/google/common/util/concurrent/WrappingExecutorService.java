package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class WrappingExecutorService implements ExecutorService {
    private final ExecutorService delegate;

    protected abstract <T> Callable<T> wrapTask(Callable<T> callable);

    protected WrappingExecutorService(ExecutorService executorService) {
        this.delegate = (ExecutorService) Preconditions.checkNotNull(executorService);
    }

    protected Runnable wrapTask(Runnable runnable) {
        final Callable callableWrapTask = wrapTask(Executors.callable(runnable, null));
        return new Runnable() {
            @Override
            public void run() throws Throwable {
                try {
                    callableWrapTask.call();
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
        };
    }

    private final <T> ImmutableList<Callable<T>> wrapTasks(Collection<? extends Callable<T>> collection) {
        ImmutableList.Builder builder = ImmutableList.builder();
        Iterator<? extends Callable<T>> it = collection.iterator();
        while (it.hasNext()) {
            builder.add(wrapTask(it.next()));
        }
        return builder.build();
    }

    @Override
    public final void execute(Runnable runnable) {
        this.delegate.execute(wrapTask(runnable));
    }

    @Override
    public final <T> Future<T> submit(Callable<T> callable) {
        return this.delegate.submit(wrapTask((Callable) Preconditions.checkNotNull(callable)));
    }

    @Override
    public final Future<?> submit(Runnable runnable) {
        return this.delegate.submit(wrapTask(runnable));
    }

    @Override
    public final <T> Future<T> submit(Runnable runnable, T t) {
        return this.delegate.submit(wrapTask(runnable), t);
    }

    @Override
    public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
        return this.delegate.invokeAll(wrapTasks(collection));
    }

    @Override
    public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long j, TimeUnit timeUnit) throws InterruptedException {
        return this.delegate.invokeAll(wrapTasks(collection), j, timeUnit);
    }

    @Override
    public final <T> T invokeAny(Collection<? extends Callable<T>> collection) throws ExecutionException, InterruptedException {
        return (T) this.delegate.invokeAny(wrapTasks(collection));
    }

    @Override
    public final <T> T invokeAny(Collection<? extends Callable<T>> collection, long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        return (T) this.delegate.invokeAny(wrapTasks(collection), j, timeUnit);
    }

    @Override
    public final void shutdown() {
        this.delegate.shutdown();
    }

    @Override
    public final List<Runnable> shutdownNow() {
        return this.delegate.shutdownNow();
    }

    @Override
    public final boolean isShutdown() {
        return this.delegate.isShutdown();
    }

    @Override
    public final boolean isTerminated() {
        return this.delegate.isTerminated();
    }

    @Override
    public final boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
        return this.delegate.awaitTermination(j, timeUnit);
    }
}
