package java.util;

import java.util.HashMap;
import java.util.Map;

public class LinkedHashMap<K, V> extends HashMap<K, V> {
    private static final long serialVersionUID = 3801124242820219131L;
    private final boolean accessOrder;
    transient LinkedEntry<K, V> header;

    public LinkedHashMap() {
        init();
        this.accessOrder = false;
    }

    public LinkedHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, false);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        init();
        this.accessOrder = accessOrder;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> map) {
        this(capacityForInitSize(map.size()));
        constructorPutAll(map);
    }

    @Override
    void init() {
        this.header = new LinkedEntry<>();
    }

    static class LinkedEntry<K, V> extends HashMap.HashMapEntry<K, V> {
        LinkedEntry<K, V> nxt;
        LinkedEntry<K, V> prv;

        LinkedEntry() {
            super(null, null, 0, null);
            this.prv = this;
            this.nxt = this;
        }

        LinkedEntry(K key, V value, int hash, HashMap.HashMapEntry<K, V> next, LinkedEntry<K, V> nxt, LinkedEntry<K, V> prv) {
            super(key, value, hash, next);
            this.nxt = nxt;
            this.prv = prv;
        }
    }

    public Map.Entry<K, V> eldest() {
        LinkedEntry<K, V> eldest = this.header.nxt;
        if (eldest != this.header) {
            return eldest;
        }
        return null;
    }

    @Override
    void addNewEntry(K key, V value, int hash, int index) {
        LinkedEntry<K, V> header = this.header;
        LinkedEntry<K, V> eldest = header.nxt;
        if (eldest != header && removeEldestEntry(eldest)) {
            remove(eldest.key);
        }
        LinkedEntry<K, V> oldTail = header.prv;
        LinkedEntry<K, V> newTail = new LinkedEntry<>(key, value, hash, this.table[index], header, oldTail);
        HashMap.HashMapEntry<K, V>[] hashMapEntryArr = this.table;
        header.prv = newTail;
        oldTail.nxt = newTail;
        hashMapEntryArr[index] = newTail;
    }

    @Override
    void addNewEntryForNullKey(V value) {
        LinkedEntry<K, V> header = this.header;
        LinkedEntry<K, V> eldest = header.nxt;
        if (eldest != header && removeEldestEntry(eldest)) {
            remove(eldest.key);
        }
        LinkedEntry<K, V> oldTail = header.prv;
        LinkedEntry<K, V> newTail = new LinkedEntry<>(null, value, 0, null, header, oldTail);
        header.prv = newTail;
        oldTail.nxt = newTail;
        this.entryForNullKey = newTail;
    }

    @Override
    HashMap.HashMapEntry<K, V> constructorNewEntry(K key, V value, int hash, HashMap.HashMapEntry<K, V> next) {
        LinkedEntry<K, V> header = this.header;
        LinkedEntry<K, V> oldTail = header.prv;
        LinkedEntry<K, V> newTail = new LinkedEntry<>(key, value, hash, next, header, oldTail);
        header.prv = newTail;
        oldTail.nxt = newTail;
        return newTail;
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            HashMap.HashMapEntry<K, V> e = this.entryForNullKey;
            if (e == null) {
                return null;
            }
            if (this.accessOrder) {
                makeTail((LinkedEntry) e);
            }
            return e.value;
        }
        int hash = Collections.secondaryHash(key);
        HashMap.HashMapEntry<K, V>[] tab = this.table;
        for (HashMap.HashMapEntry<K, V> e2 = tab[(tab.length - 1) & hash]; e2 != null; e2 = e2.next) {
            K eKey = e2.key;
            if (eKey == key || (e2.hash == hash && key.equals(eKey))) {
                if (this.accessOrder) {
                    makeTail((LinkedEntry) e2);
                }
                return e2.value;
            }
        }
        return null;
    }

    private void makeTail(LinkedEntry<K, V> e) {
        e.prv.nxt = e.nxt;
        e.nxt.prv = e.prv;
        LinkedEntry<K, V> header = this.header;
        LinkedEntry<K, V> oldTail = header.prv;
        e.nxt = header;
        e.prv = oldTail;
        header.prv = e;
        oldTail.nxt = e;
        this.modCount++;
    }

    @Override
    void preModify(HashMap.HashMapEntry<K, V> e) {
        if (this.accessOrder) {
            makeTail((LinkedEntry) e);
        }
    }

    @Override
    void postRemove(HashMap.HashMapEntry<K, V> e) {
        LinkedEntry<K, V> le = (LinkedEntry) e;
        le.prv.nxt = le.nxt;
        le.nxt.prv = le.prv;
        le.prv = null;
        le.nxt = null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            LinkedEntry<K, V> header = this.header;
            for (LinkedEntry<K, V> e = header.nxt; e != header; e = e.nxt) {
                if (e.value == null) {
                    return true;
                }
            }
            return false;
        }
        LinkedEntry<K, V> header2 = this.header;
        for (LinkedEntry<K, V> e2 = header2.nxt; e2 != header2; e2 = e2.nxt) {
            if (value.equals(e2.value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        LinkedEntry<K, V> header = this.header;
        LinkedEntry<K, V> e = header.nxt;
        while (e != header) {
            LinkedEntry<K, V> nxt = e.nxt;
            e.prv = null;
            e.nxt = null;
            e = nxt;
        }
        header.prv = header;
        header.nxt = header;
    }

    private abstract class LinkedHashIterator<T> implements Iterator<T> {
        int expectedModCount;
        LinkedEntry<K, V> lastReturned;
        LinkedEntry<K, V> next;

        private LinkedHashIterator() {
            this.next = LinkedHashMap.this.header.nxt;
            this.lastReturned = null;
            this.expectedModCount = LinkedHashMap.this.modCount;
        }

        @Override
        public final boolean hasNext() {
            return this.next != LinkedHashMap.this.header;
        }

        final LinkedEntry<K, V> nextEntry() {
            if (LinkedHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            LinkedEntry<K, V> e = this.next;
            if (e == LinkedHashMap.this.header) {
                throw new NoSuchElementException();
            }
            this.next = e.nxt;
            this.lastReturned = e;
            return e;
        }

        @Override
        public final void remove() {
            if (LinkedHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (this.lastReturned == null) {
                throw new IllegalStateException();
            }
            LinkedHashMap.this.remove(this.lastReturned.key);
            this.lastReturned = null;
            this.expectedModCount = LinkedHashMap.this.modCount;
        }
    }

    private final class KeyIterator extends LinkedHashMap<K, V>.LinkedHashIterator<K> {
        private KeyIterator() {
            super();
        }

        @Override
        public final K next() {
            return nextEntry().key;
        }
    }

    private final class ValueIterator extends LinkedHashMap<K, V>.LinkedHashIterator<V> {
        private ValueIterator() {
            super();
        }

        @Override
        public final V next() {
            return nextEntry().value;
        }
    }

    private final class EntryIterator extends LinkedHashMap<K, V>.LinkedHashIterator<Map.Entry<K, V>> {
        private EntryIterator() {
            super();
        }

        @Override
        public final Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    @Override
    Iterator<K> newKeyIterator() {
        return new KeyIterator();
    }

    @Override
    Iterator<V> newValueIterator() {
        return new ValueIterator();
    }

    @Override
    Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new EntryIterator();
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return false;
    }
}
