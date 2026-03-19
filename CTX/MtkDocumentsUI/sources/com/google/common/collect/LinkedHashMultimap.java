package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public final class LinkedHashMultimap<K, V> extends AbstractSetMultimap<K, V> {
    static final double VALUE_SET_LOAD_FACTOR = 1.0d;
    private static final long serialVersionUID = 1;
    private transient ValueEntry<K, V> multimapHeaderEntry;
    transient int valueSetCapacity;

    private interface ValueSetLink<K, V> {
        ValueSetLink<K, V> getPredecessorInValueSet();

        ValueSetLink<K, V> getSuccessorInValueSet();

        void setPredecessorInValueSet(ValueSetLink<K, V> valueSetLink);

        void setSuccessorInValueSet(ValueSetLink<K, V> valueSetLink);
    }

    @Override
    public Map asMap() {
        return super.asMap();
    }

    @Override
    public boolean containsEntry(Object obj, Object obj2) {
        return super.containsEntry(obj, obj2);
    }

    @Override
    public boolean containsValue(Object obj) {
        return super.containsValue(obj);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public Set get(Object obj) {
        return super.get(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Set keySet() {
        return super.keySet();
    }

    @Override
    public boolean put(Object obj, Object obj2) {
        return super.put(obj, obj2);
    }

    @Override
    public boolean putAll(Object obj, Iterable iterable) {
        return super.putAll(obj, iterable);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        return super.remove(obj, obj2);
    }

    @Override
    public Set removeAll(Object obj) {
        return super.removeAll(obj);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    private static <K, V> void succeedsInValueSet(ValueSetLink<K, V> valueSetLink, ValueSetLink<K, V> valueSetLink2) {
        valueSetLink.setSuccessorInValueSet(valueSetLink2);
        valueSetLink2.setPredecessorInValueSet(valueSetLink);
    }

    private static <K, V> void succeedsInMultimap(ValueEntry<K, V> valueEntry, ValueEntry<K, V> valueEntry2) {
        valueEntry.setSuccessorInMultimap(valueEntry2);
        valueEntry2.setPredecessorInMultimap(valueEntry);
    }

    private static <K, V> void deleteFromValueSet(ValueSetLink<K, V> valueSetLink) {
        succeedsInValueSet(valueSetLink.getPredecessorInValueSet(), valueSetLink.getSuccessorInValueSet());
    }

    private static <K, V> void deleteFromMultimap(ValueEntry<K, V> valueEntry) {
        succeedsInMultimap(valueEntry.getPredecessorInMultimap(), valueEntry.getSuccessorInMultimap());
    }

    static final class ValueEntry<K, V> extends ImmutableEntry<K, V> implements ValueSetLink<K, V> {
        ValueEntry<K, V> nextInValueBucket;
        ValueEntry<K, V> predecessorInMultimap;
        ValueSetLink<K, V> predecessorInValueSet;
        final int smearedValueHash;
        ValueEntry<K, V> successorInMultimap;
        ValueSetLink<K, V> successorInValueSet;

        ValueEntry(K k, V v, int i, ValueEntry<K, V> valueEntry) {
            super(k, v);
            this.smearedValueHash = i;
            this.nextInValueBucket = valueEntry;
        }

        boolean matchesValue(Object obj, int i) {
            return this.smearedValueHash == i && Objects.equal(getValue(), obj);
        }

        @Override
        public ValueSetLink<K, V> getPredecessorInValueSet() {
            return this.predecessorInValueSet;
        }

        @Override
        public ValueSetLink<K, V> getSuccessorInValueSet() {
            return this.successorInValueSet;
        }

        @Override
        public void setPredecessorInValueSet(ValueSetLink<K, V> valueSetLink) {
            this.predecessorInValueSet = valueSetLink;
        }

        @Override
        public void setSuccessorInValueSet(ValueSetLink<K, V> valueSetLink) {
            this.successorInValueSet = valueSetLink;
        }

        public ValueEntry<K, V> getPredecessorInMultimap() {
            return this.predecessorInMultimap;
        }

        public ValueEntry<K, V> getSuccessorInMultimap() {
            return this.successorInMultimap;
        }

        public void setSuccessorInMultimap(ValueEntry<K, V> valueEntry) {
            this.successorInMultimap = valueEntry;
        }

        public void setPredecessorInMultimap(ValueEntry<K, V> valueEntry) {
            this.predecessorInMultimap = valueEntry;
        }
    }

    @Override
    Set<V> createCollection() {
        return new LinkedHashSet(this.valueSetCapacity);
    }

    @Override
    Collection<V> createCollection(K k) {
        return new ValueSet(k, this.valueSetCapacity);
    }

    @Override
    public Set<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return super.replaceValues((Object) k, (Iterable) iterable);
    }

    @Override
    public Set<Map.Entry<K, V>> entries() {
        return super.entries();
    }

    @Override
    public Collection<V> values() {
        return super.values();
    }

    final class ValueSet extends Sets.ImprovedAbstractSet<V> implements ValueSetLink<K, V> {
        ValueEntry<K, V>[] hashTable;
        private final K key;
        private int size = 0;
        private int modCount = 0;
        private ValueSetLink<K, V> firstEntry = this;
        private ValueSetLink<K, V> lastEntry = this;

        ValueSet(K k, int i) {
            this.key = k;
            this.hashTable = new ValueEntry[Hashing.closedTableSize(i, LinkedHashMultimap.VALUE_SET_LOAD_FACTOR)];
        }

        private int mask() {
            return this.hashTable.length - 1;
        }

        @Override
        public ValueSetLink<K, V> getPredecessorInValueSet() {
            return this.lastEntry;
        }

        @Override
        public ValueSetLink<K, V> getSuccessorInValueSet() {
            return this.firstEntry;
        }

        @Override
        public void setPredecessorInValueSet(ValueSetLink<K, V> valueSetLink) {
            this.lastEntry = valueSetLink;
        }

        @Override
        public void setSuccessorInValueSet(ValueSetLink<K, V> valueSetLink) {
            this.firstEntry = valueSetLink;
        }

        @Override
        public Iterator<V> iterator() {
            return new Iterator<V>() {
                int expectedModCount;
                ValueSetLink<K, V> nextEntry;
                ValueEntry<K, V> toRemove;

                {
                    this.nextEntry = ValueSet.this.firstEntry;
                    this.expectedModCount = ValueSet.this.modCount;
                }

                private void checkForComodification() {
                    if (ValueSet.this.modCount != this.expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                }

                @Override
                public boolean hasNext() {
                    checkForComodification();
                    return this.nextEntry != ValueSet.this;
                }

                @Override
                public V next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    ValueEntry<K, V> valueEntry = (ValueEntry) this.nextEntry;
                    V value = valueEntry.getValue();
                    this.toRemove = valueEntry;
                    this.nextEntry = valueEntry.getSuccessorInValueSet();
                    return value;
                }

                @Override
                public void remove() {
                    checkForComodification();
                    CollectPreconditions.checkRemove(this.toRemove != null);
                    ValueSet.this.remove(this.toRemove.getValue());
                    this.expectedModCount = ValueSet.this.modCount;
                    this.toRemove = null;
                }
            };
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public boolean contains(Object obj) {
            int iSmearedHash = Hashing.smearedHash(obj);
            for (ValueEntry<K, V> valueEntry = this.hashTable[mask() & iSmearedHash]; valueEntry != null; valueEntry = valueEntry.nextInValueBucket) {
                if (valueEntry.matchesValue(obj, iSmearedHash)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean add(V v) {
            int iSmearedHash = Hashing.smearedHash(v);
            int iMask = mask() & iSmearedHash;
            ValueEntry<K, V> valueEntry = this.hashTable[iMask];
            for (ValueEntry<K, V> valueEntry2 = valueEntry; valueEntry2 != null; valueEntry2 = valueEntry2.nextInValueBucket) {
                if (valueEntry2.matchesValue(v, iSmearedHash)) {
                    return false;
                }
            }
            ValueEntry<K, V> valueEntry3 = new ValueEntry<>(this.key, v, iSmearedHash, valueEntry);
            LinkedHashMultimap.succeedsInValueSet(this.lastEntry, valueEntry3);
            LinkedHashMultimap.succeedsInValueSet(valueEntry3, this);
            LinkedHashMultimap.succeedsInMultimap(LinkedHashMultimap.this.multimapHeaderEntry.getPredecessorInMultimap(), valueEntry3);
            LinkedHashMultimap.succeedsInMultimap(valueEntry3, LinkedHashMultimap.this.multimapHeaderEntry);
            this.hashTable[iMask] = valueEntry3;
            this.size++;
            this.modCount++;
            rehashIfNecessary();
            return true;
        }

        private void rehashIfNecessary() {
            if (Hashing.needsResizing(this.size, this.hashTable.length, LinkedHashMultimap.VALUE_SET_LOAD_FACTOR)) {
                ValueEntry<K, V>[] valueEntryArr = new ValueEntry[this.hashTable.length * 2];
                this.hashTable = valueEntryArr;
                int length = valueEntryArr.length - 1;
                for (ValueSetLink<K, V> successorInValueSet = this.firstEntry; successorInValueSet != this; successorInValueSet = successorInValueSet.getSuccessorInValueSet()) {
                    ValueEntry<K, V> valueEntry = (ValueEntry) successorInValueSet;
                    int i = valueEntry.smearedValueHash & length;
                    valueEntry.nextInValueBucket = valueEntryArr[i];
                    valueEntryArr[i] = valueEntry;
                }
            }
        }

        @Override
        public boolean remove(Object obj) {
            int iSmearedHash = Hashing.smearedHash(obj);
            int iMask = mask() & iSmearedHash;
            ValueEntry<K, V> valueEntry = this.hashTable[iMask];
            ValueEntry<K, V> valueEntry2 = null;
            while (true) {
                ValueEntry<K, V> valueEntry3 = valueEntry2;
                valueEntry2 = valueEntry;
                if (valueEntry2 != null) {
                    if (!valueEntry2.matchesValue(obj, iSmearedHash)) {
                        valueEntry = valueEntry2.nextInValueBucket;
                    } else {
                        if (valueEntry3 == null) {
                            this.hashTable[iMask] = valueEntry2.nextInValueBucket;
                        } else {
                            valueEntry3.nextInValueBucket = valueEntry2.nextInValueBucket;
                        }
                        LinkedHashMultimap.deleteFromValueSet(valueEntry2);
                        LinkedHashMultimap.deleteFromMultimap(valueEntry2);
                        this.size--;
                        this.modCount++;
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public void clear() {
            Arrays.fill(this.hashTable, (Object) null);
            this.size = 0;
            for (ValueSetLink<K, V> successorInValueSet = this.firstEntry; successorInValueSet != this; successorInValueSet = successorInValueSet.getSuccessorInValueSet()) {
                LinkedHashMultimap.deleteFromMultimap((ValueEntry) successorInValueSet);
            }
            LinkedHashMultimap.succeedsInValueSet(this, this);
            this.modCount++;
        }
    }

    @Override
    Iterator<Map.Entry<K, V>> entryIterator() {
        return new Iterator<Map.Entry<K, V>>() {
            ValueEntry<K, V> nextEntry;
            ValueEntry<K, V> toRemove;

            {
                this.nextEntry = LinkedHashMultimap.this.multimapHeaderEntry.successorInMultimap;
            }

            @Override
            public boolean hasNext() {
                return this.nextEntry != LinkedHashMultimap.this.multimapHeaderEntry;
            }

            @Override
            public Map.Entry<K, V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ValueEntry<K, V> valueEntry = this.nextEntry;
                this.toRemove = valueEntry;
                this.nextEntry = this.nextEntry.successorInMultimap;
                return valueEntry;
            }

            @Override
            public void remove() {
                CollectPreconditions.checkRemove(this.toRemove != null);
                LinkedHashMultimap.this.remove(this.toRemove.getKey(), this.toRemove.getValue());
                this.toRemove = null;
            }
        };
    }

    @Override
    Iterator<V> valueIterator() {
        return Maps.valueIterator(entryIterator());
    }

    @Override
    public void clear() {
        super.clear();
        succeedsInMultimap(this.multimapHeaderEntry, this.multimapHeaderEntry);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.valueSetCapacity);
        objectOutputStream.writeInt(keySet().size());
        Iterator it = keySet().iterator();
        while (it.hasNext()) {
            objectOutputStream.writeObject(it.next());
        }
        objectOutputStream.writeInt(size());
        for (Map.Entry<K, V> entry : entries()) {
            objectOutputStream.writeObject(entry.getKey());
            objectOutputStream.writeObject(entry.getValue());
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.multimapHeaderEntry = new ValueEntry<>(null, null, 0, null);
        succeedsInMultimap(this.multimapHeaderEntry, this.multimapHeaderEntry);
        this.valueSetCapacity = objectInputStream.readInt();
        int i = objectInputStream.readInt();
        LinkedHashMap linkedHashMap = new LinkedHashMap(Maps.capacity(i));
        for (int i2 = 0; i2 < i; i2++) {
            Object object = objectInputStream.readObject();
            linkedHashMap.put(object, createCollection(object));
        }
        int i3 = objectInputStream.readInt();
        for (int i4 = 0; i4 < i3; i4++) {
            ((Collection) linkedHashMap.get(objectInputStream.readObject())).add(objectInputStream.readObject());
        }
        setMap(linkedHashMap);
    }
}
