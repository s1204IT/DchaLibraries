package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable, Cloneable {
    private static final int DEFAULT_CAPACITY = 32;
    private static final int MAXIMUM_CAPACITY = 536870912;
    private static final int MINIMUM_CAPACITY = 4;
    static final Object NULL_KEY = new Object();
    private static final long serialVersionUID = 8188218128353913216L;
    private transient Set<Map.Entry<K, V>> entrySet;
    transient int modCount;
    int size;
    transient Object[] table;

    private static Object maskNull(Object obj) {
        return obj == null ? NULL_KEY : obj;
    }

    static final Object unmaskNull(Object obj) {
        if (obj == NULL_KEY) {
            return null;
        }
        return obj;
    }

    public IdentityHashMap() {
        init(32);
    }

    public IdentityHashMap(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("expectedMaxSize is negative: " + i);
        }
        init(capacity(i));
    }

    private static int capacity(int i) {
        if (i > 178956970) {
            return MAXIMUM_CAPACITY;
        }
        if (i <= 2) {
            return 4;
        }
        return Integer.highestOneBit(i + (i << 1));
    }

    private void init(int i) {
        this.table = new Object[2 * i];
    }

    public IdentityHashMap(Map<? extends K, ? extends V> map) {
        this((int) (((double) (1 + map.size())) * 1.1d));
        putAll(map);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    private static int hash(Object obj, int i) {
        int iIdentityHashCode = System.identityHashCode(obj);
        return ((iIdentityHashCode << 1) - (iIdentityHashCode << 8)) & (i - 1);
    }

    private static int nextKeyIndex(int i, int i2) {
        int i3 = i + 2;
        if (i3 < i2) {
            return i3;
        }
        return 0;
    }

    @Override
    public V get(Object obj) {
        Object objMaskNull = maskNull(obj);
        Object[] objArr = this.table;
        int length = objArr.length;
        int iHash = hash(objMaskNull, length);
        while (true) {
            Object obj2 = objArr[iHash];
            if (obj2 == objMaskNull) {
                return (V) objArr[iHash + 1];
            }
            if (obj2 == null) {
                return null;
            }
            iHash = nextKeyIndex(iHash, length);
        }
    }

    @Override
    public boolean containsKey(Object obj) {
        Object objMaskNull = maskNull(obj);
        Object[] objArr = this.table;
        int length = objArr.length;
        int iHash = hash(objMaskNull, length);
        while (true) {
            Object obj2 = objArr[iHash];
            if (obj2 == objMaskNull) {
                return true;
            }
            if (obj2 == null) {
                return false;
            }
            iHash = nextKeyIndex(iHash, length);
        }
    }

    @Override
    public boolean containsValue(Object obj) {
        Object[] objArr = this.table;
        for (int i = 1; i < objArr.length; i += 2) {
            if (objArr[i] == obj && objArr[i - 1] != null) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMapping(Object obj, Object obj2) {
        Object objMaskNull = maskNull(obj);
        Object[] objArr = this.table;
        int length = objArr.length;
        int iHash = hash(objMaskNull, length);
        while (true) {
            Object obj3 = objArr[iHash];
            if (obj3 == objMaskNull) {
                if (objArr[iHash + 1] != obj2) {
                    return false;
                }
                return true;
            }
            if (obj3 == null) {
                return false;
            }
            iHash = nextKeyIndex(iHash, length);
        }
    }

    @Override
    public V put(K k, V v) {
        Object[] objArr;
        int length;
        int iHash;
        int i;
        Object objMaskNull = maskNull(k);
        do {
            objArr = this.table;
            length = objArr.length;
            iHash = hash(objMaskNull, length);
            while (true) {
                Object obj = objArr[iHash];
                if (obj == null) {
                    break;
                }
                if (obj != objMaskNull) {
                    iHash = nextKeyIndex(iHash, length);
                } else {
                    int i2 = iHash + 1;
                    V v2 = (V) objArr[i2];
                    objArr[i2] = v;
                    return v2;
                }
            }
        } while (resize(length));
        this.modCount++;
        objArr[iHash] = objMaskNull;
        objArr[iHash + 1] = v;
        this.size = i;
        return null;
    }

    private boolean resize(int i) {
        int i2 = i * 2;
        Object[] objArr = this.table;
        int length = objArr.length;
        if (length == 1073741824) {
            if (this.size != 536870911) {
                return false;
            }
            throw new IllegalStateException("Capacity exhausted.");
        }
        if (length >= i2) {
            return false;
        }
        Object[] objArr2 = new Object[i2];
        for (int i3 = 0; i3 < length; i3 += 2) {
            Object obj = objArr[i3];
            if (obj != null) {
                int i4 = i3 + 1;
                Object obj2 = objArr[i4];
                objArr[i3] = null;
                objArr[i4] = null;
                int iHash = hash(obj, i2);
                while (objArr2[iHash] != null) {
                    iHash = nextKeyIndex(iHash, i2);
                }
                objArr2[iHash] = obj;
                objArr2[iHash + 1] = obj2;
            }
        }
        this.table = objArr2;
        return true;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        if (size > this.size) {
            resize(capacity(size));
        }
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object obj) {
        Object objMaskNull = maskNull(obj);
        Object[] objArr = this.table;
        int length = objArr.length;
        int iHash = hash(objMaskNull, length);
        while (true) {
            Object obj2 = objArr[iHash];
            if (obj2 == objMaskNull) {
                this.modCount++;
                this.size--;
                int i = iHash + 1;
                V v = (V) objArr[i];
                objArr[i] = null;
                objArr[iHash] = null;
                closeDeletion(iHash);
                return v;
            }
            if (obj2 == null) {
                return null;
            }
            iHash = nextKeyIndex(iHash, length);
        }
    }

    private boolean removeMapping(Object obj, Object obj2) {
        Object objMaskNull = maskNull(obj);
        Object[] objArr = this.table;
        int length = objArr.length;
        int iHash = hash(objMaskNull, length);
        while (true) {
            Object obj3 = objArr[iHash];
            if (obj3 == objMaskNull) {
                int i = iHash + 1;
                if (objArr[i] != obj2) {
                    return false;
                }
                this.modCount++;
                this.size--;
                objArr[iHash] = null;
                objArr[i] = null;
                closeDeletion(iHash);
                return true;
            }
            if (obj3 == null) {
                return false;
            }
            iHash = nextKeyIndex(iHash, length);
        }
    }

    private void closeDeletion(int i) {
        Object[] objArr = this.table;
        int length = objArr.length;
        int iNextKeyIndex = nextKeyIndex(i, length);
        while (true) {
            Object obj = objArr[iNextKeyIndex];
            if (obj != null) {
                int iHash = hash(obj, length);
                if ((iNextKeyIndex < iHash && (iHash <= i || i <= iNextKeyIndex)) || (iHash <= i && i <= iNextKeyIndex)) {
                    objArr[i] = obj;
                    int i2 = iNextKeyIndex + 1;
                    objArr[i + 1] = objArr[i2];
                    objArr[iNextKeyIndex] = null;
                    objArr[i2] = null;
                    i = iNextKeyIndex;
                }
                iNextKeyIndex = nextKeyIndex(iNextKeyIndex, length);
            } else {
                return;
            }
        }
    }

    @Override
    public void clear() {
        this.modCount++;
        Object[] objArr = this.table;
        for (int i = 0; i < objArr.length; i++) {
            objArr[i] = null;
        }
        this.size = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof IdentityHashMap) {
            IdentityHashMap identityHashMap = (IdentityHashMap) obj;
            if (identityHashMap.size() != this.size) {
                return false;
            }
            Object[] objArr = identityHashMap.table;
            for (int i = 0; i < objArr.length; i += 2) {
                Object obj2 = objArr[i];
                if (obj2 != null && !containsMapping(obj2, objArr[i + 1])) {
                    return false;
                }
            }
            return true;
        }
        if (obj instanceof Map) {
            return entrySet().equals(((Map) obj).entrySet());
        }
        return false;
    }

    @Override
    public int hashCode() {
        Object[] objArr = this.table;
        int iIdentityHashCode = 0;
        for (int i = 0; i < objArr.length; i += 2) {
            Object obj = objArr[i];
            if (obj != null) {
                iIdentityHashCode += System.identityHashCode(unmaskNull(obj)) ^ System.identityHashCode(objArr[i + 1]);
            }
        }
        return iIdentityHashCode;
    }

    @Override
    public Object clone() {
        try {
            IdentityHashMap identityHashMap = (IdentityHashMap) super.clone();
            identityHashMap.entrySet = null;
            identityHashMap.table = (Object[]) this.table.clone();
            return identityHashMap;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private abstract class IdentityHashMapIterator<T> implements Iterator<T> {
        int expectedModCount;
        int index;
        boolean indexValid;
        int lastReturnedIndex;
        Object[] traversalTable;

        private IdentityHashMapIterator() {
            this.index = IdentityHashMap.this.size != 0 ? 0 : IdentityHashMap.this.table.length;
            this.expectedModCount = IdentityHashMap.this.modCount;
            this.lastReturnedIndex = -1;
            this.traversalTable = IdentityHashMap.this.table;
        }

        @Override
        public boolean hasNext() {
            Object[] objArr = this.traversalTable;
            for (int i = this.index; i < objArr.length; i += 2) {
                if (objArr[i] != null) {
                    this.index = i;
                    this.indexValid = true;
                    return true;
                }
            }
            this.index = objArr.length;
            return false;
        }

        protected int nextIndex() {
            if (IdentityHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (!this.indexValid && !hasNext()) {
                throw new NoSuchElementException();
            }
            this.indexValid = false;
            this.lastReturnedIndex = this.index;
            this.index += 2;
            return this.lastReturnedIndex;
        }

        @Override
        public void remove() {
            if (this.lastReturnedIndex == -1) {
                throw new IllegalStateException();
            }
            if (IdentityHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            IdentityHashMap identityHashMap = IdentityHashMap.this;
            int i = identityHashMap.modCount + 1;
            identityHashMap.modCount = i;
            this.expectedModCount = i;
            int i2 = this.lastReturnedIndex;
            this.lastReturnedIndex = -1;
            this.index = i2;
            this.indexValid = false;
            Object[] objArr = this.traversalTable;
            int length = objArr.length;
            Object obj = objArr[i2];
            objArr[i2] = null;
            objArr[i2 + 1] = null;
            if (objArr != IdentityHashMap.this.table) {
                IdentityHashMap.this.remove(obj);
                this.expectedModCount = IdentityHashMap.this.modCount;
                return;
            }
            IdentityHashMap identityHashMap2 = IdentityHashMap.this;
            identityHashMap2.size--;
            int iNextKeyIndex = IdentityHashMap.nextKeyIndex(i2, length);
            int i3 = i2;
            while (true) {
                Object obj2 = objArr[iNextKeyIndex];
                if (obj2 != null) {
                    int iHash = IdentityHashMap.hash(obj2, length);
                    if ((iNextKeyIndex < iHash && (iHash <= i3 || i3 <= iNextKeyIndex)) || (iHash <= i3 && i3 <= iNextKeyIndex)) {
                        if (iNextKeyIndex < i2 && i3 >= i2 && this.traversalTable == IdentityHashMap.this.table) {
                            int i4 = length - i2;
                            Object[] objArr2 = new Object[i4];
                            System.arraycopy(objArr, i2, objArr2, 0, i4);
                            this.traversalTable = objArr2;
                            this.index = 0;
                        }
                        objArr[i3] = obj2;
                        int i5 = iNextKeyIndex + 1;
                        objArr[i3 + 1] = objArr[i5];
                        objArr[iNextKeyIndex] = null;
                        objArr[i5] = null;
                        i3 = iNextKeyIndex;
                    }
                    iNextKeyIndex = IdentityHashMap.nextKeyIndex(iNextKeyIndex, length);
                } else {
                    return;
                }
            }
        }
    }

    private class KeyIterator extends IdentityHashMap<K, V>.IdentityHashMapIterator<K> {
        private KeyIterator() {
            super();
        }

        @Override
        public K next() {
            return (K) IdentityHashMap.unmaskNull(this.traversalTable[nextIndex()]);
        }
    }

    private class ValueIterator extends IdentityHashMap<K, V>.IdentityHashMapIterator<V> {
        private ValueIterator() {
            super();
        }

        @Override
        public V next() {
            return (V) this.traversalTable[nextIndex() + 1];
        }
    }

    private class EntryIterator extends IdentityHashMap<K, V>.IdentityHashMapIterator<Map.Entry<K, V>> {
        private IdentityHashMap<K, V>.EntryIterator.Entry lastReturnedEntry;

        private EntryIterator() {
            super();
        }

        @Override
        public Map.Entry<K, V> next() {
            this.lastReturnedEntry = new Entry(nextIndex());
            return this.lastReturnedEntry;
        }

        @Override
        public void remove() {
            this.lastReturnedIndex = this.lastReturnedEntry == null ? -1 : ((Entry) this.lastReturnedEntry).index;
            super.remove();
            ((Entry) this.lastReturnedEntry).index = this.lastReturnedIndex;
            this.lastReturnedEntry = null;
        }

        private class Entry implements Map.Entry<K, V> {
            private int index;

            private Entry(int i) {
                this.index = i;
            }

            @Override
            public K getKey() {
                checkIndexForEntryUse();
                return (K) IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index]);
            }

            @Override
            public V getValue() {
                checkIndexForEntryUse();
                return (V) EntryIterator.this.traversalTable[this.index + 1];
            }

            @Override
            public V setValue(V v) {
                checkIndexForEntryUse();
                V v2 = (V) EntryIterator.this.traversalTable[this.index + 1];
                EntryIterator.this.traversalTable[this.index + 1] = v;
                if (EntryIterator.this.traversalTable != IdentityHashMap.this.table) {
                    IdentityHashMap.this.put(EntryIterator.this.traversalTable[this.index], v);
                }
                return v2;
            }

            @Override
            public boolean equals(Object obj) {
                if (this.index < 0) {
                    return super.equals(obj);
                }
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                return entry.getKey() == IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index]) && entry.getValue() == EntryIterator.this.traversalTable[this.index + 1];
            }

            @Override
            public int hashCode() {
                if (EntryIterator.this.lastReturnedIndex < 0) {
                    return super.hashCode();
                }
                return System.identityHashCode(IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index])) ^ System.identityHashCode(EntryIterator.this.traversalTable[this.index + 1]);
            }

            public String toString() {
                if (this.index < 0) {
                    return super.toString();
                }
                return IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index]) + "=" + EntryIterator.this.traversalTable[this.index + 1];
            }

            private void checkIndexForEntryUse() {
                if (this.index < 0) {
                    throw new IllegalStateException("Entry was removed");
                }
            }
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set == null) {
            KeySet keySet = new KeySet();
            this.keySet = keySet;
            return keySet;
        }
        return set;
    }

    private class KeySet extends AbstractSet<K> {
        private KeySet() {
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return IdentityHashMap.this.size;
        }

        @Override
        public boolean contains(Object obj) {
            return IdentityHashMap.this.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            int i = IdentityHashMap.this.size;
            IdentityHashMap.this.remove(obj);
            return IdentityHashMap.this.size != i;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            Objects.requireNonNull(collection);
            Iterator<K> it = iterator();
            boolean z = false;
            while (it.hasNext()) {
                if (collection.contains(it.next())) {
                    it.remove();
                    z = true;
                }
            }
            return z;
        }

        @Override
        public void clear() {
            IdentityHashMap.this.clear();
        }

        @Override
        public int hashCode() {
            Iterator<K> it = iterator();
            int iIdentityHashCode = 0;
            while (it.hasNext()) {
                iIdentityHashCode += System.identityHashCode(it.next());
            }
            return iIdentityHashCode;
        }

        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            int i = IdentityHashMap.this.modCount;
            int size = size();
            if (tArr.length < size) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size));
            }
            Object[] objArr = IdentityHashMap.this.table;
            int i2 = 0;
            for (int i3 = 0; i3 < objArr.length; i3 += 2) {
                Object obj = objArr[i3];
                if (obj != null) {
                    if (i2 >= size) {
                        throw new ConcurrentModificationException();
                    }
                    tArr[i2] = IdentityHashMap.unmaskNull(obj);
                    i2++;
                }
            }
            if (i2 < size || i != IdentityHashMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (i2 < tArr.length) {
                tArr[i2] = null;
            }
            return tArr;
        }

        @Override
        public Spliterator<K> spliterator() {
            return new KeySpliterator(IdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection == null) {
            Values values = new Values();
            this.values = values;
            return values;
        }
        return collection;
    }

    private class Values extends AbstractCollection<V> {
        private Values() {
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return IdentityHashMap.this.size;
        }

        @Override
        public boolean contains(Object obj) {
            return IdentityHashMap.this.containsValue(obj);
        }

        @Override
        public boolean remove(Object obj) {
            Iterator<V> it = iterator();
            while (it.hasNext()) {
                if (it.next() == obj) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            IdentityHashMap.this.clear();
        }

        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            int i = IdentityHashMap.this.modCount;
            int size = size();
            if (tArr.length < size) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size));
            }
            Object[] objArr = IdentityHashMap.this.table;
            int i2 = 0;
            for (int i3 = 0; i3 < objArr.length; i3 += 2) {
                if (objArr[i3] != null) {
                    if (i2 >= size) {
                        throw new ConcurrentModificationException();
                    }
                    tArr[i2] = objArr[i3 + 1];
                    i2++;
                }
            }
            if (i2 < size || i != IdentityHashMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (i2 < tArr.length) {
                tArr[i2] = null;
            }
            return tArr;
        }

        @Override
        public Spliterator<V> spliterator() {
            return new ValueSpliterator(IdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        EntrySet entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        private EntrySet() {
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return IdentityHashMap.this.containsMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return IdentityHashMap.this.removeMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return IdentityHashMap.this.size;
        }

        @Override
        public void clear() {
            IdentityHashMap.this.clear();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            Objects.requireNonNull(collection);
            Iterator<Map.Entry<K, V>> it = iterator();
            boolean z = false;
            while (it.hasNext()) {
                if (collection.contains(it.next())) {
                    it.remove();
                    z = true;
                }
            }
            return z;
        }

        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            int i = IdentityHashMap.this.modCount;
            int size = size();
            if (tArr.length < size) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size));
            }
            Object[] objArr = IdentityHashMap.this.table;
            int i2 = 0;
            for (int i3 = 0; i3 < objArr.length; i3 += 2) {
                Object obj = objArr[i3];
                if (obj != null) {
                    if (i2 >= size) {
                        throw new ConcurrentModificationException();
                    }
                    tArr[i2] = new AbstractMap.SimpleEntry(IdentityHashMap.unmaskNull(obj), objArr[i3 + 1]);
                    i2++;
                }
            }
            if (i2 < size || i != IdentityHashMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (i2 < tArr.length) {
                tArr[i2] = null;
            }
            return tArr;
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator(IdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.size);
        Object[] objArr = this.table;
        for (int i = 0; i < objArr.length; i += 2) {
            Object obj = objArr[i];
            if (obj != null) {
                objectOutputStream.writeObject(unmaskNull(obj));
                objectOutputStream.writeObject(objArr[i + 1]);
            }
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        int i = objectInputStream.readInt();
        if (i < 0) {
            throw new StreamCorruptedException("Illegal mappings count: " + i);
        }
        init(capacity(i));
        for (int i2 = 0; i2 < i; i2++) {
            putForCreate(objectInputStream.readObject(), objectInputStream.readObject());
        }
    }

    private void putForCreate(K k, V v) throws StreamCorruptedException {
        Object objMaskNull = maskNull(k);
        Object[] objArr = this.table;
        int length = objArr.length;
        int iHash = hash(objMaskNull, length);
        while (true) {
            Object obj = objArr[iHash];
            if (obj != null) {
                if (obj == objMaskNull) {
                    throw new StreamCorruptedException();
                }
                iHash = nextKeyIndex(iHash, length);
            } else {
                objArr[iHash] = objMaskNull;
                objArr[iHash + 1] = v;
                return;
            }
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Objects.requireNonNull(biConsumer);
        int i = this.modCount;
        Object[] objArr = this.table;
        for (int i2 = 0; i2 < objArr.length; i2 += 2) {
            Object obj = objArr[i2];
            if (obj != null) {
                biConsumer.accept((Object) unmaskNull(obj), objArr[i2 + 1]);
            }
            if (this.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        int i = this.modCount;
        Object[] objArr = this.table;
        for (int i2 = 0; i2 < objArr.length; i2 += 2) {
            Object obj = objArr[i2];
            if (obj != null) {
                int i3 = i2 + 1;
                objArr[i3] = biFunction.apply((Object) unmaskNull(obj), objArr[i3]);
            }
            if (this.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }
    }

    static class IdentityHashMapSpliterator<K, V> {
        int est;
        int expectedModCount;
        int fence;
        int index;
        final IdentityHashMap<K, V> map;

        IdentityHashMapSpliterator(IdentityHashMap<K, V> identityHashMap, int i, int i2, int i3, int i4) {
            this.map = identityHashMap;
            this.index = i;
            this.fence = i2;
            this.est = i3;
            this.expectedModCount = i4;
        }

        final int getFence() {
            int i = this.fence;
            if (i < 0) {
                this.est = this.map.size;
                this.expectedModCount = this.map.modCount;
                int length = this.map.table.length;
                this.fence = length;
                return length;
            }
            return i;
        }

        public final long estimateSize() {
            getFence();
            return this.est;
        }
    }

    static final class KeySpliterator<K, V> extends IdentityHashMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(IdentityHashMap<K, V> identityHashMap, int i, int i2, int i3, int i4) {
            super(identityHashMap, i, i2, i3, i4);
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = ((fence + i) >>> 1) & (-2);
            if (i >= i2) {
                return null;
            }
            IdentityHashMap<K, V> identityHashMap = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new KeySpliterator<>(identityHashMap, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            Object[] objArr;
            int i;
            if (consumer == null) {
                throw new NullPointerException();
            }
            IdentityHashMap<K, V> identityHashMap = this.map;
            if (identityHashMap != null && (objArr = identityHashMap.table) != null && (i = this.index) >= 0) {
                int fence = getFence();
                this.index = fence;
                if (fence <= objArr.length) {
                    for (i = this.index; i < fence; i += 2) {
                        Object obj = objArr[i];
                        if (obj != null) {
                            consumer.accept((Object) IdentityHashMap.unmaskNull(obj));
                        }
                    }
                    if (identityHashMap.modCount == this.expectedModCount) {
                        return;
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Object[] objArr = this.map.table;
            int fence = getFence();
            while (this.index < fence) {
                Object obj = objArr[this.index];
                this.index += 2;
                if (obj != null) {
                    consumer.accept((Object) IdentityHashMap.unmaskNull(obj));
                    if (this.map.modCount != this.expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return ((this.fence < 0 || this.est == this.map.size) ? 64 : 0) | 1;
        }
    }

    static final class ValueSpliterator<K, V> extends IdentityHashMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(IdentityHashMap<K, V> identityHashMap, int i, int i2, int i3, int i4) {
            super(identityHashMap, i, i2, i3, i4);
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = ((fence + i) >>> 1) & (-2);
            if (i >= i2) {
                return null;
            }
            IdentityHashMap<K, V> identityHashMap = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new ValueSpliterator<>(identityHashMap, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> consumer) {
            Object[] objArr;
            int i;
            if (consumer == null) {
                throw new NullPointerException();
            }
            IdentityHashMap<K, V> identityHashMap = this.map;
            if (identityHashMap != null && (objArr = identityHashMap.table) != null && (i = this.index) >= 0) {
                int fence = getFence();
                this.index = fence;
                if (fence <= objArr.length) {
                    for (i = this.index; i < fence; i += 2) {
                        if (objArr[i] != null) {
                            consumer.accept(objArr[i + 1]);
                        }
                    }
                    if (identityHashMap.modCount == this.expectedModCount) {
                        return;
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Object[] objArr = this.map.table;
            int fence = getFence();
            while (this.index < fence) {
                Object obj = objArr[this.index];
                Object obj2 = objArr[this.index + 1];
                this.index += 2;
                if (obj != null) {
                    consumer.accept(obj2);
                    if (this.map.modCount == this.expectedModCount) {
                        return true;
                    }
                    throw new ConcurrentModificationException();
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return (this.fence < 0 || this.est == this.map.size) ? 64 : 0;
        }
    }

    static final class EntrySpliterator<K, V> extends IdentityHashMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(IdentityHashMap<K, V> identityHashMap, int i, int i2, int i3, int i4) {
            super(identityHashMap, i, i2, i3, i4);
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = ((fence + i) >>> 1) & (-2);
            if (i >= i2) {
                return null;
            }
            IdentityHashMap<K, V> identityHashMap = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new EntrySpliterator<>(identityHashMap, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> consumer) {
            Object[] objArr;
            int i;
            if (consumer == null) {
                throw new NullPointerException();
            }
            IdentityHashMap<K, V> identityHashMap = this.map;
            if (identityHashMap != null && (objArr = identityHashMap.table) != null && (i = this.index) >= 0) {
                int fence = getFence();
                this.index = fence;
                if (fence <= objArr.length) {
                    for (i = this.index; i < fence; i += 2) {
                        Object obj = objArr[i];
                        if (obj != null) {
                            consumer.accept(new AbstractMap.SimpleImmutableEntry(IdentityHashMap.unmaskNull(obj), objArr[i + 1]));
                        }
                    }
                    if (identityHashMap.modCount == this.expectedModCount) {
                        return;
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Object[] objArr = this.map.table;
            int fence = getFence();
            while (this.index < fence) {
                Object obj = objArr[this.index];
                Object obj2 = objArr[this.index + 1];
                this.index += 2;
                if (obj != null) {
                    consumer.accept(new AbstractMap.SimpleImmutableEntry(IdentityHashMap.unmaskNull(obj), obj2));
                    if (this.map.modCount == this.expectedModCount) {
                        return true;
                    }
                    throw new ConcurrentModificationException();
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return ((this.fence < 0 || this.est == this.map.size) ? 64 : 0) | 1;
        }
    }
}
