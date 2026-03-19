package android.icu.impl;

import android.icu.util.Freezable;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Relation<K, V> implements Freezable<Relation<K, V>> {
    private Map<K, Set<V>> data;
    volatile boolean frozen;
    Object[] setComparatorParam;
    Constructor<? extends Set<V>> setCreator;

    public static <K, V> Relation<K, V> of(Map<K, Set<V>> map, Class<?> setCreator) {
        return new Relation<>(map, setCreator);
    }

    public static <K, V> Relation<K, V> of(Map<K, Set<V>> map, Class<?> setCreator, Comparator<V> setComparator) {
        return new Relation<>(map, setCreator, setComparator);
    }

    public Relation(Map<K, Set<V>> map, Class<?> setCreator) {
        this(map, setCreator, null);
    }

    public Relation(Map<K, Set<V>> map, Class<?> cls, Comparator<V> comparator) {
        Object[] objArr = null;
        this.frozen = false;
        if (comparator != null) {
            try {
                objArr = new Object[]{comparator};
            } catch (Exception e) {
                throw ((RuntimeException) new IllegalArgumentException("Can't create new set").initCause(e));
            }
        }
        this.setComparatorParam = objArr;
        if (comparator == null) {
            this.setCreator = (Constructor<? extends Set<V>>) cls.getConstructor(new Class[0]);
            this.setCreator.newInstance(this.setComparatorParam);
        } else {
            this.setCreator = (Constructor<? extends Set<V>>) cls.getConstructor(Comparator.class);
            this.setCreator.newInstance(this.setComparatorParam);
        }
        this.data = map == null ? new HashMap<>() : map;
    }

    public void clear() {
        this.data.clear();
    }

    public boolean containsKey(Object key) {
        return this.data.containsKey(key);
    }

    public boolean containsValue(Object value) {
        for (Set<V> values : this.data.values()) {
            if (values.contains(value)) {
                return true;
            }
        }
        return false;
    }

    public final Set<Map.Entry<K, V>> entrySet() {
        return keyValueSet();
    }

    public Set<Map.Entry<K, Set<V>>> keyValuesSet() {
        return this.data.entrySet();
    }

    public Set<Map.Entry<K, V>> keyValueSet() {
        Set<Map.Entry<K, V>> result = new LinkedHashSet<>();
        for (Object obj : this.data.keySet()) {
            Iterator value$iterator = this.data.get(obj).iterator();
            while (value$iterator.hasNext()) {
                result.add(new SimpleEntry<>(obj, value$iterator.next()));
            }
        }
        return result;
    }

    public boolean equals(Object o) {
        if (o != null && o.getClass() == getClass()) {
            return this.data.equals(((Relation) o).data);
        }
        return false;
    }

    public Set<V> getAll(Object key) {
        return this.data.get(key);
    }

    public Set<V> get(Object key) {
        return this.data.get(key);
    }

    public int hashCode() {
        return this.data.hashCode();
    }

    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    public Set<K> keySet() {
        return this.data.keySet();
    }

    public V put(K key, V value) {
        Set<V> set = this.data.get(key);
        if (set == null) {
            Map<K, Set<V>> map = this.data;
            set = newSet();
            map.put(key, set);
        }
        set.add(value);
        return value;
    }

    public V putAll(K key, Collection<? extends V> values) {
        Set<V> set = this.data.get(key);
        if (set == null) {
            Map<K, Set<V>> map = this.data;
            set = newSet();
            map.put(key, set);
        }
        set.addAll(values);
        if (values.size() == 0) {
            return null;
        }
        return values.iterator().next();
    }

    public V putAll(Collection<K> collection, V v) {
        V v2 = null;
        Iterator<T> it = collection.iterator();
        while (it.hasNext()) {
            v2 = (V) put(it.next(), v);
        }
        return v2;
    }

    private Set<V> newSet() {
        try {
            return this.setCreator.newInstance(this.setComparatorParam);
        } catch (Exception e) {
            throw ((RuntimeException) new IllegalArgumentException("Can't create new set").initCause(e));
        }
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        for (Object obj : t.keySet()) {
            put(obj, t.get(obj));
        }
    }

    public void putAll(Relation<? extends K, ? extends V> t) {
        for (Object obj : t.keySet()) {
            Iterator value$iterator = t.getAll(obj).iterator();
            while (value$iterator.hasNext()) {
                put(obj, value$iterator.next());
            }
        }
    }

    public Set<V> removeAll(K key) {
        try {
            return this.data.remove(key);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean remove(K key, V value) {
        try {
            Set<V> set = this.data.get(key);
            if (set == null) {
                return false;
            }
            boolean result = set.remove(value);
            if (set.size() == 0) {
                this.data.remove(key);
            }
            return result;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public int size() {
        return this.data.size();
    }

    public Set<V> values() {
        return (Set) values(new LinkedHashSet());
    }

    public <C extends Collection<V>> C values(C result) {
        for (Map.Entry<K, Set<V>> keyValue : this.data.entrySet()) {
            result.addAll(keyValue.getValue());
        }
        return result;
    }

    public String toString() {
        return this.data.toString();
    }

    static class SimpleEntry<K, V> implements Map.Entry<K, V> {
        K key;
        V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleEntry(Map.Entry<K, V> e) {
            this.key = e.getKey();
            this.value = e.getValue();
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

    public Relation<K, V> addAllInverted(Relation<V, K> source) {
        for (Object obj : source.data.keySet()) {
            Iterator key$iterator = source.data.get(obj).iterator();
            while (key$iterator.hasNext()) {
                put(key$iterator.next(), obj);
            }
        }
        return this;
    }

    public Relation<K, V> addAllInverted(Map<V, K> source) {
        for (Object obj : source.keySet()) {
            put(source.get(obj), obj);
        }
        return this;
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }

    @Override
    public Relation<K, V> freeze() {
        if (!this.frozen) {
            for (Object obj : this.data.keySet()) {
                this.data.put((K) obj, Collections.unmodifiableSet(this.data.get(obj)));
            }
            this.data = Collections.unmodifiableMap(this.data);
            this.frozen = true;
        }
        return this;
    }

    @Override
    public Relation<K, V> cloneAsThawed() {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Relation<K, V> toBeRemoved) {
        boolean result = false;
        for (Object obj : toBeRemoved.keySet()) {
            try {
                Set<V> values = toBeRemoved.getAll(obj);
                if (values != null) {
                    result |= removeAll(obj, values);
                }
            } catch (NullPointerException e) {
            }
        }
        return result;
    }

    public Set<V> removeAll(K... keys) {
        return removeAll((Collection) Arrays.asList(keys));
    }

    public boolean removeAll(K key, Iterable<V> toBeRemoved) {
        boolean result = false;
        for (V value : toBeRemoved) {
            result |= remove(key, value);
        }
        return result;
    }

    public Set<V> removeAll(Collection<K> toBeRemoved) {
        Set<V> result = new LinkedHashSet<>();
        Iterator key$iterator = toBeRemoved.iterator();
        while (key$iterator.hasNext()) {
            try {
                Set<V> removals = this.data.remove(key$iterator.next());
                if (removals != null) {
                    result.addAll(removals);
                }
            } catch (NullPointerException e) {
            }
        }
        return result;
    }
}
