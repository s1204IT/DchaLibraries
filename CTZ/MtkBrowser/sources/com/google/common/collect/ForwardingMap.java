package com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ForwardingMap<K, V> extends ForwardingObject implements Map<K, V> {
    protected ForwardingMap() {
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public boolean containsKey(Object obj) {
        return delegate().containsKey(obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        return delegate().containsValue(obj);
    }

    @Override
    public abstract Map<K, V> delegate();

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return delegate().entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || delegate().equals(obj);
    }

    @Override
    public V get(Object obj) {
        return delegate().get(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return delegate().keySet();
    }

    @Override
    public V put(K k, V v) {
        return delegate().put(k, v);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        delegate().putAll(map);
    }

    @Override
    public V remove(Object obj) {
        return delegate().remove(obj);
    }

    @Override
    public int size() {
        return delegate().size();
    }

    protected String standardToString() {
        return Maps.toStringImpl(this);
    }

    @Override
    public Collection<V> values() {
        return delegate().values();
    }
}
