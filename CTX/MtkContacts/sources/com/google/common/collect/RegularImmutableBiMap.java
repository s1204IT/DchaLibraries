package com.google.common.collect;

import com.google.common.collect.ImmutableMapEntry;
import java.io.Serializable;
import java.util.Map;

class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
    static final double MAX_LOAD_FACTOR = 1.2d;
    private final transient ImmutableMapEntry<K, V>[] entries;
    private final transient int hashCode;
    private transient ImmutableBiMap<V, K> inverse;
    private final transient ImmutableMapEntry<K, V>[] keyTable;
    private final transient int mask;
    private final transient ImmutableMapEntry<K, V>[] valueTable;

    RegularImmutableBiMap(ImmutableMapEntry.TerminalEntry<?, ?>... terminalEntryArr) {
        this(terminalEntryArr.length, terminalEntryArr);
    }

    RegularImmutableBiMap(int i, ImmutableMapEntry.TerminalEntry<?, ?>[] terminalEntryArr) {
        ImmutableMapEntry<K, V> nonTerminalBiMapEntry;
        int i2 = i;
        int iClosedTableSize = Hashing.closedTableSize(i2, MAX_LOAD_FACTOR);
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

    RegularImmutableBiMap(Map.Entry<?, ?>[] entryArr) {
        ImmutableMapEntry<K, V> nonTerminalBiMapEntry;
        RegularImmutableBiMap<K, V> regularImmutableBiMap = this;
        Map.Entry<?, ?>[] entryArr2 = entryArr;
        int i = 0;
        int length = entryArr2.length;
        int iClosedTableSize = Hashing.closedTableSize(length, MAX_LOAD_FACTOR);
        regularImmutableBiMap.mask = iClosedTableSize - 1;
        ImmutableMapEntry<K, V>[] immutableMapEntryArrCreateEntryArray = createEntryArray(iClosedTableSize);
        ImmutableMapEntry<K, V>[] immutableMapEntryArrCreateEntryArray2 = createEntryArray(iClosedTableSize);
        ImmutableMapEntry<K, V>[] immutableMapEntryArrCreateEntryArray3 = createEntryArray(length);
        int i2 = 0;
        while (i < length) {
            Map.Entry<?, ?> entry = entryArr2[i];
            Object key = entry.getKey();
            Object value = entry.getValue();
            CollectPreconditions.checkEntryNotNull(key, value);
            int iHashCode = key.hashCode();
            int iHashCode2 = value.hashCode();
            int iSmear = Hashing.smear(iHashCode) & regularImmutableBiMap.mask;
            int iSmear2 = Hashing.smear(iHashCode2) & regularImmutableBiMap.mask;
            ImmutableMapEntry<K, V> immutableMapEntry = immutableMapEntryArrCreateEntryArray[iSmear];
            ImmutableMapEntry<K, V> nextInKeyBucket = immutableMapEntry;
            while (nextInKeyBucket != null) {
                checkNoConflict(!key.equals(nextInKeyBucket.getKey()), "key", entry, nextInKeyBucket);
                nextInKeyBucket = nextInKeyBucket.getNextInKeyBucket();
                length = length;
            }
            int i3 = length;
            ImmutableMapEntry<K, V> immutableMapEntry2 = immutableMapEntryArrCreateEntryArray2[iSmear2];
            ImmutableMapEntry<K, V> nextInValueBucket = immutableMapEntry2;
            while (nextInValueBucket != null) {
                checkNoConflict(!value.equals(nextInValueBucket.getValue()), "value", entry, nextInValueBucket);
                nextInValueBucket = nextInValueBucket.getNextInValueBucket();
                i2 = i2;
            }
            int i4 = i2;
            if (immutableMapEntry == null && immutableMapEntry2 == null) {
                nonTerminalBiMapEntry = new ImmutableMapEntry.TerminalEntry<>(key, value);
            } else {
                nonTerminalBiMapEntry = new NonTerminalBiMapEntry(key, value, immutableMapEntry, immutableMapEntry2);
            }
            immutableMapEntryArrCreateEntryArray[iSmear] = nonTerminalBiMapEntry;
            immutableMapEntryArrCreateEntryArray2[iSmear2] = nonTerminalBiMapEntry;
            immutableMapEntryArrCreateEntryArray3[i] = nonTerminalBiMapEntry;
            i2 = i4 + (iHashCode ^ iHashCode2);
            i++;
            length = i3;
            regularImmutableBiMap = this;
            entryArr2 = entryArr;
        }
        this.keyTable = immutableMapEntryArrCreateEntryArray;
        this.valueTable = immutableMapEntryArrCreateEntryArray2;
        this.entries = immutableMapEntryArrCreateEntryArray3;
        this.hashCode = i2;
    }

    private static final class NonTerminalBiMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        private final ImmutableMapEntry<K, V> nextInKeyBucket;
        private final ImmutableMapEntry<K, V> nextInValueBucket;

        NonTerminalBiMapEntry(K k, V v, ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2) {
            super(k, v);
            this.nextInKeyBucket = immutableMapEntry;
            this.nextInValueBucket = immutableMapEntry2;
        }

        NonTerminalBiMapEntry(ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2, ImmutableMapEntry<K, V> immutableMapEntry3) {
            super(immutableMapEntry);
            this.nextInKeyBucket = immutableMapEntry2;
            this.nextInValueBucket = immutableMapEntry3;
        }

        @Override
        ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return this.nextInKeyBucket;
        }

        @Override
        ImmutableMapEntry<K, V> getNextInValueBucket() {
            return this.nextInValueBucket;
        }
    }

    private static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int i) {
        return new ImmutableMapEntry[i];
    }

    @Override
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

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet<K, V>() {
            @Override
            ImmutableMap<K, V> map() {
                return RegularImmutableBiMap.this;
            }

            @Override
            public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
                return asList().iterator();
            }

            @Override
            ImmutableList<Map.Entry<K, V>> createAsList() {
                return new RegularImmutableAsList(this, RegularImmutableBiMap.this.entries);
            }

            @Override
            boolean isHashCodeFast() {
                return true;
            }

            @Override
            public int hashCode() {
                return RegularImmutableBiMap.this.hashCode;
            }
        };
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public int size() {
        return this.entries.length;
    }

    @Override
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

        @Override
        public int size() {
            return inverse().size();
        }

        @Override
        public ImmutableBiMap<K, V> inverse() {
            return RegularImmutableBiMap.this;
        }

        @Override
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

        @Override
        ImmutableSet<Map.Entry<V, K>> createEntrySet() {
            return new InverseEntrySet();
        }

        final class InverseEntrySet extends ImmutableMapEntrySet<V, K> {
            InverseEntrySet() {
            }

            @Override
            ImmutableMap<V, K> map() {
                return Inverse.this;
            }

            @Override
            boolean isHashCodeFast() {
                return true;
            }

            @Override
            public int hashCode() {
                return RegularImmutableBiMap.this.hashCode;
            }

            @Override
            public UnmodifiableIterator<Map.Entry<V, K>> iterator() {
                return asList().iterator();
            }

            @Override
            ImmutableList<Map.Entry<V, K>> createAsList() {
                return new ImmutableAsList<Map.Entry<V, K>>() {
                    @Override
                    public Map.Entry<V, K> get(int i) {
                        ImmutableMapEntry immutableMapEntry = RegularImmutableBiMap.this.entries[i];
                        return Maps.immutableEntry(immutableMapEntry.getValue(), immutableMapEntry.getKey());
                    }

                    @Override
                    ImmutableCollection<Map.Entry<V, K>> delegateCollection() {
                        return InverseEntrySet.this;
                    }
                };
            }
        }

        @Override
        boolean isPartialView() {
            return false;
        }

        @Override
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
