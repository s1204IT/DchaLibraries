package com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SetMultimap<K, V> extends Multimap<K, V> {
    @Override
    Map<K, Collection<V>> asMap();

    @Override
    Set<Map.Entry<K, V>> entries();

    @Override
    boolean equals(Object obj);

    @Override
    Set<V> get(K k);

    @Override
    Set<V> removeAll(Object obj);

    @Override
    Set<V> replaceValues(K k, Iterable<? extends V> iterable);
}
