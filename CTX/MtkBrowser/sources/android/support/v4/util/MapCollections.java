package android.support.v4.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

abstract class MapCollections<K, V> {
    MapCollections<K, V>.EntrySet mEntrySet;
    MapCollections<K, V>.KeySet mKeySet;
    MapCollections<K, V>.ValuesCollection mValues;

    final class ArrayIterator<T> implements Iterator<T> {
        boolean mCanRemove = false;
        int mIndex;
        final int mOffset;
        int mSize;
        final MapCollections this$0;

        ArrayIterator(MapCollections mapCollections, int i) {
            this.this$0 = mapCollections;
            this.mOffset = i;
            this.mSize = mapCollections.colGetSize();
        }

        @Override
        public boolean hasNext() {
            return this.mIndex < this.mSize;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T t = (T) this.this$0.colGetEntry(this.mIndex, this.mOffset);
            this.mIndex++;
            this.mCanRemove = true;
            return t;
        }

        @Override
        public void remove() {
            if (!this.mCanRemove) {
                throw new IllegalStateException();
            }
            this.mIndex--;
            this.mSize--;
            this.mCanRemove = false;
            this.this$0.colRemoveAt(this.mIndex);
        }
    }

    final class EntrySet implements Set<Map.Entry<K, V>> {
        final MapCollections this$0;

        EntrySet(MapCollections mapCollections) {
            this.this$0 = mapCollections;
        }

        @Override
        public boolean add(Map.Entry<K, V> entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Map.Entry<K, V>> collection) {
            int iColGetSize = this.this$0.colGetSize();
            for (Map.Entry<K, V> entry : collection) {
                this.this$0.colPut(entry.getKey(), entry.getValue());
            }
            return iColGetSize != this.this$0.colGetSize();
        }

