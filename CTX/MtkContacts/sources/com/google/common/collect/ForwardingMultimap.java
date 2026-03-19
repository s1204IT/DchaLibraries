package com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ForwardingMultimap<K, V> extends ForwardingObject implements Multimap<K, V> {
    @Override
    protected abstract Multimap<K, V> delegate();

    protected ForwardingMultimap() {
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return delegate().asMap();
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public boolean containsEntry(Object obj, Object obj2) {
        return delegate().containsEntry(obj, obj2);
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
    public Collection<Map.Entry<K, V>> entries() {
        return delegate().entries();
    }

    public Collection<V> get(K k) {
        return delegate().get(k);
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public Multiset<K> keys() {
        return delegate().keys();
    }

    @Override
    public Set<K> keySet() {
        return delegate().keySet();
    }

    @Override
    public boolean put(K k, V v) {
        return delegate().put(k, v);
    }

    @Override
    public boolean putAll(K k, Iterable<? extends V> iterable) {
        return delegate().putAll(k, iterable);
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
        return delegate().putAll(multimap);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        return delegate().remove(obj, obj2);
    }

    public Collection<V> removeAll(Object obj) {
        return delegate().removeAll(obj);
    }

    public Collection<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return delegate().replaceValues(k, iterable);
    }

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public Collection<V> values() {
        return delegate().values();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }
}
