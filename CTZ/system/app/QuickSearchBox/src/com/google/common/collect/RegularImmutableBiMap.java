package com.google.common.collect;

import com.google.common.collect.ImmutableMapEntry;
import java.io.Serializable;
import java.util.Map;

/* loaded from: classes.dex */
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
    private final transient ImmutableMapEntry<K, V>[] entries;
    private final transient int hashCode;
    private transient ImmutableBiMap<V, K> inverse;
    private final transient ImmutableMapEntry<K, V>[] keyTable;
    private final transient int mask;
    private final transient ImmutableMapEntry<K, V>[] valueTable;

    RegularImmutableBiMap(int i, ImmutableMapEntry.TerminalEntry<?, ?>[] terminalEntryArr) {
        ImmutableMapEntry<K, V> nonTerminalBiMapEntry;
        int i2 = i;
        int iClosedTableSize = Hashing.closedTableSize(i2, 1.2d);
        this.mask = iClosedTableSize - 1;
        ImmutableMapEntry<K, V>[] immutableMapEntryArrCreateEntryArray = createEntryArray(iClosedTableSize);
        ImmutableMapEntry<K, V>[] immutableMapEntryArrCreateEntryArray2 = createEntryArray(iClosedTableSize);
        ImmutableMapEntry<K, V>[] immutableMapEntryArrCreateEntryArray3 = createEntryArray(i);
        int i3 = 0;
        int i4 = 0;
        while (i3 < i2) {
            ImmutableMapEntry.TerminalEntry<?, ?> terminalEntry = terminalEntryArr[i3];
            Object key = terminalEntry.getKey();
            Object value = terminalEntry.getValue();
            int iHashCode = key.hashCode();
            int iHashCode2 = value.hashCode();
            int iSmear = Hashing.smear(iHashCode) & this.mask;
            int iSmear2 = Hashing.smear(iHashCode2) & this.mask;
            ImmutableMapEntry<K, V> immutableMapEntry = immutableMapEntryArrCreateEntryArray[iSmear];
            ImmutableMapEntry<K, V> nextInKeyBucket = immutableMapEntry;
            while (nextInKeyBucket != null) {
                checkNoConflict(!key.equals(nextInKeyBucket.getKey()), "key", terminalEntry, nextInKeyBucket);
                nextInKeyBucket = nextInKeyBucket.getNextInKeyBucket();
                key = key;
            }
            ImmutableMapEntry<K, V> immutableMapEntry2 = immutableMapEntryArrCreateEntryArray2[iSmear2];
            ImmutableMapEntry<K, V> nextInValueBucket = immutableMapEntry2;
            while (nextInValueBucket != null) {
                checkNoConflict(!value.equals(nextInValueBucket.getValue()), "value", terminalEntry, nextInValueBucket);
                nextInValueBucket = nextInValueBucket.getNextInValueBucket();
                value = value;
            }
            if (immutableMapEntry != null || immutableMapEntry2 != null) {
                nonTerminalBiMapEntry = new NonTerminalBiMapEntry<>(terminalEntry, immutableMapEntry, immutableMapEntry2);
            } else {
                nonTerminalBiMapEntry = terminalEntry;
            }
            immutableMapEntryArrCreateEntryArray[iSmear] = nonTerminalBiMapEntry;
            immutableMapEntryArrCreateEntryArray2[iSmear2] = nonTerminalBiMapEntry;
            immutableMapEntryArrCreateEntryArray3[i3] = nonTerminalBiMapEntry;
            i4 += iHashCode ^ iHashCode2;
            i3++;
            i2 = i;
        }
        this.keyTable = immutableMapEntryArrCreateEntryArray;
        this.valueTable = immutableMapEntryArrCreateEntryArray2;
        this.entries = immutableMapEntryArrCreateEntryArray3;
        this.hashCode = i4;
    }

    private static final class NonTerminalBiMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        private final ImmutableMapEntry<K, V> nextInKeyBucket;
        private final ImmutableMapEntry<K, V> nextInValueBucket;

        NonTerminalBiMapEntry(ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2, ImmutableMapEntry<K, V> immutableMapEntry3) {
            super(immutableMapEntry);
            this.nextInKeyBucket = immutableMapEntry2;
            this.nextInValueBucket = immutableMapEntry3;
        }

        @Override // com.google.common.collect.ImmutableMapEntry
        ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return this.nextInKeyBucket;
        }

        @Override // com.google.common.collect.ImmutableMapEntry
        ImmutableMapEntry<K, V> getNextInValueBucket() {
            return this.nextInValueBucket;
        }
    }

    private static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int i) {
        return new ImmutableMapEntry[i];
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public V get(Object obj) {
        if (obj == null) {
            return null;
        }
        for (ImmutableMapEntry<K, V> nextInKeyBucket = this.keyTable[Hashing.smear(obj.hashCode()) & this.mask]; nextInKeyBucket != null; nextInKeyBucket = nextInKeyBucket.getNextInKeyBucket()) {
            if (obj.equals(nextInKeyBucket.getKey())) {
                return nextInKeyBucket.getValue();
            }
        }
        return null;
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet<K, V>() { // from class: com.google.common.collect.RegularImmutableBiMap.1
            @Override // com.google.common.collect.ImmutableMapEntrySet
            ImmutableMap<K, V> map() {
                return RegularImmutableBiMap.this;
            }

            /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
            @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
            public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
                return asList().iterator();
            }

            @Override // com.google.common.collect.ImmutableCollection
            ImmutableList<Map.Entry<K, V>> createAsList() {
                return new RegularImmutableAsList(this, RegularImmutableBiMap.this.entries);
            }

            @Override // com.google.common.collect.ImmutableSet
            boolean isHashCodeFast() {
                return true;
            }

            @Override // com.google.common.collect.ImmutableSet, java.util.Collection, java.util.Set
            public int hashCode() {
                return RegularImmutableBiMap.this.hashCode;
            }
        };
    }

    @Override // com.google.common.collect.ImmutableMap
    boolean isPartialView() {
        return false;
    }

    @Override // java.util.Map
    public int size() {
        return this.entries.length;
    }

    @Override // com.google.common.collect.ImmutableBiMap
    public ImmutableBiMap<V, K> inverse() {
        ImmutableBiMap<V, K> immutableBiMap = this.inverse;
        if (immutableBiMap != null) {
            return immutableBiMap;
        }
        Inverse inverse = new Inverse();
        this.inverse = inverse;
        return inverse;
    }

    private final class Inverse extends ImmutableBiMap<V, K> {
        private Inverse() {
        }

        @Override // java.util.Map
        public int size() {
            return inverse().size();
        }

        @Override // com.google.common.collect.ImmutableBiMap
        public ImmutableBiMap<K, V> inverse() {
            return RegularImmutableBiMap.this;
        }

        @Override // com.google.common.collect.ImmutableMap, java.util.Map
        public K get(Object obj) {
            if (obj != null) {
                for (ImmutableMapEntry nextInValueBucket = RegularImmutableBiMap.this.valueTable[Hashing.smear(obj.hashCode()) & RegularImmutableBiMap.this.mask]; nextInValueBucket != null; nextInValueBucket = nextInValueBucket.getNextInValueBucket()) {
                    if (obj.equals(nextInValueBucket.getValue())) {
                        return nextInValueBucket.getKey();
                    }
                }
                return null;
            }
            return null;
        }

        @Override // com.google.common.collect.ImmutableMap
        ImmutableSet<Map.Entry<V, K>> createEntrySet() {
            return new InverseEntrySet();
        }

        final class InverseEntrySet extends ImmutableMapEntrySet<V, K> {
            InverseEntrySet() {
            }

            @Override // com.google.common.collect.ImmutableMapEntrySet
            ImmutableMap<V, K> map() {
                return Inverse.this;
            }

            @Override // com.google.common.collect.ImmutableSet
            boolean isHashCodeFast() {
                return true;
            }

            @Override // com.google.common.collect.ImmutableSet, java.util.Collection, java.util.Set
            public int hashCode() {
                return RegularImmutableBiMap.this.hashCode;
            }

            /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
            @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
            public UnmodifiableIterator<Map.Entry<V, K>> iterator() {
                return asList().iterator();
            }

            @Override // com.google.common.collect.ImmutableCollection
            ImmutableList<Map.Entry<V, K>> createAsList() {
                return new ImmutableAsList<Map.Entry<V, K>>() { // from class: com.google.common.collect.RegularImmutableBiMap.Inverse.InverseEntrySet.1
                    /* JADX DEBUG: Method merged with bridge method: get(I)Ljava/lang/Object; */
                    @Override // java.util.List
                    public Map.Entry<V, K> get(int i) {
                        ImmutableMapEntry immutableMapEntry = RegularImmutableBiMap.this.entries[i];
                        return Maps.immutableEntry(immutableMapEntry.getValue(), immutableMapEntry.getKey());
                    }

                    @Override // com.google.common.collect.ImmutableAsList
                    ImmutableCollection<Map.Entry<V, K>> delegateCollection() {
                        return InverseEntrySet.this;
                    }
                };
            }
        }

        @Override // com.google.common.collect.ImmutableMap
        boolean isPartialView() {
            return false;
        }

        @Override // com.google.common.collect.ImmutableBiMap, com.google.common.collect.ImmutableMap
        Object writeReplace() {
            return new InverseSerializedForm(RegularImmutableBiMap.this);
        }
    }

    private static class InverseSerializedForm<K, V> implements Serializable {
        private static final long serialVersionUID = 1;
        private final ImmutableBiMap<K, V> forward;

        InverseSerializedForm(ImmutableBiMap<K, V> immutableBiMap) {
            this.forward = immutableBiMap;
        }

        Object readResolve() {
            return this.forward.inverse();
        }
    }
}
