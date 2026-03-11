package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
abstract class AbstractMultimap<K, V> implements Multimap<K, V> {
    private transient Map<K, Collection<V>> asMap;
    private transient Collection<Map.Entry<K, V>> entries;
    private transient Set<K> keySet;

    abstract Map<K, Collection<V>> createAsMap();

    abstract Iterator<Map.Entry<K, V>> entryIterator();

    AbstractMultimap() {
    }

    @Override
    public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
        Collection<V> collection = asMap().get(key);
        if (collection != null) {
            return collection.contains(value);
        }
        return false;
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
        Collection<V> collection = asMap().get(key);
        if (collection != null) {
            return collection.remove(value);
        }
        return false;
    }

    public Collection<Map.Entry<K, V>> entries() {
        Collection<Map.Entry<K, V>> result = this.entries;
        if (result != null) {
            return result;
        }
        Collection<Map.Entry<K, V>> result2 = createEntries();
        this.entries = result2;
        return result2;
    }

    Collection<Map.Entry<K, V>> createEntries() {
        EntrySet entrySet = null;
        if (this instanceof SetMultimap) {
            return new EntrySet(this, entrySet);
        }
        return new Entries(this, entrySet);
    }

    private class Entries extends Multimaps.Entries<K, V> {
        Entries(AbstractMultimap this$0, Entries entries) {
            this();
        }

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
        EntrySet(AbstractMultimap this$0, EntrySet entrySet) {
            this();
        }

        private EntrySet() {
            super(AbstractMultimap.this, null);
        }

        @Override
        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return Sets.equalsImpl(this, obj);
        }
    }

    public Set<K> keySet() {
        Set<K> result = this.keySet;
        if (result != null) {
            return result;
        }
        Set<K> result2 = createKeySet();
        this.keySet = result2;
        return result2;
    }

    Set<K> createKeySet() {
        return new Maps.KeySet(asMap());
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        Map<K, Collection<V>> result = this.asMap;
        if (result != null) {
            return result;
        }
        Map<K, Collection<V>> result2 = createAsMap();
        this.asMap = result2;
        return result2;
    }

    public boolean equals(@Nullable Object object) {
        return Multimaps.equalsImpl(this, object);
    }

    public int hashCode() {
        return asMap().hashCode();
    }

    public String toString() {
        return asMap().toString();
    }
}
