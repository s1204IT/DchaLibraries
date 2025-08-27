package com.google.common.collect;

import com.google.common.collect.Maps;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

/* loaded from: classes.dex */
abstract class AbstractNavigableMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
    abstract Iterator<Map.Entry<K, V>> descendingEntryIterator();

    abstract Iterator<Map.Entry<K, V>> entryIterator();

    AbstractNavigableMap() {
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> firstEntry() {
        return (Map.Entry) Iterators.getNext(entryIterator(), null);
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> lastEntry() {
        return (Map.Entry) Iterators.getNext(descendingEntryIterator(), null);
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> pollFirstEntry() {
        return (Map.Entry) Iterators.pollNext(entryIterator());
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> pollLastEntry() {
        return (Map.Entry) Iterators.pollNext(descendingEntryIterator());
    }

    @Override // java.util.SortedMap
    public K firstKey() {
        Map.Entry<K, V> entryFirstEntry = firstEntry();
        if (entryFirstEntry == null) {
            throw new NoSuchElementException();
        }
        return entryFirstEntry.getKey();
    }

    @Override // java.util.SortedMap
    public K lastKey() {
        Map.Entry<K, V> entryLastEntry = lastEntry();
        if (entryLastEntry == null) {
            throw new NoSuchElementException();
        }
        return entryLastEntry.getKey();
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> lowerEntry(K k) {
        return headMap(k, false).lastEntry();
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> floorEntry(K k) {
        return headMap(k, true).lastEntry();
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> ceilingEntry(K k) {
        return tailMap(k, true).firstEntry();
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> higherEntry(K k) {
        return tailMap(k, false).firstEntry();
    }

    @Override // java.util.NavigableMap
    public K lowerKey(K k) {
        return (K) Maps.keyOrNull(lowerEntry(k));
    }

    @Override // java.util.NavigableMap
    public K floorKey(K k) {
        return (K) Maps.keyOrNull(floorEntry(k));
    }

    @Override // java.util.NavigableMap
    public K ceilingKey(K k) {
        return (K) Maps.keyOrNull(ceilingEntry(k));
    }

    @Override // java.util.NavigableMap
    public K higherKey(K k) {
        return (K) Maps.keyOrNull(higherEntry(k));
    }

    @Override // java.util.NavigableMap, java.util.SortedMap
    public SortedMap<K, V> subMap(K k, K k2) {
        return subMap(k, true, k2, false);
    }

    @Override // java.util.NavigableMap, java.util.SortedMap
    public SortedMap<K, V> headMap(K k) {
        return headMap(k, false);
    }

    @Override // java.util.NavigableMap, java.util.SortedMap
    public SortedMap<K, V> tailMap(K k) {
        return tailMap(k, true);
    }

    @Override // java.util.NavigableMap
    public NavigableSet<K> navigableKeySet() {
        return new Maps.NavigableKeySet(this);
    }

    @Override // java.util.AbstractMap, java.util.Map, java.util.SortedMap
    public Set<K> keySet() {
        return navigableKeySet();
    }

    @Override // java.util.AbstractMap, java.util.Map, java.util.SortedMap
    public Set<Map.Entry<K, V>> entrySet() {
        return new Maps.EntrySet<K, V>() { // from class: com.google.common.collect.AbstractNavigableMap.1
            @Override // com.google.common.collect.Maps.EntrySet
            Map<K, V> map() {
                return AbstractNavigableMap.this;
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
            public Iterator<Map.Entry<K, V>> iterator() {
                return AbstractNavigableMap.this.entryIterator();
            }
        };
    }

    @Override // java.util.NavigableMap
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override // java.util.NavigableMap
    public NavigableMap<K, V> descendingMap() {
        return new DescendingMap();
    }

    private final class DescendingMap extends Maps.DescendingMap<K, V> {
        private DescendingMap() {
        }

        @Override // com.google.common.collect.Maps.DescendingMap
        NavigableMap<K, V> forward() {
            return AbstractNavigableMap.this;
        }

        @Override // com.google.common.collect.Maps.DescendingMap
        Iterator<Map.Entry<K, V>> entryIterator() {
            return AbstractNavigableMap.this.descendingEntryIterator();
        }
    }
}
