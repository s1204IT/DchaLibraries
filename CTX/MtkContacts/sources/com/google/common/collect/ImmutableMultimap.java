package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset;
import com.google.common.collect.Serialization;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ImmutableMultimap<K, V> extends AbstractMultimap<K, V> implements Serializable {
    private static final long serialVersionUID = 0;
    final transient ImmutableMap<K, ? extends ImmutableCollection<V>> map;
    final transient int size;

    @Override
    public abstract ImmutableCollection<V> get(K k);

    public abstract ImmutableMultimap<V, K> inverse();

    @Override
    public boolean containsEntry(Object obj, Object obj2) {
        return super.containsEntry(obj, obj2);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static <K, V> ImmutableMultimap<K, V> of() {
        return ImmutableListMultimap.of();
    }

    public static <K, V> ImmutableMultimap<K, V> of(K k, V v) {
        return ImmutableListMultimap.of((Object) k, (Object) v);
    }

    public static <K, V> ImmutableMultimap<K, V> of(K k, V v, K k2, V v2) {
        return ImmutableListMultimap.of((Object) k, (Object) v, (Object) k2, (Object) v2);
    }

    public static <K, V> ImmutableMultimap<K, V> of(K k, V v, K k2, V v2, K k3, V v3) {
        return ImmutableListMultimap.of((Object) k, (Object) v, (Object) k2, (Object) v2, (Object) k3, (Object) v3);
    }

    public static <K, V> ImmutableMultimap<K, V> of(K k, V v, K k2, V v2, K k3, V v3, K k4, V v4) {
        return ImmutableListMultimap.of((Object) k, (Object) v, (Object) k2, (Object) v2, (Object) k3, (Object) v3, (Object) k4, (Object) v4);
    }

    public static <K, V> ImmutableMultimap<K, V> of(K k, V v, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return ImmutableListMultimap.of((Object) k, (Object) v, (Object) k2, (Object) v2, (Object) k3, (Object) v3, (Object) k4, (Object) v4, (Object) k5, (Object) v5);
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    private static class BuilderMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
        private static final long serialVersionUID = 0;

        BuilderMultimap() {
            super(new LinkedHashMap());
        }

        @Override
        Collection<V> createCollection() {
            return Lists.newArrayList();
        }
    }

    public static class Builder<K, V> {
        Multimap<K, V> builderMultimap = new BuilderMultimap();
        Comparator<? super K> keyComparator;
        Comparator<? super V> valueComparator;

        public Builder<K, V> put(K k, V v) {
            CollectPreconditions.checkEntryNotNull(k, v);
            this.builderMultimap.put(k, v);
            return this;
        }

        public Builder<K, V> put(Map.Entry<? extends K, ? extends V> entry) {
            return put(entry.getKey(), entry.getValue());
        }

        public Builder<K, V> putAll(K k, Iterable<? extends V> iterable) {
            if (k == null) {
                throw new NullPointerException("null key in entry: null=" + Iterables.toString(iterable));
            }
            Collection<V> collection = this.builderMultimap.get(k);
            for (V v : iterable) {
                CollectPreconditions.checkEntryNotNull(k, v);
                collection.add(v);
            }
            return this;
        }

        public Builder<K, V> putAll(K k, V... vArr) {
            return putAll(k, Arrays.asList(vArr));
        }

        public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
            for (Map.Entry<? extends K, Collection<? extends V>> entry : multimap.asMap().entrySet()) {
                putAll(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder<K, V> orderKeysBy(Comparator<? super K> comparator) {
            this.keyComparator = (Comparator) Preconditions.checkNotNull(comparator);
            return this;
        }

        public Builder<K, V> orderValuesBy(Comparator<? super V> comparator) {
            this.valueComparator = (Comparator) Preconditions.checkNotNull(comparator);
            return this;
        }

        public ImmutableMultimap<K, V> build() {
            if (this.valueComparator != null) {
                Iterator<Collection<V>> it = this.builderMultimap.asMap().values().iterator();
                while (it.hasNext()) {
                    Collections.sort((List) it.next(), this.valueComparator);
                }
            }
            if (this.keyComparator != null) {
                BuilderMultimap builderMultimap = new BuilderMultimap();
                ArrayList<Map.Entry> arrayListNewArrayList = Lists.newArrayList(this.builderMultimap.asMap().entrySet());
                Collections.sort(arrayListNewArrayList, Ordering.from(this.keyComparator).onKeys());
                for (Map.Entry entry : arrayListNewArrayList) {
                    builderMultimap.putAll(entry.getKey(), (Iterable) entry.getValue());
                }
                this.builderMultimap = builderMultimap;
            }
            return ImmutableMultimap.copyOf(this.builderMultimap);
        }
    }

    public static <K, V> ImmutableMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap) {
        if (multimap instanceof ImmutableMultimap) {
            ImmutableMultimap<K, V> immutableMultimap = (ImmutableMultimap) multimap;
            if (!immutableMultimap.isPartialView()) {
                return immutableMultimap;
            }
        }
        return ImmutableListMultimap.copyOf((Multimap) multimap);
    }

    static class FieldSettersHolder {
        static final Serialization.FieldSetter<ImmutableMultimap> MAP_FIELD_SETTER = Serialization.getFieldSetter(ImmutableMultimap.class, "map");
        static final Serialization.FieldSetter<ImmutableMultimap> SIZE_FIELD_SETTER = Serialization.getFieldSetter(ImmutableMultimap.class, "size");
        static final Serialization.FieldSetter<ImmutableSetMultimap> EMPTY_SET_FIELD_SETTER = Serialization.getFieldSetter(ImmutableSetMultimap.class, "emptySet");

        FieldSettersHolder() {
        }
    }

    ImmutableMultimap(ImmutableMap<K, ? extends ImmutableCollection<V>> immutableMap, int i) {
        this.map = immutableMap;
        this.size = i;
    }

    @Override
    @Deprecated
    public ImmutableCollection<V> removeAll(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public ImmutableCollection<V> replaceValues(K k, Iterable<? extends V> iterable) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean putAll(K k, Iterable<? extends V> iterable) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean remove(Object obj, Object obj2) {
        throw new UnsupportedOperationException();
    }

    boolean isPartialView() {
        return this.map.isPartialView();
    }

    @Override
    public boolean containsKey(Object obj) {
        return this.map.containsKey(obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        return obj != null && super.containsValue(obj);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public ImmutableSet<K> keySet() {
        return this.map.keySet();
    }

    @Override
    public ImmutableMap<K, Collection<V>> asMap() {
        return this.map;
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
        throw new AssertionError("should never be called");
    }

    @Override
    public ImmutableCollection<Map.Entry<K, V>> entries() {
        return (ImmutableCollection) super.entries();
    }

    @Override
    ImmutableCollection<Map.Entry<K, V>> createEntries() {
        return new EntryCollection(this);
    }

    private static class EntryCollection<K, V> extends ImmutableCollection<Map.Entry<K, V>> {
        private static final long serialVersionUID = 0;
        final ImmutableMultimap<K, V> multimap;

        EntryCollection(ImmutableMultimap<K, V> immutableMultimap) {
            this.multimap = immutableMultimap;
        }

        @Override
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return this.multimap.entryIterator();
        }

        @Override
        boolean isPartialView() {
            return this.multimap.isPartialView();
        }

        @Override
        public int size() {
            return this.multimap.size();
        }

        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                return this.multimap.containsEntry(entry.getKey(), entry.getValue());
            }
            return false;
        }
    }

    private abstract class Itr<T> extends UnmodifiableIterator<T> {
        K key;
        final Iterator<Map.Entry<K, Collection<V>>> mapIterator;
        Iterator<V> valueIterator;

        abstract T output(K k, V v);

        private Itr() {
            this.mapIterator = ImmutableMultimap.this.asMap().entrySet().iterator();
            this.key = null;
            this.valueIterator = Iterators.emptyIterator();
        }

        @Override
        public boolean hasNext() {
            return this.mapIterator.hasNext() || this.valueIterator.hasNext();
        }

        @Override
        public T next() {
            if (!this.valueIterator.hasNext()) {
                Map.Entry<K, Collection<V>> next = this.mapIterator.next();
                this.key = next.getKey();
                this.valueIterator = next.getValue().iterator();
            }
            return output(this.key, this.valueIterator.next());
        }
    }

    @Override
    UnmodifiableIterator<Map.Entry<K, V>> entryIterator() {
        return new ImmutableMultimap<K, V>.Itr<Map.Entry<K, V>>() {
            Map.Entry<K, V> output(K k, V v) {
                return Maps.immutableEntry(k, v);
            }
        };
    }

    @Override
    public ImmutableMultiset<K> keys() {
        return (ImmutableMultiset) super.keys();
    }

    @Override
    ImmutableMultiset<K> createKeys() {
        return new Keys();
    }

    class Keys extends ImmutableMultiset<K> {
        Keys() {
        }

        @Override
        public boolean contains(Object obj) {
            return ImmutableMultimap.this.containsKey(obj);
        }

        @Override
        public int count(Object obj) {
            ImmutableCollection<V> immutableCollection = ImmutableMultimap.this.map.get(obj);
            if (immutableCollection == null) {
                return 0;
            }
            return immutableCollection.size();
        }

        @Override
        public Set<K> elementSet() {
            return ImmutableMultimap.this.keySet();
        }

        @Override
        public int size() {
            return ImmutableMultimap.this.size();
        }

        @Override
        Multiset.Entry<K> getEntry(int i) {
            Map.Entry<K, ? extends ImmutableCollection<V>> entry = ImmutableMultimap.this.map.entrySet().asList().get(i);
            return Multisets.immutableEntry(entry.getKey(), entry.getValue().size());
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }

    @Override
    public ImmutableCollection<V> values() {
        return (ImmutableCollection) super.values();
    }

    @Override
    ImmutableCollection<V> createValues() {
        return new Values(this);
    }

    @Override
    UnmodifiableIterator<V> valueIterator() {
        return new ImmutableMultimap<K, V>.Itr<V>() {
            V output(K k, V v) {
                return v;
            }
        };
    }

    private static final class Values<K, V> extends ImmutableCollection<V> {
        private static final long serialVersionUID = 0;
        private final transient ImmutableMultimap<K, V> multimap;

        Values(ImmutableMultimap<K, V> immutableMultimap) {
            this.multimap = immutableMultimap;
        }

        @Override
        public boolean contains(Object obj) {
            return this.multimap.containsValue(obj);
        }

        @Override
        public UnmodifiableIterator<V> iterator() {
            return this.multimap.valueIterator();
        }

        @Override
        int copyIntoArray(Object[] objArr, int i) {
            UnmodifiableIterator<? extends ImmutableCollection<V>> it = this.multimap.map.values().iterator();
            while (it.hasNext()) {
                i = it.next().copyIntoArray(objArr, i);
            }
            return i;
        }

        @Override
        public int size() {
            return this.multimap.size();
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }
}
