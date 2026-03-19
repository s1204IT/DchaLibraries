package com.google.common.collect;

import java.util.Map;
import java.util.Set;

public abstract class ForwardingSetMultimap<K, V> extends ForwardingMultimap<K, V> implements SetMultimap<K, V> {
    @Override
    protected abstract SetMultimap<K, V> delegate();

    @Override
    public Set<Map.Entry<K, V>> entries() {
        return delegate().entries();
    }

    @Override
    public Set<V> get(K k) {
        return delegate().get((Object) k);
    }

    @Override
    public Set<V> removeAll(Object obj) {
        return delegate().removeAll(obj);
    }

    @Override
    public Set<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return delegate().replaceValues((Object) k, (Iterable) iterable);
    }
}