        @Override
        public void clear() {
            this.this$0.colClear();
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            int iColIndexOfKey = this.this$0.colIndexOfKey(entry.getKey());
            if (iColIndexOfKey >= 0) {
                return ContainerHelpers.equal(this.this$0.colGetEntry(iColIndexOfKey, 1), entry.getValue());
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            Iterator<?> it = collection.iterator();
            while (it.hasNext()) {
                if (!contains(it.next())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            return MapCollections.equalsSetHelper(this, obj);
        }

        @Override
        public int hashCode() {
            int iColGetSize = this.this$0.colGetSize() - 1;
            int iHashCode = 0;
            while (iColGetSize >= 0) {
                Object objColGetEntry = this.this$0.colGetEntry(iColGetSize, 0);
                Object objColGetEntry2 = this.this$0.colGetEntry(iColGetSize, 1);
                iColGetSize--;
                iHashCode += (objColGetEntry2 == null ? 0 : objColGetEntry2.hashCode()) ^ (objColGetEntry == null ? 0 : objColGetEntry.hashCode());
            }
            return iHashCode;
        }

        @Override
        public boolean isEmpty() {
            return this.this$0.colGetSize() == 0;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new MapIterator(this.this$0);
        }

        @Override
        public boolean remove(Object obj) {
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
            return this.this$0.colGetSize();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            throw new UnsupportedOperationException();
        }
    }

    final class KeySet implements Set<K> {
        final MapCollections this$0;

        KeySet(MapCollections mapCollections) {
            this.this$0 = mapCollections;
        }

        @Override
        public boolean add(K k) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends K> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            this.this$0.colClear();
        }

        @Override
        public boolean contains(Object obj) {
            return this.this$0.colIndexOfKey(obj) >= 0;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return MapCollections.containsAllHelper(this.this$0.colGetMap(), collection);
        }

        @Override
        public boolean equals(Object obj) {
            return MapCollections.equalsSetHelper(this, obj);
        }

        @Override
        public int hashCode() {
            int iHashCode = 0;
            for (int iColGetSize = this.this$0.colGetSize() - 1; iColGetSize >= 0; iColGetSize--) {
                Object objColGetEntry = this.this$0.colGetEntry(iColGetSize, 0);
                iHashCode += objColGetEntry == null ? 0 : objColGetEntry.hashCode();
            }
            return iHashCode;
        }

        @Override
        public boolean isEmpty() {
            return this.this$0.colGetSize() == 0;
        }

        @Override
        public Iterator<K> iterator() {
            return new ArrayIterator(this.this$0, 0);
        }

        @Override
        public boolean remove(Object obj) {
            int iColIndexOfKey = this.this$0.colIndexOfKey(obj);
            if (iColIndexOfKey < 0) {
                return false;
            }
            this.this$0.colRemoveAt(iColIndexOfKey);
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return MapCollections.removeAllHelper(this.this$0.colGetMap(), collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return MapCollections.retainAllHelper(this.this$0.colGetMap(), collection);
        }

        @Override
        public int size() {
            return this.this$0.colGetSize();
        }

        @Override
        public Object[] toArray() {
            return this.this$0.toArrayHelper(0);
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.this$0.toArrayHelper(tArr, 0);
        }
    }

    final class MapIterator implements Iterator<Map.Entry<K, V>>, Map.Entry<K, V> {
        int mEnd;
        boolean mEntryValid = false;
        int mIndex = -1;
        final MapCollections this$0;

        MapIterator(MapCollections mapCollections) {
            this.this$0 = mapCollections;
            this.mEnd = mapCollections.colGetSize() - 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (!this.mEntryValid) {
                throw new IllegalStateException("This container does not support retaining Map.Entry objects");
            }
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return ContainerHelpers.equal(entry.getKey(), this.this$0.colGetEntry(this.mIndex, 0)) && ContainerHelpers.equal(entry.getValue(), this.this$0.colGetEntry(this.mIndex, 1));
        }

        @Override
        public K getKey() {
            if (this.mEntryValid) {
                return (K) this.this$0.colGetEntry(this.mIndex, 0);
            }
            throw new IllegalStateException("This container does not support retaining Map.Entry objects");
        }

        @Override
        public V getValue() {
            if (this.mEntryValid) {
                return (V) this.this$0.colGetEntry(this.mIndex, 1);
            }
            throw new IllegalStateException("This container does not support retaining Map.Entry objects");
        }

        @Override
        public boolean hasNext() {
            return this.mIndex < this.mEnd;
        }

        @Override
        public int hashCode() {
            if (!this.mEntryValid) {
                throw new IllegalStateException("This container does not support retaining Map.Entry objects");
            }
            Object objColGetEntry = this.this$0.colGetEntry(this.mIndex, 0);
            Object objColGetEntry2 = this.this$0.colGetEntry(this.mIndex, 1);
            return (objColGetEntry2 != null ? objColGetEntry2.hashCode() : 0) ^ (objColGetEntry == null ? 0 : objColGetEntry.hashCode());
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.mIndex++;
            this.mEntryValid = true;
            return this;
        }

        @Override
        public void remove() {
            if (!this.mEntryValid) {
                throw new IllegalStateException();
            }
            this.this$0.colRemoveAt(this.mIndex);
            this.mIndex--;
            this.mEnd--;
            this.mEntryValid = false;
        }

        @Override
        public V setValue(V v) {
            if (this.mEntryValid) {
                return (V) this.this$0.colSetValue(this.mIndex, v);
            }
            throw new IllegalStateException("This container does not support retaining Map.Entry objects");
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    final class ValuesCollection implements Collection<V> {
        final MapCollections this$0;

        ValuesCollection(MapCollections mapCollections) {
            this.this$0 = mapCollections;
        }

        @Override
        public boolean add(V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends V> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            this.this$0.colClear();
        }

        @Override
        public boolean contains(Object obj) {
            return this.this$0.colIndexOfValue(obj) >= 0;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            Iterator<?> it = collection.iterator();
            while (it.hasNext()) {
                if (!contains(it.next())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return this.this$0.colGetSize() == 0;
        }

        @Override
        public Iterator<V> iterator() {
            return new ArrayIterator(this.this$0, 1);
        }

        @Override
        public boolean remove(Object obj) {
            int iColIndexOfValue = this.this$0.colIndexOfValue(obj);
            if (iColIndexOfValue < 0) {
                return false;
            }
            this.this$0.colRemoveAt(iColIndexOfValue);
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            int iColGetSize = this.this$0.colGetSize();
            int i = 0;
            boolean z = false;
            while (i < iColGetSize) {
                if (collection.contains(this.this$0.colGetEntry(i, 1))) {
                    this.this$0.colRemoveAt(i);
                    i--;
                    iColGetSize--;
                    z = true;
                }
                i++;
            }
            return z;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            int iColGetSize = this.this$0.colGetSize();
            int i = 0;
            boolean z = false;
            while (i < iColGetSize) {
                if (!collection.contains(this.this$0.colGetEntry(i, 1))) {
                    this.this$0.colRemoveAt(i);
                    i--;
                    iColGetSize--;
                    z = true;
                }
                i++;
            }
            return z;
        }

        @Override
        public int size() {
            return this.this$0.colGetSize();
        }

        @Override
        public Object[] toArray() {
            return this.this$0.toArrayHelper(1);
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.this$0.toArrayHelper(tArr, 1);
        }
    }

    MapCollections() {
    }

    public static <K, V> boolean containsAllHelper(Map<K, V> map, Collection<?> collection) {
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            if (!map.containsKey(it.next())) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean equalsSetHelper(Set<T> set, Object obj) {
        boolean z;
        if (set == obj) {
            return true;
        }
        if (!(obj instanceof Set)) {
            return false;
        }
        Set set2 = (Set) obj;
        try {
            if (set.size() == set2.size()) {
                z = set.containsAll(set2);
            }
            return z;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public static <K, V> boolean removeAllHelper(Map<K, V> map, Collection<?> collection) {
        int size = map.size();
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            map.remove(it.next());
        }
        return size != map.size();
    }

    public static <K, V> boolean retainAllHelper(Map<K, V> map, Collection<?> collection) {
        int size = map.size();
        Iterator<K> it = map.keySet().iterator();
        while (it.hasNext()) {
            if (!collection.contains(it.next())) {
                it.remove();
            }
        }
        return size != map.size();
    }

    protected abstract void colClear();

    protected abstract Object colGetEntry(int i, int i2);

    protected abstract Map<K, V> colGetMap();

    protected abstract int colGetSize();

    protected abstract int colIndexOfKey(Object obj);

    protected abstract int colIndexOfValue(Object obj);

    protected abstract void colPut(K k, V v);

    protected abstract void colRemoveAt(int i);

    protected abstract V colSetValue(int i, V v);

    public Set<Map.Entry<K, V>> getEntrySet() {
        if (this.mEntrySet == null) {
            this.mEntrySet = new EntrySet(this);
        }
        return this.mEntrySet;
    }

    public Set<K> getKeySet() {
        if (this.mKeySet == null) {
            this.mKeySet = new KeySet(this);
        }
        return this.mKeySet;
    }

    public Collection<V> getValues() {
        if (this.mValues == null) {
            this.mValues = new ValuesCollection(this);
        }
        return this.mValues;
    }

    public Object[] toArrayHelper(int i) {
        int iColGetSize = colGetSize();
        Object[] objArr = new Object[iColGetSize];
        for (int i2 = 0; i2 < iColGetSize; i2++) {
            objArr[i2] = colGetEntry(i2, i);
        }
        return objArr;
    }

    public <T> T[] toArrayHelper(T[] tArr, int i) {
        int iColGetSize = colGetSize();
        Object[] objArr = tArr.length < iColGetSize ? (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), iColGetSize)) : tArr;
        for (int i2 = 0; i2 < iColGetSize; i2++) {
            objArr[i2] = colGetEntry(i2, i);
        }
        if (objArr.length > iColGetSize) {
            objArr[iColGetSize] = null;
        }
        return (T[]) objArr;
    }
}
