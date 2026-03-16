package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Map;

public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V>, Cloneable, Serializable {
    private static final int CHARS_PER_ENTRY = 15;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1073741824;
    private static final int MINIMUM_CAPACITY = 4;
    private static final long serialVersionUID = 1421746759512286392L;
    private transient Set<Map.Entry<K, V>> entrySet;
    private transient Set<K> keySet;
    private transient int modCount;
    private transient int size;
    private transient HashtableEntry<K, V>[] table;
    private transient int threshold;
    private transient Collection<V> values;
    private static final Map.Entry[] EMPTY_TABLE = new HashtableEntry[2];
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("threshold", Integer.TYPE), new ObjectStreamField("loadFactor", Float.TYPE)};

    public Hashtable() {
        this.table = (HashtableEntry[]) EMPTY_TABLE;
        this.threshold = -1;
    }

    public Hashtable(int capacity) {
        int capacity2;
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity: " + capacity);
        }
        if (capacity == 0) {
            HashtableEntry<K, V>[] tab = (HashtableEntry[]) EMPTY_TABLE;
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

    public Hashtable(int capacity, float loadFactor) {
        this(capacity);
        if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor: " + loadFactor);
        }
    }

    public Hashtable(Map<? extends K, ? extends V> map) {
        this(capacityForInitSize(map.size()));
        constructorPutAll(map);
    }

    private void constructorPutAll(Map<? extends K, ? extends V> map) {
        if (this.table == EMPTY_TABLE) {
            doubleCapacity();
        }
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            constructorPut(e.getKey(), e.getValue());
        }
    }

    private static int capacityForInitSize(int size) {
        int result = (size >> 1) + size;
        if (((-1073741824) & result) == 0) {
            return result;
        }
        return 1073741824;
    }

    public synchronized Object clone() {
        Hashtable<K, V> result;
        try {
            result = (Hashtable) super.clone();
            result.makeTable(this.table.length);
            result.size = 0;
            result.keySet = null;
            result.entrySet = null;
            result.values = null;
            result.constructorPutAll(this);
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        return result;
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public synchronized int size() {
        return this.size;
    }

    @Override
    public synchronized V get(Object key) {
        V v;
        int hash = Collections.secondaryHash(key);
        HashtableEntry<K, V>[] tab = this.table;
        for (HashtableEntry<K, V> e = tab[(tab.length - 1) & hash]; e != null; e = e.next) {
            K eKey = e.key;
            if (eKey == key || (e.hash == hash && key.equals(eKey))) {
                v = e.value;
                break;
            }
        }
        v = null;
        return v;
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        boolean z;
        int hash = Collections.secondaryHash(key);
        HashtableEntry<K, V>[] tab = this.table;
        for (HashtableEntry<K, V> e = tab[(tab.length - 1) & hash]; e != null; e = e.next) {
            K eKey = e.key;
            if (eKey == key || (e.hash == hash && key.equals(eKey))) {
                z = true;
                break;
            }
        }
        z = false;
        return z;
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        boolean z;
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        HashtableEntry<K, V>[] hashtableEntryArr = this.table;
        int len = hashtableEntryArr.length;
        int i = 0;
        loop0: while (true) {
            if (i < len) {
                for (HashtableEntry<K, V> hashtableEntry = hashtableEntryArr[i]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
                    if (value.equals(hashtableEntry.value)) {
                        z = true;
                        break loop0;
                    }
                }
                i++;
            } else {
                z = false;
                break;
            }
        }
        return z;
    }

    public boolean contains(Object value) {
        return containsValue(value);
    }

    @Override
    public synchronized V put(K key, V value) {
        V oldValue;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        int hash = Collections.secondaryHash(key);
        HashtableEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        HashtableEntry<K, V> first = tab[index];
        HashtableEntry<K, V> e = first;
        while (true) {
            if (e != null) {
                if (e.hash != hash || !key.equals(e.key)) {
                    e = e.next;
                } else {
                    oldValue = e.value;
                    e.value = value;
                    break;
                }
            } else {
                this.modCount++;
                int i = this.size;
                this.size = i + 1;
                if (i > this.threshold) {
                    rehash();
                    tab = doubleCapacity();
                    index = hash & (tab.length - 1);
                    first = tab[index];
                }
                tab[index] = new HashtableEntry<>(key, value, hash, first);
                oldValue = null;
            }
        }
        return oldValue;
    }

    private void constructorPut(K key, V value) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        int hash = Collections.secondaryHash(key);
        HashtableEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        HashtableEntry<K, V> first = tab[index];
        for (HashtableEntry<K, V> e = first; e != null; e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                e.value = value;
                return;
            }
        }
        tab[index] = new HashtableEntry<>(key, value, hash, first);
        this.size++;
    }

    public synchronized void putAll(Map<? extends K, ? extends V> map) {
        ensureCapacity(map.size());
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    private void ensureCapacity(int numMappings) {
        int newCapacity = Collections.roundUpToPowerOfTwo(capacityForInitSize(numMappings));
        HashtableEntry<K, V>[] oldTable = this.table;
        int oldCapacity = oldTable.length;
        if (newCapacity > oldCapacity) {
            rehash();
            if (newCapacity == oldCapacity * 2) {
                doubleCapacity();
                return;
            }
            HashtableEntry<K, V>[] newTable = makeTable(newCapacity);
            if (this.size != 0) {
                int newMask = newCapacity - 1;
                for (HashtableEntry<K, V> e : oldTable) {
                    while (e != null) {
                        HashtableEntry<K, V> oldNext = e.next;
                        int newIndex = e.hash & newMask;
                        HashtableEntry<K, V> newNext = newTable[newIndex];
                        newTable[newIndex] = e;
                        e.next = newNext;
                        e = oldNext;
                    }
                }
            }
        }
    }

    protected void rehash() {
    }

    private HashtableEntry<K, V>[] makeTable(int newCapacity) {
        HashtableEntry<K, V>[] newTable = new HashtableEntry[newCapacity];
        this.table = newTable;
        this.threshold = (newCapacity >> 1) + (newCapacity >> 2);
        return newTable;
    }

    private HashtableEntry<K, V>[] doubleCapacity() {
        HashtableEntry<K, V>[] oldTable = this.table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == 1073741824) {
            return oldTable;
        }
        int newCapacity = oldCapacity * 2;
        HashtableEntry<K, V>[] newTable = makeTable(newCapacity);
        if (this.size != 0) {
            for (int j = 0; j < oldCapacity; j++) {
                HashtableEntry<K, V> e = oldTable[j];
                if (e != null) {
                    int highBit = e.hash & oldCapacity;
                    HashtableEntry<K, V> broken = null;
                    newTable[j | highBit] = e;
                    for (HashtableEntry<K, V> n = e.next; n != null; n = n.next) {
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
    public synchronized V remove(Object key) {
        V v;
        int hash = Collections.secondaryHash(key);
        HashtableEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        HashtableEntry<K, V> e = tab[index];
        HashtableEntry<K, V> prev = null;
        while (true) {
            if (e != null) {
                if (e.hash == hash && key.equals(e.key)) {
                    break;
                }
                prev = e;
                e = e.next;
            } else {
                v = null;
                break;
            }
        }
        return v;
    }

    public synchronized void clear() {
        if (this.size != 0) {
            Arrays.fill(this.table, (Object) null);
            this.modCount++;
            this.size = 0;
        }
    }

    public synchronized Set<K> keySet() {
        Set<K> ks;
        ks = this.keySet;
        if (ks == null) {
            ks = new KeySet();
            this.keySet = ks;
        }
        return ks;
    }

    public synchronized Collection<V> values() {
        Collection<V> vs;
        vs = this.values;
        if (vs == null) {
            vs = new Values();
            this.values = vs;
        }
        return vs;
    }

    public synchronized Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        es = this.entrySet;
        if (es == null) {
            es = new EntrySet();
            this.entrySet = es;
        }
        return es;
    }

    @Override
    public synchronized Enumeration<K> keys() {
        return new KeyEnumeration();
    }

    @Override
    public synchronized Enumeration<V> elements() {
        return new ValueEnumeration();
    }

    private static class HashtableEntry<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        HashtableEntry<K, V> next;
        V value;

        HashtableEntry(K key, V value, int hash, HashtableEntry<K, V> next) {
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
            if (value == null) {
                throw new NullPointerException("value == null");
            }
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
            return this.key.equals(e.getKey()) && this.value.equals(e.getValue());
        }

        @Override
        public final int hashCode() {
            return this.key.hashCode() ^ this.value.hashCode();
        }

        public final String toString() {
            return this.key + "=" + this.value;
        }
    }

    private abstract class HashIterator {
        int expectedModCount;
        HashtableEntry<K, V> lastEntryReturned;
        HashtableEntry<K, V> nextEntry;
        int nextIndex;

        HashIterator() {
            this.expectedModCount = Hashtable.this.modCount;
            HashtableEntry<K, V>[] tab = Hashtable.this.table;
            HashtableEntry<K, V> next = null;
            while (next == null && this.nextIndex < tab.length) {
                int i = this.nextIndex;
                this.nextIndex = i + 1;
                next = tab[i];
            }
            this.nextEntry = next;
        }

        public boolean hasNext() {
            return this.nextEntry != null;
        }

        HashtableEntry<K, V> nextEntry() {
            if (Hashtable.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (this.nextEntry == null) {
                throw new NoSuchElementException();
            }
            HashtableEntry<K, V> entryToReturn = this.nextEntry;
            HashtableEntry<K, V>[] tab = Hashtable.this.table;
            HashtableEntry<K, V> next = entryToReturn.next;
            while (next == null && this.nextIndex < tab.length) {
                int i = this.nextIndex;
                this.nextIndex = i + 1;
                next = tab[i];
            }
            this.nextEntry = next;
            this.lastEntryReturned = entryToReturn;
            return entryToReturn;
        }

        HashtableEntry<K, V> nextEntryNotFailFast() {
            if (this.nextEntry == null) {
                throw new NoSuchElementException();
            }
            HashtableEntry<K, V> entryToReturn = this.nextEntry;
            HashtableEntry<K, V>[] tab = Hashtable.this.table;
            HashtableEntry<K, V> next = entryToReturn.next;
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
            if (this.lastEntryReturned != null) {
                if (Hashtable.this.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                Hashtable.this.remove(this.lastEntryReturned.key);
                this.lastEntryReturned = null;
                this.expectedModCount = Hashtable.this.modCount;
                return;
            }
            throw new IllegalStateException();
        }
    }

    private final class KeyIterator extends Hashtable<K, V>.HashIterator implements Iterator<K> {
        private KeyIterator() {
            super();
        }

        @Override
        public K next() {
            return nextEntry().key;
        }
    }

    private final class ValueIterator extends Hashtable<K, V>.HashIterator implements Iterator<V> {
        private ValueIterator() {
            super();
        }

        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private final class EntryIterator extends Hashtable<K, V>.HashIterator implements Iterator<Map.Entry<K, V>> {
        private EntryIterator() {
            super();
        }

        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    private final class KeyEnumeration extends Hashtable<K, V>.HashIterator implements Enumeration<K> {
        private KeyEnumeration() {
            super();
        }

        @Override
        public boolean hasMoreElements() {
            return hasNext();
        }

        @Override
        public K nextElement() {
            return nextEntryNotFailFast().key;
        }
    }

    private final class ValueEnumeration extends Hashtable<K, V>.HashIterator implements Enumeration<V> {
        private ValueEnumeration() {
            super();
        }

        @Override
        public boolean hasMoreElements() {
            return hasNext();
        }

        @Override
        public V nextElement() {
            return nextEntryNotFailFast().value;
        }
    }

    private synchronized boolean containsMapping(Object key, Object value) {
        boolean zEquals;
        int hash = Collections.secondaryHash(key);
        HashtableEntry<K, V>[] tab = this.table;
        int index = hash & (tab.length - 1);
        HashtableEntry<K, V> e = tab[index];
        while (true) {
            if (e != null) {
                if (e.hash == hash && e.key.equals(key)) {
                    break;
                }
                e = e.next;
            } else {
                zEquals = false;
                break;
            }
        }
        return zEquals;
    }

    private synchronized boolean removeMapping(Object key, Object value) {
        boolean z = false;
        synchronized (this) {
            int hash = Collections.secondaryHash(key);
            HashtableEntry<K, V>[] tab = this.table;
            int index = hash & (tab.length - 1);
            HashtableEntry<K, V> e = tab[index];
            HashtableEntry<K, V> prev = null;
            while (true) {
                if (e != null) {
                    if (e.hash == hash && e.key.equals(key)) {
                        break;
                    }
                    prev = e;
                    e = e.next;
                } else {
                    break;
                }
            }
        }
        return z;
    }

    @Override
    public synchronized boolean equals(Object object) {
        boolean z;
        if (object instanceof Map) {
            z = entrySet().equals(((Map) object).entrySet());
        }
        return z;
    }

    @Override
    public synchronized int hashCode() {
        int result;
        result = 0;
        for (Map.Entry<K, V> e : entrySet()) {
            K key = e.getKey();
            V value = e.getValue();
            if (key != this && value != this) {
                result += (value != null ? value.hashCode() : 0) ^ (key != null ? key.hashCode() : 0);
            }
        }
        return result;
    }

    public synchronized String toString() {
        StringBuilder result;
        result = new StringBuilder(this.size * 15);
        result.append('{');
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        boolean hasMore = i.hasNext();
        while (hasMore) {
            Map.Entry<K, V> entry = i.next();
            K key = entry.getKey();
            result.append(key == this ? "(this Map)" : key.toString());
            result.append('=');
            V value = entry.getValue();
            result.append(value == this ? "(this Map)" : value.toString());
            hasMore = i.hasNext();
            if (hasMore) {
                result.append(", ");
            }
        }
        result.append('}');
        return result.toString();
    }

    private final class KeySet extends AbstractSet<K> {
        private KeySet() {
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return Hashtable.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return Hashtable.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            boolean z;
            synchronized (Hashtable.this) {
                int oldSize = Hashtable.this.size;
                Hashtable.this.remove(o);
                z = Hashtable.this.size != oldSize;
            }
            return z;
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean zRemoveAll;
            synchronized (Hashtable.this) {
                zRemoveAll = super.removeAll(collection);
            }
            return zRemoveAll;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean zRetainAll;
            synchronized (Hashtable.this) {
                zRetainAll = super.retainAll(collection);
            }
            return zRetainAll;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            boolean zContainsAll;
            synchronized (Hashtable.this) {
                zContainsAll = super.containsAll(collection);
            }
            return zContainsAll;
        }

        @Override
        public boolean equals(Object object) {
            boolean zEquals;
            synchronized (Hashtable.this) {
                zEquals = super.equals(object);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (Hashtable.this) {
                iHashCode = super.hashCode();
            }
            return iHashCode;
        }

        @Override
        public String toString() {
            String string;
            synchronized (Hashtable.this) {
                string = super.toString();
            }
            return string;
        }

        @Override
        public Object[] toArray() {
            Object[] array;
            synchronized (Hashtable.this) {
                array = super.toArray();
            }
            return array;
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            T[] tArr2;
            synchronized (Hashtable.this) {
                tArr2 = (T[]) super.toArray(tArr);
            }
            return tArr2;
        }
    }

    private final class Values extends AbstractCollection<V> {
        private Values() {
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return Hashtable.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return Hashtable.this.containsValue(o);
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            boolean zContainsAll;
            synchronized (Hashtable.this) {
                zContainsAll = super.containsAll(collection);
            }
            return zContainsAll;
        }

        @Override
        public String toString() {
            String string;
            synchronized (Hashtable.this) {
                string = super.toString();
            }
            return string;
        }

        @Override
        public Object[] toArray() {
            Object[] array;
            synchronized (Hashtable.this) {
                array = super.toArray();
            }
            return array;
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            T[] tArr2;
            synchronized (Hashtable.this) {
                tArr2 = (T[]) super.toArray(tArr);
            }
            return tArr2;
        }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        private EntrySet() {
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            return Hashtable.this.containsMapping(e.getKey(), e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            return Hashtable.this.removeMapping(e.getKey(), e.getValue());
        }

        @Override
        public int size() {
            return Hashtable.this.size();
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean zRemoveAll;
            synchronized (Hashtable.this) {
                zRemoveAll = super.removeAll(collection);
            }
            return zRemoveAll;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean zRetainAll;
            synchronized (Hashtable.this) {
                zRetainAll = super.retainAll(collection);
            }
            return zRetainAll;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            boolean zContainsAll;
            synchronized (Hashtable.this) {
                zContainsAll = super.containsAll(collection);
            }
            return zContainsAll;
        }

        @Override
        public boolean equals(Object object) {
            boolean zEquals;
            synchronized (Hashtable.this) {
                zEquals = super.equals(object);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            return Hashtable.this.hashCode();
        }

        @Override
        public String toString() {
            String string;
            synchronized (Hashtable.this) {
                string = super.toString();
            }
            return string;
        }

        @Override
        public Object[] toArray() {
            Object[] array;
            synchronized (Hashtable.this) {
                array = super.toArray();
            }
            return array;
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            T[] tArr2;
            synchronized (Hashtable.this) {
                tArr2 = (T[]) super.toArray(tArr);
            }
            return tArr2;
        }
    }

    private synchronized void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("threshold", (int) (this.table.length * DEFAULT_LOAD_FACTOR));
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
        for (int i = 0; i < size; i++) {
            constructorPut(stream.readObject(), stream.readObject());
        }
    }
}
