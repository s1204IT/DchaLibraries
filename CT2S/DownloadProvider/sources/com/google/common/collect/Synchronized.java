package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class Synchronized {

    static class SynchronizedObject implements Serializable {
        private static final long serialVersionUID = 0;
        final Object delegate;
        final Object mutex;

        SynchronizedObject(Object delegate, Object mutex) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.mutex = mutex == null ? this : mutex;
        }

        Object delegate() {
            return this.delegate;
        }

        public String toString() {
            String string;
            synchronized (this.mutex) {
                string = this.delegate.toString();
            }
            return string;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }
    }

    private static <E> Collection<E> collection(Collection<E> collection, Object mutex) {
        return new SynchronizedCollection(collection, mutex);
    }

    static class SynchronizedCollection<E> extends SynchronizedObject implements Collection<E> {
        private static final long serialVersionUID = 0;

        private SynchronizedCollection(Collection<E> delegate, Object mutex) {
            super(delegate, mutex);
        }

        @Override
        Collection<E> delegate() {
            return (Collection) super.delegate();
        }

        @Override
        public boolean add(E e) {
            boolean zAdd;
            synchronized (this.mutex) {
                zAdd = delegate().add(e);
            }
            return zAdd;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean zAddAll;
            synchronized (this.mutex) {
                zAddAll = delegate().addAll(c);
            }
            return zAddAll;
        }

        @Override
        public void clear() {
            synchronized (this.mutex) {
                delegate().clear();
            }
        }

        @Override
        public boolean contains(Object o) {
            boolean zContains;
            synchronized (this.mutex) {
                zContains = delegate().contains(o);
            }
            return zContains;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            boolean zContainsAll;
            synchronized (this.mutex) {
                zContainsAll = delegate().containsAll(c);
            }
            return zContainsAll;
        }

        @Override
        public boolean isEmpty() {
            boolean zIsEmpty;
            synchronized (this.mutex) {
                zIsEmpty = delegate().isEmpty();
            }
            return zIsEmpty;
        }

        @Override
        public Iterator<E> iterator() {
            return delegate().iterator();
        }

        @Override
        public boolean remove(Object o) {
            boolean zRemove;
            synchronized (this.mutex) {
                zRemove = delegate().remove(o);
            }
            return zRemove;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean zRemoveAll;
            synchronized (this.mutex) {
                zRemoveAll = delegate().removeAll(c);
            }
            return zRemoveAll;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            boolean zRetainAll;
            synchronized (this.mutex) {
                zRetainAll = delegate().retainAll(c);
            }
            return zRetainAll;
        }

        @Override
        public int size() {
            int size;
            synchronized (this.mutex) {
                size = delegate().size();
            }
            return size;
        }

        @Override
        public Object[] toArray() {
            Object[] array;
            synchronized (this.mutex) {
                array = delegate().toArray();
            }
            return array;
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            T[] tArr2;
            synchronized (this.mutex) {
                tArr2 = (T[]) delegate().toArray(tArr);
            }
            return tArr2;
        }
    }

    static <E> Set<E> set(Set<E> set, Object mutex) {
        return new SynchronizedSet(set, mutex);
    }

    static class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E> {
        private static final long serialVersionUID = 0;

        SynchronizedSet(Set<E> delegate, Object mutex) {
            super(delegate, mutex);
        }

        @Override
        Set<E> delegate() {
            return (Set) super.delegate();
        }

        @Override
        public boolean equals(Object o) {
            boolean zEquals;
            if (o == this) {
                return true;
            }
            synchronized (this.mutex) {
                zEquals = delegate().equals(o);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = delegate().hashCode();
            }
            return iHashCode;
        }
    }

    static <K, V> Map<K, V> map(Map<K, V> map, Object mutex) {
        return new SynchronizedMap(map, mutex);
    }

    private static class SynchronizedMap<K, V> extends SynchronizedObject implements Map<K, V> {
        private static final long serialVersionUID = 0;
        transient Set<Map.Entry<K, V>> entrySet;
        transient Set<K> keySet;
        transient Collection<V> values;

        SynchronizedMap(Map<K, V> delegate, Object mutex) {
            super(delegate, mutex);
        }

        @Override
        Map<K, V> delegate() {
            return (Map) super.delegate();
        }

        @Override
        public void clear() {
            synchronized (this.mutex) {
                delegate().clear();
            }
        }

        @Override
        public boolean containsKey(Object key) {
            boolean zContainsKey;
            synchronized (this.mutex) {
                zContainsKey = delegate().containsKey(key);
            }
            return zContainsKey;
        }

        @Override
        public boolean containsValue(Object value) {
            boolean zContainsValue;
            synchronized (this.mutex) {
                zContainsValue = delegate().containsValue(value);
            }
            return zContainsValue;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K, V>> set;
            synchronized (this.mutex) {
                if (this.entrySet == null) {
                    this.entrySet = Synchronized.set(delegate().entrySet(), this.mutex);
                }
                set = this.entrySet;
            }
            return set;
        }

        @Override
        public V get(Object key) {
            V v;
            synchronized (this.mutex) {
                v = delegate().get(key);
            }
            return v;
        }

        @Override
        public boolean isEmpty() {
            boolean zIsEmpty;
            synchronized (this.mutex) {
                zIsEmpty = delegate().isEmpty();
            }
            return zIsEmpty;
        }

        @Override
        public Set<K> keySet() {
            Set<K> set;
            synchronized (this.mutex) {
                if (this.keySet == null) {
                    this.keySet = Synchronized.set(delegate().keySet(), this.mutex);
                }
                set = this.keySet;
            }
            return set;
        }

        @Override
        public V put(K key, V value) {
            V vPut;
            synchronized (this.mutex) {
                vPut = delegate().put(key, value);
            }
            return vPut;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            synchronized (this.mutex) {
                delegate().putAll(map);
            }
        }

        @Override
        public V remove(Object key) {
            V vRemove;
            synchronized (this.mutex) {
                vRemove = delegate().remove(key);
            }
            return vRemove;
        }

        @Override
        public int size() {
            int size;
            synchronized (this.mutex) {
                size = delegate().size();
            }
            return size;
        }

        public Collection<V> values() {
            Collection<V> collection;
            synchronized (this.mutex) {
                if (this.values == null) {
                    this.values = Synchronized.collection(delegate().values(), this.mutex);
                }
                collection = this.values;
            }
            return collection;
        }

        @Override
        public boolean equals(Object o) {
            boolean zEquals;
            if (o == this) {
                return true;
            }
            synchronized (this.mutex) {
                zEquals = delegate().equals(o);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = delegate().hashCode();
            }
            return iHashCode;
        }
    }

    static class SynchronizedBiMap<K, V> extends SynchronizedMap<K, V> implements BiMap<K, V>, Serializable {
        private static final long serialVersionUID = 0;
        private transient Set<V> valueSet;

        @Override
        BiMap<K, V> delegate() {
            return (BiMap) super.delegate();
        }

        @Override
        public Set<V> values() {
            Set<V> set;
            synchronized (this.mutex) {
                if (this.valueSet == null) {
                    this.valueSet = Synchronized.set(delegate().values(), this.mutex);
                }
                set = this.valueSet;
            }
            return set;
        }
    }
}
