package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedMap;

public abstract class ForwardingSortedMap<K, V> extends ForwardingMap<K, V> implements SortedMap<K, V> {
    @Override
    protected abstract SortedMap<K, V> delegate();

    protected ForwardingSortedMap() {
    }

    @Override
    public Comparator<? super K> comparator() {
        return delegate().comparator();
    }

    @Override
    public K firstKey() {
        return delegate().firstKey();
    }

    @Override
    public SortedMap<K, V> headMap(K k) {
        return delegate().headMap(k);
    }

    @Override
    public K lastKey() {
        return delegate().lastKey();
    }

    @Override
    public SortedMap<K, V> subMap(K k, K k2) {
        return delegate().subMap(k, k2);
    }

    @Override
    public SortedMap<K, V> tailMap(K k) {
        return delegate().tailMap(k);
    }

    protected class StandardKeySet extends Maps.SortedKeySet<K, V> {
        public StandardKeySet() {
            super(ForwardingSortedMap.this);
        }
    }

    private int unsafeCompare(Object obj, Object obj2) {
        Comparator<? super K> comparator = comparator();
        if (comparator == null) {
            return ((Comparable) obj).compareTo(obj2);
        }
        return comparator.compare(obj, obj2);
    }

    @Override
    protected boolean standardContainsKey(Object obj) {
        try {
            return unsafeCompare(tailMap(obj).firstKey(), obj) == 0;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        } catch (NoSuchElementException e3) {
            return false;
        }
    }

    protected SortedMap<K, V> standardSubMap(K k, K k2) {
        Preconditions.checkArgument(unsafeCompare(k, k2) <= 0, "fromKey must be <= toKey");
        return tailMap(k).headMap(k2);
    }
}
