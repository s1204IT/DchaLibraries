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

abstract class AbstractNavigableMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {

    private final class DescendingMap extends Maps.DescendingMap<K, V> {
        final AbstractNavigableMap this$0;

        private DescendingMap(AbstractNavigableMap abstractNavigableMap) {
            this.this$0 = abstractNavigableMap;
        }

        @Override
        Iterator<Map.Entry<K, V>> entryIterator() {
            return this.this$0.descendingEntryIterator();
        }

        @Override
        NavigableMap<K, V> forward() {
            return this.this$0;
        }
    }

    AbstractNavigableMap() {
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K k) {
        return tailMap(k, true).firstEntry();
    }

    @Override
    public K ceilingKey(K k) {
        return (K) Maps.keyOrNull(ceilingEntry(k));
    }

    abstract Iterator<Map.Entry<K, V>> descendingEntryIterator();

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return new DescendingMap();
    }

    abstract Iterator<Map.Entry<K, V>> entryIterator();

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new Maps.EntrySet<K, V>(this) {
            final AbstractNavigableMap this$0;

            {
                this.this$0 = this;
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return this.this$0.entryIterator();
            }

            @Override
            Map<K, V> map() {
                return this.this$0;
            }
        };
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        return (Map.Entry) Iterators.getNext(entryIterator(), null);
    }

    @Override
    public K firstKey() {
        Map.Entry<K, V> entryFirstEntry = firstEntry();
        if (entryFirstEntry != null) {
            return entryFirstEntry.getKey();
        }
        throw new NoSuchElementException();
    }

    @Override
    public Map.Entry<K, V> floorEntry(K k) {
        return headMap(k, true).lastEntry();
    }

    @Override
    public K floorKey(K k) {
        return (K) Maps.keyOrNull(floorEntry(k));
    }

    @Override
    public SortedMap<K, V> headMap(K k) {
        return headMap(k, false);
    }

    @Override
    public Map.Entry<K, V> higherEntry(K k) {
        return tailMap(k, false).firstEntry();
    }

    @Override
    public K higherKey(K k) {
        return (K) Maps.keyOrNull(higherEntry(k));
    }

    @Override
    public Set<K> keySet() {
        return navigableKeySet();
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return (Map.Entry) Iterators.getNext(descendingEntryIterator(), null);
    }

    @Override
    public K lastKey() {
        Map.Entry<K, V> entryLastEntry = lastEntry();
        if (entryLastEntry != null) {
            return entryLastEntry.getKey();
        }
        throw new NoSuchElementException();
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K k) {
        return headMap(k, false).lastEntry();
    }

    @Override
    public K lowerKey(K k) {
        return (K) Maps.keyOrNull(lowerEntry(k));
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new Maps.NavigableKeySet(this);
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return (Map.Entry) Iterators.pollNext(entryIterator());
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return (Map.Entry) Iterators.pollNext(descendingEntryIterator());
    }

    @Override
    public SortedMap<K, V> subMap(K k, K k2) {
        return subMap(k, true, k2, false);
    }

    @Override
    public SortedMap<K, V> tailMap(K k) {
        return tailMap(k, true);
    }
}
