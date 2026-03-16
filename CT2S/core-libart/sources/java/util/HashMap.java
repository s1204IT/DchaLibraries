package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Map;

public class HashMap<K, V> extends AbstractMap<K, V> implements Cloneable, Serializable {
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1073741824;
    private static final int MINIMUM_CAPACITY = 4;
    private static final long serialVersionUID = 362498820763181265L;
    transient HashMapEntry<K, V> entryForNullKey;
    private transient Set<Map.Entry<K, V>> entrySet;
    private transient Set<K> keySet;
    transient int modCount;
    transient int size;
    transient HashMapEntry<K, V>[] table;
    private transient int threshold;
    private transient Collection<V> values;
    private static final Map.Entry[] EMPTY_TABLE = new HashMapEntry[2];
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("loadFactor", Float.TYPE)};

    public HashMap() {
        this.table = (HashMapEntry[]) EMPTY_TABLE;
        this.threshold = -1;
    }

    public HashMap(int capacity) {
        int capacity2;
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity: " + capacity);
        }
        if (capacity == 0) {
            HashMapEntry<K, V>[] tab = (HashMapEntry[]) EMPTY_TABLE;
            this.table = tab;
            this.threshold = -1;
        } else {
            if (capacity < 4) {
                capacity2 = 4;
            } else if (capacity > 1073741824) {
                capacity2 = 1073741824;
            } else {
                capacity2 = Collections.roundUpToPowerOfTwo(capacity);
            }
            makeTable(capacity2);
        }
    }

    public HashMap(int capacity, float loadFactor) {
        this(capacity);
        if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor: " + loadFactor);
        }
    }

    public HashMap(Map<? extends K, ? extends V> map) {
        this(capacityForInitSize(map.size()));
        constructorPutAll(map);
    }

    final void constructorPutAll(Map<? extends K, ? extends V> map) {
        if (this.table == EMPTY_TABLE) {
            doubleCapacity();
        }
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            constructorPut(e.getKey(), e.getValue());
        }
    }

    static int capacityForInitSize(int size) {
        int result = (size >> 1) + size;
        if (((-1073741824) & result) == 0) {
            return result;
        }
        return 1073741824;
    }

    @Override
    public Object clone() {
        try {
            HashMap<K, V> result = (HashMap) super.clone();
            result.makeTable(this.table.length);
            result.entryForNullKey = null;
            result.size = 0;
            result.keySet = null;
            result.entrySet = null;
            result.values = null;
            result.init();
            result.constructorPutAll(this);
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    void init() {
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            HashMapEntry<K, V> e = this.entryForNullKey;
            if (e == null) {
                return null;
            }
            return e.value;
        }
        int hash = Collections.secondaryHash(key);
        HashMapEntry<K, V>[] tab = this.table;
        for (HashMapEntry<K, V> e2 = tab[(tab.length - 1) & hash]; e2 != null; e2 = e2.next) {
            K eKey = e2.key;
            if (eKey == key || (e2.hash == hash && key.equals(eKey))) {
                return e2.value;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return this.entryForNullKey != null;
        }
        int hash = Collections.secondaryHash(key);
        HashMapEntry<K, V>[] tab = this.table;
        for (HashMapEntry<K, V> e = tab[(tab.length - 1) & hash]; e != null; e = e.next) {
            K eKey = e.key;
            if (eKey == key) {
                return true;
            }
            if (e.hash == hash && key.equals(eKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        HashMapEntry<K, V>[] hashMapEntryArr = this.table;
        if (value == null) {
            for (HashMapEntry<K, V> hashMapEntry : hashMapEntryArr) {
                for (; hashMapEntry != null; hashMapEntry = hashMapEntry.next) {
                    if (hashMapEntry.value == null) {
                        return true;
                    }
                }
            }
            return this.entryForNullKey != null && this.entryForNullKey.value == null;
        }
        for (HashMapEntry<K, V> hashMapEntry2 : hashMapEntryArr) {
            for (; hashMapEntry2 != null; hashMapEntry2 = hashMapEntry2.next) {
                if (value.equals(hashMapEntry2.value)) {
                    return true;
                }
            }
        }
        return this.entryForNullKey != null && value.equals(this.entryForNullKey.value);
    }

    @Override
    public V put(K key, V value) {
        if (key == null) {
            return putValueForNullKey(value);
        }
        int hash = Collections.secondaryHash(key);
        HashMapEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        for (HashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                preModify(e);
                V v = e.value;
                e.value = value;
                return v;
            }
        }
        this.modCount++;
        int i = this.size;
        this.size = i + 1;
        if (i > this.threshold) {
            index = hash & (doubleCapacity().length - 1);
        }
        addNewEntry(key, value, hash, index);
        return null;
    }

    private V putValueForNullKey(V value) {
        HashMapEntry<K, V> entry = this.entryForNullKey;
        if (entry == null) {
            addNewEntryForNullKey(value);
            this.size++;
            this.modCount++;
            return null;
        }
        preModify(entry);
        V v = entry.value;
        entry.value = value;
        return v;
    }

    void preModify(HashMapEntry<K, V> e) {
    }

    private void constructorPut(K key, V value) {
        if (key == null) {
            HashMapEntry<K, V> entry = this.entryForNullKey;
            if (entry == null) {
                this.entryForNullKey = constructorNewEntry(null, value, 0, null);
                this.size++;
                return;
            } else {
                entry.value = value;
                return;
            }
        }
        int hash = Collections.secondaryHash(key);
        HashMapEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        HashMapEntry<K, V> first = tab[index];
        for (HashMapEntry<K, V> e = first; e != null; e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                e.value = value;
                return;
            }
        }
        tab[index] = constructorNewEntry(key, value, hash, first);
        this.size++;
    }

    void addNewEntry(K key, V value, int hash, int index) {
        this.table[index] = new HashMapEntry<>(key, value, hash, this.table[index]);
    }

    void addNewEntryForNullKey(V value) {
        this.entryForNullKey = new HashMapEntry<>(null, value, 0, null);
    }

    HashMapEntry<K, V> constructorNewEntry(K key, V value, int hash, HashMapEntry<K, V> first) {
        return new HashMapEntry<>(key, value, hash, first);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        ensureCapacity(map.size());
        super.putAll(map);
    }

    private void ensureCapacity(int numMappings) {
        int newCapacity = Collections.roundUpToPowerOfTwo(capacityForInitSize(numMappings));
        HashMapEntry<K, V>[] oldTable = this.table;
        int oldCapacity = oldTable.length;
        if (newCapacity > oldCapacity) {
            if (newCapacity == oldCapacity * 2) {
                doubleCapacity();
                return;
            }
            HashMapEntry<K, V>[] newTable = makeTable(newCapacity);
            if (this.size != 0) {
                int newMask = newCapacity - 1;
                for (HashMapEntry<K, V> e : oldTable) {
                    while (e != null) {
                        HashMapEntry<K, V> oldNext = e.next;
                        int newIndex = e.hash & newMask;
                        HashMapEntry<K, V> newNext = newTable[newIndex];
                        newTable[newIndex] = e;
                        e.next = newNext;
                        e = oldNext;
                    }
                }
            }
        }
    }

    private HashMapEntry<K, V>[] makeTable(int newCapacity) {
        HashMapEntry<K, V>[] newTable = new HashMapEntry[newCapacity];
        this.table = newTable;
        this.threshold = (newCapacity >> 1) + (newCapacity >> 2);
        return newTable;
    }

    private HashMapEntry<K, V>[] doubleCapacity() {
        HashMapEntry<K, V>[] oldTable = this.table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == 1073741824) {
            return oldTable;
        }
        int newCapacity = oldCapacity * 2;
        HashMapEntry<K, V>[] newTable = makeTable(newCapacity);
        if (this.size != 0) {
            for (int j = 0; j < oldCapacity; j++) {
                HashMapEntry<K, V> e = oldTable[j];
                if (e != null) {
                    int highBit = e.hash & oldCapacity;
                    HashMapEntry<K, V> broken = null;
                    newTable[j | highBit] = e;
                    for (HashMapEntry<K, V> n = e.next; n != null; n = n.next) {
                        int nextHighBit = n.hash & oldCapacity;
                        if (nextHighBit != highBit) {
                            if (broken == null) {
                                newTable[j | nextHighBit] = n;
                            } else {
                                broken.next = n;
                            }
                            broken = e;
                            highBit = nextHighBit;
                        }
                        e = n;
                    }
                    if (broken != null) {
                        broken.next = null;
                    }
                }
            }
            return newTable;
        }
        return newTable;
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            return removeNullKey();
        }
        int hash = Collections.secondaryHash(key);
        HashMapEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        HashMapEntry<K, V> prev = null;
        for (HashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
            if (e.hash != hash || !key.equals(e.key)) {
                prev = e;
            } else {
                if (prev == null) {
                    tab[index] = e.next;
                } else {
                    prev.next = e.next;
                }
                this.modCount++;
                this.size--;
                postRemove(e);
                return e.value;
            }
        }
        return null;
    }

    private V removeNullKey() {
        HashMapEntry<K, V> e = this.entryForNullKey;
        if (e == null) {
            return null;
        }
        this.entryForNullKey = null;
        this.modCount++;
        this.size--;
        postRemove(e);
        return e.value;
    }

    void postRemove(HashMapEntry<K, V> e) {
    }

    @Override
    public void clear() {
        if (this.size != 0) {
            Arrays.fill(this.table, (Object) null);
            this.entryForNullKey = null;
            this.modCount++;
            this.size = 0;
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        Set<K> ks2 = new KeySet();
        this.keySet = ks2;
        return ks2;
    }

    @Override
    public Collection<V> values() {
        Collection<V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        Collection<V> vs2 = new Values();
        this.values = vs2;
        return vs2;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es = this.entrySet;
        if (es != null) {
            return es;
        }
        Set<Map.Entry<K, V>> es2 = new EntrySet();
        this.entrySet = es2;
        return es2;
    }

    static class HashMapEntry<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        HashMapEntry<K, V> next;
        V value;

        HashMapEntry(K key, V value, int hash, HashMapEntry<K, V> next) {
            this.key = key;
            this.value = value;
            this.hash = hash;
            this.next = next;
        }

        @Override
        public final K getKey() {
            return this.key;
        }

        @Override
        public final V getValue() {
            return this.value;
        }

        @Override
        public final V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            return libcore.util.Objects.equal(e.getKey(), this.key) && libcore.util.Objects.equal(e.getValue(), this.value);
        }

        @Override
        public final int hashCode() {
            return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public final String toString() {
            return this.key + "=" + this.value;
        }
    }

    private abstract class HashIterator {
        int expectedModCount;
        HashMapEntry<K, V> lastEntryReturned;
        HashMapEntry<K, V> nextEntry;
        int nextIndex;

        HashIterator() {
            this.nextEntry = HashMap.this.entryForNullKey;
            this.expectedModCount = HashMap.this.modCount;
            if (this.nextEntry == null) {
                HashMapEntry<K, V>[] tab = HashMap.this.table;
                HashMapEntry<K, V> next = null;
                while (next == null && this.nextIndex < tab.length) {
                    int i = this.nextIndex;
                    this.nextIndex = i + 1;
                    next = tab[i];
                }
                this.nextEntry = next;
            }
        }

        public boolean hasNext() {
            return this.nextEntry != null;
        }

        HashMapEntry<K, V> nextEntry() {
            if (HashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (this.nextEntry == null) {
                throw new NoSuchElementException();
            }
            HashMapEntry<K, V> entryToReturn = this.nextEntry;
            HashMapEntry<K, V>[] tab = HashMap.this.table;
            HashMapEntry<K, V> next = entryToReturn.next;
            while (next == null && this.nextIndex < tab.length) {
                int i = this.nextIndex;
                this.nextIndex = i + 1;
                next = tab[i];
            }
            this.nextEntry = next;
            this.lastEntryReturned = entryToReturn;
            return entryToReturn;
        }

        public void remove() {
            if (this.lastEntryReturned == null) {
                throw new IllegalStateException();
            }
            if (HashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            HashMap.this.remove(this.lastEntryReturned.key);
            this.lastEntryReturned = null;
            this.expectedModCount = HashMap.this.modCount;
        }
    }

    private final class KeyIterator extends HashMap<K, V>.HashIterator implements Iterator<K> {
        private KeyIterator() {
            super();
        }

        @Override
        public K next() {
            return nextEntry().key;
        }
    }

    private final class ValueIterator extends HashMap<K, V>.HashIterator implements Iterator<V> {
        private ValueIterator() {
            super();
        }

        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private final class EntryIterator extends HashMap<K, V>.HashIterator implements Iterator<Map.Entry<K, V>> {
        private EntryIterator() {
            super();
        }

        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    private boolean containsMapping(Object key, Object value) {
        if (key == null) {
            HashMapEntry<K, V> e = this.entryForNullKey;
            return e != null && libcore.util.Objects.equal(value, e.value);
        }
        int hash = Collections.secondaryHash(key);
        HashMapEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        for (HashMapEntry<K, V> e2 = tab[index]; e2 != null; e2 = e2.next) {
            if (e2.hash == hash && key.equals(e2.key)) {
                return libcore.util.Objects.equal(value, e2.value);
            }
        }
        return false;
    }

    private boolean removeMapping(Object key, Object value) {
        if (key == null) {
            HashMapEntry<K, V> e = this.entryForNullKey;
            if (e == null || !libcore.util.Objects.equal(value, e.value)) {
                return false;
            }
            this.entryForNullKey = null;
            this.modCount++;
            this.size--;
            postRemove(e);
            return true;
        }
        int hash = Collections.secondaryHash(key);
        HashMapEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        HashMapEntry<K, V> prev = null;
        for (HashMapEntry<K, V> e2 = tab[index]; e2 != null; e2 = e2.next) {
            if (e2.hash != hash || !key.equals(e2.key)) {
                prev = e2;
            } else {
                if (!libcore.util.Objects.equal(value, e2.value)) {
                    return false;
                }
                if (prev == null) {
                    tab[index] = e2.next;
                } else {
                    prev.next = e2.next;
                }
                this.modCount++;
                this.size--;
                postRemove(e2);
                return true;
            }
        }
        return false;
    }

    Iterator<K> newKeyIterator() {
        return new KeyIterator();
    }

    Iterator<V> newValueIterator() {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new EntryIterator();
    }

    private final class KeySet extends AbstractSet<K> {
        private KeySet() {
        }

        @Override
        public Iterator<K> iterator() {
            return HashMap.this.newKeyIterator();
        }

        @Override
        public int size() {
            return HashMap.this.size;
        }

        @Override
        public boolean isEmpty() {
            return HashMap.this.size == 0;
        }

        @Override
        public boolean contains(Object o) {
            return HashMap.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            int oldSize = HashMap.this.size;
            HashMap.this.remove(o);
            return HashMap.this.size != oldSize;
        }

        @Override
        public void clear() {
            HashMap.this.clear();
        }
    }

    private final class Values extends AbstractCollection<V> {
        private Values() {
        }

        @Override
        public Iterator<V> iterator() {
            return HashMap.this.newValueIterator();
        }

        @Override
        public int size() {
            return HashMap.this.size;
        }

        @Override
        public boolean isEmpty() {
            return HashMap.this.size == 0;
        }

        @Override
        public boolean contains(Object o) {
            return HashMap.this.containsValue(o);
        }

        @Override
        public void clear() {
            HashMap.this.clear();
        }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        private EntrySet() {
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return HashMap.this.newEntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            return HashMap.this.containsMapping(e.getKey(), e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            return HashMap.this.removeMapping(e.getKey(), e.getValue());
        }

        @Override
        public int size() {
            return HashMap.this.size;
        }

        @Override
        public boolean isEmpty() {
            return HashMap.this.size == 0;
        }

        @Override
        public void clear() {
            HashMap.this.clear();
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("loadFactor", DEFAULT_LOAD_FACTOR);
        stream.writeFields();
        stream.writeInt(this.table.length);
        stream.writeInt(this.size);
        for (Map.Entry<K, V> e : entrySet()) {
            stream.writeObject(e.getKey());
            stream.writeObject(e.getValue());
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int capacity;
        stream.defaultReadObject();
        int capacity2 = stream.readInt();
        if (capacity2 < 0) {
            throw new InvalidObjectException("Capacity: " + capacity2);
        }
        if (capacity2 < 4) {
            capacity = 4;
        } else if (capacity2 > 1073741824) {
            capacity = 1073741824;
        } else {
            capacity = Collections.roundUpToPowerOfTwo(capacity2);
        }
        makeTable(capacity);
        int size = stream.readInt();
        if (size < 0) {
            throw new InvalidObjectException("Size: " + size);
        }
        init();
        for (int i = 0; i < size; i++) {
            constructorPut(stream.readObject(), stream.readObject());
        }
    }
}
