package com.google.common.util.concurrent;

public final class SettableFuture<V> extends AbstractFuture<V> {
    public static <V> SettableFuture<V> create() {
        return new SettableFuture<>();
    }

    private SettableFuture() {
    }

    @Override
    public boolean set(V v) {
        return super.set(v);
    }

    @Override
    public boolean setException(Throwable th) {
        return super.setException(th);
    }
}
