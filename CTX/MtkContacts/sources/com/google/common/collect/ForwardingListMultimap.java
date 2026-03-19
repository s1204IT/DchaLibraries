package com.google.common.collect;

import java.util.List;

public abstract class ForwardingListMultimap<K, V> extends ForwardingMultimap<K, V> implements ListMultimap<K, V> {
    @Override
    protected abstract ListMultimap<K, V> delegate();

    protected ForwardingListMultimap() {
    }

    @Override
    public List<V> get(K k) {
        return delegate().get((Object) k);
    }

    @Override
    public List<V> removeAll(Object obj) {
        return delegate().removeAll(obj);
    }

    @Override
    public List<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return delegate().replaceValues((Object) k, (Iterable) iterable);
    }
}
