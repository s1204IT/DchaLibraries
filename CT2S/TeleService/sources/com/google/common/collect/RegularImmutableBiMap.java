package com.google.common.collect;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
    final transient ImmutableMap<K, V> delegate;
    final transient ImmutableBiMap<V, K> inverse;

    RegularImmutableBiMap(ImmutableMap<K, V> delegate) {
        this.delegate = delegate;
        ImmutableMap.Builder builder = ImmutableMap.builder();
        for (Map.Entry<K, V> entry : delegate.entrySet()) {
            builder.put(entry.getValue(), entry.getKey());
        }
        ImmutableMap<V, K> backwardMap = builder.build();
        this.inverse = new RegularImmutableBiMap(backwardMap, this);
    }

    RegularImmutableBiMap(ImmutableMap<K, V> delegate, ImmutableBiMap<V, K> inverse) {
        this.delegate = delegate;
        this.inverse = inverse;
    }

    @Override
    ImmutableMap<K, V> delegate() {
        return this.delegate;
    }

    @Override
    public ImmutableBiMap<V, K> inverse() {
        return this.inverse;
    }
}
