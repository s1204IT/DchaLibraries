package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

abstract class AbstractMultimap<K, V> implements Multimap<K, V> {
    private transient Map<K, Collection<V>> asMap;
    private transient Collection<Map.Entry<K, V>> entries;
    private transient Set<K> keySet;
    private transient Collection<V> values;

    abstract Map<K, Collection<V>> createAsMap();

    abstract Iterator<Map.Entry<K, V>> entryIterator();

    AbstractMultimap() {
    }

    public boolean containsValue(Object obj) {
        Iterator<Collection<V>> it = asMap().values().iterator();
        while (it.hasNext()) {
            if (it.next().contains(obj)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsEntry(Object obj, Object obj2) {
        Collection<V> collection = asMap().get(obj);
        return collection != null && collection.contains(obj2);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        Collection<V> collection = asMap().get(obj);
        return collection != null && collection.remove(obj2);
    }

    @Override
    public boolean put(K k, V v) {
        return get(k).add(v);
    }

    @Override
    public boolean putAll(K k, Iterable<? extends V> iterable) {
        Preconditions.checkNotNull(iterable);
        if (iterable instanceof Collection) {
            Collection<? extends V> collection = (Collection) iterable;
            return !collection.isEmpty() && get(k).addAll(collection);
        }
        Iterator<? extends V> it = iterable.iterator();
        return it.hasNext() && Iterators.addAll(get(k), it);
    }

    @Override
    public Collection<V> replaceValues(K k, Iterable<? extends V> iterable) {
        Preconditions.checkNotNull(iterable);
        Collection<V> collectionRemoveAll = removeAll(k);
        putAll(k, iterable);
        return collectionRemoveAll;
    }

    public Collection<Map.Entry<K, V>> entries() {
        Collection<Map.Entry<K, V>> collection = this.entries;
        if (collection != null) {
            return collection;
        }
        Collection<Map.Entry<K, V>> collectionCreateEntries = createEntries();
        this.entries = collectionCreateEntries;
        return collectionCreateEntries;
    }

    Collection<Map.Entry<K, V>> createEntries() {
        if (this instanceof SetMultimap) {
            return new EntrySet();
        }
        return new Entries();
    }

    private class Entries extends Multimaps.Entries<K, V> {
        private Entries() {
        }

        @Override
        Multimap<K, V> multimap() {
            return AbstractMultimap.this;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return AbstractMultimap.this.entryIterator();
        }
    }

    private class EntrySet extends AbstractMultimap<K, V>.Entries implements Set<Map.Entry<K, V>> {
        private EntrySet() {
            super();
        }

        @Override
        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }

        @Override
        public boolean equals(Object obj) {
            return Sets.equalsImpl(this, obj);
        }
    }

    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set != null) {
            return set;
        }
        Set<K> setCreateKeySet = createKeySet();
        this.keySet = setCreateKeySet;
        return setCreateKeySet;
    }

    Set<K> createKeySet() {
        return new Maps.KeySet(asMap());
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection != null) {
            return collection;
        }
        Collection<V> collectionCreateValues = createValues();
        this.values = collectionCreateValues;
        return collectionCreateValues;
    }

    Collection<V> createValues() {
        return new Values();
    }

    class Values extends AbstractCollection<V> {
        Values() {
        }

        @Override
        public Iterator<V> iterator() {
            return AbstractMultimap.this.valueIterator();
        }

        @Override
        public int size() {
            return AbstractMultimap.this.size();
        }

        @Override
        public boolean contains(Object obj) {
            return AbstractMultimap.this.containsValue(obj);
        }

        @Override
        public void clear() {
            AbstractMultimap.this.clear();
        }
    }

    Iterator<V> valueIterator() {
        return Maps.valueIterator(entries().iterator());
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        Map<K, Collection<V>> map = this.asMap;
        if (map != null) {
            return map;
        }
        Map<K, Collection<V>> mapCreateAsMap = createAsMap();
        this.asMap = mapCreateAsMap;
        return mapCreateAsMap;
    }

    public boolean equals(Object obj) {
        return Multimaps.equalsImpl(this, obj);
    }

    public int hashCode() {
        return asMap().hashCode();
    }

    public String toString() {
        return asMap().toString();
    }
}
