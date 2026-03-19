package java.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class WeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1073741824;
    private static final Object NULL_KEY = new Object();
    private transient Set<Map.Entry<K, V>> entrySet;
    private final float loadFactor;
    int modCount;
    private final ReferenceQueue<Object> queue;
    private int size;
    Entry<K, V>[] table;
    private int threshold;

    private Entry<K, V>[] newTable(int i) {
        return new Entry[i];
    }

    public WeakHashMap(int i, float f) {
        this.queue = new ReferenceQueue<>();
        if (i < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: " + i);
        }
        i = i > MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : i;
        if (f <= 0.0f || Float.isNaN(f)) {
            throw new IllegalArgumentException("Illegal Load factor: " + f);
        }
        int i2 = 1;
        while (i2 < i) {
            i2 <<= 1;
        }
        this.table = newTable(i2);
        this.loadFactor = f;
        this.threshold = (int) (i2 * f);
    }

    public WeakHashMap(int i) {
        this(i, DEFAULT_LOAD_FACTOR);
    }

    public WeakHashMap() {
        this(16, DEFAULT_LOAD_FACTOR);
    }

    public WeakHashMap(Map<? extends K, ? extends V> map) {
        this(Math.max(((int) (map.size() / DEFAULT_LOAD_FACTOR)) + 1, 16), DEFAULT_LOAD_FACTOR);
        putAll(map);
    }

    private static Object maskNull(Object obj) {
        return obj == null ? NULL_KEY : obj;
    }

    static Object unmaskNull(Object obj) {
        if (obj == NULL_KEY) {
            return null;
        }
        return obj;
    }

    private static boolean eq(Object obj, Object obj2) {
        return obj == obj2 || obj.equals(obj2);
    }

    final int hash(Object obj) {
        int iHashCode = obj.hashCode();
        int i = iHashCode ^ ((iHashCode >>> 20) ^ (iHashCode >>> 12));
        return (i >>> 4) ^ ((i >>> 7) ^ i);
    }

    private static int indexFor(int i, int i2) {
        return i & (i2 - 1);
    }

    private void expungeStaleEntries() {
        while (true) {
            Reference<? extends Object> referencePoll = this.queue.poll();
            if (referencePoll != null) {
                synchronized (this.queue) {
                    Entry<K, V> entry = (Entry) referencePoll;
                    int iIndexFor = indexFor(entry.hash, this.table.length);
                    Entry<K, V> entry2 = this.table[iIndexFor];
                    Entry<K, V> entry3 = entry2;
                    while (true) {
                        if (entry2 == null) {
                            break;
                        }
                        Entry<K, V> entry4 = entry2.next;
                        if (entry2 != entry) {
                            entry3 = entry2;
                            entry2 = entry4;
                        } else {
                            if (entry3 == entry) {
                                this.table[iIndexFor] = entry4;
                            } else {
                                entry3.next = entry4;
                            }
                            entry.value = null;
                            this.size--;
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    private Entry<K, V>[] getTable() {
        expungeStaleEntries();
        return this.table;
    }

    @Override
    public int size() {
        if (this.size == 0) {
            return 0;
        }
        expungeStaleEntries();
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public V get(Object obj) {
        Object objMaskNull = maskNull(obj);
        int iHash = hash(objMaskNull);
        Entry<K, V>[] table = getTable();
        for (Entry<K, V> entry = table[indexFor(iHash, table.length)]; entry != null; entry = entry.next) {
            if (entry.hash == iHash && eq(objMaskNull, entry.get())) {
                return entry.value;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(Object obj) {
        return getEntry(obj) != null;
    }

    Entry<K, V> getEntry(Object obj) {
        Object objMaskNull = maskNull(obj);
        int iHash = hash(objMaskNull);
        Entry<K, V>[] table = getTable();
        Entry<K, V> entry = table[indexFor(iHash, table.length)];
        while (entry != null && (entry.hash != iHash || !eq(objMaskNull, entry.get()))) {
            entry = entry.next;
        }
        return entry;
    }

    @Override
    public V put(K k, V v) {
        Object objMaskNull = maskNull(k);
        int iHash = hash(objMaskNull);
        Entry<K, V>[] table = getTable();
        int iIndexFor = indexFor(iHash, table.length);
        for (Entry<K, V> entry = table[iIndexFor]; entry != null; entry = entry.next) {
            if (iHash == entry.hash && eq(objMaskNull, entry.get())) {
                V v2 = entry.value;
                if (v != v2) {
                    entry.value = v;
                }
                return v2;
            }
        }
        this.modCount++;
        table[iIndexFor] = new Entry<>(objMaskNull, v, this.queue, iHash, table[iIndexFor]);
        int i = this.size + 1;
        this.size = i;
        if (i >= this.threshold) {
            resize(table.length * 2);
            return null;
        }
        return null;
    }

    void resize(int i) {
        Entry<K, V>[] table = getTable();
        if (table.length == MAXIMUM_CAPACITY) {
            this.threshold = Integer.MAX_VALUE;
            return;
        }
        Entry<K, V>[] entryArrNewTable = newTable(i);
        transfer(table, entryArrNewTable);
        this.table = entryArrNewTable;
        if (this.size >= this.threshold / 2) {
            this.threshold = (int) (i * this.loadFactor);
            return;
        }
        expungeStaleEntries();
        transfer(entryArrNewTable, table);
        this.table = table;
    }

    private void transfer(Entry<K, V>[] entryArr, Entry<K, V>[] entryArr2) {
        for (int i = 0; i < entryArr.length; i++) {
            Entry<K, V> entry = entryArr[i];
            entryArr[i] = null;
            while (entry != null) {
                Entry<K, V> entry2 = entry.next;
                if (entry.get() == null) {
                    entry.next = null;
                    entry.value = null;
                    this.size--;
                } else {
                    int iIndexFor = indexFor(entry.hash, entryArr2.length);
                    entry.next = entryArr2[iIndexFor];
                    entryArr2[iIndexFor] = entry;
                }
                entry = entry2;
            }
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        if (size > this.threshold) {
            int i = (int) ((size / this.loadFactor) + 1.0f);
            if (i > MAXIMUM_CAPACITY) {
                i = MAXIMUM_CAPACITY;
            }
            int length = this.table.length;
            while (length < i) {
                length <<= 1;
            }
            if (length > this.table.length) {
                resize(length);
            }
        }
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object obj) {
        Object objMaskNull = maskNull(obj);
        int iHash = hash(objMaskNull);
        Entry<K, V>[] table = getTable();
        int iIndexFor = indexFor(iHash, table.length);
        Entry<K, V> entry = table[iIndexFor];
        Entry<K, V> entry2 = entry;
        while (entry != null) {
            Entry<K, V> entry3 = entry.next;
            if (iHash != entry.hash || !eq(objMaskNull, entry.get())) {
                entry2 = entry;
                entry = entry3;
            } else {
                this.modCount++;
                this.size--;
                if (entry2 == entry) {
                    table[iIndexFor] = entry3;
                } else {
                    entry2.next = entry3;
                }
                return entry.value;
            }
        }
        return null;
    }

    boolean removeMapping(Object obj) {
        if (!(obj instanceof Map.Entry)) {
            return false;
        }
        Entry<K, V>[] table = getTable();
        Map.Entry entry = (Map.Entry) obj;
        int iHash = hash(maskNull(entry.getKey()));
        int iIndexFor = indexFor(iHash, table.length);
        Entry<K, V> entry2 = table[iIndexFor];
        Entry<K, V> entry3 = entry2;
        while (entry2 != null) {
            Entry<K, V> entry4 = entry2.next;
            if (iHash != entry2.hash || !entry2.equals(entry)) {
                entry3 = entry2;
                entry2 = entry4;
            } else {
                this.modCount++;
                this.size--;
                if (entry3 == entry2) {
                    table[iIndexFor] = entry4;
                } else {
                    entry3.next = entry4;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        while (this.queue.poll() != null) {
        }
        this.modCount++;
        Arrays.fill(this.table, (Object) null);
        this.size = 0;
        while (this.queue.poll() != null) {
        }
    }

    @Override
    public boolean containsValue(Object obj) {
        if (obj == null) {
            return containsNullValue();
        }
        Entry<K, V>[] table = getTable();
        int length = table.length;
        while (true) {
            int i = length - 1;
            if (length > 0) {
                for (Entry<K, V> entry = table[i]; entry != null; entry = entry.next) {
                    if (obj.equals(entry.value)) {
                        return true;
                    }
                }
                length = i;
            } else {
                return false;
            }
        }
    }

    private boolean containsNullValue() {
        Entry<K, V>[] table = getTable();
        int length = table.length;
        while (true) {
            int i = length - 1;
            if (length > 0) {
                for (Entry<K, V> entry = table[i]; entry != null; entry = entry.next) {
                    if (entry.value == null) {
                        return true;
                    }
                }
                length = i;
            } else {
                return false;
            }
        }
    }

    private static class Entry<K, V> extends WeakReference<Object> implements Map.Entry<K, V> {
        final int hash;
        Entry<K, V> next;
        V value;

        Entry(Object obj, V v, ReferenceQueue<Object> referenceQueue, int i, Entry<K, V> entry) {
            super(obj, referenceQueue);
            this.value = v;
            this.hash = i;
            this.next = entry;
        }

        @Override
        public K getKey() {
            return (K) WeakHashMap.unmaskNull(get());
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V v) {
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
            K key = getKey();
            Object key2 = entry.getKey();
            if (key == key2 || (key != null && key.equals(key2))) {
                V value = getValue();
                Object value2 = entry.getValue();
                if (value == value2) {
                    return true;
                }
                if (value != null && value.equals(value2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
        }

        public String toString() {
            return ((Object) getKey()) + "=" + ((Object) getValue());
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        private Object currentKey;
        private Entry<K, V> entry;
        private int expectedModCount;
        private int index;
        private Entry<K, V> lastReturned;
        private Object nextKey;

        HashIterator() {
            this.expectedModCount = WeakHashMap.this.modCount;
            this.index = WeakHashMap.this.isEmpty() ? 0 : WeakHashMap.this.table.length;
        }

        @Override
        public boolean hasNext() {
            Entry<K, V>[] entryArr = WeakHashMap.this.table;
            while (this.nextKey == null) {
                Entry<K, V> entry = this.entry;
                int i = this.index;
                while (entry == null && i > 0) {
                    i--;
                    entry = entryArr[i];
                }
                this.entry = entry;
                this.index = i;
                if (entry == null) {
                    this.currentKey = null;
                    return false;
                }
                this.nextKey = entry.get();
                if (this.nextKey == null) {
                    this.entry = this.entry.next;
                }
            }
            return true;
        }

        protected Entry<K, V> nextEntry() {
            if (WeakHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (this.nextKey == null && !hasNext()) {
                throw new NoSuchElementException();
            }
            this.lastReturned = this.entry;
            this.entry = this.entry.next;
            this.currentKey = this.nextKey;
            this.nextKey = null;
            return this.lastReturned;
        }

        @Override
        public void remove() {
            if (this.lastReturned == null) {
                throw new IllegalStateException();
            }
            if (WeakHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            WeakHashMap.this.remove(this.currentKey);
            this.expectedModCount = WeakHashMap.this.modCount;
            this.lastReturned = null;
            this.currentKey = null;
        }
    }

    private class ValueIterator extends WeakHashMap<K, V>.HashIterator<V> {
        private ValueIterator() {
            super();
        }

        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends WeakHashMap<K, V>.HashIterator<K> {
        private KeyIterator() {
            super();
        }

        @Override
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends WeakHashMap<K, V>.HashIterator<Map.Entry<K, V>> {
        private EntryIterator() {
            super();
        }

        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
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
            return WeakHashMap.this.size();
        }

        @Override
        public boolean contains(Object obj) {
            return WeakHashMap.this.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            if (WeakHashMap.this.containsKey(obj)) {
                WeakHashMap.this.remove(obj);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            WeakHashMap.this.clear();
        }

        @Override
        public Spliterator<K> spliterator() {
            return new KeySpliterator(WeakHashMap.this, 0, -1, 0, 0);
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
            return WeakHashMap.this.size();
        }

        @Override
        public boolean contains(Object obj) {
            return WeakHashMap.this.containsValue(obj);
        }

        @Override
        public void clear() {
            WeakHashMap.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return new ValueSpliterator(WeakHashMap.this, 0, -1, 0, 0);
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
            Entry<K, V> entry2 = WeakHashMap.this.getEntry(entry.getKey());
            return entry2 != null && entry2.equals(entry);
        }

        @Override
        public boolean remove(Object obj) {
            return WeakHashMap.this.removeMapping(obj);
        }

        @Override
        public int size() {
            return WeakHashMap.this.size();
        }

        @Override
        public void clear() {
            WeakHashMap.this.clear();
        }

        private List<Map.Entry<K, V>> deepCopy() {
            ArrayList arrayList = new ArrayList(size());
            Iterator<Map.Entry<K, V>> it = iterator();
            while (it.hasNext()) {
                arrayList.add(new AbstractMap.SimpleEntry(it.next()));
            }
            return arrayList;
        }

        @Override
        public Object[] toArray() {
            return deepCopy().toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) deepCopy().toArray(tArr);
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator(WeakHashMap.this, 0, -1, 0, 0);
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Objects.requireNonNull(biConsumer);
        int i = this.modCount;
        for (Entry<K, V> entry : getTable()) {
            while (entry != null) {
                Object obj = entry.get();
                if (obj != null) {
                    biConsumer.accept((Object) unmaskNull(obj), entry.value);
                }
                entry = entry.next;
                if (i != this.modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        int i = this.modCount;
        for (Entry<K, V> entry : getTable()) {
            while (entry != null) {
                Object obj = entry.get();
                if (obj != null) {
                    entry.value = biFunction.apply((Object) unmaskNull(obj), entry.value);
                }
                entry = entry.next;
                if (i != this.modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    static class WeakHashMapSpliterator<K, V> {
        Entry<K, V> current;
        int est;
        int expectedModCount;
        int fence;
        int index;
        final WeakHashMap<K, V> map;

        WeakHashMapSpliterator(WeakHashMap<K, V> weakHashMap, int i, int i2, int i3, int i4) {
            this.map = weakHashMap;
            this.index = i;
            this.fence = i2;
            this.est = i3;
            this.expectedModCount = i4;
        }

        final int getFence() {
            int i = this.fence;
            if (i < 0) {
                WeakHashMap<K, V> weakHashMap = this.map;
                this.est = weakHashMap.size();
                this.expectedModCount = weakHashMap.modCount;
                int length = weakHashMap.table.length;
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

    static final class KeySpliterator<K, V> extends WeakHashMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(WeakHashMap<K, V> weakHashMap, int i, int i2, int i3, int i4) {
            super(weakHashMap, i, i2, i3, i4);
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            WeakHashMap<K, V> weakHashMap = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new KeySpliterator<>(weakHashMap, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            int i;
            int i2;
            if (consumer == null) {
                throw new NullPointerException();
            }
            WeakHashMap<K, V> weakHashMap = this.map;
            Entry<K, V>[] entryArr = weakHashMap.table;
            int i3 = this.fence;
            if (i3 < 0) {
                int i4 = weakHashMap.modCount;
                this.expectedModCount = i4;
                int length = entryArr.length;
                this.fence = length;
                i = i4;
                i3 = length;
            } else {
                i = this.expectedModCount;
            }
            if (entryArr.length >= i3 && (i2 = this.index) >= 0) {
                this.index = i3;
                if (i2 < i3 || this.current != null) {
                    Entry<K, V> entry = this.current;
                    this.current = null;
                    while (true) {
                        if (entry == null) {
                            entry = entryArr[i2];
                            i2++;
                        } else {
                            Object obj = entry.get();
                            entry = entry.next;
                            if (obj != null) {
                                consumer.accept((Object) WeakHashMap.unmaskNull(obj));
                            }
                        }
                        if (entry == null && i2 >= i3) {
                            break;
                        }
                    }
                }
            }
            if (weakHashMap.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Entry<K, V>[] entryArr = this.map.table;
            int length = entryArr.length;
            int fence = getFence();
            if (length < fence || this.index < 0) {
                return false;
            }
            while (true) {
                if (this.current != null || this.index < fence) {
                    if (this.current == null) {
                        int i = this.index;
                        this.index = i + 1;
                        this.current = entryArr[i];
                    } else {
                        Object obj = this.current.get();
                        this.current = this.current.next;
                        if (obj != null) {
                            consumer.accept((Object) WeakHashMap.unmaskNull(obj));
                            if (this.map.modCount != this.expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public int characteristics() {
            return 1;
        }
    }

    static final class ValueSpliterator<K, V> extends WeakHashMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(WeakHashMap<K, V> weakHashMap, int i, int i2, int i3, int i4) {
            super(weakHashMap, i, i2, i3, i4);
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            WeakHashMap<K, V> weakHashMap = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new ValueSpliterator<>(weakHashMap, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> consumer) {
            int i;
            int i2;
            if (consumer == null) {
                throw new NullPointerException();
            }
            WeakHashMap<K, V> weakHashMap = this.map;
            Entry<K, V>[] entryArr = weakHashMap.table;
            int i3 = this.fence;
            if (i3 < 0) {
                int i4 = weakHashMap.modCount;
                this.expectedModCount = i4;
                int length = entryArr.length;
                this.fence = length;
                i = i4;
                i3 = length;
            } else {
                i = this.expectedModCount;
            }
            if (entryArr.length >= i3 && (i2 = this.index) >= 0) {
                this.index = i3;
                if (i2 < i3 || this.current != null) {
                    Entry<K, V> entry = this.current;
                    this.current = null;
                    while (true) {
                        if (entry == null) {
                            entry = entryArr[i2];
                            i2++;
                        } else {
                            Object obj = entry.get();
                            V v = entry.value;
                            entry = entry.next;
                            if (obj != null) {
                                consumer.accept(v);
                            }
                        }
                        if (entry == null && i2 >= i3) {
                            break;
                        }
                    }
                }
            }
            if (weakHashMap.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Entry<K, V>[] entryArr = this.map.table;
            int length = entryArr.length;
            int fence = getFence();
            if (length < fence || this.index < 0) {
                return false;
            }
            while (true) {
                if (this.current != null || this.index < fence) {
                    if (this.current == null) {
                        int i = this.index;
                        this.index = i + 1;
                        this.current = entryArr[i];
                    } else {
                        Object obj = this.current.get();
                        V v = this.current.value;
                        this.current = this.current.next;
                        if (obj != null) {
                            consumer.accept(v);
                            if (this.map.modCount != this.expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public int characteristics() {
            return 0;
        }
    }

    static final class EntrySpliterator<K, V> extends WeakHashMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(WeakHashMap<K, V> weakHashMap, int i, int i2, int i3, int i4) {
            super(weakHashMap, i, i2, i3, i4);
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            WeakHashMap<K, V> weakHashMap = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new EntrySpliterator<>(weakHashMap, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> consumer) {
            int i;
            int i2;
            if (consumer == null) {
                throw new NullPointerException();
            }
            WeakHashMap<K, V> weakHashMap = this.map;
            Entry<K, V>[] entryArr = weakHashMap.table;
            int i3 = this.fence;
            if (i3 < 0) {
                int i4 = weakHashMap.modCount;
                this.expectedModCount = i4;
                int length = entryArr.length;
                this.fence = length;
                i = i4;
                i3 = length;
            } else {
                i = this.expectedModCount;
            }
            if (entryArr.length >= i3 && (i2 = this.index) >= 0) {
                this.index = i3;
                if (i2 < i3 || this.current != null) {
                    Entry<K, V> entry = this.current;
                    this.current = null;
                    while (true) {
                        if (entry == null) {
                            entry = entryArr[i2];
                            i2++;
                        } else {
                            Object obj = entry.get();
                            V v = entry.value;
                            entry = entry.next;
                            if (obj != null) {
                                consumer.accept(new AbstractMap.SimpleImmutableEntry(WeakHashMap.unmaskNull(obj), v));
                            }
                        }
                        if (entry == null && i2 >= i3) {
                            break;
                        }
                    }
                }
            }
            if (weakHashMap.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Entry<K, V>[] entryArr = this.map.table;
            int length = entryArr.length;
            int fence = getFence();
            if (length < fence || this.index < 0) {
                return false;
            }
            while (true) {
                if (this.current != null || this.index < fence) {
                    if (this.current == null) {
                        int i = this.index;
                        this.index = i + 1;
                        this.current = entryArr[i];
                    } else {
                        Object obj = this.current.get();
                        V v = this.current.value;
                        this.current = this.current.next;
                        if (obj != null) {
                            consumer.accept(new AbstractMap.SimpleImmutableEntry(WeakHashMap.unmaskNull(obj), v));
                            if (this.map.modCount != this.expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public int characteristics() {
            return 1;
        }
    }
}
