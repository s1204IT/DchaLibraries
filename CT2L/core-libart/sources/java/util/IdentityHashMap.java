package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.MapEntry;

public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable, Cloneable {
    private static final int DEFAULT_MAX_SIZE = 21;
    private static final Object NULL_OBJECT = new Object();
    private static final int loadFactor = 7500;
    private static final long serialVersionUID = 8188218128353913216L;
    transient Object[] elementData;
    transient int modCount;
    int size;
    transient int threshold;

    static class IdentityHashMapEntry<K, V> extends MapEntry<K, V> {
        private final IdentityHashMap<K, V> map;

        IdentityHashMapEntry(IdentityHashMap<K, V> map, K theKey, V theValue) {
            super(theKey, theValue);
            this.map = map;
        }

        @Override
        public Object clone() {
            return super.clone();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) object;
            return this.key == entry.getKey() && this.value == entry.getValue();
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.key) ^ System.identityHashCode(this.value);
        }

        @Override
        public String toString() {
            return this.key + "=" + this.value;
        }

        @Override
        public V setValue(V v) {
            V v2 = (V) super.setValue(v);
            this.map.put(this.key, v);
            return v2;
        }
    }

    static class IdentityHashMapIterator<E, KT, VT> implements Iterator<E> {
        final IdentityHashMap<KT, VT> associatedMap;
        int expectedModCount;
        final MapEntry.Type<E, KT, VT> type;
        private int position = 0;
        private int lastPosition = 0;
        boolean canRemove = false;

        IdentityHashMapIterator(MapEntry.Type<E, KT, VT> value, IdentityHashMap<KT, VT> hm) {
            this.associatedMap = hm;
            this.type = value;
            this.expectedModCount = hm.modCount;
        }

        @Override
        public boolean hasNext() {
            while (this.position < this.associatedMap.elementData.length) {
                if (this.associatedMap.elementData[this.position] == null) {
                    this.position += 2;
                } else {
                    return true;
                }
            }
            return false;
        }

        void checkConcurrentMod() throws ConcurrentModificationException {
            if (this.expectedModCount != this.associatedMap.modCount) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public E next() {
            checkConcurrentMod();
            if (hasNext()) {
                IdentityHashMapEntry<KT, VT> result = this.associatedMap.getEntry(this.position);
                this.lastPosition = this.position;
                this.position += 2;
                this.canRemove = true;
                return this.type.get(result);
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            checkConcurrentMod();
            if (!this.canRemove) {
                throw new IllegalStateException();
            }
            this.canRemove = false;
            this.associatedMap.remove(this.associatedMap.elementData[this.lastPosition]);
            this.position = this.lastPosition;
            this.expectedModCount++;
        }
    }

    static class IdentityHashMapEntrySet<KT, VT> extends AbstractSet<Map.Entry<KT, VT>> {
        private final IdentityHashMap<KT, VT> associatedMap;

        public IdentityHashMapEntrySet(IdentityHashMap<KT, VT> hm) {
            this.associatedMap = hm;
        }

        IdentityHashMap<KT, VT> hashMap() {
            return this.associatedMap;
        }

        @Override
        public int size() {
            return this.associatedMap.size;
        }

        @Override
        public void clear() {
            this.associatedMap.clear();
        }

        @Override
        public boolean remove(Object object) {
            if (!contains(object)) {
                return false;
            }
            this.associatedMap.remove(((Map.Entry) object).getKey());
            return true;
        }

        @Override
        public boolean contains(Object object) {
            if (!(object instanceof Map.Entry)) {
                return false;
            }
            IdentityHashMapEntry<?, ?> entry = this.associatedMap.getEntry(((Map.Entry) object).getKey());
            return entry != null && entry.equals(object);
        }

        @Override
        public Iterator<Map.Entry<KT, VT>> iterator() {
            return new IdentityHashMapIterator(new MapEntry.Type<Map.Entry<KT, VT>, KT, VT>() {
                @Override
                public Map.Entry<KT, VT> get(MapEntry<KT, VT> entry) {
                    return entry;
                }
            }, this.associatedMap);
        }
    }

    public IdentityHashMap() {
        this(21);
    }

    public IdentityHashMap(int maxSize) {
        this.modCount = 0;
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize < 0: " + maxSize);
        }
        this.size = 0;
        this.threshold = getThreshold(maxSize);
        this.elementData = newElementArray(computeElementArraySize());
    }

    private int getThreshold(int maxSize) {
        if (maxSize > 3) {
            return maxSize;
        }
        return 3;
    }

    private int computeElementArraySize() {
        int arraySize = ((int) ((((long) this.threshold) * 10000) / 7500)) * 2;
        return arraySize < 0 ? -arraySize : arraySize;
    }

    private Object[] newElementArray(int s) {
        return new Object[s];
    }

    public IdentityHashMap(Map<? extends K, ? extends V> map) {
        this(map.size() < 6 ? 11 : map.size() * 2);
        putAllImpl(map);
    }

    private V massageValue(Object obj) {
        if (obj == NULL_OBJECT) {
            return null;
        }
        return obj;
    }

    @Override
    public void clear() {
        this.size = 0;
        for (int i = 0; i < this.elementData.length; i++) {
            this.elementData[i] = null;
        }
        this.modCount++;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            key = NULL_OBJECT;
        }
        int index = findIndex(key, this.elementData);
        return this.elementData[index] == key;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            value = NULL_OBJECT;
        }
        for (int i = 1; i < this.elementData.length; i += 2) {
            if (this.elementData[i] == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            key = NULL_OBJECT;
        }
        int index = findIndex(key, this.elementData);
        if (this.elementData[index] != key) {
            return null;
        }
        Object result = this.elementData[index + 1];
        return massageValue(result);
    }

    private IdentityHashMapEntry<K, V> getEntry(Object key) {
        if (key == null) {
            key = NULL_OBJECT;
        }
        int index = findIndex(key, this.elementData);
        if (this.elementData[index] == key) {
            return getEntry(index);
        }
        return null;
    }

    private IdentityHashMapEntry<K, V> getEntry(int index) {
        Object key = this.elementData[index];
        Object value = this.elementData[index + 1];
        if (key == NULL_OBJECT) {
            key = null;
        }
        if (value == NULL_OBJECT) {
            value = null;
        }
        return new IdentityHashMapEntry<>(this, key, value);
    }

    private int findIndex(Object key, Object[] array) {
        int length = array.length;
        int index = getModuloHash(key, length);
        int last = ((index + length) - 2) % length;
        while (index != last && array[index] != key && array[index] != null) {
            index = (index + 2) % length;
        }
        return index;
    }

    private int getModuloHash(Object key, int length) {
        return ((Collections.secondaryIdentityHash(key) & Integer.MAX_VALUE) % (length / 2)) * 2;
    }

    @Override
    public V put(K key, V value) {
        Object _key = key;
        Object _value = value;
        if (_key == null) {
            _key = NULL_OBJECT;
        }
        if (_value == null) {
            _value = NULL_OBJECT;
        }
        int index = findIndex(_key, this.elementData);
        if (this.elementData[index] != _key) {
            this.modCount++;
            int i = this.size + 1;
            this.size = i;
            if (i > this.threshold) {
                rehash();
                index = findIndex(_key, this.elementData);
            }
            this.elementData[index] = _key;
            this.elementData[index + 1] = null;
        }
        Object result = this.elementData[index + 1];
        this.elementData[index + 1] = _value;
        return massageValue(result);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        putAllImpl(map);
    }

    private void rehash() {
        int newlength = this.elementData.length * 2;
        if (newlength == 0) {
            newlength = 1;
        }
        Object[] newData = newElementArray(newlength);
        for (int i = 0; i < this.elementData.length; i += 2) {
            Object key = this.elementData[i];
            if (key != null) {
                int index = findIndex(key, newData);
                newData[index] = key;
                newData[index + 1] = this.elementData[i + 1];
            }
        }
        this.elementData = newData;
        computeMaxSize();
    }

    private void computeMaxSize() {
        this.threshold = (int) ((((long) (this.elementData.length / 2)) * 7500) / 10000);
    }

    @Override
    public V remove(Object key) {
        boolean hashedOk;
        if (key == null) {
            key = NULL_OBJECT;
        }
        int next = findIndex(key, this.elementData);
        int index = next;
        if (this.elementData[index] != key) {
            return null;
        }
        Object result = this.elementData[index + 1];
        int length = this.elementData.length;
        while (true) {
            next = (next + 2) % length;
            Object object = this.elementData[next];
            if (object != null) {
                int hash = getModuloHash(object, length);
                boolean hashedOk2 = hash > index;
                if (next < index) {
                    hashedOk = hashedOk2 || hash <= next;
                } else {
                    hashedOk = hashedOk2 && hash <= next;
                }
                if (!hashedOk) {
                    this.elementData[index] = object;
                    this.elementData[index + 1] = this.elementData[next + 1];
                    index = next;
                }
            } else {
                this.size--;
                this.modCount++;
                this.elementData[index] = null;
                this.elementData[index + 1] = null;
                return massageValue(result);
            }
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new IdentityHashMapEntrySet(this);
    }

    @Override
    public Set<K> keySet() {
        if (this.keySet == null) {
            this.keySet = new AbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return IdentityHashMap.this.containsKey(object);
                }

                @Override
                public int size() {
                    return IdentityHashMap.this.size();
                }

                @Override
                public void clear() {
                    IdentityHashMap.this.clear();
                }

                @Override
                public boolean remove(Object key) {
                    if (!IdentityHashMap.this.containsKey(key)) {
                        return false;
                    }
                    IdentityHashMap.this.remove(key);
                    return true;
                }

                @Override
                public Iterator<K> iterator() {
                    return new IdentityHashMapIterator(new MapEntry.Type<K, K, V>() {
                        @Override
                        public K get(MapEntry<K, V> entry) {
                            return entry.key;
                        }
                    }, IdentityHashMap.this);
                }
            };
        }
        return this.keySet;
    }

    @Override
    public Collection<V> values() {
        if (this.valuesCollection == null) {
            this.valuesCollection = new AbstractCollection<V>() {
                @Override
                public boolean contains(Object object) {
                    return IdentityHashMap.this.containsValue(object);
                }

                @Override
                public int size() {
                    return IdentityHashMap.this.size();
                }

                @Override
                public void clear() {
                    IdentityHashMap.this.clear();
                }

                @Override
                public Iterator<V> iterator() {
                    return new IdentityHashMapIterator(new MapEntry.Type<V, K, V>() {
                        @Override
                        public V get(MapEntry<K, V> entry) {
                            return entry.value;
                        }
                    }, IdentityHashMap.this);
                }

                @Override
                public boolean remove(Object object) {
                    Iterator<V> it = iterator();
                    while (it.hasNext()) {
                        if (object == it.next()) {
                            it.remove();
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
        return this.valuesCollection;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Map)) {
            return false;
        }
        Map<?, ?> map = (Map) object;
        if (size() != map.size()) {
            return false;
        }
        Set<Map.Entry<K, V>> set = entrySet();
        return set.equals(map.entrySet());
    }

    @Override
    public Object clone() {
        try {
            IdentityHashMap<K, V> cloneHashMap = (IdentityHashMap) super.clone();
            cloneHashMap.elementData = newElementArray(this.elementData.length);
            System.arraycopy(this.elementData, 0, cloneHashMap.elementData, 0, this.elementData.length);
            return cloneHashMap;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.size);
        Iterator<?> iterator = entrySet().iterator();
        while (iterator.hasNext()) {
            MapEntry<?, ?> entry = (MapEntry) iterator.next();
            stream.writeObject(entry.key);
            stream.writeObject(entry.value);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int savedSize = stream.readInt();
        this.threshold = getThreshold(21);
        this.elementData = newElementArray(computeElementArraySize());
        int i = savedSize;
        while (true) {
            i--;
            if (i >= 0) {
                put(stream.readObject(), stream.readObject());
            } else {
                this.size = savedSize;
                return;
            }
        }
    }

    private void putAllImpl(Map<? extends K, ? extends V> map) {
        if (map.entrySet() != null) {
            super.putAll(map);
        }
    }
}
