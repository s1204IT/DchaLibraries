package com.google.common.collect;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ListMultimap<K, V> extends Multimap<K, V> {
    Map<K, Collection<V>> asMap();

    boolean equals(Object obj);

    @Override
    List<V> get(K k);

    @Override
    List<V> removeAll(Object obj);

    @Override
    List<V> replaceValues(K k, Iterable<? extends V> iterable);
}
