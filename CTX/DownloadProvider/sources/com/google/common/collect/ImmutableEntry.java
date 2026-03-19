package com.google.common.collect;

import java.io.Serializable;

class ImmutableEntry<K, V> extends AbstractMapEntry<K, V> implements Serializable {
    private static final long serialVersionUID = 0;
    final K key;
    final V value;

    ImmutableEntry(K k, V v) {
        this.key = k;
        this.value = v;
    }

    @Override
    public final K getKey() {
        return this.key;
    }

    @Override
    public final V getValue() {
        return this.value;
    }

    @Override
    public final V setValue(V v) {
        throw new UnsupportedOperationException();
    }
}
