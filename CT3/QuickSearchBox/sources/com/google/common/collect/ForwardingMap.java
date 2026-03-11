package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingMap<K, V> extends ForwardingObject implements Map<K, V> {
    @Override
    public abstract Map<K, V> delegate();

    protected ForwardingMap() {
    }

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public V remove(Object object) {
        return delegate().remove(object);
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return delegate().containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return delegate().containsValue(value);
    }

    @Override
    public V get(@Nullable Object key) {
        return delegate().get(key);
    }

    @Override
    public V put(K key, V value) {
        return delegate().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        delegate().putAll(map);
    }

    @Override
    public Set<K> keySet() {
        return delegate().keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate().values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return delegate().entrySet();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object != this) {
            return delegate().equals(object);
        }
        return true;
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    protected String standardToString() {
        return Maps.toStringImpl(this);
    }
}
