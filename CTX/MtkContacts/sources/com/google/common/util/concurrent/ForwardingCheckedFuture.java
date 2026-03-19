package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.lang.Exception;
import java.util.concurrent.TimeUnit;

public abstract class ForwardingCheckedFuture<V, X extends Exception> extends ForwardingListenableFuture<V> implements CheckedFuture<V, X> {
    @Override
    protected abstract CheckedFuture<V, X> delegate();

    @Override
    public V checkedGet() throws Exception {
        return delegate().checkedGet();
    }

    @Override
    public V checkedGet(long j, TimeUnit timeUnit) throws Exception {
        return delegate().checkedGet(j, timeUnit);
    }

    public static abstract class SimpleForwardingCheckedFuture<V, X extends Exception> extends ForwardingCheckedFuture<V, X> {
        private final CheckedFuture<V, X> delegate;

        protected SimpleForwardingCheckedFuture(CheckedFuture<V, X> checkedFuture) {
            this.delegate = (CheckedFuture) Preconditions.checkNotNull(checkedFuture);
        }

        @Override
        protected final CheckedFuture<V, X> delegate() {
            return this.delegate;
        }
    }
}
