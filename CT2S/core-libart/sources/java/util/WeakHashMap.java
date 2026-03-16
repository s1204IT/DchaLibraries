package java.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

public class WeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    private static final int DEFAULT_SIZE = 16;
    int elementCount;
    Entry<K, V>[] elementData;
    private final int loadFactor;
    volatile int modCount;
    private final ReferenceQueue<K> referenceQueue;
    private int threshold;

    private static <K, V> Entry<K, V>[] newEntryArray(int size) {
        return new Entry[size];
    }

    private static final class Entry<K, V> extends WeakReference<K> implements Map.Entry<K, V> {
        final int hash;
        boolean isNull;
        Entry<K, V> next;
        V value;

        interface Type<R, K, V> {
            R get(Map.Entry<K, V> entry);
        }

        Entry(K key, V object, ReferenceQueue<K> queue) {
            super(key, queue);
            this.isNull = key == null;
            this.hash = this.isNull ? 0 : Collections.secondaryHash(key);
            this.value = object;
        }

        @Override
        public K getKey() {
            return (K) super.get();
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V object) {
            V result = this.value;
            this.value = object;
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) other;
            Object key = super.get();
            if (key == null) {
                if (key != entry.getKey()) {
                    return false;
                }
            } else if (!key.equals(entry.getKey())) {
                return false;
            }
            if (this.value == null) {
                if (this.value != entry.getValue()) {
                    return false;
                }
            } else if (!this.value.equals(entry.getValue())) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return (this.value == null ? 0 : this.value.hashCode()) + this.hash;
        }

        public String toString() {
            return super.get() + "=" + this.value;
        }
    }

    class HashIterator<R> implements Iterator<R> {
        private Entry<K, V> currentEntry;
        private int expectedModCount;
        private Entry<K, V> nextEntry;
        private K nextKey;
        private int position = 0;
        final Entry.Type<R, K, V> type;

        HashIterator(Entry.Type<R, K, V> type) {
            this.type = type;
            this.expectedModCount = WeakHashMap.this.modCount;
        }

        @Override
        public boolean hasNext() {
            if (this.nextEntry != null && (this.nextKey != null || this.nextEntry.isNull)) {
                return true;
            }
            while (true) {
                if (this.nextEntry == null) {
                    while (this.position < WeakHashMap.this.elementData.length) {
                        Entry<K, V>[] entryArr = WeakHashMap.this.elementData;
                        int i = this.position;
                        this.position = i + 1;
                        Entry<K, V> entry = entryArr[i];
                        this.nextEntry = entry;
                        if (entry != null) {
                            break;
                        }
                    }
                    if (this.nextEntry == null) {
                        return false;
                    }
                }
                this.nextKey = this.nextEntry.get();
                if (this.nextKey != null || this.nextEntry.isNull) {
                    return true;
                }
                this.nextEntry = this.nextEntry.next;
            }
        }

        @Override
        public R next() {
            if (this.expectedModCount == WeakHashMap.this.modCount) {
                if (hasNext()) {
                    this.currentEntry = this.nextEntry;
                    this.nextEntry = this.currentEntry.next;
                    R result = this.type.get(this.currentEntry);
                    this.nextKey = null;
                    return result;
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public void remove() {
            if (this.expectedModCount == WeakHashMap.this.modCount) {
                if (this.currentEntry != null) {
                    WeakHashMap.this.removeEntry(this.currentEntry);
                    this.currentEntry = null;
                    this.expectedModCount++;
                    return;
                }
                throw new IllegalStateException();
            }
            throw new ConcurrentModificationException();
        }
    }

    public WeakHashMap() {
        this(16);
    }

    public WeakHashMap(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        this.elementCount = 0;
        this.elementData = newEntryArray(capacity == 0 ? 1 : capacity);
        this.loadFactor = 7500;
        computeMaxSize();
        this.referenceQueue = new ReferenceQueue<>();
    }

    public WeakHashMap(int capacity, float loadFactor) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        if (loadFactor <= 0.0f) {
            throw new IllegalArgumentException("loadFactor <= 0: " + loadFactor);
        }
        this.elementCount = 0;
        this.elementData = newEntryArray(capacity == 0 ? 1 : capacity);
        this.loadFactor = (int) (10000.0f * loadFactor);
        computeMaxSize();
        this.referenceQueue = new ReferenceQueue<>();
    }

    public WeakHashMap(Map<? extends K, ? extends V> map) {
        this(map.size() < 6 ? 11 : map.size() * 2);
        putAllImpl(map);
    }

    @Override
    public void clear() {
        if (this.elementCount > 0) {
            this.elementCount = 0;
            Arrays.fill(this.elementData, (Object) null);
            this.modCount++;
            while (this.referenceQueue.poll() != null) {
            }
        }
    }

    private void computeMaxSize() {
        this.threshold = (int) ((((long) this.elementData.length) * ((long) this.loadFactor)) / 10000);
    }

    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        poll();
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public int size() {
                return WeakHashMap.this.size();
            }

            @Override
            public void clear() {
                WeakHashMap.this.clear();
            }

            @Override
            public boolean remove(Object object) {
                if (!contains(object)) {
                    return false;
                }
                WeakHashMap.this.remove(((Map.Entry) object).getKey());
                return true;
            }

            @Override
            public boolean contains(Object object) {
                Entry<K, V> entry;
                if ((object instanceof Map.Entry) && (entry = WeakHashMap.this.getEntry(((Map.Entry) object).getKey())) != null) {
                    Object key = entry.get();
                    if (key != null || entry.isNull) {
                        return object.equals(entry);
                    }
                }
                return false;
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new HashIterator(new Entry.Type<Map.Entry<K, V>, K, V>() {
                    @Override
                    public Map.Entry<K, V> get(Map.Entry<K, V> entry) {
                        return entry;
                    }
                });
            }
        };
    }

    @Override
    public Set<K> keySet() {
        poll();
        if (this.keySet == null) {
            this.keySet = new AbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return WeakHashMap.this.containsKey(object);
                }

                @Override
                public int size() {
                    return WeakHashMap.this.size();
                }

                @Override
                public void clear() {
                    WeakHashMap.this.clear();
                }

                @Override
                public boolean remove(Object key) {
                    if (!WeakHashMap.this.containsKey(key)) {
                        return false;
                    }
                    WeakHashMap.this.remove(key);
                    return true;
                }

                @Override
                public Iterator<K> iterator() {
                    return new HashIterator(new Entry.Type<K, K, V>() {
                        @Override
                        public K get(Map.Entry<K, V> entry) {
                            return entry.getKey();
                        }
                    });
                }
            };
        }
        return this.keySet;
    }

    @Override
    public Collection<V> values() {
        poll();
        if (this.valuesCollection == null) {
            this.valuesCollection = new AbstractCollection<V>() {
                @Override
                public int size() {
                    return WeakHashMap.this.size();
                }

                @Override
                public void clear() {
                    WeakHashMap.this.clear();
                }

                @Override
                public boolean contains(Object object) {
                    return WeakHashMap.this.containsValue(object);
                }

                @Override
                public Iterator<V> iterator() {
                    return new HashIterator(new Entry.Type<V, K, V>() {
                        @Override
                        public V get(Map.Entry<K, V> entry) {
                            return entry.getValue();
                        }
                    });
                }
            };
        }
        return this.valuesCollection;
    }

    @Override
    public V get(Object key) {
        poll();
        if (key != null) {
            int index = (Collections.secondaryHash(key) & Integer.MAX_VALUE) % this.elementData.length;
            for (Entry<K, V> entry = this.elementData[index]; entry != null; entry = entry.next) {
                if (key.equals(entry.get())) {
                    return entry.value;
                }
            }
            return null;
        }
        for (Entry<K, V> entry2 = this.elementData[0]; entry2 != null; entry2 = entry2.next) {
            if (entry2.isNull) {
                return entry2.value;
            }
        }
        return null;
    }

    Entry<K, V> getEntry(Object key) {
        poll();
        if (key != null) {
            int index = (Collections.secondaryHash(key) & Integer.MAX_VALUE) % this.elementData.length;
            for (Entry<K, V> entry = this.elementData[index]; entry != null; entry = entry.next) {
                if (key.equals(entry.get())) {
                    return entry;
                }
            }
            return null;
        }
        for (Entry<K, V> entry2 = this.elementData[0]; entry2 != null; entry2 = entry2.next) {
            if (entry2.isNull) {
                return entry2;
            }
        }
        return null;
    }

    @Override
    public boolean containsValue(Object value) {
        poll();
        if (value != null) {
            int i = this.elementData.length;
            while (true) {
                i--;
                if (i < 0) {
                    break;
                }
                for (Entry<K, V> entry = this.elementData[i]; entry != null; entry = entry.next) {
                    K key = entry.get();
                    if ((key != null || entry.isNull) && value.equals(entry.value)) {
                        return true;
                    }
                }
            }
        } else {
            int i2 = this.elementData.length;
            while (true) {
                i2--;
                if (i2 < 0) {
                    break;
                }
                for (Entry<K, V> entry2 = this.elementData[i2]; entry2 != null; entry2 = entry2.next) {
                    K key2 = entry2.get();
                    if ((key2 != null || entry2.isNull) && entry2.value == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    void poll() {
        while (true) {
            Entry<K, V> toRemove = (Entry) this.referenceQueue.poll();
            if (toRemove != null) {
                removeEntry(toRemove);
            } else {
                return;
            }
        }
    }

    void removeEntry(Entry<K, V> toRemove) {
        Entry<K, V> last = null;
        int index = (toRemove.hash & Integer.MAX_VALUE) % this.elementData.length;
        for (Entry<K, V> entry = this.elementData[index]; entry != null; entry = entry.next) {
            if (toRemove == entry) {
                this.modCount++;
                if (last == null) {
                    this.elementData[index] = entry.next;
                } else {
                    last.next = entry.next;
                }
                this.elementCount--;
                return;
            }
            last = entry;
        }
    }

    @Override
    public V put(K key, V value) {
        Entry<K, V> entry;
        poll();
        int index = 0;
        if (key != null) {
            index = (Collections.secondaryHash(key) & Integer.MAX_VALUE) % this.elementData.length;
            entry = this.elementData[index];
            while (entry != null && !key.equals(entry.get())) {
                entry = entry.next;
            }
        } else {
            entry = this.elementData[0];
            while (entry != null && !entry.isNull) {
                entry = entry.next;
            }
        }
        if (entry == null) {
            this.modCount++;
            int i = this.elementCount + 1;
            this.elementCount = i;
            if (i > this.threshold) {
                rehash();
                index = key == null ? 0 : (Collections.secondaryHash(key) & Integer.MAX_VALUE) % this.elementData.length;
            }
            Entry<K, V> entry2 = new Entry<>(key, value, this.referenceQueue);
            entry2.next = this.elementData[index];
            this.elementData[index] = entry2;
            return null;
        }
        V v = entry.value;
        entry.value = value;
        return v;
    }

    private void rehash() {
        int length = this.elementData.length * 2;
        if (length == 0) {
            length = 1;
        }
        Entry<K, V>[] newData = newEntryArray(length);
        for (int i = 0; i < this.elementData.length; i++) {
            Entry<K, V> entry = this.elementData[i];
            while (entry != null) {
                int index = entry.isNull ? 0 : (entry.hash & Integer.MAX_VALUE) % length;
                Entry<K, V> next = entry.next;
                entry.next = newData[index];
                newData[index] = entry;
                entry = next;
            }
        }
        this.elementData = newData;
        computeMaxSize();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        putAllImpl(map);
    }

    @Override
    public V remove(Object key) {
        Entry<K, V> entry;
        poll();
        int index = 0;
        Entry<K, V> last = null;
        if (key != null) {
            index = (Collections.secondaryHash(key) & Integer.MAX_VALUE) % this.elementData.length;
            entry = this.elementData[index];
            while (entry != null && !key.equals(entry.get())) {
                last = entry;
                entry = entry.next;
            }
        } else {
            entry = this.elementData[0];
            while (entry != null && !entry.isNull) {
                last = entry;
                entry = entry.next;
            }
        }
        if (entry != null) {
            this.modCount++;
            if (last == null) {
                this.elementData[index] = entry.next;
            } else {
                last.next = entry.next;
            }
            this.elementCount--;
            return entry.value;
        }
        return null;
    }

    @Override
    public int size() {
        poll();
        return this.elementCount;
    }

    private void putAllImpl(Map<? extends K, ? extends V> map) {
        if (map.entrySet() != null) {
            super.putAll(map);
        }
    }
}
