package com.google.common.collect;

import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract class AbstractListMultimap<K, V> extends AbstractMapBasedMultimap<K, V> implements ListMultimap<K, V> {
    private static final long serialVersionUID = 6588350623831699109L;

    @Override
    abstract List<V> createCollection();

    protected AbstractListMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    List<V> createUnmodifiableEmptyCollection() {
        return ImmutableList.of();
    }

    @Override
    public List<V> get(K k) {
        return (List) super.get((Object) k);
    }

    @Override
    public List<V> removeAll(Object obj) {
        return (List) super.removeAll(obj);
    }

    @Override
    public List<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return (List) super.replaceValues((Object) k, (Iterable) iterable);
    }

    @Override
    public boolean put(K k, V v) {
        return super.put(k, v);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
