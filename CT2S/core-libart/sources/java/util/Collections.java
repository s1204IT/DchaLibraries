package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Map;

public class Collections {
    public static final List EMPTY_LIST;
    public static final Map EMPTY_MAP;
    public static final Set EMPTY_SET;
    private static final Iterator<?> EMPTY_ITERATOR = new Iterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new IllegalStateException();
        }
    };
    private static final Enumeration<?> EMPTY_ENUMERATION = new Enumeration<Object>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public Object nextElement() {
            throw new NoSuchElementException();
        }
    };

    static {
        EMPTY_LIST = new EmptyList();
        EMPTY_SET = new EmptySet();
        EMPTY_MAP = new EmptyMap();
    }

    private static final class CopiesList<E> extends AbstractList<E> implements Serializable {
        private static final long serialVersionUID = 2739099268398711800L;
        private final E element;
        private final int n;

        CopiesList(int length, E object) {
            if (length < 0) {
                throw new IllegalArgumentException("length < 0: " + length);
            }
            this.n = length;
            this.element = object;
        }

        @Override
        public boolean contains(Object object) {
            return this.element == null ? object == null : this.element.equals(object);
        }

        @Override
        public int size() {
            return this.n;
        }

        @Override
        public E get(int location) {
            if (location >= 0 && location < this.n) {
                return this.element;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    private static final class EmptyList extends AbstractList implements RandomAccess, Serializable {
        private static final long serialVersionUID = 8842843931221139166L;

        private EmptyList() {
        }

        @Override
        public boolean contains(Object object) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Object get(int location) {
            throw new IndexOutOfBoundsException();
        }

        private Object readResolve() {
            return Collections.EMPTY_LIST;
        }
    }

    private static final class EmptySet extends AbstractSet implements Serializable {
        private static final long serialVersionUID = 1582296315990362920L;

        private EmptySet() {
        }

        @Override
        public boolean contains(Object object) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator iterator() {
            return Collections.EMPTY_ITERATOR;
        }

        private Object readResolve() {
            return Collections.EMPTY_SET;
        }
    }

    private static final class EmptyMap extends AbstractMap implements Serializable {
        private static final long serialVersionUID = 6428348081105594320L;

        private EmptyMap() {
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Set entrySet() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Set keySet() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Collection values() {
            return Collections.EMPTY_LIST;
        }

        private Object readResolve() {
            return Collections.EMPTY_MAP;
        }
    }

    private static final class ReverseComparator<T> implements Comparator<T>, Serializable {
        private static final ReverseComparator<Object> INSTANCE = new ReverseComparator<>();
        private static final long serialVersionUID = 7207038068494060240L;

        private ReverseComparator() {
        }

        @Override
        public int compare(T o1, T o2) {
            Comparable<T> c2 = (Comparable) o2;
            return c2.compareTo(o1);
        }

        private Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    private static final class ReverseComparator2<T> implements Comparator<T>, Serializable {
        private static final long serialVersionUID = 4374092139857L;
        private final Comparator<T> cmp;

        ReverseComparator2(Comparator<T> comparator) {
            this.cmp = comparator;
        }

        @Override
        public int compare(T o1, T o2) {
            return this.cmp.compare(o2, o1);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof ReverseComparator2) && ((ReverseComparator2) o).cmp.equals(this.cmp);
        }

        public int hashCode() {
            return this.cmp.hashCode() ^ (-1);
        }
    }

    private static final class SingletonSet<E> extends AbstractSet<E> implements Serializable {
        private static final long serialVersionUID = 3193687207550431679L;
        final E element;

        SingletonSet(E object) {
            this.element = object;
        }

        @Override
        public boolean contains(Object object) {
            return this.element == null ? object == null : this.element.equals(object);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                boolean hasNext = true;

                @Override
                public boolean hasNext() {
                    return this.hasNext;
                }

                @Override
                public E next() {
                    if (this.hasNext) {
                        this.hasNext = false;
                        return SingletonSet.this.element;
                    }
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private static final class SingletonList<E> extends AbstractList<E> implements Serializable {
        private static final long serialVersionUID = 3093736618740652951L;
        final E element;

        SingletonList(E object) {
            this.element = object;
        }

        @Override
        public boolean contains(Object object) {
            return this.element == null ? object == null : this.element.equals(object);
        }

        @Override
        public E get(int location) {
            if (location == 0) {
                return this.element;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    private static final class SingletonMap<K, V> extends AbstractMap<K, V> implements Serializable {
        private static final long serialVersionUID = -6979724477215052911L;
        final K k;
        final V v;

        SingletonMap(K key, V value) {
            this.k = key;
            this.v = value;
        }

        @Override
        public boolean containsKey(Object key) {
            return this.k == null ? key == null : this.k.equals(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return this.v == null ? value == null : this.v.equals(value);
        }

        @Override
        public V get(Object key) {
            if (containsKey(key)) {
                return this.v;
            }
            return null;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<Map.Entry<K, V>>() {
                @Override
                public boolean contains(Object object) {
                    if (!(object instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry<?, ?> entry = (Map.Entry) object;
                    return SingletonMap.this.containsKey(entry.getKey()) && SingletonMap.this.containsValue(entry.getValue());
                }

                @Override
                public int size() {
                    return 1;
                }

                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return new Iterator<Map.Entry<K, V>>() {
                        boolean hasNext = true;

                        @Override
                        public boolean hasNext() {
                            return this.hasNext;
                        }

                        @Override
                        public Map.Entry<K, V> next() {
                            if (!this.hasNext) {
                                throw new NoSuchElementException();
                            }
                            this.hasNext = false;
                            return new MapEntry<K, V>(SingletonMap.this.k, SingletonMap.this.v) {
                                @Override
                                public V setValue(V value) {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }
    }

    static class SynchronizedCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 3053995032091335093L;
        final Collection<E> c;
        final Object mutex;

        SynchronizedCollection(Collection<E> collection) {
            this.c = collection;
            this.mutex = this;
        }

        SynchronizedCollection(Collection<E> collection, Object mutex) {
            this.c = collection;
            this.mutex = mutex;
        }

        @Override
        public boolean add(E object) {
            boolean zAdd;
            synchronized (this.mutex) {
                zAdd = this.c.add(object);
            }
            return zAdd;
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            boolean zAddAll;
            synchronized (this.mutex) {
                zAddAll = this.c.addAll(collection);
            }
            return zAddAll;
        }

        @Override
        public void clear() {
            synchronized (this.mutex) {
                this.c.clear();
            }
        }

        @Override
        public boolean contains(Object object) {
            boolean zContains;
            synchronized (this.mutex) {
                zContains = this.c.contains(object);
            }
            return zContains;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            boolean zContainsAll;
            synchronized (this.mutex) {
                zContainsAll = this.c.containsAll(collection);
            }
            return zContainsAll;
        }

        @Override
        public boolean isEmpty() {
            boolean zIsEmpty;
            synchronized (this.mutex) {
                zIsEmpty = this.c.isEmpty();
            }
            return zIsEmpty;
        }

        @Override
        public Iterator<E> iterator() {
            Iterator<E> it;
            synchronized (this.mutex) {
                it = this.c.iterator();
            }
            return it;
        }

        @Override
        public boolean remove(Object object) {
            boolean zRemove;
            synchronized (this.mutex) {
                zRemove = this.c.remove(object);
            }
            return zRemove;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean zRemoveAll;
            synchronized (this.mutex) {
                zRemoveAll = this.c.removeAll(collection);
            }
            return zRemoveAll;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean zRetainAll;
            synchronized (this.mutex) {
                zRetainAll = this.c.retainAll(collection);
            }
            return zRetainAll;
        }

        @Override
        public int size() {
            int size;
            synchronized (this.mutex) {
                size = this.c.size();
            }
            return size;
        }

        @Override
        public Object[] toArray() {
            Object[] array;
            synchronized (this.mutex) {
                array = this.c.toArray();
            }
            return array;
        }

        public String toString() {
            String string;
            synchronized (this.mutex) {
                string = this.c.toString();
            }
            return string;
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            T[] tArr2;
            synchronized (this.mutex) {
                tArr2 = (T[]) this.c.toArray(tArr);
            }
            return tArr2;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }
    }

    static class SynchronizedRandomAccessList<E> extends SynchronizedList<E> implements RandomAccess {
        private static final long serialVersionUID = 1530674583602358482L;

        SynchronizedRandomAccessList(List<E> l) {
            super(l);
        }

        SynchronizedRandomAccessList(List<E> l, Object mutex) {
            super(l, mutex);
        }

        @Override
        public List<E> subList(int start, int end) {
            SynchronizedRandomAccessList synchronizedRandomAccessList;
            synchronized (this.mutex) {
                synchronizedRandomAccessList = new SynchronizedRandomAccessList(this.list.subList(start, end), this.mutex);
            }
            return synchronizedRandomAccessList;
        }

        private Object writeReplace() {
            return new SynchronizedList(this.list);
        }
    }

    static class SynchronizedList<E> extends SynchronizedCollection<E> implements List<E> {
        private static final long serialVersionUID = -7754090372962971524L;
        final List<E> list;

        SynchronizedList(List<E> l) {
            super(l);
            this.list = l;
        }

        SynchronizedList(List<E> l, Object mutex) {
            super(l, mutex);
            this.list = l;
        }

        @Override
        public void add(int location, E object) {
            synchronized (this.mutex) {
                this.list.add(location, object);
            }
        }

        @Override
        public boolean addAll(int location, Collection<? extends E> collection) {
            boolean zAddAll;
            synchronized (this.mutex) {
                zAddAll = this.list.addAll(location, collection);
            }
            return zAddAll;
        }

        @Override
        public boolean equals(Object object) {
            boolean zEquals;
            synchronized (this.mutex) {
                zEquals = this.list.equals(object);
            }
            return zEquals;
        }

        @Override
        public E get(int location) {
            E e;
            synchronized (this.mutex) {
                e = this.list.get(location);
            }
            return e;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = this.list.hashCode();
            }
            return iHashCode;
        }

        @Override
        public int indexOf(Object object) {
            int size;
            Object[] array;
            synchronized (this.mutex) {
                size = this.list.size();
                array = new Object[size];
                this.list.toArray(array);
            }
            if (object != null) {
                for (int i = 0; i < size; i++) {
                    if (object.equals(array[i])) {
                        return i;
                    }
                }
            } else {
                for (int i2 = 0; i2 < size; i2++) {
                    if (array[i2] == null) {
                        return i2;
                    }
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object object) {
            int size;
            Object[] array;
            synchronized (this.mutex) {
                size = this.list.size();
                array = new Object[size];
                this.list.toArray(array);
            }
            if (object != null) {
                for (int i = size - 1; i >= 0; i--) {
                    if (object.equals(array[i])) {
                        return i;
                    }
                }
            } else {
                for (int i2 = size - 1; i2 >= 0; i2--) {
                    if (array[i2] == null) {
                        return i2;
                    }
                }
            }
            return -1;
        }

        @Override
        public ListIterator<E> listIterator() {
            ListIterator<E> listIterator;
            synchronized (this.mutex) {
                listIterator = this.list.listIterator();
            }
            return listIterator;
        }

        @Override
        public ListIterator<E> listIterator(int location) {
            ListIterator<E> listIterator;
            synchronized (this.mutex) {
                listIterator = this.list.listIterator(location);
            }
            return listIterator;
        }

        @Override
        public E remove(int location) {
            E eRemove;
            synchronized (this.mutex) {
                eRemove = this.list.remove(location);
            }
            return eRemove;
        }

        @Override
        public E set(int location, E object) {
            E e;
            synchronized (this.mutex) {
                e = this.list.set(location, object);
            }
            return e;
        }

        @Override
        public List<E> subList(int start, int end) {
            SynchronizedList synchronizedList;
            synchronized (this.mutex) {
                synchronizedList = new SynchronizedList(this.list.subList(start, end), this.mutex);
            }
            return synchronizedList;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }

        private Object readResolve() {
            if (this.list instanceof RandomAccess) {
                return new SynchronizedRandomAccessList<>(this.list, this.mutex);
            }
            return this;
        }
    }

    static class SynchronizedMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = 1978198479659022715L;
        private final Map<K, V> m;
        final Object mutex;

        SynchronizedMap(Map<K, V> map) {
            this.m = map;
            this.mutex = this;
        }

        SynchronizedMap(Map<K, V> map, Object mutex) {
            this.m = map;
            this.mutex = mutex;
        }

        @Override
        public void clear() {
            synchronized (this.mutex) {
                this.m.clear();
            }
        }

        @Override
        public boolean containsKey(Object key) {
            boolean zContainsKey;
            synchronized (this.mutex) {
                zContainsKey = this.m.containsKey(key);
            }
            return zContainsKey;
        }

        @Override
        public boolean containsValue(Object value) {
            boolean zContainsValue;
            synchronized (this.mutex) {
                zContainsValue = this.m.containsValue(value);
            }
            return zContainsValue;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            SynchronizedSet synchronizedSet;
            synchronized (this.mutex) {
                synchronizedSet = new SynchronizedSet(this.m.entrySet(), this.mutex);
            }
            return synchronizedSet;
        }

        @Override
        public boolean equals(Object object) {
            boolean zEquals;
            synchronized (this.mutex) {
                zEquals = this.m.equals(object);
            }
            return zEquals;
        }

        @Override
        public V get(Object key) {
            V v;
            synchronized (this.mutex) {
                v = this.m.get(key);
            }
            return v;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = this.m.hashCode();
            }
            return iHashCode;
        }

        @Override
        public boolean isEmpty() {
            boolean zIsEmpty;
            synchronized (this.mutex) {
                zIsEmpty = this.m.isEmpty();
            }
            return zIsEmpty;
        }

        @Override
        public Set<K> keySet() {
            SynchronizedSet synchronizedSet;
            synchronized (this.mutex) {
                synchronizedSet = new SynchronizedSet(this.m.keySet(), this.mutex);
            }
            return synchronizedSet;
        }

        @Override
        public V put(K key, V value) {
            V vPut;
            synchronized (this.mutex) {
                vPut = this.m.put(key, value);
            }
            return vPut;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            synchronized (this.mutex) {
                this.m.putAll(map);
            }
        }

        @Override
        public V remove(Object key) {
            V vRemove;
            synchronized (this.mutex) {
                vRemove = this.m.remove(key);
            }
            return vRemove;
        }

        @Override
        public int size() {
            int size;
            synchronized (this.mutex) {
                size = this.m.size();
            }
            return size;
        }

        @Override
        public Collection<V> values() {
            SynchronizedCollection synchronizedCollection;
            synchronized (this.mutex) {
                synchronizedCollection = new SynchronizedCollection(this.m.values(), this.mutex);
            }
            return synchronizedCollection;
        }

        public String toString() {
            String string;
            synchronized (this.mutex) {
                string = this.m.toString();
            }
            return string;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }
    }

    static class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E> {
        private static final long serialVersionUID = 487447009682186044L;

        SynchronizedSet(Set<E> set) {
            super(set);
        }

        SynchronizedSet(Set<E> set, Object mutex) {
            super(set, mutex);
        }

        @Override
        public boolean equals(Object object) {
            boolean zEquals;
            synchronized (this.mutex) {
                zEquals = this.c.equals(object);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = this.c.hashCode();
            }
            return iHashCode;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }
    }

    static class SynchronizedSortedMap<K, V> extends SynchronizedMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = -8798146769416483793L;
        private final SortedMap<K, V> sm;

        SynchronizedSortedMap(SortedMap<K, V> map) {
            super(map);
            this.sm = map;
        }

        SynchronizedSortedMap(SortedMap<K, V> map, Object mutex) {
            super(map, mutex);
            this.sm = map;
        }

        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> comparator;
            synchronized (this.mutex) {
                comparator = this.sm.comparator();
            }
            return comparator;
        }

        @Override
        public K firstKey() {
            K kFirstKey;
            synchronized (this.mutex) {
                kFirstKey = this.sm.firstKey();
            }
            return kFirstKey;
        }

        @Override
        public SortedMap<K, V> headMap(K endKey) {
            SynchronizedSortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.headMap(endKey), this.mutex);
            }
            return synchronizedSortedMap;
        }

        @Override
        public K lastKey() {
            K kLastKey;
            synchronized (this.mutex) {
                kLastKey = this.sm.lastKey();
            }
            return kLastKey;
        }

        @Override
        public SortedMap<K, V> subMap(K startKey, K endKey) {
            SynchronizedSortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.subMap(startKey, endKey), this.mutex);
            }
            return synchronizedSortedMap;
        }

        @Override
        public SortedMap<K, V> tailMap(K startKey) {
            SynchronizedSortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.tailMap(startKey), this.mutex);
            }
            return synchronizedSortedMap;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }
    }

    static class SynchronizedSortedSet<E> extends SynchronizedSet<E> implements SortedSet<E> {
        private static final long serialVersionUID = 8695801310862127406L;
        private final SortedSet<E> ss;

        SynchronizedSortedSet(SortedSet<E> set) {
            super(set);
            this.ss = set;
        }

        SynchronizedSortedSet(SortedSet<E> set, Object mutex) {
            super(set, mutex);
            this.ss = set;
        }

        @Override
        public Comparator<? super E> comparator() {
            Comparator<? super E> comparator;
            synchronized (this.mutex) {
                comparator = this.ss.comparator();
            }
            return comparator;
        }

        @Override
        public E first() {
            E eFirst;
            synchronized (this.mutex) {
                eFirst = this.ss.first();
            }
            return eFirst;
        }

        @Override
        public SortedSet<E> headSet(E end) {
            SynchronizedSortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.headSet(end), this.mutex);
            }
            return synchronizedSortedSet;
        }

        @Override
        public E last() {
            E eLast;
            synchronized (this.mutex) {
                eLast = this.ss.last();
            }
            return eLast;
        }

        @Override
        public SortedSet<E> subSet(E start, E end) {
            SynchronizedSortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.subSet(start, end), this.mutex);
            }
            return synchronizedSortedSet;
        }

        @Override
        public SortedSet<E> tailSet(E start) {
            SynchronizedSortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.tailSet(start), this.mutex);
            }
            return synchronizedSortedSet;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }
    }

    private static class UnmodifiableCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 1820017752578914078L;
        final Collection<E> c;

        UnmodifiableCollection(Collection<E> collection) {
            this.c = collection;
        }

        @Override
        public boolean add(E object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object object) {
            return this.c.contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.c.containsAll(collection);
        }

        @Override
        public boolean isEmpty() {
            return this.c.isEmpty();
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                Iterator<E> iterator;

                {
                    this.iterator = UnmodifiableCollection.this.c.iterator();
                }

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public E next() {
                    return this.iterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return this.c.size();
        }

        @Override
        public Object[] toArray() {
            return this.c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.c.toArray(tArr);
        }

        public String toString() {
            return this.c.toString();
        }
    }

    private static class UnmodifiableRandomAccessList<E> extends UnmodifiableList<E> implements RandomAccess {
        private static final long serialVersionUID = -2542308836966382001L;

        UnmodifiableRandomAccessList(List<E> l) {
            super(l);
        }

        @Override
        public List<E> subList(int start, int end) {
            return new UnmodifiableRandomAccessList(this.list.subList(start, end));
        }

        private Object writeReplace() {
            return new UnmodifiableList(this.list);
        }
    }

    private static class UnmodifiableList<E> extends UnmodifiableCollection<E> implements List<E> {
        private static final long serialVersionUID = -283967356065247728L;
        final List<E> list;

        UnmodifiableList(List<E> l) {
            super(l);
            this.list = l;
        }

        @Override
        public void add(int location, E object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int location, Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object object) {
            return this.list.equals(object);
        }

        @Override
        public E get(int location) {
            return this.list.get(location);
        }

        @Override
        public int hashCode() {
            return this.list.hashCode();
        }

        @Override
        public int indexOf(Object object) {
            return this.list.indexOf(object);
        }

        @Override
        public int lastIndexOf(Object object) {
            return this.list.lastIndexOf(object);
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(final int location) {
            return new ListIterator<E>() {
                ListIterator<E> iterator;

                {
                    this.iterator = UnmodifiableList.this.list.listIterator(location);
                }

                @Override
                public void add(E object) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public boolean hasPrevious() {
                    return this.iterator.hasPrevious();
                }

                @Override
                public E next() {
                    return this.iterator.next();
                }

                @Override
                public int nextIndex() {
                    return this.iterator.nextIndex();
                }

                @Override
                public E previous() {
                    return this.iterator.previous();
                }

                @Override
                public int previousIndex() {
                    return this.iterator.previousIndex();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(E object) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public E remove(int location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public E set(int location, E object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<E> subList(int start, int end) {
            return new UnmodifiableList(this.list.subList(start, end));
        }

        private Object readResolve() {
            if (this.list instanceof RandomAccess) {
                return new UnmodifiableRandomAccessList<>(this.list);
            }
            return this;
        }
    }

    private static class UnmodifiableMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = -1034234728574286014L;
        private final Map<K, V> m;

        private static class UnmodifiableEntrySet<K, V> extends UnmodifiableSet<Map.Entry<K, V>> {
            private static final long serialVersionUID = 7854390611657943733L;

            private static class UnmodifiableMapEntry<K, V> implements Map.Entry<K, V> {
                Map.Entry<K, V> mapEntry;

                UnmodifiableMapEntry(Map.Entry<K, V> entry) {
                    this.mapEntry = entry;
                }

                @Override
                public boolean equals(Object object) {
                    return this.mapEntry.equals(object);
                }

                @Override
                public K getKey() {
                    return this.mapEntry.getKey();
                }

                @Override
                public V getValue() {
                    return this.mapEntry.getValue();
                }

                @Override
                public int hashCode() {
                    return this.mapEntry.hashCode();
                }

                @Override
                public V setValue(V object) {
                    throw new UnsupportedOperationException();
                }

                public String toString() {
                    return this.mapEntry.toString();
                }
            }

            UnmodifiableEntrySet(Set<Map.Entry<K, V>> set) {
                super(set);
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    Iterator<Map.Entry<K, V>> iterator;

                    {
                        this.iterator = UnmodifiableEntrySet.this.c.iterator();
                    }

                    @Override
                    public boolean hasNext() {
                        return this.iterator.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        return new UnmodifiableMapEntry(this.iterator.next());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public Object[] toArray() {
                int length = this.c.size();
                Object[] result = new Object[length];
                Iterator<?> it = iterator();
                for (int i = 0; i < length; i++) {
                    result[i] = it.next();
                }
                return result;
            }

            @Override
            public <T> T[] toArray(T[] tArr) {
                int i;
                int size = this.c.size();
                Iterator<Map.Entry<K, V>> it = iterator();
                if (size > tArr.length) {
                    tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size));
                    i = 0;
                } else {
                    i = 0;
                }
                while (i < size) {
                    tArr[i] = it.next();
                    i++;
                }
                if (i < tArr.length) {
                    tArr[i] = null;
                }
                return tArr;
            }
        }

        UnmodifiableMap(Map<K, V> map) {
            this.m = map;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            return this.m.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return this.m.containsValue(value);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new UnmodifiableEntrySet(this.m.entrySet());
        }

        @Override
        public boolean equals(Object object) {
            return this.m.equals(object);
        }

        @Override
        public V get(Object key) {
            return this.m.get(key);
        }

        @Override
        public int hashCode() {
            return this.m.hashCode();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public Set<K> keySet() {
            return new UnmodifiableSet(this.m.keySet());
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public Collection<V> values() {
            return new UnmodifiableCollection(this.m.values());
        }

        public String toString() {
            return this.m.toString();
        }
    }

    private static class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E> {
        private static final long serialVersionUID = -9215047833775013803L;

        UnmodifiableSet(Set<E> set) {
            super(set);
        }

        @Override
        public boolean equals(Object object) {
            return this.c.equals(object);
        }

        @Override
        public int hashCode() {
            return this.c.hashCode();
        }
    }

    private static class UnmodifiableSortedMap<K, V> extends UnmodifiableMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = -8806743815996713206L;
        private final SortedMap<K, V> sm;

        UnmodifiableSortedMap(SortedMap<K, V> map) {
            super(map);
            this.sm = map;
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.sm.comparator();
        }

        @Override
        public K firstKey() {
            return this.sm.firstKey();
        }

        @Override
        public SortedMap<K, V> headMap(K before) {
            return new UnmodifiableSortedMap(this.sm.headMap(before));
        }

        @Override
        public K lastKey() {
            return this.sm.lastKey();
        }

        @Override
        public SortedMap<K, V> subMap(K start, K end) {
            return new UnmodifiableSortedMap(this.sm.subMap(start, end));
        }

        @Override
        public SortedMap<K, V> tailMap(K after) {
            return new UnmodifiableSortedMap(this.sm.tailMap(after));
        }
    }

    private static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> implements SortedSet<E> {
        private static final long serialVersionUID = -4929149591599911165L;
        private final SortedSet<E> ss;

        UnmodifiableSortedSet(SortedSet<E> set) {
            super(set);
            this.ss = set;
        }

        @Override
        public Comparator<? super E> comparator() {
            return this.ss.comparator();
        }

        @Override
        public E first() {
            return this.ss.first();
        }

        @Override
        public SortedSet<E> headSet(E before) {
            return new UnmodifiableSortedSet(this.ss.headSet(before));
        }

        @Override
        public E last() {
            return this.ss.last();
        }

        @Override
        public SortedSet<E> subSet(E start, E end) {
            return new UnmodifiableSortedSet(this.ss.subSet(start, end));
        }

        @Override
        public SortedSet<E> tailSet(E after) {
            return new UnmodifiableSortedSet(this.ss.tailSet(after));
        }
    }

    private Collections() {
    }

    public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T object) {
        if (list == null) {
            throw new NullPointerException("list == null");
        }
        if (list.isEmpty()) {
            return -1;
        }
        if (!(list instanceof RandomAccess)) {
            ListIterator<? extends Comparable<? super T>> it = list.listIterator();
            while (it.hasNext()) {
                int result = -it.next().compareTo(object);
                if (result <= 0) {
                    if (result == 0) {
                        return it.previousIndex();
                    }
                    return (-it.previousIndex()) - 1;
                }
            }
            return (-list.size()) - 1;
        }
        int low = 0;
        int mid = list.size();
        int high = mid - 1;
        int result2 = -1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            result2 = -list.get(mid).compareTo(object);
            if (result2 > 0) {
                low = mid + 1;
            } else if (result2 != 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return (-mid) - (result2 < 0 ? 1 : 2);
    }

    public static <T> int binarySearch(List<? extends T> list, T t, Comparator<? super T> comparator) {
        if (comparator == null) {
            return binarySearch(list, t);
        }
        if (!(list instanceof RandomAccess)) {
            ListIterator<? extends T> listIterator = list.listIterator();
            while (listIterator.hasNext()) {
                int i = -comparator.compare(listIterator.next(), t);
                if (i <= 0) {
                    if (i == 0) {
                        return listIterator.previousIndex();
                    }
                    return (-listIterator.previousIndex()) - 1;
                }
            }
            return (-list.size()) - 1;
        }
        int i2 = 0;
        int size = list.size();
        int i3 = size - 1;
        int i4 = -1;
        while (i2 <= i3) {
            size = (i2 + i3) >>> 1;
            i4 = -comparator.compare(list.get(size), t);
            if (i4 > 0) {
                i2 = size + 1;
            } else if (i4 != 0) {
                i3 = size - 1;
            } else {
                return size;
            }
        }
        return (-size) - (i4 < 0 ? 1 : 2);
    }

    public static <T> void copy(List<? super T> list, List<? extends T> list2) {
        if (list.size() < list2.size()) {
            throw new IndexOutOfBoundsException("destination.size() < source.size(): " + list.size() + " < " + list2.size());
        }
        Iterator<? extends T> it = list2.iterator();
        ListIterator<? super T> listIterator = list.listIterator();
        while (it.hasNext()) {
            try {
                listIterator.next();
                listIterator.set(it.next());
            } catch (NoSuchElementException e) {
                throw new IndexOutOfBoundsException("Source size " + list2.size() + " does not fit into destination");
            }
        }
    }

    public static <T> Enumeration<T> enumeration(final Collection<T> collection) {
        return new Enumeration<T>() {
            Iterator<T> it;

            {
                this.it = collection.iterator();
            }

            @Override
            public boolean hasMoreElements() {
                return this.it.hasNext();
            }

            @Override
            public T nextElement() {
                return this.it.next();
            }
        };
    }

    public static <T> void fill(List<? super T> list, T t) {
        ListIterator<? super T> it = list.listIterator();
        while (it.hasNext()) {
            it.next();
            it.set(t);
        }
    }

    public static <T extends Comparable<? super T>> T max(Collection<? extends T> collection) {
        Iterator<? extends T> it = collection.iterator();
        T max = it.next();
        while (it.hasNext()) {
            T next = it.next();
            if (max.compareTo(next) < 0) {
                max = next;
            }
        }
        return max;
    }

    public static <T> T max(Collection<? extends T> collection, Comparator<? super T> comparator) {
        if (comparator == null) {
            return (T) max(collection);
        }
        Iterator<? extends T> it = collection.iterator();
        T next = it.next();
        while (it.hasNext()) {
            T next2 = it.next();
            if (comparator.compare(next, next2) < 0) {
                next = (Object) next2;
            }
        }
        return next;
    }

    public static <T extends Comparable<? super T>> T min(Collection<? extends T> collection) {
        Iterator<? extends T> it = collection.iterator();
        T min = it.next();
        while (it.hasNext()) {
            T next = it.next();
            if (min.compareTo(next) > 0) {
                min = next;
            }
        }
        return min;
    }

    public static <T> T min(Collection<? extends T> collection, Comparator<? super T> comparator) {
        if (comparator == null) {
            return (T) min(collection);
        }
        Iterator<? extends T> it = collection.iterator();
        T next = it.next();
        while (it.hasNext()) {
            T next2 = it.next();
            if (comparator.compare(next, next2) > 0) {
                next = (Object) next2;
            }
        }
        return next;
    }

    public static <T> List<T> nCopies(int length, T object) {
        return new CopiesList(length, object);
    }

    public static void reverse(List<?> list) {
        int size = list.size();
        ListIterator<?> listIterator = list.listIterator();
        ListIterator<?> listIterator2 = list.listIterator(size);
        for (int i = 0; i < size / 2; i++) {
            Object frontNext = listIterator.next();
            Object backPrev = listIterator2.previous();
            listIterator.set(backPrev);
            listIterator2.set(frontNext);
        }
    }

    public static <T> Comparator<T> reverseOrder() {
        return ReverseComparator.INSTANCE;
    }

    public static <T> Comparator<T> reverseOrder(Comparator<T> c) {
        if (c == null) {
            return reverseOrder();
        }
        if (c instanceof ReverseComparator2) {
            return ((ReverseComparator2) c).cmp;
        }
        return new ReverseComparator2(c);
    }

    public static void shuffle(List<?> list) {
        shuffle(list, new Random());
    }

    public static void shuffle(List<?> list, Random random) {
        if (list instanceof RandomAccess) {
            for (int i = list.size() - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                list.set(index, list.set(i, list.get(index)));
            }
            return;
        }
        Object[] array = list.toArray();
        for (int i2 = array.length - 1; i2 > 0; i2--) {
            int index2 = random.nextInt(i2 + 1);
            Object temp = array[i2];
            array[i2] = array[index2];
            array[index2] = temp;
        }
        int i3 = 0;
        ListIterator<?> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            listIterator.next();
            listIterator.set(array[i3]);
            i3++;
        }
    }

    public static <E> Set<E> singleton(E object) {
        return new SingletonSet(object);
    }

    public static <E> List<E> singletonList(E object) {
        return new SingletonList(object);
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        return new SingletonMap(key, value);
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        Object[] array = list.toArray();
        Arrays.sort(array);
        int i = 0;
        ListIterator<T> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            listIterator.next();
            listIterator.set((Comparable) array[i]);
            i++;
        }
    }

    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        Object[] array = list.toArray(new Object[list.size()]);
        Arrays.sort(array, comparator);
        int i = 0;
        ListIterator<T> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            listIterator.next();
            listIterator.set(array[i]);
            i++;
        }
    }

    public static void swap(List<?> list, int index1, int index2) {
        if (list == null) {
            throw new NullPointerException("list == null");
        }
        int size = list.size();
        if (index1 < 0 || index1 >= size || index2 < 0 || index2 >= size) {
            throw new IndexOutOfBoundsException();
        }
        if (index1 != index2) {
            list.set(index2, list.set(index1, list.get(index2)));
        }
    }

    public static <T> boolean replaceAll(List<T> list, T obj, T obj2) {
        boolean found = false;
        while (true) {
            int index = list.indexOf(obj);
            if (index > -1) {
                found = true;
                list.set(index, obj2);
            } else {
                return found;
            }
        }
    }

    public static void rotate(List<?> lst, int dist) {
        int normdist;
        int size = lst.size();
        if (size != 0) {
            if (dist > 0) {
                normdist = dist % size;
            } else {
                normdist = size - ((dist % size) * (-1));
            }
            if (normdist != 0 && normdist != size) {
                if (lst instanceof RandomAccess) {
                    Object temp = lst.get(0);
                    int index = 0;
                    int beginIndex = 0;
                    for (int i = 0; i < size; i++) {
                        index = (index + normdist) % size;
                        temp = lst.set(index, temp);
                        if (index == beginIndex) {
                            beginIndex++;
                            index = beginIndex;
                            temp = lst.get(beginIndex);
                        }
                    }
                    return;
                }
                int divideIndex = (size - normdist) % size;
                List<?> listSubList = lst.subList(0, divideIndex);
                List<?> listSubList2 = lst.subList(divideIndex, size);
                reverse(listSubList);
                reverse(listSubList2);
                reverse(lst);
            }
        }
    }

    public static int indexOfSubList(List<?> list, List<?> sublist) {
        int size = list.size();
        int sublistSize = sublist.size();
        if (sublistSize > size) {
            return -1;
        }
        if (sublistSize == 0) {
            return 0;
        }
        Object firstObj = sublist.get(0);
        int index = list.indexOf(firstObj);
        if (index == -1) {
            return -1;
        }
        while (index < size && size - index >= sublistSize) {
            ListIterator<?> listIt = list.listIterator(index);
            if (firstObj == null) {
                if (listIt.next() == null) {
                    ListIterator<?> sublistIt = sublist.listIterator(1);
                    boolean difFound = false;
                    while (sublistIt.hasNext()) {
                        Object element = sublistIt.next();
                        if (!listIt.hasNext()) {
                            return -1;
                        }
                        if (element == null) {
                            if (listIt.next() != null) {
                                difFound = true;
                                break;
                            }
                        } else if (!element.equals(listIt.next())) {
                            difFound = true;
                            break;
                        }
                    }
                    if (!difFound) {
                        return index;
                    }
                } else {
                    continue;
                }
            } else if (!firstObj.equals(listIt.next())) {
                continue;
            }
            index++;
        }
        return -1;
    }

    public static int lastIndexOfSubList(List<?> list, List<?> sublist) {
        int sublistSize = sublist.size();
        int size = list.size();
        if (sublistSize > size) {
            return -1;
        }
        if (sublistSize != 0) {
            Object lastObj = sublist.get(sublistSize - 1);
            for (int index = list.lastIndexOf(lastObj); index > -1 && index + 1 >= sublistSize; index--) {
                ListIterator<?> listIt = list.listIterator(index + 1);
                if (lastObj == null) {
                    if (listIt.previous() == null) {
                        ListIterator<?> sublistIt = sublist.listIterator(sublistSize - 1);
                        boolean difFound = false;
                        while (sublistIt.hasPrevious()) {
                            Object element = sublistIt.previous();
                            if (!listIt.hasPrevious()) {
                                return -1;
                            }
                            if (element != null) {
                                if (!element.equals(listIt.previous())) {
                                    difFound = true;
                                    break;
                                }
                            } else {
                                if (listIt.previous() != null) {
                                    difFound = true;
                                    break;
                                }
                            }
                        }
                        if (!difFound) {
                            return listIt.nextIndex();
                        }
                    } else {
                        continue;
                    }
                } else if (!lastObj.equals(listIt.previous())) {
                    continue;
                }
            }
            return -1;
        }
        return size;
    }

    public static <T> ArrayList<T> list(Enumeration<T> enumeration) {
        ArrayList<T> list = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }
        return list;
    }

    public static <T> Collection<T> synchronizedCollection(Collection<T> collection) {
        if (collection == null) {
            throw new NullPointerException("collection == null");
        }
        return new SynchronizedCollection(collection);
    }

    public static <T> List<T> synchronizedList(List<T> list) {
        if (list == null) {
            throw new NullPointerException("list == null");
        }
        return list instanceof RandomAccess ? new SynchronizedRandomAccessList(list) : new SynchronizedList(list);
    }

    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> map) {
        if (map == null) {
            throw new NullPointerException("map == null");
        }
        return new SynchronizedMap(map);
    }

    public static <E> Set<E> synchronizedSet(Set<E> set) {
        if (set == null) {
            throw new NullPointerException("set == null");
        }
        return new SynchronizedSet(set);
    }

    public static <K, V> SortedMap<K, V> synchronizedSortedMap(SortedMap<K, V> map) {
        if (map == null) {
            throw new NullPointerException("map == null");
        }
        return new SynchronizedSortedMap(map);
    }

    public static <E> SortedSet<E> synchronizedSortedSet(SortedSet<E> set) {
        if (set == null) {
            throw new NullPointerException("set == null");
        }
        return new SynchronizedSortedSet(set);
    }

    public static <E> Collection<E> unmodifiableCollection(Collection<? extends E> collection) {
        if (collection == null) {
            throw new NullPointerException("collection == null");
        }
        return new UnmodifiableCollection(collection);
    }

    public static <E> List<E> unmodifiableList(List<? extends E> list) {
        if (list == null) {
            throw new NullPointerException("list == null");
        }
        return list instanceof RandomAccess ? new UnmodifiableRandomAccessList(list) : new UnmodifiableList(list);
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> map) {
        if (map == null) {
            throw new NullPointerException("map == null");
        }
        return new UnmodifiableMap(map);
    }

    public static <E> Set<E> unmodifiableSet(Set<? extends E> set) {
        if (set == null) {
            throw new NullPointerException("set == null");
        }
        return new UnmodifiableSet(set);
    }

    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> map) {
        if (map == null) {
            throw new NullPointerException("map == null");
        }
        return new UnmodifiableSortedMap(map);
    }

    public static <E> SortedSet<E> unmodifiableSortedSet(SortedSet<E> set) {
        if (set == null) {
            throw new NullPointerException("set == null");
        }
        return new UnmodifiableSortedSet(set);
    }

    public static int frequency(Collection<?> c, Object o) {
        if (c == null) {
            throw new NullPointerException("c == null");
        }
        if (c.isEmpty()) {
            return 0;
        }
        int result = 0;
        for (Object e : c) {
            if (o == null) {
                if (e == null) {
                    result++;
                }
            } else if (o.equals(e)) {
                result++;
            }
        }
        return result;
    }

    public static final <T> List<T> emptyList() {
        return EMPTY_LIST;
    }

    public static final <T> Set<T> emptySet() {
        return EMPTY_SET;
    }

    public static final <K, V> Map<K, V> emptyMap() {
        return EMPTY_MAP;
    }

    public static <T> Enumeration<T> emptyEnumeration() {
        return (Enumeration<T>) EMPTY_ENUMERATION;
    }

    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EMPTY_ITERATOR;
    }

    public static <T> ListIterator<T> emptyListIterator() {
        return emptyList().listIterator();
    }

    public static <E> Collection<E> checkedCollection(Collection<E> c, Class<E> type) {
        return new CheckedCollection(c, type);
    }

    public static <K, V> Map<K, V> checkedMap(Map<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new CheckedMap(m, keyType, valueType);
    }

    public static <E> List<E> checkedList(List<E> list, Class<E> type) {
        return list instanceof RandomAccess ? new CheckedRandomAccessList(list, type) : new CheckedList(list, type);
    }

    public static <E> Set<E> checkedSet(Set<E> s, Class<E> type) {
        return new CheckedSet(s, type);
    }

    public static <K, V> SortedMap<K, V> checkedSortedMap(SortedMap<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new CheckedSortedMap(m, keyType, valueType);
    }

    public static <E> SortedSet<E> checkedSortedSet(SortedSet<E> s, Class<E> type) {
        return new CheckedSortedSet(s, type);
    }

    @SafeVarargs
    public static <T> boolean addAll(Collection<? super T> collection, T... tArr) {
        boolean zAdd = false;
        for (T t : tArr) {
            zAdd |= collection.add((Object) t);
        }
        return zAdd;
    }

    public static boolean disjoint(Collection<?> c1, Collection<?> c2) {
        if (((c1 instanceof Set) && !(c2 instanceof Set)) || c2.size() > c1.size()) {
            c1 = c2;
            c2 = c1;
        }
        Iterator<?> it = c1.iterator();
        while (it.hasNext()) {
            if (c2.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    static <E> E checkType(E obj, Class<? extends E> type) {
        if (obj != null && !type.isInstance(obj)) {
            throw new ClassCastException("Attempt to insert element of type " + obj.getClass() + " into collection of type " + type);
        }
        return obj;
    }

    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        if (map.isEmpty()) {
            return new SetFromMap(map);
        }
        throw new IllegalArgumentException("map not empty");
    }

    public static <T> Queue<T> asLifoQueue(Deque<T> deque) {
        return new AsLIFOQueue(deque);
    }

    private static class SetFromMap<E> extends AbstractSet<E> implements Serializable {
        private static final long serialVersionUID = 2454657854757543876L;
        private transient Set<E> backingSet;
        private final Map<E, Boolean> m;

        SetFromMap(Map<E, Boolean> map) {
            this.m = map;
            this.backingSet = map.keySet();
        }

        @Override
        public boolean equals(Object object) {
            return this.backingSet.equals(object);
        }

        @Override
        public int hashCode() {
            return this.backingSet.hashCode();
        }

        @Override
        public boolean add(E object) {
            return this.m.put(object, Boolean.TRUE) == null;
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public String toString() {
            return this.backingSet.toString();
        }

        @Override
        public boolean contains(Object object) {
            return this.backingSet.contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.backingSet.containsAll(collection);
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public boolean remove(Object object) {
            return this.m.remove(object) != null;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return this.backingSet.retainAll(collection);
        }

        @Override
        public Object[] toArray() {
            return this.backingSet.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.backingSet.toArray(tArr);
        }

        @Override
        public Iterator<E> iterator() {
            return this.backingSet.iterator();
        }

        @Override
        public int size() {
            return this.m.size();
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            this.backingSet = this.m.keySet();
        }
    }

    private static class AsLIFOQueue<E> extends AbstractQueue<E> implements Serializable {
        private static final long serialVersionUID = 1802017725587941708L;
        private final Deque<E> q;

        AsLIFOQueue(Deque<E> deque) {
            this.q = deque;
        }

        @Override
        public Iterator<E> iterator() {
            return this.q.iterator();
        }

        @Override
        public int size() {
            return this.q.size();
        }

        @Override
        public boolean offer(E o) {
            return this.q.offerFirst(o);
        }

        @Override
        public E peek() {
            return this.q.peekFirst();
        }

        @Override
        public E poll() {
            return this.q.pollFirst();
        }

        @Override
        public boolean add(E o) {
            this.q.push(o);
            return true;
        }

        @Override
        public void clear() {
            this.q.clear();
        }

        @Override
        public E element() {
            return this.q.getFirst();
        }

        @Override
        public E remove() {
            return this.q.pop();
        }

        @Override
        public boolean contains(Object object) {
            return this.q.contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.q.containsAll(collection);
        }

        @Override
        public boolean isEmpty() {
            return this.q.isEmpty();
        }

        @Override
        public boolean remove(Object object) {
            return this.q.remove(object);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return this.q.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return this.q.retainAll(collection);
        }

        @Override
        public Object[] toArray() {
            return this.q.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.q.toArray(tArr);
        }

        @Override
        public String toString() {
            return this.q.toString();
        }
    }

    private static class CheckedCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 1578914078182001775L;
        final Collection<E> c;
        final Class<E> type;

        public CheckedCollection(Collection<E> c, Class<E> type) {
            if (c == null) {
                throw new NullPointerException("c == null");
            }
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            this.c = c;
            this.type = type;
        }

        @Override
        public int size() {
            return this.c.size();
        }

        @Override
        public boolean isEmpty() {
            return this.c.isEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            return this.c.contains(obj);
        }

        @Override
        public Iterator<E> iterator() {
            Iterator<E> i = this.c.iterator();
            if (i instanceof ListIterator) {
                return new CheckedListIterator<>((ListIterator) i, this.type);
            }
            return i;
        }

        @Override
        public Object[] toArray() {
            return this.c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.c.toArray(tArr);
        }

        @Override
        public boolean add(E e) {
            return this.c.add((E) Collections.checkType(e, this.type));
        }

        @Override
        public boolean remove(Object obj) {
            return this.c.remove(obj);
        }

        @Override
        public boolean containsAll(Collection<?> c1) {
            return this.c.containsAll(c1);
        }

        @Override
        public boolean addAll(Collection<? extends E> c1) {
            Object[] array = c1.toArray();
            for (Object o : array) {
                Collections.checkType(o, this.type);
            }
            return this.c.addAll(Arrays.asList(array));
        }

        @Override
        public boolean removeAll(Collection<?> c1) {
            return this.c.removeAll(c1);
        }

        @Override
        public boolean retainAll(Collection<?> c1) {
            return this.c.retainAll(c1);
        }

        @Override
        public void clear() {
            this.c.clear();
        }

        public String toString() {
            return this.c.toString();
        }
    }

    private static class CheckedListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> i;
        private final Class<E> type;

        public CheckedListIterator(ListIterator<E> i, Class<E> type) {
            this.i = i;
            this.type = type;
        }

        @Override
        public boolean hasNext() {
            return this.i.hasNext();
        }

        @Override
        public E next() {
            return this.i.next();
        }

        @Override
        public void remove() {
            this.i.remove();
        }

        @Override
        public boolean hasPrevious() {
            return this.i.hasPrevious();
        }

        @Override
        public E previous() {
            return this.i.previous();
        }

        @Override
        public int nextIndex() {
            return this.i.nextIndex();
        }

        @Override
        public int previousIndex() {
            return this.i.previousIndex();
        }

        @Override
        public void set(E e) {
            this.i.set((E) Collections.checkType(e, this.type));
        }

        @Override
        public void add(E e) {
            this.i.add((E) Collections.checkType(e, this.type));
        }
    }

    private static class CheckedList<E> extends CheckedCollection<E> implements List<E> {
        private static final long serialVersionUID = 65247728283967356L;
        final List<E> l;

        public CheckedList(List<E> l, Class<E> type) {
            super(l, type);
            this.l = l;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c1) {
            Object[] array = c1.toArray();
            for (Object o : array) {
                Collections.checkType(o, this.type);
            }
            return this.l.addAll(index, Arrays.asList(array));
        }

        @Override
        public E get(int index) {
            return this.l.get(index);
        }

        @Override
        public E set(int i, E e) {
            return (E) this.l.set(i, Collections.checkType(e, this.type));
        }

        @Override
        public void add(int i, E e) {
            this.l.add(i, Collections.checkType(e, this.type));
        }

        @Override
        public E remove(int index) {
            return this.l.remove(index);
        }

        @Override
        public int indexOf(Object obj) {
            return this.l.indexOf(obj);
        }

        @Override
        public int lastIndexOf(Object obj) {
            return this.l.lastIndexOf(obj);
        }

        @Override
        public ListIterator<E> listIterator() {
            return new CheckedListIterator(this.l.listIterator(), this.type);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return new CheckedListIterator(this.l.listIterator(index), this.type);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return Collections.checkedList(this.l.subList(fromIndex, toIndex), this.type);
        }

        @Override
        public boolean equals(Object obj) {
            return this.l.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.l.hashCode();
        }
    }

    private static class CheckedRandomAccessList<E> extends CheckedList<E> implements RandomAccess {
        private static final long serialVersionUID = 1638200125423088369L;

        public CheckedRandomAccessList(List<E> l, Class<E> type) {
            super(l, type);
        }
    }

    private static class CheckedSet<E> extends CheckedCollection<E> implements Set<E> {
        private static final long serialVersionUID = 4694047833775013803L;

        public CheckedSet(Set<E> s, Class<E> type) {
            super(s, type);
        }

        @Override
        public boolean equals(Object obj) {
            return this.c.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.c.hashCode();
        }
    }

    private static class CheckedMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = 5742860141034234728L;
        final Class<K> keyType;
        final Map<K, V> m;
        final Class<V> valueType;

        private CheckedMap(Map<K, V> m, Class<K> keyType, Class<V> valueType) {
            if (m == null) {
                throw new NullPointerException("m == null");
            }
            if (keyType == null) {
                throw new NullPointerException("keyType == null");
            }
            if (valueType == null) {
                throw new NullPointerException("valueType == null");
            }
            this.m = m;
            this.keyType = keyType;
            this.valueType = valueType;
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return this.m.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return this.m.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return this.m.get(key);
        }

        @Override
        public V put(K k, V v) {
            return this.m.put((K) Collections.checkType(k, this.keyType), (V) Collections.checkType(v, this.valueType));
        }

        @Override
        public V remove(Object key) {
            return this.m.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            int size = map.size();
            if (size != 0) {
                Map.Entry[] entryArr = new Map.Entry[size];
                Iterator<Map.Entry<? extends K, ? extends V>> it = map.entrySet().iterator();
                for (int i = 0; i < size; i++) {
                    Map.Entry<? extends K, ? extends V> next = it.next();
                    Collections.checkType(next.getKey(), this.keyType);
                    Collections.checkType(next.getValue(), this.valueType);
                    entryArr[i] = next;
                }
                for (int i2 = 0; i2 < size; i2++) {
                    this.m.put((K) entryArr[i2].getKey(), (V) entryArr[i2].getValue());
                }
            }
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public Set<K> keySet() {
            return this.m.keySet();
        }

        @Override
        public Collection<V> values() {
            return this.m.values();
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new CheckedEntrySet(this.m.entrySet(), this.valueType);
        }

        @Override
        public boolean equals(Object obj) {
            return this.m.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.m.hashCode();
        }

        public String toString() {
            return this.m.toString();
        }

        private static class CheckedEntry<K, V> implements Map.Entry<K, V> {
            final Map.Entry<K, V> e;
            final Class<V> valueType;

            public CheckedEntry(Map.Entry<K, V> e, Class<V> valueType) {
                if (e == null) {
                    throw new NullPointerException("e == null");
                }
                this.e = e;
                this.valueType = valueType;
            }

            @Override
            public K getKey() {
                return this.e.getKey();
            }

            @Override
            public V getValue() {
                return this.e.getValue();
            }

            @Override
            public V setValue(V v) {
                return this.e.setValue((V) Collections.checkType(v, this.valueType));
            }

            @Override
            public boolean equals(Object obj) {
                return this.e.equals(obj);
            }

            @Override
            public int hashCode() {
                return this.e.hashCode();
            }
        }

        private static class CheckedEntrySet<K, V> implements Set<Map.Entry<K, V>> {
            final Set<Map.Entry<K, V>> s;
            final Class<V> valueType;

            public CheckedEntrySet(Set<Map.Entry<K, V>> s, Class<V> valueType) {
                this.s = s;
                this.valueType = valueType;
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new CheckedEntryIterator(this.s.iterator(), this.valueType);
            }

            @Override
            public Object[] toArray() {
                int thisSize = size();
                Object[] array = new Object[thisSize];
                Iterator<?> it = iterator();
                for (int i = 0; i < thisSize; i++) {
                    array[i] = it.next();
                }
                return array;
            }

            @Override
            public <T> T[] toArray(T[] tArr) {
                int size = size();
                if (tArr.length < size) {
                    tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size));
                }
                Iterator<Map.Entry<K, V>> it = iterator();
                for (int i = 0; i < size; i++) {
                    tArr[i] = it.next();
                }
                if (size < tArr.length) {
                    tArr[size] = null;
                }
                return tArr;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return this.s.retainAll(c);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return this.s.removeAll(c);
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return this.s.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends Map.Entry<K, V>> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                return this.s.remove(o);
            }

            @Override
            public boolean contains(Object o) {
                return this.s.contains(o);
            }

            @Override
            public boolean add(Map.Entry<K, V> o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isEmpty() {
                return this.s.isEmpty();
            }

            @Override
            public void clear() {
                this.s.clear();
            }

            @Override
            public int size() {
                return this.s.size();
            }

            @Override
            public int hashCode() {
                return this.s.hashCode();
            }

            @Override
            public boolean equals(Object object) {
                return this.s.equals(object);
            }

            private static class CheckedEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {
                Iterator<Map.Entry<K, V>> i;
                Class<V> valueType;

                public CheckedEntryIterator(Iterator<Map.Entry<K, V>> i, Class<V> valueType) {
                    this.i = i;
                    this.valueType = valueType;
                }

                @Override
                public boolean hasNext() {
                    return this.i.hasNext();
                }

                @Override
                public void remove() {
                    this.i.remove();
                }

                @Override
                public Map.Entry<K, V> next() {
                    return new CheckedEntry(this.i.next(), this.valueType);
                }
            }
        }
    }

    private static class CheckedSortedSet<E> extends CheckedSet<E> implements SortedSet<E> {
        private static final long serialVersionUID = 1599911165492914959L;
        private final SortedSet<E> ss;

        public CheckedSortedSet(SortedSet<E> s, Class<E> type) {
            super(s, type);
            this.ss = s;
        }

        @Override
        public Comparator<? super E> comparator() {
            return this.ss.comparator();
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return new CheckedSortedSet(this.ss.subSet(fromElement, toElement), this.type);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return new CheckedSortedSet(this.ss.headSet(toElement), this.type);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return new CheckedSortedSet(this.ss.tailSet(fromElement), this.type);
        }

        @Override
        public E first() {
            return this.ss.first();
        }

        @Override
        public E last() {
            return this.ss.last();
        }
    }

    private static class CheckedSortedMap<K, V> extends CheckedMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = 1599671320688067438L;
        final SortedMap<K, V> sm;

        CheckedSortedMap(SortedMap<K, V> m, Class<K> keyType, Class<V> valueType) {
            super(m, keyType, valueType);
            this.sm = m;
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.sm.comparator();
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return new CheckedSortedMap(this.sm.subMap(fromKey, toKey), this.keyType, this.valueType);
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return new CheckedSortedMap(this.sm.headMap(toKey), this.keyType, this.valueType);
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return new CheckedSortedMap(this.sm.tailMap(fromKey), this.keyType, this.valueType);
        }

        @Override
        public K firstKey() {
            return this.sm.firstKey();
        }

        @Override
        public K lastKey() {
            return this.sm.lastKey();
        }
    }

    public static int secondaryHash(Object key) {
        return secondaryHash(key.hashCode());
    }

    public static int secondaryIdentityHash(Object key) {
        return secondaryHash(System.identityHashCode(key));
    }

    private static int secondaryHash(int h) {
        int h2 = h + ((h << 15) ^ (-12931));
        int h3 = h2 ^ (h2 >>> 10);
        int h4 = h3 + (h3 << 3);
        int h5 = h4 ^ (h4 >>> 6);
        int h6 = h5 + (h5 << 2) + (h5 << 14);
        return (h6 >>> 16) ^ h6;
    }

    public static int roundUpToPowerOfTwo(int i) {
        int i2 = i - 1;
        int i3 = i2 | (i2 >>> 1);
        int i4 = i3 | (i3 >>> 2);
        int i5 = i4 | (i4 >>> 4);
        int i6 = i5 | (i5 >>> 8);
        return (i6 | (i6 >>> 16)) + 1;
    }
}
