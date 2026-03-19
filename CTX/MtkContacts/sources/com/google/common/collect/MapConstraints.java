package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public final class MapConstraints {
    private MapConstraints() {
    }

    public static MapConstraint<Object, Object> notNull() {
        return NotNullMapConstraint.INSTANCE;
    }

    private enum NotNullMapConstraint implements MapConstraint<Object, Object> {
        INSTANCE;

        @Override
        public void checkKeyValue(Object obj, Object obj2) {
            Preconditions.checkNotNull(obj);
            Preconditions.checkNotNull(obj2);
        }

        @Override
        public String toString() {
            return "Not null";
        }
    }

    public static <K, V> Map<K, V> constrainedMap(Map<K, V> map, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedMap(map, mapConstraint);
    }

    public static <K, V> Multimap<K, V> constrainedMultimap(Multimap<K, V> multimap, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedMultimap(multimap, mapConstraint);
    }

    public static <K, V> ListMultimap<K, V> constrainedListMultimap(ListMultimap<K, V> listMultimap, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedListMultimap(listMultimap, mapConstraint);
    }

    public static <K, V> SetMultimap<K, V> constrainedSetMultimap(SetMultimap<K, V> setMultimap, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedSetMultimap(setMultimap, mapConstraint);
    }

    public static <K, V> SortedSetMultimap<K, V> constrainedSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedSortedSetMultimap(sortedSetMultimap, mapConstraint);
    }

    private static <K, V> Map.Entry<K, V> constrainedEntry(final Map.Entry<K, V> entry, final MapConstraint<? super K, ? super V> mapConstraint) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(mapConstraint);
        return new ForwardingMapEntry<K, V>() {
            @Override
            protected Map.Entry<K, V> delegate() {
                return entry;
            }

            @Override
            public V setValue(V v) {
                mapConstraint.checkKeyValue(getKey(), v);
                return (V) entry.setValue(v);
            }
        };
    }

    private static <K, V> Map.Entry<K, Collection<V>> constrainedAsMapEntry(final Map.Entry<K, Collection<V>> entry, final MapConstraint<? super K, ? super V> mapConstraint) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(mapConstraint);
        return new ForwardingMapEntry<K, Collection<V>>() {
            @Override
            protected Map.Entry<K, Collection<V>> delegate() {
                return entry;
            }

            @Override
            public Collection<V> getValue() {
                return Constraints.constrainedTypePreservingCollection((Collection) entry.getValue(), new Constraint<V>() {
                    @Override
                    public V checkElement(V v) {
                        mapConstraint.checkKeyValue(getKey(), v);
                        return v;
                    }
                });
            }
        };
    }

    private static <K, V> Set<Map.Entry<K, Collection<V>>> constrainedAsMapEntries(Set<Map.Entry<K, Collection<V>>> set, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedAsMapEntries(set, mapConstraint);
    }

    private static <K, V> Collection<Map.Entry<K, V>> constrainedEntries(Collection<Map.Entry<K, V>> collection, MapConstraint<? super K, ? super V> mapConstraint) {
        if (collection instanceof Set) {
            return constrainedEntrySet((Set) collection, mapConstraint);
        }
        return new ConstrainedEntries(collection, mapConstraint);
    }

    private static <K, V> Set<Map.Entry<K, V>> constrainedEntrySet(Set<Map.Entry<K, V>> set, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedEntrySet(set, mapConstraint);
    }

    static class ConstrainedMap<K, V> extends ForwardingMap<K, V> {
        final MapConstraint<? super K, ? super V> constraint;
        private final Map<K, V> delegate;
        private transient Set<Map.Entry<K, V>> entrySet;

        ConstrainedMap(Map<K, V> map, MapConstraint<? super K, ? super V> mapConstraint) {
            this.delegate = (Map) Preconditions.checkNotNull(map);
            this.constraint = (MapConstraint) Preconditions.checkNotNull(mapConstraint);
        }

        @Override
        protected Map<K, V> delegate() {
            return this.delegate;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K, V>> set = this.entrySet;
            if (set == null) {
                Set<Map.Entry<K, V>> setConstrainedEntrySet = MapConstraints.constrainedEntrySet(this.delegate.entrySet(), this.constraint);
                this.entrySet = setConstrainedEntrySet;
                return setConstrainedEntrySet;
            }
            return set;
        }

        @Override
        public V put(K k, V v) {
            this.constraint.checkKeyValue(k, v);
            return this.delegate.put(k, v);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            this.delegate.putAll(MapConstraints.checkMap(map, this.constraint));
        }
    }

    public static <K, V> BiMap<K, V> constrainedBiMap(BiMap<K, V> biMap, MapConstraint<? super K, ? super V> mapConstraint) {
        return new ConstrainedBiMap(biMap, null, mapConstraint);
    }

    private static class ConstrainedBiMap<K, V> extends ConstrainedMap<K, V> implements BiMap<K, V> {
        volatile BiMap<V, K> inverse;

        ConstrainedBiMap(BiMap<K, V> biMap, BiMap<V, K> biMap2, MapConstraint<? super K, ? super V> mapConstraint) {
            super(biMap, mapConstraint);
            this.inverse = biMap2;
        }

        @Override
        protected BiMap<K, V> delegate() {
            return (BiMap) super.delegate();
        }

        @Override
        public V forcePut(K k, V v) {
            this.constraint.checkKeyValue(k, v);
            return delegate().forcePut(k, v);
        }

        @Override
        public BiMap<V, K> inverse() {
            if (this.inverse == null) {
                this.inverse = new ConstrainedBiMap(delegate().inverse(), this, new InverseConstraint(this.constraint));
            }
            return this.inverse;
        }

        @Override
        public Set<V> values() {
            return delegate().values();
        }
    }

    private static class InverseConstraint<K, V> implements MapConstraint<K, V> {
        final MapConstraint<? super V, ? super K> constraint;

        public InverseConstraint(MapConstraint<? super V, ? super K> mapConstraint) {
            this.constraint = (MapConstraint) Preconditions.checkNotNull(mapConstraint);
        }

        @Override
        public void checkKeyValue(K k, V v) {
            this.constraint.checkKeyValue(v, k);
        }
    }

    private static class ConstrainedMultimap<K, V> extends ForwardingMultimap<K, V> implements Serializable {
        transient Map<K, Collection<V>> asMap;
        final MapConstraint<? super K, ? super V> constraint;
        final Multimap<K, V> delegate;
        transient Collection<Map.Entry<K, V>> entries;

        public ConstrainedMultimap(Multimap<K, V> multimap, MapConstraint<? super K, ? super V> mapConstraint) {
            this.delegate = (Multimap) Preconditions.checkNotNull(multimap);
            this.constraint = (MapConstraint) Preconditions.checkNotNull(mapConstraint);
        }

        @Override
        protected Multimap<K, V> delegate() {
            return this.delegate;
        }

        @Override
        public Map<K, Collection<V>> asMap() {
            Map<K, Collection<V>> map = this.asMap;
            if (map != null) {
                return map;
            }
            final Map<K, Collection<V>> mapAsMap = this.delegate.asMap();
            ForwardingMap<K, Collection<V>> forwardingMap = new ForwardingMap<K, Collection<V>>() {
                Set<Map.Entry<K, Collection<V>>> entrySet;
                Collection<Collection<V>> values;

                @Override
                protected Map<K, Collection<V>> delegate() {
                    return mapAsMap;
                }

                @Override
                public Set<Map.Entry<K, Collection<V>>> entrySet() {
                    Set<Map.Entry<K, Collection<V>>> set = this.entrySet;
                    if (set == null) {
                        Set<Map.Entry<K, Collection<V>>> setConstrainedAsMapEntries = MapConstraints.constrainedAsMapEntries(mapAsMap.entrySet(), ConstrainedMultimap.this.constraint);
                        this.entrySet = setConstrainedAsMapEntries;
                        return setConstrainedAsMapEntries;
                    }
                    return set;
                }

                @Override
                public Collection<V> get(Object obj) {
                    try {
                        Collection<V> collection = ConstrainedMultimap.this.get(obj);
                        if (collection.isEmpty()) {
                            return null;
                        }
                        return collection;
                    } catch (ClassCastException e) {
                        return null;
                    }
                }

                @Override
                public Collection<Collection<V>> values() {
                    Collection<Collection<V>> collection = this.values;
                    if (collection == null) {
                        ConstrainedAsMapValues constrainedAsMapValues = new ConstrainedAsMapValues(delegate().values(), entrySet());
                        this.values = constrainedAsMapValues;
                        return constrainedAsMapValues;
                    }
                    return collection;
                }

                @Override
                public boolean containsValue(Object obj) {
                    return values().contains(obj);
                }
            };
            this.asMap = forwardingMap;
            return forwardingMap;
        }

        @Override
        public Collection<Map.Entry<K, V>> entries() {
            Collection<Map.Entry<K, V>> collection = this.entries;
            if (collection == null) {
                Collection<Map.Entry<K, V>> collectionConstrainedEntries = MapConstraints.constrainedEntries(this.delegate.entries(), this.constraint);
                this.entries = collectionConstrainedEntries;
                return collectionConstrainedEntries;
            }
            return collection;
        }

        @Override
        public Collection<V> get(final K k) {
            return Constraints.constrainedTypePreservingCollection(this.delegate.get(k), new Constraint<V>() {
                @Override
                public V checkElement(V v) {
                    ConstrainedMultimap.this.constraint.checkKeyValue((Object) k, v);
                    return v;
                }
            });
        }

        @Override
        public boolean put(K k, V v) {
            this.constraint.checkKeyValue(k, v);
            return this.delegate.put(k, v);
        }

        @Override
        public boolean putAll(K k, Iterable<? extends V> iterable) {
            return this.delegate.putAll(k, MapConstraints.checkValues(k, iterable, this.constraint));
        }

        @Override
        public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
            boolean zPut = false;
            for (Map.Entry<? extends K, ? extends V> entry : multimap.entries()) {
                zPut |= put(entry.getKey(), entry.getValue());
            }
            return zPut;
        }

        @Override
        public Collection<V> replaceValues(K k, Iterable<? extends V> iterable) {
            return this.delegate.replaceValues(k, MapConstraints.checkValues(k, iterable, this.constraint));
        }
    }

    private static class ConstrainedAsMapValues<K, V> extends ForwardingCollection<Collection<V>> {
        final Collection<Collection<V>> delegate;
        final Set<Map.Entry<K, Collection<V>>> entrySet;

        ConstrainedAsMapValues(Collection<Collection<V>> collection, Set<Map.Entry<K, Collection<V>>> set) {
            this.delegate = collection;
            this.entrySet = set;
        }

        @Override
        protected Collection<Collection<V>> delegate() {
            return this.delegate;
        }

        @Override
        public Iterator<Collection<V>> iterator() {
            final Iterator<Map.Entry<K, Collection<V>>> it = this.entrySet.iterator();
            return new Iterator<Collection<V>>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Collection<V> next() {
                    return (Collection) ((Map.Entry) it.next()).getValue();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public Object[] toArray() {
            return standardToArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) standardToArray(tArr);
        }

        @Override
        public boolean contains(Object obj) {
            return standardContains(obj);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return standardContainsAll(collection);
        }

        @Override
        public boolean remove(Object obj) {
            return standardRemove(obj);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return standardRemoveAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return standardRetainAll(collection);
        }
    }

    private static class ConstrainedEntries<K, V> extends ForwardingCollection<Map.Entry<K, V>> {
        final MapConstraint<? super K, ? super V> constraint;
        final Collection<Map.Entry<K, V>> entries;

        ConstrainedEntries(Collection<Map.Entry<K, V>> collection, MapConstraint<? super K, ? super V> mapConstraint) {
            this.entries = collection;
            this.constraint = mapConstraint;
        }

        @Override
        protected Collection<Map.Entry<K, V>> delegate() {
            return this.entries;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            final Iterator<Map.Entry<K, V>> it = this.entries.iterator();
            return new ForwardingIterator<Map.Entry<K, V>>() {
                @Override
                public Map.Entry<K, V> next() {
                    return MapConstraints.constrainedEntry((Map.Entry) it.next(), ConstrainedEntries.this.constraint);
                }

                @Override
                protected Iterator<Map.Entry<K, V>> delegate() {
                    return it;
                }
            };
        }

        @Override
        public Object[] toArray() {
            return standardToArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) standardToArray(tArr);
        }

        @Override
        public boolean contains(Object obj) {
            return Maps.containsEntryImpl(delegate(), obj);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return standardContainsAll(collection);
        }

        @Override
        public boolean remove(Object obj) {
            return Maps.removeEntryImpl(delegate(), obj);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return standardRemoveAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return standardRetainAll(collection);
        }
    }

    static class ConstrainedEntrySet<K, V> extends ConstrainedEntries<K, V> implements Set<Map.Entry<K, V>> {
        ConstrainedEntrySet(Set<Map.Entry<K, V>> set, MapConstraint<? super K, ? super V> mapConstraint) {
            super(set, mapConstraint);
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

    static class ConstrainedAsMapEntries<K, V> extends ForwardingSet<Map.Entry<K, Collection<V>>> {
        private final MapConstraint<? super K, ? super V> constraint;
        private final Set<Map.Entry<K, Collection<V>>> entries;

        ConstrainedAsMapEntries(Set<Map.Entry<K, Collection<V>>> set, MapConstraint<? super K, ? super V> mapConstraint) {
            this.entries = set;
            this.constraint = mapConstraint;
        }

        @Override
        protected Set<Map.Entry<K, Collection<V>>> delegate() {
            return this.entries;
        }

        @Override
        public Iterator<Map.Entry<K, Collection<V>>> iterator() {
            final Iterator<Map.Entry<K, Collection<V>>> it = this.entries.iterator();
            return new ForwardingIterator<Map.Entry<K, Collection<V>>>() {
                @Override
                public Map.Entry<K, Collection<V>> next() {
                    return MapConstraints.constrainedAsMapEntry((Map.Entry) it.next(), ConstrainedAsMapEntries.this.constraint);
                }

                @Override
                protected Iterator<Map.Entry<K, Collection<V>>> delegate() {
                    return it;
                }
            };
        }

        @Override
        public Object[] toArray() {
            return standardToArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) standardToArray(tArr);
        }

        @Override
        public boolean contains(Object obj) {
            return Maps.containsEntryImpl(delegate(), obj);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return standardContainsAll(collection);
        }

        @Override
        public boolean equals(Object obj) {
            return standardEquals(obj);
        }

        @Override
        public int hashCode() {
            return standardHashCode();
        }

        @Override
        public boolean remove(Object obj) {
            return Maps.removeEntryImpl(delegate(), obj);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return standardRemoveAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return standardRetainAll(collection);
        }
    }

    private static class ConstrainedListMultimap<K, V> extends ConstrainedMultimap<K, V> implements ListMultimap<K, V> {
        ConstrainedListMultimap(ListMultimap<K, V> listMultimap, MapConstraint<? super K, ? super V> mapConstraint) {
            super(listMultimap, mapConstraint);
        }

        @Override
        public List<V> get(K k) {
            return (List) super.get((Object) k);
        }

        @Override
        public List<V> removeAll(Object obj) {
            return (List) super.removeAll(obj);
        }

        @Override
        public List<V> replaceValues(K k, Iterable<? extends V> iterable) {
            return (List) super.replaceValues((Object) k, (Iterable) iterable);
        }
    }

    private static class ConstrainedSetMultimap<K, V> extends ConstrainedMultimap<K, V> implements SetMultimap<K, V> {
        ConstrainedSetMultimap(SetMultimap<K, V> setMultimap, MapConstraint<? super K, ? super V> mapConstraint) {
            super(setMultimap, mapConstraint);
        }

        @Override
        public Set<V> get(K k) {
            return (Set) super.get((Object) k);
        }

        @Override
        public Set<Map.Entry<K, V>> entries() {
            return (Set) super.entries();
        }

        @Override
        public Set<V> removeAll(Object obj) {
            return (Set) super.removeAll(obj);
        }

        @Override
        public Set<V> replaceValues(K k, Iterable<? extends V> iterable) {
            return (Set) super.replaceValues((Object) k, (Iterable) iterable);
        }
    }

    private static class ConstrainedSortedSetMultimap<K, V> extends ConstrainedSetMultimap<K, V> implements SortedSetMultimap<K, V> {
        ConstrainedSortedSetMultimap(SortedSetMultimap<K, V> sortedSetMultimap, MapConstraint<? super K, ? super V> mapConstraint) {
            super(sortedSetMultimap, mapConstraint);
        }

        @Override
        public SortedSet<V> get(K k) {
            return (SortedSet) super.get((Object) k);
        }

        @Override
        public SortedSet<V> removeAll(Object obj) {
            return (SortedSet) super.removeAll(obj);
        }

        @Override
        public SortedSet<V> replaceValues(K k, Iterable<? extends V> iterable) {
            return (SortedSet) super.replaceValues((Object) k, (Iterable) iterable);
        }

        @Override
        public Comparator<? super V> valueComparator() {
            return ((SortedSetMultimap) delegate()).valueComparator();
        }
    }

    private static <K, V> Collection<V> checkValues(K k, Iterable<? extends V> iterable, MapConstraint<? super K, ? super V> mapConstraint) {
        ArrayList arrayListNewArrayList = Lists.newArrayList(iterable);
        Iterator it = arrayListNewArrayList.iterator();
        while (it.hasNext()) {
            mapConstraint.checkKeyValue(k, (Object) it.next());
        }
        return arrayListNewArrayList;
    }

    private static <K, V> Map<K, V> checkMap(Map<? extends K, ? extends V> map, MapConstraint<? super K, ? super V> mapConstraint) {
        LinkedHashMap linkedHashMap = new LinkedHashMap(map);
        for (Map.Entry<K, V> entry : linkedHashMap.entrySet()) {
            mapConstraint.checkKeyValue(entry.getKey(), entry.getValue());
        }
        return linkedHashMap;
    }
}
