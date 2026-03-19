package com.android.internal.util.function.pooled;

import android.os.Message;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.HexFunction;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuadFunction;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.QuintFunction;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.TriFunction;
import com.android.internal.util.function.pooled.PooledSupplier;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PooledLambda {
    void recycle();

    PooledLambda recycleOnUse();

    static <R> ArgumentPlaceholder<R> __() {
        return (ArgumentPlaceholder<R>) ArgumentPlaceholder.INSTANCE;
    }

    static <R> ArgumentPlaceholder<R> __(Class<R> cls) {
        return __();
    }

    static <R> PooledSupplier<R> obtainSupplier(R r) {
        PooledLambdaImpl pooledLambdaImplAcquireConstSupplier = PooledLambdaImpl.acquireConstSupplier(3);
        pooledLambdaImplAcquireConstSupplier.mFunc = r;
        return pooledLambdaImplAcquireConstSupplier;
    }

    static PooledSupplier.OfInt obtainSupplier(int i) {
        PooledLambdaImpl pooledLambdaImplAcquireConstSupplier = PooledLambdaImpl.acquireConstSupplier(4);
        pooledLambdaImplAcquireConstSupplier.mConstValue = i;
        return pooledLambdaImplAcquireConstSupplier;
    }

    static PooledSupplier.OfLong obtainSupplier(long j) {
        PooledLambdaImpl pooledLambdaImplAcquireConstSupplier = PooledLambdaImpl.acquireConstSupplier(5);
        pooledLambdaImplAcquireConstSupplier.mConstValue = j;
        return pooledLambdaImplAcquireConstSupplier;
    }

    static PooledSupplier.OfDouble obtainSupplier(double d) {
        PooledLambdaImpl pooledLambdaImplAcquireConstSupplier = PooledLambdaImpl.acquireConstSupplier(6);
        pooledLambdaImplAcquireConstSupplier.mConstValue = Double.doubleToRawLongBits(d);
        return pooledLambdaImplAcquireConstSupplier;
    }

    static <A> PooledRunnable obtainRunnable(Consumer<? super A> consumer, A a) {
        return (PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, consumer, 1, 0, 1, a, null, null, null, null, null);
    }

    static <A> PooledSupplier<Boolean> obtainSupplier(Predicate<? super A> predicate, A a) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, predicate, 1, 0, 2, a, null, null, null, null, null);
    }

    static <A, R> PooledSupplier<R> obtainSupplier(Function<? super A, ? extends R> function, A a) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, function, 1, 0, 3, a, null, null, null, null, null);
    }

    static <A> Message obtainMessage(Consumer<? super A> consumer, A a) {
        Message callback;
        synchronized (Message.sPoolSync) {
            callback = Message.obtain().setCallback(((PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sMessageCallbacksPool, consumer, 1, 0, 1, a, null, null, null, null, null)).recycleOnUse());
        }
        return callback;
    }

    static <A, B> PooledRunnable obtainRunnable(BiConsumer<? super A, ? super B> biConsumer, A a, B b) {
        return (PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biConsumer, 2, 0, 1, a, b, null, null, null, null);
    }

    static <A, B> PooledSupplier<Boolean> obtainSupplier(BiPredicate<? super A, ? super B> biPredicate, A a, B b) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biPredicate, 2, 0, 2, a, b, null, null, null, null);
    }

    static <A, B, R> PooledSupplier<R> obtainSupplier(BiFunction<? super A, ? super B, ? extends R> biFunction, A a, B b) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biFunction, 2, 0, 3, a, b, null, null, null, null);
    }

    static <A, B> PooledConsumer<A> obtainConsumer(BiConsumer<? super A, ? super B> biConsumer, ArgumentPlaceholder<A> argumentPlaceholder, B b) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biConsumer, 2, 1, 1, argumentPlaceholder, b, null, null, null, null);
    }

    static <A, B> PooledPredicate<A> obtainPredicate(BiPredicate<? super A, ? super B> biPredicate, ArgumentPlaceholder<A> argumentPlaceholder, B b) {
        return (PooledPredicate) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biPredicate, 2, 1, 2, argumentPlaceholder, b, null, null, null, null);
    }

    static <A, B, R> PooledFunction<A, R> obtainFunction(BiFunction<? super A, ? super B, ? extends R> biFunction, ArgumentPlaceholder<A> argumentPlaceholder, B b) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biFunction, 2, 1, 3, argumentPlaceholder, b, null, null, null, null);
    }

    static <A, B> PooledConsumer<B> obtainConsumer(BiConsumer<? super A, ? super B> biConsumer, A a, ArgumentPlaceholder<B> argumentPlaceholder) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biConsumer, 2, 1, 1, a, argumentPlaceholder, null, null, null, null);
    }

    static <A, B> PooledPredicate<B> obtainPredicate(BiPredicate<? super A, ? super B> biPredicate, A a, ArgumentPlaceholder<B> argumentPlaceholder) {
        return (PooledPredicate) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biPredicate, 2, 1, 2, a, argumentPlaceholder, null, null, null, null);
    }

    static <A, B, R> PooledFunction<B, R> obtainFunction(BiFunction<? super A, ? super B, ? extends R> biFunction, A a, ArgumentPlaceholder<B> argumentPlaceholder) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, biFunction, 2, 1, 3, a, argumentPlaceholder, null, null, null, null);
    }

    static <A, B> Message obtainMessage(BiConsumer<? super A, ? super B> biConsumer, A a, B b) {
        Message callback;
        synchronized (Message.sPoolSync) {
            callback = Message.obtain().setCallback(((PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sMessageCallbacksPool, biConsumer, 2, 0, 1, a, b, null, null, null, null)).recycleOnUse());
        }
        return callback;
    }

    static <A, B, C> PooledRunnable obtainRunnable(TriConsumer<? super A, ? super B, ? super C> triConsumer, A a, B b, C c) {
        return (PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triConsumer, 3, 0, 1, a, b, c, null, null, null);
    }

    static <A, B, C, R> PooledSupplier<R> obtainSupplier(TriFunction<? super A, ? super B, ? super C, ? extends R> triFunction, A a, B b, C c) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triFunction, 3, 0, 3, a, b, c, null, null, null);
    }

    static <A, B, C> PooledConsumer<A> obtainConsumer(TriConsumer<? super A, ? super B, ? super C> triConsumer, ArgumentPlaceholder<A> argumentPlaceholder, B b, C c) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triConsumer, 3, 1, 1, argumentPlaceholder, b, c, null, null, null);
    }

    static <A, B, C, R> PooledFunction<A, R> obtainFunction(TriFunction<? super A, ? super B, ? super C, ? extends R> triFunction, ArgumentPlaceholder<A> argumentPlaceholder, B b, C c) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triFunction, 3, 1, 3, argumentPlaceholder, b, c, null, null, null);
    }

    static <A, B, C> PooledConsumer<B> obtainConsumer(TriConsumer<? super A, ? super B, ? super C> triConsumer, A a, ArgumentPlaceholder<B> argumentPlaceholder, C c) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triConsumer, 3, 1, 1, a, argumentPlaceholder, c, null, null, null);
    }

    static <A, B, C, R> PooledFunction<B, R> obtainFunction(TriFunction<? super A, ? super B, ? super C, ? extends R> triFunction, A a, ArgumentPlaceholder<B> argumentPlaceholder, C c) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triFunction, 3, 1, 3, a, argumentPlaceholder, c, null, null, null);
    }

    static <A, B, C> PooledConsumer<C> obtainConsumer(TriConsumer<? super A, ? super B, ? super C> triConsumer, A a, B b, ArgumentPlaceholder<C> argumentPlaceholder) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triConsumer, 3, 1, 1, a, b, argumentPlaceholder, null, null, null);
    }

    static <A, B, C, R> PooledFunction<C, R> obtainFunction(TriFunction<? super A, ? super B, ? super C, ? extends R> triFunction, A a, B b, ArgumentPlaceholder<C> argumentPlaceholder) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, triFunction, 3, 1, 3, a, b, argumentPlaceholder, null, null, null);
    }

    static <A, B, C> Message obtainMessage(TriConsumer<? super A, ? super B, ? super C> triConsumer, A a, B b, C c) {
        Message callback;
        synchronized (Message.sPoolSync) {
            callback = Message.obtain().setCallback(((PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sMessageCallbacksPool, triConsumer, 3, 0, 1, a, b, c, null, null, null)).recycleOnUse());
        }
        return callback;
    }

    static <A, B, C, D> PooledRunnable obtainRunnable(QuadConsumer<? super A, ? super B, ? super C, ? super D> quadConsumer, A a, B b, C c, D d) {
        return (PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadConsumer, 4, 0, 1, a, b, c, d, null, null);
    }

    static <A, B, C, D, R> PooledSupplier<R> obtainSupplier(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends R> quadFunction, A a, B b, C c, D d) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadFunction, 4, 0, 3, a, b, c, d, null, null);
    }

    static <A, B, C, D> PooledConsumer<A> obtainConsumer(QuadConsumer<? super A, ? super B, ? super C, ? super D> quadConsumer, ArgumentPlaceholder<A> argumentPlaceholder, B b, C c, D d) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadConsumer, 4, 1, 1, argumentPlaceholder, b, c, d, null, null);
    }

    static <A, B, C, D, R> PooledFunction<A, R> obtainFunction(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends R> quadFunction, ArgumentPlaceholder<A> argumentPlaceholder, B b, C c, D d) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadFunction, 4, 1, 3, argumentPlaceholder, b, c, d, null, null);
    }

    static <A, B, C, D> PooledConsumer<B> obtainConsumer(QuadConsumer<? super A, ? super B, ? super C, ? super D> quadConsumer, A a, ArgumentPlaceholder<B> argumentPlaceholder, C c, D d) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadConsumer, 4, 1, 1, a, argumentPlaceholder, c, d, null, null);
    }

    static <A, B, C, D, R> PooledFunction<B, R> obtainFunction(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends R> quadFunction, A a, ArgumentPlaceholder<B> argumentPlaceholder, C c, D d) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadFunction, 4, 1, 3, a, argumentPlaceholder, c, d, null, null);
    }

    static <A, B, C, D> PooledConsumer<C> obtainConsumer(QuadConsumer<? super A, ? super B, ? super C, ? super D> quadConsumer, A a, B b, ArgumentPlaceholder<C> argumentPlaceholder, D d) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadConsumer, 4, 1, 1, a, b, argumentPlaceholder, d, null, null);
    }

    static <A, B, C, D, R> PooledFunction<C, R> obtainFunction(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends R> quadFunction, A a, B b, ArgumentPlaceholder<C> argumentPlaceholder, D d) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadFunction, 4, 1, 3, a, b, argumentPlaceholder, d, null, null);
    }

    static <A, B, C, D> PooledConsumer<D> obtainConsumer(QuadConsumer<? super A, ? super B, ? super C, ? super D> quadConsumer, A a, B b, C c, ArgumentPlaceholder<D> argumentPlaceholder) {
        return (PooledConsumer) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadConsumer, 4, 1, 1, a, b, c, argumentPlaceholder, null, null);
    }

    static <A, B, C, D, R> PooledFunction<D, R> obtainFunction(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends R> quadFunction, A a, B b, C c, ArgumentPlaceholder<D> argumentPlaceholder) {
        return (PooledFunction) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quadFunction, 4, 1, 3, a, b, c, argumentPlaceholder, null, null);
    }

    static <A, B, C, D> Message obtainMessage(QuadConsumer<? super A, ? super B, ? super C, ? super D> quadConsumer, A a, B b, C c, D d) {
        Message callback;
        synchronized (Message.sPoolSync) {
            callback = Message.obtain().setCallback(((PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sMessageCallbacksPool, quadConsumer, 4, 0, 1, a, b, c, d, null, null)).recycleOnUse());
        }
        return callback;
    }

    static <A, B, C, D, E> PooledRunnable obtainRunnable(QuintConsumer<? super A, ? super B, ? super C, ? super D, ? super E> quintConsumer, A a, B b, C c, D d, E e) {
        return (PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quintConsumer, 5, 0, 1, a, b, c, d, e, null);
    }

    static <A, B, C, D, E, R> PooledSupplier<R> obtainSupplier(QuintFunction<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> quintFunction, A a, B b, C c, D d, E e) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, quintFunction, 5, 0, 3, a, b, c, d, e, null);
    }

    static <A, B, C, D, E> Message obtainMessage(QuintConsumer<? super A, ? super B, ? super C, ? super D, ? super E> quintConsumer, A a, B b, C c, D d, E e) {
        Message callback;
        synchronized (Message.sPoolSync) {
            callback = Message.obtain().setCallback(((PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sMessageCallbacksPool, quintConsumer, 5, 0, 1, a, b, c, d, e, null)).recycleOnUse());
        }
        return callback;
    }

    static <A, B, C, D, E, F> PooledRunnable obtainRunnable(HexConsumer<? super A, ? super B, ? super C, ? super D, ? super E, ? super F> hexConsumer, A a, B b, C c, D d, E e, F f) {
        return (PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, hexConsumer, 6, 0, 1, a, b, c, d, e, f);
    }

    static <A, B, C, D, E, F, R> PooledSupplier<R> obtainSupplier(HexFunction<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? extends R> hexFunction, A a, B b, C c, D d, E e, F f) {
        return (PooledSupplier) PooledLambdaImpl.acquire(PooledLambdaImpl.sPool, hexFunction, 6, 0, 3, a, b, c, d, e, f);
    }

    static <A, B, C, D, E, F> Message obtainMessage(HexConsumer<? super A, ? super B, ? super C, ? super D, ? super E, ? super F> hexConsumer, A a, B b, C c, D d, E e, F f) {
        Message callback;
        synchronized (Message.sPoolSync) {
            callback = Message.obtain().setCallback(((PooledRunnable) PooledLambdaImpl.acquire(PooledLambdaImpl.sMessageCallbacksPool, hexConsumer, 6, 0, 1, a, b, c, d, e, f)).recycleOnUse());
        }
        return callback;
    }
}
