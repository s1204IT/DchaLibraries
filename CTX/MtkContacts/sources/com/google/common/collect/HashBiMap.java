package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public final class HashBiMap<K, V> extends AbstractMap<K, V> implements BiMap<K, V>, Serializable {
    private static final double LOAD_FACTOR = 1.0d;
    private static final long serialVersionUID = 0;
    private transient BiEntry<K, V>[] hashTableKToV;
    private transient BiEntry<K, V>[] hashTableVToK;
    private transient BiMap<V, K> inverse;
    private transient int mask;
    private transient int modCount;
    private transient int size;

    public static <K, V> HashBiMap<K, V> create() {
        return create(16);
    }

    public static <K, V> HashBiMap<K, V> create(int i) {
        return new HashBiMap<>(i);
    }

    public static <K, V> HashBiMap<K, V> create(Map<? extends K, ? extends V> map) {
        HashBiMap<K, V> hashBiMapCreate = create(map.size());
        hashBiMapCreate.putAll(map);
        return hashBiMapCreate;
    }

    private static final class BiEntry<K, V> extends ImmutableEntry<K, V> {
        final int keyHash;
        BiEntry<K, V> nextInKToVBucket;
        BiEntry<K, V> nextInVToKBucket;
        final int valueHash;

        BiEntry(K k, int i, V v, int i2) {
            super(k, v);
            this.keyHash = i;
            this.valueHash = i2;
        }
    }

    private HashBiMap(int i) {
        init(i);
    }

    private void init(int i) {
        CollectPreconditions.checkNonnegative(i, "expectedSize");
        int iClosedTableSize = Hashing.closedTableSize(i, LOAD_FACTOR);
        this.hashTableKToV = createTable(iClosedTableSize);
        this.hashTableVToK = createTable(iClosedTableSize);
        this.mask = iClosedTableSize - 1;
        this.modCount = 0;
        this.size = 0;
    }

    private void delete(BiEntry<K, V> biEntry) {
        BiEntry<K, V> biEntry2;
        int i = biEntry.keyHash & this.mask;
        BiEntry<K, V> biEntry3 = null;
        BiEntry<K, V> biEntry4 = null;
        for (BiEntry<K, V> biEntry5 = this.hashTableKToV[i]; biEntry5 != biEntry; biEntry5 = biEntry5.nextInKToVBucket) {
            biEntry4 = biEntry5;
        }
        if (biEntry4 == null) {
            this.hashTableKToV[i] = biEntry.nextInKToVBucket;
        } else {
            biEntry4.nextInKToVBucket = biEntry.nextInKToVBucket;
        }
        int i2 = biEntry.valueHash & this.mask;
        BiEntry<K, V> biEntry6 = this.hashTableVToK[i2];
        while (true) {
            biEntry2 = biEntry3;
            biEntry3 = biEntry6;
            if (biEntry3 == biEntry) {
                break;
            } else {
                biEntry6 = biEntry3.nextInVToKBucket;
            }
        }
        if (biEntry2 == null) {
            this.hashTableVToK[i2] = biEntry.nextInVToKBucket;
        } else {
            biEntry2.nextInVToKBucket = biEntry.nextInVToKBucket;
        }
        this.size--;
        this.modCount++;
    }

    private void insert(BiEntry<K, V> biEntry) {
        int i = biEntry.keyHash & this.mask;
        biEntry.nextInKToVBucket = this.hashTableKToV[i];
        this.hashTableKToV[i] = biEntry;
        int i2 = biEntry.valueHash & this.mask;
        biEntry.nextInVToKBucket = this.hashTableVToK[i2];
        this.hashTableVToK[i2] = biEntry;
        this.size++;
        this.modCount++;
    }

    private static int hash(Object obj) {
        return Hashing.smear(obj == null ? 0 : obj.hashCode());
    }

    private BiEntry<K, V> seekByKey(Object obj, int i) {
        for (BiEntry<K, V> biEntry = this.hashTableKToV[this.mask & i]; biEntry != null; biEntry = biEntry.nextInKToVBucket) {
            if (i == biEntry.keyHash && Objects.equal(obj, biEntry.key)) {
                return biEntry;
            }
        }
        return null;
    }

    private BiEntry<K, V> seekByValue(Object obj, int i) {
        for (BiEntry<K, V> biEntry = this.hashTableVToK[this.mask & i]; biEntry != null; biEntry = biEntry.nextInVToKBucket) {
            if (i == biEntry.valueHash && Objects.equal(obj, biEntry.value)) {
                return biEntry;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(Object obj) {
        return seekByKey(obj, hash(obj)) != null;
    }

    @Override
    public boolean containsValue(Object obj) {
        return seekByValue(obj, hash(obj)) != null;
    }

    @Override
    public V get(Object obj) {
        BiEntry<K, V> biEntrySeekByKey = seekByKey(obj, hash(obj));
        if (biEntrySeekByKey == null) {
            return null;
        }
        return biEntrySeekByKey.value;
    }

    @Override
    public V put(K k, V v) {
        return put(k, v, false);
    }

    @Override
    public V forcePut(K k, V v) {
        return put(k, v, true);
    }

    private V put(K k, V v, boolean z) {
        int iHash = hash(k);
        int iHash2 = hash(v);
        BiEntry<K, V> biEntrySeekByKey = seekByKey(k, iHash);
        if (biEntrySeekByKey != null && iHash2 == biEntrySeekByKey.valueHash && Objects.equal(v, biEntrySeekByKey.value)) {
            return v;
        }
        BiEntry<K, V> biEntrySeekByValue = seekByValue(v, iHash2);
        if (biEntrySeekByValue != null) {
            if (z) {
                delete(biEntrySeekByValue);
            } else {
                throw new IllegalArgumentException("value already present: " + v);
            }
        }
        if (biEntrySeekByKey != null) {
            delete(biEntrySeekByKey);
        }
        insert(new BiEntry<>(k, iHash, v, iHash2));
        rehashIfNecessary();
        if (biEntrySeekByKey == null) {
            return null;
        }
        return biEntrySeekByKey.value;
    }

    private K putInverse(V v, K k, boolean z) {
        int iHash = hash(v);
        int iHash2 = hash(k);
        BiEntry<K, V> biEntrySeekByValue = seekByValue(v, iHash);
        if (biEntrySeekByValue != null && iHash2 == biEntrySeekByValue.keyHash && Objects.equal(k, biEntrySeekByValue.key)) {
            return k;
        }
        BiEntry<K, V> biEntrySeekByKey = seekByKey(k, iHash2);
        if (biEntrySeekByKey != null) {
            if (z) {
                delete(biEntrySeekByKey);
            } else {
                throw new IllegalArgumentException("value already present: " + k);
            }
        }
        if (biEntrySeekByValue != null) {
            delete(biEntrySeekByValue);
        }
        insert(new BiEntry<>(k, iHash2, v, iHash));
        rehashIfNecessary();
        if (biEntrySeekByValue == null) {
            return null;
        }
        return biEntrySeekByValue.key;
    }

    private void rehashIfNecessary() {
        BiEntry<K, V>[] biEntryArr = this.hashTableKToV;
        if (Hashing.needsResizing(this.size, biEntryArr.length, LOAD_FACTOR)) {
            int length = biEntryArr.length * 2;
            this.hashTableKToV = createTable(length);
            this.hashTableVToK = createTable(length);
            this.mask = length - 1;
            this.size = 0;
            for (BiEntry<K, V> biEntry : biEntryArr) {
                while (biEntry != null) {
                    BiEntry<K, V> biEntry2 = biEntry.nextInKToVBucket;
                    insert(biEntry);
                    biEntry = biEntry2;
                }
            }
            this.modCount++;
        }
    }

    private BiEntry<K, V>[] createTable(int i) {
        return new BiEntry[i];
    }

    @Override
    public V remove(Object obj) {
        BiEntry<K, V> biEntrySeekByKey = seekByKey(obj, hash(obj));
        if (biEntrySeekByKey == null) {
            return null;
        }
        delete(biEntrySeekByKey);
        return biEntrySeekByKey.value;
    }

    @Override
    public void clear() {
        this.size = 0;
        Arrays.fill(this.hashTableKToV, (Object) null);
        Arrays.fill(this.hashTableVToK, (Object) null);
        this.modCount++;
    }

    @Override
    public int size() {
        return this.size;
    }

    abstract class Itr<T> implements Iterator<T> {
        int expectedModCount;
        int nextBucket = 0;
        BiEntry<K, V> next = null;
        BiEntry<K, V> toRemove = null;

        abstract T output(BiEntry<K, V> biEntry);

        Itr() {
            this.expectedModCount = HashBiMap.this.modCount;
        }

        private void checkForConcurrentModification() {
            if (HashBiMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean hasNext() {
            checkForConcurrentModification();
            if (this.next != null) {
                return true;
            }
            while (this.nextBucket < HashBiMap.this.hashTableKToV.length) {
                if (HashBiMap.this.hashTableKToV[this.nextBucket] != null) {
                    BiEntry<K, V>[] biEntryArr = HashBiMap.this.hashTableKToV;
                    int i = this.nextBucket;
                    this.nextBucket = i + 1;
                    this.next = biEntryArr[i];
                    return true;
                }
                this.nextBucket++;
            }
            return false;
        }

        @Override
        public T next() {
            checkForConcurrentModification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            BiEntry<K, V> biEntry = this.next;
            this.next = biEntry.nextInKToVBucket;
            this.toRemove = biEntry;
            return output(biEntry);
        }

        @Override
        public void remove() {
            checkForConcurrentModification();
            CollectPreconditions.checkRemove(this.toRemove != null);
            HashBiMap.this.delete(this.toRemove);
            this.expectedModCount = HashBiMap.this.modCount;
            this.toRemove = null;
        }
    }

    @Override
    public Set<K> keySet() {
        return new KeySet();
    }

    private final class KeySet extends Maps.KeySet<K, V> {
        KeySet() {
            super(HashBiMap.this);
        }

        @Override
        public Iterator<K> iterator() {
            return new HashBiMap<K, V>.Itr<K>() {
                {
                    HashBiMap hashBiMap = HashBiMap.this;
                }

                K output(BiEntry<K, V> biEntry) {
                    return biEntry.key;
                }
            };
        }

        @Override
        public boolean remove(Object obj) {
            BiEntry biEntrySeekByKey = HashBiMap.this.seekByKey(obj, HashBiMap.hash(obj));
            if (biEntrySeekByKey != null) {
                HashBiMap.this.delete(biEntrySeekByKey);
                return true;
            }
            return false;
        }
    }

    @Override
    public Set<V> values() {
        return inverse().keySet();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    private final class EntrySet extends Maps.EntrySet<K, V> {
        private EntrySet() {
        }

        @Override
        Map<K, V> map() {
            return HashBiMap.this;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new HashBiMap<K, V>.Itr<Map.Entry<K, V>>() {
                {
                    HashBiMap hashBiMap = HashBiMap.this;
                }

                Map.Entry<K, V> output(BiEntry<K, V> biEntry) {
                    return new MapEntry(biEntry);
                }

                class MapEntry extends AbstractMapEntry<K, V> {
                    BiEntry<K, V> delegate;

                    MapEntry(BiEntry<K, V> biEntry) {
                        this.delegate = biEntry;
                    }

                    @Override
                    public K getKey() {
                        return this.delegate.key;
                    }

                    @Override
                    public V getValue() {
                        return this.delegate.value;
                    }

                    @Override
                    public V setValue(V v) {
                        V v2 = this.delegate.value;
                        int iHash = HashBiMap.hash(v);
                        if (iHash == this.delegate.valueHash && Objects.equal(v, v2)) {
                            return v;
                        }
                        Preconditions.checkArgument(HashBiMap.this.seekByValue(v, iHash) == null, "value already present: %s", v);
                        HashBiMap.this.delete(this.delegate);
                        BiEntry<K, V> biEntry = new BiEntry<>(this.delegate.key, this.delegate.keyHash, v, iHash);
                        HashBiMap.this.insert(biEntry);
                        AnonymousClass1.this.expectedModCount = HashBiMap.this.modCount;
                        if (AnonymousClass1.this.toRemove == this.delegate) {
                            AnonymousClass1.this.toRemove = biEntry;
                        }
                        this.delegate = biEntry;
                        return v2;
                    }
                }
            };
        }
    }

    @Override
    public BiMap<V, K> inverse() {
        if (this.inverse != null) {
            return this.inverse;
        }
        Inverse inverse = new Inverse();
        this.inverse = inverse;
        return inverse;
    }

    private final class Inverse extends AbstractMap<V, K> implements BiMap<V, K>, Serializable {
        private Inverse() {
        }

        BiMap<K, V> forward() {
            return HashBiMap.this;
        }

        @Override
        public int size() {
            return HashBiMap.this.size;
        }

        @Override
        public void clear() {
            forward().clear();
        }

        @Override
        public boolean containsKey(Object obj) {
            return forward().containsValue(obj);
        }

        @Override
        public K get(Object obj) {
            BiEntry biEntrySeekByValue = HashBiMap.this.seekByValue(obj, HashBiMap.hash(obj));
            if (biEntrySeekByValue == null) {
                return null;
            }
            return biEntrySeekByValue.key;
        }

        @Override
        public K put(V v, K k) {
            return (K) HashBiMap.this.putInverse(v, k, false);
        }

        @Override
        public K forcePut(V v, K k) {
            return (K) HashBiMap.this.putInverse(v, k, true);
        }

        @Override
        public K remove(Object obj) {
            BiEntry biEntrySeekByValue = HashBiMap.this.seekByValue(obj, HashBiMap.hash(obj));
            if (biEntrySeekByValue != null) {
                HashBiMap.this.delete(biEntrySeekByValue);
                return biEntrySeekByValue.key;
            }
            return null;
        }

        @Override
        public BiMap<K, V> inverse() {
            return forward();
        }

        @Override
        public Set<V> keySet() {
            return new InverseKeySet();
        }

        private final class InverseKeySet extends Maps.KeySet<V, K> {
            InverseKeySet() {
                super(Inverse.this);
            }

            @Override
            public boolean remove(Object obj) {
                BiEntry biEntrySeekByValue = HashBiMap.this.seekByValue(obj, HashBiMap.hash(obj));
                if (biEntrySeekByValue != null) {
                    HashBiMap.this.delete(biEntrySeekByValue);
                    return true;
                }
                return false;
            }

            @Override
            public Iterator<V> iterator() {
                return new HashBiMap<K, V>.Itr<V>() {
                    {
                        HashBiMap hashBiMap = HashBiMap.this;
                    }

                    V output(BiEntry<K, V> biEntry) {
                        return biEntry.value;
                    }
                };
            }
        }

        @Override
        public Set<K> values() {
            return forward().keySet();
        }

        class AnonymousClass1 extends Maps.EntrySet<V, K> {
            AnonymousClass1() {
            }

            @Override
            Map<V, K> map() {
                return Inverse.this;
            }

            @Override
            public Iterator<Map.Entry<V, K>> iterator() {
                return new HashBiMap<K, V>.Itr<Map.Entry<V, K>>() {
                    {
                        HashBiMap hashBiMap = HashBiMap.this;
                    }

                    Map.Entry<V, K> output(BiEntry<K, V> biEntry) {
                        return new InverseEntry(biEntry);
                    }

                    class InverseEntry extends AbstractMapEntry<V, K> {
                        BiEntry<K, V> delegate;

                        InverseEntry(BiEntry<K, V> biEntry) {
                            this.delegate = biEntry;
                        }

                        @Override
                        public V getKey() {
                            return this.delegate.value;
                        }

                        @Override
                        public K getValue() {
                            return this.delegate.key;
                        }

                        @Override
                        public K setValue(K k) {
                            K k2 = this.delegate.key;
                            int iHash = HashBiMap.hash(k);
                            if (iHash != this.delegate.keyHash || !Objects.equal(k, k2)) {
                                Preconditions.checkArgument(HashBiMap.this.seekByKey(k, iHash) == null, "value already present: %s", k);
                                HashBiMap.this.delete(this.delegate);
                                HashBiMap.this.insert(new BiEntry(k, iHash, this.delegate.value, this.delegate.valueHash));
                                C00131.this.expectedModCount = HashBiMap.this.modCount;
                                return k2;
                            }
                            return k;
                        }
                    }
                };
            }
        }

        @Override
        public Set<Map.Entry<V, K>> entrySet() {
            return new AnonymousClass1();
        }

        Object writeReplace() {
            return new InverseSerializedForm(HashBiMap.this);
        }
    }

    private static final class InverseSerializedForm<K, V> implements Serializable {
        private final HashBiMap<K, V> bimap;

        InverseSerializedForm(HashBiMap<K, V> hashBiMap) {
            this.bimap = hashBiMap;
        }

        Object readResolve() {
            return this.bimap.inverse();
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        Serialization.writeMap(this, objectOutputStream);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        int count = Serialization.readCount(objectInputStream);
        init(count);
        Serialization.populateMap(this, objectInputStream, count);
    }
}
