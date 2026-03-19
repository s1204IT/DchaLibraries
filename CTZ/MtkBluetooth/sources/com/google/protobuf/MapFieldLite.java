package com.google.protobuf;

import com.google.protobuf.Internal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MapFieldLite<K, V> implements MutabilityOracle {
    private static final MapFieldLite EMPTY_MAP_FIELD = new MapFieldLite(Collections.emptyMap());
    private boolean isMutable = true;
    private MutatabilityAwareMap<K, V> mapData;

    private MapFieldLite(Map<K, V> map) {
        this.mapData = new MutatabilityAwareMap<>(this, map);
    }

    static {
        EMPTY_MAP_FIELD.makeImmutable();
    }

    public static <K, V> MapFieldLite<K, V> emptyMapField() {
        return EMPTY_MAP_FIELD;
    }

    public static <K, V> MapFieldLite<K, V> newMapField() {
        return new MapFieldLite<>(new LinkedHashMap());
    }

    public Map<K, V> getMap() {
        return Collections.unmodifiableMap(this.mapData);
    }

    public Map<K, V> getMutableMap() {
        return this.mapData;
    }

    public void mergeFrom(MapFieldLite<K, V> mapFieldLite) {
        this.mapData.putAll(copy((Map) mapFieldLite.mapData));
    }

    public void clear() {
        this.mapData.clear();
    }

    private static boolean equals(Object obj, Object obj2) {
        return ((obj instanceof byte[]) && (obj2 instanceof byte[])) ? Arrays.equals((byte[]) obj, (byte[]) obj2) : obj.equals(obj2);
    }

    static <K, V> boolean equals(Map<K, V> map, Map<K, V> map2) {
        if (map == map2) {
            return true;
        }
        if (map.size() != map2.size()) {
            return false;
        }
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!map2.containsKey(entry.getKey()) || !equals(entry.getValue(), map2.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MapFieldLite)) {
            return false;
        }
        return equals((Map) this.mapData, (Map) obj.mapData);
    }

    private static int calculateHashCodeForObject(Object obj) {
        if (obj instanceof byte[]) {
            return Internal.hashCode((byte[]) obj);
        }
        if (obj instanceof Internal.EnumLite) {
            throw new UnsupportedOperationException();
        }
        return obj.hashCode();
    }

    static <K, V> int calculateHashCodeForMap(Map<K, V> map) {
        int iCalculateHashCodeForObject = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            iCalculateHashCodeForObject += calculateHashCodeForObject(entry.getValue()) ^ calculateHashCodeForObject(entry.getKey());
        }
        return iCalculateHashCodeForObject;
    }

    public int hashCode() {
        return calculateHashCodeForMap(this.mapData);
    }

    private static Object copy(Object obj) {
        if (obj instanceof byte[]) {
            return Arrays.copyOf((byte[]) obj, obj.length);
        }
        return obj;
    }

    static <K, V> Map<K, V> copy(Map<K, V> map) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            linkedHashMap.put(entry.getKey(), copy(entry.getValue()));
        }
        return linkedHashMap;
    }

    public MapFieldLite<K, V> copy() {
        return new MapFieldLite<>(copy((Map) this.mapData));
    }

    public void makeImmutable() {
        this.isMutable = false;
    }

    public boolean isMutable() {
        return this.isMutable;
    }

    @Override
    public void ensureMutable() {
        if (!isMutable()) {
            throw new UnsupportedOperationException();
        }
    }

    static class MutatabilityAwareMap<K, V> implements Map<K, V> {
        private final Map<K, V> delegate;
        private final MutabilityOracle mutabilityOracle;

        MutatabilityAwareMap(MutabilityOracle mutabilityOracle, Map<K, V> map) {
            this.mutabilityOracle = mutabilityOracle;
            this.delegate = map;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return this.delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.delegate.containsKey(obj);
        }

        @Override
        public boolean containsValue(Object obj) {
            return this.delegate.containsValue(obj);
        }

        @Override
        public V get(Object obj) {
            return this.delegate.get(obj);
        }

        @Override
        public V put(K k, V v) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.put(k, v);
        }

        @Override
        public V remove(Object obj) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.remove(obj);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            this.mutabilityOracle.ensureMutable();
            this.delegate.putAll(map);
        }

        @Override
        public void clear() {
            this.mutabilityOracle.ensureMutable();
            this.delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return new MutatabilityAwareSet(this.mutabilityOracle, this.delegate.keySet());
        }

        @Override
        public Collection<V> values() {
            return new MutatabilityAwareCollection(this.mutabilityOracle, this.delegate.values());
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new MutatabilityAwareSet(this.mutabilityOracle, this.delegate.entrySet());
        }

        @Override
        public boolean equals(Object obj) {
            return this.delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.delegate.hashCode();
        }

        public String toString() {
            return this.delegate.toString();
        }
    }

    private static class MutatabilityAwareCollection<E> implements Collection<E> {
        private final Collection<E> delegate;
        private final MutabilityOracle mutabilityOracle;

        MutatabilityAwareCollection(MutabilityOracle mutabilityOracle, Collection<E> collection) {
            this.mutabilityOracle = mutabilityOracle;
            this.delegate = collection;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return this.delegate.isEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            return this.delegate.contains(obj);
        }

        @Override
        public Iterator<E> iterator() {
            return new MutatabilityAwareIterator(this.mutabilityOracle, this.delegate.iterator());
        }

        @Override
        public Object[] toArray() {
            return this.delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.delegate.toArray(tArr);
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.remove(obj);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.delegate.containsAll(collection);
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.retainAll(collection);
        }

        @Override
        public void clear() {
            this.mutabilityOracle.ensureMutable();
            this.delegate.clear();
        }

        @Override
        public boolean equals(Object obj) {
            return this.delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.delegate.hashCode();
        }

        public String toString() {
            return this.delegate.toString();
        }
    }

    private static class MutatabilityAwareSet<E> implements Set<E> {
        private final Set<E> delegate;
        private final MutabilityOracle mutabilityOracle;

        MutatabilityAwareSet(MutabilityOracle mutabilityOracle, Set<E> set) {
            this.mutabilityOracle = mutabilityOracle;
            this.delegate = set;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return this.delegate.isEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            return this.delegate.contains(obj);
        }

        @Override
        public Iterator<E> iterator() {
            return new MutatabilityAwareIterator(this.mutabilityOracle, this.delegate.iterator());
        }

        @Override
        public Object[] toArray() {
            return this.delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.delegate.toArray(tArr);
        }

        @Override
        public boolean add(E e) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.add(e);
        }

        @Override
        public boolean remove(Object obj) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.remove(obj);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.delegate.containsAll(collection);
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.addAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.retainAll(collection);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            this.mutabilityOracle.ensureMutable();
            return this.delegate.removeAll(collection);
        }

        @Override
        public void clear() {
            this.mutabilityOracle.ensureMutable();
            this.delegate.clear();
        }

        @Override
        public boolean equals(Object obj) {
            return this.delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.delegate.hashCode();
        }

        public String toString() {
            return this.delegate.toString();
        }
    }

    private static class MutatabilityAwareIterator<E> implements Iterator<E> {
        private final Iterator<E> delegate;
        private final MutabilityOracle mutabilityOracle;

        MutatabilityAwareIterator(MutabilityOracle mutabilityOracle, Iterator<E> it) {
            this.mutabilityOracle = mutabilityOracle;
            this.delegate = it;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public E next() {
            return this.delegate.next();
        }

        @Override
        public void remove() {
            this.mutabilityOracle.ensureMutable();
            this.delegate.remove();
        }

        public boolean equals(Object obj) {
            return this.delegate.equals(obj);
        }

        public int hashCode() {
            return this.delegate.hashCode();
        }

        public String toString() {
            return this.delegate.toString();
        }
    }
}
