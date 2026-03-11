package com.google.common.collect;

import com.google.common.base.Objects;
import java.util.Map;

abstract class AbstractMapEntry<K, V> implements Map.Entry<K, V> {
    AbstractMapEntry() {
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map.Entry)) {
            return false;
        }
        Map.Entry entry = (Map.Entry) obj;
        return Objects.equal(getKey(), entry.getKey()) && Objects.equal(getValue(), entry.getValue());
    }

    @Override
    public abstract K getKey();

    @Override
    public abstract V getValue();

    @Override
    public int hashCode() {
        K key = getKey();
        V value = getValue();
        return (value != null ? value.hashCode() : 0) ^ (key == null ? 0 : key.hashCode());
    }

    @Override
    public V setValue(V v) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return getKey() + "=" + getValue();
    }
}
