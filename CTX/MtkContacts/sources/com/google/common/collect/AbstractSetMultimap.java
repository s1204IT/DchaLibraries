package com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

abstract class AbstractSetMultimap<K, V> extends AbstractMapBasedMultimap<K, V> implements SetMultimap<K, V> {
    private static final long serialVersionUID = 7431625294878419160L;

    @Override
    abstract Set<V> createCollection();

    protected AbstractSetMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    Set<V> createUnmodifiableEmptyCollection() {
        return ImmutableSet.of();
    }

    @Override
    public Set<V> get(K k) {
        return (Set) super.get((Object) k);
    }

    @Override
    public Set<Map.Entry<K, V>> entries() {
        return (Set) super.entries();
    }

    @Override
    public Set<V> removeAll(Object obj) {
        return (Set) super.removeAll(obj);
    }

    @Override
    public Set<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return (Set) super.replaceValues((Object) k, (Iterable) iterable);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override
    public boolean put(K k, V v) {
        return super.put(k, v);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
