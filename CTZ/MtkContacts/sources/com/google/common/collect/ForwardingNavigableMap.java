package com.google.common.collect;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedMap;

public abstract class ForwardingNavigableMap<K, V> extends ForwardingSortedMap<K, V> implements NavigableMap<K, V> {
    @Override
    protected abstract NavigableMap<K, V> delegate();

    protected ForwardingNavigableMap() {
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K k) {
        return delegate().lowerEntry(k);
    }

    protected Map.Entry<K, V> standardLowerEntry(K k) {
        return headMap(k, false).lastEntry();
    }

    @Override
    public K lowerKey(K k) {
        return delegate().lowerKey(k);
    }

    protected K standardLowerKey(K k) {
        return (K) Maps.keyOrNull(lowerEntry(k));
    }

    @Override
    public Map.Entry<K, V> floorEntry(K k) {
        return delegate().floorEntry(k);
    }

    protected Map.Entry<K, V> standardFloorEntry(K k) {
        return headMap(k, true).lastEntry();
    }

    @Override
    public K floorKey(K k) {
        return delegate().floorKey(k);
    }

    protected K standardFloorKey(K k) {
        return (K) Maps.keyOrNull(floorEntry(k));
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K k) {
        return delegate().ceilingEntry(k);
    }

    protected Map.Entry<K, V> standardCeilingEntry(K k) {
        return tailMap(k, true).firstEntry();
    }

    @Override
    public K ceilingKey(K k) {
        return delegate().ceilingKey(k);
    }

    protected K standardCeilingKey(K k) {
        return (K) Maps.keyOrNull(ceilingEntry(k));
    }

    @Override
    public Map.Entry<K, V> higherEntry(K k) {
        return delegate().higherEntry(k);
    }

    protected Map.Entry<K, V> standardHigherEntry(K k) {
        return tailMap(k, false).firstEntry();
    }

    @Override
    public K higherKey(K k) {
        return delegate().higherKey(k);
    }

    protected K standardHigherKey(K k) {
        return (K) Maps.keyOrNull(higherEntry(k));
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        return delegate().firstEntry();
    }

    protected Map.Entry<K, V> standardFirstEntry() {
        return (Map.Entry) Iterables.getFirst(entrySet(), null);
    }

    protected K standardFirstKey() {
        Map.Entry<K, V> entryFirstEntry = firstEntry();
        if (entryFirstEntry == null) {
            throw new NoSuchElementException();
        }
        return entryFirstEntry.getKey();
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return delegate().lastEntry();
    }

    protected Map.Entry<K, V> standardLastEntry() {
        return (Map.Entry) Iterables.getFirst(descendingMap().entrySet(), null);
    }

    protected K standardLastKey() {
        Map.Entry<K, V> entryLastEntry = lastEntry();
        if (entryLastEntry == null) {
            throw new NoSuchElementException();
        }
        return entryLastEntry.getKey();
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return delegate().pollFirstEntry();
    }

    protected Map.Entry<K, V> standardPollFirstEntry() {
        return (Map.Entry) Iterators.pollNext(entrySet().iterator());
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return delegate().pollLastEntry();
    }

    protected Map.Entry<K, V> standardPollLastEntry() {
        return (Map.Entry) Iterators.pollNext(descendingMap().entrySet().iterator());
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return delegate().descendingMap();
    }

    protected class StandardDescendingMap extends Maps.DescendingMap<K, V> {
        public StandardDescendingMap() {
        }

        @Override
        NavigableMap<K, V> forward() {
            return ForwardingNavigableMap.this;
        }

        @Override
        protected Iterator<Map.Entry<K, V>> entryIterator() {
            return new Iterator<Map.Entry<K, V>>() {
                private Map.Entry<K, V> nextOrNull;
                private Map.Entry<K, V> toRemove = null;

                {
                    this.nextOrNull = StandardDescendingMap.this.forward().lastEntry();
                }

                @Override
                public boolean hasNext() {
                    return this.nextOrNull != null;
                }

                @Override
                public Map.Entry<K, V> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    try {
                        return this.nextOrNull;
                    } finally {
                        this.toRemove = this.nextOrNull;
                        this.nextOrNull = StandardDescendingMap.this.forward().lowerEntry(this.nextOrNull.getKey());
                    }
                }

                @Override
                public void remove() {
                    CollectPreconditions.checkRemove(this.toRemove != null);
                    StandardDescendingMap.this.forward().remove(this.toRemove.getKey());
                    this.toRemove = null;
                }
            };
        }
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return delegate().navigableKeySet();
    }

    protected class StandardNavigableKeySet extends Maps.NavigableKeySet<K, V> {
        public StandardNavigableKeySet() {
            super(ForwardingNavigableMap.this);
        }
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return delegate().descendingKeySet();
    }

    protected NavigableSet<K> standardDescendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    protected SortedMap<K, V> standardSubMap(K k, K k2) {
        return subMap(k, true, k2, false);
    }

    @Override
    public NavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
        return delegate().subMap(k, z, k2, z2);
    }

    @Override
    public NavigableMap<K, V> headMap(K k, boolean z) {
        return delegate().headMap(k, z);
    }

    @Override
    public NavigableMap<K, V> tailMap(K k, boolean z) {
        return delegate().tailMap(k, z);
    }

    protected SortedMap<K, V> standardHeadMap(K k) {
        return headMap(k, false);
    }

    protected SortedMap<K, V> standardTailMap(K k) {
        return tailMap(k, true);
    }
}
