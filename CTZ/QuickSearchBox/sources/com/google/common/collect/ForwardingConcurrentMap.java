package com.google.common.collect;

import java.util.concurrent.ConcurrentMap;

public abstract class ForwardingConcurrentMap<K, V> extends ForwardingMap<K, V> implements ConcurrentMap<K, V> {
    @Override
    protected abstract ConcurrentMap<K, V> delegate();

    protected ForwardingConcurrentMap() {
    }

    @Override
    public V putIfAbsent(K k, V v) {
        return delegate().putIfAbsent(k, v);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        return delegate().remove(obj, obj2);
    }

    @Override
    public V replace(K k, V v) {
        return delegate().replace(k, v);
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        return delegate().replace(k, v, v2);
    }
}
