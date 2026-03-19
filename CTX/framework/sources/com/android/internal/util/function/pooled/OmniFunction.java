package com.android.internal.util.function.pooled;

import com.android.internal.util.FunctionalUtils;
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
import java.util.function.Function;

abstract class OmniFunction<A, B, C, D, E, F, R> implements PooledFunction<A, R>, BiFunction<A, B, R>, TriFunction<A, B, C, R>, QuadFunction<A, B, C, D, R>, QuintFunction<A, B, C, D, E, R>, HexFunction<A, B, C, D, E, F, R>, PooledConsumer<A>, BiConsumer<A, B>, TriConsumer<A, B, C>, QuadConsumer<A, B, C, D>, QuintConsumer<A, B, C, D, E>, HexConsumer<A, B, C, D, E, F>, PooledPredicate<A>, BiPredicate<A, B>, PooledSupplier<R>, PooledRunnable, FunctionalUtils.ThrowingRunnable, FunctionalUtils.ThrowingSupplier<R>, PooledSupplier.OfInt, PooledSupplier.OfLong, PooledSupplier.OfDouble {
    @Override
    public abstract <V> OmniFunction<A, B, C, D, E, F, V> andThen(Function<? super R, ? extends V> function);

    abstract R invoke(A a, B b, C c, D d, E e, F f);

    @Override
    public abstract OmniFunction<A, B, C, D, E, F, R> negate();

    @Override
    public abstract OmniFunction<A, B, C, D, E, F, R> recycleOnUse();

    OmniFunction() {
    }

    @Override
    public R apply(A a, B b) {
        return invoke(a, b, null, null, null, null);
    }

    @Override
    public R apply(A a) {
        return invoke(a, null, null, null, null, null);
    }

    @Override
    public void accept(A a, B b) {
        invoke(a, b, null, null, null, null);
    }

    @Override
    public void accept(A a) {
        invoke(a, null, null, null, null, null);
    }

    @Override
    public void run() {
        invoke(null, null, null, null, null, null);
    }

    @Override
    public R get() {
        return invoke(null, null, null, null, null, null);
    }

    @Override
    public boolean test(A a, B b) {
        return ((Boolean) invoke(a, b, null, null, null, null)).booleanValue();
    }

    @Override
    public boolean test(A a) {
        return ((Boolean) invoke(a, null, null, null, null, null)).booleanValue();
    }

    @Override
    public PooledRunnable asRunnable() {
        return this;
    }

    @Override
    public PooledConsumer<A> asConsumer() {
        return this;
    }

    @Override
    public R apply(A a, B b, C c) {
        return invoke(a, b, c, null, null, null);
    }

    @Override
    public void accept(A a, B b, C c) {
        invoke(a, b, c, null, null, null);
    }

    @Override
    public R apply(A a, B b, C c, D d) {
        return invoke(a, b, c, d, null, null);
    }

    @Override
    public R apply(A a, B b, C c, D d, E e) {
        return invoke(a, b, c, d, e, null);
    }

    @Override
    public R apply(A a, B b, C c, D d, E e, F f) {
        return invoke(a, b, c, d, e, f);
    }

    @Override
    public void accept(A a, B b, C c, D d) {
        invoke(a, b, c, d, null, null);
    }

    @Override
    public void accept(A a, B b, C c, D d, E e) {
        invoke(a, b, c, d, e, null);
    }

    @Override
    public void accept(A a, B b, C c, D d, E e, F f) {
        invoke(a, b, c, d, e, f);
    }

    @Override
    public void runOrThrow() throws Exception {
        run();
    }

    @Override
    public R getOrThrow() throws Exception {
        return get();
    }
}
