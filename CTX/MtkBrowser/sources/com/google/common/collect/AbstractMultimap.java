package com.google.common.collect;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

abstract class AbstractMultimap<K, V> implements Multimap<K, V> {
    private transient Map<K, Collection<V>> asMap;
    private transient Collection<Map.Entry<K, V>> entries;
    private transient Set<K> keySet;

    private class Entries extends Multimaps.Entries<K, V> {
        final AbstractMultimap this$0;

        private Entries(AbstractMultimap abstractMultimap) {
            this.this$0 = abstractMultimap;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return this.this$0.entryIterator();
        }

        @Override
        Multimap<K, V> multimap() {
            return this.this$0;
        }
    }

    private class EntrySet extends AbstractMultimap<K, V>.Entries implements Set<Map.Entry<K, V>> {
        final AbstractMultimap this$0;

        private EntrySet(AbstractMultimap abstractMultimap) {
            super();
            this.this$0 = abstractMultimap;
        }

        @Override
        public boolean equals(Object obj) {
            return Sets.equalsImpl(this, obj);
        }

        @Override
        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }
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

    @Override
    public boolean containsEntry(Object obj, Object obj2) {
        Collection<V> collection = asMap().get(obj);
        return collection != null && collection.contains(obj2);
    }

    abstract Map<K, Collection<V>> createAsMap();

    Collection<Map.Entry<K, V>> createEntries() {
        return this instanceof SetMultimap ? new EntrySet() : new Entries();
    }

    Set<K> createKeySet() {
        return new Maps.KeySet(asMap());
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

    abstract Iterator<Map.Entry<K, V>> entryIterator();

    public boolean equals(Object obj) {
        return Multimaps.equalsImpl(this, obj);
    }

    public int hashCode() {
        return asMap().hashCode();
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

    @Override
    public boolean remove(Object obj, Object obj2) {
        Collection<V> collection = asMap().get(obj);
        return collection != null && collection.remove(obj2);
    }

    public String toString() {
        return asMap().toString();
    }
}
