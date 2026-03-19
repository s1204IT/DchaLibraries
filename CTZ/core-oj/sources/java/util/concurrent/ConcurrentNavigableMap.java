package java.util.concurrent;

import java.util.NavigableMap;
import java.util.NavigableSet;

public interface ConcurrentNavigableMap<K, V> extends ConcurrentMap<K, V>, NavigableMap<K, V> {
    @Override
    NavigableSet<K> descendingKeySet();

    @Override
    ConcurrentNavigableMap<K, V> descendingMap();

    @Override
    ConcurrentNavigableMap<K, V> headMap(K k);

    @Override
    ConcurrentNavigableMap<K, V> headMap(K k, boolean z);

    @Override
    NavigableSet<K> keySet();

    @Override
    NavigableSet<K> navigableKeySet();

    @Override
    ConcurrentNavigableMap<K, V> subMap(K k, K k2);

    @Override
    ConcurrentNavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2);

    @Override
    ConcurrentNavigableMap<K, V> tailMap(K k);

    @Override
    ConcurrentNavigableMap<K, V> tailMap(K k, boolean z);
}
