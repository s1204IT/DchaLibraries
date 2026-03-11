package com.google.common.collect;

import com.google.common.collect.ImmutableMapEntry;
import java.io.Serializable;
import java.util.Map;

class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
    private final transient ImmutableMapEntry<K, V>[] entries;
    private final transient int hashCode;
    private transient ImmutableBiMap<V, K> inverse;
    private final transient ImmutableMapEntry<K, V>[] keyTable;
    private final transient int mask;
    private final transient ImmutableMapEntry<K, V>[] valueTable;

    private final class Inverse extends ImmutableBiMap<V, K> {
        final RegularImmutableBiMap this$0;

        final class InverseEntrySet extends ImmutableMapEntrySet<V, K> {
            final Inverse this$1;

            InverseEntrySet(Inverse inverse) {
                this.this$1 = inverse;
            }

            @Override
            ImmutableList<Map.Entry<V, K>> createAsList() {
                return new ImmutableAsList<Map.Entry<V, K>>(this) {
                    final InverseEntrySet this$2;

                    {
                        this.this$2 = this;
                    }

                    @Override
                    ImmutableCollection<Map.Entry<V, K>> delegateCollection() {
                        return this.this$2;
                    }

                    @Override
                    public Map.Entry<V, K> get(int i) {
                        ImmutableMapEntry immutableMapEntry = this.this$2.this$1.this$0.entries[i];
                        return Maps.immutableEntry(immutableMapEntry.getValue(), immutableMapEntry.getKey());
                    }
                };
            }

            @Override
            public int hashCode() {
                return this.this$1.this$0.hashCode;
            }

            @Override
            boolean isHashCodeFast() {
                return true;
            }

            @Override
            public UnmodifiableIterator<Map.Entry<V, K>> iterator() {
                return asList().iterator();
            }

            @Override
            ImmutableMap<V, K> map() {
                return this.this$1;
            }
        }

        private Inverse(RegularImmutableBiMap regularImmutableBiMap) {
            this.this$0 = regularImmutableBiMap;
        }

        @Override
        ImmutableSet<Map.Entry<V, K>> createEntrySet() {
            return new InverseEntrySet(this);
        }

        @Override
        public K get(Object obj) {
            if (obj == null) {
                return null;
            }
            for (ImmutableMapEntry nextInValueBucket = this.this$0.valueTable[Hashing.smear(obj.hashCode()) & this.this$0.mask]; nextInValueBucket != null; nextInValueBucket = nextInValueBucket.getNextInValueBucket()) {
                if (obj.equals(nextInValueBucket.getValue())) {
                    return nextInValueBucket.getKey();
                }
            }
            return null;
        }

        @Override
        public ImmutableBiMap<K, V> inverse() {
            return this.this$0;
        }

        @Override
        boolean isPartialView() {
            return false;
        }

        @Override
        public int size() {
            return inverse().size();
        }

        @Override
        Object writeReplace() {
            return new InverseSerializedForm(this.this$0);
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

    private static final class NonTerminalBiMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        private final ImmutableMapEntry<K, V> nextInKeyBucket;
        private final ImmutableMapEntry<K, V> nextInValueBucket;

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

    RegularImmutableBiMap(int i, ImmutableMapEntry.TerminalEntry<?, ?>[] terminalEntryArr) {
        int iClosedTableSize = Hashing.closedTableSize(i, 1.2d);
        this.mask = iClosedTableSize - 1;
        ?? r8 = (ImmutableMapEntry<K, V>[]) createEntryArray(iClosedTableSize);
        ?? r9 = (ImmutableMapEntry<K, V>[]) createEntryArray(iClosedTableSize);
        ?? r10 = (ImmutableMapEntry<K, V>[]) createEntryArray(i);
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int i4 = i3;
            int i5 = i2;
            if (i5 >= i) {
                this.keyTable = r8;
                this.valueTable = r9;
                this.entries = r10;
                this.hashCode = i4;
                return;
            }
            ?? nonTerminalBiMapEntry = terminalEntryArr[i5];
            Object key = nonTerminalBiMapEntry.getKey();
            Object value = nonTerminalBiMapEntry.getValue();
            int iHashCode = key.hashCode();
            int iHashCode2 = value.hashCode();
            int iSmear = Hashing.smear(iHashCode) & this.mask;
            int iSmear2 = Hashing.smear(iHashCode2) & this.mask;
            ?? r5 = r8[iSmear];
            for (?? nextInKeyBucket = r5; nextInKeyBucket != 0; nextInKeyBucket = nextInKeyBucket.getNextInKeyBucket()) {
                checkNoConflict(!key.equals(nextInKeyBucket.getKey()), "key", nonTerminalBiMapEntry, nextInKeyBucket);
            }
            ?? r4 = r9[iSmear2];
            for (?? nextInValueBucket = r4; nextInValueBucket != 0; nextInValueBucket = nextInValueBucket.getNextInValueBucket()) {
                checkNoConflict(!value.equals(nextInValueBucket.getValue()), "value", nonTerminalBiMapEntry, nextInValueBucket);
            }
            if (r5 != 0 || r4 != 0) {
                nonTerminalBiMapEntry = new NonTerminalBiMapEntry(nonTerminalBiMapEntry, r5, r4);
            }
            r8[iSmear] = nonTerminalBiMapEntry;
            r9[iSmear2] = nonTerminalBiMapEntry;
            r10[i5] = nonTerminalBiMapEntry;
            i3 = (iHashCode ^ iHashCode2) + i4;
            i2 = i5 + 1;
        }
    }

    private static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int i) {
        return new ImmutableMapEntry[i];
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet<K, V>(this) {
            final RegularImmutableBiMap this$0;

            {
                this.this$0 = this;
            }

            @Override
            ImmutableList<Map.Entry<K, V>> createAsList() {
                return new RegularImmutableAsList(this, this.this$0.entries);
            }

            @Override
            public int hashCode() {
                return this.this$0.hashCode;
            }

            @Override
            boolean isHashCodeFast() {
                return true;
            }

            @Override
            public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
                return asList().iterator();
            }

            @Override
            ImmutableMap<K, V> map() {
                return this.this$0;
            }
        };
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
    public ImmutableBiMap<V, K> inverse() {
        ImmutableBiMap<V, K> immutableBiMap = this.inverse;
        if (immutableBiMap != null) {
            return immutableBiMap;
        }
        Inverse inverse = new Inverse();
        this.inverse = inverse;
        return inverse;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public int size() {
        return this.entries.length;
    }
}
