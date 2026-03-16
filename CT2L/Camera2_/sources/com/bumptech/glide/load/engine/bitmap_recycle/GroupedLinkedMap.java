package com.bumptech.glide.load.engine.bitmap_recycle;

import com.bumptech.glide.load.engine.bitmap_recycle.Poolable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GroupedLinkedMap<K extends Poolable, V> {
    private final LinkedEntry<K, V> head = new LinkedEntry<>();
    private final Map<K, LinkedEntry<K, V>> keyToEntry = new HashMap();

    GroupedLinkedMap() {
    }

    public void put(K key, V value) {
        LinkedEntry<K, V> entry = this.keyToEntry.get(key);
        if (entry == null) {
            entry = new LinkedEntry<>(key);
            makeTail(entry);
            this.keyToEntry.put(key, entry);
        } else {
            key.offer();
        }
        entry.add(value);
    }

    public V get(K key) {
        LinkedEntry<K, V> entry = this.keyToEntry.get(key);
        if (entry == null) {
            entry = new LinkedEntry<>(key);
            this.keyToEntry.put(key, entry);
        } else {
            key.offer();
        }
        makeHead(entry);
        return entry.removeLast();
    }

    public V removeLast() {
        for (LinkedEntry linkedEntry = this.head.prev; linkedEntry != this.head; linkedEntry = linkedEntry.prev) {
            V v = (V) linkedEntry.removeLast();
            if (v == null) {
                removeEntry(linkedEntry);
                this.keyToEntry.remove(linkedEntry.key);
                ((Poolable) linkedEntry.key).offer();
            } else {
                return v;
            }
        }
        return null;
    }

    public String toString() {
        String result = "GroupedLinkedMap( ";
        boolean hadAtLeastOneItem = false;
        for (LinkedEntry linkedEntry = this.head.next; linkedEntry != this.head; linkedEntry = linkedEntry.next) {
            hadAtLeastOneItem = true;
            result = result + "{" + linkedEntry.key + ":" + linkedEntry.size() + "}, ";
        }
        if (hadAtLeastOneItem) {
            result = result.substring(0, result.length() - 2);
        }
        return result + " )";
    }

    private void makeHead(LinkedEntry<K, V> entry) {
        removeEntry(entry);
        entry.prev = this.head;
        entry.next = this.head.next;
        updateEntry(entry);
    }

    private void makeTail(LinkedEntry<K, V> entry) {
        removeEntry(entry);
        entry.prev = this.head.prev;
        entry.next = this.head;
        updateEntry(entry);
    }

    private static void updateEntry(LinkedEntry entry) {
        entry.next.prev = entry;
        entry.prev.next = entry;
    }

    private static void removeEntry(LinkedEntry entry) {
        entry.prev.next = entry.next;
        entry.next.prev = entry.prev;
    }

    private static class LinkedEntry<K, V> {
        private final K key;
        LinkedEntry<K, V> next;
        LinkedEntry<K, V> prev;
        private List<V> values;

        public LinkedEntry() {
            this(null);
        }

        public LinkedEntry(K key) {
            this.prev = this;
            this.next = this;
            this.key = key;
        }

        public V removeLast() {
            int valueSize = size();
            if (valueSize > 0) {
                return this.values.remove(valueSize - 1);
            }
            return null;
        }

        public int size() {
            if (this.values != null) {
                return this.values.size();
            }
            return 0;
        }

        public void add(V value) {
            if (this.values == null) {
                this.values = new ArrayList();
            }
            this.values.add(value);
        }
    }
}
