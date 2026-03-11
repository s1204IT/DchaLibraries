package com.google.common.collect;

import com.google.common.collect.ImmutableMapEntry;
import java.util.Map;

final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
    private static final long serialVersionUID = 0;
    private final transient ImmutableMapEntry<K, V>[] entries;
    private final transient int mask;
    private final transient ImmutableMapEntry<K, V>[] table;

    RegularImmutableMap(int i, ImmutableMapEntry.TerminalEntry<?, ?>[] terminalEntryArr) {
        this.entries = createEntryArray(i);
        int iClosedTableSize = Hashing.closedTableSize(i, 1.2d);
        this.table = createEntryArray(iClosedTableSize);
        this.mask = iClosedTableSize - 1;
        for (int i2 = 0; i2 < i; i2++) {
            ?? nonTerminalMapEntry = terminalEntryArr[i2];
            Object key = nonTerminalMapEntry.getKey();
            int iSmear = Hashing.smear(key.hashCode()) & this.mask;
            ImmutableMapEntry<K, V> immutableMapEntry = this.table[iSmear];
            if (immutableMapEntry != null) {
                nonTerminalMapEntry = new NonTerminalMapEntry(nonTerminalMapEntry, immutableMapEntry);
            }
            this.table[iSmear] = nonTerminalMapEntry;
            this.entries[i2] = nonTerminalMapEntry;
            checkNoConflictInBucket(key, nonTerminalMapEntry, immutableMapEntry);
        }
    }

    RegularImmutableMap(Map.Entry<?, ?>[] entryArr) {
        ImmutableMapEntry<K, V> nonTerminalMapEntry;
        int length = entryArr.length;
        this.entries = createEntryArray(length);
        int iClosedTableSize = Hashing.closedTableSize(length, 1.2d);
        this.table = createEntryArray(iClosedTableSize);
        this.mask = iClosedTableSize - 1;
        for (int i = 0; i < length; i++) {
            Map.Entry<?, ?> entry = entryArr[i];
            Object key = entry.getKey();
            Object value = entry.getValue();
            CollectPreconditions.checkEntryNotNull(key, value);
            int iSmear = Hashing.smear(key.hashCode()) & this.mask;
            ImmutableMapEntry<K, V> immutableMapEntry = this.table[iSmear];
            if (immutableMapEntry == null) {
                nonTerminalMapEntry = new ImmutableMapEntry.TerminalEntry<>(key, value);
            } else {
                nonTerminalMapEntry = new NonTerminalMapEntry<>(key, value, immutableMapEntry);
            }
            this.table[iSmear] = nonTerminalMapEntry;
            this.entries[i] = nonTerminalMapEntry;
            checkNoConflictInBucket(key, nonTerminalMapEntry, immutableMapEntry);
        }
    }

    private void checkNoConflictInBucket(K k, ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2) {
        while (immutableMapEntry2 != null) {
            checkNoConflict(!k.equals(immutableMapEntry2.getKey()), "key", immutableMapEntry, immutableMapEntry2);
            immutableMapEntry2 = immutableMapEntry2.getNextInKeyBucket();
        }
    }

    private static final class NonTerminalMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        private final ImmutableMapEntry<K, V> nextInKeyBucket;

        NonTerminalMapEntry(K k, V v, ImmutableMapEntry<K, V> immutableMapEntry) {
            super(k, v);
            this.nextInKeyBucket = immutableMapEntry;
        }

        NonTerminalMapEntry(ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2) {
            super(immutableMapEntry);
            this.nextInKeyBucket = immutableMapEntry2;
        }

        @Override
        ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return this.nextInKeyBucket;
        }

        @Override
        ImmutableMapEntry<K, V> getNextInValueBucket() {
            return null;
        }
    }

    private ImmutableMapEntry<K, V>[] createEntryArray(int i) {
        return new ImmutableMapEntry[i];
    }

    @Override
    public V get(Object obj) {
        if (obj == null) {
            return null;
        }
        for (ImmutableMapEntry<K, V> nextInKeyBucket = this.table[Hashing.smear(obj.hashCode()) & this.mask]; nextInKeyBucket != null; nextInKeyBucket = nextInKeyBucket.getNextInKeyBucket()) {
            if (obj.equals(nextInKeyBucket.getKey())) {
                return nextInKeyBucket.getValue();
            }
        }
        return null;
    }

    @Override
    public int size() {
        return this.entries.length;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new EntrySet();
    }

    private class EntrySet extends ImmutableMapEntrySet<K, V> {
        private EntrySet() {
        }

        @Override
        ImmutableMap<K, V> map() {
            return RegularImmutableMap.this;
        }

        @Override
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return asList().iterator();
        }

        @Override
        ImmutableList<Map.Entry<K, V>> createAsList() {
            return new RegularImmutableAsList(this, RegularImmutableMap.this.entries);
        }
    }
}
