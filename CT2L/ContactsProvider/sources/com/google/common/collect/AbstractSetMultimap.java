package com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

abstract class AbstractSetMultimap<K, V> extends AbstractMultimap<K, V> implements SetMultimap<K, V> {
    private static final long serialVersionUID = 7431625294878419160L;

    @Override
    abstract Set<V> createCollection();

    protected AbstractSetMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    public Set<V> get(K key) {
        return (Set) super.get((Object) key);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override
    public boolean put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }
}
