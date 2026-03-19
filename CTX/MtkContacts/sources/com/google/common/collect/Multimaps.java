package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

public final class Multimaps {
    private Multimaps() {
    }

    public static <K, V> Multimap<K, V> newMultimap(Map<K, Collection<V>> map, Supplier<? extends Collection<V>> supplier) {
        return new CustomMultimap(map, supplier);
    }

    private static class CustomMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends Collection<V>> factory;

        CustomMultimap(Map<K, Collection<V>> map, Supplier<? extends Collection<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
        }

        @Override
        protected Collection<V> createCollection() {
            return this.factory.get();
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) objectInputStream.readObject();
            setMap((Map) objectInputStream.readObject());
        }
    }

    public static <K, V> ListMultimap<K, V> newListMultimap(Map<K, Collection<V>> map, Supplier<? extends List<V>> supplier) {
        return new CustomListMultimap(map, supplier);
    }

    private static class CustomListMultimap<K, V> extends AbstractListMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends List<V>> factory;

        CustomListMultimap(Map<K, Collection<V>> map, Supplier<? extends List<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
        }

        @Override
        protected List<V> createCollection() {
            return this.factory.get();
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) objectInputStream.readObject();
            setMap((Map) objectInputStream.readObject());
        }
    }

    public static <K, V> SetMultimap<K, V> newSetMultimap(Map<K, Collection<V>> map, Supplier<? extends Set<V>> supplier) {
        return new CustomSetMultimap(map, supplier);
    }

    private static class CustomSetMultimap<K, V> extends AbstractSetMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends Set<V>> factory;

        CustomSetMultimap(Map<K, Collection<V>> map, Supplier<? extends Set<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
        }

        @Override
        protected Set<V> createCollection() {
            return this.factory.get();
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) objectInputStream.readObject();
            setMap((Map) objectInputStream.readObject());
        }
    }

    public static <K, V> SortedSetMultimap<K, V> newSortedSetMultimap(Map<K, Collection<V>> map, Supplier<? extends SortedSet<V>> supplier) {
        return new CustomSortedSetMultimap(map, supplier);
    }

    private static class CustomSortedSetMultimap<K, V> extends AbstractSortedSetMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Supplier<? extends SortedSet<V>> factory;
        transient Comparator<? super V> valueComparator;

        CustomSortedSetMultimap(Map<K, Collection<V>> map, Supplier<? extends SortedSet<V>> supplier) {
            super(map);
            this.factory = (Supplier) Preconditions.checkNotNull(supplier);
            this.valueComparator = supplier.get().comparator();
        }

        @Override
        protected SortedSet<V> createCollection() {
            return this.factory.get();
        }

        @Override
        public Comparator<? super V> valueComparator() {
            return this.valueComparator;
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(this.factory);
            objectOutputStream.writeObject(backingMap());
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.factory = (Supplier) objectInputStream.readObject();
            this.valueComparator = this.factory.get().comparator();
            setMap((Map) objectInputStream.readObject());
        }
    }

    public static <K, V, M extends Multimap<K, V>> M invertFrom(Multimap<? extends V, ? extends K> multimap, M m) {
        Preconditions.checkNotNull(m);
        for (Map.Entry<? extends V, ? extends K> entry : multimap.entries()) {
            m.put(entry.getValue(), entry.getKey());
        }
        return m;
    }

    public static <K, V> Multimap<K, V> synchronizedMultimap(Multimap<K, V> multimap) {
        return Synchronized.multimap(multimap, null);
    }

    public static <K, V> Multimap<K, V> unmodifiableMultimap(Multimap<K, V> multimap) {
        if ((multimap instanceof UnmodifiableMultimap) || (multimap instanceof ImmutableMultimap)) {
            return multimap;
        }
        return new UnmodifiableMultimap(multimap);
    }

    @Deprecated
    public static <K, V> Multimap<K, V> unmodifiableMultimap(ImmutableMultimap<K, V> immutableMultimap) {
        return (Multimap) Preconditions.checkNotNull(immutableMultimap);
    }

    private static class UnmodifiableMultimap<K, V> extends ForwardingMultimap<K, V> implements Serializable {
        private static final long serialVersionUID = 0;
        final Multimap<K, V> delegate;
        transient Collection<Map.Entry<K, V>> entries;
        transient Set<K> keySet;
        transient Multiset<K> keys;
        transient Map<K, Collection<V>> map;
        transient Collection<V> values;

        UnmodifiableMultimap(Multimap<K, V> multimap) {
            this.delegate = (Multimap) Preconditions.checkNotNull(multimap);
        }

        @Override
        protected Multimap<K, V> delegate() {
            return this.delegate;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<K, Collection<V>> asMap() {
            Map<K, Collection<V>> map = this.map;
            if (map == null) {
                Map<K, Collection<V>> mapUnmodifiableMap = Collections.unmodifiableMap(Maps.transformValues(this.delegate.asMap(), new Function<Collection<V>, Collection<V>>() {
                    @Override
                    public Collection<V> apply(Collection<V> collection) {
                        return Multimaps.unmodifiableValueCollection(collection);
                    }
                }));
                this.map = mapUnmodifiableMap;
                return mapUnmodifiableMap;
            }
            return map;
        }

        @Override
        public Collection<Map.Entry<K, V>> entries() {
            Collection<Map.Entry<K, V>> collection = this.entries;
            if (collection == null) {
                Collection<Map.Entry<K, V>> collectionUnmodifiableEntries = Multimaps.unmodifiableEntries(this.delegate.entries());
                this.entries = collectionUnmodifiableEntries;
                return collectionUnmodifiableEntries;
            }
            return collection;
        }

        @Override
        public Collection<V> get(K k) {
            return Multimaps.unmodifiableValueCollection(this.delegate.get(k));
        }

        @Override
        public Multiset<K> keys() {
            Multiset<K> multiset = this.keys;
            if (multiset == null) {
                Multiset<K> multisetUnmodifiableMultiset = Multisets.unmodifiableMultiset(this.delegate.keys());
                this.keys = multisetUnmodifiableMultiset;
                return multisetUnmodifiableMultiset;
            }
            return multiset;
        }

        @Override
        public Set<K> keySet() {
            Set<K> set = this.keySet;
            if (set == null) {
                Set<K> setUnmodifiableSet = Collections.unmodifiableSet(this.delegate.keySet());
                this.keySet = setUnmodifiableSet;
                return setUnmodifiableSet;
            }
            return set;
        }

        @Override
        public boolean put(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> removeAll(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> replaceValues(K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            Collection<V> collection = this.values;
            if (collection == null) {
                Collection<V> collectionUnmodifiableCollection = Collections.unmodifiableCollection(this.delegate.values());
                this.values = collectionUnmodifiableCollection;
                return collectionUnmodifiableCollection;
            }
            return collection;
        }
    }

    private static class UnmodifiableListMultimap<K, V> extends UnmodifiableMultimap<K, V> implements ListMultimap<K, V> {
        private static final long serialVersionUID = 0;

        UnmodifiableListMultimap(ListMultimap<K, V> listMultimap) {
            super(listMultimap);
        }

        @Override
        public ListMultimap<K, V> delegate() {
            return (ListMultimap) super.delegate();
        }

        @Override
        public List<V> get(K k) {
            return Collections.unmodifiableList(delegate().get((Object) k));
        }

        @Override
        public List<V> removeAll(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<V> replaceValues(K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }
    }

    private static class UnmodifiableSetMultimap<K, V> extends UnmodifiableMultimap<K, V> implements SetMultimap<K, V> {
        private static final long serialVersionUID = 0;

        UnmodifiableSetMultimap(SetMultimap<K, V> setMultimap) {
            super(setMultimap);
        }

        @Override
        public SetMultimap<K, V> delegate() {
            return (SetMultimap) super.delegate();
        }

        @Override
        public Set<V> get(K k) {
            return Collections.unmodifiableSet(delegate().get((Object) k));
        }

        @Override
        public Set<Map.Entry<K, V>> entries() {
            return Maps.unmodifiableEntrySet(delegate().entries());
        }

        @Override
        public Set<V> removeAll(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<V> replaceValues(K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }
    }

    private static class UnmodifiableSortedSetMultimap<K, V> extends UnmodifiableSetMultimap<K, V> implements SortedSetMultimap<K, V> {
        private static final long serialVersionUID = 0;

        UnmodifiableSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap) {
            super(sortedSetMultimap);
        }

        @Override
        public SortedSetMultimap<K, V> delegate() {
            return (SortedSetMultimap) super.delegate();
        }

        @Override
        public SortedSet<V> get(K k) {
            return Collections.unmodifiableSortedSet(delegate().get((Object) k));
        }

        @Override
        public SortedSet<V> removeAll(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SortedSet<V> replaceValues(K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Comparator<? super V> valueComparator() {
            return delegate().valueComparator();
        }
    }

    public static <K, V> SetMultimap<K, V> synchronizedSetMultimap(SetMultimap<K, V> setMultimap) {
        return Synchronized.setMultimap(setMultimap, null);
    }

    public static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(SetMultimap<K, V> setMultimap) {
        if ((setMultimap instanceof UnmodifiableSetMultimap) || (setMultimap instanceof ImmutableSetMultimap)) {
            return setMultimap;
        }
        return new UnmodifiableSetMultimap(setMultimap);
    }

    @Deprecated
    public static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(ImmutableSetMultimap<K, V> immutableSetMultimap) {
        return (SetMultimap) Preconditions.checkNotNull(immutableSetMultimap);
    }

    public static <K, V> SortedSetMultimap<K, V> synchronizedSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap) {
        return Synchronized.sortedSetMultimap(sortedSetMultimap, null);
    }

    public static <K, V> SortedSetMultimap<K, V> unmodifiableSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap) {
        if (sortedSetMultimap instanceof UnmodifiableSortedSetMultimap) {
            return sortedSetMultimap;
        }
        return new UnmodifiableSortedSetMultimap(sortedSetMultimap);
    }

    public static <K, V> ListMultimap<K, V> synchronizedListMultimap(ListMultimap<K, V> listMultimap) {
        return Synchronized.listMultimap(listMultimap, null);
    }

    public static <K, V> ListMultimap<K, V> unmodifiableListMultimap(ListMultimap<K, V> listMultimap) {
        if ((listMultimap instanceof UnmodifiableListMultimap) || (listMultimap instanceof ImmutableListMultimap)) {
            return listMultimap;
        }
        return new UnmodifiableListMultimap(listMultimap);
    }

    @Deprecated
    public static <K, V> ListMultimap<K, V> unmodifiableListMultimap(ImmutableListMultimap<K, V> immutableListMultimap) {
        return (ListMultimap) Preconditions.checkNotNull(immutableListMultimap);
    }

    private static <V> Collection<V> unmodifiableValueCollection(Collection<V> collection) {
        if (collection instanceof SortedSet) {
            return Collections.unmodifiableSortedSet((SortedSet) collection);
        }
        if (collection instanceof Set) {
            return Collections.unmodifiableSet((Set) collection);
        }
        if (collection instanceof List) {
            return Collections.unmodifiableList((List) collection);
        }
        return Collections.unmodifiableCollection(collection);
    }

    private static <K, V> Collection<Map.Entry<K, V>> unmodifiableEntries(Collection<Map.Entry<K, V>> collection) {
        if (collection instanceof Set) {
            return Maps.unmodifiableEntrySet((Set) collection);
        }
        return new Maps.UnmodifiableEntries(Collections.unmodifiableCollection(collection));
    }

    public static <K, V> Map<K, List<V>> asMap(ListMultimap<K, V> listMultimap) {
        return listMultimap.asMap();
    }

    public static <K, V> Map<K, Set<V>> asMap(SetMultimap<K, V> setMultimap) {
        return setMultimap.asMap();
    }

    public static <K, V> Map<K, SortedSet<V>> asMap(SortedSetMultimap<K, V> sortedSetMultimap) {
        return sortedSetMultimap.asMap();
    }

    public static <K, V> Map<K, Collection<V>> asMap(Multimap<K, V> multimap) {
        return multimap.asMap();
    }

    public static <K, V> SetMultimap<K, V> forMap(Map<K, V> map) {
        return new MapMultimap(map);
    }

    private static class MapMultimap<K, V> extends AbstractMultimap<K, V> implements SetMultimap<K, V>, Serializable {
        private static final long serialVersionUID = 7845222491160860175L;
        final Map<K, V> map;

        MapMultimap(Map<K, V> map) {
            this.map = (Map) Preconditions.checkNotNull(map);
        }

        @Override
        public int size() {
            return this.map.size();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.map.containsKey(obj);
        }

        @Override
        public boolean containsValue(Object obj) {
            return this.map.containsValue(obj);
        }

        @Override
        public boolean containsEntry(Object obj, Object obj2) {
            return this.map.entrySet().contains(Maps.immutableEntry(obj, obj2));
        }

        @Override
        public Set<V> get(final K k) {
            return new Sets.ImprovedAbstractSet<V>() {
                @Override
                public Iterator<V> iterator() {
                    return new Iterator<V>() {
                        int i;

                        @Override
                        public boolean hasNext() {
                            return this.i == 0 && MapMultimap.this.map.containsKey(k);
                        }

                        @Override
                        public V next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            this.i++;
                            return MapMultimap.this.map.get(k);
                        }

                        @Override
                        public void remove() {
                            CollectPreconditions.checkRemove(this.i == 1);
                            this.i = -1;
                            MapMultimap.this.map.remove(k);
                        }
                    };
                }

                @Override
                public int size() {
                    return MapMultimap.this.map.containsKey(k) ? 1 : 0;
                }
            };
        }

        @Override
        public boolean put(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<V> replaceValues(K k, Iterable<? extends V> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            return this.map.entrySet().remove(Maps.immutableEntry(obj, obj2));
        }

        @Override
        public Set<V> removeAll(Object obj) {
            HashSet hashSet = new HashSet(2);
            if (!this.map.containsKey(obj)) {
                return hashSet;
            }
            hashSet.add(this.map.remove(obj));
            return hashSet;
        }

        @Override
        public void clear() {
            this.map.clear();
        }

        @Override
        public Set<K> keySet() {
            return this.map.keySet();
        }

        @Override
        public Collection<V> values() {
            return this.map.values();
        }

        @Override
        public Set<Map.Entry<K, V>> entries() {
            return this.map.entrySet();
        }

        @Override
        Iterator<Map.Entry<K, V>> entryIterator() {
            return this.map.entrySet().iterator();
        }

        @Override
        Map<K, Collection<V>> createAsMap() {
            return new AsMap(this);
        }

        @Override
        public int hashCode() {
            return this.map.hashCode();
        }
    }

    public static <K, V1, V2> Multimap<K, V2> transformValues(Multimap<K, V1> multimap, Function<? super V1, V2> function) {
        Preconditions.checkNotNull(function);
        return transformEntries(multimap, Maps.asEntryTransformer(function));
    }

    public static <K, V1, V2> Multimap<K, V2> transformEntries(Multimap<K, V1> multimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
        return new TransformedEntriesMultimap(multimap, entryTransformer);
    }

    private static class TransformedEntriesMultimap<K, V1, V2> extends AbstractMultimap<K, V2> {
        final Multimap<K, V1> fromMultimap;
        final Maps.EntryTransformer<? super K, ? super V1, V2> transformer;

        TransformedEntriesMultimap(Multimap<K, V1> multimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
            this.fromMultimap = (Multimap) Preconditions.checkNotNull(multimap);
            this.transformer = (Maps.EntryTransformer) Preconditions.checkNotNull(entryTransformer);
        }

        Collection<V2> transform(K k, Collection<V1> collection) {
            Function functionAsValueToValueFunction = Maps.asValueToValueFunction(this.transformer, k);
            if (collection instanceof List) {
                return Lists.transform((List) collection, functionAsValueToValueFunction);
            }
            return Collections2.transform(collection, functionAsValueToValueFunction);
        }

        @Override
        Map<K, Collection<V2>> createAsMap() {
            return Maps.transformEntries(this.fromMultimap.asMap(), new Maps.EntryTransformer<K, Collection<V1>, Collection<V2>>() {
                @Override
                public Collection<V2> transformEntry(K k, Collection<V1> collection) {
                    return TransformedEntriesMultimap.this.transform(k, collection);
                }
            });
        }

        @Override
        public void clear() {
            this.fromMultimap.clear();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.fromMultimap.containsKey(obj);
        }

        @Override
        Iterator<Map.Entry<K, V2>> entryIterator() {
            return Iterators.transform(this.fromMultimap.entries().iterator(), Maps.asEntryToEntryFunction(this.transformer));
        }

        @Override
        public Collection<V2> get(K k) {
            return transform(k, this.fromMultimap.get(k));
        }

        @Override
        public boolean isEmpty() {
            return this.fromMultimap.isEmpty();
        }

        @Override
        public Set<K> keySet() {
            return this.fromMultimap.keySet();
        }

        @Override
        public Multiset<K> keys() {
            return this.fromMultimap.keys();
        }

        @Override
        public boolean put(K k, V2 v2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(K k, Iterable<? extends V2> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(Multimap<? extends K, ? extends V2> multimap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            return get(obj).remove(obj2);
        }

        @Override
        public Collection<V2> removeAll(Object obj) {
            return transform(obj, this.fromMultimap.removeAll(obj));
        }

        @Override
        public Collection<V2> replaceValues(K k, Iterable<? extends V2> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return this.fromMultimap.size();
        }

        @Override
        Collection<V2> createValues() {
            return Collections2.transform(this.fromMultimap.entries(), Maps.asEntryToValueFunction(this.transformer));
        }
    }

    public static <K, V1, V2> ListMultimap<K, V2> transformValues(ListMultimap<K, V1> listMultimap, Function<? super V1, V2> function) {
        Preconditions.checkNotNull(function);
        return transformEntries((ListMultimap) listMultimap, Maps.asEntryTransformer(function));
    }

    public static <K, V1, V2> ListMultimap<K, V2> transformEntries(ListMultimap<K, V1> listMultimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
        return new TransformedEntriesListMultimap(listMultimap, entryTransformer);
    }

    private static final class TransformedEntriesListMultimap<K, V1, V2> extends TransformedEntriesMultimap<K, V1, V2> implements ListMultimap<K, V2> {
        TransformedEntriesListMultimap(ListMultimap<K, V1> listMultimap, Maps.EntryTransformer<? super K, ? super V1, V2> entryTransformer) {
            super(listMultimap, entryTransformer);
        }

        @Override
        List<V2> transform(K k, Collection<V1> collection) {
            return Lists.transform((List) collection, Maps.asValueToValueFunction(this.transformer, k));
        }

        @Override
        public List<V2> get(K k) {
            return transform((Object) k, (Collection) this.fromMultimap.get(k));
        }

        @Override
        public List<V2> removeAll(Object obj) {
            return transform(obj, (Collection) this.fromMultimap.removeAll(obj));
        }

        @Override
        public List<V2> replaceValues(K k, Iterable<? extends V2> iterable) {
            throw new UnsupportedOperationException();
        }
    }

    public static <K, V> ImmutableListMultimap<K, V> index(Iterable<V> iterable, Function<? super V, K> function) {
        return index(iterable.iterator(), function);
    }

    public static <K, V> ImmutableListMultimap<K, V> index(Iterator<V> it, Function<? super V, K> function) {
        Preconditions.checkNotNull(function);
        ImmutableListMultimap.Builder builder = ImmutableListMultimap.builder();
        while (it.hasNext()) {
            V next = it.next();
            Preconditions.checkNotNull(next, it);
            builder.put((Object) function.apply(next), (Object) next);
        }
        return builder.build();
    }

    static class Keys<K, V> extends AbstractMultiset<K> {
        final Multimap<K, V> multimap;

        Keys(Multimap<K, V> multimap) {
            this.multimap = multimap;
        }

        @Override
        Iterator<Multiset.Entry<K>> entryIterator() {
            return new TransformedIterator<Map.Entry<K, Collection<V>>, Multiset.Entry<K>>(this.multimap.asMap().entrySet().iterator()) {
                @Override
                Multiset.Entry<K> transform(final Map.Entry<K, Collection<V>> entry) {
                    return new Multisets.AbstractEntry<K>() {
                        @Override
                        public K getElement() {
                            return (K) entry.getKey();
                        }

                        @Override
                        public int getCount() {
                            return ((Collection) entry.getValue()).size();
                        }
                    };
                }
            };
        }

        @Override
        int distinctElements() {
            return this.multimap.asMap().size();
        }

        @Override
        Set<Multiset.Entry<K>> createEntrySet() {
            return new KeysEntrySet();
        }

        class KeysEntrySet extends Multisets.EntrySet<K> {
            KeysEntrySet() {
            }

            @Override
            Multiset<K> multiset() {
                return Keys.this;
            }

            @Override
            public Iterator<Multiset.Entry<K>> iterator() {
                return Keys.this.entryIterator();
            }

            @Override
            public int size() {
                return Keys.this.distinctElements();
            }

            @Override
            public boolean isEmpty() {
                return Keys.this.multimap.isEmpty();
            }

            @Override
            public boolean contains(Object obj) {
                if (!(obj instanceof Multiset.Entry)) {
                    return false;
                }
                Multiset.Entry entry = (Multiset.Entry) obj;
                Collection<V> collection = Keys.this.multimap.asMap().get(entry.getElement());
                return collection != null && collection.size() == entry.getCount();
            }

            @Override
            public boolean remove(Object obj) {
                if (obj instanceof Multiset.Entry) {
                    Multiset.Entry entry = (Multiset.Entry) obj;
                    Collection<V> collection = Keys.this.multimap.asMap().get(entry.getElement());
                    if (collection != null && collection.size() == entry.getCount()) {
                        collection.clear();
                        return true;
                    }
                    return false;
                }
                return false;
            }
        }

        @Override
        public boolean contains(Object obj) {
            return this.multimap.containsKey(obj);
        }

        @Override
        public Iterator<K> iterator() {
            return Maps.keyIterator(this.multimap.entries().iterator());
        }

        @Override
        public int count(Object obj) {
            Collection collection = (Collection) Maps.safeGet(this.multimap.asMap(), obj);
            if (collection == null) {
                return 0;
            }
            return collection.size();
        }

        @Override
        public int remove(Object obj, int i) {
            CollectPreconditions.checkNonnegative(i, "occurrences");
            if (i == 0) {
                return count(obj);
            }
            Collection collection = (Collection) Maps.safeGet(this.multimap.asMap(), obj);
            if (collection == null) {
                return 0;
            }
            int size = collection.size();
            if (i >= size) {
                collection.clear();
            } else {
                Iterator it = collection.iterator();
                for (int i2 = 0; i2 < i; i2++) {
                    it.next();
                    it.remove();
                }
            }
            return size;
        }

        @Override
        public void clear() {
            this.multimap.clear();
        }

        @Override
        public Set<K> elementSet() {
            return this.multimap.keySet();
        }
    }

    static abstract class Entries<K, V> extends AbstractCollection<Map.Entry<K, V>> {
        abstract Multimap<K, V> multimap();

        Entries() {
        }

        @Override
        public int size() {
            return multimap().size();
        }

        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                return multimap().containsEntry(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public boolean remove(Object obj) {
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                return multimap().remove(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public void clear() {
            multimap().clear();
        }
    }

    static final class AsMap<K, V> extends Maps.ImprovedAbstractMap<K, Collection<V>> {
        private final Multimap<K, V> multimap;

        AsMap(Multimap<K, V> multimap) {
            this.multimap = (Multimap) Preconditions.checkNotNull(multimap);
        }

        @Override
        public int size() {
            return this.multimap.keySet().size();
        }

        @Override
        protected Set<Map.Entry<K, Collection<V>>> createEntrySet() {
            return new EntrySet();
        }

        void removeValuesForKey(Object obj) {
            this.multimap.keySet().remove(obj);
        }

        class EntrySet extends Maps.EntrySet<K, Collection<V>> {
            EntrySet() {
            }

            @Override
            Map<K, Collection<V>> map() {
                return AsMap.this;
            }

            @Override
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return Maps.asMapEntryIterator(AsMap.this.multimap.keySet(), new Function<K, Collection<V>>() {
                    @Override
                    public Collection<V> apply(K k) {
                        return AsMap.this.multimap.get(k);
                    }
                });
            }

            @Override
            public boolean remove(Object obj) {
                if (!contains(obj)) {
                    return false;
                }
                AsMap.this.removeValuesForKey(((Map.Entry) obj).getKey());
                return true;
            }
        }

        @Override
        public Collection<V> get(Object obj) {
            if (containsKey(obj)) {
                return this.multimap.get(obj);
            }
            return null;
        }

        @Override
        public Collection<V> remove(Object obj) {
            if (containsKey(obj)) {
                return this.multimap.removeAll(obj);
            }
            return null;
        }

        @Override
        public Set<K> keySet() {
            return this.multimap.keySet();
        }

        @Override
        public boolean isEmpty() {
            return this.multimap.isEmpty();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.multimap.containsKey(obj);
        }

        @Override
        public void clear() {
            this.multimap.clear();
        }
    }

    public static <K, V> Multimap<K, V> filterKeys(Multimap<K, V> multimap, Predicate<? super K> predicate) {
        if (multimap instanceof SetMultimap) {
            return filterKeys((SetMultimap) multimap, (Predicate) predicate);
        }
        if (multimap instanceof ListMultimap) {
            return filterKeys((ListMultimap) multimap, (Predicate) predicate);
        }
        if (multimap instanceof FilteredKeyMultimap) {
            FilteredKeyMultimap filteredKeyMultimap = (FilteredKeyMultimap) multimap;
            return new FilteredKeyMultimap(filteredKeyMultimap.unfiltered, Predicates.and(filteredKeyMultimap.keyPredicate, predicate));
        }
        if (multimap instanceof FilteredMultimap) {
            return filterFiltered((FilteredMultimap) multimap, Maps.keyPredicateOnEntries(predicate));
        }
        return new FilteredKeyMultimap(multimap, predicate);
    }

    public static <K, V> SetMultimap<K, V> filterKeys(SetMultimap<K, V> setMultimap, Predicate<? super K> predicate) {
        if (setMultimap instanceof FilteredKeySetMultimap) {
            FilteredKeySetMultimap filteredKeySetMultimap = (FilteredKeySetMultimap) setMultimap;
            return new FilteredKeySetMultimap(filteredKeySetMultimap.unfiltered(), Predicates.and(filteredKeySetMultimap.keyPredicate, predicate));
        }
        if (setMultimap instanceof FilteredSetMultimap) {
            return filterFiltered((FilteredSetMultimap) setMultimap, Maps.keyPredicateOnEntries(predicate));
        }
        return new FilteredKeySetMultimap(setMultimap, predicate);
    }

    public static <K, V> ListMultimap<K, V> filterKeys(ListMultimap<K, V> listMultimap, Predicate<? super K> predicate) {
        if (listMultimap instanceof FilteredKeyListMultimap) {
            FilteredKeyListMultimap filteredKeyListMultimap = (FilteredKeyListMultimap) listMultimap;
            return new FilteredKeyListMultimap(filteredKeyListMultimap.unfiltered(), Predicates.and(filteredKeyListMultimap.keyPredicate, predicate));
        }
        return new FilteredKeyListMultimap(listMultimap, predicate);
    }

    public static <K, V> Multimap<K, V> filterValues(Multimap<K, V> multimap, Predicate<? super V> predicate) {
        return filterEntries(multimap, Maps.valuePredicateOnEntries(predicate));
    }

    public static <K, V> SetMultimap<K, V> filterValues(SetMultimap<K, V> setMultimap, Predicate<? super V> predicate) {
        return filterEntries((SetMultimap) setMultimap, Maps.valuePredicateOnEntries(predicate));
    }

    public static <K, V> Multimap<K, V> filterEntries(Multimap<K, V> multimap, Predicate<? super Map.Entry<K, V>> predicate) {
        Preconditions.checkNotNull(predicate);
        if (multimap instanceof SetMultimap) {
            return filterEntries((SetMultimap) multimap, (Predicate) predicate);
        }
        if (multimap instanceof FilteredMultimap) {
            return filterFiltered((FilteredMultimap) multimap, predicate);
        }
        return new FilteredEntryMultimap((Multimap) Preconditions.checkNotNull(multimap), predicate);
    }

    public static <K, V> SetMultimap<K, V> filterEntries(SetMultimap<K, V> setMultimap, Predicate<? super Map.Entry<K, V>> predicate) {
        Preconditions.checkNotNull(predicate);
        if (setMultimap instanceof FilteredSetMultimap) {
            return filterFiltered((FilteredSetMultimap) setMultimap, (Predicate) predicate);
        }
        return new FilteredEntrySetMultimap((SetMultimap) Preconditions.checkNotNull(setMultimap), predicate);
    }

    private static <K, V> Multimap<K, V> filterFiltered(FilteredMultimap<K, V> filteredMultimap, Predicate<? super Map.Entry<K, V>> predicate) {
        return new FilteredEntryMultimap(filteredMultimap.unfiltered(), Predicates.and(filteredMultimap.entryPredicate(), predicate));
    }

    private static <K, V> SetMultimap<K, V> filterFiltered(FilteredSetMultimap<K, V> filteredSetMultimap, Predicate<? super Map.Entry<K, V>> predicate) {
        return new FilteredEntrySetMultimap(filteredSetMultimap.unfiltered(), Predicates.and(filteredSetMultimap.entryPredicate(), predicate));
    }

    static boolean equalsImpl(Multimap<?, ?> multimap, Object obj) {
        if (obj == multimap) {
            return true;
        }
        if (obj instanceof Multimap) {
            return multimap.asMap().equals(((Multimap) obj).asMap());
        }
        return false;
    }
}
