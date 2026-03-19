package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V>, Cloneable, Serializable {
    private static final int ENTRIES = 2;
    private static final int KEYS = 0;
    private static final int MAX_ARRAY_SIZE = 2147483639;
    private static final int VALUES = 1;
    private static final long serialVersionUID = 1421746759512286392L;
    private transient int count;
    private volatile transient Set<Map.Entry<K, V>> entrySet;
    private volatile transient Set<K> keySet;
    private float loadFactor;
    private transient int modCount;
    private transient HashtableEntry<?, ?>[] table;
    private int threshold;
    private volatile transient Collection<V> values;

    static int access$210(Hashtable hashtable) {
        int i = hashtable.count;
        hashtable.count = i - 1;
        return i;
    }

    static int access$508(Hashtable hashtable) {
        int i = hashtable.modCount;
        hashtable.modCount = i + 1;
        return i;
    }

    public Hashtable(int i, float f) {
        this.modCount = 0;
        if (i < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + i);
        }
        if (f <= 0.0f || Float.isNaN(f)) {
            throw new IllegalArgumentException("Illegal Load: " + f);
        }
        i = i == 0 ? 1 : i;
        this.loadFactor = f;
        this.table = new HashtableEntry[i];
        this.threshold = Math.min(i, 2147483640);
    }

    public Hashtable(int i) {
        this(i, 0.75f);
    }

    public Hashtable() {
        this(11, 0.75f);
    }

    public Hashtable(Map<? extends K, ? extends V> map) {
        this(Math.max(2 * map.size(), 11), 0.75f);
        putAll(map);
    }

    @Override
    public synchronized int size() {
        return this.count;
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.count == 0;
    }

    @Override
    public synchronized Enumeration<K> keys() {
        return (Enumeration<K>) getEnumeration(0);
    }

    @Override
    public synchronized Enumeration<V> elements() {
        return (Enumeration<V>) getEnumeration(1);
    }

    public synchronized boolean contains(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        HashtableEntry[] hashtableEntryArr = this.table;
        int length = hashtableEntryArr.length;
        while (true) {
            int i = length - 1;
            if (length > 0) {
                for (HashtableEntry hashtableEntry = hashtableEntryArr[i]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
                    if (hashtableEntry.value.equals(obj)) {
                        return true;
                    }
                }
                length = i;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean containsValue(Object obj) {
        return contains(obj);
    }

    @Override
    public synchronized boolean containsKey(Object obj) {
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = obj.hashCode();
        for (HashtableEntry hashtableEntry = hashtableEntryArr[(Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(obj)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized V get(Object obj) {
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = obj.hashCode();
        for (HashtableEntry hashtableEntry = hashtableEntryArr[(Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(obj)) {
                return hashtableEntry.value;
            }
        }
        return null;
    }

    protected void rehash() {
        int length = this.table.length;
        HashtableEntry[] hashtableEntryArr = this.table;
        int i = (length << 1) + 1;
        if (i - MAX_ARRAY_SIZE > 0) {
            if (length == MAX_ARRAY_SIZE) {
                return;
            } else {
                i = MAX_ARRAY_SIZE;
            }
        }
        HashtableEntry<?, ?>[] hashtableEntryArr2 = new HashtableEntry[i];
        this.modCount++;
        this.threshold = (int) Math.min(i * this.loadFactor, 2.1474836E9f);
        this.table = hashtableEntryArr2;
        while (true) {
            int i2 = length - 1;
            if (length > 0) {
                HashtableEntry hashtableEntry = hashtableEntryArr[i2];
                while (hashtableEntry != null) {
                    HashtableEntry<K, V> hashtableEntry2 = hashtableEntry.next;
                    int i3 = (hashtableEntry.hash & Integer.MAX_VALUE) % i;
                    hashtableEntry.next = (HashtableEntry<K, V>) hashtableEntryArr2[i3];
                    hashtableEntryArr2[i3] = hashtableEntry;
                    hashtableEntry = hashtableEntry2;
                }
                length = i2;
            } else {
                return;
            }
        }
    }

    private void addEntry(int i, K k, V v, int i2) {
        this.modCount++;
        HashtableEntry<?, ?>[] hashtableEntryArr = this.table;
        if (this.count >= this.threshold) {
            rehash();
            hashtableEntryArr = this.table;
            i = k.hashCode();
            i2 = (Integer.MAX_VALUE & i) % hashtableEntryArr.length;
        }
        hashtableEntryArr[i2] = new HashtableEntry<>(i, k, v, hashtableEntryArr[i2]);
        this.count++;
    }

    @Override
    public synchronized V put(K k, V v) {
        if (v == null) {
            throw new NullPointerException();
        }
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        for (HashtableEntry hashtableEntry = hashtableEntryArr[length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(k)) {
                V v2 = hashtableEntry.value;
                hashtableEntry.value = v;
                return v2;
            }
        }
        addEntry(iHashCode, k, v, length);
        return null;
    }

    @Override
    public synchronized V remove(Object obj) {
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = obj.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        HashtableEntry hashtableEntry = null;
        for (HashtableEntry hashtableEntry2 = hashtableEntryArr[length]; hashtableEntry2 != null; hashtableEntry2 = hashtableEntry2.next) {
            if (hashtableEntry2.hash != iHashCode || !hashtableEntry2.key.equals(obj)) {
                hashtableEntry = hashtableEntry2;
            } else {
                this.modCount++;
                if (hashtableEntry != null) {
                    hashtableEntry.next = hashtableEntry2.next;
                } else {
                    hashtableEntryArr[length] = hashtableEntry2.next;
                }
                this.count--;
                V v = hashtableEntry2.value;
                hashtableEntry2.value = null;
                return v;
            }
        }
        return null;
    }

    public synchronized void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void clear() {
        HashtableEntry<?, ?>[] hashtableEntryArr = this.table;
        this.modCount++;
        int length = hashtableEntryArr.length;
        while (true) {
            length--;
            if (length >= 0) {
                hashtableEntryArr[length] = null;
            } else {
                this.count = 0;
            }
        }
    }

    public synchronized Object clone() {
        Hashtable hashtable;
        try {
            hashtable = (Hashtable) super.clone();
            hashtable.table = new HashtableEntry[this.table.length];
            int length = this.table.length;
            while (true) {
                int i = length - 1;
                HashtableEntry<?, ?> hashtableEntry = null;
                if (length > 0) {
                    HashtableEntry<?, ?>[] hashtableEntryArr = hashtable.table;
                    if (this.table[i] != null) {
                        hashtableEntry = (HashtableEntry) this.table[i].clone();
                    }
                    hashtableEntryArr[i] = hashtableEntry;
                    length = i;
                } else {
                    hashtable.keySet = null;
                    hashtable.entrySet = null;
                    hashtable.values = null;
                    hashtable.modCount = 0;
                }
            }
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        return hashtable;
    }

    public synchronized String toString() {
        int size = size() - 1;
        if (size == -1) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        sb.append('{');
        int i = 0;
        while (true) {
            Map.Entry<K, V> next = it.next();
            K key = next.getKey();
            V value = next.getValue();
            sb.append(key == this ? "(this Map)" : key.toString());
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value.toString());
            if (i == size) {
                sb.append('}');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    private <T> Enumeration<T> getEnumeration(int i) {
        if (this.count == 0) {
            return Collections.emptyEnumeration();
        }
        return new Enumerator(i, false);
    }

    private <T> Iterator<T> getIterator(int i) {
        if (this.count == 0) {
            return Collections.emptyIterator();
        }
        return new Enumerator(i, true);
    }

    public Set<K> keySet() {
        if (this.keySet == null) {
            this.keySet = Collections.synchronizedSet(new KeySet(), this);
        }
        return this.keySet;
    }

    private class KeySet extends AbstractSet<K> {
        private KeySet() {
        }

        @Override
        public Iterator<K> iterator() {
            return Hashtable.this.getIterator(0);
        }

        @Override
        public int size() {
            return Hashtable.this.count;
        }

        @Override
        public boolean contains(Object obj) {
            return Hashtable.this.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            return Hashtable.this.remove(obj) != null;
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        if (this.entrySet == null) {
            this.entrySet = Collections.synchronizedSet(new EntrySet(), this);
        }
        return this.entrySet;
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        private EntrySet() {
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return Hashtable.this.getIterator(2);
        }

        @Override
        public boolean add(Map.Entry<K, V> entry) {
            return super.add(entry);
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            HashtableEntry<K, V>[] hashtableEntryArr = Hashtable.this.table;
            int iHashCode = key.hashCode();
            for (HashtableEntry<K, V> hashtableEntry = hashtableEntryArr[(Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
                if (hashtableEntry.hash == iHashCode && hashtableEntry.equals(entry)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            HashtableEntry<K, V>[] hashtableEntryArr = Hashtable.this.table;
            int iHashCode = key.hashCode();
            int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
            HashtableEntry<K, V> hashtableEntry = null;
            for (HashtableEntry<K, V> hashtableEntry2 = hashtableEntryArr[length]; hashtableEntry2 != null; hashtableEntry2 = hashtableEntry2.next) {
                if (hashtableEntry2.hash != iHashCode || !hashtableEntry2.equals(entry)) {
                    hashtableEntry = hashtableEntry2;
                } else {
                    Hashtable.access$508(Hashtable.this);
                    if (hashtableEntry != null) {
                        hashtableEntry.next = hashtableEntry2.next;
                    } else {
                        hashtableEntryArr[length] = hashtableEntry2.next;
                    }
                    Hashtable.access$210(Hashtable.this);
                    hashtableEntry2.value = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public int size() {
            return Hashtable.this.count;
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }
    }

    public Collection<V> values() {
        if (this.values == null) {
            this.values = Collections.synchronizedCollection(new ValueCollection(), this);
        }
        return this.values;
    }

    private class ValueCollection extends AbstractCollection<V> {
        private ValueCollection() {
        }

        @Override
        public Iterator<V> iterator() {
            return Hashtable.this.getIterator(1);
        }

        @Override
        public int size() {
            return Hashtable.this.count;
        }

        @Override
        public boolean contains(Object obj) {
            return Hashtable.this.containsValue(obj);
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }
    }

    @Override
    public synchronized boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        Map map = (Map) obj;
        if (map.size() != size()) {
            return false;
        }
        try {
            for (Map.Entry<K, V> entry : entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();
                if (value == null) {
                    if (map.get(key) != null || !map.containsKey(key)) {
                        return false;
                    }
                } else if (!value.equals(map.get(key))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @Override
    public synchronized int hashCode() {
        if (this.count != 0 && this.loadFactor >= 0.0f) {
            this.loadFactor = -this.loadFactor;
            int iHashCode = 0;
            for (HashtableEntry hashtableEntry : this.table) {
                for (; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
                    iHashCode += hashtableEntry.hashCode();
                }
            }
            this.loadFactor = -this.loadFactor;
            return iHashCode;
        }
        return 0;
    }

    public synchronized V getOrDefault(Object obj, V v) {
        V v2;
        v2 = get(obj);
        if (v2 == null) {
            v2 = v;
        }
        return v2;
    }

    public synchronized void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Objects.requireNonNull(biConsumer);
        int i = this.modCount;
        for (HashtableEntry hashtableEntry : this.table) {
            while (hashtableEntry != null) {
                biConsumer.accept(hashtableEntry.key, hashtableEntry.value);
                hashtableEntry = hashtableEntry.next;
                if (i != this.modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        int i = this.modCount;
        for (HashtableEntry hashtableEntry : this.table) {
            while (hashtableEntry != null) {
                hashtableEntry.value = (V) Objects.requireNonNull(biFunction.apply(hashtableEntry.key, hashtableEntry.value));
                hashtableEntry = hashtableEntry.next;
                if (i != this.modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    public synchronized V putIfAbsent(K k, V v) {
        Objects.requireNonNull(v);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        for (HashtableEntry hashtableEntry = hashtableEntryArr[length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(k)) {
                V v2 = hashtableEntry.value;
                if (v2 == null) {
                    hashtableEntry.value = v;
                }
                return v2;
            }
        }
        addEntry(iHashCode, k, v, length);
        return null;
    }

    public synchronized boolean remove(Object obj, Object obj2) {
        Objects.requireNonNull(obj2);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = obj.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        HashtableEntry hashtableEntry = null;
        for (HashtableEntry hashtableEntry2 = hashtableEntryArr[length]; hashtableEntry2 != null; hashtableEntry2 = hashtableEntry2.next) {
            if (hashtableEntry2.hash != iHashCode || !hashtableEntry2.key.equals(obj) || !hashtableEntry2.value.equals(obj2)) {
                hashtableEntry = hashtableEntry2;
            } else {
                this.modCount++;
                if (hashtableEntry != null) {
                    hashtableEntry.next = hashtableEntry2.next;
                } else {
                    hashtableEntryArr[length] = hashtableEntry2.next;
                }
                this.count--;
                hashtableEntry2.value = null;
                return true;
            }
        }
        return false;
    }

    public synchronized boolean replace(K k, V v, V v2) {
        Objects.requireNonNull(v);
        Objects.requireNonNull(v2);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        for (HashtableEntry hashtableEntry = hashtableEntryArr[(Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(k)) {
                if (!hashtableEntry.value.equals(v)) {
                    return false;
                }
                hashtableEntry.value = v2;
                return true;
            }
        }
        return false;
    }

    public synchronized V replace(K k, V v) {
        Objects.requireNonNull(v);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        for (HashtableEntry hashtableEntry = hashtableEntryArr[(Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(k)) {
                V v2 = hashtableEntry.value;
                hashtableEntry.value = v;
                return v2;
            }
        }
        return null;
    }

    public synchronized V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
        Objects.requireNonNull(function);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        for (HashtableEntry hashtableEntry = hashtableEntryArr[length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(k)) {
                return hashtableEntry.value;
            }
        }
        V vApply = function.apply(k);
        if (vApply != null) {
            addEntry(iHashCode, k, vApply, length);
        }
        return vApply;
    }

    public synchronized V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        HashtableEntry hashtableEntry = null;
        for (HashtableEntry hashtableEntry2 = hashtableEntryArr[length]; hashtableEntry2 != null; hashtableEntry2 = hashtableEntry2.next) {
            if (hashtableEntry2.hash != iHashCode || !hashtableEntry2.key.equals(k)) {
                hashtableEntry = hashtableEntry2;
            } else {
                V vApply = biFunction.apply(k, hashtableEntry2.value);
                if (vApply == null) {
                    this.modCount++;
                    if (hashtableEntry != null) {
                        hashtableEntry.next = hashtableEntry2.next;
                    } else {
                        hashtableEntryArr[length] = hashtableEntry2.next;
                    }
                    this.count--;
                } else {
                    hashtableEntry2.value = vApply;
                }
                return vApply;
            }
        }
        return null;
    }

    public synchronized V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        HashtableEntry hashtableEntry = null;
        for (HashtableEntry hashtableEntry2 = hashtableEntryArr[length]; hashtableEntry2 != null; hashtableEntry2 = hashtableEntry2.next) {
            if (hashtableEntry2.hash != iHashCode || !Objects.equals(hashtableEntry2.key, k)) {
                hashtableEntry = hashtableEntry2;
            } else {
                V vApply = biFunction.apply(k, hashtableEntry2.value);
                if (vApply == null) {
                    this.modCount++;
                    if (hashtableEntry != null) {
                        hashtableEntry.next = hashtableEntry2.next;
                    } else {
                        hashtableEntryArr[length] = hashtableEntry2.next;
                    }
                    this.count--;
                } else {
                    hashtableEntry2.value = vApply;
                }
                return vApply;
            }
        }
        V vApply2 = biFunction.apply(k, null);
        if (vApply2 != null) {
            addEntry(iHashCode, k, vApply2, length);
        }
        return vApply2;
    }

    public synchronized V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        HashtableEntry[] hashtableEntryArr = this.table;
        int iHashCode = k.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        HashtableEntry hashtableEntry = hashtableEntryArr[length];
        HashtableEntry hashtableEntry2 = null;
        while (true) {
            HashtableEntry hashtableEntry3 = hashtableEntry2;
            hashtableEntry2 = hashtableEntry;
            if (hashtableEntry2 != null) {
                if (hashtableEntry2.hash != iHashCode || !hashtableEntry2.key.equals(k)) {
                    hashtableEntry = hashtableEntry2.next;
                } else {
                    V vApply = biFunction.apply(hashtableEntry2.value, v);
                    if (vApply == null) {
                        this.modCount++;
                        if (hashtableEntry3 != null) {
                            hashtableEntry3.next = hashtableEntry2.next;
                        } else {
                            hashtableEntryArr[length] = hashtableEntry2.next;
                        }
                        this.count--;
                    } else {
                        hashtableEntry2.value = vApply;
                    }
                    return vApply;
                }
            } else {
                if (v != null) {
                    addEntry(iHashCode, k, v, length);
                }
                return v;
            }
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        HashtableEntry<K, V> hashtableEntry;
        synchronized (this) {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeInt(this.table.length);
            objectOutputStream.writeInt(this.count);
            hashtableEntry = null;
            for (int i = 0; i < this.table.length; i++) {
                HashtableEntry hashtableEntry2 = this.table[i];
                while (hashtableEntry2 != null) {
                    HashtableEntry<K, V> hashtableEntry3 = new HashtableEntry<>(0, hashtableEntry2.key, hashtableEntry2.value, hashtableEntry);
                    hashtableEntry2 = hashtableEntry2.next;
                    hashtableEntry = hashtableEntry3;
                }
            }
        }
        while (hashtableEntry != null) {
            objectOutputStream.writeObject(hashtableEntry.key);
            objectOutputStream.writeObject(hashtableEntry.value);
            hashtableEntry = hashtableEntry.next;
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.loadFactor <= 0.0f || Float.isNaN(this.loadFactor)) {
            throw new StreamCorruptedException("Illegal Load: " + this.loadFactor);
        }
        int i = objectInputStream.readInt();
        int i2 = objectInputStream.readInt();
        if (i2 < 0) {
            throw new StreamCorruptedException("Illegal # of Elements: " + i2);
        }
        int iMax = Math.max(i, ((int) (i2 / this.loadFactor)) + 1);
        int i3 = ((int) (((i2 / 20) + i2) / this.loadFactor)) + 3;
        if (i3 > i2 && (i3 & 1) == 0) {
            i3--;
        }
        int iMin = Math.min(i3, iMax);
        this.table = new HashtableEntry[iMin];
        this.threshold = (int) Math.min(iMin * this.loadFactor, 2.1474836E9f);
        this.count = 0;
        while (i2 > 0) {
            reconstitutionPut(this.table, objectInputStream.readObject(), objectInputStream.readObject());
            i2--;
        }
    }

    private void reconstitutionPut(HashtableEntry<?, ?>[] hashtableEntryArr, K k, V v) throws StreamCorruptedException {
        if (v == null) {
            throw new StreamCorruptedException();
        }
        int iHashCode = k.hashCode();
        int length = (Integer.MAX_VALUE & iHashCode) % hashtableEntryArr.length;
        for (HashtableEntry<K, V> hashtableEntry = (HashtableEntry<K, V>) hashtableEntryArr[length]; hashtableEntry != null; hashtableEntry = hashtableEntry.next) {
            if (hashtableEntry.hash == iHashCode && hashtableEntry.key.equals(k)) {
                throw new StreamCorruptedException();
            }
        }
        hashtableEntryArr[length] = new HashtableEntry(iHashCode, k, v, hashtableEntryArr[length]);
        this.count++;
    }

    private static class HashtableEntry<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        HashtableEntry<K, V> next;
        V value;

        protected HashtableEntry(int i, K k, V v, HashtableEntry<K, V> hashtableEntry) {
            this.hash = i;
            this.key = k;
            this.value = v;
            this.next = hashtableEntry;
        }

        protected Object clone() {
            return new HashtableEntry(this.hash, this.key, this.value, this.next == null ? null : (HashtableEntry) this.next.clone());
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
        public V setValue(V v) {
            if (v == null) {
                throw new NullPointerException();
            }
            V v2 = this.value;
            this.value = v;
            return v2;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            if (this.key == null) {
                if (entry.getKey() != null) {
                    return false;
                }
            } else if (!this.key.equals(entry.getKey())) {
                return false;
            }
            if (this.value == null) {
                if (entry.getValue() != null) {
                    return false;
                }
            } else if (!this.value.equals(entry.getValue())) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return this.hash ^ Objects.hashCode(this.value);
        }

        public String toString() {
            return this.key.toString() + "=" + this.value.toString();
        }
    }

    private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
        HashtableEntry<?, ?> entry;
        protected int expectedModCount;
        int index;
        boolean iterator;
        HashtableEntry<?, ?> lastReturned;
        HashtableEntry<?, ?>[] table;
        int type;

        Enumerator(int i, boolean z) {
            this.table = Hashtable.this.table;
            this.index = this.table.length;
            this.expectedModCount = Hashtable.this.modCount;
            this.type = i;
            this.iterator = z;
        }

        @Override
        public boolean hasMoreElements() {
            HashtableEntry<?, ?> hashtableEntry = this.entry;
            int i = this.index;
            HashtableEntry<?, ?>[] hashtableEntryArr = this.table;
            while (hashtableEntry == null && i > 0) {
                i--;
                hashtableEntry = hashtableEntryArr[i];
            }
            this.entry = hashtableEntry;
            this.index = i;
            return hashtableEntry != null;
        }

        @Override
        public T nextElement() {
            HashtableEntry<?, ?> hashtableEntry = this.entry;
            int i = this.index;
            HashtableEntry<?, ?>[] hashtableEntryArr = this.table;
            while (hashtableEntry == null && i > 0) {
                i--;
                hashtableEntry = hashtableEntryArr[i];
            }
            this.entry = hashtableEntry;
            this.index = i;
            if (hashtableEntry != null) {
                ?? r0 = (T) this.entry;
                this.lastReturned = r0;
                this.entry = r0.next;
                return this.type == 0 ? r0.key : this.type == 1 ? r0.value : r0;
            }
            throw new NoSuchElementException("Hashtable Enumerator");
        }

        @Override
        public boolean hasNext() {
            return hasMoreElements();
        }

        @Override
        public T next() {
            if (Hashtable.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return nextElement();
        }

        @Override
        public void remove() {
            if (!this.iterator) {
                throw new UnsupportedOperationException();
            }
            if (this.lastReturned != null) {
                if (Hashtable.this.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                synchronized (Hashtable.this) {
                    HashtableEntry<K, V>[] hashtableEntryArr = Hashtable.this.table;
                    int length = (this.lastReturned.hash & Integer.MAX_VALUE) % hashtableEntryArr.length;
                    HashtableEntry<K, V> hashtableEntry = null;
                    for (HashtableEntry<K, V> hashtableEntry2 = hashtableEntryArr[length]; hashtableEntry2 != null; hashtableEntry2 = hashtableEntry2.next) {
                        if (hashtableEntry2 != this.lastReturned) {
                            hashtableEntry = hashtableEntry2;
                        } else {
                            Hashtable.access$508(Hashtable.this);
                            this.expectedModCount++;
                            if (hashtableEntry == null) {
                                hashtableEntryArr[length] = hashtableEntry2.next;
                            } else {
                                hashtableEntry.next = hashtableEntry2.next;
                            }
                            Hashtable.access$210(Hashtable.this);
                            this.lastReturned = null;
                        }
                    }
                    throw new ConcurrentModificationException();
                }
                return;
            }
            throw new IllegalStateException("Hashtable Enumerator");
        }
    }
}
