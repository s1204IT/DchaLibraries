package com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface Multimap<K, V> {
    Map<K, Collection<V>> asMap();

    boolean containsKey(Object obj);

    Collection<V> get(K k);

    Set<K> keySet();

    boolean put(K k, V v);
}
