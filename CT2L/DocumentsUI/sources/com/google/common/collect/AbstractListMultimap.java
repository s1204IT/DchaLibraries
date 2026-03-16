package com.google.common.collect;

import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract class AbstractListMultimap<K, V> extends AbstractMultimap<K, V> implements ListMultimap<K, V> {
    private static final long serialVersionUID = 6588350623831699109L;

    @Override
    abstract List<V> createCollection();

    protected AbstractListMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    public List<V> get(K key) {
        return (List) super.get((Object) key);
    }

    @Override
    public boolean put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }
}
