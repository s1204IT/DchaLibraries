package com.google.common.util.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Futures {
    private static final AsyncFunction<ListenableFuture<Object>, Object> DEREFERENCER = new AsyncFunction<ListenableFuture<Object>, Object>() {
        @Override
        public ListenableFuture<Object> apply(ListenableFuture<Object> listenableFuture) {
            return listenableFuture;
        }
    };
    private static final Ordering<Constructor<?>> WITH_STRING_PARAM_FIRST = Ordering.natural().onResultOf(new Function<Constructor<?>, Boolean>() {
        @Override
        public Boolean apply(Constructor<?> constructor) {
            return Boolean.valueOf(Arrays.asList(constructor.getParameterTypes()).contains(String.class));
        }
    }).reverse();

    private interface FutureCombiner<V, C> {
        C combine(List<Optional<V>> list);
    }

    private Futures() {
    }

    public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(ListenableFuture<V> listenableFuture, Function<? super Exception, X> function) {
        return new MappingCheckedFuture((ListenableFuture) Preconditions.checkNotNull(listenableFuture), function);
    }

    private static abstract class ImmediateFuture<V> implements ListenableFuture<V> {
        private static final Logger log = Logger.getLogger(ImmediateFuture.class.getName());

        @Override
        public abstract V get() throws ExecutionException;

        private ImmediateFuture() {
        }

        @Override
        public void addListener(Runnable runnable, Executor executor) {
            Preconditions.checkNotNull(runnable, "Runnable was null.");
            Preconditions.checkNotNull(executor, "Executor was null.");
            try {
                executor.execute(runnable);
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "RuntimeException while executing runnable " + runnable + " with executor " + executor, (Throwable) e);
            }
        }

        @Override
        public boolean cancel(boolean z) {
            return false;
        }

        @Override
        public V get(long j, TimeUnit timeUnit) throws ExecutionException {
            Preconditions.checkNotNull(timeUnit);
            return get();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }

    private static class ImmediateSuccessfulFuture<V> extends ImmediateFuture<V> {
        private final V value;

        ImmediateSuccessfulFuture(V v) {
            super();
            this.value = v;
        }

        @Override
        public V get() {
            return this.value;
        }
    }

    private static class ImmediateSuccessfulCheckedFuture<V, X extends Exception> extends ImmediateFuture<V> implements CheckedFuture<V, X> {
        private final V value;

        ImmediateSuccessfulCheckedFuture(V v) {
            super();
            this.value = v;
        }

        @Override
        public V get() {
            return this.value;
        }

        @Override
        public V checkedGet() {
            return this.value;
        }

        @Override
        public V checkedGet(long j, TimeUnit timeUnit) {
            Preconditions.checkNotNull(timeUnit);
            return this.value;
        }
    }

    private static class ImmediateFailedFuture<V> extends ImmediateFuture<V> {
        private final Throwable thrown;

        ImmediateFailedFuture(Throwable th) {
            super();
            this.thrown = th;
        }

        @Override
        public V get() throws ExecutionException {
            throw new ExecutionException(this.thrown);
        }
    }

    private static class ImmediateCancelledFuture<V> extends ImmediateFuture<V> {
        private final CancellationException thrown;

        ImmediateCancelledFuture() {
            super();
            this.thrown = new CancellationException("Immediate cancelled future.");
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public V get() {
            throw AbstractFuture.cancellationExceptionWithCause("Task was cancelled.", this.thrown);
        }
    }

    private static class ImmediateFailedCheckedFuture<V, X extends Exception> extends ImmediateFuture<V> implements CheckedFuture<V, X> {
        private final X thrown;

        ImmediateFailedCheckedFuture(X x) {
            super();
            this.thrown = x;
        }

        @Override
        public V get() throws ExecutionException {
            throw new ExecutionException(this.thrown);
        }

        @Override
        public V checkedGet() throws Exception {
            throw this.thrown;
        }

        @Override
        public V checkedGet(long j, TimeUnit timeUnit) throws Exception {
            Preconditions.checkNotNull(timeUnit);
            throw this.thrown;
        }
    }

    public static <V> ListenableFuture<V> immediateFuture(V v) {
        return new ImmediateSuccessfulFuture(v);
    }

    public static <V, X extends Exception> CheckedFuture<V, X> immediateCheckedFuture(V v) {
        return new ImmediateSuccessfulCheckedFuture(v);
    }

    public static <V> ListenableFuture<V> immediateFailedFuture(Throwable th) {
        Preconditions.checkNotNull(th);
        return new ImmediateFailedFuture(th);
    }

    public static <V> ListenableFuture<V> immediateCancelledFuture() {
        return new ImmediateCancelledFuture();
    }

    public static <V, X extends Exception> CheckedFuture<V, X> immediateFailedCheckedFuture(X x) {
        Preconditions.checkNotNull(x);
        return new ImmediateFailedCheckedFuture(x);
    }

    public static <V> ListenableFuture<V> withFallback(ListenableFuture<? extends V> listenableFuture, FutureFallback<? extends V> futureFallback) {
        return withFallback(listenableFuture, futureFallback, MoreExecutors.directExecutor());
    }

    public static <V> ListenableFuture<V> withFallback(ListenableFuture<? extends V> listenableFuture, FutureFallback<? extends V> futureFallback, Executor executor) {
        Preconditions.checkNotNull(futureFallback);
        return new FallbackFuture(listenableFuture, futureFallback, executor);
    }

    private static class FallbackFuture<V> extends AbstractFuture<V> {
        private volatile ListenableFuture<? extends V> running;

        FallbackFuture(ListenableFuture<? extends V> listenableFuture, final FutureFallback<? extends V> futureFallback, Executor executor) {
            this.running = listenableFuture;
            Futures.addCallback(this.running, new FutureCallback<V>() {
                @Override
                public void onSuccess(V v) {
                    FallbackFuture.this.set(v);
                }

                @Override
                public void onFailure(Throwable th) {
                    if (FallbackFuture.this.isCancelled()) {
                        return;
                    }
                    try {
                        FallbackFuture.this.running = futureFallback.create(th);
                        if (FallbackFuture.this.isCancelled()) {
                            FallbackFuture.this.running.cancel(FallbackFuture.this.wasInterrupted());
                        } else {
                            Futures.addCallback(FallbackFuture.this.running, new FutureCallback<V>() {
                                @Override
                                public void onSuccess(V v) {
                                    FallbackFuture.this.set(v);
                                }

                                @Override
                                public void onFailure(Throwable th2) {
                                    if (FallbackFuture.this.running.isCancelled()) {
                                        FallbackFuture.this.cancel(false);
                                    } else {
                                        FallbackFuture.this.setException(th2);
                                    }
                                }
                            }, MoreExecutors.directExecutor());
                        }
                    } catch (Throwable th2) {
                        FallbackFuture.this.setException(th2);
                    }
                }
            }, executor);
        }

        @Override
        public boolean cancel(boolean z) {
            if (super.cancel(z)) {
                this.running.cancel(z);
                return true;
            }
            return false;
        }
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> listenableFuture, AsyncFunction<? super I, ? extends O> asyncFunction) {
        ChainingListenableFuture chainingListenableFuture = new ChainingListenableFuture(asyncFunction, listenableFuture);
        listenableFuture.addListener(chainingListenableFuture, MoreExecutors.directExecutor());
        return chainingListenableFuture;
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> listenableFuture, AsyncFunction<? super I, ? extends O> asyncFunction, Executor executor) {
        Preconditions.checkNotNull(executor);
        ChainingListenableFuture chainingListenableFuture = new ChainingListenableFuture(asyncFunction, listenableFuture);
        listenableFuture.addListener(rejectionPropagatingRunnable(chainingListenableFuture, chainingListenableFuture, executor), MoreExecutors.directExecutor());
        return chainingListenableFuture;
    }

    private static Runnable rejectionPropagatingRunnable(final AbstractFuture<?> abstractFuture, final Runnable runnable, final Executor executor) {
        return new Runnable() {
            @Override
            public void run() {
                final AtomicBoolean atomicBoolean = new AtomicBoolean(true);
                try {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            atomicBoolean.set(false);
                            runnable.run();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    if (atomicBoolean.get()) {
                        abstractFuture.setException(e);
                    }
                }
            }
        };
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> listenableFuture, Function<? super I, ? extends O> function) {
        Preconditions.checkNotNull(function);
        ChainingListenableFuture chainingListenableFuture = new ChainingListenableFuture(asAsyncFunction(function), listenableFuture);
        listenableFuture.addListener(chainingListenableFuture, MoreExecutors.directExecutor());
        return chainingListenableFuture;
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> listenableFuture, Function<? super I, ? extends O> function, Executor executor) {
        Preconditions.checkNotNull(function);
        return transform(listenableFuture, asAsyncFunction(function), executor);
    }

    private static <I, O> AsyncFunction<I, O> asAsyncFunction(final Function<? super I, ? extends O> function) {
        return new AsyncFunction<I, O>() {
            @Override
            public ListenableFuture<O> apply(I i) {
                return Futures.immediateFuture(function.apply(i));
            }
        };
    }

    public static <I, O> Future<O> lazyTransform(final Future<I> future, final Function<? super I, ? extends O> function) {
        Preconditions.checkNotNull(future);
        Preconditions.checkNotNull(function);
        return new Future<O>() {
            @Override
            public boolean cancel(boolean z) {
                return future.cancel(z);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public O get() throws ExecutionException, InterruptedException {
                return applyTransformation(future.get());
            }

            @Override
            public O get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
                return applyTransformation(future.get(j, timeUnit));
            }

            private O applyTransformation(I i) throws ExecutionException {
                try {
                    return (O) function.apply(i);
                } catch (Throwable th) {
                    throw new ExecutionException(th);
                }
            }
        };
    }

    private static class ChainingListenableFuture<I, O> extends AbstractFuture<O> implements Runnable {
        private AsyncFunction<? super I, ? extends O> function;
        private ListenableFuture<? extends I> inputFuture;
        private volatile ListenableFuture<? extends O> outputFuture;

        private ChainingListenableFuture(AsyncFunction<? super I, ? extends O> asyncFunction, ListenableFuture<? extends I> listenableFuture) {
            this.function = (AsyncFunction) Preconditions.checkNotNull(asyncFunction);
            this.inputFuture = (ListenableFuture) Preconditions.checkNotNull(listenableFuture);
        }

        @Override
        public boolean cancel(boolean z) {
            if (super.cancel(z)) {
                cancel(this.inputFuture, z);
                cancel(this.outputFuture, z);
                return true;
            }
            return false;
        }

        private void cancel(Future<?> future, boolean z) {
            if (future != null) {
                future.cancel(z);
            }
        }

        @Override
        public void run() {
            try {
            } catch (UndeclaredThrowableException e) {
                setException(e.getCause());
            } catch (Throwable th) {
                setException(th);
            }
            try {
                try {
                    final ListenableFuture<? extends O> listenableFuture = (ListenableFuture) Preconditions.checkNotNull(this.function.apply(Uninterruptibles.getUninterruptibly(this.inputFuture)), "AsyncFunction may not return null.");
                    this.outputFuture = listenableFuture;
                    if (!isCancelled()) {
                        listenableFuture.addListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    try {
                                        ChainingListenableFuture.this.set(Uninterruptibles.getUninterruptibly(listenableFuture));
                                    } catch (CancellationException e2) {
                                        ChainingListenableFuture.this.cancel(false);
                                        ChainingListenableFuture.this.outputFuture = null;
                                        return;
                                    } catch (ExecutionException e3) {
                                        ChainingListenableFuture.this.setException(e3.getCause());
                                    }
                                    ChainingListenableFuture.this.outputFuture = null;
                                } catch (Throwable th2) {
                                    ChainingListenableFuture.this.outputFuture = null;
                                    throw th2;
                                }
                            }
                        }, MoreExecutors.directExecutor());
                        this.function = null;
                        this.inputFuture = null;
                    } else {
                        listenableFuture.cancel(wasInterrupted());
                        this.outputFuture = null;
                        this.function = null;
                        this.inputFuture = null;
                    }
                } catch (Throwable th2) {
                    this.function = null;
                    this.inputFuture = null;
                    throw th2;
                }
            } catch (CancellationException e2) {
                cancel(false);
                this.function = null;
                this.inputFuture = null;
            } catch (ExecutionException e3) {
                setException(e3.getCause());
                this.function = null;
                this.inputFuture = null;
            }
        }
    }

    public static <V> ListenableFuture<V> dereference(ListenableFuture<? extends ListenableFuture<? extends V>> listenableFuture) {
        return transform(listenableFuture, DEREFERENCER);
    }

    public static <V> ListenableFuture<List<V>> allAsList(ListenableFuture<? extends V>... listenableFutureArr) {
        return listFuture(ImmutableList.copyOf(listenableFutureArr), true, MoreExecutors.directExecutor());
    }

    public static <V> ListenableFuture<List<V>> allAsList(Iterable<? extends ListenableFuture<? extends V>> iterable) {
        return listFuture(ImmutableList.copyOf(iterable), true, MoreExecutors.directExecutor());
    }

    private static final class WrappedCombiner<T> implements Callable<T> {
        final Callable<T> delegate;
        CombinerFuture<T> outputFuture;

        WrappedCombiner(Callable<T> callable) {
            this.delegate = (Callable) Preconditions.checkNotNull(callable);
        }

        @Override
        public T call() throws Exception {
            try {
                return this.delegate.call();
            } catch (CancellationException e) {
                this.outputFuture.cancel(false);
                return null;
            } catch (ExecutionException e2) {
                this.outputFuture.setException(e2.getCause());
                return null;
            }
        }
    }

    private static final class CombinerFuture<V> extends ListenableFutureTask<V> {
        ImmutableList<ListenableFuture<?>> futures;

        CombinerFuture(Callable<V> callable, ImmutableList<ListenableFuture<?>> immutableList) {
            super(callable);
            this.futures = immutableList;
        }

        @Override
        public boolean cancel(boolean z) {
            ImmutableList<ListenableFuture<?>> immutableList = this.futures;
            if (super.cancel(z)) {
                UnmodifiableIterator<ListenableFuture<?>> it = immutableList.iterator();
                while (it.hasNext()) {
                    it.next().cancel(z);
                }
                return true;
            }
            return false;
        }

        @Override
        protected void done() {
            super.done();
            this.futures = null;
        }

        @Override
        protected void setException(Throwable th) {
            super.setException(th);
        }
    }

    public static <V> ListenableFuture<V> nonCancellationPropagating(ListenableFuture<V> listenableFuture) {
        return new NonCancellationPropagatingFuture(listenableFuture);
    }

    private static class NonCancellationPropagatingFuture<V> extends AbstractFuture<V> {
        NonCancellationPropagatingFuture(final ListenableFuture<V> listenableFuture) {
            Preconditions.checkNotNull(listenableFuture);
            Futures.addCallback(listenableFuture, new FutureCallback<V>() {
                @Override
                public void onSuccess(V v) {
                    NonCancellationPropagatingFuture.this.set(v);
                }

                @Override
                public void onFailure(Throwable th) {
                    if (listenableFuture.isCancelled()) {
                        NonCancellationPropagatingFuture.this.cancel(false);
                    } else {
                        NonCancellationPropagatingFuture.this.setException(th);
                    }
                }
            }, MoreExecutors.directExecutor());
        }
    }

    public static <V> ListenableFuture<List<V>> successfulAsList(ListenableFuture<? extends V>... listenableFutureArr) {
        return listFuture(ImmutableList.copyOf(listenableFutureArr), false, MoreExecutors.directExecutor());
    }

    public static <V> ListenableFuture<List<V>> successfulAsList(Iterable<? extends ListenableFuture<? extends V>> iterable) {
        return listFuture(ImmutableList.copyOf(iterable), false, MoreExecutors.directExecutor());
    }

    public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(Iterable<? extends ListenableFuture<? extends T>> iterable) {
        final ConcurrentLinkedQueue concurrentLinkedQueueNewConcurrentLinkedQueue = Queues.newConcurrentLinkedQueue();
        ImmutableList.Builder builder = ImmutableList.builder();
        SerializingExecutor serializingExecutor = new SerializingExecutor(MoreExecutors.directExecutor());
        for (final ListenableFuture<? extends T> listenableFuture : iterable) {
            AsyncSettableFuture asyncSettableFutureCreate = AsyncSettableFuture.create();
            concurrentLinkedQueueNewConcurrentLinkedQueue.add(asyncSettableFutureCreate);
            listenableFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    ((AsyncSettableFuture) concurrentLinkedQueueNewConcurrentLinkedQueue.remove()).setFuture(listenableFuture);
                }
            }, serializingExecutor);
            builder.add(asyncSettableFutureCreate);
        }
        return builder.build();
    }

    public static <V> void addCallback(ListenableFuture<V> listenableFuture, FutureCallback<? super V> futureCallback) {
        addCallback(listenableFuture, futureCallback, MoreExecutors.directExecutor());
    }

    public static <V> void addCallback(final ListenableFuture<V> listenableFuture, final FutureCallback<? super V> futureCallback, Executor executor) {
        Preconditions.checkNotNull(futureCallback);
        listenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    futureCallback.onSuccess(Uninterruptibles.getUninterruptibly(listenableFuture));
                } catch (Error e) {
                    futureCallback.onFailure(e);
                } catch (RuntimeException e2) {
                    futureCallback.onFailure(e2);
                } catch (ExecutionException e3) {
                    futureCallback.onFailure(e3.getCause());
                }
            }
        }, executor);
    }

    public static <V, X extends Exception> V get(Future<V> future, Class<X> cls) throws Exception {
        Preconditions.checkNotNull(future);
        Preconditions.checkArgument(!RuntimeException.class.isAssignableFrom(cls), "Futures.get exception type (%s) must not be a RuntimeException", cls);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newWithCause(cls, e);
        } catch (ExecutionException e2) {
            wrapAndThrowExceptionOrError(e2.getCause(), cls);
            throw new AssertionError();
        }
    }

    public static <V, X extends Exception> V get(Future<V> future, long j, TimeUnit timeUnit, Class<X> cls) throws Exception {
        Preconditions.checkNotNull(future);
        Preconditions.checkNotNull(timeUnit);
        Preconditions.checkArgument(!RuntimeException.class.isAssignableFrom(cls), "Futures.get exception type (%s) must not be a RuntimeException", cls);
        try {
            return future.get(j, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newWithCause(cls, e);
        } catch (ExecutionException e2) {
            wrapAndThrowExceptionOrError(e2.getCause(), cls);
            throw new AssertionError();
        } catch (TimeoutException e3) {
            throw newWithCause(cls, e3);
        }
    }

    private static <X extends Exception> void wrapAndThrowExceptionOrError(Throwable th, Class<X> cls) throws Exception {
        if (th instanceof Error) {
            throw new ExecutionError((Error) th);
        }
        if (th instanceof RuntimeException) {
            throw new UncheckedExecutionException(th);
        }
        throw newWithCause(cls, th);
    }

    public static <V> V getUnchecked(Future<V> future) {
        Preconditions.checkNotNull(future);
        try {
            return (V) Uninterruptibles.getUninterruptibly(future);
        } catch (ExecutionException e) {
            wrapAndThrowUnchecked(e.getCause());
            throw new AssertionError();
        }
    }

    private static void wrapAndThrowUnchecked(Throwable th) {
        if (th instanceof Error) {
            throw new ExecutionError((Error) th);
        }
        throw new UncheckedExecutionException(th);
    }

    private static <X extends Exception> X newWithCause(Class<X> cls, Throwable th) {
        Iterator it = preferringStrings(Arrays.asList(cls.getConstructors())).iterator();
        while (it.hasNext()) {
            X x = (X) newFromConstructor((Constructor) it.next(), th);
            if (x != null) {
                if (x.getCause() == null) {
                    x.initCause(th);
                }
                return x;
            }
        }
        throw new IllegalArgumentException("No appropriate constructor for exception of type " + cls + " in response to chained exception", th);
    }

    private static <X extends Exception> List<Constructor<X>> preferringStrings(List<Constructor<X>> list) {
        return (List<Constructor<X>>) WITH_STRING_PARAM_FIRST.sortedCopy(list);
    }

    private static <X> X newFromConstructor(Constructor<X> constructor, Throwable th) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] objArr = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> cls = parameterTypes[i];
            if (cls.equals(String.class)) {
                objArr[i] = th.toString();
            } else {
                if (!cls.equals(Throwable.class)) {
                    return null;
                }
                objArr[i] = th;
            }
        }
        try {
            return constructor.newInstance(objArr);
        } catch (IllegalAccessException e) {
            return null;
        } catch (IllegalArgumentException e2) {
            return null;
        } catch (InstantiationException e3) {
            return null;
        } catch (InvocationTargetException e4) {
            return null;
        }
    }

    private static class CombinedFuture<V, C> extends AbstractFuture<C> {
        private static final Logger logger = Logger.getLogger(CombinedFuture.class.getName());
        final boolean allMustSucceed;
        FutureCombiner<V, C> combiner;
        ImmutableCollection<? extends ListenableFuture<? extends V>> futures;
        final AtomicInteger remaining;
        Set<Throwable> seenExceptions;
        final Object seenExceptionsLock = new Object();
        List<Optional<V>> values;

        CombinedFuture(ImmutableCollection<? extends ListenableFuture<? extends V>> immutableCollection, boolean z, Executor executor, FutureCombiner<V, C> futureCombiner) {
            this.futures = immutableCollection;
            this.allMustSucceed = z;
            this.remaining = new AtomicInteger(immutableCollection.size());
            this.combiner = futureCombiner;
            this.values = Lists.newArrayListWithCapacity(immutableCollection.size());
            init(executor);
        }

        protected void init(Executor executor) {
            addListener(new Runnable() {
                @Override
                public void run() {
                    if (CombinedFuture.this.isCancelled()) {
                        UnmodifiableIterator<? extends ListenableFuture<? extends V>> it = CombinedFuture.this.futures.iterator();
                        while (it.hasNext()) {
                            it.next().cancel(CombinedFuture.this.wasInterrupted());
                        }
                    }
                    CombinedFuture.this.futures = null;
                    CombinedFuture.this.values = null;
                    CombinedFuture.this.combiner = null;
                }
            }, MoreExecutors.directExecutor());
            if (this.futures.isEmpty()) {
                set(this.combiner.combine(ImmutableList.of()));
                return;
            }
            final int i = 0;
            for (int i2 = 0; i2 < this.futures.size(); i2++) {
                this.values.add(null);
            }
            UnmodifiableIterator<? extends ListenableFuture<? extends V>> it = this.futures.iterator();
            while (it.hasNext()) {
                final ListenableFuture<? extends V> next = it.next();
                next.addListener(new Runnable() {
                    @Override
                    public void run() {
                        CombinedFuture.this.setOneValue(i, next);
                    }
                }, executor);
                i++;
            }
        }

        private void setExceptionAndMaybeLog(Throwable th) {
            boolean exception;
            boolean zAdd;
            if (this.allMustSucceed) {
                exception = super.setException(th);
                synchronized (this.seenExceptionsLock) {
                    if (this.seenExceptions == null) {
                        this.seenExceptions = Sets.newHashSet();
                    }
                    zAdd = this.seenExceptions.add(th);
                }
            } else {
                exception = false;
                zAdd = true;
            }
            if ((th instanceof Error) || (this.allMustSucceed && !exception && zAdd)) {
                logger.log(Level.SEVERE, "input future failed.", th);
            }
        }

        private void setOneValue(int i, Future<? extends V> future) {
            FutureCombiner<V, C> futureCombiner;
            int iDecrementAndGet;
            List<Optional<V>> list = this.values;
            if (isDone() || list == null) {
                Preconditions.checkState(this.allMustSucceed || isCancelled(), "Future was done before all dependencies completed");
            }
            try {
                try {
                    Preconditions.checkState(future.isDone(), "Tried to set value from future which is not done");
                    Object uninterruptibly = Uninterruptibles.getUninterruptibly(future);
                    if (list != null) {
                        list.set(i, Optional.fromNullable(uninterruptibly));
                    }
                    iDecrementAndGet = this.remaining.decrementAndGet();
                    Preconditions.checkState(iDecrementAndGet >= 0, "Less than 0 remaining futures");
                } catch (CancellationException e) {
                    if (this.allMustSucceed) {
                        cancel(false);
                    }
                    int iDecrementAndGet2 = this.remaining.decrementAndGet();
                    Preconditions.checkState(iDecrementAndGet2 >= 0, "Less than 0 remaining futures");
                    if (iDecrementAndGet2 == 0) {
                        futureCombiner = this.combiner;
                        if (futureCombiner != null) {
                        }
                        Preconditions.checkState(isDone());
                    }
                    return;
                } catch (ExecutionException e2) {
                    setExceptionAndMaybeLog(e2.getCause());
                    int iDecrementAndGet3 = this.remaining.decrementAndGet();
                    Preconditions.checkState(iDecrementAndGet3 >= 0, "Less than 0 remaining futures");
                    if (iDecrementAndGet3 == 0) {
                        futureCombiner = this.combiner;
                        if (futureCombiner != null) {
                        }
                        Preconditions.checkState(isDone());
                    }
                    return;
                } catch (Throwable th) {
                    setExceptionAndMaybeLog(th);
                    int iDecrementAndGet4 = this.remaining.decrementAndGet();
                    Preconditions.checkState(iDecrementAndGet4 >= 0, "Less than 0 remaining futures");
                    if (iDecrementAndGet4 == 0) {
                        futureCombiner = this.combiner;
                        if (futureCombiner != null) {
                        }
                        Preconditions.checkState(isDone());
                    }
                    return;
                }
                if (iDecrementAndGet == 0) {
                    futureCombiner = this.combiner;
                    if (futureCombiner != null) {
                    }
                    Preconditions.checkState(isDone());
                }
            } catch (Throwable th2) {
                int iDecrementAndGet5 = this.remaining.decrementAndGet();
                Preconditions.checkState(iDecrementAndGet5 >= 0, "Less than 0 remaining futures");
                if (iDecrementAndGet5 == 0) {
                    FutureCombiner<V, C> futureCombiner2 = this.combiner;
                    if (futureCombiner2 == null || list == null) {
                        Preconditions.checkState(isDone());
                    } else {
                        set(futureCombiner2.combine(list));
                    }
                }
                throw th2;
            }
        }
    }

    private static <V> ListenableFuture<List<V>> listFuture(ImmutableList<ListenableFuture<? extends V>> immutableList, boolean z, Executor executor) {
        return new CombinedFuture(immutableList, z, executor, new FutureCombiner<V, List<V>>() {
            @Override
            public List<V> combine(List<Optional<V>> list) {
                ArrayList arrayListNewArrayList = Lists.newArrayList();
                Iterator<Optional<V>> it = list.iterator();
                while (it.hasNext()) {
                    Optional<V> next = it.next();
                    arrayListNewArrayList.add(next != null ? next.orNull() : null);
                }
                return Collections.unmodifiableList(arrayListNewArrayList);
            }
        });
    }

    private static class MappingCheckedFuture<V, X extends Exception> extends AbstractCheckedFuture<V, X> {
        final Function<? super Exception, X> mapper;

        MappingCheckedFuture(ListenableFuture<V> listenableFuture, Function<? super Exception, X> function) {
            super(listenableFuture);
            this.mapper = (Function) Preconditions.checkNotNull(function);
        }

        @Override
        protected X mapException(Exception exc) {
            return this.mapper.apply(exc);
        }
    }
}
